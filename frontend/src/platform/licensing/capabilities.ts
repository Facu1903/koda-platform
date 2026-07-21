export type ModuleCode =
  | 'AUDIT'
  | 'CASH'
  | 'CATALOGS'
  | 'COMMERCIAL_PARTNERS'
  | 'COMMERCIAL_REPORTS'
  | 'CONFIGURATION'
  | 'PURCHASES'
  | 'SALES'
  | 'STOCK';

export type ProductCode = 'KODA_ERP';

export interface ModuleCapability {
  id: string;
  productCode: ProductCode | string;
  code: ModuleCode | string;
  name: string;
  enabled: boolean;
  coreModule: boolean;
  commerciallyToggleable: boolean;
  entitlementStatus: string;
  validFrom: string | null;
  validUntil: string | null;
}

export interface ProductCapability {
  id: string;
  code: ProductCode | string;
  name: string;
  enabled: boolean;
  entitlementStatus: string;
  entitlementValidFrom: string | null;
  entitlementValidUntil: string | null;
  subscriptionId: string;
  subscriptionStatus: string;
  subscriptionValidFrom: string | null;
  subscriptionValidUntil: string | null;
  planCode: string;
  planName: string;
  modules: ModuleCapability[];
}

export interface FeatureFlagCapability {
  productCode: ProductCode | string;
  moduleCode: ModuleCode | string | null;
  code: string;
  enabled: boolean;
  validFrom: string | null;
  validUntil: string | null;
}

export interface LimitCapability {
  productCode: ProductCode | string;
  code: string;
  value: number | null;
  unlimited: boolean;
  unit: string;
  source: string;
}

export interface TenantCapabilities {
  tenantId: string;
  tenantActive: boolean;
  calculatedAt: string;
  products: ProductCapability[];
  featureFlags: FeatureFlagCapability[];
  limits: LimitCapability[];
}

export interface ModuleShellItem {
  code: ModuleCode;
  label: string;
  path: string;
  productCode: ProductCode;
  summary: string;
}

export const moduleShellItems: ModuleShellItem[] = [
  {
    code: 'CONFIGURATION',
    label: 'Configuracion',
    path: '/configuracion',
    productCode: 'KODA_ERP',
    summary: 'Preferencias operativas y datos institucionales.',
  },
  {
    code: 'CATALOGS',
    label: 'Catalogos',
    path: '/catalogos',
    productCode: 'KODA_ERP',
    summary: 'Productos, marcas, categorias, presentaciones y unidades.',
  },
  {
    code: 'COMMERCIAL_PARTNERS',
    label: 'Clientes y proveedores',
    path: '/terceros',
    productCode: 'KODA_ERP',
    summary: 'Base comercial comun para clientes y proveedores.',
  },
  {
    code: 'STOCK',
    label: 'Stock',
    path: '/stock',
    productCode: 'KODA_ERP',
    summary: 'Saldos, depositos y movimientos de inventario.',
  },
  {
    code: 'CASH',
    label: 'Caja',
    path: '/caja',
    productCode: 'KODA_ERP',
    summary: 'Aperturas, cierres y movimientos de caja.',
  },
  {
    code: 'SALES',
    label: 'Ventas',
    path: '/ventas',
    productCode: 'KODA_ERP',
    summary: 'Documentos de venta y confirmacion comercial.',
  },
  {
    code: 'PURCHASES',
    label: 'Compras',
    path: '/compras',
    productCode: 'KODA_ERP',
    summary: 'Documentos de compra e ingreso operativo.',
  },
  {
    code: 'COMMERCIAL_REPORTS',
    label: 'Reportes',
    path: '/reportes',
    productCode: 'KODA_ERP',
    summary: 'Indicadores comerciales, caja y stock.',
  },
  {
    code: 'AUDIT',
    label: 'Auditoria',
    path: '/auditoria',
    productCode: 'KODA_ERP',
    summary: 'Consulta controlada de eventos auditados.',
  },
];

export function findProduct(capabilities: TenantCapabilities | null, productCode: ProductCode): ProductCapability | null {
  return capabilities?.products.find((product) => product.code === productCode) ?? null;
}

export function findModule(capabilities: TenantCapabilities | null, moduleCode: ModuleCode): ModuleCapability | null {
  return (
    findProduct(capabilities, 'KODA_ERP')?.modules.find((module) => module.code === moduleCode) ?? null
  );
}

export function isModuleEnabled(capabilities: TenantCapabilities | null, moduleCode: ModuleCode): boolean {
  const product = findProduct(capabilities, 'KODA_ERP');
  const module = findModule(capabilities, moduleCode);

  return Boolean(capabilities?.tenantActive && product?.enabled && module?.enabled);
}

export function enabledModuleShellItems(capabilities: TenantCapabilities | null): ModuleShellItem[] {
  return moduleShellItems.filter((item) => isModuleEnabled(capabilities, item.code));
}

export function disabledModuleShellItems(capabilities: TenantCapabilities | null): ModuleShellItem[] {
  return moduleShellItems.filter((item) => !isModuleEnabled(capabilities, item.code));
}

export function findModuleByPath(path: string): ModuleShellItem | null {
  return moduleShellItems.find((item) => item.path === path) ?? null;
}
