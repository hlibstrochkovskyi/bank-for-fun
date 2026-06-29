"use client";

import { Target } from "lucide-react";
import { useGoals } from "@/lib/queries";
import { GoalRing } from "@/components/goals/goal-ring";
import { SetGoalDialog } from "@/components/goals/set-goal-dialog";
import { Skeleton } from "@/components/ui/skeleton";

export default function SavingsPage() {
  const { data: goals, isLoading } = useGoals();
  const hasGoals = (goals?.length ?? 0) > 0;

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="eyebrow">Savings</p>
          <h1 className="mt-1.5 text-3xl tracking-tight">Goals</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Targets on your savings accounts. Progress is the account’s real balance.
          </p>
        </div>
        {hasGoals && <SetGoalDialog label="New goal" />}
      </header>

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2">
          <Skeleton className="h-40 rounded-xl" />
          <Skeleton className="h-40 rounded-xl" />
        </div>
      ) : !hasGoals ? (
        <EmptyGoals />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {goals!.map((goal) => (
            <div
              key={goal.id}
              className="rounded-xl border bg-card p-6 shadow-[0_1px_4px_rgba(0,0,0,0.04)]"
            >
              <GoalRing goal={goal} />
              <p className="mt-4 text-xs text-muted-foreground">
                {goal.accountNickname || "Savings"}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function EmptyGoals() {
  return (
    <div className="grid place-items-center rounded-2xl border border-dashed bg-card/50 px-6 py-20 text-center">
      <span className="grid size-12 place-items-center rounded-xl bg-accent text-accent-foreground">
        <Target className="size-6" aria-hidden />
      </span>
      <h2 className="mt-5 text-lg font-semibold">No goals yet</h2>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">
        Set a target on a savings account to track your progress here.
      </p>
      <div className="mt-6">
        <SetGoalDialog />
      </div>
    </div>
  );
}
