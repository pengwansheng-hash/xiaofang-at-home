# xiaofang care service

`care-service` is the first-stage backend scaffold for `xiaofang-at-home`.

Current scope:
- health check
- family binding read/write
- senior profile read/update
- care plan read/create/update
- care plan event write
- AI runtime status
- companion reply generation
- topic brief generation

Storage strategy for the current phase:
- single-process TypeScript HTTP service
- local JSON file storage
- easy to replace with a real database later

Recommended next database:
- SQLite as the default lightweight free option for the current MVP stage
- upgrade to PostgreSQL only after multi-device concurrency or hosted deployment becomes necessary

Run locally after installing dependencies:

```bash
npm install
npm run dev
```

Default URL:

```text
http://127.0.0.1:3301/api/health
```

Optional AI environment variables:

```text
AI_BASE_URL=https://your-openai-compatible-endpoint/v1
AI_API_KEY=your_api_key
AI_MODEL=your_model_name
AI_TIMEOUT_MS=20000
```

You can also put the same values into a local `care-service/.env.local` file.

AI debug endpoints:

```text
GET  /api/ai/runtime
POST /api/ai/companion-reply
POST /api/ai/topic-briefs/generate
```
