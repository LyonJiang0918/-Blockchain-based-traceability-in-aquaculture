import React, { createContext, useContext, useState, useMemo } from 'react'

const AuthContext = createContext(null)

const STORAGE_KEY = 'TRACE_AUTH_USER'

function loadStoredUser() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      return JSON.parse(stored)
    }
  } catch {
    localStorage.removeItem(STORAGE_KEY)
  }
  return null
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadStoredUser)

  const value = useMemo(() => ({
    user,
    login: (userData) => {
      setUser(userData)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(userData))
    },
    logout: () => {
      setUser(null)
      localStorage.removeItem(STORAGE_KEY)
    },
  }), [user])

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

export const ROLE_MAP = {
  ADMIN: '管理员',
  FARM: '养殖场',
  PROCESS: '加工厂',
  SALES: '销售商',
}

/** 养殖场业务编号：与创建批次、后端会话强制逻辑一致 */
export function getEffectiveFarmId(user) {
  if (!user || user.role !== 'FARM') return null
  const fid = typeof user.farmId === 'string' ? user.farmId.trim() : ''
  if (fid) return fid
  return typeof user.username === 'string' ? user.username.trim() : null
}

/** 获取有效的业务ID（用于标识用户身份） */
export function getEffectiveBizId(user) {
  if (!user) return null
  const role = user.role
  if (role === 'FARM') return getEffectiveFarmId(user)
  // PROCESS/SALES 用户直接用 username 作为标识
  if (role === 'PROCESS' || role === 'SALES') {
    return typeof user.username === 'string' ? user.username.trim() : null
  }
  return null
}
