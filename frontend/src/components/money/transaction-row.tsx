import { cn } from "@/lib/utils";
import { formatRelativeDate, formatSignedMoney } from "@/lib/format";
import { categoryLabel, merchantInitial, transactionMeta } from "@/lib/labels";
import type { Transaction } from "@/lib/schemas";

export function TransactionRow({ tx }: { tx: Transaction }) {
  const { label } = transactionMeta(tx.type);
  const isCredit = tx.amount.minorUnits > 0;
  const title = tx.merchant || tx.description || label;
  const category = categoryLabel(tx.category);

  return (
    <div className="flex items-center gap-3 py-3.5">
      <span
        className={cn(
          "grid size-9 shrink-0 place-items-center rounded-full text-xs font-semibold",
          isCredit ? "bg-accent text-accent-foreground" : "bg-secondary text-secondary-foreground",
        )}
        aria-hidden
      >
        {merchantInitial(tx.merchant || title)}
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">{title}</p>
        <p className="truncate text-xs text-muted-foreground">
          {category ? `${category} · ` : ""}
          {formatRelativeDate(tx.createdAt)}
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
