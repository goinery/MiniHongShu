export const DEFAULT_THEME_COLOR = '#ff6699'

export function normalizeThemeColor(value) {
	const color = typeof value === 'string' ? value.trim() : ''
	if (/^#[\da-f]{6}$/i.test(color)) return color.toLowerCase()
	if (/^#[\da-f]{3}$/i.test(color)) {
		return '#' + color.slice(1).split('').map(channel => channel + channel).join('').toLowerCase()
	}
	const rgb = color.match(/^rgb\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*\)$/i)
	if (rgb) {
		const channels = rgb.slice(1).map(Number)
		if (channels.every(channel => channel <= 255)) return rgbToHex(...channels)
	}
	return DEFAULT_THEME_COLOR
}

// 处理主题样式
export function handleThemeStyle(theme, isDark = false) {
	theme = normalizeThemeColor(theme)
	if (isDark) theme = getLightColor(theme, .25)
	document.documentElement.style.setProperty('--el-color-primary', theme)
	for (let i = 1; i <= 9; i++) {
		const color = isDark ? mixColors(theme, '#202328', i / 10) : getLightColor(theme, i / 10)
		document.documentElement.style.setProperty(`--el-color-primary-light-${i}`, color)
	}
	for (let i = 1; i <= 9; i++) {
		document.documentElement.style.setProperty(`--el-color-primary-dark-${i}`, isDark ? getLightColor(theme, i / 10) : getDarkColor(theme, i / 10))
	}
}

function mixColors(color, target, amount) {
	const from = hexToRgb(color)
	const to = hexToRgb(target)
	return rgbToHex(...from.map((channel, index) => Math.round(channel + (to[index] - channel) * amount)))
}

/**
 * @param {unknown} str
 * @returns {[number, number, number]}
 */
export function hexToRgb(str) {
	const color = normalizeThemeColor(str).slice(1)
	return [parseInt(color.slice(0, 2), 16), parseInt(color.slice(2, 4), 16), parseInt(color.slice(4, 6), 16)]
}

/**
 * @param {number} r
 * @param {number} g
 * @param {number} b
 * @returns {string}
 */
export function rgbToHex(r, g, b) {
	let hexs = [r.toString(16), g.toString(16), b.toString(16)]
	for (let i = 0; i < 3; i++) {
		if (hexs[i].length === 1) {
			hexs[i] = `0${hexs[i]}`
		}
	}
	return `#${hexs.join('')}`
}

// 变浅颜色值
export function getLightColor(color, level) {
	let rgb = hexToRgb(color)
	for (let i = 0; i < 3; i++) {
		rgb[i] = Math.floor((255 - rgb[i]) * level + rgb[i])
	}
	return rgbToHex(rgb[0], rgb[1], rgb[2])
}

// 变深颜色值
export function getDarkColor(color, level) {
	let rgb = hexToRgb(color)
	for (let i = 0; i < 3; i++) {
		rgb[i] = Math.floor(rgb[i] * (1 - level))
	}
	return rgbToHex(rgb[0], rgb[1], rgb[2])
}
