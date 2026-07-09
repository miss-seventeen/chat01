<template>
  <div class="input-area">
    <div class="input-wrapper">
      <textarea
        ref="inputRef"
        v-model="text"
        placeholder="输入您的问题..."
        rows="1"
        maxlength="5000"
        :disabled="disabled"
        @keydown="onKeyDown"
        @input="autoResize"
      ></textarea>
      <button class="send-btn" :disabled="disabled || !text.trim()" @click="submit">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 2L11 13M22 2L15 22L11 13M11 13L2 9L22 2" />
        </svg>
      </button>
    </div>
    <div class="input-tips">按 Enter 发送，Shift + Enter 换行</div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const props = defineProps({
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['send'])

const text = ref('')
const inputRef = ref(null)

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 150) + 'px'
}

function onKeyDown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    submit()
  }
}

function submit() {
  const v = text.value.trim()
  if (!v || props.disabled) return
  emit('send', v)
  text.value = ''
  nextTick(autoResize)
}
</script>
