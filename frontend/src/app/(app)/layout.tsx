import { auth } from "@/auth";
import { AppShell } from "@/components/app/app-shell";

export default async function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await auth();
  return (
    <AppShell user={{ name: session?.user?.name, email: session?.user?.email }}>
      {children}
    </AppShell>
  );
}
