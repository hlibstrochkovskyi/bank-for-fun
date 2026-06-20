import Link from "next/link";
import { ChevronRight } from "lucide-react";
import { accountMeta } from "@/lib/labels";
import { formatMoney } from "@/lib/format";
import type { Account } from "@/lib/schemas";

export function AccountCard({ account }: { account: Account }) {
  const { label, Icon } = accountMeta(account.type);

  return (
    <Link
      href={`/accounts/${account.id}`}
      className="group block rounded-xl border bg-card p-5 shadow-sm transition-shadow hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <div className="flex items-center justify-between">
        <span className="grid size-9 place-items-center rounded-lg bg-accent text-accent-foreground">
          <Icon className="size-[18px]" aria-hidden />
        </span>
        <ChevronRight className="size-4 text-muted-foreground/50 transition-transform group-hover:translate-x-0.5" />
      </div>
      <p className="mt-4 text-sm text-muted-foreground">
        {label} · {account.currency}
      </p>
      <p className="mt-0.5 text-2xl font-semibold tracking-tight tabular-nums">
        {formatMoney(account.balance)}
      </p>
    </Link>
  );
}
