# Continuum

A persistent memory layer between you and ChatGPT. Continuum keeps your goals, preferences, decisions, and tasks alive across sessions — so AI never forgets what matters to you.

## Architecture

```
┌──────────────────┐     ┌─────────────────────┐     ┌──────────────┐
│ Chrome Extension │────▶│  Spring Boot API     │────▶│  PostgreSQL  │
│ (ChatGPT overlay)│     │  :8080               │     └──────────────┘
└──────────────────┘     │                      │
                         │  Users / Workspaces  │     ┌──────────────┐
                         │  Memories / Prompts  │────▶│  NLU Service │
                         └─────────────────────┘     │  (FastAPI)   │
                                                      │  :8090       │
                                                      └──────────────┘
```

**Three components:**

| Component | Stack | Purpose |
|-----------|-------|---------|
| **Backend** | Spring Boot 3, Java 17, PostgreSQL | REST API for memory storage, context querying, and prompt generation |
| **NLU Service** | FastAPI, scikit-learn, sentence-transformers | Classifies memories into semantic types (GOAL, TASK, PREFERENCE, etc.) |
| **Extension** | Chrome Manifest v3 | Overlay button on ChatGPT — enhance prompts with context or save to memory |

## How It Works

1. You type a prompt in ChatGPT
2. Click the Continuum overlay button
3. Choose: **Enhance Prompt** (weave in stored context), **Save to Memory**, or both
4. Backend retrieves relevant memories, partitions by type, and builds a context-enriched prompt
5. Memories are auto-classified by the NLU service using a trained LogisticRegression model

### Memory Types

| Type | Example |
|------|---------|
| `PREFERENCE` | "I prefer concise, direct answers" |
| `GOAL` | "Building a SaaS product by Q3" |
| `TASK` | "Refactor the auth module" |
| `DECISION` | "Using PostgreSQL over MongoDB" |
| `FACT` | "Our API uses REST, not GraphQL" |
| `CONSTRAINT` | "Must support Java 17+" |

Preferences auto-supersede older entries on the same topic.

## Setup

### Prerequisites

- Java 17+
- PostgreSQL
- Python 3.10+

### 1. Database

```sql
CREATE DATABASE continuum;
```

### 2. Backend

```bash
cd backend
# set env vars: DB_PASSWORD
./mvnw spring-boot:run
```

Runs on `localhost:8080`.

### 3. NLU Service

```bash
cd nlu-service
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --port 8090
```

### 4. Chrome Extension

1. Open `chrome://extensions`
2. Enable Developer Mode
3. Load Unpacked → select the `extension/` directory
4. Navigate to ChatGPT — the overlay button appears

## API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/users` | POST/GET | Manage users |
| `/api/workspaces` | POST/GET | Manage workspaces |
| `/api/memories` | POST/GET | CRUD memories |
| `/api/ingestion/messages` | POST | Ingest raw text as a classified memory |
| `/api/context/query` | POST | Retrieve relevant memories for a query |
| `/api/prompts/generate` | POST | Build a context-enriched prompt |
