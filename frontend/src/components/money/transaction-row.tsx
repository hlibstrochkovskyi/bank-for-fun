import { cn } from "@/lib/utils";
import { formatDateTime, formatSignedMoney } from "@/lib/format";
import { transactionMeta } from "@/lib/labels";
import type { Transaction } from "@/lib/schemas";

export function TransactionRow({ tx }: { tx: Transaction }) {
  const { label, Icon } = transactionMeta(tx.type);
  const isCredit = tx.amount.minorUnits > 0;

  return (
    <div className="flex items-center gap-3 py-3.5">
      <span
        className={cn(
          "grid size-9 shrink-0 place-items-center rounded-md border",
          isCredit
            ? "border-transparent bg-accent text-accent-foreground"
            : "border-border bg-secondary text-muted-foreground",
        )}
      >
        <Icon className="size-4" aria-hidden />
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">{tx.description || label}</p>
        <p className="truncate text-xs text-muted-foreground">
          {label} · {formatDateTime(tx.createdAt)}
        </p>
      </div>
      <span
        className={cn(
          "shrink-0 font-mono text-sm font-medium tabular-nums",
          isCredit ? "text-positive" : "text-foreground",
        )}
      >
        {formatSignedMoney(tx.amount)}
      </span>
    </div>
  );
}
