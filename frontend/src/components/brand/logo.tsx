import { cn } from "@/lib/utils";

export function Logo({
  className,
  showWordmark = true,
}: {
  className?: string;
  showWordmark?: boolean;
}) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <span className="grid size-8 place-items-center rounded-[9px] bg-clay text-white shadow-sm">
        {/* A balanced laurel sprig over a ledger baseline. */}
        <svg width="17" height="17" viewBox="0 0 18 18" fill="none" aria-hidden>
          <path
            d="M9 3.2c-1.7 1-2.6 2.5-2.6 4.4M9 3.2c1.7 1 2.6 2.5 2.6 4.4M9 4.6v8.2"
            stroke="currentColor"
            strokeWidth="1.4"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <path
            d="M4.5 14.4h9"
            stroke="currentColor"
            strokeWidth="1.4"
            strokeLinecap="round"
          />
        </svg>
      </span>
      {showWordmark && (
        <span className="font-display text-[21px] font-medium leading-none tracking-[-0.01em]">
          Ledger
        </span>
      )}
    </span>
  );
}
