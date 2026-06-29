"use client";

import {
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { z } from "zod";
import { api, newIdempotencyKey } from "./api";
import {
  type Account,
  type Transaction,
  accountSchema,
  adminHeldTransferSchema,
  cardSchema,
  heldTransferSchema,
  paymentResultSchema,
  statementSchema,
  transactionSchema,
} from "./schemas";

export type ActivityItem = Transaction & { accountId: string };

/** Merge recent transactions across all of a user's accounts, newest first. */
export function useRecentActivity(accounts: Account[] | undefined, limit = 25) {
  const list = accounts ?? [];
  const results = useQueries({
    queries: list.map((account) => ({
      queryKey: ["accounts", account.id, "transactions", limit],
      queryFn: () =>
        api.get(
          `accounts/${account.id}/transactions?limit=${limit}`,
          z.array(transactionSchema),
        ),
    })),
  });

  const isLoading = results.some((r) => r.isLoading);
  const transactions: ActivityItem[] = results
    .flatMap((r, i) =>
      (r.data ?? []).map((t) => ({ ...t, accountId: list[i].id })),
    )
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt));

  return { transactions, isLoading };
}

const accountsKey = ["accounts"] as const;

export function useAccounts() {
  return useQuery({
    queryKey: accountsKey,
    queryFn: () => api.get("accounts", z.array(accountSchema)),
  });
}

export function useAccount(accountId: string | undefined) {
  return useQuery({
    queryKey: ["accounts", accountId],
    queryFn: () => api.get(`accounts/${accountId}`, accountSchema),
    enabled: Boolean(accountId),
  });
}

export function useTransactions(accountId: string | undefined, limit = 50) {
  return useQuery({
    queryKey: ["accounts", accountId, "transactions", limit],
    queryFn: () =>
      api.get(
        `accounts/${accountId}/transactions?limit=${limit}`,
        z.array(transactionSchema),
      ),
    enabled: Boolean(accountId),
  });
}

export function useStatement(
  accountId: string | undefined,
  from: string,
  to: string,
) {
  return useQuery({
    queryKey: ["accounts", accountId, "statement", from, to],
    queryFn: () =>
      api.get(`accounts/${accountId}/statement?from=${from}&to=${to}`, statementSchema),
    enabled: Boolean(accountId && from && to),
  });
}

export function useCards() {
  return useQuery({
    queryKey: ["cards"],
    queryFn: () => api.get("cards", z.array(cardSchema)),
  });
}

export function useIssueCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (accountId: string) =>
      api.post(`accounts/${accountId}/cards`, cardSchema, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["cards"] }),
  });
}

export function useHeldTransfers() {
  return useQuery({
    queryKey: ["held-transfers"],
    queryFn: () => api.get("held-transfers", z.array(heldTransferSchema)),
  });
}

const adminQueueKey = ["admin", "held-transfers"] as const;

export function useAdminHeldTransfers() {
  return useQuery({
    queryKey: adminQueueKey,
    queryFn: () => api.get("admin/held-transfers", z.array(adminHeldTransferSchema)),
  });
}

export function useReviewHeld() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: string; action: "release" | "reject" }) =>
      api.post(`admin/held-transfers/${input.id}/${input.action}`, z.unknown(), {}),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminQueueKey });
      invalidateMoney(qc);
    },
  });
}

function invalidateMoney(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: accountsKey });
  qc.invalidateQueries({ queryKey: ["held-transfers"] });
}

export function useOpenAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { type: string; currency: string; nickname?: string }) =>
      api.post("accounts", accountSchema, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: accountsKey }),
  });
}

export function useDeposit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      accountId: string;
      amount: string;
      currency: string;
      description?: string;
    }) =>
      api.post(
        `accounts/${input.accountId}/deposits`,
        paymentResultSchema,
        { amount: input.amount, currency: input.currency, description: input.description },
        newIdempotencyKey(),
      ),
    onSuccess: () => invalidateMoney(qc),
  });
}

export function useWithdraw() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      accountId: string;
      amount: string;
      currency: string;
      description?: string;
    }) =>
      api.post(
        `accounts/${input.accountId}/withdrawals`,
        paymentResultSchema,
        { amount: input.amount, currency: input.currency, description: input.description },
        newIdempotencyKey(),
      ),
    onSuccess: () => invalidateMoney(qc),
  });
}

export function useTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      fromAccountId: string;
      toAccountId: string;
      amount: string;
      currency: string;
      description?: string;
    }) => api.post("transfers", paymentResultSchema, input, newIdempotencyKey()),
    onSuccess: () => invalidateMoney(qc),
  });
}
