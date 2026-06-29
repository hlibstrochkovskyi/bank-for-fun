import { categoryColor, categoryLabel } from "@/lib/labels";
import { formatMoney } from "@/lib/format";
import type { CategorySlice } from "@/lib/insights";

export function CategoryBreakdown({
  slices,
  totalMinor,
  currency,
}: {
  slices: CategorySlice[];
  totalMinor: number;
  currency: string;
}) {
  if (totalMinor === 0) {
    return (
      <p className="py-8 text-center text-sm text-muted-foreground">
        No spending in the last 30 days.
      </p>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex h-2.5 w-full overflow-hidden rounded-full bg-secondary">
        {slices.map((s) => (
          <div
            key={s.category}
            style={{ width: `${s.pct}%`, backgroundColor: categoryColor(s.category) }}
            title={`${categoryLabel(s.category) ?? s.category} · ${s.pct.toFixed(0)}%`}
          />
        ))}
      </div>

      <ul className="space-y-2.5">
        {slices.map((s) => (
          <li key={s.category} className="flex items-center gap-3 text-sm">
            <span
              className="size-2.5 shrink-0 rounded-full"
              style={{ backgroundColor: categoryColor(s.category) }}
              aria-hidden
            />
            <span className="flex-1 truncate">{categoryLabel(s.category) ?? "Other"}</span>
            <span className="font-mono text-xs tabular-nums text-muted-foreground">
              {s.pct.toFixed(0)}%
            </span>
            <span className="w-20 text-right font-mono text-sm tabular-nums">
              {formatMoney({
                amount: (s.minor / 100).toFixed(2),
                currency,
                minorUnits: s.minor,
              })}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
