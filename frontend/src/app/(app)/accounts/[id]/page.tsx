"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useAccount, useTransactions } from "@/lib/queries";
import { accountMeta, accountName } from "@/lib/labels";
import { formatMoney, maskAccountNumber } from "@/lib/format";
import { AccountMoneyDialog } from "@/components/account/account-money-dialog";
import { StatementView } from "@/components/account/statement-view";
import { TransactionRow } from "@/components/money/transaction-row";
import { ErrorState } from "@/components/app/state";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";

export default function AccountDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const { data: account, isLoading, error } = useAccount(id);
  const { data: txns, isLoading: txLoading, error: txError } = useTransactions(id, 100);

  if (isLoading) return <Skeleton className="h-40 w-full rounded-2xl" />;
  if (error || !account)
    return (
      <ErrorState
        title="Account unavailable"
        message="This account couldn’t be found, or you don’t have access to it."
      />
    );

  const { label, Icon } = accountMeta(account.type);

  return (
    <div className="space-y-6">
      <Link
        href="/dashboard"
        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Dashboard
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-4 rounded-2xl border bg-card p-6 shadow-[0_1px_4px_rgba(0,0,0,0.04)] sm:p-8">
        <div className="flex items-center gap-4">
          <span className="grid size-12 place-items-center rounded-xl bg-accent text-accent-foreground">
            <Icon className="size-6" aria-hidden />
          </span>
          <div>
            <p className="text-lg font-medium">{accountName(account)}</p>
            <p className="text-xs text-muted-foreground">
              {label}
              {account.accountNumber && (
                <span className="font-mono"> · {maskAccountNumber(account.accountNumber)}</span>
              )}
              <span className="font-mono"> · {account.currency}</span>
            </p>
            <p className="mt-2 font-display text-4xl tracking-tight tabular-nums">
              {formatMoney(account.balance)}
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          <AccountMoneyDialog account={account} op="deposit" />
          <AccountMoneyDialog account={account} op="withdraw" />
        </div>
      </div>

      <Tabs defaultValue="activity">
        <TabsList>
          <TabsTrigger value="activity">Activity</TabsTrigger>
          <TabsTrigger value="statement">Statement</TabsTrigger>
        </TabsList>

        <TabsContent value="activity" className="mt-4">
          <div className="rounded-xl border bg-card px-5 shadow-sm">
            {txLoading ? (
              <div className="py-8">
                <Skeleton className="h-5 w-full" />
              </div>
            ) : txError ? (
              <p className="py-10 text-center text-sm text-destructive">
                Couldn’t load activity. Please try again.
              </p>
            ) : !txns?.length ? (
              <p className="py-10 text-center text-sm text-muted-foreground">
                No transactions yet.
              </p>
            ) : (
              <div className="divide-y divide-border">
                {txns.map((tx) => (
                  <TransactionRow key={tx.entryId} tx={tx} />
                ))}
              </div>
            )}
          </div>
        </TabsContent>

        <TabsContent value="statement" className="mt-4">
          <StatementView accountId={id} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
