<template>
  <el-card shadow="never" class="stat-card">
    <div class="stat-inner">
      <div class="stat-icon" :style="{ background: color + '20', color }">
        <el-icon size="28"><component :is="icon" /></el-icon>
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ value }}</div>
        <div class="stat-label">{{ label }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({ icon: String, label: String, value: [String, Number], color: String })
</script>

<style scoped>
.stat-card { border-radius: var(--radius-md, 8px); }
.stat-inner { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 56px; height: 56px; border-radius: var(--radius-lg, 12px); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-value { font-size: var(--font-size-xxl, 24px); font-weight: var(--font-weight-bold, 700); color: var(--color-text-primary, #1a1a1a); line-height: 1; }
.stat-label { font-size: var(--font-size-sm, 13px); color: var(--color-text-secondary, #909399); margin-top: 4px; }
</style>
