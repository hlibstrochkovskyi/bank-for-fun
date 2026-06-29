"use client";

import { useState } from "react";
import { CreditCard } from "lucide-react";
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
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAccounts, useIssueCard } from "@/lib/queries";
import { accountName } from "@/lib/labels";
import { maskAccountNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

export function IssueCardDialog() {
  const { data: accounts } = useAccounts();
  const issue = useIssueCard();
  const [open, setOpen] = useState(false);
  const [accountId, setAccountId] = useState("");

  function submit() {
    if (!accountId) {
      toast.error("Choose an account for the card.");
      return;
    }
    issue.mutate(accountId, {
      onSuccess: () => {
        toast.success("Card issued.");
        setOpen(false);
        setAccountId("");
      },
      onError: (e) => toast.error(e.message || "Couldn’t issue the card."),
    });
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger className={cn(buttonVariants({ size: "sm" }))}>
        <CreditCard className="size-4" />
        Issue card
      </DialogTrigger>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Issue a new card</DialogTitle>
          <DialogDescription>
            A card is bound to one account; spending posts to that account’s ledger.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-2 py-2">
          <Label htmlFor="card-account">Account</Label>
          <Select value={accountId} onValueChange={(v) => setAccountId(v ?? "")}>
            <SelectTrigger id="card-account" className="w-full">
              <SelectValue placeholder="Select an account" />
            </SelectTrigger>
            <SelectContent>
              {(accounts ?? []).map((a) => (
                <SelectItem key={a.id} value={a.id}>
                  {accountName(a)}
                  <span className="ml-1 font-mono text-xs text-muted-foreground">
                    {maskAccountNumber(a.accountNumber)}
                  </span>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <DialogFooter>
          <DialogClose className={cn(buttonVariants({ variant: "outline", size: "sm" }))}>
            Cancel
          </DialogClose>
          <Button size="sm" onClick={submit} disabled={issue.isPending}>
            {issue.isPending ? "Issuing…" : "Issue card"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
