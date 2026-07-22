import AnalyticsOutlinedIcon from '@mui/icons-material/AnalyticsOutlined';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import GavelOutlinedIcon from '@mui/icons-material/GavelOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import PointOfSaleOutlinedIcon from '@mui/icons-material/PointOfSaleOutlined';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import RefreshOutlinedIcon from '@mui/icons-material/RefreshOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import {
  Alert,
  AppBar,
  Box,
  Button,
  Chip,
  Divider,
  Drawer,
  IconButton,
  LinearProgress,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Paper,
  Skeleton,
  Stack,
  Tab,
  Tabs,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import type { SvgIconProps } from '@mui/material/SvgIcon';
import { alpha } from '@mui/material/styles';
import { useEffect, useMemo, useState } from 'react';
import { useCompanyProfile } from '../platform/configuration/CompanyProfileProvider';
import type { RegionalFormatters } from '../platform/configuration/regionalFormatting';
import { useRegionalFormatters } from '../platform/configuration/useRegionalFormatters';
import { useCapabilities } from '../platform/licensing/CapabilitiesProvider';
import {
  disabledModuleShellItems,
  enabledModuleShellItems,
  findModule,
  findModuleByPath,
  findProduct,
  isModuleEnabled,
  moduleShellItems,
} from '../platform/licensing/capabilities';
import type { ModuleCode, ModuleShellItem } from '../platform/licensing/capabilities';

const drawerWidth = 280;
const dashboardPath = '/dashboard';

interface NavigationItem {
  label: string;
  path: string;
  moduleCode: ModuleCode | null;
}

function readHashPath(): string {
  const hashPath = window.location.hash.replace(/^#/, '');

  if (hashPath === '') {
    return dashboardPath;
  }

  return hashPath.startsWith('/') ? hashPath : `/${hashPath}`;
}

function writeHashPath(path: string) {
  window.location.hash = path;
}

function ModuleShellIcon({ moduleCode, ...props }: { moduleCode: ModuleCode } & SvgIconProps) {
  switch (moduleCode) {
    case 'AUDIT':
      return <GavelOutlinedIcon {...props} />;
    case 'CASH':
      return <PointOfSaleOutlinedIcon {...props} />;
    case 'CATALOGS':
      return <CategoryOutlinedIcon {...props} />;
    case 'COMMERCIAL_PARTNERS':
      return <GroupsOutlinedIcon {...props} />;
    case 'COMMERCIAL_REPORTS':
      return <AnalyticsOutlinedIcon {...props} />;
    case 'CONFIGURATION':
      return <SettingsOutlinedIcon {...props} />;
    case 'PURCHASES':
      return <ShoppingCartOutlinedIcon {...props} />;
    case 'SALES':
      return <ReceiptLongOutlinedIcon {...props} />;
    case 'STOCK':
      return <Inventory2OutlinedIcon {...props} />;
  }
}

function NavigationIcon({ moduleCode, ...props }: { moduleCode: ModuleCode | null } & SvgIconProps) {
  if (moduleCode === null) {
    return <DashboardOutlinedIcon {...props} />;
  }

  return <ModuleShellIcon moduleCode={moduleCode} {...props} />;
}

export function App() {
  const { capabilities, error, reload, status } = useCapabilities();
  const { effectiveProfile } = useCompanyProfile();
  const regionalFormatters = useRegionalFormatters();
  const [currentPath, setCurrentPath] = useState(readHashPath);

  useEffect(() => {
    const syncPath = () => setCurrentPath(readHashPath());

    window.addEventListener('hashchange', syncPath);
    syncPath();

    return () => window.removeEventListener('hashchange', syncPath);
  }, []);

  const product = findProduct(capabilities, 'KODA_ERP');
  const tenantName = effectiveProfile.tenant.commercialName;
  const tenantLocale = effectiveProfile.regional.defaultLocale;
  const tenantCurrency = effectiveProfile.regional.defaultCurrency;
  const enabledModules = useMemo(() => enabledModuleShellItems(capabilities), [capabilities]);
  const disabledModules = useMemo(() => disabledModuleShellItems(capabilities), [capabilities]);
  const navigationItems = useMemo<NavigationItem[]>(
    () => [
      { label: 'Dashboard', path: dashboardPath, moduleCode: null },
      ...enabledModules.map((item) => ({
        label: item.label,
        path: item.path,
        moduleCode: item.code,
      })),
    ],
    [enabledModules],
  );

  const requestedModule = findModuleByPath(currentPath);
  const routeBlocked =
    requestedModule !== null && status === 'ready' && !isModuleEnabled(capabilities, requestedModule.code);
  const selectedNavigationPath = navigationItems.some((item) => item.path === currentPath) ? currentPath : dashboardPath;
  const activeModule =
    requestedModule !== null && !routeBlocked
      ? requestedModule
      : moduleShellItems.find((item) => item.path === selectedNavigationPath) ?? null;

  const navigate = (path: string) => {
    writeHashPath(path);
    setCurrentPath(path);
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar
        color="default"
        elevation={0}
        position="fixed"
        sx={{
          borderBottom: '1px solid',
          borderColor: 'divider',
          bgcolor: (theme) => alpha(theme.palette.background.default, 0.94),
          backdropFilter: 'blur(12px)',
          zIndex: (theme) => theme.zIndex.drawer + 1,
        }}
      >
        <Toolbar sx={{ gap: 2, minHeight: { xs: 72, md: 80 } }}>
          <Box sx={{ width: 12, height: 30, borderRadius: 1, bgcolor: 'primary.main', flex: '0 0 auto' }} />
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            <Typography variant="h2" component="div" noWrap>
              KODA PLATFORM
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap component="div">
              {product?.name ?? 'KODA ERP'} - {tenantName} - {tenantLocale} - {tenantCurrency}
            </Typography>
          </Box>
          <Chip
            color={status === 'ready' ? 'primary' : 'default'}
            label={product?.planCode ?? 'Licencia'}
            size="small"
            sx={{ display: { xs: 'none', sm: 'inline-flex' } }}
          />
          <Tooltip title="Recargar capacidades">
            <IconButton color="inherit" aria-label="Recargar licencia" onClick={reload}>
              <RefreshOutlinedIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', md: 'block' },
          width: drawerWidth,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: drawerWidth,
            boxSizing: 'border-box',
            borderRight: '1px solid',
            borderColor: 'divider',
            bgcolor: 'background.paper',
            pt: 10,
          },
        }}
      >
        <List sx={{ px: 1 }}>
          {navigationItems.map((item) => {
            return (
              <ListItemButton
                key={item.path}
                selected={selectedNavigationPath === item.path}
                sx={{ borderRadius: 1, mb: 0.5, minHeight: 44 }}
                onClick={() => navigate(item.path)}
              >
                <ListItemIcon sx={{ minWidth: 42 }}>
                  <NavigationIcon moduleCode={item.moduleCode} />
                </ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            );
          })}
        </List>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, pt: { xs: 10, md: 12 }, px: { xs: 2, md: 4 }, pb: 4 }}>
        <Stack spacing={3}>
          <Tabs
            aria-label="Navegacion de modulos"
            onChange={(_, value: string) => navigate(value)}
            sx={{
              display: { xs: 'flex', md: 'none' },
              minHeight: 44,
              maxWidth: 'calc(100vw - 32px)',
              '& .MuiTab-root': { minHeight: 44 },
            }}
            value={selectedNavigationPath}
            variant="scrollable"
            scrollButtons="auto"
          >
            {navigationItems.map((item) => {
              return (
                <Tab
                  key={item.path}
                  icon={<NavigationIcon moduleCode={item.moduleCode} />}
                  iconPosition="start"
                  label={item.label}
                  value={item.path}
                />
              );
            })}
          </Tabs>

          {status === 'loading' ? (
            <LoadingShell />
          ) : status === 'unavailable' ? (
            <UnavailableShell error={error} onRetry={reload} />
          ) : routeBlocked && requestedModule !== null ? (
            <BlockedModuleShell module={requestedModule} />
          ) : activeModule !== null ? (
            <ModuleWorkspace formatDateTime={regionalFormatters.formatDateTime} module={activeModule} />
          ) : (
            <DashboardShell
              calculatedAt={capabilities?.calculatedAt ?? null}
              disabledModules={disabledModules}
              enabledModules={enabledModules}
              formatDateTime={regionalFormatters.formatDateTime}
              tenantName={tenantName}
            />
          )}
        </Stack>
      </Box>
    </Box>
  );
}

function LoadingShell() {
  return (
    <Stack spacing={3}>
      <Box>
        <Skeleton width={220} height={40} />
        <Skeleton width={360} height={24} />
      </Box>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(4, 1fr)' }, gap: 2 }}>
        {[1, 2, 3, 4].map((item) => (
          <Paper key={item} variant="outlined" sx={{ p: 2, borderColor: 'divider' }}>
            <Stack spacing={1.5}>
              <Skeleton width="70%" />
              <Skeleton width="100%" height={8} />
            </Stack>
          </Paper>
        ))}
      </Box>
    </Stack>
  );
}

function UnavailableShell({ error, onRetry }: { error: string | null; onRetry: () => void }) {
  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h1">Licencia no disponible</Typography>
        <Typography color="text.secondary">No se pudo confirmar el acceso operativo de la empresa.</Typography>
      </Box>
      <Alert
        action={
          <Button color="inherit" size="small" startIcon={<RefreshOutlinedIcon />} onClick={onRetry}>
            Reintentar
          </Button>
        }
        severity="warning"
        variant="outlined"
      >
        {error ?? 'No se pudo cargar la licencia efectiva.'}
      </Alert>
    </Stack>
  );
}

function DashboardShell({
  calculatedAt,
  disabledModules,
  enabledModules,
  formatDateTime,
  tenantName,
}: {
  calculatedAt: string | null;
  disabledModules: ModuleShellItem[];
  enabledModules: ModuleShellItem[];
  formatDateTime: RegionalFormatters['formatDateTime'];
  tenantName: string;
}) {
  const readiness = [
    { label: 'Modulos activos', value: String(enabledModules.length), progress: enabledModules.length === 0 ? 0 : 100 },
    { label: 'Modulos bloqueados', value: String(disabledModules.length), progress: disabledModules.length === 0 ? 100 : 35 },
    { label: 'Producto', value: 'KODA ERP', progress: 100 },
    { label: 'Calculado', value: formatDateTime(calculatedAt, { emptyLabel: 'Pendiente' }), progress: 100 },
  ];

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h1">Dashboard</Typography>
        <Typography color="text.secondary">Estado operativo del tenant {tenantName}.</Typography>
      </Box>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(4, 1fr)' }, gap: 2 }}>
        {readiness.map((item) => (
          <Paper key={item.label} variant="outlined" sx={{ p: 2, borderColor: 'divider' }}>
            <Stack spacing={1.5}>
              <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', gap: 1 }}>
                <Typography sx={{ fontWeight: 700 }}>{item.label}</Typography>
                <Chip label={item.value} size="small" variant="outlined" />
              </Stack>
              <LinearProgress variant="determinate" value={item.progress} color="primary" />
            </Stack>
          </Paper>
        ))}
      </Box>

      <Paper variant="outlined" sx={{ p: 0, overflow: 'hidden', borderColor: 'divider' }}>
        <Box sx={{ px: 2, py: 1.5 }}>
          <Typography variant="h2">Modulos disponibles</Typography>
        </Box>
        <Divider />
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
          {enabledModules.map((item, index) => {
            return (
              <Box
                key={item.code}
                sx={{
                  p: 2,
                  borderRight: { md: index % 2 === 0 ? '1px solid' : 0 },
                  borderBottom: '1px solid',
                  borderColor: 'divider',
                }}
              >
                <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                  <ModuleShellIcon color="primary" moduleCode={item.code} />
                  <Box sx={{ minWidth: 0 }}>
                    <Typography sx={{ fontWeight: 700 }}>{item.label}</Typography>
                    <Typography color="text.secondary" variant="body2">
                      {item.summary}
                    </Typography>
                  </Box>
                </Stack>
              </Box>
            );
          })}
        </Box>
      </Paper>

      {disabledModules.length > 0 ? (
        <Paper variant="outlined" sx={{ p: 0, overflow: 'hidden', borderColor: 'divider' }}>
          <Box sx={{ px: 2, py: 1.5 }}>
            <Typography variant="h2">Modulos sin licencia activa</Typography>
          </Box>
          <Divider />
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
            {disabledModules.map((item, index) => {
              return (
                <Box
                  key={item.code}
                  sx={{
                    p: 2,
                    borderRight: { md: index % 2 === 0 ? '1px solid' : 0 },
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                  }}
                >
                  <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                    <ModuleShellIcon color="disabled" moduleCode={item.code} />
                    <Box sx={{ minWidth: 0 }}>
                      <Typography sx={{ fontWeight: 700 }}>{item.label}</Typography>
                      <Typography color="text.secondary" variant="body2">
                        No aparece en el menu operativo.
                      </Typography>
                    </Box>
                  </Stack>
                </Box>
              );
            })}
          </Box>
        </Paper>
      ) : null}
    </Stack>
  );
}

function ModuleWorkspace({
  formatDateTime,
  module,
}: {
  formatDateTime: RegionalFormatters['formatDateTime'];
  module: ModuleShellItem;
}) {
  const { capabilities } = useCapabilities();
  const capability = findModule(capabilities, module.code);

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h1">{module.label}</Typography>
        <Typography color="text.secondary">{module.summary}</Typography>
      </Box>

      <Paper variant="outlined" sx={{ p: 2, borderColor: 'divider' }}>
        <Stack spacing={2}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ alignItems: { sm: 'center' } }}>
            <Chip color="primary" label={capability?.entitlementStatus ?? 'ACTIVE'} size="small" />
            <Chip label={capability?.coreModule ? 'Modulo core' : 'Modulo comercial'} size="small" variant="outlined" />
            <Chip
              label={`Vigencia: ${formatDateTime(capability?.validUntil ?? null, { emptyLabel: 'Sin vencimiento' })}`}
              size="small"
              variant="outlined"
            />
          </Stack>
          <Divider />
          <Typography color="text.secondary">
            Acceso habilitado para operaciones del tenant dentro de este modulo.
          </Typography>
        </Stack>
      </Paper>
    </Stack>
  );
}

function BlockedModuleShell({ module }: { module: ModuleShellItem }) {
  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h1">Modulo no habilitado</Typography>
        <Typography color="text.secondary">{module.label} no tiene licencia activa para este tenant.</Typography>
      </Box>
      <Paper variant="outlined" sx={{ p: 2, borderColor: 'divider' }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'center' } }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexGrow: 1 }}>
            <BlockOutlinedIcon color="warning" />
            <ModuleShellIcon color="disabled" moduleCode={module.code} />
            <Box>
              <Typography sx={{ fontWeight: 700 }}>{module.label}</Typography>
              <Typography color="text.secondary" variant="body2">
                Las operaciones nuevas permanecen bloqueadas para este modulo.
              </Typography>
            </Box>
          </Box>
          <Chip label={module.code} size="small" variant="outlined" />
        </Stack>
      </Paper>
    </Stack>
  );
}
