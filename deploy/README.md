# Deploying CarbonTrack on AWS

Target: one EC2 instance (Ubuntu 24.04, t3.small), Docker Compose (`db`, `backend`, `web`), images from GHCR, deployed by GitHub Actions on push to `main`. Estimated cost is about $22-25/month.

**Status (see `CLAUDE.md` and `claude/` for details):** steps 1 to 3 are done and the server answers on port 80 from the internet. GitHub secrets, the domain, HTTPS and the real app are still to do.

## 1. AWS account
- Turn on MFA for root and use an IAM admin user.
- Create a Budget (about $25/month) with email alerts.
- Use one region. This project uses `ap-south-1` (Mumbai).

## 2. EC2 host
1. Create an IAM role `carbontrack-ec2-ssm` (trusted entity EC2, policy `AmazonSSMManagedInstanceCore`). It lets you open a browser terminal through Session Manager.
2. Launch Ubuntu 24.04, t3.small, 25 GB gp3. Attach the role under Advanced details, and proceed without a key pair.
3. Security group: 80 and 443 from anywhere. Never open 5432 or 8080. Add 22 from **My IP** only after step 5 below.
4. Allocate an Elastic IP and attach it.
5. Connect with **Connect > Session Manager** (you are `ssm-user`). To also use SSH, create a key on your own machine and install its public key:
   ```bash
   ssh-keygen -t ed25519 -f ~/.ssh/carbontrack_ed25519 -C carbontrack-ec2
   ```
   Then run this once in Session Manager, with your public key pasted in:
   ```bash
   sudo mkdir -p /home/ubuntu/.ssh && echo '<PUBLIC_KEY>' | sudo tee -a /home/ubuntu/.ssh/authorized_keys >/dev/null && sudo chown -R ubuntu:ubuntu /home/ubuntu/.ssh && sudo chmod 700 /home/ubuntu/.ssh && sudo chmod 600 /home/ubuntu/.ssh/authorized_keys
   ```
   Connect with `ssh -i ~/.ssh/carbontrack_ed25519 ubuntu@<elastic-ip>`. If SSH times out, your IP changed: update the port 22 rule.
6. Install Docker and add swap. **In Session Manager, paste one command per line.** Multi-line pastes get joined and break commands. Over SSH this does not matter.
   ```bash
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker ubuntu
   sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
   echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
   ```
7. Set up the app folder and files:
   ```bash
   sudo mkdir -p /opt/carbontrack && sudo chown ubuntu /opt/carbontrack
   scp -i ~/.ssh/carbontrack_ed25519 deploy/docker-compose.prod.yml ubuntu@<elastic-ip>:/opt/carbontrack/
   ```
   Create the real `.env` on the server with generated secrets (it never leaves the server):
   ```bash
   printf 'GHCR_OWNER=<github-owner-lowercase>\nDB_USERNAME=carbontrack\nDB_PASSWORD=%s\nJWT_SECRET=%s\nJWT_EXPIRY_MINUTES=1440\nAPP_SEED_ENABLED=true\nJAVA_TOOL_OPTIONS=-Xmx512m\nLLM_BASE_URL=\nLLM_API_KEY=\nLLM_MODEL=\n' "$(openssl rand -hex 16)" "$(openssl rand -hex 48)" > /opt/carbontrack/.env && chmod 600 /opt/carbontrack/.env
   ```
   Check it (prints names only): `cd /opt/carbontrack && cut -d= -f1 .env && docker compose -f docker-compose.prod.yml config --quiet && echo OK`
8. Check the server is reachable: run `docker run -d --rm --name hello-web -p 80:80 nginx:1.27-alpine`, open `http://<elastic-ip>`, then `docker stop hello-web`.
9. Make the two GHCR packages public, or run `docker login ghcr.io` with a read-only token.

## 3. GitHub secrets
`EC2_HOST` (the domain or IP), `EC2_USER` (`ubuntu`), `EC2_SSH_KEY` (a separate deploy-only key, not your personal one).

## 4. Domain and HTTPS (not done yet, command untested)
1. Register the domain in Route 53 and add an A record to the Elastic IP.
2. With the stack running, issue a certificate:
   ```bash
   docker run --rm -v carbontrack_certbot-www:/var/www/certbot -v carbontrack_certbot-certs:/etc/letsencrypt certbot/certbot certonly --webroot -w /var/www/certbot -d <domain>
   ```
   Then add a 443 server block to `nginx.conf` using `/etc/letsencrypt/live/<domain>/` and redirect port 80 to HTTPS. Renew with a weekly cron job running `certbot renew` and `docker compose exec web nginx -s reload`.
3. Change the smoke test step in `deploy.yml` if `EC2_HOST` is not the domain (it calls `https://$EC2_HOST`). Until HTTPS works, run `bash deploy/smoke.sh http://<elastic-ip>` by hand.

## 5. Operations
- Never run `docker compose down -v` on the server (it deletes the database).
- Never edit a Flyway migration that has already run. Add a new one.
- Check `docker stats` and `df -h` now and then. Back up with `pg_dump` to a private S3 bucket.
- Smoke test: `bash deploy/smoke.sh https://<domain>`.
