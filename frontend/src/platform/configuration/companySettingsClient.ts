import { PlatformApiError, requestJson } from '../api/platformHttp';
import type { CompanySettings, UpdateCompanySettingsRequest } from './companySettings';

export class CompanySettingsApiError extends PlatformApiError {
  constructor(status: number, message: string, code: string | null = null) {
    super(status, message, code);
    this.name = 'CompanySettingsApiError';
  }
}

export async function fetchCompanySettings(signal?: AbortSignal): Promise<CompanySettings> {
  try {
    return await requestJson<CompanySettings>('/api/v1/company/settings', {
      fallbackMessage: 'No se pudo cargar la configuracion de empresa.',
      signal,
    });
  } catch (caught) {
    if (caught instanceof PlatformApiError) {
      throw new CompanySettingsApiError(caught.status, caught.message, caught.code);
    }

    throw caught;
  }
}

export async function updateCompanySettings(
  request: UpdateCompanySettingsRequest,
  signal?: AbortSignal,
): Promise<CompanySettings> {
  try {
    return await requestJson<CompanySettings>('/api/v1/company/settings', {
      body: request,
      fallbackMessage: 'No se pudo actualizar la configuracion de empresa.',
      method: 'PUT',
      signal,
    });
  } catch (caught) {
    if (caught instanceof PlatformApiError) {
      throw new CompanySettingsApiError(caught.status, caught.message, caught.code);
    }

    throw caught;
  }
}
