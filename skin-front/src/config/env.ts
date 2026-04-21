const trimEndSlash = (value: string) => value.replace(/\/+$/, '')

const rawApiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const API_BASE_URL = trimEndSlash(rawApiBaseUrl)
