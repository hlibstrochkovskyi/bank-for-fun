"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import Link from "next/link";
import { ArrowRight, ShieldAlert } from "lucide-react";
import { useAccounts, useTransfer } from "@/lib/queries";
import { accountName } from "@/lib/labels";
import { formatMoney, maskAccountNumber } from "@/lib/format";
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
  const [manualTo, setManualTo] = useState(false);

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
  const amount = watch("amount");
  const fromAccount = accounts?.find((a) => a.id === fromId);
  const others = (accounts ?? []).filter((a) => a.id !== fromId);

  const amountMinor = /^\d+(\.\d{1,2})?$/.test(amount ?? "")
    ? Math.round(Number(amount) * 100)
    : 0;
  const balanceAfter =
    fromAccount && amountMinor > 0
      ? { ...fromAccount.balance, minorUnits: fromAccount.balance.minorUnits - amountMinor }
      : null;
  const insufficient = balanceAfter ? balanceAfter.minorUnits < 0 : false;

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
                <span className="flex w-full items-center justify-between gap-3">
                  <span className="truncate">
                    {accountName(a)}
                    <span className="ml-1 font-mono text-xs text-muted-foreground">
                      {maskAccountNumber(a.accountNumber)}
                    </span>
                  </span>
                  <span className="tabular-nums text-muted-foreground">{formatMoney(a.balance)}</span>
                </span>
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <FieldError message={errors.fromAccountId?.message} />
      </div>

      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <Label htmlFor="to">To</Label>
          <button
            type="button"
            onClick={() => {
              setManualTo((m) => !m);
              setValue("toAccountId", "", { shouldValidate: false });
            }}
            className="text-xs text-muted-foreground underline-offset-2 hover:text-foreground hover:underline"
          >
            {manualTo ? "Choose my account" : "Use an account ID"}
          </button>
        </div>
        {manualTo ? (
          <Input
            id="to"
            placeholder="Recipient account ID"
            {...register("toAccountId")}
            autoComplete="off"
          />
        ) : (
          <Select
            value={toId}
            onValueChange={(v) => setValue("toAccountId", v ?? "", { shouldValidate: true })}
          >
            <SelectTrigger id="to" className="w-full">
              <SelectValue placeholder={others.length ? "Select an account" : "No other accounts"} />
            </SelectTrigger>
            <SelectContent>
              {others.map((a) => (
                <SelectItem key={a.id} value={a.id}>
                  <span className="flex w-full items-center justify-between gap-3">
                    <span className="truncate">
                      {accountName(a)}
                      <span className="ml-1 font-mono text-xs text-muted-foreground">
                        {maskAccountNumber(a.accountNumber)}
                      </span>
                    </span>
                    <span className="tabular-nums text-muted-foreground">{formatMoney(a.balance)}</span>
                  </span>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
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
        {balanceAfter && (
          <p className={cn("text-xs", insufficient ? "text-destructive" : "text-muted-foreground")}>
            {insufficient
              ? "Not enough funds in this account."
              : `Balance after: ${formatMoney(balanceAfter)}`}
          </p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Note (optional)</Label>
        <Input id="description" placeholder="What's it for?" {...register("description")} />
      </div>

      {held && (
        <div className="flex items-start gap-3 rounded-lg border border-gold/40 bg-gold/10 p-3 text-sm text-gold">
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

      <Button type="submit" className="w-full" disabled={transfer.isPending || insufficient}>
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
