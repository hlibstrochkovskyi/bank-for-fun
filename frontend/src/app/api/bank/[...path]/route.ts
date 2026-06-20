import { auth } from "@/auth";

const CORE_BANK_URL = process.env.CORE_BANK_URL ?? "http://localhost:8081";

/**
 * Backend-for-frontend proxy: forwards /api/bank/* to the core-bank API, attaching
 * the user's access token server-side. The browser never sees the bearer token.
 */
async function proxy(
  request: Request,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const session = await auth();
  if (!session?.accessToken) {
    return Response.json({ error: "unauthorized" }, { status: 401 });
  }

  const { path } = await params;
  const search = new URL(request.url).search;
  const target = `${CORE_BANK_URL}/api/${path.join("/")}${search}`;

  const headers = new Headers();
  headers.set("Authorization", `Bearer ${session.accessToken}`);
  const contentType = request.headers.get("content-type");
  if (contentType) headers.set("content-type", contentType);
  const idempotencyKey = request.headers.get("idempotency-key");
  if (idempotencyKey) headers.set("idempotency-key", idempotencyKey);

  const hasBody = request.method !== "GET" && request.method !== "HEAD";
  const body = hasBody ? await request.text() : undefined;

  const upstream = await fetch(target, {
    method: request.method,
    headers,
    body,
    cache: "no-store",
  });

  const payload = await upstream.text();
  return new Response(payload || null, {
    status: upstream.status,
    headers: {
      "content-type": upstream.headers.get("content-type") ?? "application/json",
    },
  });
}

export { proxy as GET, proxy as POST };
