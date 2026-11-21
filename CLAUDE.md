# CarbonTrack: working notes for Claude

Keep this file current as work progresses. Update the "Progress" and "Decisions" sections whenever a step is finished or a choice is made.

## Project
Carbon emission tracking platform for Indian companies. React frontend, Spring Boot 3 / Java 21 backend, PostgreSQL 16. Full spec and API contract: `docs/` (see `docs/MANIT_deploy_integration.md` for deployment, with the shared contract in its appendix). README.md has the overview.

Team: Saneh (frontend), Guransh (backend), Manit (deployment, CI/CD, seed data, AI endpoints).

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

## Known conditions
- `deploy.yml` smoke step uses `https://$EC2_HOST`. It only works once a domain with HTTPS exists. Until then run `bash deploy/smoke.sh http://<elastic-ip>` by hand. **TODO: change the smoke step when the domain is set.**
- `backend/` has only a Dockerfile; there is no `pom.xml` until Guransh merges. CI jobs skip when `pom.xml` or `package.json` are missing.
- Frontend Dockerfile builds from the repo root: `docker build -f frontend/Dockerfile .`
- Never run `docker compose down -v` on the server. Never edit an applied Flyway migration.

## Progress
- [x] Plan agreed (`~/.claude/plans/so-i-want-to-tranquil-canyon.md`)
- [x] Branch `ops/aws-deployment` created and pushed
- [x] Scaffolding committed: Dockerfiles, nginx.conf, prod compose, .env.example, smoke.sh, ci.yml, deploy.yml, deploy/README.md
- [ ] AWS account: MFA, IAM admin user, budget alert (user, in console)
- [ ] EC2 launched, Elastic IP attached, Docker installed (user, in console)
- [ ] `.env` created on server, GitHub secrets added
- [ ] Hello-world deployed over HTTP
- [ ] Domain registered in Route 53, A record set
- [ ] HTTPS (certbot, 443 block in nginx.conf), smoke step updated
- [ ] PR `ops/aws-deployment` into `develop`
- [ ] Real backend and frontend merged and deployed

## Values to fill in as we learn them (no secrets here)
- AWS region: _not chosen_
- Elastic IP: _not allocated_
- Domain: _not chosen_
- GitHub repo: sanehbir05-dot/CarbonTrack
