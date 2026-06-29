# WorldsBank — Deployment Runbook
> Maintained by: Kamau | Server: 174.138.7.233 | Last updated: June 2026

---

## Architecture
Browser

│

▼

nginx (port 80/443) ── host reverse proxy

│

├── / ──────────────▶ worldsbank_frontend (nginx:80) — Angular SPA

│

└── /api/ ──────────▶ worldsbank_app (Spring Boot:8080)

│

▼

worldsbank_db (PostgreSQL:5432)

All three services run as Docker containers on a single Ubuntu 24.04 VPS.
Images are published to Docker Hub at kv1248/worldsbank-backend and kv1248/worldsbank-frontend.
Every push to the Kamau branch triggers a GitHub Actions pipeline that builds and publishes fresh images automatically.

---

## Server Access

```bash
ssh -i ~/.ssh/id_ed25519 kamau@174.138.7.233
```

- OS: Ubuntu 24.04.4 LTS
- User: kamau (non-root, sudo)
- SSH: key-only — password auth and root login disabled
- Firewall: ufw — ports 22, 80, 443 only
- Auto-updates: unattended-upgrades active

---

## Repository Structure

| Repo | Purpose | Branch |
|------|---------|--------|
| github.com/KV-1248/WorldsBankBE | Spring Boot backend + Docker Compose | Kamau |
| github.com/KV-1248/WorldsBank-app | Angular frontend | Kamau |

---

## Environment Variables

All secrets live in `/home/kamau/apps/WorldsBankBE/.env` on the server.
This file is never committed to git.

| Variable | Description |
|----------|-------------|
| POSTGRES_DB | Database name |
| POSTGRES_USER | Database user |
| POSTGRES_PASSWORD | Database password |
| SPRING_DATASOURCE_URL | Full JDBC connection string |
| SPRING_DATASOURCE_USERNAME | DB username for Spring |
| SPRING_DATASOURCE_PASSWORD | DB password for Spring |
| JWT_SECRET | JWT signing secret |
| SPRING_MAIL_USERNAME | Gmail address for OTP emails |
| SPRING_MAIL_PASSWORD | Gmail app password |
| OPENAI_API_KEY | Groq API key |
| EXCHANGE_RATE_API_KEY | Exchange rate API key |

---

## Deploy a New Version

A push to the Kamau branch triggers CI/CD automatically.
To deploy manually:

```bash
# SSH into server
ssh -i ~/.ssh/id_ed25519 kamau@174.138.7.233

# Pull latest images
cd ~/apps/WorldsBankBE
docker compose pull

# Recreate containers with new images
docker compose up -d
```

---

## Roll Back to a Previous Version

Every image is tagged with the git commit SHA. Find the SHA from Docker Hub or GitHub Actions, then:

```bash
# Edit docker-compose.yml to pin the image to a specific SHA tag
nano ~/apps/WorldsBankBE/docker-compose.yml
# Change: image: kv1248/worldsbank-backend:latest
# To:     image: kv1248/worldsbank-backend:<commit-sha>

docker compose up -d
```

---

## Restart a Service

```bash
cd ~/apps/WorldsBankBE

# Restart backend only
docker compose restart worldsbank

# Restart frontend only
docker compose restart frontend

# Restart everything
docker compose down && docker compose up -d
```

---

## Read the Logs

```bash
cd ~/apps/WorldsBankBE

# Follow live logs for backend
docker compose logs -f worldsbank

# Last 50 lines from backend
docker compose logs worldsbank --tail 50

# Last 50 lines from frontend
docker compose logs frontend --tail 50

# All services
docker compose logs -f
```

---

## Check Service Health

```bash
# See running containers and status
docker compose ps

# Test backend API
curl http://localhost:8080/v3/api-docs

# Test nginx routing
curl http://localhost/api/v1/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test"}'
```

---

## nginx Operations

```bash
# Test nginx config before applying
sudo nginx -t

# Reload nginx (no downtime)
sudo systemctl reload nginx

# View nginx config
cat /etc/nginx/sites-available/worldsbank

# View nginx logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

---

## TLS Certificate (pending domain)

Once domain is configured:

```bash
# Issue certificate
sudo certbot --nginx -d yourdomain.com

# Test renewal
sudo certbot renew --dry-run

# Check certificate expiry
sudo certbot certificates
```

UptimeRobot is configured to alert on certificate expiry before it occurs.

---

## Monitoring

- Uptime monitor: UptimeRobot — http://174.138.7.233
- Alerts: email on downtime and certificate expiry
- Dashboard: dashboard.uptimerobot.com

---

## Module Completion Status

| Module | Description | Status |
|--------|-------------|--------|
| 00 | Server hardened | ✅ |
| 01 | Application documented | ✅ |
| 02 | Services containerized | ✅ |
| 03 | Docker Compose orchestration | ✅ |
| 04 | Images published to Docker Hub | ✅ |
| 05 | Deployed to production server | ✅ |
| 06 | Domain and DNS | ⏳ awaiting domain |
| 07 | nginx reverse proxy | ✅ (IP only, update server_name when domain arrives) |
| 08 | TLS with Let's Encrypt | ⏳ awaiting domain |
| 09 | CI/CD with GitHub Actions | ✅ |
| 10 | Monitoring and uptime | ✅ |
| 11 | Runbook | ✅ |