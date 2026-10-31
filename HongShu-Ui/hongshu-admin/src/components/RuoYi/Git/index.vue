<template>
  <div>
    <svg-icon icon-class="github" @click="goto" />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const url = ref("https://github.com/Ma-YongJian/HongShu");

function goto() {
  window.open(url.value);
}
</script>
