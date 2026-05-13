export type Patient = {
  id: number
  doctorId: number
  patientName: string
  gender: string
  age: number
  phone?: string
  idCardMasked?: string
  notes?: string
  createdAt?: string
  updatedAt?: string
}
