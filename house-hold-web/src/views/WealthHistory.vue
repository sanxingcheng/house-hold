<template>
  <MainLayout>
    <div class="wealth-history">
      <h1>资产趋势</h1>

      <div class="controls">
        <div class="tabs">
          <button :class="['tab', { active: tab === 'user' }]" @click="tab = 'user'">个人资产</button>
          <button :class="['tab', { active: tab === 'family' }]" @click="tab = 'family'">家庭资产</button>
        </div>
        <div class="range-picker">
          <button :class="['range-btn', { active: range === '30' }]" @click="range = '30'">近 30 天</button>
          <button :class="['range-btn', { active: range === '90' }]" @click="range = '90'">近 90 天</button>
          <button :class="['range-btn', { active: range === '180' }]" @click="range = '180'">近 180 天</button>
        </div>
      </div>

      <div class="chart-container">
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="chartData.length === 0" class="empty">
          <p>{{ tab === 'family' && !hasFamilyId ? '尚未加入家庭' : '尚无资产记录，去添加账户吧' }}</p>
        </div>
        <div v-else class="chart-wrapper">
          <canvas ref="chartCanvas"></canvas>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import MainLayout from '@/layouts/MainLayout.vue'
import { useWealthStore } from '@/stores/wealth'
import { useAuthStore } from '@/stores/auth'

const wealthStore = useWealthStore()
const authStore = useAuthStore()
const { userHistory, familyHistory } = storeToRefs(wealthStore)

const tab = ref<'user' | 'family'>('user')
const range = ref('30')
const loading = ref(false)
const chartCanvas = ref<HTMLCanvasElement | null>(null)

const hasFamilyId = computed(() => !!authStore.user?.familyId)

const chartData = computed(() => {
  return tab.value === 'user' ? userHistory.value : familyHistory.value
})

function getDateRange() {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - parseInt(range.value))
  return {
    from: from.toISOString().slice(0, 10),
    to: to.toISOString().slice(0, 10),
  }
}

async function loadData() {
  loading.value = true
  try {
    const { from, to } = getDateRange()
    if (tab.value === 'user') {
      await wealthStore.fetchUserHistory(from, to)
    } else {
      if (hasFamilyId.value) {
        await wealthStore.fetchFamilyHistory(from, to)
      }
    }
  } finally {
    loading.value = false
  }
}

function drawChart() {
  const canvas = chartCanvas.value
  if (!canvas || chartData.value.length === 0) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const dpr = window.devicePixelRatio || 1
  const rect = canvas.parentElement!.getBoundingClientRect()
  canvas.width = rect.width * dpr
  canvas.height = 320 * dpr
  canvas.style.width = rect.width + 'px'
  canvas.style.height = '320px'
  ctx.scale(dpr, dpr)

  const w = rect.width
  const h = 320
  const pad = { top: 30, right: 40, bottom: 50, left: 80 }
  const cw = w - pad.left - pad.right
  const ch = h - pad.top - pad.bottom

  const points = chartData.value
  const values = points.map(p => p.netWorth / 100)
  const minVal = Math.min(...values)
  const maxVal = Math.max(...values)
  const valRange = maxVal - minVal || 1

  ctx.clearRect(0, 0, w, h)

  ctx.strokeStyle = '#e5e7eb'
  ctx.lineWidth = 1
  const gridLines = 5
  for (let i = 0; i <= gridLines; i++) {
    const y = pad.top + (ch / gridLines) * i
    ctx.beginPath()
    ctx.moveTo(pad.left, y)
    ctx.lineTo(w - pad.right, y)
    ctx.stroke()

    const val = maxVal - (valRange / gridLines) * i
    ctx.fillStyle = '#999'
    ctx.font = '12px sans-serif'
    ctx.textAlign = 'right'
    ctx.fillText(formatCompact(val), pad.left - 8, y + 4)
  }

  const lineColor = tab.value === 'user' ? '#2563eb' : '#ea580c'
  ctx.strokeStyle = lineColor
  ctx.lineWidth = 2
  ctx.lineJoin = 'round'
  ctx.beginPath()
  points.forEach((p, i) => {
    const x = pad.left + (cw / Math.max(points.length - 1, 1)) * i
    const y = pad.top + ch - ((values[i] - minVal) / valRange) * ch
    if (i === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  })
  ctx.stroke()

  ctx.fillStyle = lineColor
  points.forEach((p, i) => {
    const x = pad.left + (cw / Math.max(points.length - 1, 1)) * i
    const y = pad.top + ch - ((values[i] - minVal) / valRange) * ch
    ctx.beginPath()
    ctx.arc(x, y, 3, 0, Math.PI * 2)
    ctx.fill()
  })

  ctx.fillStyle = '#999'
  ctx.font = '11px sans-serif'
  ctx.textAlign = 'center'
  const labelStep = Math.max(1, Math.floor(points.length / 8))
  points.forEach((p, i) => {
    if (i % labelStep === 0 || i === points.length - 1) {
      const x = pad.left + (cw / Math.max(points.length - 1, 1)) * i
      ctx.fillText(p.snapshotDate.slice(5), x, h - pad.bottom + 18)
    }
  })
}

function formatCompact(val: number): string {
  if (Math.abs(val) >= 10000) {
    return (val / 10000).toFixed(1) + '万'
  }
  return val.toFixed(0)
}

watch([tab, range], () => {
  loadData()
})

watch(chartData, async () => {
  await nextTick()
  drawChart()
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.wealth-history {
  max-width: 960px;
  margin: 0 auto;
}
.wealth-history h1 {
  font-size: 1.5rem;
  color: #333;
  margin-bottom: 1.25rem;
}
.controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}
.tabs {
  display: flex;
  gap: 0;
  border: 1px solid #ddd;
  border-radius: 6px;
  overflow: hidden;
}
.tab {
  padding: 0.45rem 1rem;
  border: none;
  background: #fff;
  cursor: pointer;
  font-size: 0.875rem;
  color: #555;
}
.tab.active {
  background: #2563eb;
  color: #fff;
}
.tab:not(:last-child) {
  border-right: 1px solid #ddd;
}
.range-picker {
  display: flex;
  gap: 0.5rem;
}
.range-btn {
  padding: 0.4rem 0.85rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 0.8rem;
  color: #555;
}
.range-btn.active {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}
.chart-container {
  background: #fff;
  border-radius: 10px;
  padding: 1.5rem;
  min-height: 360px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}
.chart-wrapper {
  width: 100%;
}
.loading {
  color: #999;
}
.empty {
  color: #999;
  text-align: center;
}
</style>
