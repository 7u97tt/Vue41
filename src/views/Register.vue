<template>
  <div class="page">
    <div class="card">
      <h1>注册</h1>

      <form @submit.prevent="handleRegister">
        <label for="username">账号</label>
        <input
          id="username"
          v-model="username"
          type="text"
          placeholder="4-10位字母或数字"
          autocomplete="username"
        />

        <label for="password">密码</label>
        <div class="password-wrap">
          <input
            id="password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
            autocomplete="new-password"
          />
          <button
            type="button"
            class="eye-btn"
            aria-label="切换密码可见性"
            @click="showPassword = !showPassword"
          >
            <svg
              v-if="showPassword"
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="1.8"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"
              />
            </svg>
            <svg
              v-else
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="1.8"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.964-7.178z"
              />
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
          </button>
        </div>

        <label for="confirmPassword">确认密码</label>
        <div class="password-wrap">
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            placeholder="请再次输入密码"
            autocomplete="new-password"
          />
          <button
            type="button"
            class="eye-btn"
            aria-label="切换确认密码可见性"
            @click="showConfirmPassword = !showConfirmPassword"
          >
            <svg
              v-if="showConfirmPassword"
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="1.8"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"
              />
            </svg>
            <svg
              v-else
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="1.8"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.964-7.178z"
              />
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
          </button>
        </div>

        <p v-if="message" class="message" :class="messageType">{{ message }}</p>

        <button type="submit" :disabled="submitting">
          {{ submitting ? '注册中…' : '注册' }}
        </button>
      </form>

      <p class="footer">
        已有账号？
        <RouterLink to="/login">去登录</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import axios from 'axios'
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { apiUrl } from '@/config/api.js'

// #region agent log
onMounted(() => {
  fetch('http://127.0.0.1:7491/ingest/47454bd9-cd64-4d3c-a84e-ffc937c920bc', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Debug-Session-Id': '251970' },
    body: JSON.stringify({
      sessionId: '251970',
      hypothesisId: 'H-C',
      location: 'Register.vue:onMounted',
      message: 'register-module-loaded',
      data: { axiosPostFn: typeof axios.post === 'function' },
      timestamp: Date.now(),
      runId: 'pre-fix',
    }),
  }).catch(() => {})
})
// #endregion

const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const message = ref('')
const messageType = ref('error')
const submitting = ref(false)

const usernameReg = /^[A-Za-z0-9]{4,10}$/

function setMessage(text, type = 'error') {
  message.value = text
  messageType.value = type
}

function validateForm() {
  const name = username.value.trim()
  if (!name) {
    setMessage('用户名不能为空')
    return false
  }
  if (!usernameReg.test(name)) {
    setMessage('用户名为 4-10 位字母或数字，不能包含特殊字符')
    return false
  }
  if (!password.value) {
    setMessage('密码不能为空')
    return false
  }
  if (!confirmPassword.value) {
    setMessage('确认密码不能为空')
    return false
  }
  if (password.value !== confirmPassword.value) {
    setMessage('两次输入的密码不一致')
    return false
  }
  return true
}

async function handleRegister() {
  message.value = ''
  if (!validateForm()) {
    return
  }

  submitting.value = true
  try {
    const { data: body } = await axios.post(apiUrl('/api/user/register'), {
      username: username.value.trim(),
      password: password.value,
      confirmPassword: confirmPassword.value,
    })

    if (body.code !== 200) {
      setMessage(body.msg || '注册失败')
      return
    }

    setMessage(body.msg || '注册成功', 'success')
    setTimeout(() => {
      router.push('/login')
    }, 800)
  } catch {
    setMessage('无法连接后端，请确认 Spring Boot 已启动')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f1f5f9;
}

.card {
  width: 100%;
  max-width: 400px;
  padding: 32px 28px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

h1 {
  margin: 0 0 24px;
  text-align: center;
  font-size: 24px;
  color: #1e293b;
}

form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

label {
  font-size: 14px;
  color: #475569;
}

input {
  margin-bottom: 8px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.password-wrap {
  position: relative;
}

.password-wrap input {
  width: 100%;
  box-sizing: border-box;
  padding-right: 42px;
}

.eye-btn {
  position: absolute;
  top: 0;
  right: 0;
  width: 42px;
  height: calc(100% - 8px);
  margin: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.eye-btn:hover {
  color: #64748b;
  background: transparent;
}

input:focus {
  border-color: #42b883;
}

.message {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
  text-align: center;
}

.message.error {
  color: #dc2626;
}

.message.success {
  color: #16a34a;
}

button[type='submit'] {
  margin-top: 8px;
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: #42b883;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
}

button[type='submit']:hover:not(:disabled) {
  background: #33a06f;
}

button[type='submit']:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.footer {
  margin: 20px 0 0;
  text-align: center;
  font-size: 14px;
  color: #64748b;
}

.footer a {
  color: #42b883;
  text-decoration: none;
}

.footer a:hover {
  text-decoration: underline;
}
</style>
