# CarbonTrack: working notes for Claude

Keep this file current as work progresses. Update the "Progress" and "Decisions" sections whenever a step is finished or a choice is made.

## Project
Carbon emission tracking platform for Indian companies. React frontend, Spring Boot 3 / Java 21 backend, PostgreSQL 16. Full spec and API contract: `docs/` (see `docs/MANIT_deploy_integration.md` for deployment, with the shared contract in its appendix). README.md has the overview.

Team: Saneh (frontend), Guransh (backend), Manit (deployment, CI/CD, seed data, AI endpoints).

## Session notes
Per-session notes live in `claude/` (index: `claude/README.md`). Read the latest file there at the start of a session, and add a new `claude/session-YYYY-MM-DD-<topic>.md` when a session ends. Never put secrets in them.

## Branches
- `main`: deployable; pushing to it triggers `deploy.yml`.
- `develop`: integration branch (README + docs only so far; no backend or frontend code yet).
- `feature/frontend-prototype`: Saneh's frontend prototype (plain JSX, not yet merged).
- `ops/aws-deployment`: deployment work (this branch). PR into `develop` when ready. Do not commit deployment work directly to `develop`.

## Deployment target (decided)
- One AWS EC2 instance (Ubuntu 24.04, t3.small), Docker Compose with `db`, `backend`, `web` (Nginx). Only `web` publishes ports (80, 443).
- Images built in GitHub Actions, pushed to GHCR, pulled on the server over SSH.
- Domain via Route 53 with HTTPS (Let's Encrypt). No domain yet.
- Budget: AWS free tier plus about $100 credits; estimate $22-25/month.
- Server folder: `/opt/carbontrack` (compose file and real `.env`, never committed).
- GitHub secrets: `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`.

- Server access: instance launched without a key pair; Session Manager (browser terminal, `ssm-user`) works via IAM role `carbontrack-ec2-ssm`. User then asked to ALSO have SSH (multi-line paste breaks in Session Manager): add own ed25519 public key (`~/.ssh/carbontrack_ed25519`) to `/home/ubuntu/.ssh/authorized_keys` and open port 22 to My IP only. Keep Session Manager as a backup. Open question: `deploy.yml` currently deploys over SSH using `EC2_SSH_KEY`; decide later between a separate deploy-only key (port 22 open) or switching the deploy step to SSM.

## Known conditions
- `deploy.yml` smoke step uses `https://$EC2_HOST`. It only works once a domain with HTTPS exists. Until then run `bash deploy/smoke.sh http://<elastic-ip>` by hand. **TODO: change the smoke step when the domain is set.**
- `backend/` has only a Dockerfile; there is no `pom.xml` until Guransh merges. CI jobs skip when `pom.xml` or `package.json` are missing.
- Frontend Dockerfile builds from the repo root: `docker build -f frontend/Dockerfile .`
- Never run `docker compose down -v` on the server. Never edit an applied Flyway migration.

## Progress
- [x] Plan agreed (`~/.claude/plans/so-i-want-to-tranquil-canyon.md`)
- [x] Branch `ops/aws-deployment` created and pushed
- [x] Scaffolding committed: Dockerfiles, nginx.conf, prod compose, .env.example, smoke.sh, ci.yml, deploy.yml, deploy/README.md
- [x] AWS account: MFA, IAM admin user, budget alert, region chosen (user confirmed steps 1-3)
- [x] EC2 launched (Session Manager access works), Elastic IP attached
- [x] Swap (2 GB, in /etc/fstab) and /opt/carbontrack (owner ssm-user) done
- [x] Docker 29.10.0 and Compose v5.8.0 installed and verified (hello-world ran)
- Note: Session Manager terminal mangles multi-line pastes; give the user single-line or `&&`-chained commands.
- [x] SSH works: `ssh -i ~/.ssh/carbontrack_ed25519 ubuntu@15.252.44.221` (port 22 open to My IP only; Session Manager still works as backup)
- [x] Compose file and `.env` on server (`.env` has 10 vars, mode 600, secrets generated on the server; compose config validates)
- [ ] GitHub secrets added
- Note: Claude can run server commands over SSH from the local shell (key is loaded in the Mac ssh-agent via `ssh-add --apple-use-keychain`). SSH from this shell occasionally times out; retry once. Never print `.env` values.
- [x] Hello-world: temporary Nginx on port 80 reachable from the internet at http://15.252.44.221 (test container removed afterwards; security group ports 80/443 verified)
- [ ] Domain registered in Route 53, A record set
- [ ] HTTPS (certbot, 443 block in nginx.conf), smoke step updated
- [x] Session notes folder `claude/` created, READMEs updated
- [x] PR `ops/aws-deployment` into `develop` opened (see GitHub; do not merge without the user's go-ahead)
- [ ] Real backend and frontend merged and deployed

## Values to fill in as we learn them (no secrets here)
- AWS region: ap-south-1 (Mumbai)
- Elastic IP: 15.252.44.221
- Instance: t3.small (1.9 GiB RAM confirmed)
- Domain: _not chosen_
- GitHub repo: sanehbir05-dot/CarbonTrack
