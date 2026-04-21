import type { APIReport } from '@/types/report'
import {http, uploadFile} from '@/utils/http'

export const getReports = () =>{
  return http<APIReport[]>({
    method:'GET',
    url:'/reports',
  })
}

export const searchReports = (username:string) =>{
  return http<APIReport[]>({
    method:'GET',
    url:`/reports/search/${username}`,
  })
}


export const deleteReport = (id:number) =>{
  return http({
    method:'DELETE',
    url: `/reports/${id}`,
  })
}


export const getReportDetail = (id:number) =>{
  return http<APIReport>({
    method:'GET',
    url: `/reports/${id}`,
  })
}

export const postReport = (data:APIReport) =>{
  return http({
    method:'POST',
    url: '/reports',
    data
  })
}

export const analysReport = (data:APIReport) =>{
  return http<APIReport>({
    method:'POST',
    url: '/reports/analys',
    data
  })
}

export const uploadImageToServer = (file:string) =>{
  return uploadFile<string>({
      url: '/oss/upload',
      name: 'file',
      filePath:file
    })
}

export const getImage = (objectKey:string) =>{
  return http<string>({
      method:'POST',
      url: '/oss/image-url',
      data: objectKey
    })
}

export const deleteUploadedImage = (objectKey: string) => {
  return http({
    method: 'DELETE',
    url: `/oss/object?objectKey=${encodeURIComponent(objectKey)}`
  })
}
