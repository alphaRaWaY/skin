import { http, uploadFile } from '@/utils/http'

export type AccountProfile = {
  id: number
  username: string
  nickname?: string
  mobile?: string
  avatar?: string
  jobNumber?: string
}

export const getMyAccount = () => {
  return http<AccountProfile>({
    method: 'GET',
    url: '/account/me',
  })
}

export const updateMyProfile = (data: {
  nickname?: string
  mobile?: string
  avatar?: string
  jobNumber?: string
}) => {
  return http({
    method: 'PUT',
    url: '/account/profile',
    data,
  })
}

export const changeMyPassword = (data: { oldPassword: string; newPassword: string }) => {
  return http({
    method: 'PUT',
    url: '/account/password',
    data,
  })
}

export const verifyMyOldPassword = (oldPassword: string) => {
  return http({
    method: 'POST',
    url: '/account/password/verify-old',
    data: { oldPassword },
  })
}

export const logoutAccount = () => {
  return http({
    method: 'POST',
    url: '/account/logout',
  })
}

export const uploadAvatarImage = (filePath: string) => {
  return uploadFile<string>({
    url: '/oss/upload',
    name: 'file',
    filePath,
  })
}

export const getOssImageUrl = (objectKey: string) => {
  return http<string>({
    method: 'POST',
    url: '/oss/image-url',
    data: objectKey,
  })
}
