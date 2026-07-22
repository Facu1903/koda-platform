import type { CompanyRuntimeProfile } from './companyProfile';
import { PlatformApiError, requestJson } from '../api/platformHttp';

export class CompanyProfileApiError extends PlatformApiError {
  constructor(status: number, message: string, code: string | null = null) {
    super(status, message, code);
    this.name = 'CompanyProfileApiError';
  }
}

export async function fetchCompanyProfile(signal?: AbortSignal): Promise<CompanyRuntimeProfile> {
  try {
    return await requestJson<CompanyRuntimeProfile>('/api/v1/company/profile', {
      fallbackMessage: 'No se pudo cargar el perfil de empresa.',
      signal,
    });
  } catch (caught) {
    if (caught instanceof PlatformApiError) {
      throw new CompanyProfileApiError(caught.status, caught.message, caught.code);
    }

    throw caught;
  }
}
