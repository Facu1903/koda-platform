import type { CompanyProfileRegional } from './companyProfile';
import { fallbackCompanyProfile } from './companyProfile';

export type RegionalDateInput = Date | number | string | null | undefined;
export type RegionalNumberInput = number | null | undefined;

export interface RegionalFormatOptions {
  emptyLabel?: string;
  invalidLabel?: string;
}

export interface RegionalCurrencyFormatOptions extends RegionalFormatOptions {
  currency?: string;
}

export interface RegionalFormatters {
  currency: string;
  locale: string;
  numberLocale: string;
  timeZone: string;
  formatCurrency: (value: RegionalNumberInput, options?: RegionalCurrencyFormatOptions) => string;
  formatDate: (value: RegionalDateInput, options?: RegionalFormatOptions) => string;
  formatDateTime: (value: RegionalDateInput, options?: RegionalFormatOptions) => string;
  formatNumber: (value: RegionalNumberInput, options?: RegionalFormatOptions) => string;
  formatTime: (value: RegionalDateInput, options?: RegionalFormatOptions) => string;
}

interface DateParts {
  day: string;
  day2: string;
  month: string;
  month2: string;
  monthLong: string;
  monthShort: string;
  year: string;
  year2: string;
}

interface TimeParts {
  dayPeriod: string;
  hour12: string;
  hour12Padded: string;
  hour24: string;
  hour24Padded: string;
  minute: string;
  minute2: string;
  second: string;
  second2: string;
}

const fallbackRegional = fallbackCompanyProfile.regional;
const defaultEmptyLabel = '-';
const defaultInvalidDateLabel = 'Fecha invalida';
const defaultInvalidNumberLabel = 'Numero invalido';
const dateTokens = ['MMMM', 'yyyy', 'MMM', 'MM', 'dd', 'yy', 'M', 'd'];
const timeTokens = ['HH', 'hh', 'mm', 'ss', 'H', 'h', 'm', 's', 'a'];
const safeLiteralPattern = /^[\s.,/:-]$/;

export function createRegionalFormatters(regional: CompanyProfileRegional): RegionalFormatters {
  return {
    currency: regional.defaultCurrency,
    locale: regional.defaultLocale,
    numberLocale: regional.numberLocale,
    timeZone: regional.timeZone,
    formatCurrency: (value, options) => formatCurrency(value, regional, options),
    formatDate: (value, options) => formatDate(value, regional, options),
    formatDateTime: (value, options) => formatDateTime(value, regional, options),
    formatNumber: (value, options) => formatNumber(value, regional, options),
    formatTime: (value, options) => formatTime(value, regional, options),
  };
}

function formatDate(value: RegionalDateInput, regional: CompanyProfileRegional, options?: RegionalFormatOptions): string {
  const parsed = parseDateInput(value);

  if (parsed === null) {
    return resolveEmptyLabel(value, options, defaultInvalidDateLabel);
  }

  return formatDateValue(parsed, regional);
}

function formatDateTime(value: RegionalDateInput, regional: CompanyProfileRegional, options?: RegionalFormatOptions): string {
  const parsed = parseDateInput(value);

  if (parsed === null) {
    return resolveEmptyLabel(value, options, defaultInvalidDateLabel);
  }

  return `${formatDateValue(parsed, regional)} ${formatTimeValue(parsed, regional)}`;
}

function formatTime(value: RegionalDateInput, regional: CompanyProfileRegional, options?: RegionalFormatOptions): string {
  const parsed = parseDateInput(value);

  if (parsed === null) {
    return resolveEmptyLabel(value, options, defaultInvalidDateLabel);
  }

  return formatTimeValue(parsed, regional);
}

function formatNumber(value: RegionalNumberInput, regional: CompanyProfileRegional, options?: RegionalFormatOptions): string {
  if (value === null || value === undefined) {
    return options?.emptyLabel ?? defaultEmptyLabel;
  }

  if (!Number.isFinite(value)) {
    return options?.invalidLabel ?? defaultInvalidNumberLabel;
  }

  return safeNumberFormatter(regional).format(value);
}

function formatCurrency(
  value: RegionalNumberInput,
  regional: CompanyProfileRegional,
  options?: RegionalCurrencyFormatOptions,
): string {
  if (value === null || value === undefined) {
    return options?.emptyLabel ?? defaultEmptyLabel;
  }

  if (!Number.isFinite(value)) {
    return options?.invalidLabel ?? defaultInvalidNumberLabel;
  }

  return safeCurrencyFormatter(regional, options?.currency ?? regional.defaultCurrency).format(value);
}

function formatDateValue(date: Date, regional: CompanyProfileRegional): string {
  const parts = safeDateParts(date, regional);
  const formatted = formatPattern(
    regional.dateFormat,
    {
      d: parts.day,
      dd: parts.day2,
      M: parts.month,
      MM: parts.month2,
      MMM: parts.monthShort,
      MMMM: parts.monthLong,
      yy: parts.year2,
      yyyy: parts.year,
    },
    dateTokens,
  );

  if (formatted !== null) {
    return formatted;
  }

  return safeDateFormatter(regional, { dateStyle: 'medium' }).format(date);
}

function formatTimeValue(date: Date, regional: CompanyProfileRegional): string {
  const parts = safeTimeParts(date, regional);
  const formatted = formatPattern(
    regional.timeFormat,
    {
      H: parts.hour24,
      HH: parts.hour24Padded,
      h: parts.hour12,
      hh: parts.hour12Padded,
      m: parts.minute,
      mm: parts.minute2,
      s: parts.second,
      ss: parts.second2,
      a: parts.dayPeriod,
    },
    timeTokens,
  );

  if (formatted !== null) {
    return formatted;
  }

  return safeDateFormatter(regional, { timeStyle: 'short' }).format(date);
}

function parseDateInput(value: RegionalDateInput): Date | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  const parsed = value instanceof Date ? value : new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return null;
  }

  return parsed;
}

function resolveEmptyLabel(
  value: RegionalDateInput,
  options: RegionalFormatOptions | undefined,
  defaultInvalidLabel: string,
): string {
  if (value === null || value === undefined || value === '') {
    return options?.emptyLabel ?? defaultEmptyLabel;
  }

  return options?.invalidLabel ?? defaultInvalidLabel;
}

function formatPattern(pattern: string, tokenValues: Record<string, string>, tokens: string[]): string | null {
  let formatted = '';
  let index = 0;

  while (index < pattern.length) {
    const current = pattern[index];

    if (current === "'") {
      const closingIndex = pattern.indexOf("'", index + 1);

      if (closingIndex < 0) {
        return null;
      }

      formatted += pattern.slice(index + 1, closingIndex);
      index = closingIndex + 1;
      continue;
    }

    const token = tokens.find((candidate) => pattern.startsWith(candidate, index));

    if (token !== undefined) {
      formatted += tokenValues[token];
      index += token.length;
      continue;
    }

    if (safeLiteralPattern.test(current)) {
      formatted += current;
      index += 1;
      continue;
    }

    return null;
  }

  return formatted;
}

function safeDateParts(date: Date, regional: CompanyProfileRegional): DateParts {
  const primary = createDateParts(date, regional);

  if (primary !== null) {
    return primary;
  }

  const fallback = createDateParts(date, fallbackRegional);

  if (fallback !== null) {
    return fallback;
  }

  const utcDay = String(date.getUTCDate());
  const utcMonth = String(date.getUTCMonth() + 1);
  const utcYear = String(date.getUTCFullYear());

  return {
    day: utcDay,
    day2: utcDay.padStart(2, '0'),
    month: utcMonth,
    month2: utcMonth.padStart(2, '0'),
    monthLong: utcMonth.padStart(2, '0'),
    monthShort: utcMonth.padStart(2, '0'),
    year: utcYear,
    year2: utcYear.slice(-2),
  };
}

function createDateParts(date: Date, regional: CompanyProfileRegional): DateParts | null {
  try {
    const numericParts = new Intl.DateTimeFormat(regional.defaultLocale, {
      day: '2-digit',
      month: '2-digit',
      timeZone: regional.timeZone,
      year: 'numeric',
    }).formatToParts(date);
    const monthShortParts = new Intl.DateTimeFormat(regional.defaultLocale, {
      month: 'short',
      timeZone: regional.timeZone,
    }).formatToParts(date);
    const monthLongParts = new Intl.DateTimeFormat(regional.defaultLocale, {
      month: 'long',
      timeZone: regional.timeZone,
    }).formatToParts(date);
    const day2 = getPart(numericParts, 'day');
    const month2 = getPart(numericParts, 'month');
    const year = getPart(numericParts, 'year');

    if (day2 === null || month2 === null || year === null) {
      return null;
    }

    return {
      day: stripLeadingZero(day2),
      day2,
      month: stripLeadingZero(month2),
      month2,
      monthLong: getPart(monthLongParts, 'month') ?? month2,
      monthShort: getPart(monthShortParts, 'month') ?? month2,
      year,
      year2: year.slice(-2),
    };
  } catch {
    return null;
  }
}

function safeTimeParts(date: Date, regional: CompanyProfileRegional): TimeParts {
  const primary = createTimeParts(date, regional);

  if (primary !== null) {
    return primary;
  }

  const fallback = createTimeParts(date, fallbackRegional);

  if (fallback !== null) {
    return fallback;
  }

  const utcHour = String(date.getUTCHours());
  const utcMinute = String(date.getUTCMinutes());
  const utcSecond = String(date.getUTCSeconds());

  return {
    dayPeriod: date.getUTCHours() >= 12 ? 'PM' : 'AM',
    hour12: String(toHour12(date.getUTCHours())),
    hour12Padded: String(toHour12(date.getUTCHours())).padStart(2, '0'),
    hour24: utcHour,
    hour24Padded: utcHour.padStart(2, '0'),
    minute: utcMinute,
    minute2: utcMinute.padStart(2, '0'),
    second: utcSecond,
    second2: utcSecond.padStart(2, '0'),
  };
}

function createTimeParts(date: Date, regional: CompanyProfileRegional): TimeParts | null {
  try {
    const hour24Parts = new Intl.DateTimeFormat(regional.defaultLocale, {
      hour: '2-digit',
      hourCycle: 'h23',
      minute: '2-digit',
      second: '2-digit',
      timeZone: regional.timeZone,
    }).formatToParts(date);
    const hour12Parts = new Intl.DateTimeFormat(regional.defaultLocale, {
      hour: '2-digit',
      hourCycle: 'h12',
      minute: '2-digit',
      second: '2-digit',
      timeZone: regional.timeZone,
    }).formatToParts(date);
    const hour24 = getPart(hour24Parts, 'hour');
    const hour12 = getPart(hour12Parts, 'hour');
    const minute = getPart(hour24Parts, 'minute');
    const second = getPart(hour24Parts, 'second');

    if (hour24 === null || hour12 === null || minute === null || second === null) {
      return null;
    }

    return {
      dayPeriod: getPart(hour12Parts, 'dayPeriod') ?? '',
      hour12: stripLeadingZero(hour12),
      hour12Padded: hour12,
      hour24: stripLeadingZero(hour24),
      hour24Padded: hour24,
      minute: stripLeadingZero(minute),
      minute2: minute,
      second: stripLeadingZero(second),
      second2: second,
    };
  } catch {
    return null;
  }
}

function safeDateFormatter(regional: CompanyProfileRegional, options: Intl.DateTimeFormatOptions): Intl.DateTimeFormat {
  try {
    return new Intl.DateTimeFormat(regional.defaultLocale, {
      ...options,
      timeZone: regional.timeZone,
    });
  } catch {
    return new Intl.DateTimeFormat(fallbackRegional.defaultLocale, {
      ...options,
      timeZone: fallbackRegional.timeZone,
    });
  }
}

function safeNumberFormatter(regional: CompanyProfileRegional): Intl.NumberFormat {
  try {
    return new Intl.NumberFormat(regional.numberLocale);
  } catch {
    return new Intl.NumberFormat(fallbackRegional.numberLocale);
  }
}

function safeCurrencyFormatter(regional: CompanyProfileRegional, currency: string): Intl.NumberFormat {
  try {
    return new Intl.NumberFormat(regional.numberLocale, {
      currency,
      currencyDisplay: normalizeCurrencyDisplay(regional.currencyFormat),
      style: 'currency',
    });
  } catch {
    return new Intl.NumberFormat(fallbackRegional.numberLocale, {
      currency: fallbackRegional.defaultCurrency,
      currencyDisplay: normalizeCurrencyDisplay(fallbackRegional.currencyFormat),
      style: 'currency',
    });
  }
}

function normalizeCurrencyDisplay(value: string): Intl.NumberFormatOptions['currencyDisplay'] {
  if (value === 'code' || value === 'name' || value === 'narrowSymbol' || value === 'symbol') {
    return value;
  }

  return 'symbol';
}

function getPart(parts: Intl.DateTimeFormatPart[], type: Intl.DateTimeFormatPartTypes): string | null {
  return parts.find((part) => part.type === type)?.value ?? null;
}

function stripLeadingZero(value: string): string {
  return value.replace(/^0/, '');
}

function toHour12(hour: number): number {
  const remainder = hour % 12;
  return remainder === 0 ? 12 : remainder;
}
