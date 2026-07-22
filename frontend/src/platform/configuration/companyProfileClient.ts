import type { CompanyRuntimeProfile } from './companyProfile';

export class CompanyProfileApiError extends Error {
  readonly code: string | null;
  readonly status: number;

  constructor(status: number, message: string, code: string | null = null) {
    super(message);
    this.name = 'CompanyProfileApiError';
    this.status = status;
    this.code = code;
  }
}

interface ProblemDetail {
  code?: string;
  detail?: string;
  title?: string;
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

export async function fetchCompanyProfile(signal?: AbortSignal): Promise<CompanyRuntimeProfile> {
  const token = readAccessToken();
  const headers = new Headers({ Accept: 'application/json' });

  if (token !== null) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch('/api/v1/company/profile', {
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
      ? problem.detail ?? problem.title ?? 'No se pudo cargar el perfil de empresa.'
      : 'No se pudo cargar el perfil de empresa.';

    throw new CompanyProfileApiError(response.status, message, isProblemDetail(problem) ? problem.code ?? null : null);
  }

  return response.json() as Promise<CompanyRuntimeProfile>;
}
