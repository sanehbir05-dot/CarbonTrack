# Deploying CarbonTrack on AWS

Target: one EC2 instance (Ubuntu 24.04, t3.small), Docker Compose (`db`, `backend`, `web`), images from GHCR, deployed by GitHub Actions on push to `main`. Estimated cost is about $22-25/month.

## 1. AWS account
- Turn on MFA for root and use an IAM admin user.
- Create a Budget (about $25/month) with email alerts.
- Use one region, for example `ap-south-1`.

## 2. EC2 host
1. Launch Ubuntu 24.04, t3.small, 20-30 GB gp3.
2. Security group: 22 from your IP only, 80 and 443 from anywhere. Never open 5432 or 8080.
3. Allocate an Elastic IP and attach it.
4. Install Docker and add swap:
   ```bash
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker ubuntu
   sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
   sudo mkswap /swapfile && sudo swapon /swapfile
   echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
   ```
5. Set up the app folder:
   ```bash
   sudo mkdir -p /opt/carbontrack && sudo chown ubuntu /opt/carbontrack
   scp deploy/docker-compose.prod.yml ubuntu@<ip>:/opt/carbontrack/
   scp deploy/.env.example ubuntu@<ip>:/opt/carbontrack/.env   # then edit and chmod 600
   ```
6. Make the two GHCR packages public, or run `docker login ghcr.io` with a read-only token.

## 3. GitHub secrets
`EC2_HOST` (the domain or IP), `EC2_USER`, `EC2_SSH_KEY` (a dedicated deploy key).

## 4. Domain and HTTPS
1. Register the domain in Route 53 and add an A record to the Elastic IP.
2. Issue a certificate (after the site works over HTTP):
   ```bash
   docker compose -f docker-compose.prod.yml run --rm --entrypoint certbot \
     -v carbontrack_certbot-www:/var/www/certbot -v carbontrack_certbot-certs:/etc/letsencrypt \
     certbot/certbot certonly --webroot -w /var/www/certbot -d <domain>
   ```
   Then add a 443 server block to `nginx.conf` using `/etc/letsencrypt/live/<domain>/` and redirect port 80 to HTTPS. Renew with a weekly cron job running `certbot renew` and `docker compose exec web nginx -s reload`.

## 5. Operations
- Never run `docker compose down -v` on the server (it deletes the database).
- Never edit a Flyway migration that has already run. Add a new one.
- Check `docker stats` and `df -h` now and then. Back up with `pg_dump` to a private S3 bucket.
- Smoke test: `bash deploy/smoke.sh https://<domain>`.
