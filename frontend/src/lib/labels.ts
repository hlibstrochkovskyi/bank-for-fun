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
