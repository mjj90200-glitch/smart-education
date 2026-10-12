<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <div class="profile-content">
            <!-- 头像上传 -->
            <div class="avatar-section">
              <label>头像</label>
              <div class="avatar-upload" @click="triggerFileInput">
                <el-avatar :size="120" :src="formData.avatar || defaultAvatar" />
                <div class="avatar-overlay">
                  <el-icon><Camera /></el-icon>
                  <span>更换头像</span>
                </div>
                <input
                  ref="fileInputRef"
                  type="file"
                  accept="image/*"
                  @change="handleAvatarChange"
                  class="avatar-input"
                />
              </div>
              <el-alert
                v-if="avatarError"
                type="error"
                :closable="false"
                style="margin-top: 10px; max-width: 300px;"
              >
                {{ avatarError }}
              </el-alert>
              <p class="avatar-tip">支持 JPG、PNG 格式，最大 10MB，建议选择清晰的照片</p>
            </div>

            <el-divider />

            <!-- 用户信息表单 -->
            <el-form :model="formData" label-width="100px" class="info-form">
              <el-form-item label="学号/工号">
                <el-input v-model="formData.username" disabled>
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
                <span class="form-tip">学号/工号由管理员分配，不可修改</span>
              </el-form-item>

              <el-form-item label="姓名">
                <el-input v-model="formData.realName" disabled>
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
                <span class="form-tip">姓名与学籍信息绑定，不可修改</span>
              </el-form-item>

              <el-form-item label="角色">
                <el-tag :type="getRoleType(formData.role)" size="large">
                  {{ getRoleText(formData.role) }}
                </el-tag>
              </el-form-item>

              <template v-if="isStudent">
                <el-form-item label="年级">
                  <el-input v-model="formData.grade" disabled />
                </el-form-item>
                <el-form-item label="专业">
                  <el-input v-model="formData.major" disabled />
                </el-form-item>
                <el-form-item label="班级">
                  <el-input v-model="formData.className" disabled />
                </el-form-item>
              </template>

              <template v-if="isTeacher">
                <el-form-item label="院系">
                  <el-input v-model="formData.department" disabled />
                </el-form-item>
                <el-form-item label="职称">
                  <el-input v-model="formData.title" disabled />
                </el-form-item>
              </template>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <div class="password-content">
            <el-alert
              title="为了您的账号安全，请定期修改密码"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 20px;"
            />

            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="100px"
              class="password-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入原密码"
                  show-password
                  clearable
                />
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码（6-20 位）"
                  show-password
                  clearable
                />
                <span class="form-tip">密码长度 6-20 位，建议包含字母和数字</span>
              </el-form-item>

              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                  clearable
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">
                  确认修改
                </el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 图片裁剪对话框 -->
    <el-dialog
      v-model="cropDialogVisible"
      title="裁剪头像"
      width="500px"
      :close-on-click-modal="false"
      @opened="handleCropDialogOpened"
      @closed="handleCropDialogClosed"
    >
      <div class="crop-wrapper">
        <div class="crop-box" ref="cropBoxRef">
          <img ref="cropImageRef" :src="cropImageSrc" alt="待裁剪图片" class="crop-image" />
        </div>
        <div class="preview-box">
          <p>预览：</p>
          <div class="preview-avatar" :style="previewStyle">
            <el-avatar :size="100" :src="previewUrl" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="cropDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCropConfirm" :loading="cropping">
          确定裁剪
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Camera, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { getCurrentUserInfo, changePassword, uploadAvatar } from '@/api/user'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

const userStore = useUserStore()

// 激活的标签页
const activeTab = ref('basic')

// 默认头像
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjY2NjIiBzdHJva2Utd2lkdGg9IjEiPjxjaXJjbGUgY3g9IjEyIiBjeT0iOCIgcj0iNCIvPjxwYXRoIGQ9Ik02IDIwdi0yYTYgNiAwIDAgMSAxMiAwdjIiLz48L3N2Zz4='

// 文件输入引用
const fileInputRef = ref<HTMLInputElement>()

// 表单数据
const formData = ref({
  id: 0,
  username: '',
  realName: '',
  email: '',
  phone: '',
  avatar: '',
  role: '',
  grade: '',
  major: '',
  className: '',
  department: '',
  title: ''
})

// 加载状态
const updateLoading = ref(false)
const avatarError = ref('')

// 密码表单
const passwordFormRef = ref()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordLoading = ref(false)

// 图片裁剪相关
const cropDialogVisible = ref(false)
const cropImageSrc = ref('')
const cropping = ref(false)
const selectedFile = ref<File | null>(null)
const cropperInstance = ref<Cropper | null>(null)
const cropBoxRef = ref<HTMLElement>()
const cropImageRef = ref<HTMLImageElement>()

// 实时预览
const previewUrl = ref('')
const previewStyle = ref({})

// 文件大小限制：10MB
const MAX_FILE_SIZE = 10 * 1024 * 1024

// 计算属性
const isStudent = computed(() => formData.value.role === 'STUDENT')
const isTeacher = computed(() => formData.value.role === 'TEACHER')

// 获取角色类型
const getRoleType = (role: string) => {
  const map: Record<string, any> = {
    STUDENT: 'primary',
    TEACHER: 'success',
    ADMIN: 'danger'
  }
  return map[role] || 'info'
}

// 获取角色文本
const getRoleText = (role: string) => {
  const map: Record<string, string> = {
    STUDENT: '学生',
    TEACHER: '教师',
    ADMIN: '管理员'
  }
  return map[role] || role
}

// 触发文件选择
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 处理头像选择
const handleAvatarChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files || files.length === 0) return

  const file = files[0]
  avatarError.value = ''

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    avatarError.value = '请选择图片文件（支持 JPG、PNG 等格式）'
    target.value = ''
    return
  }

  // 验证文件大小
  if (file.size > MAX_FILE_SIZE) {
    avatarError.value = '图片体积过大，请选择小于 10MB 的图片'
    target.value = ''
    return
  }

  // 保存文件并打开裁剪对话框
  selectedFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    cropImageSrc.value = e.target?.result as string
    // 先设置 cropImageSrc，然后在下一个 tick 打开对话框
    nextTick(() => {
      cropDialogVisible.value = true
    })
  }
  reader.readAsDataURL(file)

  // 清空 input 以允许重复选择同一文件
  target.value = ''
}

// 裁剪对话框打开后初始化 Cropper
const handleCropDialogOpened = async () => {
  await nextTick()

  // 销毁旧的 cropper 实例
  if (cropperInstance.value) {
    cropperInstance.value.destroy()
    cropperInstance.value = null
  }

  if (cropImageRef.value) {
    // 初始化 Cropper
    cropperInstance.value = new Cropper(cropImageRef.value, {
      aspectRatio: 1,
      viewMode: 1,
      dragMode: 'move',
      autoCropArea: 0.8,
      responsive: true,
      background: false,
      // 启用实时预览
      preview: '.preview-avatar'
    })
  }
}

// 裁剪对话框关闭后的清理
const handleCropDialogClosed = () => {
  // 销毁 cropper 实例
  if (cropperInstance.value) {
    cropperInstance.value.destroy()
    cropperInstance.value = null
  }
  cropImageSrc.value = ''
  selectedFile.value = null
}

// 处理裁剪确认
const handleCropConfirm = () => {
  if (!cropperInstance.value || cropping.value) {
    ElMessage.error('裁剪工具未初始化或正在处理')
    return
  }

  cropping.value = true

  // 获取裁剪后的 canvas
  const canvas = cropperInstance.value.getCroppedCanvas({
    width: 250,
    height: 250,
    imageSmoothingEnabled: true,
    imageSmoothingQuality: 'high',
  })

  if (!canvas) {
    ElMessage.error('裁剪失败，请重试')
    cropping.value = false
    return
  }

  // 将 canvas 转为 Blob
  canvas.toBlob((blob) => {
    if (!blob) {
      ElMessage.error('裁剪失败，请重试')
      cropping.value = false
      return
    }

    const file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
    uploadAvatarFile(file).finally(() => {
      cropDialogVisible.value = false
    })
  }, 'image/jpeg', 0.9)
}

// 上传头像文件
const uploadAvatarFile = async (file: File) => {
  if (updateLoading.value) {
    console.warn('上传正在进行中，请勿重复操作')
    return
  }

  updateLoading.value = true
  try {
    const formDataUpload = new FormData()
    formDataUpload.append('file', file)

    const res = await uploadAvatar(formDataUpload)

    if (res.code === 200 && res.data) {
      formData.value.avatar = res.data
      ElMessage.success('头像更新成功')

      // 更新 store 中的头像
      userStore.setUserInfo({
        ...userStore.userInfo!,
        avatar: res.data
      })
    } else {
      avatarError.value = res.message || '上传失败'
    }
  } catch (error: any) {
    console.error('上传头像失败:', error)
    avatarError.value = error.message || '上传失败，请稍后重试'
  } finally {
    updateLoading.value = false
    cropping.value = false
  }
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const res = await getCurrentUserInfo()
    if (res.code === 200 && res.data) {
      formData.value = {
        ...formData.value,
        ...res.data
      }
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
    ElMessage.error('加载用户信息失败')
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    passwordLoading.value = true
    try {
      const res = await changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })

      if (res.code === 200) {
        ElMessage.success('密码修改成功，请重新登录')

        // 清空表单
        resetPasswordForm()

        // 退出登录
        setTimeout(() => {
          userStore.logout()
          window.location.href = '/login'
        }, 1500)
      } else {
        ElMessage.error(res.message || '密码修改失败')
      }
    } catch (error: any) {
      console.error('密码修改失败:', error)
      if (error.response?.data?.message) {
        ElMessage.error(error.response.data.message)
      } else {
        ElMessage.error('密码修改失败，请稍后重试')
      }
    } finally {
      passwordLoading.value = false
    }
  })
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

// 密码验证规则
const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = reactive({
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在 6-20 位之间', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在 6-20 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
})

// 初始化
onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped lang="scss">
.profile-container {
  padding: 20px;

  .profile-card {
    max-width: 700px;
    margin: 0 auto;

    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .profile-content {
    padding: 20px 10px;

    .avatar-section {
      margin-bottom: 20px;
      text-align: center;

      label {
        display: block;
        font-size: 14px;
        color: #606266;
        margin-bottom: 15px;
        font-weight: 500;
      }

      .avatar-upload {
        position: relative;
        display: inline-block;
        cursor: pointer;

        &:hover .avatar-overlay {
          opacity: 1;
        }

        .avatar-overlay {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          color: #fff;
          opacity: 0;
          transition: opacity 0.3s ease;
          border-radius: 50%;

          .el-icon {
            font-size: 28px;
            margin-bottom: 5px;
          }

          span {
            font-size: 13px;
          }
        }

        .avatar-input {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          opacity: 0;
          cursor: pointer;
        }
      }

      .avatar-tip {
        margin-top: 12px;
        font-size: 12px;
        color: #909399;
      }
    }

    .info-form {
      max-width: 400px;
      margin: 0 auto;

      .form-tip {
        font-size: 12px;
        color: #909399;
        margin-top: 5px;
        display: block;
      }
    }
  }

  .password-content {
    padding: 20px 10px;

    .password-form {
      max-width: 400px;
      margin: 0 auto;

      .form-tip {
        font-size: 12px;
        color: #909399;
        margin-top: 5px;
        display: block;
      }
    }
  }
}

// 裁剪对话框样式
.crop-wrapper {
  display: flex;
  gap: 20px;
}

.crop-box {
  flex: 1;
  max-width: 350px;
  height: 350px;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;

  .crop-image {
    max-width: 100%;
    display: block;
  }
}

.preview-box {
  width: 150px;
  text-align: center;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 8px;

  p {
    margin: 0 0 15px 0;
    font-size: 14px;
    color: #606266;
  }

  .preview-avatar {
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
  }
}

// 覆盖对话框内容区域的内边距
:deep(.el-dialog__body) {
  padding-top: 20px;
}

// Cropper 样式
:deep(.cropper-container) {
  background: #f5f7fa;
  border-radius: 8px;
}

:deep(.cropper-view-box) {
  outline: 2px solid var(--el-color-primary);
  border-radius: 50%;
}

:deep(.cropper-face) {
  background: transparent;
}
</style>
