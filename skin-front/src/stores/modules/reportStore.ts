import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { APIReport } from '@/types/report'

type ConceptScore = {
  conceptIndex: number
  conceptNameEn?: string
  conceptNameCn?: string
  score: number
  rankNo: number
}

export const useReportStore = defineStore('report', () => {
  const form = ref({
    username: '',
    gender: '',
    age: 0,
    symptoms: '',
    duration: '',
    treatment: '',
    other: '',
    checkTime: ''
  })

  const imageUrl = ref('/static/images/sample.jpg')

  const resultValue = ref({
    diseaseType: '',
    value: '',
    advice: '',
    introduction: '',
    conceptScores: [] as ConceptScore[]
  })

  const setForm = (newForm: typeof form.value) => {
    form.value = { ...newForm }
    form.value.checkTime = new Date().toLocaleString()
  }

  const setImageUrl = (url: string) => {
    imageUrl.value = url
  }

  const setResult = (result: APIReport | null | undefined) => {
    if (!result) {
      resultValue.value = {
        diseaseType: '',
        value: '',
        advice: '',
        introduction: '',
        conceptScores: []
      }
      return
    }
    resultValue.value.advice = result.advice
    resultValue.value.diseaseType = result.diseaseType
    resultValue.value.introduction = result.introduction
    resultValue.value.value = result.value
    resultValue.value.conceptScores = result.conceptScores || []
  }

  const reset = () => {
    imageUrl.value = '/static/images/sample.jpg'
    resultValue.value = {
      diseaseType: '',
      value: '',
      advice: '',
      introduction: '',
      conceptScores: []
    }
    form.value = {
      username: '',
      gender: '',
      age: 0,
      symptoms: '',
      duration: '',
      treatment: '',
      other: '',
      checkTime: ''
    }
  }

  const getReport = (): APIReport => {
    return {
      id: 0,
      ...form.value,
      imageUrl: imageUrl.value,
      ...resultValue.value
    }
  }

  return {
    form,
    imageUrl,
    resultValue,
    setForm,
    setImageUrl,
    reset,
    getReport,
    setResult
  }
})
