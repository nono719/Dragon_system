import { defineStore } from 'pinia'

const STORAGE_KEY = 'academic_blockchain_auth'

export const useUserStore = defineStore('user', {
  state: () => {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      try {
        const parsed = JSON.parse(raw)
        return { token: parsed.token, user: parsed.user, wallet: parsed.user?.walletAddress || '' }
      } catch (e) { /* ignore */ }
    }
    return { token: '', user: null, wallet: '' }
  },
  getters: {
    isLoggedIn: (s) => !!s.token,
    isAdmin: (s) => s.user?.role === 'ADMIN'
  },
  actions: {
    setAuth(token, user) {
      this.token = token
      this.user = user
      this.wallet = user?.walletAddress || ''
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, user }))
    },
    logout() {
      this.token = ''
      this.user = null
      this.wallet = ''
      localStorage.removeItem(STORAGE_KEY)
    },
    updateUser(user) {
      this.user = user
      this.wallet = user?.walletAddress || this.wallet
      if (this.token) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: this.token, user }))
      }
    }
  }
})
