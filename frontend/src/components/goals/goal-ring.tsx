import { formatMoney } from "@/lib/format";
import type { Goal } from "@/lib/schemas";

/** A circular progress ring for a savings goal. Progress is derived server-side. */
export function GoalRing({ goal, size = 132 }: { goal: Goal; size?: number }) {
  const pct = Math.max(0, Math.min(100, goal.pct));
  const stroke = 10;
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const remaining = Math.max(0, goal.target.minorUnits - goal.saved.minorUnits);

  return (
    <div className="flex items-center gap-5">
      <div className="relative shrink-0" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="-rotate-90">
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            stroke="var(--color-secondary)"
            strokeWidth={stroke}
          />
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            stroke="var(--color-clay)"
            strokeWidth={stroke}
            strokeLinecap="round"
            strokeDasharray={c}
            strokeDashoffset={c - (pct / 100) * c}
          />
        </svg>
        <div className="absolute inset-0 grid place-items-center">
          <div className="text-center">
            <p className="font-display text-2xl tabular-nums leading-none">{Math.round(pct)}%</p>
            <p className="mt-0.5 text-[0.65rem] uppercase tracking-[0.14em] text-muted-foreground">
              saved
            </p>
          </div>
        </div>
      </div>

      <div className="min-w-0">
        <p className="eyebrow">Goal</p>
        <p className="truncate text-lg font-medium">{goal.name}</p>
        <p className="mt-1 font-mono text-sm tabular-nums">
          {formatMoney(goal.saved)}{" "}
          <span className="text-muted-foreground">/ {formatMoney(goal.target)}</span>
        </p>
        {remaining > 0 ? (
          <p className="mt-0.5 text-xs text-muted-foreground">
            {formatMoney({
              amount: (remaining / 100).toFixed(2),
              currency: goal.target.currency,
              minorUnits: remaining,
            })}{" "}
            to go
          </p>
        ) : (
          <p className="mt-0.5 text-xs text-positive">Goal reached 🎉</p>
        )}
      </div>
    </div>
  );
}
