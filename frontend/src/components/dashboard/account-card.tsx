import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { accountMeta } from "@/lib/labels";
import { formatMoney } from "@/lib/format";
import type { Account } from "@/lib/schemas";

export function AccountCard({ account }: { account: Account }) {
  const { label, Icon } = accountMeta(account.type);

  return (
    <Link
      href={`/accounts/${account.id}`}
      className="group block rounded-lg border border-border bg-card p-5 transition-colors hover:border-primary/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <div className="flex items-center justify-between">
        <span className="inline-flex items-center gap-2 text-sm text-muted-foreground">
          <Icon className="size-4" aria-hidden />
          {label}
          <span className="font-mono text-xs">· {account.currency}</span>
        </span>
        <ArrowUpRight className="size-4 text-muted-foreground/40 transition-all group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:text-primary" />
      </div>
      <p className="mt-4 font-display text-3xl font-medium tracking-tight tabular-nums">
        {formatMoney(account.balance)}
      </p>
    </Link>
  );
}
