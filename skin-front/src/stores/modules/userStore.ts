import type { UserState, LoginResult } from '@/types/user'
import { defineStore } from 'pinia'

// 定义 Store
export const useAuthorizationStore = defineStore('Authorization', {
  state: (): UserState => ({
    token: '',
    profile: {
      username: '',
      nickname: '',
      mobile: '',
      avatar: '',
      jobNumber: ''
    }
  }),
  actions: {
    // 登录成功后调用
    setUserInfo({ token, profile }: LoginResult) {
      this.token = token
      this.profile = profile
    },
    // 退出登录
    logout() {
      this.token = ''
      this.profile = {
        username: '',
        nickname: '',
        mobile: '',
        avatar: '',
        jobNumber: ''
      }
    }
  },
  persist: {
    storage: {
      getItem(key){
        return uni.getStorageSync(key)
      },
      setItem(key,value){
        uni.setStorageSync(key,value)
      }
    }
  }
})
