import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { AppProviders } from './providers/AppProviders';
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

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.localStorage.clear();
    window.location.hash = '';
  });

  it('renders navigation only for enabled modules', async () => {
    mockCapabilities(capabilities);

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    expect(screen.getByText('KODA PLATFORM')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Catalogos/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Ventas/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Compras/i })).not.toBeInTheDocument();
    expect(screen.getByText('Modulos sin licencia activa')).toBeInTheDocument();
  });

  it('blocks direct navigation to a disabled module route', async () => {
    window.location.hash = '#/compras';
    mockCapabilities(capabilities);

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByRole('heading', { name: 'Modulo no habilitado' })).toBeInTheDocument();
    expect(screen.getByText('Compras no tiene licencia activa para este tenant.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Compras/i })).not.toBeInTheDocument();
  });

  it('shows a controlled state when capabilities cannot be loaded', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: false,
          status: 401,
          json: () => Promise.resolve({ detail: 'No se pudo cargar la licencia efectiva.' }),
        }),
      ) as unknown as typeof fetch,
    );

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(await screen.findByRole('heading', { name: 'Licencia no disponible' })).toBeInTheDocument();
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

function mockCapabilities(response: TenantCapabilities) {
  vi.stubGlobal(
    'fetch',
    vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve(response),
      }),
    ) as unknown as typeof fetch,
  );
}
