<template>
  <div class="guest-layout">
    <header class="header">
      <router-link to="/" class="logo">家庭资产</router-link>
    </header>
    <main class="main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.guest-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}
.header {
  padding: 1rem 2rem;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.logo {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
  text-decoration: none;
}
.main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}
</style>
