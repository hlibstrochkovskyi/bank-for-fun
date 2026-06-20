import { signIn } from "@/auth";
import { Logo } from "@/components/brand/logo";
import { Button } from "@/components/ui/button";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ callbackUrl?: string }>;
}) {
  const { callbackUrl } = await searchParams;

  return (
    <div className="grid min-h-full place-items-center px-6 py-16">
      <div className="w-full max-w-sm rounded-2xl border bg-card p-8 text-center shadow-sm">
        <Logo className="justify-center" />
        <h1 className="mt-6 text-xl font-semibold tracking-tight">Welcome back</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          Sign in to access your accounts.
        </p>

        <form
          className="mt-6"
          action={async () => {
            "use server";
            await signIn("keycloak", { redirectTo: callbackUrl || "/dashboard" });
          }}
        >
          <Button type="submit" size="lg" className="w-full">
            Continue with Keycloak
          </Button>
        </form>

        <p className="mt-5 text-xs text-muted-foreground">
          Demo users <span className="font-medium text-foreground">alice</span> or{" "}
          <span className="font-medium text-foreground">bob</span> — password{" "}
          <span className="font-medium text-foreground">password</span>.
        </p>
      </div>
    </div>
  );
}
