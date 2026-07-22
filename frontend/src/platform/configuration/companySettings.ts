import type {
  CompanyProfileBranding,
  CompanyProfileRegional,
  CompanyProfileTenant,
  CompanyRuntimeProfile,
  ThemeMode,
} from './companyProfile';

export interface CompanySettingsTenant extends CompanyProfileTenant {
  legalName: string;
}

export interface CompanySettingsBranding extends CompanyProfileBranding {
  themeMode: ThemeMode;
}

export type CompanySettingsRegional = CompanyProfileRegional;

export interface CompanySettings {
  id: string;
  tenant: CompanySettingsTenant;
  branding: CompanySettingsBranding;
  regional: CompanySettingsRegional;
  updatedAt: string;
  version: number;
}

export interface UpdateCompanySettingsRequest {
  dateFormat: string;
  defaultCurrency: string;
  defaultLocale: string;
  faviconUrl: string | null;
  currencyFormat: string;
  logoUrl: string | null;
  loginImageUrl: string | null;
  numberLocale: string;
  primaryColor: string;
  secondaryColor: string | null;
  themeMode: ThemeMode;
  timeFormat: string;
  timeZone: string;
  version: number;
}

export function companySettingsToRuntimeProfile(settings: CompanySettings): CompanyRuntimeProfile {
  return {
    tenant: {
      id: settings.tenant.id,
      commercialName: settings.tenant.commercialName,
      countryCode: settings.tenant.countryCode,
    },
    branding: settings.branding,
    regional: settings.regional,
    updatedAt: settings.updatedAt,
  };
}
