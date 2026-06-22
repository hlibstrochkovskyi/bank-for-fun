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
      <span className="grid size-8 place-items-center rounded-md bg-primary text-primary-foreground">
        {/* Ledger rules + a balanced baseline. */}
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden>
          <path
            d="M3 4h10M3 7.5h6M3 11.5h10"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
          />
        </svg>
      </span>
      {showWordmark && (
        <span className="font-display text-[20px] font-medium leading-none tracking-tight text-foreground">
          Ledger
        </span>
      )}
    </span>
  );
}
