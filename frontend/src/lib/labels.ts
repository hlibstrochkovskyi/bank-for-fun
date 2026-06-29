import {
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowUpRight,
  Landmark,
  PiggyBank,
  Undo2,
  Wallet,
  type LucideIcon,
} from "lucide-react";

/** Display name for an account: its nickname, or the type label as a fallback. */
export function accountName(account: { nickname?: string | null; type: string }): string {
  return account.nickname?.trim() || accountMeta(account.type).label;
}

export function accountMeta(type: string): { label: string; Icon: LucideIcon } {
  switch (type) {
    case "CHECKING":
      return { label: "Checking", Icon: Wallet };
    case "SAVINGS":
      return { label: "Savings", Icon: PiggyBank };
    default:
      return { label: type, Icon: Landmark };
  }
}

/** An earthy, on-palette colour per spending category (for bars, dots, tiles). */
const CATEGORY_COLOR: Record<string, string> = {
  INCOME: "oklch(0.535 0.072 132)", // olive
  HOUSING: "oklch(0.305 0.045 162)", // forest
  GROCERIES: "oklch(0.555 0.078 138)", // sage-olive
  DINING: "oklch(0.615 0.122 47)", // terracotta
  SHOPPING: "oklch(0.665 0.094 78)", // gold
  UTILITIES: "oklch(0.50 0.05 200)", // muted teal
  TRANSPORT: "oklch(0.50 0.055 60)", // brown
  SUBSCRIPTIONS: "oklch(0.58 0.10 30)", // rust
  TRANSFER: "oklch(0.62 0.014 74)", // taupe
  UNCATEGORIZED: "oklch(0.62 0.014 74)", // taupe
};

export function categoryColor(category?: string | null): string {
  return CATEGORY_COLOR[category ?? "UNCATEGORIZED"] ?? CATEGORY_COLOR.UNCATEGORIZED;
}

/** Human label for a derived spending category, e.g. "GROCERIES" -> "Groceries". */
export function categoryLabel(category?: string | null): string | null {
  if (!category || category === "UNCATEGORIZED") return null;
  const lower = category.replace(/_/g, " ").toLowerCase();
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}

/** First letter for a merchant avatar tile. */
export function merchantInitial(merchant?: string | null): string {
  const m = merchant?.trim();
  return m ? m.charAt(0).toUpperCase() : "•";
}

export function transactionMeta(type: string): { label: string; Icon: LucideIcon } {
  switch (type) {
    case "DEPOSIT":
      return { label: "Deposit", Icon: ArrowDownLeft };
    case "WITHDRAWAL":
      return { label: "Withdrawal", Icon: ArrowUpRight };
    case "TRANSFER":
      return { label: "Transfer", Icon: ArrowLeftRight };
    case "REVERSAL":
      return { label: "Reversal", Icon: Undo2 };
    default:
      return { label: type, Icon: ArrowLeftRight };
  }
}
