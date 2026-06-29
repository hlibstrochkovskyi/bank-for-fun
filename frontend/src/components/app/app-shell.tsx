"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSession } from "next-auth/react";
import {
  LayoutDashboard,
  ArrowLeftRight,
  CreditCard,
  Receipt,
  ShieldAlert,
  ShieldHalf,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Logo } from "@/components/brand/logo";
import { UserMenu } from "./user-menu";
import { cn } from "@/lib/utils";

type NavItem = { href: string; label: string; icon: LucideIcon; admin?: boolean };

const nav: NavItem[] = [
  { href: "/dashboard", label: "Home", icon: LayoutDashboard },
  { href: "/transactions", label: "Transactions", icon: Receipt },
  { href: "/cards", label: "Cards", icon: CreditCard },
  { href: "/transfers", label: "Transfer", icon: ArrowLeftRight },
  { href: "/held-transfers", label: "Held", icon: ShieldAlert },
  { href: "/admin", label: "Review queue", icon: ShieldHalf, admin: true },
];

function useNavItems() {
  const pathname = usePathname();
  const { data: session } = useSession();
  const isAdmin = session?.roles?.includes("admin") ?? false;
  return nav
    .filter((item) => !item.admin || isAdmin)
    .map((item) => ({
      ...item,
      active: pathname === item.href || pathname.startsWith(`${item.href}/`),
    }));
}

export function AppShell({
  user,
  children,
}: {
  user: { name?: string | null; email?: string | null };
  children: React.ReactNode;
}) {
  const items = useNavItems();

  return (
    <div className="min-h-full">
      {/* Desktop: fixed forest-green sidebar. */}
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground lg:flex">
        <div className="flex h-16 items-center px-6">
          <Link href="/dashboard" aria-label="Ledger home" className="text-sidebar-foreground">
            <Logo />
          </Link>
        </div>

        <p className="eyebrow px-6 pb-2 pt-4 text-sidebar-muted">Personal banking</p>
        <nav className="flex flex-1 flex-col gap-1 px-3">
          {items.map((item) => (
            <NavLink key={item.href} {...item} />
          ))}
        </nav>

        <div className="border-t border-sidebar-border p-4">
          <div className="flex items-center gap-3">
            <UserMenu name={user.name} email={user.email} />
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-sidebar-foreground">
                {user.name ?? "Account"}
              </p>
              {user.email && (
                <p className="truncate text-xs text-sidebar-muted">{user.email}</p>
              )}
            </div>
          </div>
        </div>
      </aside>

      {/* Mobile: forest-green top bar. */}
      <header className="sticky top-0 z-40 border-b border-sidebar-border bg-sidebar text-sidebar-foreground lg:hidden">
        <div className="flex h-14 items-center justify-between px-4">
          <Link href="/dashboard" aria-label="Ledger home" className="text-sidebar-foreground">
            <Logo />
          </Link>
          <UserMenu name={user.name} email={user.email} />
        </div>
        <nav className="flex gap-1 overflow-x-auto px-3 pb-2">
          {items.map((item) => (
            <NavLink key={item.href} {...item} compact />
          ))}
        </nav>
      </header>

      <main className="lg:pl-64">
        <div className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 lg:py-12">
          {children}
        </div>
      </main>
    </div>
  );
}

function NavLink({
  href,
  label,
  icon: Icon,
  active,
  compact = false,
}: {
  href: string;
  label: string;
  icon: LucideIcon;
  active: boolean;
  compact?: boolean;
}) {
  return (
    <Link
      href={href}
      className={cn(
        "group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors",
        compact ? "whitespace-nowrap" : "",
        active
          ? "bg-sidebar-accent text-sidebar-accent-foreground"
          : "text-sidebar-foreground/65 hover:bg-sidebar-accent/60 hover:text-sidebar-foreground",
      )}
    >
      {active && !compact && (
        <span className="absolute inset-y-1.5 left-0 w-0.5 rounded-full bg-sidebar-primary" />
      )}
      <Icon className="size-4 shrink-0" aria-hidden />
      {label}
    </Link>
  );
}
