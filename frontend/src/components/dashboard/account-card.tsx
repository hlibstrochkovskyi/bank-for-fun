import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { accountMeta, accountName } from "@/lib/labels";
import { formatMoney, maskAccountNumber } from "@/lib/format";
import type { Account } from "@/lib/schemas";

export function AccountCard({ account }: { account: Account }) {
  const { label, Icon } = accountMeta(account.type);
  const masked = maskAccountNumber(account.accountNumber);

  return (
    <Link
      href={`/accounts/${account.id}`}
      className="group block rounded-lg border border-border bg-card p-5 shadow-[0_1px_4px_rgba(0,0,0,0.04)] transition-colors hover:border-clay/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <div className="flex items-start justify-between">
        <div className="min-w-0">
          <span className="inline-flex items-center gap-2 text-sm font-medium">
            <Icon className="size-4 text-muted-foreground" aria-hidden />
            <span className="truncate">{accountName(account)}</span>
          </span>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {label}
            {masked && <span className="font-mono"> · {masked}</span>}
            <span className="font-mono"> · {account.currency}</span>
          </p>
        </div>
        <ArrowUpRight className="size-4 shrink-0 text-muted-foreground/40 transition-all group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:text-clay" />
      </div>
      <p className="mt-4 font-display text-3xl tracking-tight tabular-nums">
        {formatMoney(account.balance)}
      </p>
    </Link>
  );
}
