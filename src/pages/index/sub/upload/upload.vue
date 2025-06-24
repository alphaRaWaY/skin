<script setup lang="ts">
import { ref } from 'vue'
import { useReportStore } from '@/stores/modules/reportStore'
import { analysReport, getImage, uploadImageToServer } from '@/services/reportService'
import { report } from 'process'

const store = useReportStore()
const imageList = ref<string[]>([])
const defaultImage = '/static/images/default.png'

function chooseImage() {
  uni.chooseImage({
    count: 1,
    sourceType: ['album', 'camera'],
    success: function (res) {
      const filePath = Array.isArray(res.tempFilePaths) ? res.tempFilePaths[0] : res.tempFilePaths
      imageList.value = [filePath]
    }
  })
}


const testImg = ref('');
let raw = '';
async function testupload(){
  const localPath = imageList.value[0] || defaultImage
  // console.log(localPath);
  const res =await uploadImageToServer(localPath);
  raw =res.result;
  console.log(res);
}

async function handleConfirm() {
  const localPath = imageList.value[0] || defaultImage
  try {
    uni.showLoading({ title: '上传中...' })
    // 上传照片
    const res =await uploadImageToServer(localPath);
    store.setImageUrl(res.result);
    console.log('完整报告数据：', store.getReport())
    uni.hideLoading()


    uni.navigateTo({ url: '/pages/index/sub/result/result' })
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: '图片上传失败', icon: 'none' })
    console.error(error)
  }
}

// async function test(){
//   const localPath = imageList.value[0] || defaultImage
//   try {
//     uni.showLoading({ title: '上传中...' })
//     // 上传照片
//     const res =await uploadImageToServer(localPath);
//     store.setImageUrl(res.result);
//     console.log('完整报告数据：', store.getReport())
//     uni.hideLoading()
//     const ana = await analysReport(store.getReport());
//     console.log('分析结果为',ana);
//   } catch (error) {
//     uni.hideLoading()
//     uni.showToast({ title: '图片上传失败', icon: 'none' })
//     console.error(error)
//   }
// }

const test = async () => {
  const localPath = imageList.value[0] || defaultImage

  uni.showLoading({ title: '上传照片中...' })
  // 上传照片
  const res =await uploadImageToServer(localPath);
  store.setImageUrl(res.result);
  console.log('完整报告数据：', store.getReport())
  uni.hideLoading()
  // 确保日期格式正确
  const data = store.getReport();
  const postData = {
    ...data,
    checkTime: convertToISOTime(data.checkTime)
  };
  console.log(postData);
  try {
    uni.showLoading({ title: '正在分析结果，请耐心等待' })
    const response = await analysReport(postData);
    console.log('分析结果为',response);
    uni.hideLoading();
    uni.showToast({
      title:'检测成功！',
      icon:'success'
    })
    store.setResult(response.result);
    setTimeout(()=>{
      console.log('分析后的完整报告数据：', store.getReport())
      uni.navigateTo({ url: '/pages/index/sub/result/result' })
    },500);
  } catch (error) {
    console.log(error)
  }
}

const convertToISOTime = (timeString: string) => {
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(timeString)) {
    return timeString;
  }
  if (timeString.includes('上午') || timeString.includes('下午')) {
    return convertChineseTimeToISO(timeString);
  }
  try {
    const date = new Date(timeString);
    return date.toISOString().split('.')[0];
  } catch (e) {
    console.warn('无法解析日期，使用当前时间:', timeString);
    return new Date().toISOString().split('.')[0];
  }
}

const convertChineseTimeToISO = (chineseTime: string) => {
  try {
    const normalized = chineseTime
      .replace('上午', 'AM')
      .replace('下午', 'PM')
      .replace(/\s+/g, ' ');
    const date = new Date(normalized);
    if (isNaN(date.getTime())) {
      throw new Error('Invalid date format');
    }
    return date.toISOString().split('.')[0];
  } catch (error) {
    console.error('日期转换失败:', error);
    return new Date().toISOString();
  }
}

</script>

<template>
  <view class="container">
    <button @click="chooseImage">选择/拍摄照片</button>
    <view>
      <image
        :src="imageList.length > 0 ? imageList[0] : defaultImage"
        style="width: 100%; height: 500rpx;"
        mode="aspectFit"
        @click="chooseImage"
      />
    </view>
    <button type="primary" @click="test">上传并查看结果</button>
   <!-- <button type="primary" @click="test">测试照片上传</button> -->
    <!-- <button type="primary" @click="testshow">测试显示照片</button> -->

   <view v-if="testImg">
      <image
        :src="testImg"
        style="width: 100%; height: 500rpx;"
        mode="aspectFit"
      />
    </view>
  </view>
</template>

<style scoped>
.container {
  padding: 20rpx;
}
</style>
