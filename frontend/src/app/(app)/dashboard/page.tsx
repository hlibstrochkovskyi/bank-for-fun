"use client";

import { useAccounts } from "@/lib/queries";

export default function DashboardPage() {
  const { data: accounts, isLoading, error } = useAccounts();

  return (
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
      {isLoading && <p className="mt-4 text-muted-foreground">Loading…</p>}
      {error && (
        <p className="mt-4 text-destructive">Couldn’t load your accounts.</p>
      )}
      {accounts && (
        <ul className="mt-4 space-y-2">
          {accounts.map((a) => (
            <li key={a.id} className="rounded-lg border bg-card p-4">
              {a.type} — {a.balance.amount} {a.currency}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
