import type { ActivityItem } from "./queries";
import type { TrendPoint } from "./trend";

/** Change in total balance across the trend window (minor units + percentage). */
export function trendDelta(trend: TrendPoint[]): { minor: number; pct: number | null } {
  if (trend.length < 2) return { minor: 0, pct: null };
  const first = trend[0].value;
  const last = trend[trend.length - 1].value;
  const minor = Math.round((last - first) * 100);
  const pct = first > 0 ? ((last - first) / first) * 100 : null;
  return { minor, pct };
}

export type CategorySlice = {
  category: string;
  minor: number;
  pct: number;
};

/**
 * Spending grouped by category over the last `days`. Counts debits only and
 * excludes internal transfers (moving money between your own accounts isn't
 * spending). Percentages are of total spending.
 */
export function spendingByCategory(txs: ActivityItem[], days = 30): {
  slices: CategorySlice[];
  totalMinor: number;
} {
  const since = Date.now() - days * 86_400_000;
  const totals = new Map<string, number>();
  for (const t of txs) {
    if (t.amount.minorUnits >= 0) continue; // debits only
    const category = t.category ?? "UNCATEGORIZED";
    if (category === "TRANSFER") continue;
    if (new Date(t.createdAt).getTime() < since) continue;
    totals.set(category, (totals.get(category) ?? 0) + Math.abs(t.amount.minorUnits));
  }
  const totalMinor = [...totals.values()].reduce((a, b) => a + b, 0);
  const slices = [...totals.entries()]
    .map(([category, minor]) => ({
      category,
      minor,
      pct: totalMinor > 0 ? (minor / totalMinor) * 100 : 0,
    }))
    .sort((a, b) => b.minor - a.minor);
  return { slices, totalMinor };
}

/** Total income (credits, excluding internal transfers) over the last `days`. */
export function incomeOver(txs: ActivityItem[], days = 30): number {
  const since = Date.now() - days * 86_400_000;
  return txs
    .filter(
      (t) =>
        t.amount.minorUnits > 0 &&
        (t.category ?? "") !== "TRANSFER" &&
        new Date(t.createdAt).getTime() >= since,
    )
    .reduce((sum, t) => sum + t.amount.minorUnits, 0);
}
