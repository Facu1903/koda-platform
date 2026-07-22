import { describe, expect, it } from 'vitest';
import type { CompanyProfileRegional } from './companyProfile';
import { createRegionalFormatters } from './regionalFormatting';

const usRegional: CompanyProfileRegional = {
  defaultLocale: 'en-US',
  defaultCurrency: 'USD',
  timeZone: 'UTC',
  dateFormat: 'MM/dd/yyyy',
  timeFormat: 'HH:mm',
  numberLocale: 'en-US',
  currencyFormat: 'symbol',
};

describe('regionalFormatting', () => {
  it('formats date and time with tenant regional patterns', () => {
    const formatters = createRegionalFormatters(usRegional);

    expect(formatters.formatDate('2026-07-21T12:05:06Z')).toBe('07/21/2026');
    expect(formatters.formatTime('2026-07-21T12:05:06Z')).toBe('12:05');
    expect(formatters.formatDateTime('2026-07-21T12:05:06Z')).toBe('07/21/2026 12:05');
  });

  it('formats numbers and money with tenant regional settings', () => {
    const formatters = createRegionalFormatters(usRegional);

    expect(formatters.formatNumber(1234.5)).toBe('1,234.5');
    expect(formatters.formatCurrency(1234.5)).toBe('$1,234.50');
  });

  it('supports code currency display when the tenant requests it', () => {
    const formatters = createRegionalFormatters({
      ...usRegional,
      currencyFormat: 'code',
    });

    expect(formatters.formatCurrency(10)).toContain('USD');
  });

  it('returns controlled labels for empty, invalid and unsafe regional values', () => {
    const formatters = createRegionalFormatters({
      ...usRegional,
      defaultCurrency: 'NOT_A_CURRENCY',
      defaultLocale: 'invalid_locale',
      numberLocale: 'invalid_locale',
      timeZone: 'Invalid/Timezone',
    });

    expect(formatters.formatDateTime(null, { emptyLabel: 'Pending' })).toBe('Pending');
    expect(formatters.formatDateTime('not-a-date', { invalidLabel: 'Invalid date' })).toBe('Invalid date');
    expect(formatters.formatCurrency(null, { emptyLabel: 'No amount' })).toBe('No amount');
    expect(formatters.formatCurrency(10)).not.toHaveLength(0);
  });
});
