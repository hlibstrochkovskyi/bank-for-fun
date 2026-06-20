import NextAuth from "next-auth";
import Keycloak from "next-auth/providers/keycloak";
import type { JWT } from "next-auth/jwt";

/** Refresh the access token with Keycloak when it has expired. */
async function refreshAccessToken(token: JWT): Promise<JWT> {
  try {
    const response = await fetch(
      `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`,
      {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          grant_type: "refresh_token",
          client_id: process.env.KEYCLOAK_CLIENT_ID!,
          client_secret: process.env.KEYCLOAK_CLIENT_SECRET!,
          refresh_token: token.refreshToken!,
        }),
      },
    );
    const refreshed = await response.json();
    if (!response.ok) throw refreshed;
    return {
      ...token,
      accessToken: refreshed.access_token,
      expiresAt: Math.floor(Date.now() / 1000) + refreshed.expires_in,
      refreshToken: refreshed.refresh_token ?? token.refreshToken,
      error: undefined,
    };
  } catch {
    return { ...token, error: "RefreshAccessTokenError" };
  }
}

const PROTECTED_PREFIXES = [
  "/dashboard",
  "/accounts",
  "/transfers",
  "/statements",
  "/held-transfers",
];

export const { handlers, signIn, signOut, auth } = NextAuth({
  trustHost: true,
  pages: { signIn: "/login" },
  providers: [
    Keycloak({
      issuer: process.env.KEYCLOAK_ISSUER,
      clientId: process.env.KEYCLOAK_CLIENT_ID,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET,
    }),
  ],
  callbacks: {
    authorized({ auth, request }) {
      const isProtected = PROTECTED_PREFIXES.some((p) =>
        request.nextUrl.pathname.startsWith(p),
      );
      return isProtected ? !!auth?.user : true;
    },
    async jwt({ token, account, profile }) {
      // Initial sign-in: capture the tokens from Keycloak.
      if (account) {
        return {
          ...token,
          accessToken: account.access_token,
          refreshToken: account.refresh_token,
          expiresAt: account.expires_at,
          name: (profile?.name as string) ?? token.name,
          email: (profile?.email as string) ?? token.email,
        };
      }
      // Still valid (with a 30s safety margin)?
      if (token.expiresAt && Date.now() < token.expiresAt * 1000 - 30_000) {
        return token;
      }
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken;
      session.error = token.error;
      return session;
    },
  },
});
