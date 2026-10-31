import { defineStore } from "pinia";
import { store } from "@/store";

const THEME_KEY = "theme";
const THEME_COLOR_KEY = "theme-color";
export const DEFAULT_THEME_COLOR = "#ff6699";

export type ThemeMode = "light" | "dark" | "system";
type RgbColor = [number, number, number];

let systemThemeQuery: MediaQueryList | undefined;
let stopSystemThemeListener: (() => void) | undefined;

function readPreference(key: string): string | null {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function savePreference(key: string, value: string) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(key, value);
  } catch {
    // The current session still supports themes when storage is unavailable.
  }
}

function readStoredTheme(): ThemeMode {
  const theme = readPreference(THEME_KEY);
  return theme === "dark" || theme === "system" ? theme : "light";
}

function normalizeColor(color: string): string | null {
  const value = color.trim().toLowerCase();
  if (/^#[0-9a-f]{6}$/.test(value)) return value;
  if (/^#[0-9a-f]{3}$/.test(value)) {
    return `#${value.slice(1).split("").map((channel) => channel + channel).join("")}`;
  }
  return null;
}

function readStoredColor(): string {
  return normalizeColor(readPreference(THEME_COLOR_KEY) || "") || DEFAULT_THEME_COLOR;
}

function getSystemThemeQuery() {
  if (!systemThemeQuery && typeof window !== "undefined" && typeof window.matchMedia === "function") {
    systemThemeQuery = window.matchMedia("(prefers-color-scheme: dark)");
  }
  return systemThemeQuery;
}

function mixColor(color: RgbColor, target: RgbColor, weight: number): string {
  const channels = color.map((channel, index) => Math.round(channel * (1 - weight) + target[index] * weight));
  return `#${channels.map((channel) => channel.toString(16).padStart(2, "0")).join("")}`;
}

function luminance(color: RgbColor): number {
  const channels = color.map((channel) => {
    const value = channel / 255;
    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  });
  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}

function applyDocumentTheme(theme: ThemeMode, color: string) {
  if (typeof document === "undefined") return;

  const dark = theme === "dark" || (theme === "system" && Boolean(getSystemThemeQuery()?.matches));
  const root = document.documentElement;
  root.classList.toggle("dark-mode", dark);
  root.classList.toggle("dark", dark);
  root.style.colorScheme = dark ? "dark" : "light";

  const rgb: RgbColor = [parseInt(color.slice(1, 3), 16), parseInt(color.slice(3, 5), 16), parseInt(color.slice(5, 7), 16)];
  const surface: RgbColor = dark ? [31, 41, 55] : [255, 255, 255];
  root.style.setProperty("--brand-color", color);
  root.style.setProperty("--brand-color-rgb", rgb.join(", "));
  root.style.setProperty("--el-color-primary", color);
  root.style.setProperty("--el-color-primary-rgb", rgb.join(", "));
  for (const level of [3, 5, 7, 8, 9]) {
    root.style.setProperty(`--el-color-primary-light-${level}`, mixColor(rgb, surface, level / 10));
  }
  root.style.setProperty("--el-color-primary-dark-2", mixColor(rgb, dark ? [255, 255, 255] : [0, 0, 0], 0.2));

  const colorLuminance = luminance(rgb);
  const whiteContrast = 1.05 / (colorLuminance + 0.05);
  const darkLuminance = luminance([17, 24, 39]);
  const darkContrast = (Math.max(colorLuminance, darkLuminance) + 0.05) / (Math.min(colorLuminance, darkLuminance) + 0.05);
  root.style.setProperty("--brand-color-contrast", whiteContrast >= darkContrast ? "#ffffff" : "#111827");
}

export const themeStore = defineStore("themeStore", {
  state: () => ({
    theme: readStoredTheme(),
    themeColor: readStoredColor(),
  }),
  actions: {
    initTheme() {
      this.theme = readStoredTheme();
      this.themeColor = readStoredColor();
      applyDocumentTheme(this.theme, this.themeColor);

      stopSystemThemeListener?.();
      const query = getSystemThemeQuery();
      if (query) {
        const onSystemThemeChange = () => {
          if (this.theme === "system") applyDocumentTheme(this.theme, this.themeColor);
        };
        query.addEventListener("change", onSystemThemeChange);
        stopSystemThemeListener = () => query.removeEventListener("change", onSystemThemeChange);
      }
    },
    setTheme(theme: ThemeMode) {
      this.theme = theme;
      savePreference(THEME_KEY, theme);
      applyDocumentTheme(theme, this.themeColor);
    },
    setThemeColor(color: string) {
      const normalized = normalizeColor(color);
      if (!normalized) return;
      this.themeColor = normalized;
      savePreference(THEME_COLOR_KEY, normalized);
      applyDocumentTheme(this.theme, normalized);
    },
    resetThemeColor() {
      this.setThemeColor(DEFAULT_THEME_COLOR);
    },
  },
});

export function useThemeStore() {
  return themeStore(store);
}

if (import.meta.hot) {
  import.meta.hot.dispose(() => stopSystemThemeListener?.());
}
