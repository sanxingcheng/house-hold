<template>
  <MainLayout>
    <div class="wealth-history">
      <h1 style="margin:0 0 20px">资产趋势</h1>

      <el-card shadow="never">
        <div class="controls">
          <el-tabs v-model="tab" @tab-change="loadData">
            <el-tab-pane label="个人资产" name="user" />
            <el-tab-pane label="家庭资产" name="family" />
          </el-tabs>

          <el-radio-group v-model="range" size="small" @change="loadData">
            <el-radio-button value="30">近 30 天</el-radio-button>
            <el-radio-button value="90">近 90 天</el-radio-button>
            <el-radio-button value="180">近 180 天</el-radio-button>
          </el-radio-group>
        </div>

        <div class="chart-area" v-loading="loading">
          <template v-if="!loading && chartData.length === 0">
            <el-empty :description="emptyText" />
          </template>
          <v-chart v-else-if="chartData.length > 0" :option="chartOption" autoresize style="height:400px;width:100%" />
        </div>
      </el-card>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import MainLayout from '@/layouts/MainLayout.vue'
import { useWealthStore } from '@/stores/wealth'
import { useAuthStore } from '@/stores/auth'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const wealthStore = useWealthStore()
const authStore = useAuthStore()
const { userHistory, familyHistory } = storeToRefs(wealthStore)

const tab = ref('user')
const range = ref('30')
const loading = ref(false)

const hasFamilyId = computed(() => !!authStore.user?.familyId)
const chartData = computed(() => tab.value === 'user' ? userHistory.value : familyHistory.value)
const emptyText = computed(() =>
  tab.value === 'family' && !hasFamilyId.value ? '尚未加入家庭' : '尚无资产记录，去添加账户吧'
)

function formatYuan(fen: number) {
  return (fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const chartOption = computed(() => {
  const dates = chartData.value.map(p => p.snapshotDate)
  const values = chartData.value.map(p => p.netWorth / 100)
  const color = tab.value === 'user' ? '#409eff' : '#e6a23c'

  return {
    tooltip: {
      trigger: 'axis' as const,
      formatter: (params: Array<{ axisValue: string; value: number }>) => {
        const p = params[0]
        return `${p.axisValue}<br/>净资产：<b>¥ ${formatYuan(p.value * 100)}</b>`
      },
    },
    grid: { left: 80, right: 30, top: 30, bottom: 40 },
    xAxis: {
      type: 'category' as const,
      data: dates,
      axisLabel: {
        formatter: (v: string) => v.slice(5),
      },
    },
    yAxis: {
      type: 'value' as const,
      axisLabel: {
        formatter: (v: number) => {
          if (Math.abs(v) >= 10000) return (v / 10000).toFixed(1) + '万'
          return v.toFixed(0)
        },
      },
    },
    series: [{
      type: 'line' as const,
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color, width: 2 },
      itemStyle: { color },
      areaStyle: { color, opacity: 0.08 },
    }],
  }
})

function getDateRange() {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - parseInt(range.value))
  return { from: from.toISOString().slice(0, 10), to: to.toISOString().slice(0, 10) }
}

async function loadData() {
  loading.value = true
  try {
    const { from, to } = getDateRange()
    if (tab.value === 'user') {
      await wealthStore.fetchUserHistory(from, to)
    } else if (hasFamilyId.value) {
      await wealthStore.fetchFamilyHistory(from, to)
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.wealth-history {
  max-width: 960px;
  margin: 0 auto;
}
.controls {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.chart-area {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
