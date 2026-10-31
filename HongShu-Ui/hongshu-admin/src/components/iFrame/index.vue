<template>
  <div ref="containerRef" v-loading="loading" class="console-iframe" :style="{ height: height + 'px' }">
    <iframe
      ref="frameRef"
      :src="src"
      :title="title"
      @load="handleLoad"
    />
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import useSettingsStore from '@/store/modules/settings'
import { EMBEDDED_APPEARANCES, embeddedAppearanceCss } from './appearance'

const props = defineProps({
  src: { type: String, required: true },
  title: { type: String, default: '嵌入页面' },
  appearance: { type: String, default: '', validator: value => value === '' || Object.hasOwn(EMBEDDED_APPEARANCES, value) },
})

const settingsStore = useSettingsStore()
const containerRef = ref(null)
const frameRef = ref(null)
const height = ref(640)
const loading = ref(true)
const STYLE_ID = 'hongshu-embedded-appearance'
const originalDarkModes = new WeakMap()

function updateHeight() {
  if (containerRef.value) {
    height.value = Math.max(320, window.innerHeight - containerRef.value.getBoundingClientRect().top - 24)
  }
}

function canThemeDocument(document) {
  if (!Object.hasOwn(EMBEDDED_APPEARANCES, props.appearance)) return false
  const segment = EMBEDDED_APPEARANCES[props.appearance]
  const source = new URL(props.src, window.location.href)
  const current = new URL(document.URL)
  const sectionIndex = source.pathname.indexOf(segment)
  if (sectionIndex < 0 || source.origin !== window.location.origin || current.origin !== source.origin) return false
  return current.pathname.startsWith(source.pathname.slice(0, sectionIndex + segment.length))
}

function syncAppearance() {
  try {
    const embeddedDocument = frameRef.value?.contentDocument
    if (!embeddedDocument?.head) return
    if (!canThemeDocument(embeddedDocument)) {
      embeddedDocument.getElementById(STYLE_ID)?.remove()
      if (originalDarkModes.has(embeddedDocument)) {
        embeddedDocument.documentElement.classList.toggle('dark-mode', originalDarkModes.get(embeddedDocument))
        originalDarkModes.delete(embeddedDocument)
      }
      embeddedDocument.documentElement.removeAttribute('data-hongshu-appearance')
      return
    }
    let style = embeddedDocument.getElementById(STYLE_ID)
    if (!style) {
      style = embeddedDocument.createElement('style')
      style.id = STYLE_ID
      embeddedDocument.head.appendChild(style)
    }
    if (props.appearance === 'swagger') {
      if (!originalDarkModes.has(embeddedDocument)) {
        originalDarkModes.set(embeddedDocument, embeddedDocument.documentElement.classList.contains('dark-mode'))
      }
      embeddedDocument.documentElement.classList.toggle('dark-mode', settingsStore.isDark)
    }
    embeddedDocument.documentElement.setAttribute('data-hongshu-appearance', props.appearance)
    style.textContent = embeddedAppearanceCss(props.appearance, settingsStore.theme, settingsStore.isDark)
  } catch {
    // Cross-origin documents and redirects retain their own presentation.
  }
}

function handleLoad() {
  loading.value = false
  syncAppearance()
  updateHeight()
}

watch(() => props.src, () => { loading.value = true })
watch(() => [settingsStore.theme, settingsStore.isDark, props.appearance], syncAppearance)
onMounted(() => {
  updateHeight()
  window.addEventListener('resize', updateHeight)
})
onBeforeUnmount(() => window.removeEventListener('resize', updateHeight))
</script>

<style scoped>
.console-iframe { min-height: 320px; background: var(--cf-surface); }
.console-iframe iframe { display: block; width: 100%; height: 100%; border: 0; }
</style>
