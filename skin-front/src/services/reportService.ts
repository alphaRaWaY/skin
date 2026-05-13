import type { APIReport } from '@/types/report'
import { http, uploadFile } from '@/utils/http'

export const getReports = () => {
  return http<APIReport[]>({
    method: 'GET',
    url: '/cases',
  })
}

export const searchReports = (username: string) => {
  return http<APIReport[]>({
    method: 'GET',
    url: `/cases/search/${encodeURIComponent(username)}`,
  })
}

export const deleteReport = (id: number) => {
  return http({
    method: 'DELETE',
    url: `/cases/${id}`,
  })
}

export const getReportDetail = (id: number) => {
  return http<APIReport>({
    method: 'GET',
    url: `/cases/${id}`,
  })
}

export const postReport = (data: APIReport) => {
  return http({
    method: 'POST',
    url: '/cases/save',
    data,
  })
}

export const analysReport = (data: APIReport) => {
  return http<APIReport>({
    method: 'POST',
    url: '/cases/analyze',
    data,
  })
}

export const analysReportWithImage = (
  filePath: string,
  data: APIReport,
  onProgress?: (progress: number) => void,
) => {
  return uploadFile<APIReport>({
    url: '/cases/analyze-upload',
    name: 'image',
    filePath,
    formData: {
      reportJson: JSON.stringify(data),
    },
    onProgress: (progress) => {
      onProgress?.(progress)
    },
  })
}

export const uploadImageToServer = (file: string) => {
  return uploadFile<string>({
    url: '/oss/upload',
    name: 'file',
    filePath: file,
  })
}

export const getImage = (objectKey: string) => {
  return http<string>({
    method: 'POST',
    url: '/oss/image-url',
    data: objectKey,
  })
}

export const deleteUploadedImage = (objectKey: string) => {
  return http({
    method: 'DELETE',
    url: `/oss/object?objectKey=${encodeURIComponent(objectKey)}`,
  })
}
