<template>
  <div class="page">
    <div class="card">
      <h1>游戏进行中</h1>
    </div>
  </div>
</template>

<script setup>
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
  margin: 0;
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}
</style>
