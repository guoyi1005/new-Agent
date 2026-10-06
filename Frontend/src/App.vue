<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AndroidBottomNav from './components/AndroidBottomNav.vue'
import { getStoredApiOrigin, isAndroidApp } from './config/androidApi'
import AndroidConnectionView from './views/AndroidConnectionView.vue'

const route = useRoute()
const needsConnection = isAndroidApp && !getStoredApiOrigin()
const showBottomNav = computed(() => isAndroidApp && !needsConnection && !['/login', '/mobile-connection'].includes(route.path))
</script>

<template>
  <AndroidConnectionView v-if="needsConnection" first-run />
  <template v-else>
    <RouterView />
    <AndroidBottomNav v-if="showBottomNav" />
  </template>
</template>

