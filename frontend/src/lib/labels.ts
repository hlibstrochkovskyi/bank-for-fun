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
