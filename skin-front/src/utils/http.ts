import { useAuthorizationStore } from '@/stores/modules/userStore'
import { API_BASE_URL } from '@/config/env'

const httpInterceptor = {
  invoke(options: UniApp.RequestOptions) {
    if (!options.url.startsWith('http')) {
      options.url = `${API_BASE_URL}${options.url}`
    }

    options.timeout = 600000

    options.header = {
      ...options.header,
      'source-client': 'miniapp',
    }

    const authStore = useAuthorizationStore()
    const token = authStore?.token
    if (token) {
      options.header.Authorization = `Bearer ${token}`
    }
  },
}

uni.addInterceptor('request', httpInterceptor)
uni.addInterceptor('uploadFile', httpInterceptor)

interface Data<T> {
  code: number
  msg: string
  result: T
}

export const http = <T>(options: UniApp.RequestOptions) => {
  return new Promise<Data<T>>((resolve, reject) => {
    uni.request({
      ...options,
      success(res) {
        if (res.statusCode === 401) {
          const authStore = useAuthorizationStore()
          authStore.profile = {
            username: '',
            nickname: '',
            mobile: '',
            avatar: ''
          }
          authStore.token = ''
          uni.navigateTo({
            url: '/pages/login/login'
          })
          reject(res)
        } else if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data as Data<T>)
        } else {
          uni.showToast({
            icon: 'error',
            title: (res.data as Data<T>).msg || '请求错误'
          })
          reject(res)
        }
      },
      fail(err) {
        uni.showToast({
          icon: 'error',
          title: '网络错误'
        })
        reject(err)
      }
    })
  })
}

export const uploadFile = <T>(options: UniApp.UploadFileOption) => {
  return new Promise<Data<T>>((resolve, reject) => {
    uni.uploadFile({
      ...options,
      success(res) {
        let data: Data<T>
        try {
          data = JSON.parse(res.data)
        } catch (error) {
          uni.showToast({
            icon: 'error',
            title: '响应数据解析错误'
          })
          return reject(error)
        }

        if (res.statusCode === 401) {
          const authStore = useAuthorizationStore()
          authStore.profile = {
            username: '',
            nickname: '',
            mobile: '',
            avatar: ''
          }
          authStore.token = ''
          uni.navigateTo({ url: '/pages/login/login' })
          return reject(res)
        } else if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(data)
        } else {
          uni.showToast({
            icon: 'error',
            title: data.msg || '上传失败'
          })
          reject(res)
        }
      },
      fail(err) {
        uni.showToast({
          icon: 'error',
          title: '上传失败'
        })
        reject(err)
      }
    })
  })
}
