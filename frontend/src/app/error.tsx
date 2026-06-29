"use client";

import { useEffect } from "react";
import { Button, buttonVariants } from "@/components/ui/button";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="grid min-h-[60vh] place-items-center px-6 text-center">
      <div>
        <p className="eyebrow text-muted-foreground">Error</p>
        <h1 className="mt-2 font-display text-3xl tracking-tight">Something broke</h1>
        <p className="mx-auto mt-2 max-w-sm text-sm text-muted-foreground">
          An unexpected error occurred. You can try again, or head back home.
        </p>
        <div className="mt-6 flex items-center justify-center gap-2">
          <Button size="sm" onClick={() => reset()}>
            Try again
          </Button>
          <a href="/dashboard" className={buttonVariants({ variant: "outline", size: "sm" })}>
            Go home
          </a>
        </div>
      </div>
    </div>
  );
}
