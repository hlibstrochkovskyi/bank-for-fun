import { cn } from "@/lib/utils";
import type { Card } from "@/lib/schemas";

/** A realistic debit-card visual. Presentational only — shows the last four. */
export function CardVisual({ card, className }: { card: Card; className?: string }) {
  const frozen = card.status === "FROZEN" || card.status === "CANCELLED";

  return (
    <div
      className={cn(
        "relative flex aspect-[1.586/1] w-full flex-col justify-between overflow-hidden rounded-2xl p-5 text-white shadow-md",
        "bg-[radial-gradient(120%_120%_at_0%_0%,oklch(0.34_0.04_162)_0%,oklch(0.2_0.02_165)_55%,oklch(0.16_0.015_165)_100%)]",
        frozen && "opacity-60 grayscale",
        className,
      )}
    >
      {/* sheen */}
      <div className="pointer-events-none absolute -right-8 -top-10 size-40 rounded-full bg-white/5 blur-2xl" />

      <div className="flex items-start justify-between">
        <span className="font-display text-lg tracking-tight">Ledger</span>
        <span className="text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-white/60">
          {frozen ? card.status : "Debit"}
        </span>
      </div>

      {/* chip */}
      <div className="h-7 w-10 rounded-md bg-gradient-to-br from-[oklch(0.8_0.1_85)] to-[oklch(0.62_0.1_75)] shadow-inner" />

      <div className="font-mono text-base tracking-[0.2em] tabular-nums">
        •••• •••• •••• {card.last4}
      </div>

      <div className="flex items-end justify-between">
        <div className="min-w-0">
          <p className="text-[0.6rem] uppercase tracking-[0.16em] text-white/50">Card holder</p>
          <p className="truncate text-sm font-medium">{card.cardholder}</p>
        </div>
        <div className="text-right">
          <p className="text-[0.6rem] uppercase tracking-[0.16em] text-white/50">Exp</p>
          <p className="font-mono text-sm tabular-nums">
            {String(card.expMonth).padStart(2, "0")}/{String(card.expYear).slice(-2)}
          </p>
        </div>
        <span className="ml-3 text-xs font-semibold uppercase tracking-wide text-white/80">
          {card.network}
        </span>
      </div>
    </div>
  );
}
