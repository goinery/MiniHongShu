import { watch } from 'vue'
import { brandName } from '@/config/brand'
import useSettingsStore from '@/store/modules/settings'
import { getLightColor, hexToRgb, normalizeThemeColor } from '@/utils/theme'

export function installBrandFavicon(store) {
  const link = document.querySelector('#brand-favicon')
  if (!(link instanceof HTMLLinkElement)) return
  const settingsStore = useSettingsStore(store)

  return watch(() => [settingsStore.theme, settingsStore.isDark], ([theme, isDark]) => {
    const selected = normalizeThemeColor(theme)
    const backgroundColor = isDark ? getLightColor(selected, .25) : selected
    const [red, green, blue] = hexToRgb(backgroundColor)
    const foregroundColor = (red * 299 + green * 587 + blue * 114) / 1000 > 160 ? '#17191d' : '#ffffff'
    const namespace = 'http://www.w3.org/2000/svg'
    const svg = document.createElementNS(namespace, 'svg')
    svg.setAttribute('viewBox', '0 0 64 64')

    const title = document.createElementNS(namespace, 'title')
    title.textContent = brandName
    const background = document.createElementNS(namespace, 'rect')
    background.setAttribute('width', '64')
    background.setAttribute('height', '64')
    background.setAttribute('rx', '14')
    background.setAttribute('fill', backgroundColor)

    const text = document.createElementNS(namespace, 'text')
    text.textContent = [...brandName].slice(0, 2).join('')
    text.setAttribute('x', '32')
    text.setAttribute('y', '33')
    text.setAttribute('dy', '.35em')
    text.setAttribute('text-anchor', 'middle')
    text.setAttribute('font-family', 'system-ui, sans-serif')
    text.setAttribute('font-size', '25')
    text.setAttribute('font-weight', '700')
    text.setAttribute('fill', foregroundColor)

    svg.append(title, background, text)
    link.href = `data:image/svg+xml,${encodeURIComponent(new XMLSerializer().serializeToString(svg))}`
  }, { immediate: true })
}
