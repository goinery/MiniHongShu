/**
* v-copyText 复制文本内容
* Copyright (c) 2022 ruoyi
*/

export default {
  beforeMount(el, { value, arg }) {
    if (arg === "callback") {
      el.$copyCallback = value;
    } else {
      el.$copyValue = value;
      const handler = async () => {
        const copied = await copyTextToClipboard(el.$copyValue);
        if (copied && typeof el.$copyCallback === 'function') {
          el.$copyCallback(el.$copyValue);
        }
      };
      el.addEventListener("click", handler);
      el.$destroyCopy = () => el.removeEventListener("click", handler);
    }
  },
  updated(el, { value, arg }) {
    if (arg === 'callback') {
      el.$copyCallback = value;
    } else {
      el.$copyValue = value;
    }
  },
  unmounted(el) {
    el.$destroyCopy?.();
    delete el.$destroyCopy;
    delete el.$copyValue;
    delete el.$copyCallback;
  },
}

async function copyTextToClipboard(input, { target = document.body } = {}) {
  const text = String(input ?? '');
  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      // 未获剪贴板授权时，继续使用当前点击事件支持的兼容方案。
    }
  }
  const element = document.createElement('textarea');
  const previouslyFocusedElement = document.activeElement;

  element.value = text;

  // Prevent keyboard from showing on mobile
  element.setAttribute('readonly', '');

  element.style.contain = 'strict';
  element.style.position = 'absolute';
  element.style.left = '-9999px';
  element.style.fontSize = '12pt'; // Prevent zooming on iOS

  const selection = document.getSelection();
  const originalRange = selection && selection.rangeCount > 0 && selection.getRangeAt(0);

  target.append(element);
  element.select();

  // Explicit selection workaround for iOS
  element.selectionStart = 0;
  element.selectionEnd = text.length;

  let isSuccess;
  try {
    isSuccess = document.execCommand('copy');
  } catch {
    isSuccess = false;
  }

  element.remove();

  if (originalRange) {
    selection.removeAllRanges();
    selection.addRange(originalRange);
  }

  // Get the focus back on the previously focused element, if any
  if (previouslyFocusedElement instanceof HTMLElement) {
    previouslyFocusedElement.focus();
  }

  return isSuccess;
}
