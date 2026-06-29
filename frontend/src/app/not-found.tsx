import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";

export default function NotFound() {
  return (
    <div className="grid min-h-[60vh] place-items-center px-6 text-center">
      <div>
        <p className="eyebrow text-muted-foreground">404</p>
        <h1 className="mt-2 font-display text-3xl tracking-tight">Page not found</h1>
        <p className="mx-auto mt-2 max-w-sm text-sm text-muted-foreground">
          The page you’re looking for doesn’t exist or has moved.
        </p>
        <div className="mt-6">
          <Link href="/dashboard" className={buttonVariants({ size: "sm" })}>
            Back to dashboard
          </Link>
        </div>
      </div>
    </div>
  );
}
