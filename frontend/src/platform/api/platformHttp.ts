export class PlatformApiError extends Error {
  readonly code: string | null;
  readonly status: number;

  constructor(status: number, message: string, code: string | null = null) {
    super(message);
    this.name = 'PlatformApiError';
    this.status = status;
    this.code = code;
  }
}

interface ProblemDetail {
  code?: string;
  detail?: string;
  title?: string;
}

interface JsonRequestOptions {
  body?: unknown;
  fallbackMessage: string;
  method?: 'GET' | 'PUT';
  signal?: AbortSignal;
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  return typeof value === 'object' && value !== null;
}

function readAccessToken(): string | null {
  const storageKeys = ['koda.accessToken', 'koda.auth.accessToken', 'accessToken'];

  for (const key of storageKeys) {
    const value = window.localStorage.getItem(key);
    if (value !== null && value.trim() !== '') {
      return value;
    }
  }

  return null;
}

export async function requestJson<T>(url: string, options: JsonRequestOptions): Promise<T> {
  const token = readAccessToken();
  const headers = new Headers({ Accept: 'application/json' });

  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }

  if (token !== null) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(url, {
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    credentials: 'include',
    headers,
    method: options.method ?? 'GET',
    signal: options.signal,
  });

  if (!response.ok) {
    let problem: unknown;

    try {
      problem = await response.json();
    } catch {
      problem = undefined;
    }

    const message = isProblemDetail(problem)
      ? problem.detail ?? problem.title ?? options.fallbackMessage
      : options.fallbackMessage;

    throw new PlatformApiError(response.status, message, isProblemDetail(problem) ? problem.code ?? null : null);
  }

  return response.json() as Promise<T>;
}
