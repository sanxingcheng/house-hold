import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: () => {
        const auth = useAuthStore()
        return auth.isLoggedIn ? '/dashboard' : '/login'
      },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { guest: true },
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/Profile.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/family',
      name: 'Family',
      component: () => import('@/views/Family.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/family/assets',
      name: 'FamilyAssets',
      component: () => import('@/views/FamilyAssets.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/family/accounts',
      name: 'FamilyAccounts',
      component: () => import('@/views/FamilyAccounts.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/wealth/accounts',
      name: 'WealthAccounts',
      component: () => import('@/views/WealthAccounts.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/wealth/history',
      name: 'WealthHistory',
      component: () => import('@/views/WealthHistory.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.guest && auth.isLoggedIn) {
    next('/dashboard')
    return
  }
  next()
})

export default router
