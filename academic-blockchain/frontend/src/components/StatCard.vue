<template>
  <div class="stat-card" :style="{ borderLeftColor: color }">
    <div class="stat-title">{{ title }}</div>
    <div class="stat-value" :style="{ color }">{{ value || 0 }}</div>
  </div>
</template>

<script setup>
defineProps({
  title: String,
  value: [Number, String],
  color: { type: String, default: '#1f4cf0' }
})
</script>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  border-left: 4px solid #1f4cf0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}
.stat-title { color: #909399; font-size: 13px; }
.stat-value { font-size: 28px; font-weight: 600; margin-top: 4px; }
</style>
