<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { initAuthGuard, enforceCurrentPageAuth } from '@/utils/authGuard'

onLaunch(() => {
  initAuthGuard()
  enforceCurrentPageAuth()
})

onShow(() => {
  enforceCurrentPageAuth()
})

onHide(() => {})
</script>

<style></style>
