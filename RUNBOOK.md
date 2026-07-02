# WorldsBank — Deployment Runbook
> Maintained by: Kamau | Domain: worldsbank.cfd | Server: 174.138.7.233 | Last updated: June 2026

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
The app is live at https://worldsbank.cfd with a valid TLS certificate.

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
This file is never committed to git. To edit: `nano ~/apps/WorldsBankBE/.env`

| Variable | Description |
|----------|-------------|
| POSTGRES_DB | Database name |
| POSTGRES_USER | Database user |
| POSTGRES_PASSWORD | Database password |
| SPRING_DATASOURCE_URL | Full JDBC connection string (uses postgres service name) |
| SPRING_DATASOURCE_USERNAME | DB username for Spring |
| SPRING_DATASOURCE_PASSWORD | DB password for Spring |
| JWT_SECRET | JWT signing secret |
| SPRING_MAIL_USERNAME | Gmail address for OTP emails |
| SPRING_MAIL_PASSWORD | Gmail app password |
| OPENAI_API_KEY | Groq API key |
| EXCHANGE_RATE_API_KEY | Exchange rate API key |

---

## Deploy a New Version

A push to the Kamau branch triggers CI/CD automatically — no manual steps needed.

To deploy manually:

```bash
ssh -i ~/.ssh/id_ed25519 kamau@174.138.7.233
cd ~/apps/WorldsBankBE
docker compose pull
docker compose up -d
```

---

## Roll Back to a Previous Version

Every image is tagged with the git commit SHA. Find the SHA from Docker Hub or GitHub Actions, then:

```bash
nano ~/apps/WorldsBankBE/docker-compose.yml
# Change: image: kv1248/worldsbank-backend:latest
# To:     image: kv1248/worldsbank-backend:<commit-sha>

docker compose up -d --force-recreate worldsbank
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

# Follow live logs — backend
docker compose logs -f worldsbank

# Last 50 lines — backend
docker compose logs worldsbank --tail 50

# Last 50 lines — frontend
docker compose logs frontend --tail 50

# All services live
docker compose logs -f
```

---

## Check Service Health

```bash
# Container status
cd ~/apps/WorldsBankBE && docker compose ps

# Test backend API directly
curl -s http://localhost:8080/v3/api-docs | head -c 100

# Test nginx routing to backend
curl -s https://worldsbank.cfd/api/v1/auth/login \
  -X POST -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test"}'

# Test frontend
curl -s https://worldsbank.cfd | head -c 100
```

---

## nginx Operations

```bash
# Test config before applying
sudo nginx -t

# Reload without downtime
sudo systemctl reload nginx

# View config
cat /etc/nginx/sites-available/worldsbank

# Live access log
sudo tail -f /var/log/nginx/access.log

# Live error log
sudo tail -f /var/log/nginx/error.log
```

---

## TLS Certificate

```bash
# Check certificate status and expiry
sudo certbot certificates

# Test renewal (safe — no changes made)
sudo certbot renew --dry-run

# Force renew if needed
sudo certbot renew --force-renewal

# Reload nginx after manual renewal
sudo systemctl reload nginx
```

UptimeRobot is configured to alert on downtime and certificate expiry before it occurs.

---

## Monitoring

- Live site: https://worldsbank.cfd
- Uptime monitor: UptimeRobot — https://dashboard.uptimerobot.com
- Alerts: email on downtime
- Server IP: 174.138.7.233

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
| 06 | Domain and DNS | ✅ worldsbank.cfd |
| 07 | nginx reverse proxy | ✅ |
| 08 | TLS with Let's Encrypt | ✅ |
| 09 | CI/CD with GitHub Actions | ✅ |
| 10 | Monitoring and uptime | ✅ |
| 11 | Runbook | ✅ |