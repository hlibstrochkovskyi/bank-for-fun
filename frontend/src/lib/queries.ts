"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { z } from "zod";
import { api, newIdempotencyKey } from "./api";
import {
  accountSchema,
  heldTransferSchema,
  paymentResultSchema,
  statementSchema,
  transactionSchema,
} from "./schemas";

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

export function useHeldTransfers() {
  return useQuery({
    queryKey: ["held-transfers"],
    queryFn: () => api.get("held-transfers", z.array(heldTransferSchema)),
  });
}

function invalidateMoney(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: accountsKey });
  qc.invalidateQueries({ queryKey: ["held-transfers"] });
}

export function useOpenAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { type: string; currency: string }) =>
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
