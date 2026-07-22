import type { TenantCapabilities } from './capabilities';
import { PlatformApiError, requestJson } from '../api/platformHttp';

export class CapabilitiesApiError extends PlatformApiError {
  constructor(status: number, message: string, code: string | null = null) {
    super(status, message, code);
    this.name = 'CapabilitiesApiError';
  }
}

export async function fetchTenantCapabilities(signal?: AbortSignal): Promise<TenantCapabilities> {
  try {
    return await requestJson<TenantCapabilities>('/api/v1/capabilities', {
      fallbackMessage: 'No se pudo cargar la licencia efectiva.',
      signal,
    });
  } catch (caught) {
    if (caught instanceof PlatformApiError) {
      throw new CapabilitiesApiError(caught.status, caught.message, caught.code);
    }

    throw caught;
  }
}
