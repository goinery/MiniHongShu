import { getChartAppearance } from '@/utils/chartTheme'
import { getLightColor, normalizeThemeColor } from '@/utils/theme'

// The bundled consoles opt in explicitly; other embedded pages keep their styles.
export const EMBEDDED_APPEARANCES = { swagger: '/swagger-ui/', druid: '/druid/' }

export function embeddedAppearanceCss(kind, theme, isDark) {
  const appearance = getChartAppearance(isDark)
  const selected = normalizeThemeColor(theme)
  const primary = isDark ? getLightColor(selected, .25) : selected
  const scope = `html[data-hongshu-appearance="${kind}"]`
  const common = `
    ${scope} {
      color-scheme: ${isDark ? 'dark' : 'light'};
      --hs-surface: ${appearance.surface};
      --hs-subtle: ${appearance.surfaceSubtle};
      --hs-text: ${appearance.text};
      --hs-muted: ${appearance.muted};
      --hs-border: ${appearance.border};
      --hs-primary: ${primary};
      background: var(--hs-surface) !important;
    }
    ${scope} body { background: var(--hs-surface) !important; color: var(--hs-text) !important; }
    ${scope} :focus, ${scope} :focus-visible { outline: none !important; }
  `
  if (kind === 'swagger') {
    return common + `
      ${scope} .swagger-ui { background: var(--hs-surface) !important; color: var(--hs-text); }
      ${scope} .swagger-ui .topbar .dark-mode-toggle { display: none; }
      ${scope} .swagger-ui .info .title,
      ${scope} .swagger-ui .info p,
      ${scope} .swagger-ui .info li,
      ${scope} .swagger-ui .opblock-tag,
      ${scope} .swagger-ui .opblock-tag small,
      ${scope} .swagger-ui .opblock-summary-path,
      ${scope} .swagger-ui .opblock-summary-description,
      ${scope} .swagger-ui .opblock-description-wrapper p,
      ${scope} .swagger-ui .opblock-external-docs-wrapper,
      ${scope} .swagger-ui .opblock-external-docs-wrapper p,
      ${scope} .swagger-ui .opblock-title_normal,
      ${scope} .swagger-ui .opblock-title_normal p,
      ${scope} .swagger-ui .opblock-section-header h4,
      ${scope} .swagger-ui .opblock-section-header label,
      ${scope} .swagger-ui .responses-inner h4,
      ${scope} .swagger-ui .responses-inner h5,
      ${scope} .swagger-ui .parameter__name,
      ${scope} .swagger-ui .parameter__type,
      ${scope} .swagger-ui .parameter__in,
      ${scope} .swagger-ui .response-col_status,
      ${scope} .swagger-ui .response-col_description,
      ${scope} .swagger-ui .model,
      ${scope} .swagger-ui .model-title,
      ${scope} .swagger-ui .prop-type,
      ${scope} .swagger-ui .prop-format,
      ${scope} .swagger-ui .markdown pre,
      ${scope} .swagger-ui .renderedMarkdown pre,
      ${scope} .swagger-ui section.models h4,
      ${scope} .swagger-ui .tab li,
      ${scope} .swagger-ui .dialog-ux .modal-ux-content p,
      ${scope} .swagger-ui .dialog-ux .modal-ux-content h4,
      ${scope} .swagger-ui .dialog-ux .modal-ux-header h3,
      ${scope} .swagger-ui label { color: var(--hs-text) !important; }
      ${scope} .swagger-ui .scheme-container,
      ${scope} .swagger-ui .opblock .opblock-section-header,
      ${scope} .swagger-ui .dialog-ux .modal-ux,
      ${scope} .swagger-ui section.models,
      ${scope} .swagger-ui .model-box {
        background: var(--hs-surface) !important; border-color: var(--hs-border) !important; box-shadow: none !important;
      }
      ${scope} .swagger-ui .opblock-tag,
      ${scope} .swagger-ui .dialog-ux .modal-ux-header,
      ${scope} .swagger-ui table thead tr th,
      ${scope} .swagger-ui table tbody tr td {
        color: var(--hs-text); border-color: var(--hs-border) !important;
      }
      ${scope} .swagger-ui input:not(.invalid):not([type="checkbox"]):not([type="radio"]),
      ${scope} .swagger-ui textarea:not(.invalid),
      ${scope} .swagger-ui select:not(.invalid) {
        background-color: var(--hs-subtle) !important; color: var(--hs-text) !important; border-color: var(--hs-border) !important;
      }
      ${scope} .swagger-ui input:not(.invalid):focus,
      ${scope} .swagger-ui textarea:not(.invalid):focus,
      ${scope} .swagger-ui select:not(.invalid):focus { box-shadow: none !important; }
      ${scope} .swagger-ui a,
      ${scope} .swagger-ui .info a { color: var(--hs-primary); }
      ${scope} .swagger-ui .btn:not(.cancel),
      ${scope} .swagger-ui .btn.execute,
      ${scope} .swagger-ui .btn.authorize {
        background: var(--hs-surface); border-color: var(--hs-primary); color: var(--hs-primary); box-shadow: none;
      }
      ${scope} .swagger-ui .btn.authorize svg,
      ${scope} .swagger-ui .authorization__btn svg { fill: var(--hs-primary); }
      ${scope} .swagger-ui .expand-operation svg,
      ${scope} .swagger-ui .opblock-control-arrow svg,
      ${scope} .swagger-ui .models-control svg { fill: var(--hs-text); }
    `
  }
  if (kind === 'druid') {
    return common + `
      ${scope} h1, ${scope} h2, ${scope} h3, ${scope} h4, ${scope} label, ${scope} legend,
      ${scope} .help-block, ${scope} .help-inline { color: var(--hs-text); }
      ${scope} .muted { color: var(--hs-muted); }
      ${scope} a, ${scope} #dataTable th > a, ${scope} #dataTable1 th > a,
      ${scope} #dataTable2 th > a { color: var(--hs-primary); }
      ${scope} .navbar-inner,
      ${scope} .content,
      ${scope} .footer,
      ${scope} .well,
      ${scope} .modal,
      ${scope} .modal-footer,
      ${scope} .dropdown-menu {
        background: var(--hs-surface) !important; color: var(--hs-text); border-color: var(--hs-border); box-shadow: none;
      }
      ${scope} .navbar .brand,
      ${scope} .navbar .nav > li > a { color: var(--hs-text); text-shadow: none; }
      ${scope} .navbar .nav .active > a,
      ${scope} .navbar .nav > li > a:hover,
      ${scope} .nav-tabs > .active > a,
      ${scope} .nav-tabs > .active > a:hover,
      ${scope} .dropdown-menu li > a:hover {
        background: var(--hs-subtle); color: var(--hs-primary); border-color: var(--hs-border); text-shadow: none;
      }
      ${scope} table, ${scope} .striped { background: var(--hs-surface) !important; color: var(--hs-text); }
      ${scope} table th, ${scope} table td, ${scope} .table-bordered,
      ${scope} .nav-tabs, ${scope} .modal-header { border-color: var(--hs-border) !important; }
      ${scope} #dataTable th, ${scope} #dataTable1 th, ${scope} #dataTable2 th,
      ${scope} .td_lable, ${scope} .table-striped tbody > tr:nth-child(odd) > td,
      ${scope} .table-striped tbody > tr:nth-child(odd) > th,
      ${scope} .table tbody tr:hover td, ${scope} .table tbody tr:hover th {
        background: var(--hs-subtle) !important; color: var(--hs-text) !important;
      }
      ${scope} input:not([type="checkbox"]):not([type="radio"]):not([type="submit"]):not([type="button"]),
      ${scope} textarea, ${scope} select {
        background: var(--hs-subtle); color: var(--hs-text); border-color: var(--hs-border);
      }
      ${scope} input:focus, ${scope} textarea:focus, ${scope} select:focus {
        border-color: var(--hs-primary); box-shadow: none !important;
      }
      ${scope} .btn:not(.btn-primary):not(.btn-danger):not(.btn-warning):not(.btn-success):not(.btn-info) {
        background: var(--hs-subtle); color: var(--hs-text); border-color: var(--hs-border); text-shadow: none; box-shadow: none;
      }
      ${scope} .btn.btn-primary { background: var(--hs-surface); color: var(--hs-primary); border-color: var(--hs-primary); text-shadow: none; box-shadow: none; }
      ${scope} pre, ${scope} code { background: var(--hs-subtle); color: var(--hs-text); border-color: var(--hs-border); }
      ${scope} hr { border-color: var(--hs-border); }
    `
  }
  return ''
}
