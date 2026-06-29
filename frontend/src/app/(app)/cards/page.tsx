"use client";

import { CreditCard } from "lucide-react";
import { useCards } from "@/lib/queries";
import { CardVisual } from "@/components/cards/card-visual";
import { IssueCardDialog } from "@/components/cards/issue-card-dialog";
import { Skeleton } from "@/components/ui/skeleton";

export default function CardsPage() {
  const { data: cards, isLoading } = useCards();
  const hasCards = (cards?.length ?? 0) > 0;

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="eyebrow">Wallet</p>
          <h1 className="mt-1.5 text-3xl tracking-tight">Cards</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Cards are bound to an account; spending posts to that account’s ledger.
          </p>
        </div>
        {hasCards && <IssueCardDialog />}
      </header>

      {isLoading ? (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="aspect-[1.586/1] rounded-2xl" />
          <Skeleton className="aspect-[1.586/1] rounded-2xl" />
        </div>
      ) : !hasCards ? (
        <EmptyCards />
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {cards!.map((card) => (
            <div key={card.id} className="space-y-2">
              <CardVisual card={card} />
              <p className="px-1 text-xs text-muted-foreground">
                {card.accountNickname || "Account"} ·{" "}
                <span className="capitalize">{card.status.toLowerCase()}</span>
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function EmptyCards() {
  return (
    <div className="grid place-items-center rounded-2xl border border-dashed bg-card/50 px-6 py-20 text-center">
      <span className="grid size-12 place-items-center rounded-xl bg-accent text-accent-foreground">
        <CreditCard className="size-6" aria-hidden />
      </span>
      <h2 className="mt-5 text-lg font-semibold">No cards yet</h2>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">
        Issue a card against one of your accounts to see it here.
      </p>
      <div className="mt-6">
        <IssueCardDialog />
      </div>
    </div>
  );
}
