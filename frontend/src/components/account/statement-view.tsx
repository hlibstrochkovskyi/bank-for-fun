"use client";

import { useState } from "react";
import { useStatement } from "@/lib/queries";
import { formatMoney } from "@/lib/format";
import { TransactionRow } from "@/components/money/transaction-row";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}

export function StatementView({ accountId }: { accountId: string }) {
  const [from, setFrom] = useState(isoDaysAgo(30));
  const [to, setTo] = useState(isoDaysAgo(0));
  const { data, isLoading } = useStatement(accountId, from, to);

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="from">From</Label>
          <Input
            id="from"
            type="date"
            value={from}
            max={to}
            onChange={(e) => setFrom(e.target.value)}
            className="w-40"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="to">To</Label>
          <Input
            id="to"
            type="date"
            value={to}
            min={from}
            onChange={(e) => setTo(e.target.value)}
            className="w-40"
          />
        </div>
      </div>

      {isLoading || !data ? (
        <Skeleton className="h-40 w-full rounded-xl" />
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <Stat label="Opening" value={formatMoney(data.openingBalance)} />
            <Stat label="Closing" value={formatMoney(data.closingBalance)} />
            <Stat label="Money in" value={formatMoney(data.totalCredits)} tone="positive" />
            <Stat label="Money out" value={formatMoney(data.totalDebits)} />
          </div>

          <div className="rounded-xl border bg-card px-5 shadow-sm">
            {data.transactions.length === 0 ? (
              <p className="py-10 text-center text-sm text-muted-foreground">
                No transactions in this period.
              </p>
            ) : (
              <div className="divide-y divide-border">
                {data.transactions.map((tx) => (
                  <TransactionRow key={tx.entryId} tx={tx} />
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}

function Stat({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone?: "positive";
}) {
  return (
    <div className="rounded-xl border bg-card p-4 shadow-sm">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p
        className={cn(
          "mt-1 text-lg font-semibold tabular-nums",
          tone === "positive" && "text-emerald-600",
        )}
      >
        {value}
      </p>
    </div>
  );
}
