import Link from "next/link";
import { ArrowLeftRight, Plus, Receipt } from "lucide-react";
import type { LucideIcon } from "lucide-react";

export function QuickActions({ primaryAccountId }: { primaryAccountId?: string }) {
  const actions: { href: string; label: string; icon: LucideIcon }[] = [
    { href: "/transfers", label: "Transfer", icon: ArrowLeftRight },
    {
      href: primaryAccountId ? `/accounts/${primaryAccountId}` : "/dashboard",
      label: "Add money",
      icon: Plus,
    },
    { href: "/transactions", label: "Activity", icon: Receipt },
  ];

  return (
    <div className="grid grid-cols-3 gap-3">
      {actions.map((a) => (
        <Link
          key={a.label}
          href={a.href}
          className="group flex flex-col items-start gap-3 rounded-xl border border-border bg-card p-4 transition-colors hover:border-clay/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <span className="grid size-9 place-items-center rounded-lg bg-accent text-accent-foreground transition-colors group-hover:bg-clay group-hover:text-white">
            <a.icon className="size-4" aria-hidden />
          </span>
          <span className="text-sm font-medium">{a.label}</span>
        </Link>
      ))}
    </div>
  );
}
