import Image from "next/image";

/** Moldura das telas de sessão: card estreito e centrado, com a marca em cima. */
export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <div className="mx-auto flex w-full max-w-md flex-col px-6 py-14">
      <Image src="/logo-mark.png" alt="" width={48} height={48} className="mb-5 rounded-full" />
      <h1 className="text-2xl font-extrabold tracking-tight">{title}</h1>
      <p className="mt-1 mb-7 text-muted">{subtitle}</p>
      <div className="rounded-[18px] border border-line bg-card p-6 shadow-[var(--shadow)]">
        {children}
      </div>
    </div>
  );
}
