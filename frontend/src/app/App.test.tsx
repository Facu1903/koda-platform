import { render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { AppProviders } from './providers/AppProviders';
import type { CompanyRuntimeProfile } from '../platform/configuration/companyProfile';
import type { TenantCapabilities } from '../platform/licensing/capabilities';

const capabilities: TenantCapabilities = {
  tenantId: '00000000-0000-4000-8000-000000000001',
  tenantActive: true,
  calculatedAt: '2026-07-21T12:00:00Z',
  products: [
    {
      id: '10000000-0000-4000-8000-000000000001',
      code: 'KODA_ERP',
      name: 'KODA ERP',
      enabled: true,
      entitlementStatus: 'ACTIVE',
      entitlementValidFrom: '2026-07-20T00:00:00Z',
      entitlementValidUntil: null,
      subscriptionId: '20000000-0000-4000-8000-000000000001',
      subscriptionStatus: 'ACTIVE',
      subscriptionValidFrom: '2026-07-20T00:00:00Z',
      subscriptionValidUntil: null,
      planCode: 'KODA_PILOT',
      planName: 'KODA Pilot',
      modules: [
        enabledModule('CONFIGURATION', 'Configuration'),
        enabledModule('CATALOGS', 'Catalogs'),
        enabledModule('SALES', 'Sales'),
        disabledModule('PURCHASES', 'Purchases'),
      ],
    },
  ],
  featureFlags: [],
  limits: [],
};

const companyProfile: CompanyRuntimeProfile = {
  tenant: {
    id: '00000000-0000-4000-8000-000000000001',
    commercialName: 'KODA Retail',
    countryCode: 'UY',
  },
  branding: {
    logoUrl: null,
    faviconUrl: null,
    loginImageUrl: null,
    primaryColor: '#2BA84A',
    secondaryColor: '#111111',
    themeMode: 'light',
  },
  regional: {
    defaultLocale: 'es-UY',
    defaultCurrency: 'UYU',
    timeZone: 'America/Montevideo',
    dateFormat: 'dd/MM/yyyy',
    timeFormat: 'HH:mm',
    numberLocale: 'es-UY',
    currencyFormat: 'symbol',
  },
  updatedAt: '2026-07-22T12:00:00Z',
};

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.localStorage.clear();
    window.location.hash = '';
  });

  it('renders navigation only for enabled modules', async () => {
    mockPlatformApi({ capabilitiesResponse: capabilities, profileResponse: companyProfile });

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByText('Modulos disponibles')).toBeInTheDocument();
    expect(screen.getByText('KODA PLATFORM')).toBeInTheDocument();
    expect(screen.getByText('KODA ERP - KODA Retail - es-UY - UYU')).toBeInTheDocument();
    expect(screen.getByText('Estado operativo del tenant KODA Retail.')).toBeInTheDocument();
    const navigation = screen.getByLabelText('Navegacion de modulos');
    expect(within(navigation).getByText('Catalogos')).toBeInTheDocument();
    expect(within(navigation).getByText('Ventas')).toBeInTheDocument();
    expect(within(navigation).queryByText('Compras')).not.toBeInTheDocument();
    expect(screen.getByText('Modulos sin licencia activa')).toBeInTheDocument();
  });

  it('blocks direct navigation to a disabled module route', async () => {
    window.location.hash = '#/compras';
    mockPlatformApi({ capabilitiesResponse: capabilities, profileResponse: companyProfile });

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByText('Modulo no habilitado')).toBeInTheDocument();
    expect(screen.getByText('Compras no tiene licencia activa para este tenant.')).toBeInTheDocument();
    const navigation = screen.getByLabelText('Navegacion de modulos');
    expect(within(navigation).queryByText('Compras')).not.toBeInTheDocument();
  });

  it('shows a controlled state when capabilities cannot be loaded', async () => {
    mockPlatformApi({ capabilitiesStatus: 401, profileResponse: companyProfile });

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByText('Licencia no disponible')).toBeInTheDocument();
    expect(screen.getByText('No se pudo cargar la licencia efectiva.')).toBeInTheDocument();
  });
});

function enabledModule(code: string, name: string) {
  return moduleCapability(code, name, true, 'ACTIVE');
}

function disabledModule(code: string, name: string) {
  return moduleCapability(code, name, false, 'SUSPENDED');
}

function moduleCapability(code: string, name: string, enabled: boolean, entitlementStatus: string) {
  return {
    id: `10000000-0000-4000-8000-${code.padStart(12, '0').slice(0, 12)}`,
    productCode: 'KODA_ERP',
    code,
    name,
    enabled,
    coreModule: code === 'CONFIGURATION',
    commerciallyToggleable: code !== 'CONFIGURATION',
    entitlementStatus,
    validFrom: '2026-07-20T00:00:00Z',
    validUntil: null,
  };
}

function mockPlatformApi({
  capabilitiesResponse,
  capabilitiesStatus = 200,
  profileResponse,
  profileStatus = 200,
}: {
  capabilitiesResponse?: TenantCapabilities;
  capabilitiesStatus?: number;
  profileResponse?: CompanyRuntimeProfile;
  profileStatus?: number;
}) {
  vi.stubGlobal(
    'fetch',
    vi.fn((input: RequestInfo | URL) => {
      const url = typeof input === 'string' ? input : 'url' in input ? input.url : input.toString();

      if (url.endsWith('/api/v1/company/profile')) {
        return Promise.resolve({
          ok: profileStatus >= 200 && profileStatus < 300,
          status: profileStatus,
          json: () =>
            Promise.resolve(
              profileStatus >= 200 && profileStatus < 300
                ? profileResponse
                : { detail: 'No se pudo cargar el perfil de empresa.' },
            ),
        });
      }

      if (url.endsWith('/api/v1/capabilities')) {
        return Promise.resolve({
          ok: capabilitiesStatus >= 200 && capabilitiesStatus < 300,
          status: capabilitiesStatus,
          json: () =>
            Promise.resolve(
              capabilitiesStatus >= 200 && capabilitiesStatus < 300
                ? capabilitiesResponse
                : { detail: 'No se pudo cargar la licencia efectiva.' },
            ),
        });
      }

      return Promise.reject(new Error(`Unexpected fetch URL: ${url}`));
    }) as unknown as typeof fetch,
  );
}
