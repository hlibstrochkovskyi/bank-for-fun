import type { ZodType } from "zod";

/** An error from the bank API, carrying the HTTP status and the RFC 7807 detail. */
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public problem?: { title?: string; detail?: string; status?: number },
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(
  path: string,
  schema: ZodType<T>,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(`/api/bank/${path}`, {
    ...init,
    headers: { "content-type": "application/json", ...init?.headers },
  });

  const text = await response.text();
  const json = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message =
      json?.title ?? json?.detail ?? response.statusText ?? "Request failed";
    throw new ApiError(response.status, message, json);
  }
  return schema.parse(json);
}

export const api = {
  get: <T>(path: string, schema: ZodType<T>) =>
    request(path, schema, { method: "GET" }),

  post: <T>(
    path: string,
    schema: ZodType<T>,
    body?: unknown,
    idempotencyKey?: string,
  ) =>
    request(path, schema, {
      method: "POST",
      body: body !== undefined ? JSON.stringify(body) : undefined,
      headers: idempotencyKey ? { "idempotency-key": idempotencyKey } : {},
    }),
};

/** A client-generated idempotency key for a money operation. */
export function newIdempotencyKey(): string {
  return crypto.randomUUID();
}
