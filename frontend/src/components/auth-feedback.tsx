"use client";

import { useEffect, useRef, useState, type ComponentProps } from "react";

/** Anuncia cada resposta e permite encontrá-la pelo teclado após a ação. */
export function AuthFeedback({ state }: { state: { error?: string; notice?: string } }) {
  const ref = useRef<HTMLParagraphElement>(null);
  useEffect(() => {
    if (state.error || state.notice) ref.current?.focus();
  }, [state]);

  const message = state.error ?? state.notice;
  if (!message) return null;
  return (
    <p
      ref={ref}
      tabIndex={-1}
      role={state.error ? "alert" : "status"}
      className={state.error
        ? "rounded-xl border border-busy/40 bg-busy/10 px-3.5 py-2.5 text-sm text-busy"
        : "rounded-xl border border-line bg-panel px-3.5 py-2.5 text-sm text-muted"}
    >
      {message}
    </p>
  );
}

/** O chamador usa retryAt como key para reiniciar a contagem a cada novo bloqueio. */
export function AuthSubmitButton({
  pending, retryAt, children, disabled, className, ...props
}: ComponentProps<"button"> & { pending: boolean; retryAt?: number }) {
  const [seconds, setSeconds] = useState(() =>
    retryAt ? Math.max(0, Math.ceil((retryAt - Date.now()) / 1000)) : 0,
  );
  useEffect(() => {
    if (!retryAt) return;
    const timer = setInterval(() => {
      const remaining = Math.max(0, Math.ceil((retryAt - Date.now()) / 1000));
      setSeconds(remaining);
      if (remaining === 0) clearInterval(timer);
    }, 1000);
    return () => clearInterval(timer);
  }, [retryAt]);

  return (
    <button
      type="submit"
      {...props}
      disabled={disabled || pending || seconds > 0}
      className={className ?? "btn btn-primary btn-block btn-lg disabled:opacity-60"}
    >
      {pending ? "Um instante…" : seconds > 0 ? `Tente novamente em ${seconds}s` : children}
    </button>
  );
}
