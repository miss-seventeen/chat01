<template>
  <div v-if="empty" class="welcome-message">
    <div class="welcome-icon">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
    </div>
    <h2>欢迎使用AI智能助手</h2>
    <p>开始新的对话吧！您可以问我任何问题。</p>
  </div>

  <template v-else>
    <div
      v-for="(msg, idx) in messages"
      :key="idx"
      class="message"
      :class="`message-${msg.role}`"
    >
      <div class="message-content">
        <template v-if="msg.role === 'assistant' && msg.content === '' && streaming && idx === messages.length - 1">
          <span class="typing-indicator"><span></span><span></span><span></span></span>
        </template>
        <template v-else>{{ msg.content }}</template>
      </div>
    </div>
  </template>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  streaming: { type: Boolean, default: false }
})

const empty = computed(() => props.messages.length === 0)
</script>
