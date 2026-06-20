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
  createdAt: z.string(),
});
export type Transaction = z.infer<typeof transactionSchema>;

export const heldTransferSchema = z.object({
  id: z.string(),
  fromAccountId: z.string(),
  toAccountId: z.string(),
  amount: moneySchema,
  riskScore: z.number(),
  reason: z.string().nullable(),
  status: z.string(),
  createdAt: z.string(),
});
export type HeldTransfer = z.infer<typeof heldTransferSchema>;

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
