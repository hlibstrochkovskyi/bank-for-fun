"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import Link from "next/link";
import { ArrowRight, ShieldAlert } from "lucide-react";
import { useAccounts, useTransfer } from "@/lib/queries";
import { accountMeta } from "@/lib/labels";
import { formatMoney } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";

const schema = z
  .object({
    fromAccountId: z.string().min(1, "Choose an account to send from"),
    toAccountId: z.string().min(1, "Enter a recipient account"),
    amount: z
      .string()
      .regex(/^\d+(\.\d{1,2})?$/, "Enter a valid amount, e.g. 25.00")
      .refine((v) => Number(v) > 0, "Amount must be greater than zero"),
    description: z.string().max(140).optional(),
  })
  .refine((d) => d.fromAccountId !== d.toAccountId, {
    message: "Pick a different recipient account",
    path: ["toAccountId"],
  });

type FormValues = z.infer<typeof schema>;

export function TransferForm() {
  const { data: accounts } = useAccounts();
  const transfer = useTransfer();
  const [held, setHeld] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { fromAccountId: "", toAccountId: "", amount: "", description: "" },
  });

  const fromId = watch("fromAccountId");
  const toId = watch("toAccountId");
  const fromAccount = accounts?.find((a) => a.id === fromId);
  const others = (accounts ?? []).filter((a) => a.id !== fromId);

  function onSubmit(values: FormValues) {
    setHeld(false);
    transfer.mutate(
      {
        fromAccountId: values.fromAccountId,
        toAccountId: values.toAccountId,
        amount: values.amount,
        currency: fromAccount?.currency ?? "USD",
        description: values.description || undefined,
      },
      {
        onSuccess: (result) => {
          if (result.status === "HELD") {
            setHeld(true);
            toast.warning("Transfer held for review by our fraud checks.");
          } else {
            toast.success("Transfer complete.");
            reset({ fromAccountId: values.fromAccountId, toAccountId: "", amount: "", description: "" });
          }
        },
        onError: (e) => toast.error(e.message || "The transfer could not be completed."),
      },
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <div className="space-y-2">
        <Label htmlFor="from">From</Label>
        <Select
          value={fromId}
          onValueChange={(v) => setValue("fromAccountId", v ?? "", { shouldValidate: true })}
        >
          <SelectTrigger id="from" className="w-full">
            <SelectValue placeholder="Select an account" />
          </SelectTrigger>
          <SelectContent>
            {(accounts ?? []).map((a) => (
              <SelectItem key={a.id} value={a.id}>
                {accountMeta(a.type).label} · {formatMoney(a.balance)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <FieldError message={errors.fromAccountId?.message} />
      </div>

      <div className="space-y-2">
        <Label htmlFor="to">To</Label>
        <Input
          id="to"
          placeholder="Recipient account ID"
          {...register("toAccountId")}
          autoComplete="off"
        />
        {others.length > 0 && (
          <div className="flex flex-wrap gap-1.5 pt-0.5">
            <span className="text-xs text-muted-foreground">Your accounts:</span>
            {others.map((a) => (
              <button
                key={a.id}
                type="button"
                onClick={() => setValue("toAccountId", a.id, { shouldValidate: true })}
                className={cn(
                  "rounded-full border px-2.5 py-0.5 text-xs transition-colors hover:bg-secondary",
                  toId === a.id && "border-primary bg-accent text-accent-foreground",
                )}
              >
                {accountMeta(a.type).label}
              </button>
            ))}
          </div>
        )}
        <FieldError message={errors.toAccountId?.message} />
      </div>

      <div className="space-y-2">
        <Label htmlFor="amount">Amount</Label>
        <div className="relative">
          <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
            $
          </span>
          <Input id="amount" inputMode="decimal" placeholder="0.00" className="pl-7" {...register("amount")} />
        </div>
        <FieldError message={errors.amount?.message} />
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Note (optional)</Label>
        <Input id="description" placeholder="What's it for?" {...register("description")} />
      </div>

      {held && (
        <div className="flex items-start gap-3 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
          <ShieldAlert className="mt-0.5 size-4 shrink-0" />
          <div>
            This transfer was flagged and is{" "}
            <span className="font-medium">held for review</span> — the money hasn’t
            moved yet.{" "}
            <Link href="/held-transfers" className="font-medium underline underline-offset-2">
              View held transfers
            </Link>
          </div>
        </div>
      )}

      <Button type="submit" className="w-full" disabled={transfer.isPending}>
        {transfer.isPending ? "Sending…" : "Send transfer"}
        {!transfer.isPending && <ArrowRight className="size-4" />}
      </Button>
    </form>
  );
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return <p className="text-xs text-destructive">{message}</p>;
}
