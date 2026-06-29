"use client";

import { useSession } from "next-auth/react";
import { toast } from "sonner";
import { ShieldAlert, ShieldCheck, Check, X } from "lucide-react";
import { useAdminHeldTransfers, useReviewHeld } from "@/lib/queries";
import { formatMoney, formatDateTime, humanize } from "@/lib/format";
import type { AdminHeldTransfer } from "@/lib/schemas";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

const STATUS_STYLES: Record<string, string> = {
  PENDING_REVIEW: "border-gold/40 bg-gold/10 text-gold",
  RELEASED: "border-positive/30 bg-positive/10 text-positive",
  REJECTED: "border-border bg-secondary text-muted-foreground",
};

export default function AdminPage() {
  const { data: session } = useSession();
  const isAdmin = session?.roles?.includes("admin") ?? false;
  const { data, isLoading, error } = useAdminHeldTransfers();

  if (!isAdmin) {
    return (
      <div className="grid place-items-center rounded-2xl border border-dashed bg-card/50 px-6 py-20 text-center">
        <ShieldAlert className="size-7 text-muted-foreground" />
        <h1 className="mt-4 text-lg font-semibold">Admin access required</h1>
        <p className="mt-1 max-w-sm text-sm text-muted-foreground">
          This area is for fraud reviewers. Sign in with an account that has the admin role.
        </p>
      </div>
    );
  }

  const pending = (data ?? []).filter((h) => h.status === "PENDING_REVIEW");
  const resolved = (data ?? []).filter((h) => h.status !== "PENDING_REVIEW");

  return (
    <div className="space-y-8">
      <header>
        <p className="eyebrow">Fraud operations</p>
        <h1 className="mt-1.5 text-3xl tracking-tight">Review queue</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Transfers the risk engine held. Release to post them, or reject to discard.
        </p>
      </header>

      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-28 rounded-xl" />
        </div>
      ) : error ? (
        <p className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
          Couldn’t load the queue. {error.message}
        </p>
      ) : (
        <>
          <section className="space-y-3">
            <h2 className="eyebrow">Pending · {pending.length}</h2>
            {pending.length === 0 ? (
              <div className="grid place-items-center rounded-xl border border-dashed bg-card/50 px-6 py-12 text-center">
                <ShieldCheck className="size-6 text-positive" />
                <p className="mt-2 text-sm text-muted-foreground">Queue clear. Nothing awaiting review.</p>
              </div>
            ) : (
              <div className="space-y-3">
                {pending.map((h) => (
                  <ReviewRow key={h.id} held={h} />
                ))}
              </div>
            )}
          </section>

          {resolved.length > 0 && (
            <section className="space-y-3">
              <h2 className="eyebrow">Recently resolved</h2>
              <div className="space-y-3">
                {resolved.map((h) => (
                  <ReviewRow key={h.id} held={h} />
                ))}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  );
}

function ReviewRow({ held }: { held: AdminHeldTransfer }) {
  const review = useReviewHeld();
  const pending = held.status === "PENDING_REVIEW";
  const risk = Math.round(held.riskScore * 100);

  function act(action: "release" | "reject") {
    review.mutate(
      { id: held.id, action },
      {
        onSuccess: () =>
          toast.success(action === "release" ? "Transfer released and posted." : "Transfer rejected."),
        onError: (e) => toast.error(e.message || "That didn’t work."),
      },
    );
  }

  return (
    <div className="rounded-xl border border-border bg-card p-5 shadow-[0_1px_4px_rgba(0,0,0,0.04)]">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-display text-2xl font-medium tabular-nums">{formatMoney(held.amount)}</p>
          <p className="mt-0.5 font-mono text-xs text-muted-foreground">
            acct …{held.fromAccountId.slice(-4)} → …{held.toAccountId.slice(-4)} ·{" "}
            {formatDateTime(held.createdAt)}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <RiskBadge risk={risk} />
          <span
            className={cn(
              "rounded-full border px-2.5 py-1 text-xs font-medium",
              STATUS_STYLES[held.status] ?? STATUS_STYLES.REJECTED,
            )}
          >
            {humanize(held.status)}
          </span>
        </div>
      </div>

      {held.reason && (
        <p className="mt-3 flex items-start gap-2 text-sm text-muted-foreground">
          <ShieldAlert className="mt-0.5 size-4 shrink-0 text-gold" />
          {held.reason}
        </p>
      )}

      {pending && (
        <div className="mt-4 flex gap-2">
          <Button size="sm" onClick={() => act("release")} disabled={review.isPending}>
            <Check className="size-4" /> Release
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => act("reject")}
            disabled={review.isPending}
          >
            <X className="size-4" /> Reject
          </Button>
        </div>
      )}
    </div>
  );
}

function RiskBadge({ risk }: { risk: number }) {
  const tone =
    risk >= 80
      ? "border-destructive/30 bg-destructive/10 text-destructive"
      : risk >= 50
        ? "border-gold/40 bg-gold/10 text-gold"
        : "border-positive/30 bg-positive/10 text-positive";
  return (
    <span className={cn("rounded-full border px-2.5 py-1 font-mono text-xs tabular-nums", tone)}>
      risk {risk}%
    </span>
  );
}
