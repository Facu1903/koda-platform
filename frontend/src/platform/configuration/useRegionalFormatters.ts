import { useMemo } from 'react';
import { useCompanyProfile } from './CompanyProfileProvider';
import { createRegionalFormatters } from './regionalFormatting';

export function useRegionalFormatters() {
  const { effectiveProfile } = useCompanyProfile();
  const regional = effectiveProfile.regional;

  return useMemo(() => createRegionalFormatters(regional), [regional]);
}
