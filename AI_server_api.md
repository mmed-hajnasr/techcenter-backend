# techcenter_agents

FastAPI service that generates a professional researcher biography from a list of publications using Pydantic AI + Mistral.

## Setup

```bash
# Install dependencies (uses uv, NOT pip)
uv sync --locked

# Start Jaeger (distributed tracing – optional for local dev)
docker compose up -d

# Run locally
uv run -m techcenter_agents
```

The API will be available at `http://localhost:8001`.
Interactive docs: `http://localhost:8001/docs`

---

## Endpoints

### `POST /biography`

Generates a researcher biography from a list of publications. Each publication requires a **title** and a **resume** (short abstract).

#### Request body

```json
{
  "researcher_name": "Jane Doe",
  "publications": [
    {
      "title": "Deep Learning for Medical Image Segmentation",
      "resume": "We propose a U-Net variant with attention gates for automated segmentation of MRI scans, achieving state-of-the-art results on three public benchmarks."
    },
    {
      "title": "Transfer Learning in Low-Resource NLP Settings",
      "resume": "This paper examines the effectiveness of large pre-trained language models when fine-tuned on small domain-specific corpora, with experiments on clinical text."
    }
  ],
  "tokens_limit": 4000
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `researcher_name` | `string` | ❌ | Researcher's name (used directly in the bio; defaults to 'The Researcher') |
| `publications` | `Publication[]` | ✅ | List of publications (at least one) |
| `publications[].title` | `string` | ✅ | Publication title |
| `publications[].resume` | `string` | ✅ | Short abstract / summary |
| `tokens_limit` | `integer` | ❌ | Max tokens to spend (default: 4000) |

#### Success response (`200`)

```json
{
  "type": "biography",
  "payload": {
    "name": "The Researcher",
    "biography": "The researcher is a prolific scientist whose work spans...",
    "research_areas": ["Medical Imaging", "Deep Learning", "Natural Language Processing"]
  },
  "tokens_used": 512
}
```

#### Error response – token limit exceeded

```json
{
  "type": "error",
  "message": "Token limit exceeded. Try increasing tokens_limit or reducing the number of publications.",
  "tokens_used": 4000
}
```

---

## Usage with `curl`

### Minimal request (two publications)

```bash
curl -X POST http://localhost:8001/biography \
  -H "Content-Type: application/json" \
  -d '{
    "researcher_name": "Jane Doe",
    "publications": [
      {
        "title": "Deep Learning for Medical Image Segmentation",
        "resume": "We propose a U-Net variant with attention gates for automated segmentation of MRI scans, achieving state-of-the-art results on three public benchmarks."
      },
      {
        "title": "Transfer Learning in Low-Resource NLP Settings",
        "resume": "This paper examines the effectiveness of large pre-trained language models when fine-tuned on small domain-specific corpora, with experiments on clinical text."
      }
    ]
  }'
```

### Request with a custom token limit

```bash
curl -X POST http://localhost:8001/biography \
  -H "Content-Type: application/json" \
  -d '{
    "publications": [
      {
        "title": "Graph Neural Networks for Drug Discovery",
        "resume": "We model molecular interactions as graphs and apply message-passing neural networks to predict binding affinity with high accuracy."
      }
    ],
    "tokens_limit": 2000
  }'
```

### Pretty-print the response

```bash
curl -s -X POST http://localhost:8001/biography \
  -H "Content-Type: application/json" \
  -d '{
    "publications": [
      {
        "title": "Explainability in Reinforcement Learning",
        "resume": "A survey of post-hoc explanation techniques applied to policy networks in continuous control tasks."
      }
    ]
  }' | python3 -m json.tool
```

---

### `POST /paper`

Extracts the **title** and a concise **resume** from a research paper PDF accessible via a public URL.

#### Request body

```json
{
  "pdf_url": "https://arxiv.org/pdf/1706.03762",
  "tokens_limit": 4000
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `pdf_url` | `string (URL)` | ✅ | Publicly accessible URL to the PDF |
| `tokens_limit` | `integer` | ❌ | Max tokens to spend (default: 4000) |

#### Success response (`200`)

```json
{
  "type": "paper",
  "payload": {
    "title": "Attention Is All You Need",
    "resume": "This paper proposes the Transformer, a novel neural network architecture based solely on attention mechanisms, dispensing with recurrence and convolutions..."
  },
  "tokens_used": 731
}
```

#### Error responses

```json
{ "type": "error", "message": "Could not download PDF: ...", "tokens_used": 0 }
{ "type": "error", "message": "No readable text found in the PDF (may be scanned/image-based).", "tokens_used": 0 }
{ "type": "error", "message": "Token limit exceeded. Try increasing tokens_limit.", "tokens_used": 4000 }
```

## Usage with `curl` – paper endpoint

### Basic request

```bash
curl -X POST http://localhost:8001/paper \
  -H "Content-Type: application/json" \
  -d '{
    "pdf_url": "https://arxiv.org/pdf/1706.03762"
  }'
```

### With a custom token limit

```bash
curl -X POST http://localhost:8001/paper \
  -H "Content-Type: application/json" \
  -d '{
    "pdf_url": "https://arxiv.org/pdf/1706.03762",
    "tokens_limit": 2000
  }'
```

### Pretty-print the response

```bash
curl -s -X POST http://localhost:8001/paper \
  -H "Content-Type: application/json" \
  -d '{
    "pdf_url": "https://arxiv.org/pdf/1706.03762"
  }' | python3 -m json.tool
```

---

### `POST /news-article`

Generates a **catchy headline** and a **250-400 word news article** in plain language from a publication's title, resume, and the researchers' biographies.

#### Request body

```json
{
  "title": "Attention Is All You Need",
  "resume": "We propose the Transformer, a model architecture based solely on attention mechanisms, achieving state-of-the-art results on machine translation tasks.",
  "researchers": [
    {
      "name": "Ashish Vaswani",
      "biography": "Ashish Vaswani is a research scientist specialising in deep learning and neural machine translation at Google Brain."
    },
    {
      "name": "Noam Shazeer",
      "biography": "Noam Shazeer is a senior research scientist at Google focused on large-scale neural network architectures."
    }
  ],
  "tokens_limit": 4000
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `title` | `string` | ✅ | Publication title |
| `resume` | `string` | ✅ | Short abstract / summary of the paper |
| `researchers` | `Researcher[]` | ✅ | List of researchers (at least one) |
| `researchers[].name` | `string` | ✅ | Researcher's full name |
| `researchers[].biography` | `string` | ✅ | Short biography of the researcher |
| `tokens_limit` | `integer` | ❌ | Max tokens to spend (default: 4000) |

#### Success response (`200`)

```json
{
  "type": "news_article",
  "payload": {
    "headline": "The AI That Ditched the Rule Book – And Changed Language Forever",
    "article": "Imagine teaching a machine to translate between languages not by feeding it grammar rules, but by letting it figure out which words matter most..."
  },
  "tokens_used": 842
}
```

## Usage with `curl` – news article endpoint

### Basic request

```bash
curl -X POST http://localhost:8001/news-article \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Attention Is All You Need",
    "resume": "We propose the Transformer, a model based solely on attention mechanisms, achieving state-of-the-art results on machine translation tasks.",
    "researchers": [
      {
        "name": "Ashish Vaswani",
        "biography": "Research scientist specialising in deep learning and neural machine translation at Google Brain."
      }
    ]
  }'
```

### Pretty-print the response

```bash
curl -s -X POST http://localhost:8001/news-article \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Attention Is All You Need",
    "resume": "We propose the Transformer, a model based solely on attention mechanisms, achieving state-of-the-art results on machine translation tasks.",
    "researchers": [
      {
        "name": "Ashish Vaswani",
        "biography": "Research scientist specialising in deep learning and neural machine translation at Google Brain."
      }
    ]
  }' | python3 -m json.tool
```

---

## Health check

```bash
curl http://localhost:8001/health
```

---

## Environment variables

All settings use the `AGENTS_` prefix:

| Variable | Description |
|---|---|
| `AGENTS_MISTRAL_API_KEY` | Mistral API key (required) |
| `AGENTS_LANGFUSE_PUBLIC_KEY` | Langfuse public key (optional, for tracing) |
| `AGENTS_LANGFUSE_SECRET_KEY` | Langfuse secret key (optional, for tracing) |
```
