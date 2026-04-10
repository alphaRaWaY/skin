/**
 * 添加请求拦截器
 *  拦截 request 请求
 *  拦截 uploadFile 文件上传
 *
 *  TODO
 *    1. 非http开头的请求需要拼接地址
 *    2. 请求超时
 *    3. 添加小程序请求头标识
 *    4. 添加token请求头标识
*/

// import { resolve } from "dns";
import { useAuthorizationStore } from "@/stores/modules/userStore"
// import { reject } from "assert";

// const baseURL = 'https://pcapi-xiaotuxian-front-devtest.itheima.net'
const baseURL = 'http://localhost:8080/api'

// 添加拦截器
const httpInterceptor = {
  // 拦截前触发
  invoke(options:UniApp.RequestOptions){
    // 1.拼接地址
    if(!options.url.startsWith('http'))
    {
      // 如果不是以http开头
      options.url = baseURL+options.url
    }
    // 2.请求超时，默认超时时间 60s
    options.timeout = 600000;
    console.log(options);
    // 3. 添加请求头标识
    options.header = {
      ...options.header,
      'source-client': 'miniapp',
    }
    // 4.添加token请求头标识
    const authStore = useAuthorizationStore()
    const token = authStore?.token
    if(token)
    {
      options.header.Authorization = `Bearer ${token}`
    }
  }
}

uni.addInterceptor('request',httpInterceptor)
uni.addInterceptor('uploadFile',httpInterceptor)


/**
 * 请求函数z
 * @param UniApp.RequestOptions
 * @returns Promise
 */
// 封装返回数据
interface Data<T>{
  code:number
  msg:string
  result:T
}

// 添加泛型
export const http = <T>(options:UniApp.RequestOptions) => {
  return new Promise<Data<T> >((resolve,reject)=>{
      uni.request({
        ...options,
        // 请求成功
        success(res){
          // 未授权
          if(res.statusCode===401)
          {
            // 清除用户信息
            const authStore = useAuthorizationStore()
            authStore.profile = {
              username: '',
              nickname: '',
              mobile: '',
              avatar: ''
            }
            authStore.token = '';
            // 跳转登录
            uni.navigateTo({
              url:'/pages/login/login'
            })
            reject(res);
          }
          // 状态码为2开头
          else if(res.statusCode>=200&&res.statusCode<300)
          {
            // 提取核心数据
            resolve(res.data as Data<T>)
          }
          // 其他错误
          else
          {
            uni.showToast({
              icon:'error',
              title:(res.data as Data<T>).msg || '请求失败'
            })
            reject(res);
          }
        },
        fail(err) {
          // 网络失败
          uni.showToast({
            icon:'error',
            title:'网络错误'
          })
          reject(err)
        }
      })
  })
}


/**
 * 文件上传函数
 * @param options UniApp.UploadFileOption
 * @returns Promise<Data<T>>
 */
export const uploadFile = <T>(options: UniApp.UploadFileOption) => {
  return new Promise<Data<T>>((resolve, reject) => {
    uni.uploadFile({
      ...options,
      success(res) {
        // 后端返回的是 JSON 字符串，要手动解析
        let data: Data<T>
        try {
          data = JSON.parse(res.data)
        } catch (error) {
          uni.showToast({
            icon: 'error',
            title: '返回数据解析失败'
          })
          return reject(error)
        }

        // 模拟 request 的判断逻辑
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
