import AnalyticsOutlinedIcon from '@mui/icons-material/AnalyticsOutlined';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined';
import SecurityOutlinedIcon from '@mui/icons-material/SecurityOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import {
  AppBar,
  Box,
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
  Stack,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';

const drawerWidth = 272;

const navItems = [
  { label: 'Dashboard', icon: <DashboardOutlinedIcon /> },
  { label: 'Empresas', icon: <BusinessOutlinedIcon /> },
  { label: 'Usuarios', icon: <PeopleAltOutlinedIcon /> },
  { label: 'Productos', icon: <CategoryOutlinedIcon /> },
  { label: 'Stock', icon: <Inventory2OutlinedIcon /> },
  { label: 'Auditoria', icon: <SecurityOutlinedIcon /> },
  { label: 'Reportes', icon: <AnalyticsOutlinedIcon /> },
  { label: 'Configuracion', icon: <SettingsOutlinedIcon /> },
];

const readiness = [
  { label: 'Backend', value: 'Scaffold', progress: 30 },
  { label: 'Frontend', value: 'Scaffold', progress: 35 },
  { label: 'PostgreSQL', value: 'Docker', progress: 25 },
  { label: 'Multi-tenant', value: 'Pendiente', progress: 10 },
];

export function App() {
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar
        elevation={0}
        position="fixed"
        sx={{
          borderBottom: '1px solid',
          borderColor: 'divider',
          bgcolor: 'rgba(11,13,16,0.94)',
          backdropFilter: 'blur(12px)',
          zIndex: (theme) => theme.zIndex.drawer + 1,
        }}
      >
        <Toolbar sx={{ gap: 2 }}>
          <Box sx={{ width: 12, height: 28, borderRadius: 1, bgcolor: 'primary.main' }} />
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h2" component="div">KODA PLATFORM</Typography>
            <Typography variant="caption" color="text.secondary">KODA ERP - Tenant KODA - es-AR - ARS</Typography>
          </Box>
          <Chip color="primary" label="Sprint 1" size="small" />
          <Tooltip title="Configuracion">
            <IconButton color="inherit" aria-label="Configuracion">
              <SettingsOutlinedIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          width: drawerWidth,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: drawerWidth,
            boxSizing: 'border-box',
            borderRight: '1px solid',
            borderColor: 'divider',
            bgcolor: '#0F1318',
            pt: 9,
          },
        }}
      >
        <List sx={{ px: 1 }}>
          {navItems.map((item, index) => (
            <ListItemButton key={item.label} selected={index === 0} sx={{ borderRadius: 1, mb: 0.5 }}>
              <ListItemIcon sx={{ minWidth: 42 }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, pt: 12, px: { xs: 2, md: 4 }, pb: 4 }}>
        <Stack spacing={3}>
          <Box>
            <Typography variant="h1">Dashboard</Typography>
            <Typography color="text.secondary">Estado operativo inicial de KODA ERP</Typography>
          </Box>

          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(4, 1fr)' }, gap: 2 }}>
            {readiness.map((item) => (
              <Paper key={item.label} variant="outlined" sx={{ p: 2, borderColor: 'divider' }}>
                <Stack spacing={1.5}>
                  <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
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
              <Typography variant="h2">Sprint 1 - Hitos</Typography>
            </Box>
            <Divider />
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
              {[
                'Scaffolding tecnico',
                'PostgreSQL y Flyway',
                'Tenant context',
                'JWT y permisos',
                'Configuracion de empresa',
                'Catalogos ERP',
                'Movimientos de stock',
                'Auditoria transversal',
              ].map((item, index) => (
                <Box key={item} sx={{ p: 2, borderRight: { md: index % 2 === 0 ? '1px solid' : 0 }, borderBottom: '1px solid', borderColor: 'divider' }}>
                  <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                    <Chip label={index + 1} color={index === 0 ? 'primary' : 'default'} size="small" />
                    <Typography>{item}</Typography>
                  </Stack>
                </Box>
              ))}
            </Box>
          </Paper>
        </Stack>
      </Box>
    </Box>
  );
}