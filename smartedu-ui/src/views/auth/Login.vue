<template>
  <div class="login-container">
    <!-- 左侧卡通角色区域 -->
    <div class="left-section">
      <div class="top-bar">
        <div class="logo">
          <div class="logo-icon">
            <span class="logo-text">计学网</span>
          </div>
        </div>
      </div>

      <div class="characters-section">
        <div class="characters-wrapper">
          <!-- 紫色高个角色 - 后层 -->
          <div
            ref="purpleRef"
            class="character purple-character"
            :style="purpleStyle"
          >
            <div class="head head-purple" :style="purpleHeadStyle">
              <img :src="purpleImage" alt="Purple" class="head-image" />
            </div>
          </div>

          <!-- 黑色高个角色 - 中层 -->
          <div
            ref="blackRef"
            class="character black-character"
            :style="blackStyle"
          >
            <div class="head head-black" :style="blackHeadStyle">
              <img :src="blackImage" alt="Black" class="head-image" />
            </div>
          </div>

          <!-- 橙色半圆角色 - 前左 -->
          <div
            ref="orangeRef"
            class="character orange-character"
            :style="orangeStyle"
          >
            <div class="head head-orange" :style="orangeHeadStyle">
              <img :src="orangeImage" alt="Orange" class="head-image" />
            </div>
          </div>

          <!-- 黄色高个角色 - 前右 -->
          <div
            ref="yellowRef"
            class="character yellow-character"
            :style="yellowStyle"
          >
            <div class="head head-yellow" :style="yellowHeadStyle">
              <img :src="yellowImage" alt="Yellow" class="head-image" />
            </div>
          </div>
        </div>
      </div>

      <div class="bottom-bar">
        <a href="#" class="footer-link">隐私政策</a>
        <a href="#" class="footer-link">服务条款</a>
        <a href="#" class="footer-link">联系我们</a>
      </div>

      <!-- 装饰元素 -->
      <div class="decoration decoration-1"></div>
      <div class="decoration decoration-2"></div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="right-section">
      <div class="login-wrapper">
        <!-- 移动端 Logo -->
        <div class="mobile-logo">
          <div class="logo-icon-small">
            <span>计学网</span>
          </div>
        </div>

        <div class="login-header">
          <h2>欢迎你登入计学网</h2>
          <p>请输入您的账号信息</p>
        </div>

        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <div class="input-group">
              <label for="username" class="input-label">用户名</label>
              <el-input
                id="username"
                v-model="loginForm.username"
                placeholder="请输入用户名"
                size="large"
                clearable
                @focus="handleTyping(true)"
                @blur="handleTyping(false)"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <div class="input-group">
              <label for="password" class="input-label">密码</label>
              <div class="password-input-wrapper">
                <el-input
                  id="password"
                  v-model="loginForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="请输入密码"
                  size="large"
                  @focus="handleTyping(true)"
                  @blur="handleTyping(false)"
                >
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                  <template #suffix>
                    <el-icon
                      class="password-toggle"
                      @click="showPassword = !showPassword"
                    >
                      <View v-if="!showPassword" />
                      <Hide v-else />
                    </el-icon>
                  </template>
                </el-input>
              </div>
            </div>
          </el-form-item>

          <div class="form-options">
            <div class="remember-me">
              <el-checkbox v-model="loginForm.rememberMe">
                <span class="checkbox-label">记住我 30 天</span>
              </el-checkbox>
            </div>
            <a href="#" class="forgot-password" @click.prevent="handleForgotPassword">
              忘记密码？
            </a>
          </div>

          <div v-if="error" class="error-message">
            {{ error }}
          </div>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="register-link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, View, Hide } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { login } from '@/api/auth'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)

// 鼠标位置
const mouseX = ref(0)
const mouseY = ref(0)

// 角色动画状态
const isTyping = ref(false)

// 角色引用
const purpleRef = ref<HTMLElement | null>(null)
const blackRef = ref<HTMLElement | null>(null)
const yellowRef = ref<HTMLElement | null>(null)
const orangeRef = ref<HTMLElement | null>(null)

// 角色图片 - 请替换为实际的图片文件（放在 public/images/characters 目录下）
const purpleImage = ref('/images/characters/purple.png')
const blackImage = ref('/images/characters/black.png')
const orangeImage = ref('/images/characters/orange.png')
const yellowImage = ref('/images/characters/yellow.png')

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

// 计算属性 - 密码状态
const isPasswordVisible = computed(() => loginForm.password.length > 0 && showPassword.value)
const isPasswordHidden = computed(() => isTyping.value || (loginForm.password.length > 0 && !showPassword.value))

// 角色位置数据
const purplePos = ref({ faceX: 0, faceY: 0, bodySkew: 0 })
const blackPos = ref({ faceX: 0, faceY: 0, bodySkew: 0 })
const yellowPos = ref({ faceX: 0, faceY: 0, bodySkew: 0 })
const orangePos = ref({ faceX: 0, faceY: 0, bodySkew: 0 })

// 计算角色样式
const purpleStyle = computed(() => ({
  transform: isPasswordVisible.value
    ? `skewX(0deg)`
    : isPasswordHidden.value
      ? `skewX(${(purplePos.value.bodySkew || 0) - 12}deg) translateX(40px)`
      : `skewX(${purplePos.value.bodySkew || 0}deg)`,
  height: isPasswordHidden.value ? '440px' : '400px'
}))

const purpleHeadStyle = computed(() => ({
  transform: `translate(${purplePos.value.faceX || 0}px, ${purplePos.value.faceY || 0}px)`
}))

const blackStyle = computed(() => ({
  transform: isPasswordVisible.value
    ? `skewX(0deg)`
    : isPasswordHidden.value
      ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5}deg)`
      : `skewX(${blackPos.value.bodySkew || 0}deg)`
}))

const blackHeadStyle = computed(() => ({
  transform: `translate(${blackPos.value.faceX || 0}px, ${blackPos.value.faceY || 0}px)`
}))

const orangeStyle = computed(() => ({
  transform: isPasswordVisible.value ? `skewX(0deg)` : `skewX(${orangePos.value.bodySkew || 0}deg)`
}))

const orangeHeadStyle = computed(() => ({
  transform: `translate(${orangePos.value.faceX || 0}px, ${orangePos.value.faceY || 0}px)`
}))

const yellowStyle = computed(() => ({
  transform: isPasswordVisible.value ? `skewX(0deg)` : `skewX(${yellowPos.value.bodySkew || 0}deg)`
}))

const yellowHeadStyle = computed(() => ({
  transform: `translate(${yellowPos.value.faceX || 0}px, ${yellowPos.value.faceY || 0}px)`
}))

// 计算角色位置
const calculatePosition = (element: HTMLElement | null) => {
  if (!element) return { faceX: 0, faceY: 0, bodySkew: 0 }

  const rect = element.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 3

  const deltaX = mouseX.value - centerX
  const deltaY = mouseY.value - centerY

  const faceX = Math.max(-15, Math.min(15, deltaX / 20))
  const faceY = Math.max(-10, Math.min(10, deltaY / 30))
  const bodySkew = Math.max(-6, Math.min(6, -deltaX / 120))

  return { faceX, faceY, bodySkew }
}

const calculateAllPositions = () => {
  purplePos.value = calculatePosition(purpleRef.value)
  blackPos.value = calculatePosition(blackRef.value)
  yellowPos.value = calculatePosition(yellowRef.value)
  orangePos.value = calculatePosition(orangeRef.value)
}

// 鼠标移动监听
const handleMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
  calculateAllPositions()
}

// 处理输入焦点
const handleTyping = (isFocusing: boolean) => {
  isTyping.value = isFocusing
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    error.value = ''

    try {
      const res = await login(loginForm)

      console.log('[登录] 响应数据:', res)

      if (res.code === 200 && res.data) {
        const loginData = res.data

        console.log('[登录] loginData:', loginData)
        console.log('[登录] userInfo:', loginData.userInfo)
        console.log('[登录] role:', loginData.userInfo?.role)

        // 同时设置 Token 和用户信息
        userStore.setToken(loginData.token)
        userStore.setUserInfo(loginData.userInfo)

        // 手动同步到 localStorage，确保格式与路由守卫期望的一致
        const storeData = {
          token: loginData.token,
          userInfo: loginData.userInfo
        }
        localStorage.setItem('smartedu_user', JSON.stringify(storeData))

        console.log('[登录] localStorage 已设置:', localStorage.getItem('smartedu_user'))

        ElMessage.success('登录成功')

        // 使用 replace 代替 push，避免浏览器 back 按钮回到登录页
        // 获取跳转路径
        const redirect = route.query.redirect as string
        let targetPath: string

        if (redirect) {
          targetPath = redirect
        } else {
          const role = loginData.userInfo?.role
          console.log('[登录] 用户角色:', role)
          if (role === 'STUDENT') {
            targetPath = '/student/dashboard'
          } else if (role === 'TEACHER' || role === 'ADMIN') {
            targetPath = '/teacher/homework'
          } else {
            targetPath = '/home'
          }
        }

        console.log('[登录] 即将跳转到:', targetPath)

        // 先验证 localStorage 中的值
        const storedValue = localStorage.getItem('smartedu_user')
        console.log('[登录] 验证 localStorage:', storedValue)

        // 执行跳转
        router.replace(targetPath)
      }
    } catch (e) {
      console.error('登录失败:', e)
      error.value = '用户名或密码错误，请重试'
    } finally {
      loading.value = false
    }
  })
}

const handleForgotPassword = () => {
  ElMessage.info('请联系管理员重置密码')
}

// 生命周期
onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleMouseMove)
})
</script>

<style scoped lang="scss">
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.login-container {
  width: 100%;
  height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

/* ============ 左侧区域 ============ */
.left-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;

  @media (max-width: 768px) {
    display: none;
  }

  .top-bar {
    position: relative;
    z-index: 10;

    .logo {
      .logo-icon {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        background: rgba(255, 255, 255, 0.15);
        padding: 10px 20px;
        border-radius: 10px;
        backdrop-filter: blur(10px);

        .logo-text {
          font-size: 20px;
          font-weight: 700;
          color: #fff;
        }
      }
    }
  }

  .characters-section {
    display: flex;
    align-items: flex-end;
    justify-content: center;
    height: 450px;
    position: relative;
    z-index: 5;

    .characters-wrapper {
      position: relative;
      width: 550px;
      height: 400px;
    }

    .character {
      position: absolute;
      bottom: 0;
      border-radius: 10px 10px 0 0;
      transition: all 0.7s ease-in-out;
      transform-origin: bottom center;

      .head {
        position: absolute;
        width: 80px;
        height: 80px;
        border-radius: 50%;
        overflow: hidden;
        transition: all 0.2s ease-out;

        .head-image {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }

      .mouth {
        position: absolute;
        width: 32px;
        height: 4px;
        background: #2D2D2D;
        border-radius: 2px;
        transition: all 0.2s ease-out;
      }
    }

    .purple-character {
      left: 70px;
      width: 180px;
      background: #6C3FF5;
      z-index: 1;

      .head-purple {
        width: 100px;
        height: 100px;
        top: -50px;
        left: 40px;
        background: #6C3FF5;
        border: 4px solid rgba(255, 255, 255, 0.2);
      }
    }

    .black-character {
      left: 240px;
      width: 120px;
      height: 310px;
      background: #2D2D2D;
      z-index: 2;

      .head-black {
        width: 90px;
        height: 90px;
        top: -45px;
        left: 15px;
        background: #2D2D2D;
        border: 4px solid rgba(255, 255, 255, 0.2);
      }
    }

    .orange-character {
      left: 0;
      width: 240px;
      height: 200px;
      background: #FF9B6B;
      border-radius: 120px 120px 0 0;
      z-index: 3;

      .head-orange {
        width: 110px;
        height: 110px;
        top: -55px;
        left: 65px;
        background: #FF9B6B;
        border: 4px solid rgba(255, 255, 255, 0.2);
      }
    }

    .yellow-character {
      left: 310px;
      width: 140px;
      height: 230px;
      background: #E8D754;
      border-radius: 70px 70px 0 0;
      z-index: 4;

      .head-yellow {
        width: 95px;
        height: 95px;
        top: -48px;
        left: 22px;
        background: #E8D754;
        border: 4px solid rgba(255, 255, 255, 0.2);
      }
    }
  }

  .bottom-bar {
    position: relative;
    z-index: 10;
    display: flex;
    gap: 32px;
    justify-content: center;

    .footer-link {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.6);
      text-decoration: none;
      transition: color 0.2s;

      &:hover {
        color: rgba(255, 255, 255, 0.9);
      }
    }
  }

  .decoration {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);

    &.decoration-1 {
      top: 25%;
      right: 25%;
      width: 256px;
      height: 256px;
      background: rgba(255, 255, 255, 0.1);
    }

    &.decoration-2 {
      bottom: 25%;
      left: 25%;
      width: 384px;
      height: 384px;
      background: rgba(255, 255, 255, 0.05);
    }
  }
}

/* ============ 右侧区域 ============ */
.right-section {
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;

  .login-wrapper {
    width: 100%;
    max-width: 420px;

    .mobile-logo {
      display: none;
      text-align: center;
      margin-bottom: 32px;

      @media (max-width: 768px) {
        display: block;
      }

      .logo-icon-small {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        background: rgba(102, 126, 234, 0.1);
        padding: 10px 20px;
        border-radius: 10px;

        span {
          font-size: 20px;
          font-weight: 700;
          color: #667eea;
        }
      }
    }

    .login-header {
      text-align: center;
      margin-bottom: 40px;

      h2 {
        font-size: 32px;
        font-weight: 700;
        color: #1a1a1a;
        margin-bottom: 8px;
      }

      p {
        font-size: 14px;
        color: #999;
      }
    }
  }
}

/* ============ 表单样式 ============ */
.login-form {
  .input-group {
    width: 100%;

    .input-label {
      display: block;
      font-size: 14px;
      font-weight: 500;
      color: #333;
      margin-bottom: 8px;
    }
  }

  .password-input-wrapper {
    :deep(.el-input__wrapper) {
      box-shadow: 0 0 0 1px #dcdfe6 inset;

      &:hover {
        box-shadow: 0 0 0 1px #c0c4cc inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 1px #667eea inset;
      }
    }

    .password-toggle {
      cursor: pointer;
      transition: color 0.2s;

      &:hover {
        color: #667eea;
      }
    }
  }

  :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 1px #dcdfe6 inset;
    border-radius: 8px;
    padding: 12px 14px;
    height: 48px;

    &:hover {
      box-shadow: 0 0 0 1px #c0c4cc inset;
    }

    &.is-focus {
      box-shadow: 0 0 0 1px #667eea inset;
    }
  }

  :deep(.el-input__inner) {
    font-size: 14px;
  }

  :deep(.el-input) {
    width: 100%;
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .remember-me {
    .el-checkbox {
      margin-right: 0;

      :deep(.el-checkbox__label) {
        font-size: 14px;
        color: #666;
      }
    }

    .checkbox-label {
      font-size: 14px;
      color: #666;
    }
  }

  .forgot-password {
    font-size: 14px;
    color: #667eea;
    text-decoration: none;
    font-weight: 500;

    &:hover {
      text-decoration: underline;
    }
  }
}

.error-message {
  padding: 12px 16px;
  background: rgba(245, 108, 108, 0.1);
  border: 1px solid rgba(245, 108, 108, 0.3);
  border-radius: 8px;
  color: #f56c6c;
  font-size: 14px;
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;

  &:hover {
    opacity: 0.9;
  }

  &:active {
    opacity: 0.8;
  }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #666;

  .register-link {
    color: #667eea;
    font-weight: 600;
    text-decoration: none;
    margin-left: 4px;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
