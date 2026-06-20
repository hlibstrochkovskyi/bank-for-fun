import type { ActivityItem } from "./queries";

export type TrendPoint = { label: string; value: number };

/**
 * Reconstruct a total-balance-over-time series from recent transactions and the
 * current total (minor units). Walks the signed entries forward from the implied
 * starting balance and keeps the end-of-day value, so the series ends at "today".
 */
export function balanceTrend(
  transactions: ActivityItem[],
  currentTotalMinor: number,
  days = 30,
): TrendPoint[] {
  const ascending = [...transactions].sort((a, b) =>
    a.createdAt.localeCompare(b.createdAt),
  );
  const totalDelta = ascending.reduce((sum, t) => sum + t.amount.minorUnits, 0);
  let running = currentTotalMinor - totalDelta;

  const byDay = new Map<string, number>();
  const start = new Date();
  start.setDate(start.getDate() - days);
  byDay.set(dayKey(start), running);

  for (const t of ascending) {
    running += t.amount.minorUnits;
    byDay.set(dayKey(new Date(t.createdAt)), running);
  }
  byDay.set(dayKey(new Date()), currentTotalMinor);

  // Fill forward so the line is continuous across days with no activity.
  const points: TrendPoint[] = [];
  let last = currentTotalMinor - totalDelta;
  for (let i = days; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const key = dayKey(d);
    if (byDay.has(key)) last = byDay.get(key)!;
    points.push({
      label: d.toLocaleDateString("en-US", { month: "short", day: "numeric" }),
      value: last / 100,
    });
  }
  return points;
}

function dayKey(d: Date): string {
  return d.toISOString().slice(0, 10);
}
