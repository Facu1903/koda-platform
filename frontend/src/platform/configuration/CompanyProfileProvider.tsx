import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { PropsWithChildren } from 'react';
import type { CompanyRuntimeProfile } from './companyProfile';
import { effectiveCompanyProfile } from './companyProfile';
import { fetchCompanyProfile } from './companyProfileClient';

type CompanyProfileStatus = 'loading' | 'ready' | 'unavailable';

interface CompanyProfileContextValue {
  effectiveProfile: CompanyRuntimeProfile;
  error: string | null;
  profile: CompanyRuntimeProfile | null;
  reload: () => void;
  status: CompanyProfileStatus;
}

const CompanyProfileContext = createContext<CompanyProfileContextValue | null>(null);

export function CompanyProfileProvider({ children }: PropsWithChildren) {
  const [error, setError] = useState<string | null>(null);
  const [profile, setProfile] = useState<CompanyRuntimeProfile | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [status, setStatus] = useState<CompanyProfileStatus>('loading');

  const reload = useCallback(() => {
    setError(null);
    setProfile(null);
    setStatus('loading');
    setReloadKey((current) => current + 1);
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    fetchCompanyProfile(controller.signal)
      .then((loadedProfile) => {
        setError(null);
        setProfile(loadedProfile);
        setStatus('ready');
      })
      .catch((caught: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        setProfile(null);
        setStatus('unavailable');
        setError(caught instanceof Error ? caught.message : 'No se pudo cargar el perfil de empresa.');
      });

    return () => controller.abort();
  }, [reloadKey]);

  const value = useMemo<CompanyProfileContextValue>(
    () => ({
      effectiveProfile: effectiveCompanyProfile(profile),
      error,
      profile,
      reload,
      status,
    }),
    [error, profile, reload, status],
  );

  return <CompanyProfileContext.Provider value={value}>{children}</CompanyProfileContext.Provider>;
}

export function useCompanyProfile(): CompanyProfileContextValue {
  const context = useContext(CompanyProfileContext);

  if (context === null) {
    throw new Error('useCompanyProfile must be used inside CompanyProfileProvider');
  }

  return context;
}
