<template>
  <aside class="sidebar" :class="{ open }">
    <div class="sidebar-header">
      <button class="new-chat-btn" @click="$emit('new-chat')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14" />
        </svg>
        <span>开启新对话</span>
      </button>
    </div>

    <div class="history-section">
      <div class="section-title">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
        对话历史
      </div>
      <ul class="conversation-list">
        <li
          v-for="convId in conversations"
          :key="convId"
          class="conversation-item"
          :class="{ active: convId === activeId }"
          :title="convId"
          @click="$emit('switch', convId)"
        >
          <span class="conv-label">{{ convId === 'default' ? '默认对话' : convId }}</span>
          <button
            class="del-btn"
            title="删除会话"
            @click.stop="$emit('delete', convId)"
          >×</button>
        </li>
      </ul>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  conversations: { type: Array, default: () => [] },
  activeId: { type: String, default: null },
  open: { type: Boolean, default: false }
})
defineEmits(['new-chat', 'switch', 'delete'])
</script>

<style scoped>
.conv-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conversation-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.del-btn {
  background: transparent;
  border: none;
  color: #999;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
}
.del-btn:hover {
  background: #fce8e6;
  color: #d93025;
}
</style>
