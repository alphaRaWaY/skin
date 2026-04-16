// stores/reportStore.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {APIReport, Report} from '@/types/report'

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
      value: '', // 实际评分值（示例：中等严重程度）
      advice: '',
      introduction: ''
    })

  const setForm = (newForm: typeof form.value) => {
    form.value = { ...newForm }
    form.value.checkTime = new Date().toLocaleString()
  }

  const setImageUrl = (url: string) => {
    imageUrl.value = url
  }

  const setResult = (result:APIReport)=>{
    resultValue.value.advice=result.advice;
    resultValue.value.diseaseType=result.diseaseType
    resultValue.value.introduction=result.introduction
    resultValue.value.value=result.value
  }
  const reset = () => {
    imageUrl.value = '/static/images/sample.jpg'
    // resultValue.value = {
    //   diseaseType: '银屑病',
    //   value: '3,2,3,1', // 实际评分值（示例：中等严重程度）
    //   advice: `
    //   根据PASI评分，您的银屑病处于中度阶段（总分9分）：
    //   1. 局部治疗：建议使用含维生素D3类似物（如卡泊三醇）或弱效糖皮质激素药膏
    //   2. 日常护理：每日使用无刺激保湿霜，避免热水浴和过度搓洗
    //   3. 生活方式：减少压力，避免饮酒和吸烟
    //   4. 复诊建议：请于2周内预约皮肤科医生，评估是否需要光疗或系统治疗
    //   `,
    //     introduction: `
    //   银屑病（牛皮癣）是一种慢性自身免疫性皮肤病，特征为：
    //   - 典型表现：红色斑块覆盖银白色鳞屑，好发于肘部、膝盖和头皮
    //   - 常见诱因：压力、感染、外伤或某些药物
    //   - 疾病特点：反复发作，目前无法根治但可良好控制
    //   - 注意事项：避免抓挠，以防同形反应加重病情
    //   `
    // }
    resultValue.value = {
      diseaseType: '',
      value: '', // 实际评分值（示例：中等严重程度）
      advice: '',
      introduction: ''
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

  /** ✅ 导出完整报告对象，可用于下载、打印、发送后端等 */
  const getReport = (): APIReport => {
    return {
      id:0,
       ...form.value ,
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
