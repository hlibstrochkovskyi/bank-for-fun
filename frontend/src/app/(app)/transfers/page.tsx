import { TransferForm } from "@/components/transfer/transfer-form";

export default function TransfersPage() {
  return (
    <div className="mx-auto max-w-md">
      <h1 className="text-2xl font-semibold tracking-tight">Send a transfer</h1>
      <p className="mt-1 text-sm text-muted-foreground">
        Move money between accounts. Large or unusual transfers may be held for a
        quick fraud review before they settle.
      </p>

      <div className="mt-6 rounded-2xl border bg-card p-6 shadow-sm">
        <TransferForm />
      </div>
    </div>
  );
}
