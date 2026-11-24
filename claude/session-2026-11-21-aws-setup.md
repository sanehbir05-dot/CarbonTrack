# Session notes: AWS server setup (21 Nov 2026)

Branch: `ops/aws-deployment`. Owner: Manit. These notes record what was decided and done so a later session (or a teammate) can pick up without re-deriving it. No secrets are stored here.

## Goal
Deploy CarbonTrack on AWS with the free tier plus about $100 of credits. Target from `docs/MANIT_deploy_integration.md`: one EC2 instance running a Docker Compose stack (`db`, `backend`, `web`), images built by GitHub Actions and pushed to GHCR.

## Decisions

| Decision | Choice | Why |
|---|---|---|
| Architecture | Single EC2 + Docker Compose | About $22-25/month, so credits last about 4 months. Managed services (RDS, Fargate) would cost $60-90/month. |
| Region | `ap-south-1` (Mumbai) | Users are Indian companies. |
| Instance | t3.small, Ubuntu 24.04, 25 GiB gp3 | Needs 2 GB RAM for Spring Boot plus Postgres. Swap added as a safety net. |
| Domain and HTTPS | Own domain via Route 53, Let's Encrypt | Not done yet. |
| Network | Ports 80 and 443 open to everyone, 22 open to the owner's IP only, nothing for 5432 or 8080 | A public site must accept visitors. Only Nginx is exposed. |
| Server access | Session Manager (browser) and SSH key | No key pair at launch. Session Manager mangles multi-line pastes, so an ed25519 key was added through it afterwards. |
| Git workflow | Work on `ops/aws-deployment`, PR into `develop` | Matches the `ops/<feature>` convention in the docs. |

## What was done

### In the repo (committed)
- `docker-compose.yml`: local Postgres.
- `backend/Dockerfile`, `frontend/Dockerfile` (frontend builds from the repo root).
- `deploy/nginx.conf`, `deploy/docker-compose.prod.yml`, `deploy/.env.example`, `deploy/smoke.sh`, `deploy/README.md`.
- `.github/workflows/ci.yml` (runs on pull requests, skips backend or frontend jobs when their files are missing) and `deploy.yml` (runs on push to `main`).
- `.dockerignore`, and a `.gitignore` change so `deploy/.env.example` is not ignored.
- `CLAUDE.md` and this `claude/` folder.

### In AWS and on the server (not in git)
1. Root and IAM admin user with MFA, a $25/month budget alert, region chosen.
2. IAM role `carbontrack-ec2-ssm` (policy `AmazonSSMManagedInstanceCore`) for Session Manager.
3. EC2 instance `carbontrack-server` launched, Elastic IP `15.252.44.221` attached.
4. Security group: 80 and 443 from anywhere, 22 from the owner's IP.
5. Docker 29.10.0, Compose v5.8.0, 2 GB swap (saved in `/etc/fstab`).
6. `/opt/carbontrack` holds `docker-compose.prod.yml` and `.env` (mode 600). The secrets (`DB_PASSWORD`, `JWT_SECRET`) were generated on the server and never shown.
7. Hello-world test: a temporary Nginx on port 80 was reachable from the internet at the Elastic IP, then removed.

## Problems hit and how they were solved
- **Multi-line paste in Session Manager** joined lines and dropped the last newline, so swap setup half-ran and `/etc/fstab` was nearly corrupted. Fix: single-line or `&&`-chained commands, then SSH access.
- **Swap showed 0B and the folder owner was root** because those commands had been skipped. Fixed by rebuilding the swap file in one chained line.
- **SSH key had a passphrase**, so non-interactive use from Claude failed until it was loaded with `ssh-add --apple-use-keychain`.
- **SSH from the Claude shell sometimes times out.** Retrying once works.

## Open items
- [ ] Add GitHub secrets `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` (use a separate deploy-only key, not the personal one).
- [ ] Decide deploy transport: SSH from GitHub Actions (port 22 would need to be open wider) or SSM.
- [ ] Register the domain in Route 53 and set an A record to the Elastic IP.
- [ ] Issue the certificate, add the 443 block to `deploy/nginx.conf`, and change the `deploy.yml` smoke step (it currently uses `https://$EC2_HOST`, which fails against a raw IP).
- [ ] Real backend (Guransh) and frontend (Saneh) must merge before the pipeline can build and deploy the full app.
- [ ] Nightly `pg_dump` backup to S3.
- [ ] Note the credit expiry date.

## Rules to keep
- Never run `docker compose down -v` on the server.
- Never edit a Flyway migration that has already run.
- Never commit `.env`, keys, tokens or passwords. Never print `.env` values.
