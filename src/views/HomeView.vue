<template>
  <div class="page">
    <div class="card">
      <h1>炒股模拟器</h1>

      <button type="button" class="start-btn" @click="startGame">开始游戏</button>

      <div class="difficulty-list">
        <button
          type="button"
          :class="{ active: currentDifficulty === 'easy' }"
          @click="currentDifficulty = 'easy'"
        >
          简单
        </button>
        <button
          type="button"
          :class="{ active: currentDifficulty === 'medium' }"
          @click="currentDifficulty = 'medium'"
        >
          中等
        </button>
        <button
          type="button"
          :class="{ active: currentDifficulty === 'hard' }"
          @click="currentDifficulty = 'hard'"
        >
          困难
        </button>
        <button
          type="button"
          :class="{ active: currentDifficulty === 'custom' }"
          @click="currentDifficulty = 'custom'"
        >
          自定义
        </button>
      </div>

      <div class="desc">
        <p v-if="currentDifficulty === 'easy'">初始金钱10000，突发事件概率降低</p>
        <p v-else-if="currentDifficulty === 'medium'">初始金钱5000，突发事件概率中等</p>
        <p v-else-if="currentDifficulty === 'hard'">初始金钱1000，突发事件概率较高</p>
        <div v-else class="custom-form">
          <label for="customMoney">初始金钱</label>
          <input
            id="customMoney"
            v-model="customMoney"
            type="number"
            min="1"
            placeholder="请输入初始金钱"
          />

          <label for="customProbability">突发事件概率</label>
          <div class="percent-wrap">
            <input
              id="customProbability"
              v-model="customProbability"
              type="number"
              min="0"
              max="100"
              placeholder="请输入概率"
            />
            <span class="suffix">%</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const currentDifficulty = ref('medium')
const customMoney = ref('')
const customProbability = ref('')

const difficultyLabel = {
  easy: '简单',
  medium: '中等',
  hard: '困难',
  custom: '自定义',
}

const difficultyConfig = {
  easy: {
    money: 10000,
    probability: 10,
  },
  medium: {
    money: 5000,
    probability: 25,
  },
  hard: {
    money: 1000,
    probability: 50,
  },
}

const API_BASE = 'http://localhost:8080'

const startGame = async () => {
  let money
  let probability

  if (currentDifficulty.value === 'custom') {
    money = Number(customMoney.value)
    probability = Number(customProbability.value)

    if (customMoney.value === '' || Number.isNaN(money) || money <= 0) {
      alert('请输入有效的初始金钱')
      return
    }

    if (
      customProbability.value === '' ||
      Number.isNaN(probability) ||
      probability < 0 ||
      probability > 100
    ) {
      alert('请输入有效的突发事件概率（0-100）')
      return
    }
  } else {
    const config = difficultyConfig[currentDifficulty.value]
    money = config.money
    probability = config.probability
  }

  const userId = Number(localStorage.getItem('userId'))
  const username = localStorage.getItem('username')
  if (!userId || !username) {
    alert('请先登录')
    router.push('/login')
    return
  }

  const now = new Date()
  const gameDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  const gameTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`

  try {
    const res = await fetch(`${API_BASE}/api/user/saveGameSetting`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId,
        username,
        difficulty: difficultyLabel[currentDifficulty.value],
        money,
        gameTime,
        gameDate,
      }),
    })
    const body = await res.json()
    if (body.code !== 200) {
      alert(body.msg || '保存游戏设置失败')
      return
    }
  } catch {
    alert('无法连接后端，请确认 Spring Boot 已启动')
    return
  }

  sessionStorage.setItem('gameProbability', String(probability))
  router.push('/game')
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
  font-weight: 700;
  color: #1e293b;
}

.start-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: #42b883;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
}

.start-btn:hover {
  background: #33a06f;
}

.difficulty-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.difficulty-list button {
  flex: 1 1 calc(50% - 8px);
  min-width: 0;
  padding: 10px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 14px;
  cursor: pointer;
}

.difficulty-list button.active {
  background: #42b883;
  border-color: #42b883;
  color: #fff;
}

@media (min-width: 480px) {
  .difficulty-list button {
    flex: 1 1 0;
  }
}

.desc {
  margin-top: 16px;
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
}

.desc p {
  margin: 0;
}

.custom-form {
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

.percent-wrap {
  position: relative;
}

.percent-wrap input {
  width: 100%;
  box-sizing: border-box;
  padding-right: 36px;
}

.suffix {
  position: absolute;
  top: 0;
  right: 12px;
  height: calc(100% - 8px);
  display: flex;
  align-items: center;
  color: #64748b;
  font-size: 14px;
}
</style>
