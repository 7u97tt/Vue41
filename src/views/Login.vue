<template>
  <div class="page">
    <div class="card">
      <h1>登录</h1>

      <form @submit.prevent="handleLogin">
        <label for="account">账号</label>
        <input
          id="account"
          v-model="account"
          type="text"
          placeholder="请输入账号"
          autocomplete="username"
        />

        <label for="password">密码</label>
        <input
          id="password"
          v-model="password"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
        />

        <button type="submit">登录</button>
      </form>

      <p class="footer">
        没有账号？
        <RouterLink to="/register">去注册</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { apiUrl } from '@/config/api.js'

const router = useRouter()
const account = ref('')
const password = ref('')

const handleLogin = async () => {
  if (!account.value.trim()) {
    alert('账号不能为空')
    return
  }
  if (!password.value) {
    alert('密码不能为空')
    return
  }

  const loginUrl = apiUrl('/api/user/login')
  try {
    const res = await fetch(loginUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: account.value.trim(),
        password: password.value,
      }),
    })
    const rawText = await res.text()
    let body
    try {
      body = JSON.parse(rawText)
    } catch {
      let hint = ''
      if (res.status === 403) {
        hint =
          '\n\nHTTP 403 常见原因：\n① 穿透只放了前端，POST /api 被网关拦截 → 请再给本机 8080 映射一个公网端口，并在 .env.local 设置 VITE_API_BASE=http://218.12.120.170:后端端口\n② 请求路径错误（应含 /api/user/login 而非 /login/api/...）'
      }
      alert(`接口返回异常 (HTTP ${res.status})\n完整请求：${loginUrl}${hint}`)
      return
    }
    if (body.code !== 200) {
      alert(body.msg || '登录失败')
      return
    }
    localStorage.setItem('token', body.data.token)
    localStorage.setItem('userId', String(body.data.userId))
    localStorage.setItem('username', body.data.username)

    const archiveRes = await fetch(
      apiUrl(`/api/user/checkHasGameArchive?userId=${body.data.userId}`),
    )
    const archiveBody = await archiveRes.json()
    if (archiveBody.code !== 200) {
      alert(archiveBody.msg || '查询存档失败')
      return
    }
    if (archiveBody.data?.hasArchive) {
      router.push('/game')
    } else {
      router.push('/home')
    }
  } catch (e) {
    console.error('login request failed', loginUrl, e)
    const msg = e?.message ? String(e.message) : '网络错误'
    alert(
      `无法完成登录请求：${msg}\n请求地址：${loginUrl}\n\n请确认：Spring Boot 已启动；穿透指向 127.0.0.1:5173；外网打开的是穿透域名而非 localhost。`,
    )
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

input:focus {
  border-color: #42b883;
}

button {
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

button:hover {
  background: #33a06f;
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
