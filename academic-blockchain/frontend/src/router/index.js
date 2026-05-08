import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

const Layout = () => import('../components/Layout.vue')
const Home = () => import('../views/Home.vue')
const Login = () => import('../views/Login.vue')
const RegisterAchievement = () => import('../views/RegisterAchievement.vue')
const Verify = () => import('../views/Verify.vue')
const GrantAccess = () => import('../views/GrantAccess.vue')
const MyAchievements = () => import('../views/MyAchievements.vue')
const AchievementDetail = () => import('../views/AchievementDetail.vue')
const AuthorizedToMe = () => import('../views/AuthorizedToMe.vue')
const Profile = () => import('../views/Profile.vue')
const Admin = () => import('../views/Admin.vue')

const routes = [
  {
    path: '/',
    component: Layout,
    children: [
      { path: '',                  name: 'home',     component: Home,                meta: { title: '首页' } },
      { path: 'login',             name: 'login',    component: Login,               meta: { title: '钱包登录', public: true } },
      { path: 'verify',            name: 'verify',   component: Verify,              meta: { title: '成果核验', public: true } },
      { path: 'achievements/new',  name: 'create',   component: RegisterAchievement, meta: { title: '成果存证', auth: true } },
      { path: 'achievements/mine', name: 'mine',     component: MyAchievements,      meta: { title: '我的成果', auth: true } },
      { path: 'achievements/:id',  name: 'detail',   component: AchievementDetail,   meta: { title: '成果详情' } },
      { path: 'grant',             name: 'grant',    component: GrantAccess,         meta: { title: '授权共享', auth: true } },
      { path: 'authorized',        name: 'received', component: AuthorizedToMe,      meta: { title: '我被授权的成果', auth: true } },
      { path: 'profile',           name: 'profile',  component: Profile,             meta: { title: '个人中心', auth: true } },
      { path: 'admin',             name: 'admin',    component: Admin,               meta: { title: '后台管理', admin: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.admin && !userStore.isAdmin) {
    ElMessage.warning('需要管理员权限')
    return userStore.isLoggedIn ? '/' : '/login'
  }
  if (to.meta.auth && !userStore.isLoggedIn) {
    ElMessage.info('请先连接钱包登录')
    return '/login'
  }
  if (to.meta.title) document.title = `${to.meta.title} · 学术成果区块链平台`
  return true
})

export default router
