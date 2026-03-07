# CogniSphere — Complete Deployment Guide

**Goal:** Get a live link that works 24/7, even when your laptop is off.

**How it works:** Your code runs on an AWS EC2 server (a computer in Amazon's data center that never turns off). People reach it through the server's public IP address.

**Port reference (important to understand):**
| Where | Backend port | Frontend port |
|-------|-------------|---------------|
| Your laptop (no Docker) | 8081 | 5173 |
| Docker on your laptop | 8080 | 3000 |
| EC2 server (same Docker) | 8080 | 3000 |

---

## What is Docker and Why We Use It

**The problem without Docker:**  
Your app needs Java 21, Maven, Node.js, and specific library versions installed. When you copy your code to an EC2 server, none of that is there — it breaks.

**What Docker does:**  
Docker packages your app + everything it needs (Java, Node, configs, files) into a "container" — a sealed box that runs the same everywhere. Your EC2 just needs Docker installed, and then your box runs perfectly.

**In this project:**
- `backend-dev` container → runs your Spring Boot Java app on port **8080**
- `frontend-dev` container → runs Nginx serving your React app on port **3000**
- `neo4j-dev` container → runs Neo4j database locally (on EC2 we use AuraDB cloud instead, so this container can be skipped)

`docker-compose` is the tool that starts all containers together with one command: `docker-compose up`.

---

## Prerequisites

Before starting, make sure you have:
- [ ] AWS account set up (Steps 1–12 of AWS_SETUP_GUIDE.md done)
- [ ] S3 bucket created, Bedrock/Textract/Transcribe enabled
- [ ] Neo4j AuraDB running and credentials ready
- [ ] Google API key (for web search feature)
- [ ] Your `application.properties` file filled with real values

---

## Part 1 — Push Code to GitHub

Your code needs to be on GitHub so you can pull it onto the EC2 server. **Your credentials are safe** — `.gitignore` already blocks `application.properties` from being uploaded.

### Step 1.1 — Create a GitHub Account & Repository

1. Go to **[github.com](https://github.com)** → Sign up or log in
2. Click **"+"** (top right) → **New repository**
3. Settings:
   - **Repository name:** `cognisphere`
   - **Visibility:** Private ← important, keeps your code private
   - Leave everything else unchecked
4. Click **Create repository**
5. You'll see a page with a URL like `https://github.com/YOURNAME/cognisphere.git` — copy it

### Step 1.2 — Create a GitHub Personal Access Token

GitHub no longer accepts passwords — you need a token:

1. GitHub → click your profile picture → **Settings**
2. Scroll to the bottom → **Developer settings**
3. **Personal access tokens** → **Tokens (classic)** → **Generate new token (classic)**
4. Give it a name: `cognisphere-deploy`
5. Set expiration: **90 days**
6. Check the **`repo`** checkbox
7. Click **Generate token**
8. **Copy the token immediately** — you won't see it again. Save it in Notepad.

### Step 1.3 — Initialize Git and Push

Open the terminal in VS Code (`Ctrl + \``) and run:

```powershell
cd "d:\Projects\Cognisphere\Cognisphere"

# Initialize git
git init

# Set your identity (replace with your details)
git config user.name "Your Name"
git config user.email "you@email.com"

# Stage all files
git add .
```

Now **verify your credentials are NOT included**:
```powershell
git status
```
Scroll through the output. You should **NOT** see `application.properties` listed. If you do — stop and don't continue.

```powershell
# Commit
git commit -m "Initial commit - CogniSphere"

# Connect to GitHub (replace URL with yours from Step 1.1)
git remote add origin https://github.com/YOURNAME/cognisphere.git

# Push
git branch -M main
git push -u origin main
```

When prompted:
- **Username:** your GitHub username
- **Password:** paste the token from Step 1.2 (not your GitHub password)

### Step 1.4 — Verify

Open `https://github.com/YOURNAME/cognisphere` in your browser. You should see your project files. Confirm `application.properties` is **not there**.

---

## Part 2 — Launch an EC2 Server on AWS

This server stays running 24/7 in Amazon's data center — it has nothing to do with your laptop being on or off.

### Step 2.1 — Launch the Instance

1. Go to **AWS Console → EC2 → Launch Instance**
2. Fill in:
   - **Name:** `cognisphere-server`
   - **AMI:** `Amazon Linux 2023` (select this)
   - **Instance type:** `t2.medium` — **important**, smaller types don't have enough RAM for Java
   - **Key pair:** Click **Create new key pair**
     - Name: `cognisphere-key`
     - Type: RSA
     - Format: `.pem`
     - Click **Create** → a `.pem` file downloads automatically → **don't lose this file**
3. **Network settings** → click **Edit**:
   - ✅ Allow SSH traffic from: **My IP** (so only you can SSH in)
   - ✅ Allow HTTP traffic from: `0.0.0.0/0` (anyone can access port 80)
   - Click **Add security group rule**:
     - Type: `Custom TCP` | Port: `8080` | Source: `0.0.0.0/0`
   - Click **Add security group rule**:
     - Type: `Custom TCP` | Port: `3000` | Source: `0.0.0.0/0`
4. **Storage:** Change to **20 GB**
5. Click **Launch Instance**

Wait ~1 minute. Click **View Instances**. When state shows **Running**, copy the **Public IPv4 address** (e.g. `54.123.45.67`). This is your server's address.

### Step 2.2 — Give EC2 Permission to Use AWS Services

Your backend needs to call S3, Bedrock, Textract, etc. without credentials hardcoded. We attach an IAM role to the EC2:

1. **AWS Console → IAM → Roles → Create Role**
2. Trusted entity type: **AWS service** → Use case: **EC2** → Next
3. Search and attach these policies one by one:
   - `AmazonS3FullAccess`
   - `AmazonBedrockFullAccess`
   - `AmazonTextractFullAccess`
   - `AmazonTranscribeFullAccess`
   - `AmazonRekognitionFullAccess`
4. Role name: `cognisphere-ec2-role` → **Create role**
5. Go back to **EC2 → Instances → click your instance**
6. **Actions → Security → Modify IAM role** → select `cognisphere-ec2-role` → **Update IAM role**

---

## Part 3 — Set Up the Server

### Step 3.1 — SSH Into Your Server

**On Windows, open PowerShell:**

First, fix permissions on your key file (required by SSH):
```powershell
icacls "C:\path\to\cognisphere-key.pem" /inheritance:r
icacls "C:\path\to\cognisphere-key.pem" /grant:r "$($env:USERNAME):(R)"
```

Then connect (replace with your actual IP):
```powershell
ssh -i "C:\path\to\cognisphere-key.pem" ec2-user@54.123.45.67
```

You should see a welcome message and a prompt like `[ec2-user@ip-172-31... ~]$` — you're now inside your EC2 server.

### Step 3.2 — Install Docker and Java

Copy-paste this entire block into the SSH terminal:

```bash
# Update system
sudo yum update -y

# Install Docker
sudo yum install -y docker git
sudo service docker start
sudo usermod -aG docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Install Java 21 and Maven
sudo yum install -y java-21-amazon-corretto-devel maven

# Log out and back in so Docker group applies
exit
```

Reconnect:
```powershell
ssh -i "C:\path\to\cognisphere-key.pem" ec2-user@54.123.45.67
```

Verify everything works:
```bash
docker --version      # should print Docker version
docker-compose --version  # should print v2.x.x
java -version         # should say openjdk 21
```

---

## Part 4 — Deploy the App

### Step 4.1 — Clone Your Code

```bash
git clone https://github.com/YOURNAME/cognisphere.git
cd cognisphere
```

When prompted: use your GitHub username and the **personal access token** (not your password).

### Step 4.2 — Create the Config File

`application.properties` was not pushed to GitHub (intentionally). Create it now on the server:

```bash
nano backend/src/main/resources/application.properties
```

Paste this and fill in your real values:

```properties
spring.application.name=cognisphere

# AWS (credentials come automatically from the IAM role attached to EC2)
aws.region=us-east-1
aws.s3.bucket=cognisphere-storage-9125
aws.bedrock.modelId=openai.gpt-oss-120b-1:0
aws.bedrock.embeddingModelId=amazon.titan-embed-text-v2:0

# Neo4j AuraDB
spring.neo4j.uri=neo4j+s://YOUR_ID.databases.neo4j.io
spring.neo4j.authentication.username=YOUR_NEO4J_USERNAME
spring.neo4j.authentication.password=YOUR_NEO4J_PASSWORD
spring.neo4j.database=YOUR_NEO4J_DATABASE

server.address=0.0.0.0
server.port=8080

spring.servlet.multipart.max-file-size=30MB
spring.servlet.multipart.max-request-size=30MB

# CORS — allow your frontend (EC2 IP on port 3000)
cors.allowed-origins=http://54.123.45.67:3000

# Google Custom Search
google.api.key=YOUR_GOOGLE_API_KEY
google.customsearch.cx=YOUR_GOOGLE_CSE_CX
google.customsearch.search-url=https://cse.google.com/cse?cx=YOUR_GOOGLE_CSE_CX
```

Replace `54.123.45.67` with your actual EC2 IP in the `cors.allowed-origins` line.

Save: `Ctrl+O` → `Enter` → `Ctrl+X`

### Step 4.3 — Create the .env File

```bash
nano .env
```

Paste (replace with your actual EC2 IP):
```
VITE_API_URL=http://54.123.45.67:8080
CORS_ALLOWED_ORIGINS=http://54.123.45.67:3000
```

Save: `Ctrl+O` → `Enter` → `Ctrl+X`

### Step 4.4 — Build and Start

```bash
# Build the backend JAR
cd backend
mvn clean package -DskipTests
cd ..

# Build Docker images and start all containers
docker-compose up --build -d
```

This takes 3–5 minutes the first time (downloading base images, compiling). The `-d` flag means "run in background."

### Step 4.5 — Check It's Running

```bash
docker ps
```

You should see 3 containers all with status `Up`:
```
backend-dev    Up X minutes
frontend-dev   Up X minutes
neo4j-dev      Up X minutes
```

---

## Part 5 — Access Your Live App

Open these in your browser (replace with your EC2 IP):

| What | URL |
|------|-----|
| **Frontend (your app)** | `http://54.123.45.67:3000` |
| **Backend API** | `http://54.123.45.67:8080/api/graph` |

**This link works from anywhere, forever, even when your laptop is off** — because the app is running on Amazon's servers.

---

## Updating the App After Code Changes

When you make changes and want them live:

```powershell
# On your laptop — push changes to GitHub
git add .
git commit -m "update: describe your change"
git push
```

```bash
# On the EC2 server — pull and redeploy
ssh -i "cognisphere-key.pem" ec2-user@54.123.45.67

cd cognisphere
git pull

cd backend
mvn clean package -DskipTests
cd ..

docker-compose up --build -d
```

---

## Managing the Server

```bash
# Check all running containers
docker ps

# See live backend logs (Ctrl+C to stop)
docker logs -f backend-dev

# See frontend logs
docker logs -f frontend-dev

# Restart everything
docker-compose restart

# Stop everything
docker-compose down

# Start again
docker-compose up -d
```

---

## Stop/Start the EC2 to Save Money

EC2 charges by the hour (~$0.046/hr for t2.medium = ~$1.10/day). Stop it when not needed:

```powershell
# Stop the server (no charges for compute while stopped)
aws ec2 stop-instances --instance-ids YOUR_INSTANCE_ID

# Start it again before a demo
aws ec2 start-instances --instance-ids YOUR_INSTANCE_ID
```

Find your instance ID in the EC2 console, or:
```powershell
aws ec2 describe-instances --filters "Name=tag:Name,Values=cognisphere-server" `
  --query "Reservations[0].Instances[0].InstanceId" --output text
```

> ⚠️ **Important:** The public IP changes every time you stop/start. If you need a fixed IP for sharing a permanent link, allocate an **Elastic IP** in the EC2 console (free while attached to a running instance) and associate it with your instance.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Page won't open in browser | Make sure EC2 security group has ports 3000 and 8080 open to `0.0.0.0/0` |
| Backend container keeps restarting | `docker logs backend-dev` — usually wrong Neo4j password or missing application.properties |
| Frontend shows blank page or CORS error | Check `.env` has correct EC2 IP; rebuild: `docker-compose up --build -d` |
| Neo4j AuraDB connection refused | Log into Neo4j Aura console — free tier pauses after 3 days of inactivity; click Resume |
| "Permission denied" on .pem file | Run the `icacls` commands from Step 3.1 |
| Out of memory / container killed | You're on t2.micro — upgrade to t2.medium |
| Changes not showing after redeploy | Make sure you ran `mvn clean package -DskipTests` before `docker-compose up --build -d` |

---

## Cost Summary

| Resource | Monthly cost |
|----------|-------------|
| EC2 t2.medium (always on) | ~$34 |
| EC2 t2.medium (8hrs/day only) | ~$9 |
| Neo4j AuraDB Free | $0 |
| S3 storage (small) | < $1 |
| Bedrock per chat message | ~$0.003 |
| **For a hackathon demo (stop when not presenting)** | **< $5 total** |
