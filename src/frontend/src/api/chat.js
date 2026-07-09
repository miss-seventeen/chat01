const BASE = '/chat'

export async function fetchConversations(userId) {
  const res = await fetch(`${BASE}/conversations?userId=${encodeURIComponent(userId)}`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export async function fetchHistory(userId, conversationId) {
  const res = await fetch(
    `${BASE}/history?userId=${encodeURIComponent(userId)}&conversationId=${encodeURIComponent(conversationId)}`
  )
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export async function deleteConversation(userId, conversationId) {
  const res = await fetch(
    `${BASE}/conversation?userId=${encodeURIComponent(userId)}&conversationId=${encodeURIComponent(conversationId)}`,
    { method: 'DELETE' }
  )
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.text()
}

export async function deleteAll(userId) {
  const res = await fetch(`${BASE}/all?userId=${encodeURIComponent(userId)}`, {
    method: 'DELETE'
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.text()
}

export async function streamChat({ question, userId, conversationId, onChunk }) {
  const url = `${BASE}/stream?question=${encodeURIComponent(question)}&userId=${encodeURIComponent(
    userId
  )}&conversationId=${encodeURIComponent(conversationId)}`

  const res = await fetch(url)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let full = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
    full += chunk
    onChunk && onChunk(full, chunk)
  }
  return full
}
