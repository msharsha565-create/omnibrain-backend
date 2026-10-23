# OmniBrain backend

[![CI](https://github.com/msharsha565-create/omnibrain-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/msharsha565-create/omnibrain-backend/actions)

A Java 21 / Spring Boot 3 backend prototype: JWT authentication with per-user usage limits in front of an AI chat endpoint, and a document-upload RAG pipeline. **It is a prototype.** The authentication, usage limits and persistence are real and tested; parts of the AI layer are placeholders, listed honestly below.

## What works

| Area | Status |
| --- | --- |
| Registration and login | Real. BCrypt-hashed passwords, validated input, constant-cost miss path for unknown users |
| JWT auth | Real. HS256, signing secret required from `JWT_SECRET` (the app refuses to start without 32+ bytes), expiry, forged/tampered/expired/`alg=none` tokens rejected |
| Authorization | Real. Everything except health and `/auth/**` needs a token; `/admin/**` needs a username listed in `ADMIN_USERS`. Checked against a real embedded server, so errors keep their real status (403/400, not a masked 401) |
| Usage limits | Real. Daily prompt cap per user by plan (FREE 5, PRO 100, ENTERPRISE unlimited), recorded in PostgreSQL |
| AI chat (`POST /api/ai/chat`) | With `OPENAI_API_KEY` set it calls an OpenAI-compatible API (`OPENAI_BASE_URL`, `OPENAI_MODEL`); otherwise it returns a canned "demo mode" reply. The live path has not been run against a real provider |
| **RAG** (`/api/rag/*`) | **Real**, described below |

## RAG pipeline

Upload `.pdf`, `.txt` or `.md` files (PDFs are read with PDFBox), then ask questions answered **only from your own documents**.

```
upload ─► clean ─► sentence-aware chunks ─► embed ─► store (per owner)
ask ────► embed question ─► cosine top-k (your chunks only) ─► grounded answer + citations + sources
```

- **Chunking**: sentence-aligned chunks of about 700 characters with one sentence of overlap (the earlier fixed 300-character slicer cut words in half).
- **Embeddings** (default `hashing-v1-2048`): a deterministic feature-hashing embedder over words and word pairs. No model download, no API key. It captures **vocabulary overlap, not meaning**: it will not connect "tides" with "the sea rises and falls". For real semantic search set `EMBEDDINGS_PROVIDER=openai` (any OpenAI-compatible `/embeddings` endpoint: OpenAI, Gemini's compatibility endpoint, Ollama, vLLM).
- **Answers** (default `extractive`): quotes the best-matching sentences from your documents with `[n]` citations. It cannot invent text: a passage is used only if it contains at least half of the question's content words, otherwise the reply is "I couldn't find that in your documents." Set `RAG_ANSWERER=llm` for a chat model that is told to answer only from the passages and to reply `NOT_FOUND` otherwise.
- **Isolation**: every chunk belongs to the user who uploaded it and retrieval is filtered by owner. Chunks also record which embedding model produced them and are only compared with vectors from the same model.
- **Endpoints**: `POST /api/rag/upload`, `POST /api/rag/ask` (JSON: answer, `answerable`, sources), `POST /api/rag/ask/stream` (SSE), `GET /api/rag/documents`, `DELETE /api/rag/documents?source=name`. Uploading a file with the same name replaces the earlier copy.

### Measured retrieval quality

A 15-passage, 15-question corpus (hand-written, unrelated topics, one correct passage per question; included as `RetrievalQualityTest`):

| Embedder | Precision@1 | MRR |
| --- | --- | --- |
| Previous implementation (pseudo-random vector from the text hash) | 0.07 (chance is 0.07) | 0.15 |
| `hashing-v1-2048` | **0.93** (14/15) | 0.94 |

The one miss is the deliberate paraphrase ("Why do the seas rise and fall twice daily?"), which is exactly the limit of a lexical embedder. This is a small corpus I wrote myself, and I adjusted the stemmer after seeing an earlier miss on it, so treat the numbers as a sanity check, not a benchmark.

Run against the app with the sample files in `samples/`: "What does OmniBrain allow users to do?" is answered from `sample.txt` and `sample_omnibrain.pdf` with citations, "What file types are supported?" quotes the FAQ PDF ("Text and PDF files."), and "Who won the football world cup in 2010?" returns "I couldn't find that in your documents."

## What is still a placeholder

- **`GovernanceEngine`**: a substring check for "pii", not PII detection. Used only by the demo `/chat` endpoint.
- **`/chat`**: an echo stub, kept as the demo of the governance hook.
- **Not run live**: the OpenAI-compatible chat path, `EMBEDDINGS_PROVIDER=openai` and `RAG_ANSWERER=llm`. They are tested against a local fake HTTP server and with scripted providers, not against a real provider.

## Run it

Requires Java 21 and PostgreSQL.

```bash
cp .env.example .env        # fill in JWT_SECRET and the database settings
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run
```

```bash
curl -X POST localhost:8080/auth/register -H 'Content-Type: application/json' \
     -d '{"username":"alice","password":"correct-horse-battery"}'
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
     -d '{"username":"alice","password":"correct-horse-battery"}' | jq -r .token)
curl -X POST localhost:8080/api/ai/chat -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' -d '{"prompt":"What is IAM?"}'
```

Docker: `docker build -t omnibrain . && docker run --env-file .env -p 8080:8080 omnibrain`.

## Tests

```bash
./mvnw verify      # 79 tests, in-memory H2, no PostgreSQL or API key needed
```

The tests cover registration and login, protected endpoints, forged and expired tokens, the admin role, usage limits, password hashing, chunking, tokenisation, the embedder, retrieval quality, grounded answers, per-user isolation, the optional embeddings client and LLM answerer (against a local fake server), upload validation, and error statuses on a real embedded server. I also checked that the key tests fail when the behaviour is deliberately broken (login ignoring the password, the admin area opened to all users, retrieval ignoring the owner, a single shared word counting as an answer, and the embedding-model check removed).

## Limitations

- No rate limiting or lockout on `/auth/login`.
- `spring.jpa.hibernate.ddl-auto=update`: fine for development, use migrations for production. Rows from older versions have no owner and are never returned; re-upload documents after upgrading.
- Retrieval scans a user's chunks in memory and keeps the top k: fine for thousands of chunks, not millions (a vector index would be next).
- Extractive answers quote sentences rather than synthesising an answer; use `RAG_ANSWERER=llm` for that.
- Uploads are limited to 5 MB and to `.pdf`, `.txt`, `.md`.
- CORS allows only `http://localhost:5173`.
- Earlier versions of this repository committed a JWT signing secret and a database password. They are removed from the code, but **they remain in git history**: treat them as compromised and rotate them.
