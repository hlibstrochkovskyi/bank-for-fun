"use client";

import { ShieldCheck, ShieldAlert } from "lucide-react";
import { useHeldTransfers } from "@/lib/queries";
import { formatMoney, formatDateTime, humanize } from "@/lib/format";
import type { HeldTransfer } from "@/lib/schemas";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

const STATUS_STYLES: Record<string, string> = {
  PENDING_REVIEW: "border-gold/40 bg-gold/10 text-gold",
  RELEASED: "border-positive/30 bg-positive/10 text-positive",
  REJECTED: "border-border bg-secondary text-muted-foreground",
};

export default function HeldTransfersPage() {
  const { data, isLoading } = useHeldTransfers();

  return (
    <div className="space-y-6">
      <div>
        <p className="eyebrow">Fraud review</p>
        <h1 className="mt-1.5 text-3xl tracking-tight">Held transfers</h1>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          Transfers our fraud checks paused for a quick review.
        </p>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-24 rounded-xl" />
          <Skeleton className="h-24 rounded-xl" />
        </div>
      ) : !data?.length ? (
        <EmptyHeld />
      ) : (
        <div className="space-y-3">
          {data.map((held) => (
            <HeldRow key={held.id} held={held} />
          ))}
        </div>
      )}
    </div>
  );
}

function HeldRow({ held }: { held: HeldTransfer }) {
  return (
    <div className="rounded-lg border border-border bg-card p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-display text-2xl font-medium tabular-nums">
            {formatMoney(held.amount)}
          </p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {formatDateTime(held.createdAt)}
          </p>
        </div>
        <span
          className={cn(
            "rounded-full border px-2.5 py-1 text-xs font-medium",
            STATUS_STYLES[held.status] ?? STATUS_STYLES.REJECTED,
          )}
        >
          {humanize(held.status)}
        </span>
      </div>
      {held.reason && (
        <p className="mt-3 flex items-start gap-2 text-sm text-muted-foreground">
          <ShieldAlert className="mt-0.5 size-4 shrink-0 text-gold" />
          {held.reason}
          <span className="text-muted-foreground/70">
            · risk {Math.round(held.riskScore * 100)}%
          </span>
        </p>
      )}
    </div>
  );
}

function EmptyHeld() {
  return (
    <div className="grid place-items-center rounded-2xl border border-dashed bg-card/50 px-6 py-20 text-center">
      <span className="grid size-12 place-items-center rounded-xl bg-accent text-accent-foreground">
        <ShieldCheck className="size-6" aria-hidden />
      </span>
      <h2 className="mt-5 text-lg font-semibold">Nothing held</h2>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">
        None of your transfers are awaiting review. You’re all clear.
      </p>
    </div>
  );
}
