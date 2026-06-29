"use client";

import { useMemo, useState } from "react";
import { ArrowDownRight, ArrowUpRight } from "lucide-react";
import type { Account } from "@/lib/schemas";
import type { ActivityItem } from "@/lib/queries";
import { balanceTrend } from "@/lib/trend";
import { trendDelta } from "@/lib/insights";
import { formatMoney } from "@/lib/format";
import { BalanceTrendChart } from "./balance-trend-chart";
import { cn } from "@/lib/utils";

type Filter = "ALL" | "CHECKING" | "SAVINGS";

export function HeroBalance({
  accounts,
  transactions,
  activityLoading,
}: {
  accounts: Account[];
  transactions: ActivityItem[];
  activityLoading: boolean;
}) {
  const [filter, setFilter] = useState<Filter>("ALL");

  const types = useMemo(() => {
    const present = new Set(accounts.map((a) => a.type));
    return (["ALL", "CHECKING", "SAVINGS"] as Filter[]).filter(
      (t) => t === "ALL" || present.has(t),
    );
  }, [accounts]);

  const filtered = filter === "ALL" ? accounts : accounts.filter((a) => a.type === filter);
  const ids = new Set(filtered.map((a) => a.id));
  const totalMinor = filtered.reduce((sum, a) => sum + a.balance.minorUnits, 0);
  const currency = filtered[0]?.currency ?? "USD";
  const scopedTx = transactions.filter((t) => ids.has(t.accountId));
  const trend = balanceTrend(scopedTx, totalMinor);
  const delta = trendDelta(trend);
  const up = delta.minor >= 0;

  return (
    <div className="overflow-hidden rounded-2xl bg-primary p-6 text-primary-foreground shadow-sm sm:p-8">
      <div className="flex items-center justify-between gap-4">
        <p className="eyebrow !text-primary-foreground/60">Total balance</p>
        {types.length > 1 && (
          <div className="flex rounded-full bg-white/10 p-0.5 text-xs">
            {types.map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setFilter(t)}
                className={cn(
                  "rounded-full px-3 py-1 capitalize transition-colors",
                  filter === t
                    ? "bg-white/90 font-medium text-primary"
                    : "text-primary-foreground/70 hover:text-primary-foreground",
                )}
              >
                {t === "ALL" ? "All" : t.toLowerCase()}
              </button>
            ))}
          </div>
        )}
      </div>

      <p className="mt-3 font-display text-5xl tracking-tight tabular-nums sm:text-6xl">
        {formatMoney({ amount: (totalMinor / 100).toFixed(2), currency, minorUnits: totalMinor })}
      </p>

      <div className="mt-3 flex items-center gap-2 text-sm">
        <span
          className={cn(
            "inline-flex items-center gap-1 rounded-full px-2 py-0.5 font-mono tabular-nums",
            up ? "bg-white/15 text-primary-foreground" : "bg-white/15 text-primary-foreground",
          )}
        >
          {up ? <ArrowUpRight className="size-3.5" /> : <ArrowDownRight className="size-3.5" />}
          {formatMoney({
            amount: (Math.abs(delta.minor) / 100).toFixed(2),
            currency,
            minorUnits: Math.abs(delta.minor),
          })}
          {delta.pct !== null && <span> · {Math.abs(delta.pct).toFixed(1)}%</span>}
        </span>
        <span className="text-primary-foreground/60">vs last 30 days</span>
      </div>

      <div className="mt-4">
        {activityLoading ? (
          <div className="h-[180px] w-full animate-pulse rounded-lg bg-white/5" />
        ) : (
          <BalanceTrendChart data={trend} variant="onDark" height={180} />
        )}
      </div>
    </div>
  );
}
