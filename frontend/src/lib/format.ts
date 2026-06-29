import type { Money } from "./schemas";

/** Format a Money value as a localized currency string, e.g. "$1,250.00". */
export function formatMoney(money: Money): string {
  const major = Number(money.minorUnits) / 100;
  try {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: money.currency,
    }).format(major);
  } catch {
    return `${money.amount} ${money.currency}`;
  }
}

/** A signed amount for transaction rows: "+$50.00" / "−$30.00". */
export function formatSignedMoney(money: Money): string {
  const formatted = formatMoney({ ...money, minorUnits: Math.abs(money.minorUnits) });
  if (money.minorUnits > 0) return `+${formatted}`;
  if (money.minorUnits < 0) return `−${formatted}`;
  return formatted;
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

/** Mask an account number for display, e.g. "1234567890" -> "•••• 7890". */
export function maskAccountNumber(num?: string | null): string {
  if (!num) return "";
  return `•••• ${num.slice(-4)}`;
}

/** Title-case an enum-ish token, e.g. "PENDING_REVIEW" -> "Pending review". */
export function humanize(token: string): string {
  const lower = token.replace(/_/g, " ").toLowerCase();
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}
