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
import { RouterLink } from 'vue-router'

const account = ref('')
const password = ref('')

const handleLogin = () => {
  if (!account.value.trim()) {
    alert('账号不能为空')
    return
  }
  if (!password.value) {
    alert('密码不能为空')
    return
  }
  console.log({
    account: account.value.trim(),
    password: password.value,
  })
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
