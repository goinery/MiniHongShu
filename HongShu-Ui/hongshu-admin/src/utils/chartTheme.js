import { normalizeThemeColor as normalizeColor } from '@/utils/theme';

// Chart series share the selected accent hue and vary only in lightness.

function channels(color) {
  const value = parseInt(normalizeColor(color).slice(1), 16);
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255];
}

function mix(color, target, amount) {
  return '#' + channels(color).map((channel) => Math.round(channel + (target - channel) * amount).toString(16).padStart(2, '0')).join('');
}

export function getChartAppearance(isDark = false) {
  return isDark
    ? {
        surface: '#202328', surfaceSubtle: '#25292f', text: '#e6e8ec', muted: '#a1a7b1',
        border: '#363b44', grid: '#363b44', tooltip: '#202328',
        shadow: '0 2px 8px rgba(0,0,0,.28)',
      }
    : {
        surface: '#ffffff', surfaceSubtle: '#fafafa', text: '#202124', muted: '#70757c',
        border: '#e5e7eb', grid: '#edf0f3', tooltip: '#ffffff',
        shadow: '0 2px 8px rgba(15,23,42,.06)',
      };
}

export function getChartPalette(theme, count = 8, isDark = false) {
  const selected = normalizeColor(theme);
  const base = isDark ? mix(selected, 255, .25) : selected;
  const palette = isDark
    ? [base, mix(base, 255, .42), mix(base, 0, .15), mix(base, 255, .65), mix(base, 0, .3), mix(base, 255, .2), mix(base, 255, .54), mix(base, 255, .32)]
    : [base, mix(base, 255, .48), mix(base, 0, .3), mix(base, 255, .7), mix(base, 0, .5), mix(base, 255, .24), mix(base, 255, .58), mix(base, 0, .15)];
  return Array.from({ length: count }, (_, index) => palette[index % palette.length]);
}

export function getChartHeatmapColors(theme, isDark = false) {
  const selected = normalizeColor(theme);
  const base = isDark ? mix(selected, 255, .25) : selected;
  if (isDark) {
    const surface = channels(getChartAppearance(true).surface);
    const blend = (amount) => '#' + channels(base).map((channel, index) => Math.round(channel + (surface[index] - channel) * amount).toString(16).padStart(2, '0')).join('');
    return [blend(.9), blend(.6), base, mix(base, 255, .25), mix(base, 255, .55)];
  }
  return [mix(base, 255, .95), mix(base, 255, .74), mix(base, 255, .46), base, mix(base, 0, .25)];
}

export function themeColorWithAlpha(theme, alpha) {
  return `rgba(${channels(theme).join(',')},${alpha})`;
}
