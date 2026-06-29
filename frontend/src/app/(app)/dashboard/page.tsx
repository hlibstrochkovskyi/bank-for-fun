"use client";

import Link from "next/link";
import { useSession } from "next-auth/react";
import { ArrowLeftRight, Sparkles, Wallet } from "lucide-react";
import { useAccounts, useRecentActivity } from "@/lib/queries";
import { spendingByCategory } from "@/lib/insights";
import { formatMoney } from "@/lib/format";
import { categoryLabel } from "@/lib/labels";
import { HeroBalance } from "@/components/dashboard/hero-balance";
import { QuickActions } from "@/components/dashboard/quick-actions";
import { CategoryBreakdown } from "@/components/dashboard/category-breakdown";
import { AccountCard } from "@/components/dashboard/account-card";
import { OpenAccountDialog } from "@/components/dashboard/open-account-dialog";
import { TransactionRow } from "@/components/money/transaction-row";
import { Skeleton } from "@/components/ui/skeleton";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function DashboardPage() {
  const { data: session } = useSession();
  const { data: accounts, isLoading } = useAccounts();
  const { transactions, isLoading: activityLoading } = useRecentActivity(accounts);

  if (isLoading) return <DashboardSkeleton />;

  const hasAccounts = (accounts?.length ?? 0) > 0;
  const currency = accounts?.[0]?.currency ?? "USD";
  const primaryAccountId =
    accounts?.find((a) => a.type === "CHECKING")?.id ?? accounts?.[0]?.id;
  const { slices, totalMinor: spentMinor } = spendingByCategory(transactions);
  const topSlice = slices[0];

  const firstName = (session?.user?.name ?? "").split(" ")[0];
  const greeting = `Good ${partOfDay()}${firstName ? `, ${firstName}` : ""}`;
  const today = new Date().toLocaleDateString("en-US", {
    weekday: "long",
    month: "long",
    day: "numeric",
  });

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-3xl tracking-tight">{greeting}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {today} · here’s where your money stands.
          </p>
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
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="lg:col-span-2">
              <HeroBalance
                accounts={accounts!}
                transactions={transactions}
                activityLoading={activityLoading}
              />
            </div>
            <aside className="space-y-3">
              <h2 className="eyebrow">Quick actions</h2>
              <QuickActions primaryAccountId={primaryAccountId} />
            </aside>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <div className="space-y-6 lg:col-span-2">
              <section className="space-y-3">
                <h2 className="eyebrow">Accounts</h2>
                <div className="grid gap-3 sm:grid-cols-2">
                  {accounts!.map((account) => (
                    <AccountCard key={account.id} account={account} />
                  ))}
                </div>
              </section>

              <section className="space-y-3">
                <div className="flex items-baseline justify-between">
                  <h2 className="eyebrow">Spending · last 30 days</h2>
                  <span className="font-mono text-sm tabular-nums text-muted-foreground">
                    {formatMoney({
                      amount: (spentMinor / 100).toFixed(2),
                      currency,
                      minorUnits: spentMinor,
                    })}
                  </span>
                </div>
                <div className="rounded-xl border bg-card p-5 shadow-[0_1px_4px_rgba(0,0,0,0.04)]">
                  <CategoryBreakdown slices={slices} totalMinor={spentMinor} currency={currency} />
                </div>
              </section>
            </div>

            <div className="space-y-6">
              <section className="space-y-3">
                <div className="flex items-baseline justify-between">
                  <h2 className="eyebrow">Recent activity</h2>
                  <Link
                    href="/transactions"
                    className="text-xs text-muted-foreground underline-offset-2 hover:text-foreground hover:underline"
                  >
                    View all
                  </Link>
                </div>
                <div className="rounded-xl border bg-card px-5 shadow-[0_1px_4px_rgba(0,0,0,0.04)]">
                  {activityLoading ? (
                    <ActivitySkeleton />
                  ) : transactions.length === 0 ? (
                    <p className="py-10 text-center text-sm text-muted-foreground">
                      No activity yet.
                    </p>
                  ) : (
                    <div className="divide-y divide-border">
                      {transactions.slice(0, 6).map((tx) => (
                        <TransactionRow key={tx.entryId} tx={tx} />
                      ))}
                    </div>
                  )}
                </div>
              </section>

              {topSlice && (
                <div className="rounded-xl border border-clay/30 bg-clay/5 p-5">
                  <p className="eyebrow flex items-center gap-1.5 !text-clay">
                    <Sparkles className="size-3.5" /> Insight
                  </p>
                  <p className="mt-2 text-sm">
                    Your biggest category was{" "}
                    <span className="font-medium">{categoryLabel(topSlice.category) ?? "Other"}</span>{" "}
                    at{" "}
                    <span className="font-mono tabular-nums">
                      {formatMoney({
                        amount: (topSlice.minor / 100).toFixed(2),
                        currency,
                        minorUnits: topSlice.minor,
                      })}
                    </span>{" "}
                    — {topSlice.pct.toFixed(0)}% of the last 30 days.
                  </p>
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function partOfDay() {
  const h = new Date().getHours();
  return h < 12 ? "morning" : h < 18 ? "afternoon" : "evening";
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
      <Skeleton className="h-8 w-56" />
      <div className="grid gap-6 lg:grid-cols-3">
        <Skeleton className="h-72 rounded-2xl lg:col-span-2" />
        <Skeleton className="h-72 rounded-2xl" />
      </div>
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
