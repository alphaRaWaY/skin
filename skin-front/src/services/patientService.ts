import type { Patient } from '@/types/patient'
import { http } from '@/utils/http'

export const getPatients = (keyword = '') => {
  return http<Patient[]>({
    method: 'GET',
    url: `/patients${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`,
  })
}

export const createPatient = (data: Partial<Patient>) => {
  return http<Patient>({
    method: 'POST',
    url: '/patients',
    data,
  })
}

export const deletePatient = (id: number) => {
  return http({
    method: 'DELETE',
    url: `/patients/${id}`,
  })
}
