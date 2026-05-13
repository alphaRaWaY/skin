const WHITE_LIST = new Set<string>([
  '/pages/login/login',
  '/pages/login/policy',
])

function normalizePath(url?: string): string {
  if (!url) return ''
  const clean = url.split('?')[0]
  return clean.startsWith('/') ? clean : `/${clean}`
}

function readTokenFromStorage(): string {
  try {
    const raw = uni.getStorageSync('Authorization')
    if (!raw) return ''
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return parsed?.token || ''
  } catch {
    return ''
  }
}

function hasLogin(): boolean {
  return !!readTokenFromStorage()
}

function isProtectedPath(path: string): boolean {
  if (!path) return false
  return !WHITE_LIST.has(path)
}

function redirectToLogin() {
  const pages = getCurrentPages()
  const current = pages.length ? `/${pages[pages.length - 1].route}` : ''
  if (current === '/pages/login/login') return

  uni.reLaunch({ url: '/pages/login/login' })
}

function shouldBlock(url?: string): boolean {
  const path = normalizePath(url)
  if (!path) return false
  if (!isProtectedPath(path)) return false
  if (hasLogin()) return false
  redirectToLogin()
  return true
}

function guardInvoke(options: { url?: string }) {
  if (shouldBlock(options.url)) {
    return false
  }
  return options
}

export function initAuthGuard() {
  uni.addInterceptor('navigateTo', { invoke: guardInvoke })
  uni.addInterceptor('redirectTo', { invoke: guardInvoke })
  uni.addInterceptor('reLaunch', { invoke: guardInvoke })
  uni.addInterceptor('switchTab', { invoke: guardInvoke })
}

export function enforceCurrentPageAuth() {
  const pages = getCurrentPages()
  if (!pages.length) return
  const current = pages[pages.length - 1]
  const path = `/${current.route}`
  if (!isProtectedPath(path)) return
  if (hasLogin()) return
  redirectToLogin()
}
