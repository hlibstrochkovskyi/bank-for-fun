import { z } from "zod";

export const moneySchema = z.object({
  amount: z.string(),
  currency: z.string(),
  minorUnits: z.number(),
});
export type Money = z.infer<typeof moneySchema>;

export const accountSchema = z.object({
  id: z.string(),
  type: z.string(),
  nickname: z.string().nullable().optional(),
  accountNumber: z.string().nullable().optional(),
  currency: z.string(),
  status: z.string(),
  balance: moneySchema,
});
export type Account = z.infer<typeof accountSchema>;

export const transactionSchema = z.object({
  entryId: z.number(),
  postingId: z.string(),
  type: z.string(),
  amount: moneySchema,
  description: z.string().nullable(),
  merchant: z.string().nullable().optional(),
  category: z.string().nullable().optional(),
  createdAt: z.string(),
});
export type Transaction = z.infer<typeof transactionSchema>;

export const cardSchema = z.object({
  id: z.string(),
  accountId: z.string(),
  accountNickname: z.string().nullable().optional(),
  cardholder: z.string(),
  network: z.string(),
  last4: z.string(),
  expMonth: z.number(),
  expYear: z.number(),
  status: z.string(),
});
export type Card = z.infer<typeof cardSchema>;

// Customer view — no internal risk score or rule reasons.
export const goalSchema = z.object({
  id: z.string(),
  accountId: z.string(),
  accountNickname: z.string().nullable().optional(),
  name: z.string(),
  target: moneySchema,
  saved: moneySchema,
  pct: z.number(),
});
export type Goal = z.infer<typeof goalSchema>;

export const heldTransferSchema = z.object({
  id: z.string(),
  fromAccountId: z.string(),
  toAccountId: z.string(),
  amount: moneySchema,
  status: z.string(),
  createdAt: z.string(),
});
export type HeldTransfer = z.infer<typeof heldTransferSchema>;

// Admin review view — includes the risk score and reasons.
export const adminHeldTransferSchema = heldTransferSchema.extend({
  riskScore: z.number(),
  reason: z.string().nullable(),
});
export type AdminHeldTransfer = z.infer<typeof adminHeldTransferSchema>;

export const paymentResultSchema = z.object({
  status: z.string(),
  postingId: z.string().nullable(),
  heldTransferId: z.string().nullable(),
  balanceAfter: moneySchema.nullable(),
});
export type PaymentResult = z.infer<typeof paymentResultSchema>;

export const statementSchema = z.object({
  accountId: z.string(),
  from: z.string(),
  to: z.string(),
  openingBalance: moneySchema,
  closingBalance: moneySchema,
  totalCredits: moneySchema,
  totalDebits: moneySchema,
  transactions: z.array(transactionSchema),
});
export type Statement = z.infer<typeof statementSchema>;
