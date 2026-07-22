import RestartAltOutlinedIcon from '@mui/icons-material/RestartAltOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  MenuItem,
  Paper,
  Skeleton,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { useCompanyProfile } from './CompanyProfileProvider';
import type { CompanySettings, UpdateCompanySettingsRequest } from './companySettings';
import { companySettingsToRuntimeProfile } from './companySettings';
import { CompanySettingsApiError, fetchCompanySettings, updateCompanySettings } from './companySettingsClient';
import type { ThemeMode } from './companyProfile';
import { createRegionalFormatters } from './regionalFormatting';
import { useRegionalFormatters } from './useRegionalFormatters';
import { normalizeVisualAssetUrl, safeVisualAssetUrl, validateVisualAssetUrl } from './visualAssets';

type CompanySettingsStatus = 'loading' | 'ready' | 'unavailable';
type DraftField = keyof UpdateCompanySettingsRequest;
type ValidationErrors = Partial<Record<DraftField, string>>;

interface SettingsLoadError {
  message: string;
  status: number | null;
}

const colorPattern = /^#[0-9A-Fa-f]{6}$/;
const currencyPattern = /^[A-Za-z]{3}$/;
const localePattern = /^[a-zA-Z]{2,3}([-_][a-zA-Z]{2})?$/;
const sampleDate = '2026-07-21T12:05:00Z';
const sampleAmount = 1234.5;
const themeModes: ThemeMode[] = ['dark', 'light', 'system'];
const currencyFormats = ['symbol', 'code', 'name', 'narrowSymbol'];

export function CompanySettingsWorkspace() {
  const { applyProfile } = useCompanyProfile();
  const regionalFormatters = useRegionalFormatters();
  const [draft, setDraft] = useState<UpdateCompanySettingsRequest | null>(null);
  const [loadError, setLoadError] = useState<SettingsLoadError | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [settings, setSettings] = useState<CompanySettings | null>(null);
  const [status, setStatus] = useState<CompanySettingsStatus>('loading');

  useEffect(() => {
    const controller = new AbortController();

    fetchCompanySettings(controller.signal)
      .then((loadedSettings) => {
        setSettings(loadedSettings);
        setDraft(toDraft(loadedSettings));
        setStatus('ready');
      })
      .catch((caught: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        setSettings(null);
        setDraft(null);
        setStatus('unavailable');
        setLoadError({
          message: caught instanceof Error ? caught.message : 'No se pudo cargar la configuracion de empresa.',
          status: caught instanceof CompanySettingsApiError ? caught.status : null,
        });
      });

    return () => controller.abort();
  }, [reloadKey]);

  const validationErrors = useMemo(() => (draft === null ? {} : validateDraft(draft)), [draft]);
  const dirty = useMemo(() => {
    if (settings === null || draft === null) {
      return false;
    }

    return JSON.stringify(toRequest(draft)) !== JSON.stringify(toRequest(toDraft(settings)));
  }, [draft, settings]);
  const previewRegional = useMemo(
    () =>
      draft === null
        ? null
        : createRegionalFormatters({
            currencyFormat: draft.currencyFormat,
            dateFormat: draft.dateFormat,
            defaultCurrency: draft.defaultCurrency,
            defaultLocale: draft.defaultLocale,
            numberLocale: draft.numberLocale,
            timeFormat: draft.timeFormat,
            timeZone: draft.timeZone,
          }),
    [draft],
  );
  const canSave = draft !== null && dirty && !saving && Object.keys(validationErrors).length === 0;

  const updateDraftField = <Field extends DraftField>(field: Field, value: UpdateCompanySettingsRequest[Field]) => {
    setDraft((current) => (current === null ? current : { ...current, [field]: value }));
    setSaveError(null);
    setSaveMessage(null);
  };

  const resetDraft = () => {
    if (settings !== null) {
      setDraft(toDraft(settings));
      setSaveError(null);
      setSaveMessage(null);
    }
  };

  const retryLoad = () => {
    setStatus('loading');
    setLoadError(null);
    setSaveError(null);
    setSaveMessage(null);
    setReloadKey((current) => current + 1);
  };

  const submit = async () => {
    if (draft === null || !canSave) {
      return;
    }

    setSaving(true);
    setSaveError(null);
    setSaveMessage(null);

    try {
      const updated = await updateCompanySettings(toRequest(draft));
      setSettings(updated);
      setDraft(toDraft(updated));
      applyProfile(companySettingsToRuntimeProfile(updated));
      setSaveMessage('Configuracion guardada.');
    } catch (caught) {
      if (caught instanceof CompanySettingsApiError && caught.status === 409) {
        setSaveError('Conflicto de version. Recarga la configuracion antes de guardar nuevos cambios.');
      } else {
        setSaveError(caught instanceof Error ? caught.message : 'No se pudo actualizar la configuracion de empresa.');
      }
    } finally {
      setSaving(false);
    }
  };

  if (status === 'loading') {
    return <CompanySettingsLoading />;
  }

  if (status === 'unavailable' || settings === null || draft === null) {
    return (
      <CompanySettingsUnavailable
        error={loadError}
        onRetry={retryLoad}
      />
    );
  }

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ alignItems: { md: 'flex-start' } }}>
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Typography variant="h1">Configuracion de empresa</Typography>
          <Typography color="text.secondary">{settings.tenant.commercialName}</Typography>
        </Box>
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', justifyContent: { md: 'flex-end' } }}>
          <Chip label={`Version ${settings.version}`} size="small" variant="outlined" />
          <Chip
            label={regionalFormatters.formatDateTime(settings.updatedAt, { emptyLabel: 'Sin cambios' })}
            size="small"
            variant="outlined"
          />
          {dirty ? <Chip color="warning" label="Cambios sin guardar" size="small" /> : null}
        </Stack>
      </Stack>

      {saveMessage !== null ? (
        <Alert severity="success" variant="outlined">
          {saveMessage}
        </Alert>
      ) : null}
      {saveError !== null ? (
        <Alert severity="warning" variant="outlined">
          {saveError}
        </Alert>
      ) : null}

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1fr 1fr' }, gap: 2 }}>
        <CompanyIdentityPanel settings={settings} />
        <BrandingPanel draft={draft} errors={validationErrors} onChange={updateDraftField} />
        <RegionalPanel draft={draft} errors={validationErrors} onChange={updateDraftField} />
        <PreviewPanel draft={draft} previewRegional={previewRegional} />
      </Box>

      <Paper
        variant="outlined"
        sx={{
          borderColor: 'divider',
          bottom: 16,
          p: 2,
          position: { md: 'sticky' },
          zIndex: 1,
        }}
      >
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ justifyContent: 'flex-end' }}>
          <Button disabled={!dirty || saving} startIcon={<RestartAltOutlinedIcon />} variant="outlined" onClick={resetDraft}>
            Cancelar
          </Button>
          <Button disabled={!canSave} startIcon={saving ? <CircularProgress color="inherit" size={16} /> : <SaveOutlinedIcon />} variant="contained" onClick={submit}>
            Guardar
          </Button>
        </Stack>
      </Paper>
    </Stack>
  );
}

function CompanySettingsLoading() {
  return (
    <Stack spacing={3}>
      <Box>
        <Skeleton height={40} width={280} />
        <Skeleton height={24} width={220} />
      </Box>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1fr 1fr' }, gap: 2 }}>
        {[1, 2, 3, 4].map((item) => (
          <Paper key={item} variant="outlined" sx={{ borderColor: 'divider', p: 2 }}>
            <Stack spacing={2}>
              <Skeleton height={28} width="45%" />
              <Skeleton height={56} />
              <Skeleton height={56} />
              <Skeleton height={56} />
            </Stack>
          </Paper>
        ))}
      </Box>
    </Stack>
  );
}

function CompanySettingsUnavailable({ error, onRetry }: { error: SettingsLoadError | null; onRetry: () => void }) {
  const title = error?.status === 403 ? 'Acceso restringido' : 'Configuracion no disponible';

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h1">{title}</Typography>
        <Typography color="text.secondary">Modulo de configuracion</Typography>
      </Box>
      <Alert
        action={
          <Button color="inherit" size="small" startIcon={<RestartAltOutlinedIcon />} onClick={onRetry}>
            Reintentar
          </Button>
        }
        severity={error?.status === 403 ? 'info' : 'warning'}
        variant="outlined"
      >
        {error?.message ?? 'No se pudo cargar la configuracion de empresa.'}
      </Alert>
    </Stack>
  );
}

function CompanyIdentityPanel({ settings }: { settings: CompanySettings }) {
  return (
    <Paper variant="outlined" sx={{ borderColor: 'divider', p: 2 }}>
      <Stack spacing={2}>
        <Typography variant="h2">Empresa</Typography>
        <Divider />
        <FieldPair label="Nombre comercial" value={settings.tenant.commercialName} />
        <FieldPair label="Razon social" value={settings.tenant.legalName} />
        <FieldPair label="Pais" value={settings.tenant.countryCode} />
        <FieldPair label="Tenant" value={settings.tenant.id} />
      </Stack>
    </Paper>
  );
}

function BrandingPanel({
  draft,
  errors,
  onChange,
}: {
  draft: UpdateCompanySettingsRequest;
  errors: ValidationErrors;
  onChange: <Field extends DraftField>(field: Field, value: UpdateCompanySettingsRequest[Field]) => void;
}) {
  return (
    <Paper variant="outlined" sx={{ borderColor: 'divider', p: 2 }}>
      <Stack spacing={2}>
        <Typography variant="h2">Identidad visual</Typography>
        <Divider />
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
          <ColorField
            error={errors.primaryColor}
            label="Color primario"
            value={draft.primaryColor}
            onChange={(value) => onChange('primaryColor', value)}
          />
          <ColorField
            error={errors.secondaryColor}
            label="Color secundario"
            optional
            value={draft.secondaryColor ?? ''}
            onChange={(value) => onChange('secondaryColor', value)}
          />
        </Stack>
        <ToggleButtonGroup
          exclusive
          aria-label="Modo de tema"
          size="small"
          value={draft.themeMode}
          onChange={(_, value: ThemeMode | null) => {
            if (value !== null) {
              onChange('themeMode', value);
            }
          }}
        >
          {themeModes.map((mode) => (
            <ToggleButton key={mode} value={mode}>
              {themeModeLabel(mode)}
            </ToggleButton>
          ))}
        </ToggleButtonGroup>
        <TextField
          error={Boolean(errors.logoUrl)}
          helperText={errors.logoUrl}
          label="Logo URL"
          value={draft.logoUrl ?? ''}
          onChange={(event) => onChange('logoUrl', event.target.value)}
        />
        <TextField
          error={Boolean(errors.faviconUrl)}
          helperText={errors.faviconUrl}
          label="Favicon URL"
          value={draft.faviconUrl ?? ''}
          onChange={(event) => onChange('faviconUrl', event.target.value)}
        />
        <TextField
          error={Boolean(errors.loginImageUrl)}
          helperText={errors.loginImageUrl}
          label="Imagen de login URL"
          value={draft.loginImageUrl ?? ''}
          onChange={(event) => onChange('loginImageUrl', event.target.value)}
        />
      </Stack>
    </Paper>
  );
}

function RegionalPanel({
  draft,
  errors,
  onChange,
}: {
  draft: UpdateCompanySettingsRequest;
  errors: ValidationErrors;
  onChange: <Field extends DraftField>(field: Field, value: UpdateCompanySettingsRequest[Field]) => void;
}) {
  return (
    <Paper variant="outlined" sx={{ borderColor: 'divider', p: 2 }}>
      <Stack spacing={2}>
        <Typography variant="h2">Regional</Typography>
        <Divider />
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1.5 }}>
          <TextField
            error={Boolean(errors.defaultLocale)}
            helperText={errors.defaultLocale}
            label="Locale"
            value={draft.defaultLocale}
            onChange={(event) => onChange('defaultLocale', event.target.value)}
          />
          <TextField
            error={Boolean(errors.numberLocale)}
            helperText={errors.numberLocale}
            label="Locale numerico"
            value={draft.numberLocale}
            onChange={(event) => onChange('numberLocale', event.target.value)}
          />
          <TextField
            error={Boolean(errors.defaultCurrency)}
            helperText={errors.defaultCurrency}
            label="Moneda"
            value={draft.defaultCurrency}
            onChange={(event) => onChange('defaultCurrency', event.target.value.toUpperCase())}
          />
          <TextField
            select
            error={Boolean(errors.currencyFormat)}
            helperText={errors.currencyFormat}
            label="Formato moneda"
            value={draft.currencyFormat}
            onChange={(event) => onChange('currencyFormat', event.target.value)}
          >
            {currencyFormats.map((format) => (
              <MenuItem key={format} value={format}>
                {currencyFormatLabel(format)}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            error={Boolean(errors.timeZone)}
            helperText={errors.timeZone}
            label="Zona horaria"
            value={draft.timeZone}
            onChange={(event) => onChange('timeZone', event.target.value)}
          />
          <TextField
            error={Boolean(errors.dateFormat)}
            helperText={errors.dateFormat}
            label="Formato fecha"
            value={draft.dateFormat}
            onChange={(event) => onChange('dateFormat', event.target.value)}
          />
          <TextField
            error={Boolean(errors.timeFormat)}
            helperText={errors.timeFormat}
            label="Formato hora"
            value={draft.timeFormat}
            onChange={(event) => onChange('timeFormat', event.target.value)}
          />
        </Box>
      </Stack>
    </Paper>
  );
}

function PreviewPanel({
  draft,
  previewRegional,
}: {
  draft: UpdateCompanySettingsRequest;
  previewRegional: ReturnType<typeof createRegionalFormatters> | null;
}) {
  const primaryColor = colorPattern.test(draft.primaryColor) ? draft.primaryColor.toUpperCase() : '#F6862B';
  const secondaryColor =
    draft.secondaryColor !== null && colorPattern.test(draft.secondaryColor) ? draft.secondaryColor.toUpperCase() : '#FFFFFF';
  const contrastColor = readableTextColor(primaryColor);
  const previewAmount = previewRegional?.formatCurrency(sampleAmount) ?? '-';
  const previewDate = previewRegional?.formatDateTime(sampleDate) ?? '-';

  return (
    <Paper variant="outlined" sx={{ borderColor: 'divider', p: 2 }}>
      <Stack spacing={2}>
        <Typography variant="h2">Vista previa</Typography>
        <Divider />
        <Box
          sx={{
            bgcolor: primaryColor,
            borderRadius: 1,
            color: contrastColor,
            p: 2,
          }}
        >
          <Typography sx={{ fontWeight: 700 }}>KODA PLATFORM</Typography>
          <Typography variant="body2">{draft.defaultLocale} / {draft.defaultCurrency}</Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          <Box sx={{ bgcolor: primaryColor, borderRadius: 1, height: 36, width: 56 }} />
          <Box sx={{ bgcolor: secondaryColor, border: '1px solid', borderColor: 'divider', borderRadius: 1, height: 36, width: 56 }} />
        </Stack>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1.5 }}>
          <VisualAssetPreview label="Logo" url={draft.logoUrl} />
          <VisualAssetPreview label="Favicon" compact url={draft.faviconUrl} />
          <VisualAssetPreview label="Login" wide url={draft.loginImageUrl} />
        </Box>
        <FieldPair label="Importe" value={previewAmount} />
        <FieldPair label="Fecha" value={previewDate} />
        <FieldPair label="Tema" value={themeModeLabel(draft.themeMode)} />
      </Stack>
    </Paper>
  );
}

function VisualAssetPreview({
  compact = false,
  label,
  url,
  wide = false,
}: {
  compact?: boolean;
  label: string;
  url: string | null;
  wide?: boolean;
}) {
  const safeUrl = safeVisualAssetUrl(url);
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const visibleUrl = safeUrl !== null && safeUrl !== failedUrl ? safeUrl : null;

  return (
    <Paper
      variant="outlined"
      sx={{
        borderColor: 'divider',
        gridColumn: wide ? { sm: '1 / -1' } : undefined,
        overflow: 'hidden',
      }}
    >
      <Box sx={{ px: 1.25, py: 1 }}>
        <Typography sx={{ fontWeight: 700 }} variant="body2">
          {label}
        </Typography>
      </Box>
      <Divider />
      <Box
        sx={{
          alignItems: 'center',
          bgcolor: 'background.default',
          display: 'flex',
          height: compact ? 72 : wide ? 132 : 96,
          justifyContent: 'center',
          p: 1,
        }}
      >
        {visibleUrl === null ? (
          <Typography color="text.secondary" variant="body2">
            Sin asset seguro
          </Typography>
        ) : (
          <Box
            alt={`${label} preview`}
            component="img"
            loading="lazy"
            referrerPolicy="no-referrer"
            src={visibleUrl}
            sx={{
              borderRadius: 1,
              maxHeight: '100%',
              maxWidth: '100%',
              objectFit: 'contain',
            }}
            onError={() => setFailedUrl(visibleUrl)}
          />
        )}
      </Box>
    </Paper>
  );
}

function FieldPair({ label, value }: { label: string; value: string }) {
  return (
    <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'space-between', minWidth: 0 }}>
      <Typography color="text.secondary" variant="body2">
        {label}
      </Typography>
      <Typography sx={{ fontWeight: 700, overflowWrap: 'anywhere', textAlign: 'right' }} variant="body2">
        {value}
      </Typography>
    </Stack>
  );
}

function ColorField({
  error,
  label,
  optional = false,
  value,
  onChange,
}: {
  error?: string;
  label: string;
  optional?: boolean;
  value: string;
  onChange: (value: string) => void;
}) {
  const colorValue = colorPattern.test(value) ? value : '#F6862B';

  return (
    <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start', flex: 1, minWidth: 0 }}>
      <Box
        aria-label={`${label} selector`}
        component="input"
        type="color"
        value={colorValue}
        sx={{
          bgcolor: 'transparent',
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 1,
          cursor: 'pointer',
          height: 56,
          p: 0.5,
          width: 56,
        }}
        onChange={(event) => onChange(event.target.value.toUpperCase())}
      />
      <TextField
        error={Boolean(error)}
        helperText={error}
        label={label}
        value={value}
        sx={{ flex: 1 }}
        onChange={(event) => onChange(optional && event.target.value.trim() === '' ? '' : event.target.value)}
      />
    </Stack>
  );
}

function toDraft(settings: CompanySettings): UpdateCompanySettingsRequest {
  return {
    dateFormat: settings.regional.dateFormat,
    defaultCurrency: settings.regional.defaultCurrency,
    defaultLocale: settings.regional.defaultLocale,
    faviconUrl: settings.branding.faviconUrl,
    currencyFormat: settings.regional.currencyFormat,
    logoUrl: settings.branding.logoUrl,
    loginImageUrl: settings.branding.loginImageUrl,
    numberLocale: settings.regional.numberLocale,
    primaryColor: settings.branding.primaryColor,
    secondaryColor: settings.branding.secondaryColor,
    themeMode: settings.branding.themeMode,
    timeFormat: settings.regional.timeFormat,
    timeZone: settings.regional.timeZone,
    version: settings.version,
  };
}

function toRequest(draft: UpdateCompanySettingsRequest): UpdateCompanySettingsRequest {
  return {
    ...draft,
    dateFormat: draft.dateFormat.trim(),
    defaultCurrency: draft.defaultCurrency.trim().toUpperCase(),
    defaultLocale: normalizeLocaleInput(draft.defaultLocale),
    faviconUrl: normalizeVisualAssetUrl(draft.faviconUrl),
    logoUrl: normalizeVisualAssetUrl(draft.logoUrl),
    loginImageUrl: normalizeVisualAssetUrl(draft.loginImageUrl),
    numberLocale: normalizeLocaleInput(draft.numberLocale),
    primaryColor: draft.primaryColor.trim().toUpperCase(),
    secondaryColor: trimToNull(draft.secondaryColor)?.toUpperCase() ?? null,
    timeFormat: draft.timeFormat.trim(),
    timeZone: draft.timeZone.trim(),
  };
}

function validateDraft(draft: UpdateCompanySettingsRequest): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!colorPattern.test(draft.primaryColor.trim())) {
    errors.primaryColor = 'Usar #RRGGBB.';
  }

  const secondaryColor = trimToNull(draft.secondaryColor);
  if (secondaryColor !== null && !colorPattern.test(secondaryColor)) {
    errors.secondaryColor = 'Usar #RRGGBB.';
  }

  if (!localePattern.test(normalizeLocaleInput(draft.defaultLocale))) {
    errors.defaultLocale = 'Locale invalido.';
  }

  if (!localePattern.test(normalizeLocaleInput(draft.numberLocale))) {
    errors.numberLocale = 'Locale invalido.';
  }

  if (!currencyPattern.test(draft.defaultCurrency.trim())) {
    errors.defaultCurrency = 'Usar ISO 4217.';
  }

  if (draft.timeZone.trim() === '') {
    errors.timeZone = 'Campo obligatorio.';
  }

  if (draft.dateFormat.trim() === '') {
    errors.dateFormat = 'Campo obligatorio.';
  }

  if (draft.timeFormat.trim() === '') {
    errors.timeFormat = 'Campo obligatorio.';
  }

  if (!currencyFormats.includes(draft.currencyFormat)) {
    errors.currencyFormat = 'Formato invalido.';
  }

  validateAssetUrl(errors, 'logoUrl', draft.logoUrl);
  validateAssetUrl(errors, 'faviconUrl', draft.faviconUrl);
  validateAssetUrl(errors, 'loginImageUrl', draft.loginImageUrl);

  return errors;
}

function validateAssetUrl(errors: ValidationErrors, field: DraftField, value: string | null) {
  const error = validateVisualAssetUrl(value);

  if (error !== null) {
    errors[field] = error;
  }
}

function trimToNull(value: string | null): string | null {
  if (value === null) {
    return null;
  }

  const trimmed = value.trim();
  return trimmed === '' ? null : trimmed;
}

function normalizeLocaleInput(value: string): string {
  const [language, country] = value.trim().replace('_', '-').split('-');

  if (language === undefined || language === '') {
    return '';
  }

  if (country === undefined || country === '') {
    return language.toLowerCase();
  }

  return `${language.toLowerCase()}-${country.toUpperCase()}`;
}

function themeModeLabel(mode: ThemeMode): string {
  switch (mode) {
    case 'dark':
      return 'Oscuro';
    case 'light':
      return 'Claro';
    case 'system':
      return 'Sistema';
  }
}

function currencyFormatLabel(value: string): string {
  switch (value) {
    case 'code':
      return 'Codigo';
    case 'name':
      return 'Nombre';
    case 'narrowSymbol':
      return 'Simbolo corto';
    case 'symbol':
      return 'Simbolo';
    default:
      return value;
  }
}

function readableTextColor(hexColor: string): string {
  const red = Number.parseInt(hexColor.slice(1, 3), 16);
  const green = Number.parseInt(hexColor.slice(3, 5), 16);
  const blue = Number.parseInt(hexColor.slice(5, 7), 16);
  const luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;

  return luminance > 0.58 ? '#111111' : '#FFFFFF';
}
