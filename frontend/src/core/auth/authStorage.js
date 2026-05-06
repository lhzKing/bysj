const TOKEN_STORAGE_KEY = 'token'
const USER_STORAGE_KEY = 'user'

export function readToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY) || ''
}

export function writeToken(token) {
  if (!token) {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    return
  }

  localStorage.setItem(TOKEN_STORAGE_KEY, token)
}

export function readCachedUser() {
  const rawUser = localStorage.getItem(USER_STORAGE_KEY)
  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser)
  } catch {
    localStorage.removeItem(USER_STORAGE_KEY)
    return null
  }
}

export function writeCachedUser(user) {
  if (!user) {
    localStorage.removeItem(USER_STORAGE_KEY)
    return
  }

  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
}

export function writeAuthSession({ token, user } = {}) {
  writeToken(token)
  writeCachedUser(user)
}

export function clearAuthSession() {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  localStorage.removeItem(USER_STORAGE_KEY)
}

