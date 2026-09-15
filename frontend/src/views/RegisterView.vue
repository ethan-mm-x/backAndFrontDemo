<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchCaptcha, fetchPublicKey, register } from '../api/user'
import { encryptPassword } from '../api/sm2'

const router = useRouter()
const loading = ref(false)
const captchaImg = ref('')
const publicKeyHex = ref('')
const form = reactive({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: '',
})

async function loadCaptcha() {
  const [{ data: captcha }, { data: key }] = await Promise.all([fetchCaptcha(), fetchPublicKey()])
  form.captchaId = captcha.data.captchaId
  captchaImg.value = captcha.data.imageBase64
  publicKeyHex.value = key.data.publicKeyHex
  form.captchaCode = ''
}

async function onSubmit() {
  if (!publicKeyHex.value) {
    ElMessage.error('公钥未就绪，请刷新验证码后重试')
    return
  }
  loading.value = true
  try {
    await register({
      ...form,
      password: encryptPassword(form.password, publicKeyHex.value),
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<template>
  <div class="auth-page">
    <el-card class="card">
      <h1>注册</h1>
      <el-form label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" maxlength="8" />
            <img v-if="captchaImg" :src="captchaImg" class="captcha" alt="captcha" @click="loadCaptcha" />
          </div>
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">注册</el-button>
      </el-form>
      <p class="link">
        已有账号？
        <router-link to="/login">去登录</router-link>
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #e8eef5, #f7f8fa 45%, #dfe8f2);
}
.card {
  width: 400px;
}
h1 {
  margin: 0 0 16px;
  font-size: 24px;
}
.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.captcha {
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
}
.link {
  margin-top: 16px;
  text-align: center;
  color: #666;
}
</style>
