import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../api/http'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/users' },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { public: true },
    },
    {
      path: '/users',
      name: 'users',
      component: () => import('../views/UserListView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }
  if (!getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
