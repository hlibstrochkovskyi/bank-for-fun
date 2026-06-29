import { cn } from "@/lib/utils";

export function Logo({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "font-display text-[21px] font-medium leading-none tracking-[-0.01em]",
        className,
      )}
    >
      Funny Bank
    </span>
  );
}
