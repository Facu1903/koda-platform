import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { AppProviders } from './providers/AppProviders';
import type { CompanySettings, UpdateCompanySettingsRequest } from '../platform/configuration/companySettings';
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

const companySettings: CompanySettings = {
  id: '30000000-0000-4000-8000-000000000001',
  tenant: {
    id: '00000000-0000-4000-8000-000000000001',
    commercialName: 'KODA Retail',
    legalName: 'KODA Retail SA',
    countryCode: 'UY',
  },
  branding: companyProfile.branding,
  regional: companyProfile.regional,
  updatedAt: '2026-07-22T12:00:00Z',
  version: 1,
};

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.querySelectorAll('link[rel="icon"]').forEach((link) => link.remove());
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
    expect(screen.getByText('21/07/2026 09:00')).toBeInTheDocument();
    const navigation = screen.getByLabelText('Navegacion de modulos');
    expect(within(navigation).getByText('Catalogos')).toBeInTheDocument();
    expect(within(navigation).getByText('Ventas')).toBeInTheDocument();
    expect(within(navigation).queryByText('Compras')).not.toBeInTheDocument();
    expect(screen.getByText('Modulos sin licencia activa')).toBeInTheDocument();
  });

  it('renders safe tenant logo and applies favicon from the runtime profile', async () => {
    const visualProfile: CompanyRuntimeProfile = {
      ...companyProfile,
      branding: {
        ...companyProfile.branding,
        faviconUrl: 'https://cdn.example.com/favicon.ico',
        logoUrl: 'https://cdn.example.com/logo.png',
      },
    };
    mockPlatformApi({ capabilitiesResponse: capabilities, profileResponse: visualProfile });

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByAltText('KODA Retail logo')).toHaveAttribute('src', 'https://cdn.example.com/logo.png');
    expect(document.querySelector<HTMLLinkElement>('link[rel="icon"]')?.href).toBe('https://cdn.example.com/favicon.ico');
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

  it('allows updating company settings from the configuration module', async () => {
    window.location.hash = '#/configuracion';
    const updatedSettings: CompanySettings = {
      ...companySettings,
      branding: {
        ...companySettings.branding,
        primaryColor: '#F6862B',
      },
      regional: {
        ...companySettings.regional,
        defaultCurrency: 'USD',
        defaultLocale: 'en-US',
        numberLocale: 'en-US',
      },
      updatedAt: '2026-07-22T13:00:00Z',
      version: 2,
    };
    const api = mockPlatformApi({
      capabilitiesResponse: capabilities,
      profileResponse: companyProfile,
      settingsResponse: companySettings,
      updateSettingsResponse: updatedSettings,
    });

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByText('Configuracion de empresa')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Color primario'), { target: { value: '#F6862B' } });
    fireEvent.change(screen.getByLabelText('Locale'), { target: { value: 'en-US' } });
    fireEvent.change(screen.getByLabelText('Locale numerico'), { target: { value: 'en-US' } });
    fireEvent.change(screen.getByLabelText('Moneda'), { target: { value: 'USD' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(api.latestSettingsRequest?.primaryColor).toBe('#F6862B'));
    expect(api.latestSettingsRequest?.defaultLocale).toBe('en-US');
    expect(api.latestSettingsRequest?.defaultCurrency).toBe('USD');
    expect(await screen.findByText('Configuracion guardada.')).toBeInTheDocument();
    expect(screen.getByText('Version 2')).toBeInTheDocument();
    expect(screen.getByText('KODA ERP - KODA Retail - en-US - USD')).toBeInTheDocument();
  }, 15000);

  it('shows a restricted state when company settings cannot be read', async () => {
    window.location.hash = '#/configuracion';
    mockPlatformApi({
      capabilitiesResponse: capabilities,
      profileResponse: companyProfile,
      settingsStatus: 403,
      settingsProblem: { detail: 'No tenes permiso para leer la configuracion.' },
    });

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByText('Acceso restringido')).toBeInTheDocument();
    expect(screen.getByText('No tenes permiso para leer la configuracion.')).toBeInTheDocument();
  }, 15000);

  it('shows a clear conflict when company settings were changed elsewhere', async () => {
    window.location.hash = '#/configuracion';
    mockPlatformApi({
      capabilitiesResponse: capabilities,
      profileResponse: companyProfile,
      settingsResponse: companySettings,
      updateSettingsStatus: 409,
      updateSettingsProblem: { detail: 'La configuracion fue modificada por otra sesion.' },
    });

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByText('Configuracion de empresa')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Color primario'), { target: { value: '#F6862B' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Conflicto de version. Recarga la configuracion antes de guardar nuevos cambios.')).toBeInTheDocument();
  }, 15000);

  it('blocks unsafe visual asset urls in company settings', async () => {
    window.location.hash = '#/configuracion';
    mockPlatformApi({
      capabilitiesResponse: capabilities,
      profileResponse: companyProfile,
      settingsResponse: companySettings,
    });

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByText('Configuracion de empresa')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Logo URL'), { target: { value: 'javascript:alert(1)' } });

    expect(await screen.findByText('Usar URL https valida.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  }, 15000);
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
  settingsProblem,
  settingsResponse,
  settingsStatus = 200,
  updateSettingsProblem,
  updateSettingsResponse,
  updateSettingsStatus = 200,
}: {
  capabilitiesResponse?: TenantCapabilities;
  capabilitiesStatus?: number;
  profileResponse?: CompanyRuntimeProfile;
  profileStatus?: number;
  settingsProblem?: { detail?: string; title?: string; code?: string };
  settingsResponse?: CompanySettings;
  settingsStatus?: number;
  updateSettingsProblem?: { detail?: string; title?: string; code?: string };
  updateSettingsResponse?: CompanySettings;
  updateSettingsStatus?: number;
}) {
  const controls = {
    latestSettingsRequest: null as UpdateCompanySettingsRequest | null,
  };

  vi.stubGlobal(
    'fetch',
    vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === 'string' ? input : 'url' in input ? input.url : input.toString();
      const method = init?.method ?? 'GET';

      if (url.endsWith('/api/v1/company/profile') && method === 'GET') {
        return Promise.resolve(jsonResponse(profileStatus, profileResponse, { detail: 'No se pudo cargar el perfil de empresa.' }));
      }

      if (url.endsWith('/api/v1/capabilities') && method === 'GET') {
        return Promise.resolve(
          jsonResponse(capabilitiesStatus, capabilitiesResponse, { detail: 'No se pudo cargar la licencia efectiva.' }),
        );
      }

      if (url.endsWith('/api/v1/company/settings') && method === 'GET') {
        return Promise.resolve(
          jsonResponse(settingsStatus, settingsResponse, settingsProblem ?? { detail: 'No se pudo cargar la configuracion.' }),
        );
      }

      if (url.endsWith('/api/v1/company/settings') && method === 'PUT') {
        if (typeof init?.body === 'string') {
          controls.latestSettingsRequest = JSON.parse(init.body) as UpdateCompanySettingsRequest;
        }

        return Promise.resolve(
          jsonResponse(
            updateSettingsStatus,
            updateSettingsResponse ?? settingsResponse,
            updateSettingsProblem ?? { detail: 'No se pudo actualizar la configuracion.' },
          ),
        );
      }

      return Promise.reject(new Error(`Unexpected fetch URL: ${url}`));
    }) as unknown as typeof fetch,
  );

  return controls;
}

function jsonResponse(status: number, data: unknown, problem: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(status >= 200 && status < 300 ? data : problem),
  };
}
