export type Report = {
  id: number
  form: {
    username: string
    gender: string
    age: number
    symptoms: string
    duration: string
    treatment: string
    other: string
    checkTime: string
  }
  imageUrl: string
  resultValue: {
    diseaseType: string
    value: string
    advice: string
    introduction: string
  }
}

// 如果使用扁平结构
export type APIReport = {
  id: number
  username: string
  gender: string
  age: number
  symptoms: string
  duration: string
  treatment: string
  other: string
  checkTime: string
  imageUrl: string
  diseaseType: string
  value: string
  advice: string
  introduction: string
  conceptScores?: {
    conceptIndex: number
    conceptNameEn?: string
    conceptNameCn?: string
    score: number
    rankNo: number
  }[]
}
