import { TransferForm } from "@/components/transfer/transfer-form";

export default function TransfersPage() {
  return (
    <div className="mx-auto max-w-md">
      <p className="eyebrow">Move money</p>
      <h1 className="mt-1.5 text-3xl tracking-tight">Send a transfer</h1>
      <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
        Move money between accounts. Large or unusual transfers may be held for a
        quick fraud review before they settle.
      </p>

      <div className="mt-6 rounded-2xl border bg-card p-6 shadow-[0_1px_4px_rgba(0,0,0,0.04)]">
        <TransferForm />
      </div>
    </div>
  );
}
