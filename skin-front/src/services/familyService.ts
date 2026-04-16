import {type responseFamily} from '../types/family.d'
import {http} from '@/utils/http'

/**
 * 获取当前用户所有家人
 */
export const getFamily = () =>{
  return http<responseFamily[]>({
    method:'GET',
    url:'/family'
  })
};
/**
 * 删除家人
 * @param name 要删除的家人的姓名
 */
export const deleteFamily = (name:string)=>{
  return http<boolean>({
    method:'DELETE',
    url:`/family/${name}`
  })
}


/**
 * 编辑家人，根据家人的姓名修改家人
 * @param data 修改后的家人的信息
 */
export const updateFamily = (data:responseFamily)=>{
  return http({
    method:'PUT',
    url:"/family",
    data
  })
}


/**
 * 新增家人
 * @param data 新增的家人信息
 */
export const addFamily = (data:responseFamily)=>{
  return http({
    method:'POST',
    url:"/family",
    data
  })
}
