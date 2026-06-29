"use client";

import { useState } from "react";
import { Target } from "lucide-react";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAccounts, useSetGoal } from "@/lib/queries";
import { accountName } from "@/lib/labels";
import { formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";

export function SetGoalDialog({ label = "Set a goal" }: { label?: string }) {
  const { data: accounts } = useAccounts();
  const setGoal = useSetGoal();
  const [open, setOpen] = useState(false);
  const [accountId, setAccountId] = useState("");
  const [name, setName] = useState("");
  const [target, setTarget] = useState("");

  const savings = (accounts ?? []).filter((a) => a.type === "SAVINGS");

  function submit() {
    if (!accountId) return toast.error("Choose a savings account.");
    if (!name.trim()) return toast.error("Name your goal.");
    if (!/^\d+(\.\d{1,2})?$/.test(target) || Number(target) <= 0) {
      return toast.error("Enter a valid target amount, e.g. 20000.00");
    }
    setGoal.mutate(
      { accountId, name: name.trim(), target },
      {
        onSuccess: () => {
          toast.success("Goal saved.");
          setOpen(false);
          setAccountId("");
          setName("");
          setTarget("");
        },
        onError: (e) => toast.error(e.message || "Couldn’t save the goal."),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger className={cn(buttonVariants({ size: "sm" }))}>
        <Target className="size-4" />
        {label}
      </DialogTrigger>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Set a savings goal</DialogTitle>
          <DialogDescription>
            Track progress toward a target. Progress is your savings account’s balance.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3 py-2">
          <div className="grid gap-2">
            <Label htmlFor="goal-account">Savings account</Label>
            <Select value={accountId} onValueChange={(v) => setAccountId(v ?? "")}>
              <SelectTrigger id="goal-account" className="w-full">
                <SelectValue placeholder={savings.length ? "Select an account" : "No savings account"} />
              </SelectTrigger>
              <SelectContent>
                {savings.map((a) => (
                  <SelectItem key={a.id} value={a.id}>
                    {accountName(a)}
                    <span className="ml-1 font-mono text-xs text-muted-foreground">
                      {formatMoney(a.balance)}
                    </span>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="goal-name">Goal name</Label>
            <Input
              id="goal-name"
              placeholder="e.g. New apartment"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={40}
            />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="goal-target">Target amount</Label>
            <div className="relative">
              <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
                $
              </span>
              <Input
                id="goal-target"
                inputMode="decimal"
                placeholder="20000.00"
                className="pl-7"
                value={target}
                onChange={(e) => setTarget(e.target.value)}
              />
            </div>
          </div>
        </div>

        <DialogFooter>
          <DialogClose className={cn(buttonVariants({ variant: "outline", size: "sm" }))}>
            Cancel
          </DialogClose>
          <Button size="sm" onClick={submit} disabled={setGoal.isPending}>
            {setGoal.isPending ? "Saving…" : "Save goal"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
