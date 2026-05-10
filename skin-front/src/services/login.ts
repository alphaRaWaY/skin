import { http } from '@/utils/http'
import type { LoginResult } from '@/types/user.d'

type WxLoginParams = {
  code: string
  encryptedData: string
  iv: string
}

export type PwdLoginParams = {
  account: string
  password: string
}

export const postLoginWxMin = (data: WxLoginParams) => {
  return http<LoginResult>({
    method: 'POST',
    url: '/login/wxMin',
    data,
  })
}

export const postLoginPwd = (data: PwdLoginParams) => {
  return http<LoginResult>({
    method: 'POST',
    url: '/login/pwd',
    data,
  })
}
