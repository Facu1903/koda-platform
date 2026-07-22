const maxVisualAssetUrlLength = 2048;

export function normalizeVisualAssetUrl(value: string | null | undefined): string | null {
  if (value === null || value === undefined) {
    return null;
  }

  const normalized = value.trim();
  return normalized === '' ? null : normalized;
}

export function validateVisualAssetUrl(value: string | null | undefined): string | null {
  const normalized = normalizeVisualAssetUrl(value);

  if (normalized === null) {
    return null;
  }

  if (normalized.length > maxVisualAssetUrlLength) {
    return 'Maximo 2048 caracteres.';
  }

  let url: URL;
  try {
    url = new URL(normalized);
  } catch {
    return 'Usar URL https valida.';
  }

  if (url.protocol !== 'https:' || url.hostname.trim() === '') {
    return 'Usar URL https valida.';
  }

  if (url.username !== '' || url.password !== '' || url.hash !== '') {
    return 'Usar URL https valida.';
  }

  return null;
}

export function safeVisualAssetUrl(value: string | null | undefined): string | null {
  const normalized = normalizeVisualAssetUrl(value);

  if (normalized === null || validateVisualAssetUrl(normalized) !== null) {
    return null;
  }

  return normalized;
}
