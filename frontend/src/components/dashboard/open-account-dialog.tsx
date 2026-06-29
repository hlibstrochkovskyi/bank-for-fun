"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
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
import { useOpenAccount } from "@/lib/queries";
import { accountMeta } from "@/lib/labels";
import { cn } from "@/lib/utils";

export function OpenAccountDialog({ variant = "default" }: { variant?: "default" | "ghost" }) {
  const [open, setOpen] = useState(false);
  const [type, setType] = useState("CHECKING");
  const [nickname, setNickname] = useState("");
  const openAccount = useOpenAccount();

  function submit() {
    openAccount.mutate(
      { type, currency: "USD", nickname: nickname.trim() || undefined },
      {
        onSuccess: (account) => {
          toast.success(`${accountMeta(account.type).label} account opened`);
          setOpen(false);
          setType("CHECKING");
          setNickname("");
        },
        onError: (e) => toast.error(e.message || "Couldn’t open the account"),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger
        className={cn(buttonVariants({ variant, size: "sm" }))}
      >
        <Plus className="size-4" />
        Open account
      </DialogTrigger>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Open a new account</DialogTitle>
          <DialogDescription>
            Choose a type. Accounts start at a zero balance.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-2 py-2">
          <Label htmlFor="account-type">Account type</Label>
          <Select value={type} onValueChange={(v) => setType(v ?? "CHECKING")}>
            <SelectTrigger id="account-type" className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="CHECKING">Checking</SelectItem>
              <SelectItem value="SAVINGS">Savings</SelectItem>
            </SelectContent>
          </Select>

          <Label htmlFor="account-nickname" className="mt-2">
            Nickname <span className="font-normal text-muted-foreground">(optional)</span>
          </Label>
          <Input
            id="account-nickname"
            placeholder="e.g. Travel Fund"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            maxLength={40}
          />
        </div>

        <DialogFooter>
          <DialogClose className={cn(buttonVariants({ variant: "outline", size: "sm" }))}>
            Cancel
          </DialogClose>
          <Button size="sm" onClick={submit} disabled={openAccount.isPending}>
            {openAccount.isPending ? "Opening…" : "Open account"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
