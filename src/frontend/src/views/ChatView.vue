<template>
  <div class="app-container">
    <Sidebar
      :conversations="conversations"
      :active-id="currentConversationId"
      @new-chat="startNewConversation"
      @switch="switchConversation"
      @delete="removeConversation"
    />

    <main class="chat-main">
      <div ref="scrollRef" class="messages-container">
        <MessageList :messages="messages" :streaming="isStreaming" />
      </div>
      <MessageInput :disabled="isStreaming" @send="sendMessage" />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import MessageList from '../components/MessageList.vue'
import MessageInput from '../components/MessageInput.vue'
import {
  fetchConversations,
  fetchHistory,
  streamChat,
  deleteConversation
} from '../api/chat.js'

function generateUserId() {
  return 'user_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}
function generateConversationId() {
  return 'conv_' + Date.now()
}

const userId = ref(localStorage.getItem('userId') || generateUserId())
localStorage.setItem('userId', userId.value)

const conversations = ref([])
const currentConversationId = ref(null)
const messages = ref([])
const isStreaming = ref(false)
const scrollRef = ref(null)

function scrollToBottom() {
  nextTick(() => {
    const el = scrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

watch(messages, scrollToBottom, { deep: true })

async function loadConversations() {
  try {
    const list = await fetchConversations(userId.value)
    conversations.value = Array.isArray(list) ? list : Array.from(list || [])
  } catch (e) {
    console.error('加载对话列表失败:', e)
  }
}

function startNewConversation() {
  if (isStreaming.value) return
  currentConversationId.value = null
  messages.value = []
}

async function switchConversation(convId) {
  if (isStreaming.value) {
    alert('正在生成回复中，请稍候')
    return
  }
  currentConversationId.value = convId
  messages.value = []
  try {
    const history = await fetchHistory(userId.value, convId)
    if (Array.isArray(history) && history.length > 0) {
      messages.value = history.map(m => ({
        role: (m.role || '').toLowerCase(),
        content: m.content
      }))
    }
  } catch (e) {
    console.error('加载对话历史失败:', e)
  }
}

async function removeConversation(convId) {
  if (isStreaming.value) return
  if (!confirm(`确定要删除会话 "${convId === 'default' ? '默认对话' : convId}" 吗？`)) return
  try {
    await deleteConversation(userId.value, convId)
    conversations.value = conversations.value.filter(c => c !== convId)
    if (currentConversationId.value === convId) {
      currentConversationId.value = null
      messages.value = []
    }
  } catch (e) {
    console.error('删除会话失败:', e)
  }
}

async function sendMessage(text) {
  if (!currentConversationId.value) {
    currentConversationId.value = generateConversationId()
    if (!conversations.value.includes(currentConversationId.value)) {
      conversations.value = [currentConversationId.value, ...conversations.value]
    }
  }

  messages.value.push({ role: 'user', content: text })
  const assistantMsg = { role: 'assistant', content: '' }
  messages.value.push(assistantMsg)

  isStreaming.value = true
  try {
    await streamChat({
      question: text,
      userId: userId.value,
      conversationId: currentConversationId.value,
      onChunk: (full) => {
        assistantMsg.content = full
      }
    })
  } catch (e) {
    console.error('发送消息失败:', e)
    assistantMsg.content = '抱歉，发生错误，请重试。'
  } finally {
    isStreaming.value = false
  }
}

onMounted(loadConversations)
</script>
