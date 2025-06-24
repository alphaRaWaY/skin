import {http} from '@/utils/http'
import {type LoginResult, } from '@/types/user.d'
// 定义登录请求参数类型
type LoginParams = {
  code: string
  encryptedData: string
  iv: string
}
/**
 * 小程序登录
 * @param data 请求参数
 */
export const postLoginWxMin = (data: LoginParams) =>{
  return http<LoginResult>({
    method:'POST',
    url:'/login/wxMin',
    data,
  })
}


// export const postLoginWxMinSimpleAPI =(phoneNumber: string)=>{
//   return http<UserProfile>({
//     method:'POST',
//     url:'/login/wxMin/simple',
//     data:{
//       phoneNumber,
//     }
//   })
// }
