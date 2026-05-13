import { http } from '@/utils/http'

export type DashboardSummary = {
  todayDiagnosed: number
  pendingCases: number
  followupCases: number
  historyCases: number
}

export const getDashboardSummary = () => {
  return http<DashboardSummary>({
    method: 'GET',
    url: '/dashboard/summary',
  })
}
