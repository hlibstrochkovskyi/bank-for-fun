"use client";

import { useState } from "react";
import { ArrowDownLeft, ArrowUpRight } from "lucide-react";
import { toast } from "sonner";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useDeposit, useWithdraw } from "@/lib/queries";
import type { Account } from "@/lib/schemas";
import { cn } from "@/lib/utils";

const COPY = {
  deposit: {
    trigger: "Deposit",
    title: "Deposit money",
    description: "Add funds to this account.",
    confirm: "Deposit",
    Icon: ArrowDownLeft,
    triggerVariant: "default" as const,
  },
  withdraw: {
    trigger: "Withdraw",
    title: "Withdraw money",
    description: "Move funds out of this account.",
    confirm: "Withdraw",
    Icon: ArrowUpRight,
    triggerVariant: "outline" as const,
  },
};

export function AccountMoneyDialog({
  account,
  op,
}: {
  account: Account;
  op: "deposit" | "withdraw";
}) {
  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState("");
  const deposit = useDeposit();
  const withdraw = useWithdraw();
  const mutation = op === "deposit" ? deposit : withdraw;
  const copy = COPY[op];

  function submit() {
    if (!/^\d+(\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) {
      toast.error("Enter a valid amount, e.g. 50.00");
      return;
    }
    mutation.mutate(
      { accountId: account.id, amount, currency: account.currency },
      {
        onSuccess: () => {
          toast.success(op === "deposit" ? "Deposit complete." : "Withdrawal complete.");
          setOpen(false);
          setAmount("");
        },
        onError: (e) => toast.error(e.message || "That didn’t work."),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger className={cn(buttonVariants({ variant: copy.triggerVariant, size: "sm" }))}>
        <copy.Icon className="size-4" />
        {copy.trigger}
      </DialogTrigger>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>{copy.title}</DialogTitle>
          <DialogDescription>{copy.description}</DialogDescription>
        </DialogHeader>

        <div className="grid gap-2 py-2">
          <Label htmlFor="op-amount">Amount</Label>
          <div className="relative">
            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
              $
            </span>
            <Input
              id="op-amount"
              inputMode="decimal"
              placeholder="0.00"
              className="pl-7"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              autoFocus
            />
          </div>
        </div>

        <DialogFooter>
          <DialogClose className={cn(buttonVariants({ variant: "outline", size: "sm" }))}>
            Cancel
          </DialogClose>
          <Button size="sm" onClick={submit} disabled={mutation.isPending}>
            {mutation.isPending ? "Working…" : copy.confirm}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
