<script setup>
import { onMounted } from 'vue'
import AppTabBar from '../../components/AppTabBar.vue'
import { getToken, getUserInfo } from '../../utils/auth'

onMounted(() => {
  const token = getToken()
  if (token) {
    localStorage.setItem('session_token', token)
  }

  const user = getUserInfo()
  if (user?.id) {
    localStorage.setItem('user_id', String(user.id))
  }
  if (user?.nickname || user?.realName || user?.username) {
    localStorage.setItem(
      'nickname',
      String(user.nickname || user.realName || user.username),
    )
  }

  const role = String(user?.role || user?.userRole || user?.userType || '').toLowerCase()
  const isManager =
    role === 'admin' ||
    role === 'manager' ||
    user?.isManager === true ||
    user?.is_manager === 1 ||
    user?.is_manager === '1'
  localStorage.setItem('is_manager', isManager ? '1' : '0')
})
</script>

<template>
  <div class="interview-shell">
    <AppTabBar />
    <div class="interview-shell__content">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.interview-shell {
  min-height: 100vh;
  background: #f3efe6;
}

.interview-shell__content {
  padding-top: 60px;
  min-height: 100vh;
  background: #f3efe6;
}

</style>
