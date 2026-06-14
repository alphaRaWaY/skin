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

  const imageUrl = ref('')
  const heatmapUrl = ref('')
  const heatmapBase64 = ref('')

  const resultValue = ref({
    diseaseType: '',
    diseaseIndex: undefined as number | undefined,
    confidence: undefined as number | undefined,
    modelVersion: '',
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
      heatmapUrl.value = ''
      heatmapBase64.value = ''
      resultValue.value = {
        diseaseType: '',
        diseaseIndex: undefined,
        confidence: undefined,
        modelVersion: '',
        value: '',
        advice: '',
        introduction: '',
        conceptScores: []
      }
      return
    }
    resultValue.value.advice = result.advice
    resultValue.value.diseaseType = result.diseaseType
    resultValue.value.diseaseIndex = result.diseaseIndex
    resultValue.value.confidence = result.confidence
    resultValue.value.modelVersion = result.modelVersion || ''
    resultValue.value.introduction = result.introduction
    resultValue.value.value = result.value
    resultValue.value.conceptScores = result.conceptScores || []
    heatmapBase64.value = result.heatmapBase64 || ''
    heatmapUrl.value = result.heatmapUrl || ''
  }

  const setHeatmapUrl = (url: string) => {
    heatmapUrl.value = url
  }

  const reset = () => {
    imageUrl.value = ''
    heatmapUrl.value = ''
    heatmapBase64.value = ''
    resultValue.value = {
      diseaseType: '',
      diseaseIndex: undefined,
      confidence: undefined,
      modelVersion: '',
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
      heatmapUrl: heatmapUrl.value,
      ...resultValue.value
    }
  }

  return {
    form,
    imageUrl,
    heatmapUrl,
    heatmapBase64,
    resultValue,
    setForm,
    setImageUrl,
    setHeatmapUrl,
    reset,
    getReport,
    setResult
  }
})
