import { http } from '@/utils/http'

export interface MedicalCaseItem {
  id: number
  doctorId?: number
  caseNo: string
  status: string
  patientId: number
  patientName: string
  diagnosedType: string
  chiefComplaint: string
  presentHistory?: string
  treatmentHistory?: string
  duration?: string
  extraNotes?: string
  aiAdvice?: string
  aiIntroduction?: string
  checkTime: string
  createdAt: string
  updatedAt?: string
}

export const getMedicalCases = (params?: {
  status?: string
  patientId?: number
  keyword?: string
}) => {
  const query: string[] = []
  if (params?.status) query.push(`status=${encodeURIComponent(params.status)}`)
  if (typeof params?.patientId === 'number') query.push(`patientId=${params.patientId}`)
  if (params?.keyword) query.push(`keyword=${encodeURIComponent(params.keyword)}`)
  const q = query.length ? `?${query.join('&')}` : ''

  return http<MedicalCaseItem[]>({
    method: 'GET',
    url: `/cases${q}`,
  })
}

export const getMedicalCaseDetail = (id: number) => {
  return http<MedicalCaseItem>({
    method: 'GET',
    url: `/cases/${id}`,
  })
}

export const deleteMedicalCase = (id: number) => {
  return http({
    method: 'DELETE',
    url: `/cases/${id}`,
  })
}
