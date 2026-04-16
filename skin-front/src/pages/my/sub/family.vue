<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { addFamily, deleteFamily, getFamily, updateFamily } from '../../../services/familyService'

// 响应式数据
const familyList = ref([])
const currentId = ref(null)
const isEdit = ref(false)
const dialogType = ref('info')
const dialogTitle = ref('添加家人')
const dialogContent = ref('')
const formData = ref({
  name: '',
  gender: '',
  age: '',
  relation: ''
})

const popup = ref(null)
const form = ref(null)

// 选项数据
const relations = [
  { value: '父亲', text: '父亲' },
  { value: '母亲', text: '母亲' },
  { value: '配偶', text: '配偶' },
  { value: '子女', text: '子女' },
  { value: '其他', text: '其他' }
]

const genders = [
  { value: '男', text: '男' },
  { value: '女', text: '女' }
]

// 表单验证规则
const rules = {
  name: {
    rules: [{
      required: true,
      errorMessage: '请输入姓名'
    }]
  },
  gender: {
    rules: [{
      required: true,
      errorMessage: '请选择性别'
    }]
  },
  age: {
    rules: [{
      required: true,
      errorMessage: '请输入年龄'
    }, {
      pattern: /^[1-9]\d*$/,
      errorMessage: '年龄必须为正整数'
    }]
  },
  relation: {
    rules: [{
      required: true,
      errorMessage: '请选择关系'
    }]
  }
}

// 生命周期钩子
onShow(() => {
  loadFamilyList()
})

// 方法
const loadFamilyList = async () => {
  try {
    uni.showLoading({ title: '加载中...' })
    const res = await getFamily();

    if (res.code === 0) {
      familyList.value = res.result || []
    } else {
      throw new Error(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载家人列表失败:', error)
    uni.showToast({ title: error.message, icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

const handleAdd = () => {
  isEdit.value = false
  currentId.value = null
  dialogTitle.value = '添加家人'
  resetForm()
  popup.value.open()
}

const handleEdit = (item) => {
  isEdit.value = true
  currentId.value = item.id
  dialogTitle.value = '编辑家人信息'
  formData.value = {
    name: item.name,
    gender: item.gender,
    age: item.age,
    relation: item.relation
  }
  popup.value.open()
}

const handleDelete = (name) => {
  uni.showModal({
    title: '提示',
    content: '确定要删除该家人信息吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          uni.showLoading({ title: '删除中...' })
          const result = await deleteFamily(name)

          if (result.code === 0) {
            uni.showToast({ title: '删除成功' })
            loadFamilyList()
          } else {
            throw new Error(result.msg || '删除失败')
          }
        } catch (error) {
          console.error('删除失败:', error)
          uni.showToast({ title: error.message, icon: 'none' })
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

const resetForm = () => {
  formData.value = {
    name: '',
    gender: '',
    age: '',
    relation: ''
  }
  form.value && form.value.clearValidate()
}

const closeDialog = () => {
  popup.value.close()
}

const confirmDialog = async (done) => {
  try {
    // 表单验证
    await form.value.validate()

    const requestParams = {
      name: formData.value.name,
      gender: formData.value.gender,
      relation: formData.value.relation,
      age: formData.value.age,
      id: isEdit.value ? formData.value.id : 0, // 编辑时用现有ID，新增时用0
      userid: 3
    }

    let res
    if (isEdit.value) {
      // 编辑应该调用 update 接口
      res = await updateFamily(requestParams)
      dialogContent.value = res.msg || '修改成功'
    } else {
      // 新增调用 add 接口
      res = await addFamily(requestParams)
      dialogContent.value = res.msg || '添加成功'
    }

    if (res.code !== 0) {
      throw new Error(res.msg || '操作失败')
    }

    // 刷新列表
    await loadFamilyList()

    // 如果有done回调则执行
    done?.()

    // 显示成功提示
    uni.showToast({ title: dialogContent.value, icon: 'success' })
  } catch (error) {
    console.error('操作失败:', error)
    uni.showToast({ title: error.message, icon: 'none' })
  }
}
</script>

<template>
  <view class="container">
    <!-- 标题 -->
    <view class="header">
      <text class="title">家人信息管理</text>
    </view>

    <!-- 添加按钮 -->
    <view class="add-btn">
      <button type="primary" @click="handleAdd">添加家人</button>
    </view>

    <!-- 家人列表 -->
    <view class="list-container">
      <uni-list>
        <uni-list-item
          v-for="item in familyList"
          :key="item.id"
          :title="item.name"
          :note="`关系: ${item.relation} | 性别: ${item.gender} | 年龄: ${item.age}`"
          clickable
          @click="handleEdit(item)"
        >
          <template v-slot:footer>
            <view class="action-btns">
              <uni-icons
                type="compose"
                size="20"
                color="#007AFF"
                @click.stop="handleEdit(item)"
              />

              <uni-icons
                type="trash"
                size="20"
                color="#DD524D"
                @click.stop="handleDelete(item.name)"
                style="margin-left: 15px;"
              />
            </view>
          </template>
        </uni-list-item>

        <uni-list-item v-if="familyList.length === 0" title="暂无家人信息" />
      </uni-list>
    </view>

    <!-- 添加/编辑弹窗 -->
    <uni-popup ref="popup" type="dialog">
      <uni-popup-dialog
        :type="dialogType"
        :title="dialogTitle"
        :content="dialogContent"
        :duration="2000"
        :before-close="true"
        @close="closeDialog"
        @confirm="confirmDialog"
      >
        <view class="form-container">
          <uni-forms ref="form" :model="formData" :rules="rules">
            <uni-forms-item label="姓名" name="name">
              <uni-easyinput v-model="formData.name" placeholder="请输入姓名" />
            </uni-forms-item>
            <uni-forms-item label="性别" name="gender">
              <uni-data-select
                v-model="formData.gender"
                :localdata="genders"
                placeholder="请选择性别"
              />
            </uni-forms-item>
            <uni-forms-item label="年龄" name="age">
              <uni-easyinput type="number" v-model="formData.age" placeholder="请输入年龄" />
            </uni-forms-item>
            <uni-forms-item label="关系" name="relation">
              <uni-data-select
                v-model="formData.relation"
                :localdata="relations"
                placeholder="请选择关系"
              />
            </uni-forms-item>
          </uni-forms>
        </view>
      </uni-popup-dialog>
    </uni-popup>
  </view>
</template>

<style scoped>
.container {
  padding: 20rpx;
}

.header {
  padding: 30rpx 0;
  text-align: center;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
}

.add-btn {
  margin: 20rpx 0;
}

.list-container {
  margin-top: 20rpx;
}

.action-btns {
  display: flex;
  align-items: center;
}

.form-container {
  padding: 20rpx;
}

/* 适配不同平台 */
@media (min-width: 768px) {
  .container {
    max-width: 750px;
    margin: 0 auto;
  }
}
</style>
