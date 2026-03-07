# CogniSphere

**AI-powered knowledge graph workspace** that transforms scattered information into a connected, visual graph you can explore, query, and expand — all in one place.

---

## What It Does

CogniSphere takes documents, URLs, images, audio, video, and plain text, extracts key concepts using AI, and organizes them as interconnected nodes in a visual knowledge graph. An embedded AI chat assistant lets you query your entire knowledge base conversationally.

**Core capabilities:**

- **Visual Knowledge Graph** — Interactive node-based graph powered by ReactFlow with force-directed layout, clustering, and drag-and-drop
- **Multi-Source Ingestion** — Upload PDFs (OCR via Textract), images, audio/video (transcription via AWS Transcribe), YouTube links, URLs, and plain text
- **AI Chat Assistant** — Conversational interface that searches your graph, answers questions, and auto-captures insights as new nodes
- **Smart Linking** — AI evaluates semantic relationships between nodes and auto-creates weighted edges with descriptive labels
- **Knowledge-Only Mode** — Toggle to restrict the assistant to search only your graph data for accurate, personal-knowledge answers
- **Web Search Fallback** — In "All Sources" mode, the AI answers any question using web search and silently saves the insight
- **Node Recommendations** — AI suggests related topics to expand your graph
- **Video Attachments** — Search and attach YouTube videos to any node

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                     Frontend                        │
│  React 18 · ReactFlow · Framer Motion · Vite        │
│  Dashboard with graph view, chat panel, upload box  │
└────────────────────┬────────────────────────────────┘
                     │ REST API
┌────────────────────▼────────────────────────────────┐
│                     Backend                         │
│  Spring Boot 3.4 · Java 21 · LangChain4j           │
│                                                     │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────┐ │
│  │  Agent    │  │ Parsers  │  │  Knowledge        │ │
│  │ Provider  │  │ (6 types)│  │  Service           │ │
│  │ + Tools   │  │          │  │  (CRUD, link, AI) │ │
│  └─────┬────┘  └────┬─────┘  └────────┬──────────┘ │
│        │            │                  │            │
└────────┼────────────┼──────────────────┼────────────┘
         │            │                  │
    ┌────▼────┐  ┌────▼─────┐   ┌───────▼───────┐
    │ Bedrock │  │   S3     │   │  Neo4j AuraDB │
    │ (LLM + │  │ (files)  │   │  (graph store) │
    │ embed)  │  └──────────┘   └───────────────┘
    └─────────┘
         │
   ┌─────┴──────────────────────┐
   │  Textract · Transcribe     │
   │  Rekognition               │
   └────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, ReactFlow, Framer Motion, Tailwind CSS, Vite |
| Backend | Spring Boot 3.4, Java 21, LangChain4j, Maven |
| AI/LLM | Amazon Bedrock (Claude / GPT), Amazon Titan Embeddings |
| Graph DB | Neo4j AuraDB (free tier — 200K nodes) |
| Storage | Amazon S3 |
| OCR | Amazon Textract |
| Transcription | Amazon Transcribe |
| Vision | Amazon Rekognition |
| Search | Google Custom Search API |
| Deployment | AWS ECS Fargate, Docker, GitHub Actions CI/CD |

---

## Project Structure

```
CogniSphere/
├── backend/
│   ├── src/main/java/com/cognisphere/backend/
│   │   ├── agent/           # AI agent, tools, parsers (6 source types)
│   │   ├── config/          # AWS clients, CORS, vector store
│   │   ├── controller/      # REST endpoints (chat, upload, graph, node)
│   │   ├── mapper/          # Neo4j query layer
│   │   ├── service/         # Knowledge graph service (save, link, merge, recommend)
│   │   └── util/            # Prompts, constants, helpers
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/      # ChatDialog, Flow (graph), UploadBox, NodeDetails
│   │   ├── pages/           # LandingPage, DashboardPage
│   │   ├── api/             # REST client (chat, graph, upload)
│   │   └── context/         # Theme provider
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml        # Local dev (with Neo4j container)
├── docker-compose.prod.yml   # Production
├── AWS_SETUP_GUIDE.md        # Complete AWS setup (16 steps)
└── DEPLOYMENT.md             # ECS Fargate deployment instructions
```

---

## Getting Started

### Prerequisites

- Java 21+
- Node.js 18+
- Maven 3.9+
- AWS account with Bedrock, S3, and Neo4j AuraDB configured
- Google API key for Custom Search (optional — for web search & YouTube)

### Local Development

```bash
# 1. Clone the repository
git clone https://github.com/your-org/cognisphere.git
cd cognisphere

# 2. Configure backend
# Copy and fill in your credentials:
cp backend/src/main/resources/application.properties.example \
   backend/src/main/resources/application.properties

# 3. Build and run backend
cd backend
mvn clean install -DskipTests
mvn spring-boot:run

# 4. In a new terminal — run frontend
cd frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173` and the backend at `http://localhost:8081`.

### Docker (Local)

```bash
docker-compose up --build
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/chat` | Send a message to the AI assistant |
| POST | `/api/upload` | Upload a file (PDF, image, audio, video) |
| POST | `/api/upload/text` | Submit raw text for parsing |
| POST | `/api/upload/url` | Submit a URL for extraction |
| POST | `/api/upload/youtube` | Submit a YouTube link |
| GET | `/api/graph` | Fetch the full knowledge graph |
| GET | `/api/node/{uuid}` | Get details of a specific node |
| DELETE | `/api/node/{uuid}` | Delete a node |
| POST | `/api/node/merge` | Merge multiple nodes into one |
| POST | `/api/node/{uuid}/recommend` | Get AI recommendations for a node |
| POST | `/api/node/confirm` | Confirm and save a recommended node |
| POST | `/api/graph/relink` | Re-evaluate all graph connections |

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `aws.region` | AWS region (e.g., `us-east-1`) |
| `aws.s3.bucket` | S3 bucket name for file storage |
| `aws.bedrock.modelId` | Bedrock LLM model ID |
| `aws.bedrock.embeddingModelId` | Bedrock embedding model ID |
| `spring.neo4j.uri` | Neo4j connection URI |
| `spring.neo4j.authentication.username` | Neo4j username |
| `spring.neo4j.authentication.password` | Neo4j password |
| `google.api.key` | Google API key (search + YouTube) |
| `google.customsearch.cx` | Google Custom Search engine ID |

---

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for full ECS Fargate deployment instructions.  
See [AWS_SETUP_GUIDE.md](AWS_SETUP_GUIDE.md) for complete AWS service configuration (steps 1–12).

---

## License

MIT
