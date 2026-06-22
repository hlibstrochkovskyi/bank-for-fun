"use client";

import Link from "next/link";
import { ArrowLeftRight, Wallet } from "lucide-react";
import { useAccounts, useRecentActivity } from "@/lib/queries";
import { balanceTrend } from "@/lib/trend";
import { formatMoney } from "@/lib/format";
import { BalanceTrendChart } from "@/components/dashboard/balance-trend-chart";
import { AccountCard } from "@/components/dashboard/account-card";
import { OpenAccountDialog } from "@/components/dashboard/open-account-dialog";
import { TransactionRow } from "@/components/money/transaction-row";
import { Skeleton } from "@/components/ui/skeleton";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function DashboardPage() {
  const { data: accounts, isLoading } = useAccounts();
  const { transactions, isLoading: activityLoading } = useRecentActivity(accounts);

  if (isLoading) return <DashboardSkeleton />;

  const hasAccounts = (accounts?.length ?? 0) > 0;
  const totalMinor = (accounts ?? []).reduce(
    (sum, a) => sum + a.balance.minorUnits,
    0,
  );
  const currency = accounts?.[0]?.currency ?? "USD";
  const total = {
    amount: (totalMinor / 100).toFixed(2),
    currency,
    minorUnits: totalMinor,
  };
  const trend = balanceTrend(transactions, totalMinor);

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Overview</h1>
          <p className="text-sm text-muted-foreground">Your accounts at a glance.</p>
        </div>
        {hasAccounts && (
          <div className="flex gap-2">
            <Link
              href="/transfers"
              className={cn(buttonVariants({ variant: "outline", size: "sm" }))}
            >
              <ArrowLeftRight className="size-4" />
              Transfer
            </Link>
            <OpenAccountDialog />
          </div>
        )}
      </header>

      {!hasAccounts ? (
        <EmptyAccounts />
      ) : (
        <>
          <div className="rounded-xl border border-border bg-card p-6">
            <p className="font-mono text-xs uppercase tracking-widest text-muted-foreground">
              Total balance
            </p>
            <p className="mt-2 font-display text-5xl font-medium tracking-tight tabular-nums">
              {formatMoney(total)}
            </p>
            <div className="mt-5">
              {activityLoading ? (
                <Skeleton className="h-[200px] w-full rounded-lg" />
              ) : (
                <BalanceTrendChart data={trend} />
              )}
            </div>
          </div>

          <div className="grid gap-6 lg:grid-cols-5">
            <section className="space-y-3 lg:col-span-3">
              <h2 className="text-sm font-medium text-muted-foreground">Accounts</h2>
              <div className="grid gap-3 sm:grid-cols-2">
                {accounts!.map((account) => (
                  <AccountCard key={account.id} account={account} />
                ))}
              </div>
            </section>

            <section className="space-y-3 lg:col-span-2">
              <h2 className="text-sm font-medium text-muted-foreground">
                Recent activity
              </h2>
              <div className="rounded-xl border bg-card px-5 shadow-sm">
                {activityLoading ? (
                  <ActivitySkeleton />
                ) : transactions.length === 0 ? (
                  <p className="py-10 text-center text-sm text-muted-foreground">
                    No activity yet.
                  </p>
                ) : (
                  <div className="divide-y divide-border">
                    {transactions.slice(0, 8).map((tx) => (
                      <TransactionRow key={tx.entryId} tx={tx} />
                    ))}
                  </div>
                )}
              </div>
            </section>
          </div>
        </>
      )}
    </div>
  );
}

function EmptyAccounts() {
  return (
    <div className="grid place-items-center rounded-2xl border border-dashed bg-card/50 px-6 py-20 text-center">
      <span className="grid size-12 place-items-center rounded-xl bg-accent text-accent-foreground">
        <Wallet className="size-6" aria-hidden />
      </span>
      <h2 className="mt-5 text-lg font-semibold">No accounts yet</h2>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">
        Open your first account to start depositing and moving money.
      </p>
      <div className="mt-6">
        <OpenAccountDialog />
      </div>
    </div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="space-y-8">
      <Skeleton className="h-8 w-40" />
      <Skeleton className="h-52 w-full rounded-2xl" />
      <div className="grid gap-3 sm:grid-cols-2">
        <Skeleton className="h-28 rounded-xl" />
        <Skeleton className="h-28 rounded-xl" />
      </div>
    </div>
  );
}

function ActivitySkeleton() {
  return (
    <div className="space-y-4 py-5">
      {Array.from({ length: 4 }).map((_, i) => (
        <div key={i} className="flex items-center gap-3">
          <Skeleton className="size-9 rounded-full" />
          <div className="flex-1 space-y-1.5">
            <Skeleton className="h-3.5 w-32" />
            <Skeleton className="h-3 w-20" />
          </div>
          <Skeleton className="h-4 w-16" />
        </div>
      ))}
    </div>
  );
}
