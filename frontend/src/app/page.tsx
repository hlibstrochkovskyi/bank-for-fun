import Link from "next/link";
import { ArrowRight, Check } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { Logo } from "@/components/brand/logo";
import { cn } from "@/lib/utils";

const principles = [
  {
    n: "01",
    title: "Double-entry, always",
    body: "Every cent is an immutable, balanced posting. Balances are derived from the ledger — never overwritten.",
  },
  {
    n: "02",
    title: "Idempotent by design",
    body: "Each money movement carries a key. Retry all you like; it settles exactly once.",
  },
  {
    n: "03",
    title: "Flagged, then held",
    body: "A risk engine reviews every transfer. Anything unusual is held before it ever posts.",
  },
];

export default function Home() {
  return (
    <div className="flex min-h-full flex-col">
      <header className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-6">
        <Logo />
        <Link href="/dashboard" className={cn(buttonVariants({ size: "sm" }))}>
          Sign in
          <ArrowRight className="size-4" />
        </Link>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-6">
        <section className="grid items-center gap-12 py-16 lg:grid-cols-[1.1fr_0.9fr] lg:py-24">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-xs text-muted-foreground">
              <span className="size-1.5 rounded-full bg-primary" />
              A simulated retail bank, built correctly
            </span>
            <h1 className="mt-6 font-display text-5xl font-medium leading-[1.04] tracking-tight text-foreground sm:text-6xl">
              A bank on books
              <br />
              that truly{" "}
              <span className="text-primary italic">balance</span>.
            </h1>
            <p className="mt-6 max-w-md text-lg leading-relaxed text-muted-foreground">
              Open accounts, move money, and watch every transaction settle on an
              immutable double-entry ledger — with idempotency, fraud holds, and
              full traceability.
            </p>
            <div className="mt-9 flex flex-col gap-3 sm:flex-row">
              <Link href="/dashboard" className={cn(buttonVariants({ size: "lg" }))}>
                Get started
                <ArrowRight className="size-4" />
              </Link>
              <Link
                href="/dashboard"
                className={cn(buttonVariants({ variant: "outline", size: "lg" }))}
              >
                View dashboard
              </Link>
            </div>
          </div>

          <LedgerCard />
        </section>

        <div className="ledger-rule" />

        <section className="grid gap-10 py-16 sm:grid-cols-3">
          {principles.map((p) => (
            <div key={p.n}>
              <p className="font-display text-3xl text-primary/40">{p.n}</p>
              <h3 className="mt-3 text-base font-semibold">{p.title}</h3>
              <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">
                {p.body}
              </p>
            </div>
          ))}
        </section>
      </main>

      <footer className="mx-auto w-full max-w-6xl px-6 py-8">
        <div className="ledger-rule mb-6" />
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <Logo showWordmark={false} />
          <span>A learning &amp; portfolio project — not a real bank.</span>
        </div>
      </footer>
    </div>
  );
}

/** The memorable detail: a real double-entry posting that sums to zero. */
function LedgerCard() {
  return (
    <div className="rounded-xl border border-border bg-card p-6 shadow-[0_1px_0_oklch(0_0_0/0.03),0_12px_32px_-16px_oklch(0_0_0/0.18)]">
      <div className="flex items-center justify-between">
        <span className="font-mono text-xs uppercase tracking-widest text-muted-foreground">
          Posting · Transfer
        </span>
        <span className="font-mono text-xs text-muted-foreground">#000128</span>
      </div>

      <div className="my-5 space-y-3 font-mono text-sm">
        <Row label="Checking" amount="−$250.00" tone="debit" />
        <Row label="Savings" amount="+$250.00" tone="credit" />
      </div>

      <div className="ledger-rule" />

      <div className="mt-4 flex items-center justify-between">
        <span className="text-sm text-muted-foreground">Net change</span>
        <span className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1 rounded-full bg-accent px-2 py-0.5 text-xs font-medium text-accent-foreground">
            <Check className="size-3" /> balances
          </span>
          <span className="font-mono text-base font-semibold tabular-nums">$0.00</span>
        </span>
      </div>
    </div>
  );
}

function Row({
  label,
  amount,
  tone,
}: {
  label: string;
  amount: string;
  tone: "debit" | "credit";
}) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-foreground">{label}</span>
      <span
        className={cn(
          "tabular-nums",
          tone === "credit" ? "text-positive" : "text-destructive",
        )}
      >
        {amount}
      </span>
    </div>
  );
}
