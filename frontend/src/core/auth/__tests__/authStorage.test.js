import { beforeEach, describe, expect, it } from 'vitest'
import {
  clearAuthSession,
  readCachedUser,
  readToken,
  writeAuthSession,
  writeCachedUser,
  writeToken
} from '@/core/auth/authStorage'

describe('authStorage', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('centralizes token and user cache writes', () => {
    writeAuthSession({
      token: 'jwt-token',
      user: {
        id: 1,
        username: 'alice',
        permissions: ['trace:view']
      }
    })

    expect(readToken()).toBe('jwt-token')
    expect(readCachedUser()).toEqual({
      id: 1,
      username: 'alice',
      permissions: ['trace:view']
    })
  })

  it('removes empty token and user values instead of storing placeholders', () => {
    writeToken('jwt-token')
    writeCachedUser({ username: 'alice' })

    writeToken('')
    writeCachedUser(null)

    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('clears the complete auth session on logout or unauthorized responses', () => {
    writeAuthSession({ token: 'jwt-token', user: { username: 'alice' } })

    clearAuthSession()

    expect(readToken()).toBe('')
    expect(readCachedUser()).toBeNull()
  })

  it('drops corrupted cached user data and keeps the app bootable', () => {
    localStorage.setItem('user', '{not-json')

    expect(readCachedUser()).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })
})

