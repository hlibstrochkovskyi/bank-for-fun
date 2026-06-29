"use client";

import {
  Area,
  AreaChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { TrendPoint } from "@/lib/trend";

function currency(value: number) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0,
  }).format(value);
}

export function BalanceTrendChart({
  data,
  variant = "light",
  height = 200,
  showAxis = true,
}: {
  data: TrendPoint[];
  variant?: "light" | "onDark";
  height?: number;
  showAxis?: boolean;
}) {
  const onDark = variant === "onDark";
  const stroke = onDark ? "var(--color-gold)" : "var(--color-chart-1)";
  const tick = onDark ? "oklch(0.82 0.02 90 / 0.7)" : "var(--color-muted-foreground)";
  const gradId = onDark ? "balanceFillDark" : "balanceFill";

  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={data} margin={{ top: 8, right: 4, left: 4, bottom: 0 }}>
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={stroke} stopOpacity={onDark ? 0.35 : 0.28} />
            <stop offset="100%" stopColor={stroke} stopOpacity={0} />
          </linearGradient>
        </defs>
        {showAxis && (
          <XAxis
            dataKey="label"
            tickLine={false}
            axisLine={false}
            minTickGap={40}
            tick={{ fontSize: 11, fill: tick }}
          />
        )}
        <YAxis hide domain={["dataMin - 100", "dataMax + 100"]} />
        <Tooltip
          cursor={{ stroke: onDark ? "oklch(1 0 0 / 0.2)" : "var(--color-border)", strokeWidth: 1 }}
          contentStyle={{
            borderRadius: 12,
            border: "1px solid var(--color-border)",
            background: "var(--color-popover)",
            color: "var(--color-popover-foreground)",
            fontSize: 12,
            boxShadow: "0 4px 16px rgb(0 0 0 / 0.08)",
          }}
          labelStyle={{ color: "var(--color-muted-foreground)" }}
          formatter={(value) => [currency(Number(value)), "Balance"]}
        />
        <Area
          type="monotone"
          dataKey="value"
          stroke={stroke}
          strokeWidth={2}
          fill={`url(#${gradId})`}
          dot={false}
          activeDot={{ r: 4, strokeWidth: 0 }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
