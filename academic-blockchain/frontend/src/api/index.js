import http from './http'

export const userApi = {
  login: (data) => http.post('/users/login', data),
  me: () => http.get('/users/me'),
  updateMe: (data) => http.put('/users/me', data),
  byWallet: (wallet) => http.get(`/users/by-wallet/${wallet}`)
}

export const achievementApi = {
  create: (data) => http.post('/achievements', data),
  update: (id, data) => http.put(`/achievements/${id}`, data),
  delete: (id) => http.delete(`/achievements/${id}`),
  detail: (id) => http.get(`/achievements/${id}`),
  mine: () => http.get('/achievements/mine'),
  list: (params) => http.get('/achievements', { params })
}

export const fileApi = {
  upload: (achievementId, file, onProgress) => {
    const fd = new FormData()
    fd.append('achievementId', achievementId)
    fd.append('file', file)
    return http.post('/files/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total))
      }
    })
  },
  meta: (fileId) => http.get(`/files/${fileId}/meta`),
  downloadUrl: (fileId) => `/api/files/${fileId}/download`,
  previewUrl: (fileId) => `/api/files/${fileId}/preview`
}

export const recordApi = {
  /** 拿到要做 keccak256 的 metadata 字符串（前端必须和后端用同一份输入算哈希） */
  metadataInput: (achievementId) => http.get(`/records/metadata-input/${achievementId}`),
  /** 前端钱包签名 registerRecord 完成后回传链上结果 */
  confirmRegister: (data) => http.post('/records/confirm-register', data),
  byAchievement: (achievementId) => http.get(`/records/by-achievement/${achievementId}`),
  byHash: (fileHash) => http.get(`/records/by-hash/${fileHash}`)
}

export const verifyApi = {
  verifyFile: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/verify', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  verifyByHash: (fileHash) => http.get('/verify/by-hash', { params: { fileHash } }),
  logs: (mine) => http.get('/verify/logs', { params: { mine } })
}

export const authorizationApi = {
  /** 前端钱包签名 grantAccess 完成后回传链上结果 */
  confirmGrant: (data) => http.post('/authorizations/confirm-grant', data),
  /** 前端钱包签名 revokeAccess 完成后回传链上结果 */
  confirmRevoke: (data) => http.post('/authorizations/confirm-revoke', data),
  granted: () => http.get('/authorizations/granted'),
  received: (onlyActive) => http.get('/authorizations/received', { params: { onlyActive } }),
  byAchievement: (achievementId) => http.get(`/authorizations/by-achievement/${achievementId}`),
  check: (achievementId, wallet) => http.get('/authorizations/check', { params: { achievementId, wallet } })
}

export const adminApi = {
  stats: () => http.get('/admin/stats'),
  users: () => http.get('/admin/users'),
  achievements: (keyword) => http.get('/admin/achievements', { params: { keyword } }),
  authorizations: () => http.get('/admin/authorizations'),
  verifyLogs: () => http.get('/admin/verify-logs')
}

export const healthApi = {
  ping: () => http.get('/health')
}

export const incentiveApi = {
  me: () => http.get('/incentive/me'),
  balance: (wallet) => http.get(`/incentive/balance/${wallet}`),
  info: () => http.get('/incentive/info')
}
