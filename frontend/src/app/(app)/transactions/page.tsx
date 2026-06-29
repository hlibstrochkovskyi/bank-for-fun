"use client";

import { useAccounts, useRecentActivity } from "@/lib/queries";
import { TransactionRow } from "@/components/money/transaction-row";
import { ErrorState } from "@/components/app/state";
import { Skeleton } from "@/components/ui/skeleton";

export default function TransactionsPage() {
  const { data: accounts, isLoading, error } = useAccounts();
  const { transactions, isLoading: activityLoading } = useRecentActivity(accounts, 100);

  return (
    <div className="space-y-8">
      <header>
        <p className="eyebrow">Activity</p>
        <h1 className="mt-1.5 text-3xl tracking-tight">Transactions</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Every posting across your accounts, newest first.
        </p>
      </header>

      {error ? (
        <ErrorState title="Couldn’t load transactions" message={error.message} />
      ) : (
      <div className="rounded-xl border bg-card px-5 shadow-[0_1px_4px_rgba(0,0,0,0.04)]">
        {isLoading || activityLoading ? (
          <ListSkeleton />
        ) : transactions.length === 0 ? (
          <p className="py-12 text-center text-sm text-muted-foreground">No activity yet.</p>
        ) : (
          <div className="divide-y divide-border">
            {transactions.map((tx) => (
              <TransactionRow key={`${tx.accountId}-${tx.entryId}`} tx={tx} />
            ))}
          </div>
        )}
      </div>
      )}
    </div>
  );
}

function ListSkeleton() {
  return (
    <div className="space-y-4 py-5">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="flex items-center gap-3">
          <Skeleton className="size-9 rounded-full" />
          <div className="flex-1 space-y-1.5">
            <Skeleton className="h-3.5 w-40" />
            <Skeleton className="h-3 w-24" />
          </div>
          <Skeleton className="h-4 w-16" />
        </div>
      ))}
    </div>
  );
}
