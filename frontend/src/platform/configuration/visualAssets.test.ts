import { describe, expect, it } from 'vitest';
import { normalizeVisualAssetUrl, safeVisualAssetUrl, validateVisualAssetUrl } from './visualAssets';

describe('visualAssets', () => {
  it('normalizes empty visual asset urls', () => {
    expect(normalizeVisualAssetUrl(null)).toBeNull();
    expect(normalizeVisualAssetUrl('   ')).toBeNull();
    expect(normalizeVisualAssetUrl('  https://cdn.example.com/logo.png  ')).toBe('https://cdn.example.com/logo.png');
  });

  it('accepts https asset urls without credentials or fragments', () => {
    expect(validateVisualAssetUrl('https://cdn.example.com/logo.png')).toBeNull();
    expect(safeVisualAssetUrl('https://cdn.example.com/logo.png')).toBe('https://cdn.example.com/logo.png');
  });

  it('rejects unsafe or ambiguous asset urls', () => {
    expect(validateVisualAssetUrl('http://cdn.example.com/logo.png')).toBe('Usar URL https valida.');
    expect(validateVisualAssetUrl('data:image/png;base64,AAAA')).toBe('Usar URL https valida.');
    expect(validateVisualAssetUrl('javascript:alert(1)')).toBe('Usar URL https valida.');
    expect(validateVisualAssetUrl('/assets/logo.png')).toBe('Usar URL https valida.');
    expect(validateVisualAssetUrl('https://user:secret@cdn.example.com/logo.png')).toBe('Usar URL https valida.');
    expect(validateVisualAssetUrl('https://cdn.example.com/logo.png#fragment')).toBe('Usar URL https valida.');
    expect(safeVisualAssetUrl('javascript:alert(1)')).toBeNull();
  });
});
