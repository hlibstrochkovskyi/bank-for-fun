# syntax=docker/dockerfile:1
FROM python:3.12-slim AS runtime

# uv for fast, reproducible dependency installation.
COPY --from=ghcr.io/astral-sh/uv:latest /uv /bin/uv

ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy

WORKDIR /app

# Install dependencies from the lockfile (cached unless the lock changes).
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project

COPY app ./app

RUN useradd --system appuser && chown -R appuser /app
USER appuser

EXPOSE 8000
# opentelemetry-instrument auto-instruments FastAPI and exports OTLP (configured via
# OTEL_* env in docker-compose); it is a no-op if those vars are unset.
CMD [".venv/bin/opentelemetry-instrument", ".venv/bin/uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
