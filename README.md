# OmniBrain backend

[![CI](https://github.com/msharsha565-create/omnibrain-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/msharsha565-create/omnibrain-backend/actions)

A Java 21 / Spring Boot 3 backend prototype: JWT authentication with per-user usage limits in front of an AI chat endpoint, and a document-upload RAG pipeline. **It is a prototype.** The authentication, usage limits and persistence are real and tested; parts of the AI layer are placeholders, listed honestly below.

## What works

| Area | Status |
| --- | --- |
| Registration and login | Real. BCrypt-hashed passwords, validated input, constant-cost miss path for unknown users |
| JWT auth | Real. HS256, signing secret required from `JWT_SECRET` (the app refuses to start without 32+ bytes), expiry, forged/tampered/expired/`alg=none` tokens rejected |
| Authorization | Real. Everything except health and `/auth/**` needs a token; `/admin/**` needs a username listed in `ADMIN_USERS` |
| Usage limits | Real. Daily prompt cap per user by plan (FREE 5, PRO 100, ENTERPRISE unlimited), recorded in PostgreSQL |
| AI chat (`POST /api/ai/chat`) | Switches by configuration: with `OPENAI_API_KEY` set it calls OpenAI (`gpt-4o-mini`); otherwise it returns a canned "demo mode" reply. The OpenAI path has not been run live |
| Document upload (`POST /api/rag/upload`) | Real. Extracts text from PDF/plain text, chunks it (300 chars, 50 overlap), stores chunks in the database |
| Retrieval | Cosine similarity over stored vectors, top 3, streamed over SSE with sources |

## What is a placeholder

- **Embeddings** (`SimpleEmbedder`): a pseudo-random vector derived from the text's hash. It is deterministic but **not semantic**, so retrieval ranking and the "confidence" figure are effectively arbitrary until this is replaced with a real embedding model.
- **RAG answer** (`rag.SimpleLlmClient`): ignores the prompt and streams one fixed sentence.
- **`GovernanceEngine`**: a substring check for "pii", not PII detection. Used only by the demo `/chat` endpoint.
- **`/chat`**: an echo stub, kept as the demo of the governance hook.

If you want the RAG path to answer from your documents, the missing pieces are a real embedder and an LLM call in `RagService`.

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
./mvnw verify      # 23 tests, in-memory H2, no PostgreSQL or API key needed
```

The security tests cover registration validation, login, protected endpoints, forged and expired tokens, the admin role, usage limits and password hashing. I also checked that they fail when the security is deliberately broken (login ignoring the password; the admin area opened to all users).

## Limitations

- No rate limiting or lockout on `/auth/login`.
- `spring.jpa.hibernate.ddl-auto=update`: fine for development, use migrations for production.
- CORS allows only `http://localhost:5173`.
- Earlier versions of this repository committed a JWT signing secret and a database password. They are removed from the code, but **they remain in git history**: treat them as compromised and rotate them.
