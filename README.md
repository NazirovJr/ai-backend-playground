# AI Backend Playground — Production-Grade RAG Service

A Spring Boot service demonstrating **production-grade Retrieval-Augmented Generation (RAG)** and modern LLM integration patterns on the JVM — built with **Spring Boot 4.1** and **Spring AI 2.0**, running entirely on **local models** via Ollama.

The service answers questions strictly from a private knowledge base, cites its sources, and refuses to answer when the knowledge base has no relevant information.

---

## Why this project

Most RAG demos stop at "embed a document, ask a question". This one covers the parts that actually break in production:

- **Retrieval quality** — similarity thresholds, chunking strategy, metadata filtering
- **Grounding** — two independent layers of hallucination defence
- **Trust** — every answer returns the source passages it was built from
- **Cost & observability** — token accounting and Actuator/Micrometer metrics
- **Security** — prompt-injection–resistant prompts and injection-safe metadata filters

---

## Tech stack

| Layer | Choice |
|---|---|
| Runtime | Java 21, Spring Boot 4.1.0 |
| AI framework | Spring AI 2.0.0 |
| Chat model | `gemma4` (local, via Ollama) |
| Embedding model | `bge-m3` — 1024-dim, multilingual |
| Vector store | PostgreSQL + `pgvector` (HNSW, cosine) |
| Observability | Spring Boot Actuator + Micrometer |
| Build | Maven |

---

## Architecture

```
          ┌──────────────┐
Client ──▶│ Spring Boot  │
          │  ChatClient  │──▶ Ollama ── gemma4        (generation)
          │  + Advisors  │──▶ Ollama ── bge-m3        (embeddings)
          └──────┬───────┘
                 │
                 ▼
        PostgreSQL + pgvector      (vector_store, 1024-dim, HNSW/cosine)
```

**Ingestion pipeline:** `DocumentReader` → `TokenTextSplitter` (chunking) → `EmbeddingModel` → `VectorStore`

**Query pipeline:** question → embed → vector search (threshold + top-K) → guard → prompt assembly with numbered sources → generation → answer + citations

---

## Getting started

### Prerequisites
- Java 21
- Docker
- [Ollama](https://ollama.com) with the required models:

```bash
ollama pull gemma4
ollama pull bge-m3
```

### Run

```bash
# 1. Start PostgreSQL with pgvector
docker run --name pgvector -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d pgvector/pgvector:pg17

# 2. Start the application (schema is created automatically)
./mvnw spring-boot:run

# 3. Seed the knowledge base
curl "http://localhost:8081/vectors/seed"
```

---

## API

### RAG
| Endpoint | Description |
|---|---|
| `GET /rag/ask?question=` | RAG via Spring AI `QuestionAnswerAdvisor` |
| `GET /rag/cited?question=` | Manual RAG with hard guard + source citations |

### Vector store
| Endpoint | Description |
|---|---|
| `GET /vectors/seed` | Ingest knowledge base (read → chunk → embed → store) |
| `GET /vectors/search?query=` | Semantic search with similarity threshold |
| `GET /vectors/search-filtered?query=&category=` | Semantic search scoped by metadata |

### LLM fundamentals
| Endpoint | Description |
|---|---|
| `GET /chat?message=` | Chat with a system persona |
| `GET /chat/stream?message=` | Streaming responses (SSE) |
| `GET /structured/classify?message=` | Structured output into a typed record |
| `GET /tools/ask?message=` | Tool / function calling |
| `GET /memory/chat?message=&conversationId=` | Conversation memory, isolated per conversation |
| `GET /metrics-demo/ask?message=` | Token usage and cost estimation |
| `GET /embeddings/similarity?a=&b=` | Cosine similarity between two texts |

---

## Design decisions

### 1. Embedding model chosen by measurement, not by popularity

The initial choice (`nomic-embed-text`) was benchmarked against the actual target language (Russian) and **failed**:

| Query pair | nomic-embed-text | bge-m3 |
|---|---|---|
| Semantically related | 0.68 | **0.72** |
| Unrelated | **0.71** ❌ | **0.40** ✅ |

With `nomic-embed-text`, an *unrelated* pair scored **higher** than a semantically related one — retrieval would have been unusable. English pairs separated cleanly (0.32 vs 0.73), which isolated the cause: the model is English-centric. Switching to the multilingual `bge-m3` fixed the inversion and additionally enabled **cross-lingual retrieval** (an English query retrieves Russian documents at 0.81 similarity).

**Takeaway:** embedding quality is language-dependent — always evaluate on your own data.

### 2. Two independent hallucination guards

| Guard | Mechanism | Trade-off |
|---|---|---|
| **Hard** | If retrieval returns nothing above the threshold, return a fixed response — **the LLM is never called** | Deterministic, zero cost, cannot be talked around |
| **Soft** | System prompt: answer only from the provided context | Handles "retrieved something, but it doesn't answer the question" |

They cover different failure modes: the hard guard blocks out-of-domain questions cheaply; the soft guard catches partially-relevant retrievals.

### 3. Similarity threshold is mandatory

`topK` always returns K results — even for nonsense queries. Without a `similarityThreshold`, irrelevant chunks reach the LLM and become hallucinations. Thresholds are model-specific and must be calibrated per embedding model.

### 4. Injection-safe metadata filtering

Metadata filters use the typed `FilterExpressionBuilder` rather than string concatenation, eliminating filter-injection by construction — the same reasoning as parameterised SQL.

### 5. Tool outputs are curated, not raw

An early version returned a full weather API JSON payload to the model, which overwhelmed the local model and degraded answers. Tool responses are now compact: every token returned by a tool enters the context window and costs money.

---

## Roadmap

- [ ] Reranking (cross-encoder) for higher retrieval precision
- [ ] Idempotent ingestion via deterministic document IDs
- [ ] RAG evaluation harness (groundedness / relevance scoring)
- [ ] MCP server exposing the knowledge base to external agents
- [ ] Containerisation and Kubernetes deployment
