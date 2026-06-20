// Next.js 16 renamed the "middleware" convention to "proxy". Auth.js's `auth`
// wrapper enforces the `authorized` callback (route protection) here.
export { auth as proxy } from "@/auth";

export const config = {
  // Run on everything except Next internals, the auth routes, and static files.
  matcher: ["/((?!api/auth|_next/static|_next/image|favicon.ico|.*\\.svg).*)"],
};
