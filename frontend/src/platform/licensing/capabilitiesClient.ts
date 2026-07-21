import type { TenantCapabilities } from './capabilities';

export class CapabilitiesApiError extends Error {
  readonly status: number;
  readonly code: string | null;

  constructor(status: number, message: string, code: string | null = null) {
    super(message);
    this.name = 'CapabilitiesApiError';
    this.status = status;
    this.code = code;
  }
}

interface ProblemDetail {
  detail?: string;
  title?: string;
  code?: string;
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

export async function fetchTenantCapabilities(signal?: AbortSignal): Promise<TenantCapabilities> {
  const token = readAccessToken();
  const headers = new Headers({ Accept: 'application/json' });

  if (token !== null) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch('/api/v1/capabilities', {
    credentials: 'include',
    headers,
    signal,
  });

  if (!response.ok) {
    let problem: unknown;

    try {
      problem = await response.json();
    } catch {
      problem = undefined;
    }

    const message = isProblemDetail(problem)
      ? problem.detail ?? problem.title ?? 'No se pudo cargar la licencia efectiva.'
      : 'No se pudo cargar la licencia efectiva.';

    throw new CapabilitiesApiError(response.status, message, isProblemDetail(problem) ? problem.code ?? null : null);
  }

  return response.json() as Promise<TenantCapabilities>;
}
