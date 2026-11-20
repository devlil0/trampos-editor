/** Prazo para conexão e leitura da resposta, sem repetir mutações. */
export function fetchWithTimeout(url: string, options: RequestInit = {}): Promise<Response> {
  const deadline = AbortSignal.timeout(8_000);
  const signal = options.signal ? AbortSignal.any([options.signal, deadline]) : deadline;
  return fetch(url, { ...options, signal });
}
