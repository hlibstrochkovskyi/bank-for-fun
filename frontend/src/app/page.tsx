import Link from "next/link";
import { ArrowRight, ShieldCheck, Repeat, Scale } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { Logo } from "@/components/brand/logo";
import { cn } from "@/lib/utils";

const features = [
  {
    icon: Scale,
    title: "Double-entry ledger",
    body: "Every cent is an immutable, balanced posting. Balances are derived, never overwritten.",
  },
  {
    icon: Repeat,
    title: "Idempotent money moves",
    body: "Retries never double-charge. Each transfer carries an idempotency key end to end.",
  },
  {
    icon: ShieldCheck,
    title: "Real-time fraud checks",
    body: "Suspicious transfers are held for review by a dedicated risk engine before they post.",
  },
];

export default function Home() {
  return (
    <div className="flex min-h-full flex-col">
      <header className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-5">
        <Logo />
        <Link href="/dashboard" className={cn(buttonVariants({ size: "sm" }))}>
          Sign in
          <ArrowRight className="size-4" />
        </Link>
      </header>

      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col px-6">
        <section className="flex flex-col items-center py-20 text-center sm:py-28">
          <span className="rounded-full border bg-card px-3 py-1 text-xs font-medium text-muted-foreground shadow-sm">
            A simulated retail bank, built correctly
          </span>
          <h1 className="mt-6 max-w-3xl text-balance text-4xl font-semibold tracking-tight sm:text-6xl">
            Banking on a ledger that{" "}
            <span className="text-primary">actually balances</span>.
          </h1>
          <p className="mt-6 max-w-xl text-balance text-lg text-muted-foreground">
            Open accounts, move money, and watch every transaction settle on an
            immutable double-entry ledger — with idempotency, fraud holds, and
            full traceability.
          </p>
          <div className="mt-9 flex flex-col items-center gap-3 sm:flex-row">
            <Link href="/dashboard" className={cn(buttonVariants({ size: "lg" }))}>
              Get started
              <ArrowRight className="size-4" />
            </Link>
            <Link
              href="/dashboard"
              className={cn(buttonVariants({ variant: "ghost", size: "lg" }))}
            >
              View dashboard
            </Link>
          </div>
        </section>

        <section className="grid gap-4 pb-24 sm:grid-cols-3">
          {features.map(({ icon: Icon, title, body }) => (
            <div
              key={title}
              className="rounded-xl border bg-card p-6 shadow-sm transition-shadow hover:shadow-md"
            >
              <span className="grid size-10 place-items-center rounded-lg bg-accent text-accent-foreground">
                <Icon className="size-5" aria-hidden />
              </span>
              <h3 className="mt-4 font-semibold">{title}</h3>
              <p className="mt-1.5 text-sm text-muted-foreground">{body}</p>
            </div>
          ))}
        </section>
      </main>

      <footer className="mx-auto w-full max-w-6xl px-6 py-8 text-sm text-muted-foreground">
        <div className="flex items-center justify-between border-t pt-6">
          <Logo showWordmark={false} />
          <span>A learning &amp; portfolio project — not a real bank.</span>
        </div>
      </footer>
    </div>
  );
}
