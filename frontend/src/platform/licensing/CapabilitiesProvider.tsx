import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { PropsWithChildren } from 'react';
import type { ModuleCode, TenantCapabilities } from './capabilities';
import { isModuleEnabled } from './capabilities';
import { fetchTenantCapabilities } from './capabilitiesClient';

type CapabilitiesStatus = 'loading' | 'ready' | 'unavailable';

interface CapabilitiesContextValue {
  capabilities: TenantCapabilities | null;
  error: string | null;
  hasModule: (moduleCode: ModuleCode) => boolean;
  reload: () => void;
  status: CapabilitiesStatus;
}

const CapabilitiesContext = createContext<CapabilitiesContextValue | null>(null);

export function CapabilitiesProvider({ children }: PropsWithChildren) {
  const [capabilities, setCapabilities] = useState<TenantCapabilities | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [status, setStatus] = useState<CapabilitiesStatus>('loading');

  const reload = useCallback(() => {
    setCapabilities(null);
    setError(null);
    setStatus('loading');
    setReloadKey((current) => current + 1);
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    fetchTenantCapabilities(controller.signal)
      .then((loadedCapabilities) => {
        setCapabilities(loadedCapabilities);
        setStatus('ready');
      })
      .catch((caught: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        setCapabilities(null);
        setStatus('unavailable');
        setError(caught instanceof Error ? caught.message : 'No se pudieron cargar las capacidades del tenant.');
      });

    return () => controller.abort();
  }, [reloadKey]);

  const value = useMemo<CapabilitiesContextValue>(
    () => ({
      capabilities,
      error,
      hasModule: (moduleCode) => isModuleEnabled(capabilities, moduleCode),
      reload,
      status,
    }),
    [capabilities, error, reload, status],
  );

  return <CapabilitiesContext.Provider value={value}>{children}</CapabilitiesContext.Provider>;
}

export function useCapabilities(): CapabilitiesContextValue {
  const context = useContext(CapabilitiesContext);

  if (context === null) {
    throw new Error('useCapabilities must be used inside CapabilitiesProvider');
  }

  return context;
}
