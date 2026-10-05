<template>
  <div class="page">
    <header class="top-bar card">
      <div class="top-left">
        <span class="label">当前时间</span>
        <span class="time-value">{{ currentTime }}</span>
        <span class="label game-time-label">游戏时间</span>
        <span class="time-value">{{ gameTime }}</span>
      </div>
      <div class="top-center">
        <button type="button" class="btn-secondary" @click="nextTradingDay">
          下一个交易日
        </button>
      </div>
      <div class="top-right">
        <span class="username">{{ username }}</span>
        <button type="button" class="btn-outline" @click="logout">退出登录</button>
      </div>
    </header>

    <section class="asset-bar card">
      <div class="asset-stats">
        <div class="stat-item">
          <span class="stat-label">用户金钱(可用)</span>
          <span class="stat-value">¥ {{ formatMoney(userMoney) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">用户资产</span>
          <span class="stat-value">¥ {{ formatMoney(userAsset) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">资产涨跌幅(%)</span>
          <span class="stat-value" :class="changeClass(assetChange)">
            {{ formatPercent(assetChange) }}
          </span>
        </div>
      </div>
      <div class="asset-actions">
        <button type="button" class="btn-primary" @click="goHoldings">我的持仓</button>
        <button
          type="button"
          class="btn-primary"
          :class="{ active: showNews }"
          @click="toggleNews"
        >
          新闻一览
        </button>
      </div>
      <div class="early-holdings">
        <div
          v-for="item in earlyHoldingsDisplay"
          :key="item.code"
          class="holding-chip"
        >
          <span class="chip-name">{{ item.name }}</span>
          <span class="chip-change" :class="changeClass(item.change)">
            {{ formatPercent(item.change) }}
          </span>
        </div>
      </div>
    </section>

    <div class="main-body">
      <aside class="sub-nav">
        <button
          type="button"
          class="sub-nav-btn"
          :class="{ active: currentSubPage === 'stock' }"
          @click="selectSubPage('stock')"
        >
          股票交易
        </button>
        <button
          type="button"
          class="sub-nav-btn"
          :class="{ active: currentSubPage === 'futures' }"
          @click="selectSubPage('futures')"
        >
          期货交易
        </button>
        <button
          type="button"
          class="sub-nav-btn"
          :class="{ active: currentSubPage === 'crypto' }"
          @click="selectSubPage('crypto')"
        >
          虚拟货币
        </button>
      </aside>

      <main class="main-content">
        <div v-if="currentSubPage !== 'stock'" class="coming-soon card">
          <p>敬请期待</p>
        </div>

        <div v-else class="stock-panel">
          <div class="industry-col card">
            <button
              v-for="ind in industries"
              :key="ind.key"
              type="button"
              class="industry-btn"
              :class="{ active: currentIndustry === ind.key }"
              @click="selectIndustry(ind.key)"
            >
              {{ ind.label }}
            </button>
          </div>

          <div class="b-area card">
            <template v-if="showNews">
              <h2 class="panel-title">历史新闻</h2>
              <ul class="news-list">
                <li v-for="(news, idx) in newsList" :key="idx" class="news-item">
                  <h3 class="news-title">{{ news.title }}</h3>
                  <time class="news-time">{{ news.time }}</time>
                  <p class="news-summary">{{ news.summary }}</p>
                </li>
              </ul>
            </template>

            <template v-else-if="currentStock">
              <button type="button" class="back-btn" @click="backToStockList">← 返回</button>
              <h2 class="panel-title detail-title">
                {{ currentStock.name }}
                <span class="detail-code">{{ currentStock.code }}</span>
                <span v-if="marketClosed" class="market-closed-tag">已收盘</span>
              </h2>

              <div class="chart-wrap">
                <svg
                  class="price-chart"
                  :viewBox="`0 0 ${chartSize.width} ${chartSize.height}`"
                  preserveAspectRatio="xMidYMid meet"
                >
                  <line
                    v-for="(grid, i) in chartGridLines"
                    :key="'g' + i"
                    :x1="chartPadding.left"
                    :y1="grid.y"
                    :x2="chartSize.width - chartPadding.right"
                    :y2="grid.y"
                    class="grid-line"
                  />
                  <line
                    :x1="chartPadding.left"
                    :y1="chartPadding.top"
                    :x2="chartPadding.left"
                    :y2="chartSize.height - chartPadding.bottom"
                    class="axis-line"
                  />
                  <line
                    :x1="chartPadding.left"
                    :y1="chartSize.height - chartPadding.bottom"
                    :x2="chartSize.width - chartPadding.right"
                    :y2="chartSize.height - chartPadding.bottom"
                    class="axis-line"
                  />
                  <rect
                    v-if="lunchGapRect"
                    :x="lunchGapRect.x"
                    :y="chartPadding.top"
                    :width="lunchGapRect.width"
                    :height="chartSize.height - chartPadding.top - chartPadding.bottom"
                    class="lunch-gap"
                  />
                  <text
                    v-if="lunchGapRect"
                    :x="lunchGapRect.x + lunchGapRect.width / 2"
                    :y="chartSize.height / 2"
                    class="lunch-label"
                    text-anchor="middle"
                  >
                    休盘
                  </text>
                  <text
                    v-for="(tick, i) in yAxisLabels"
                    :key="'y' + i"
                    :x="chartPadding.left - 8"
                    :y="tick.y + 4"
                    class="axis-label axis-label-y"
                    text-anchor="end"
                  >
                    {{ tick.label }}
                  </text>
                  <text
                    v-for="(tick, i) in xAxisLabels"
                    :key="'x' + i"
                    :x="tick.x"
                    :y="chartSize.height - chartPadding.bottom + 22"
                    class="axis-label"
                    text-anchor="middle"
                  >
                    {{ tick.label }}
                  </text>
                  <polyline
                    v-for="(seg, si) in chartPolylines"
                    :key="'line' + si + chartRevision"
                    :points="seg"
                    fill="none"
                    :stroke="chartLineColor"
                    stroke-width="1.5"
                    stroke-linejoin="round"
                    stroke-linecap="round"
                  />
                  <circle
                    v-for="(pt, i) in chartPoints"
                    :key="'p' + i + chartRevision"
                    :cx="pt.x"
                    :cy="pt.y"
                    r="2"
                    :fill="chartLineColor"
                  />
                </svg>
              </div>

              <div class="live-price-block">
                <div class="live-price" :class="{ closed: marketClosed }">
                  ¥ {{ detailLatestPrice.toFixed(2) }}
                </div>
                <div class="live-delta">
                  <span :class="changeClass(tickPriceDelta)">
                    {{ formatDeltaAmount(tickPriceDelta) }}
                  </span>
                  <span :class="changeClass(tickPriceDeltaPct)">
                    {{ formatPercent(tickPriceDeltaPct) }}
                  </span>
                </div>
              </div>

              <div class="trade-actions">
                <button
                  type="button"
                  class="btn-buy"
                  :disabled="marketClosed"
                  @click="openTradeModal('buy')"
                >
                  买入
                </button>
                <button
                  v-if="currentHolding && currentHolding.quantity > 0"
                  type="button"
                  class="btn-sell"
                  :disabled="marketClosed"
                  @click="openTradeModal('sell')"
                >
                  卖出
                </button>
              </div>
            </template>

            <template v-else>
              <h2 class="panel-title">{{ currentIndustryLabel }}</h2>
              <div class="table-wrap">
                <table class="stock-table">
                  <thead>
                    <tr>
                      <th>股票名称</th>
                      <th>股票代码</th>
                      <th>当前价格</th>
                      <th>涨跌幅</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="stock in currentStocks"
                      :key="stock.code"
                      class="stock-row"
                      @click="openStockDetail(stock)"
                    >
                      <td>{{ stock.name }}</td>
                      <td>{{ stock.code }}</td>
                      <td>{{ stock.price.toFixed(2) }}</td>
                      <td :class="changeClass(stock.change)">
                        {{ formatPercent(stock.change) }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </template>
          </div>
        </div>
      </main>
    </div>

    <div
      v-if="tradeModal"
      class="modal-overlay"
      @click.self="closeTradeModal"
    >
      <div class="modal-card">
        <h3 class="modal-title">
          {{ tradeModal === 'buy' ? '买入' : '卖出' }}股票 - {{ currentStock?.name }}
        </h3>

        <p v-if="tradeModal === 'sell' && currentHolding" class="modal-hint">
          当前持有数量：{{ currentHolding.quantity }} 股
        </p>

        <label class="modal-label" for="tradeQty">股数</label>
        <input
          id="tradeQty"
          v-model="tradeQuantityInput"
          type="number"
          class="modal-input"
          :placeholder="tradeModal === 'buy' ? '请输入买入股数' : '请输入卖出股数'"
          @input="tradeError = ''"
        />
        <p v-if="tradeError" class="modal-error">{{ tradeError }}</p>

        <p class="modal-hint">当前股价：¥ {{ detailLatestPrice.toFixed(2) }}</p>
        <p class="modal-hint">
          预计总金额：¥ {{ formatMoney(estimatedTradeAmount) }}
        </p>

        <div class="modal-actions">
          <button type="button" class="btn-modal-cancel" @click="closeTradeModal">
            取消
          </button>
          <button
            type="button"
            class="btn-modal-confirm"
            :class="tradeModal === 'sell' ? 'btn-sell' : 'btn-buy'"
            @click="confirmTrade"
          >
            确认{{ tradeModal === 'buy' ? '买入' : '卖出' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const POSITIVE_INT_REG = /^[1-9]\d*$/
const POINT_COUNT = 50
const INITIAL_GAME_TIME = '13:35'

const currentTime = ref('')
const gameTime = ref(INITIAL_GAME_TIME)
const marketClosed = ref(false)
const stockPrices = ref({})
const chartRevision = ref(0)

const userMoney = ref(128500.5)
const userAsset = ref(256800.75)
const assetChange = ref(2.35)
const currentSubPage = ref('stock')
const currentIndustry = ref('daily')
const showNews = ref(false)
const username = ref('游客')
const currentStock = ref(null)
const tradeModal = ref(null)
const tradeQuantityInput = ref('')
const tradeError = ref('')

const holdings = ref([
  { code: '600101', name: '清新日化', quantity: 200, avgPrice: 17.8 },
  { code: '600201', name: '城启地产', quantity: 500, avgPrice: 5.05 },
  { code: '600701', name: '芯联电子', quantity: 80, avgPrice: 44.2 },
  { code: '601201', name: '驰峰汽车', quantity: 120, avgPrice: 27.5 },
])

const initialTotalAsset = ref(0)
const rngStore = new Map()

let timeTimer = null
let gameTimer = null

const chartSize = { width: 720, height: 250 }
const chartPadding = { top: 20, right: 20, bottom: 44, left: 52 }

const industries = [
  { key: 'daily', label: '日化用品' },
  { key: 'realestate', label: '房地产' },
  { key: 'transport', label: '交通运输' },
  { key: 'agri', label: '农林牧渔' },
  { key: 'metallurgy', label: '冶金矿产' },
  { key: 'petro', label: '石油化工' },
  { key: 'it', label: '电子信息' },
  { key: 'machinery', label: '机械机电' },
  { key: 'food', label: '食品玩具' },
  { key: 'textile', label: '服装纺织' },
  { key: 'service', label: '专业服务' },
  { key: 'auto', label: '汽车制造' },
  { key: 'office', label: '办公文教' },
  { key: 'furniture', label: '家具用品' },
]

const stockMap = {
  daily: [
    { name: '清新日化', code: '600101', basePrice: 18.42 },
    { name: '蓝湾洗护', code: '600102', basePrice: 9.88 },
    { name: '优护生活', code: '600103', basePrice: 24.15 },
    { name: '净界股份', code: '600104', basePrice: 31.6 },
  ],
  realestate: [
    { name: '城启地产', code: '600201', basePrice: 5.12 },
    { name: '万城控股', code: '600202', basePrice: 8.76 },
    { name: '新岸置业', code: '600203', basePrice: 12.33 },
    { name: '景园开发', code: '600204', basePrice: 6.05 },
    { name: '和信商管', code: '600205', basePrice: 15.8 },
  ],
  transport: [
    { name: '远航物流', code: '600301', basePrice: 11.25 },
    { name: '港通航运', code: '600302', basePrice: 7.44 },
    { name: '陆联快运', code: '600303', basePrice: 19.6 },
    { name: '铁运集团', code: '600304', basePrice: 4.98 },
  ],
  agri: [
    { name: '绿野农牧', code: '600401', basePrice: 13.55 },
    { name: '丰禾种业', code: '600402', basePrice: 22.1 },
    { name: '渔丰水产', code: '600403', basePrice: 8.32 },
    { name: '林源林业', code: '600404', basePrice: 6.77 },
  ],
  metallurgy: [
    { name: '华钢股份', code: '600501', basePrice: 10.85 },
    { name: '矿源集团', code: '600502', basePrice: 14.22 },
    { name: '鑫铜金属', code: '600503', basePrice: 28.9 },
    { name: '合金科技', code: '600504', basePrice: 16.03 },
    { name: '铁矿能源', code: '600505', basePrice: 9.15 },
  ],
  petro: [
    { name: '东海石化', code: '600601', basePrice: 21.4 },
    { name: '炼化股份', code: '600602', basePrice: 17.88 },
    { name: '油气管道', code: '600603', basePrice: 12.05 },
    { name: '化工新材', code: '600604', basePrice: 33.2 },
  ],
  it: [
    { name: '芯联电子', code: '600701', basePrice: 45.6 },
    { name: '云智软件', code: '600702', basePrice: 38.15 },
    { name: '光讯通信', code: '600703', basePrice: 52.8 },
    { name: '数创科技', code: '600704', basePrice: 29.44 },
    { name: '智联设备', code: '600705', basePrice: 18.9 },
  ],
  machinery: [
    { name: '重工装备', code: '600801', basePrice: 14.75 },
    { name: '精机制造', code: '600802', basePrice: 26.3 },
    { name: '机电一体', code: '600803', basePrice: 11.08 },
    { name: '传动机电', code: '600804', basePrice: 9.62 },
  ],
  food: [
    { name: '味享食品', code: '600901', basePrice: 16.25 },
    { name: '童趣玩具', code: '600902', basePrice: 8.9 },
    { name: '鲜焙烘焙', code: '600903', basePrice: 12.44 },
    { name: '乐玩创意', code: '600904', basePrice: 20.15 },
    { name: '优选零食', code: '600905', basePrice: 7.33 },
  ],
  textile: [
    { name: '锦绣纺织', code: '601001', basePrice: 6.85 },
    { name: '尚衣股份', code: '601002', basePrice: 13.2 },
    { name: '丝韵面料', code: '601003', basePrice: 9.05 },
    { name: '潮牌服饰', code: '601004', basePrice: 18.66 },
  ],
  service: [
    { name: '智询顾问', code: '601101', basePrice: 25.5 },
    { name: '安维检测', code: '601102', basePrice: 14.08 },
    { name: '律信服务', code: '601103', basePrice: 32.4 },
    { name: '云账财税', code: '601104', basePrice: 19.75 },
  ],
  auto: [
    { name: '驰峰汽车', code: '601201', basePrice: 28.6 },
    { name: '新能动力', code: '601202', basePrice: 41.2 },
    { name: '精零部件', code: '601203', basePrice: 15.88 },
    { name: '智驾系统', code: '601204', basePrice: 55.3 },
    { name: '商用客车', code: '601205', basePrice: 10.05 },
  ],
  office: [
    { name: '文汇办公', code: '601301', basePrice: 11.35 },
    { name: '学具股份', code: '601302', basePrice: 8.42 },
    { name: '纸品文具', code: '601303', basePrice: 5.96 },
    { name: '智慧教具', code: '601304', basePrice: 22.18 },
  ],
  furniture: [
    { name: '木语家居', code: '601401', basePrice: 17.65 },
    { name: '尚品家具', code: '601402', basePrice: 23.4 },
    { name: '绿居定制', code: '601403', basePrice: 14.05 },
    { name: '软体生活', code: '601404', basePrice: 19.88 },
    { name: '整装空间', code: '601405', basePrice: 27.12 },
  ],
}

const newsList = ref([
  {
    title: '央行维持政策利率不变，市场流动性保持平稳',
    time: '2026/10/05 09:30',
    summary: '最新货币政策会议结果公布，短期利率区间未作调整，金融机构普遍预期后续仍将保持稳健偏宽松基调。',
  },
  {
    title: '电子信息板块午后走强，半导体龙头领涨',
    time: '2026/10/04 14:15',
    summary: '受海外订单回暖预期影响，芯片设计与设备制造相关个股集体上扬，板块成交额较前一交易日明显放大。',
  },
  {
    title: '房地产支持政策持续落地，多地优化购房信贷条件',
    time: '2026/10/03 11:20',
    summary: '部分城市下调首付比例并延长贷款年限，分析师认为政策有助于稳定市场预期，但分化行情仍将延续。',
  },
  {
    title: '国际油价震荡回落，石油化工板块承压',
    time: '2026/10/02 16:45',
    summary: '原油期货价格小幅下跌，炼化企业成本端压力有所缓解，但下游需求恢复节奏仍偏缓慢。',
  },
  {
    title: '新能源汽车销量数据公布，产业链个股分化加剧',
    time: '2026/10/01 10:00',
    summary: '9月乘用车零售数据出炉，头部品牌维持增长，二线厂商竞争加剧，零部件企业业绩预期出现明显差异。',
  },
])

const currentStocks = computed(() => {
  return (stockMap[currentIndustry.value] || []).map((s) => ({
    name: s.name,
    code: s.code,
    price: getLatestPrice(s.code),
    change: getChangePercent(s.code),
  }))
})

const currentIndustryLabel = computed(() => {
  const found = industries.find((i) => i.key === currentIndustry.value)
  return found ? found.label : ''
})

const currentHolding = computed(() => {
  if (!currentStock.value) return null
  return holdings.value.find((h) => h.code === currentStock.value.code) || null
})

const detailPriceSeries = computed(() => {
  chartRevision.value
  if (!currentStock.value) return []
  return stockPrices.value[currentStock.value.code]?.prices ?? []
})

const detailLatestPrice = computed(() => {
  const series = detailPriceSeries.value
  if (!series.length) return 0
  return series[series.length - 1].price
})

const tickPriceDelta = computed(() => {
  const series = detailPriceSeries.value
  if (series.length < 2) return 0
  const last = series[series.length - 1].price
  const prev = series[series.length - 2].price
  return +(last - prev).toFixed(2)
})

const tickPriceDeltaPct = computed(() => {
  const series = detailPriceSeries.value
  if (series.length < 2) return 0
  const last = series[series.length - 1].price
  const prev = series[series.length - 2].price
  if (prev === 0) return 0
  return +(((last - prev) / prev) * 100).toFixed(2)
})

const estimatedTradeAmount = computed(() => {
  if (!currentStock.value || !POSITIVE_INT_REG.test(tradeQuantityInput.value.trim())) {
    return 0
  }
  return detailLatestPrice.value * Number(tradeQuantityInput.value.trim())
})

const earlyHoldingsDisplay = computed(() => {
  chartRevision.value
  return holdings.value.slice(0, 4).map((h) => ({
    code: h.code,
    name: h.name,
    change: getChangePercent(h.code),
  }))
})

const chartPlot = computed(() => {
  chartRevision.value
  const data = detailPriceSeries.value
  if (!data.length) {
    return { min: 0, max: 1, points: [], segments: [] }
  }
  const prices = data.map((d) => d.price)
  let min = Math.min(...prices)
  let max = Math.max(...prices)
  if (min === max) {
    min -= 1
    max += 1
  } else {
    const padVal = (max - min) * 0.1
    min -= padVal
    max += padVal
  }

  const innerW = chartSize.width - chartPadding.left - chartPadding.right
  const innerH = chartSize.height - chartPadding.top - chartPadding.bottom
  const lunchStartX = chartPadding.left + innerW * 0.48
  const lunchEndX = chartPadding.left + innerW * 0.52
  const amWidth = lunchStartX - chartPadding.left
  const pmWidth = chartSize.width - chartPadding.right - lunchEndX

  const points = data.map((d) => {
    const x = timeToChartX(d.time, chartPadding.left, amWidth, lunchEndX, pmWidth)
    const y = chartPadding.top + innerH - ((d.price - min) / (max - min)) * innerH
    return { x, y, price: d.price, time: d.time }
  })

  const segments = []
  let current = []
  for (let i = 0; i < points.length; i += 1) {
    current.push(points[i])
    const next = points[i + 1]
    if (!next) break
    const crossLunch =
      timeToMinutes(points[i].time) <= timeToMinutes('11:30') &&
      timeToMinutes(next.time) >= timeToMinutes('13:00')
    if (crossLunch) {
      segments.push([...current])
      current = []
    }
  }
  if (current.length) segments.push(current)

  return {
    min,
    max,
    points,
    segments,
    lunchStartX,
    lunchEndX,
  }
})

const chartPoints = computed(() => chartPlot.value.points)

const chartPolylines = computed(() =>
  chartPlot.value.segments.map((seg) => seg.map((p) => `${p.x},${p.y}`).join(' ')),
)

const chartLineColor = computed(() => {
  const data = detailPriceSeries.value
  if (data.length < 2) return '#42b883'
  const first = data[0].price
  const last = data[data.length - 1].price
  if (last > first) return '#ef4444'
  if (last < first) return '#22c55e'
  return '#42b883'
})

const yAxisLabels = computed(() => {
  const { min, max } = chartPlot.value
  const innerH = chartSize.height - chartPadding.top - chartPadding.bottom
  const ticks = 5
  const labels = []
  for (let i = 0; i <= ticks; i += 1) {
    const ratio = i / ticks
    const price = max - (max - min) * ratio
    const y = chartPadding.top + innerH * ratio
    labels.push({ y, label: price.toFixed(2) })
  }
  return labels
})

const xAxisLabels = computed(() => {
  const innerW = chartSize.width - chartPadding.left - chartPadding.right
  const lunchStartX = chartPadding.left + innerW * 0.48
  const lunchEndX = chartPadding.left + innerW * 0.52
  const amWidth = lunchStartX - chartPadding.left
  const pmWidth = chartSize.width - chartPadding.right - lunchEndX
  const marks = ['09:30', '11:30', '13:00', '15:00']
  return marks.map((label) => ({
    label,
    x: timeToChartX(label, chartPadding.left, amWidth, lunchEndX, pmWidth),
  }))
})

const lunchGapRect = computed(() => {
  const { lunchStartX, lunchEndX } = chartPlot.value
  if (!lunchStartX) return null
  return { x: lunchStartX, width: lunchEndX - lunchStartX }
})

const chartGridLines = computed(() => yAxisLabels.value)

function pad(n) {
  return String(n).padStart(2, '0')
}

function timeToMinutes(time) {
  const [h, m] = time.split(':').map(Number)
  return h * 60 + m
}

function minutesToTime(mins) {
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return `${pad(h)}:${pad(m)}`
}

function hashCode(str) {
  let hash = 0
  for (let i = 0; i < str.length; i += 1) {
    hash = (hash << 5) - hash + str.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}

function createRng(seed) {
  let state = seed % 233280 || 1
  return () => {
    state = (state * 9301 + 49297) % 233280
    return state / 233280
  }
}

function getRng(code) {
  if (!rngStore.has(code)) {
    rngStore.set(code, createRng(hashCode(code)))
  }
  return rngStore.get(code)
}

function fluctuate(price, pct, rng) {
  const delta = (rng() * 2 - 1) * pct
  return Math.max(0.01, +(price * (1 + delta)).toFixed(2))
}

function openGapPrice(closePrice, rng) {
  const gap = (rng() * 2 - 1) * 0.01
  return Math.max(0.01, +(closePrice * (1 + gap)).toFixed(2))
}

function stepBackSessionTime(time) {
  const m = timeToMinutes(time)
  if (m <= timeToMinutes('09:30')) {
    return minutesToTime(m - 5)
  }
  if (m === timeToMinutes('13:00')) {
    return '11:30'
  }
  return minutesToTime(m - 5)
}

function buildFullDayTimeSlots() {
  const morning = []
  for (let i = 0; i < 25; i += 1) {
    morning.push(minutesToTime(timeToMinutes('09:30') + i * 5))
  }
  const afternoon = []
  for (let i = 0; i < 25; i += 1) {
    afternoon.push(minutesToTime(timeToMinutes('13:00') + i * 5))
  }
  return [...morning, ...afternoon]
}

function buildSessionTimesEndingAt(endTime, count) {
  const times = [endTime]
  let cur = endTime
  while (times.length < count) {
    cur = stepBackSessionTime(cur)
    times.push(cur)
  }
  return times.reverse()
}

function isMorningTime(time) {
  return timeToMinutes(time) <= timeToMinutes('11:30')
}

function isAfternoonTime(time) {
  return timeToMinutes(time) >= timeToMinutes('13:00')
}

function generatePricesAlongTimes(times, startPrice, code) {
  const rng = getRng(code)
  const prices = []
  let lastPrice = startPrice

  for (let i = 0; i < times.length; i += 1) {
    const t = times[i]
    if (i === 0) {
      prices.push({ time: t, price: lastPrice })
      continue
    }
    const prevTime = times[i - 1]
    const crossedLunch =
      isMorningTime(prevTime) &&
      isAfternoonTime(t) &&
      timeToMinutes(prevTime) <= timeToMinutes('11:30')
    if (crossedLunch) {
      const morningClose =
        [...prices].reverse().find((p) => p.time === '11:30')?.price ??
        prices[prices.length - 1].price
      lastPrice = openGapPrice(morningClose, rng)
    } else {
      lastPrice = fluctuate(lastPrice, 0.02, rng)
    }
    prices.push({ time: t, price: lastPrice })
  }
  return prices
}

function generateInitialPrices(basePrice, code) {
  const daySlots = buildFullDayTimeSlots()
  const endIdx = daySlots.indexOf(INITIAL_GAME_TIME)
  const intradayTimes = endIdx >= 0 ? daySlots.slice(0, endIdx + 1) : daySlots
  const intradayPrices = generatePricesAlongTimes(intradayTimes, basePrice, code)

  if (intradayPrices.length >= POINT_COUNT) {
    return intradayPrices.slice(intradayPrices.length - POINT_COUNT)
  }

  const need = POINT_COUNT - intradayPrices.length
  const prefixTimes = buildSessionTimesEndingAt(intradayTimes[0], need + 1).slice(0, need)
  const prefixPrices = generatePricesAlongTimes(
    prefixTimes,
    Math.max(0.01, basePrice * (0.95 + getRng(code)() * 0.05)),
    `${code}_pre`,
  )
  return [...prefixPrices, ...intradayPrices].slice(-POINT_COUNT)
}

function initStockPrices() {
  const next = {}
  rngStore.clear()
  for (const list of Object.values(stockMap)) {
    for (const stock of list) {
      next[stock.code] = {
        name: stock.name,
        prices: generateInitialPrices(stock.basePrice, stock.code),
      }
    }
  }
  stockPrices.value = next
  gameTime.value = INITIAL_GAME_TIME
  marketClosed.value = false
}

function getLatestPrice(code) {
  const entry = stockPrices.value[code]
  if (!entry?.prices?.length) return 0
  return entry.prices[entry.prices.length - 1].price
}

function getChangePercent(code) {
  const entry = stockPrices.value[code]
  if (!entry?.prices || entry.prices.length < 2) return 0
  const last = entry.prices[entry.prices.length - 1].price
  const prev = entry.prices[entry.prices.length - 2].price
  if (prev === 0) return 0
  return +(((last - prev) / prev) * 100).toFixed(2)
}

function timeToChartX(time, left, amWidth, pmLeft, pmWidth) {
  const m = timeToMinutes(time)
  const amStart = timeToMinutes('09:30')
  const amEnd = timeToMinutes('11:30')
  const pmStart = timeToMinutes('13:00')
  const pmEnd = timeToMinutes('15:00')

  if (m <= amEnd) {
    const ratio = (m - amStart) / (amEnd - amStart)
    return left + amWidth * Math.min(1, Math.max(0, ratio))
  }
  const ratio = (m - pmStart) / (pmEnd - pmStart)
  return pmLeft + pmWidth * Math.min(1, Math.max(0, ratio))
}

function advanceGameTime() {
  const t = gameTime.value
  if (t === '14:55') {
    gameTime.value = '15:00'
    return
  }
  if (t === '11:25') {
    gameTime.value = '13:00'
    return
  }
  const m = timeToMinutes(t)
  if (m < timeToMinutes('11:25')) {
    gameTime.value = minutesToTime(m + 5)
    return
  }
  if (m >= timeToMinutes('13:00') && m < timeToMinutes('14:55')) {
    gameTime.value = minutesToTime(m + 5)
  }
}

function appendPriceTickForStock(code, entry, newTime) {
  const prices = entry.prices
  const last = prices[prices.length - 1]
  const rng = getRng(code)
  let newPrice

  if (newTime === '13:00' && timeToMinutes(last.time) <= timeToMinutes('11:30')) {
    const morningClose =
      [...prices].reverse().find((p) => p.time === '11:30')?.price ?? last.price
    newPrice = openGapPrice(morningClose, rng)
  } else {
    newPrice = fluctuate(last.price, 0.02, rng)
  }

  const nextPrices = [...prices, { time: newTime, price: newPrice }]
  if (nextPrices.length > POINT_COUNT) {
    nextPrices.shift()
  }
  entry.prices = nextPrices
}

function runGameTick() {
  if (marketClosed.value) return

  advanceGameTime()
  const newTime = gameTime.value

  for (const [code, entry] of Object.entries(stockPrices.value)) {
    appendPriceTickForStock(code, entry, newTime)
  }

  if (newTime === '15:00') {
    marketClosed.value = true
    stopGameTimer()
  }

  if (currentStock.value) {
    currentStock.value = {
      ...currentStock.value,
      price: getLatestPrice(currentStock.value.code),
    }
  }

  syncUserAssets()
  chartRevision.value += 1
}

function stopGameTimer() {
  if (gameTimer) {
    clearInterval(gameTimer)
    gameTimer = null
  }
}

function startGameTimer() {
  stopGameTimer()
  gameTimer = setInterval(runGameTick, 2000)
}

function updateTime() {
  const now = new Date()
  currentTime.value = `${now.getFullYear()}/${pad(now.getMonth() + 1)}/${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`
}

function formatMoney(val) {
  return Number(val).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function changeClass(val) {
  if (val > 0) return 'up'
  if (val < 0) return 'down'
  return 'flat'
}

function formatPercent(val) {
  if (val > 0) return `+${val.toFixed(2)}%`
  if (val < 0) return `${val.toFixed(2)}%`
  return '0.00%'
}

function formatDeltaAmount(val) {
  if (val > 0) return `+${val.toFixed(2)}`
  if (val < 0) return val.toFixed(2)
  return '0.00'
}

function syncUserAssets() {
  let holdingsValue = 0
  for (const h of holdings.value) {
    holdingsValue += h.quantity * getLatestPrice(h.code)
  }
  userAsset.value = +(userMoney.value + holdingsValue).toFixed(2)
  if (initialTotalAsset.value > 0) {
    assetChange.value = +(
      ((userAsset.value - initialTotalAsset.value) / initialTotalAsset.value) *
      100
    ).toFixed(2)
  }
}

function openStockDetail(stock) {
  currentStock.value = {
    code: stock.code,
    name: stock.name,
    price: getLatestPrice(stock.code),
  }
}

function backToStockList() {
  currentStock.value = null
  closeTradeModal()
}

function openTradeModal(type) {
  if (marketClosed.value) return
  tradeModal.value = type
  tradeQuantityInput.value = ''
  tradeError.value = ''
}

function closeTradeModal() {
  tradeModal.value = null
  tradeQuantityInput.value = ''
  tradeError.value = ''
}

function validateTradeInput() {
  const raw = tradeQuantityInput.value.trim()
  if (!raw) {
    tradeError.value = '股数不能为空'
    return null
  }
  if (!POSITIVE_INT_REG.test(raw)) {
    tradeError.value = '请输入正整数股数'
    return null
  }
  const qty = Number(raw)
  if (tradeModal.value === 'sell') {
    const holdQty = currentHolding.value?.quantity ?? 0
    if (qty > holdQty) {
      tradeError.value = '卖出数量不能超过当前持有数量'
      return null
    }
  }
  if (tradeModal.value === 'buy') {
    const amount = detailLatestPrice.value * qty
    if (amount > userMoney.value) {
      tradeError.value = '可用资金不足，无法完成买入'
      return null
    }
  }
  tradeError.value = ''
  return qty
}

function confirmTrade() {
  if (!currentStock.value || !tradeModal.value) return
  const qty = validateTradeInput()
  if (qty === null) return

  const price = getLatestPrice(currentStock.value.code)
  const amount = +(price * qty).toFixed(2)
  const { code, name } = currentStock.value

  if (tradeModal.value === 'buy') {
    userMoney.value = +(userMoney.value - amount).toFixed(2)
    const existing = holdings.value.find((h) => h.code === code)
    if (existing) {
      const totalQty = existing.quantity + qty
      existing.avgPrice = +(
        (existing.avgPrice * existing.quantity + price * qty) /
        totalQty
      ).toFixed(2)
      existing.quantity = totalQty
    } else {
      holdings.value.push({ code, name, quantity: qty, avgPrice: price })
    }
    console.log('[交易] 买入', { code, name, quantity: qty, price, amount, type: 'buy' })
    alert('买入成功')
  } else {
    userMoney.value = +(userMoney.value + amount).toFixed(2)
    const existing = holdings.value.find((h) => h.code === code)
    if (existing) {
      existing.quantity -= qty
      if (existing.quantity <= 0) {
        holdings.value = holdings.value.filter((h) => h.code !== code)
      }
    }
    console.log('[交易] 卖出', { code, name, quantity: qty, price, amount, type: 'sell' })
    alert('卖出成功')
  }

  syncUserAssets()
  closeTradeModal()
}

function nextTradingDay() {
  console.log('模拟推进到下一个交易日')
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/login')
}

function goHoldings() {
  router.push('/holdings')
}

function toggleNews() {
  showNews.value = !showNews.value
}

function selectSubPage(page) {
  currentSubPage.value = page
  if (page === 'stock') {
    showNews.value = false
  } else {
    currentStock.value = null
    closeTradeModal()
  }
}

function selectIndustry(key) {
  currentIndustry.value = key
  showNews.value = false
}

function loadUserInfo() {
  try {
    const raw = localStorage.getItem('userInfo')
    if (raw) {
      const info = JSON.parse(raw)
      username.value = info.username || info.account || info.name || '用户'
      return
    }
  } catch {
    /* ignore */
  }
  const account = localStorage.getItem('account')
  if (account) {
    username.value = account
  }
}

function bootstrapGameSession() {
  initStockPrices()
  syncUserAssets()
  initialTotalAsset.value = userAsset.value
  startGameTimer()
}

onMounted(() => {
  loadUserInfo()
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  bootstrapGameSession()
})

onUnmounted(() => {
  if (timeTimer) {
    clearInterval(timeTimer)
    timeTimer = null
  }
  stopGameTimer()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px 20px 24px;
  background: #f1f5f9;
  box-sizing: border-box;
}

.card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  gap: 16px;
  flex-wrap: wrap;
}

.top-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.label {
  font-size: 14px;
  color: #475569;
}

.game-time-label {
  margin-left: 8px;
}

.time-value {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.top-center {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 140px;
}

.top-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.btn-secondary {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  font-size: 14px;
  cursor: pointer;
}

.btn-secondary:hover {
  border-color: #42b883;
  color: #42b883;
}

.btn-outline {
  padding: 8px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
}

.btn-outline:hover {
  border-color: #fca5a5;
  color: #ef4444;
}

.btn-primary {
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  background: #42b883;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-primary:hover {
  background: #33a06f;
}

.btn-primary.active {
  background: #2d8a66;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.35);
}

.asset-bar {
  display: flex;
  align-items: stretch;
  gap: 20px;
  padding: 18px 20px;
  flex-wrap: wrap;
}

.asset-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 24px 32px;
  flex: 1;
  min-width: 240px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.asset-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  justify-content: center;
}

.early-holdings {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 160px;
  border-left: 1px solid #e2e8f0;
  padding-left: 20px;
}

.holding-chip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.chip-name {
  color: #475569;
}

.chip-change {
  font-weight: 600;
}

.up {
  color: #ef4444;
}

.down {
  color: #22c55e;
}

.flat {
  color: #94a3b8;
}

.main-body {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
  align-items: stretch;
}

.sub-nav {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 120px;
  flex-shrink: 0;
}

.sub-nav-btn {
  width: 100%;
  padding: 16px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  color: #475569;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
}

.sub-nav-btn:hover {
  border-color: #42b883;
  color: #42b883;
}

.sub-nav-btn.active {
  background: #42b883;
  border-color: #42b883;
  color: #fff;
}

.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.coming-soon {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
}

.coming-soon p {
  margin: 0;
  font-size: 36px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.08em;
}

.stock-panel {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 360px;
}

.industry-col {
  width: 140px;
  flex-shrink: 0;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.industry-btn {
  width: 100%;
  padding: 10px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  text-align: center;
  cursor: pointer;
}

.industry-btn:hover {
  border-color: #42b883;
  color: #42b883;
}

.industry-btn.active {
  background: #ecfdf5;
  border-color: #42b883;
  color: #15803d;
  font-weight: 600;
}

.b-area {
  flex: 1;
  min-width: 0;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
}

.panel-title {
  margin: 0 0 16px;
  font-size: 18px;
  color: #1e293b;
}

.back-btn {
  align-self: flex-start;
  margin-bottom: 8px;
  padding: 0;
  border: none;
  background: transparent;
  color: #42b883;
  font-size: 14px;
  cursor: pointer;
}

.back-btn:hover {
  color: #33a06f;
  text-decoration: underline;
}

.detail-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-code {
  font-size: 14px;
  color: #64748b;
  font-weight: 400;
}

.market-closed-tag {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f1f5f9;
}

.chart-wrap {
  width: 70%;
  max-width: 600px;
  height: 250px;
  margin: 0 auto 16px;
}

.price-chart {
  width: 100%;
  height: 100%;
  display: block;
}

.grid-line {
  stroke: #e2e8f0;
  stroke-width: 1;
}

.axis-line {
  stroke: #94a3b8;
  stroke-width: 1.2;
}

.lunch-gap {
  fill: #f8fafc;
}

.lunch-label {
  fill: #94a3b8;
  font-size: 12px;
}

.axis-label {
  fill: #64748b;
  font-size: 12px;
}

.axis-label-y {
  font-size: 12px;
}

.live-price-block {
  text-align: center;
  margin-bottom: 20px;
}

.live-price {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.live-price.closed {
  color: #94a3b8;
}

.live-delta {
  margin-top: 8px;
  display: flex;
  justify-content: center;
  gap: 16px;
  font-size: 15px;
  font-weight: 600;
}

.trade-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-buy {
  min-width: 120px;
  padding: 12px 28px;
  border: none;
  border-radius: 8px;
  background: #42b883;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.btn-buy:hover:not(:disabled) {
  background: #33a06f;
}

.btn-buy:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-sell {
  min-width: 120px;
  padding: 12px 28px;
  border: none;
  border-radius: 8px;
  background: #ef4444;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.btn-sell:hover:not(:disabled) {
  background: #dc2626;
}

.btn-sell:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.table-wrap {
  overflow: auto;
  flex: 1;
}

.stock-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.stock-table th,
.stock-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.stock-table th {
  color: #64748b;
  font-weight: 500;
  background: #f8fafc;
}

.stock-table tbody tr.stock-row {
  cursor: pointer;
}

.stock-table tbody tr.stock-row:hover {
  background: #f1f5f9;
}

.news-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: auto;
}

.news-item {
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.news-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.news-title {
  margin: 0 0 6px;
  font-size: 16px;
  color: #1e293b;
}

.news-time {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.news-summary {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.45);
}

.modal-card {
  width: 100%;
  max-width: 420px;
  padding: 24px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.2);
}

.modal-title {
  margin: 0 0 16px;
  font-size: 18px;
  color: #1e293b;
}

.modal-label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #475569;
}

.modal-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.modal-input:focus {
  border-color: #42b883;
}

.modal-error {
  margin: 8px 0 0;
  font-size: 13px;
  color: #ef4444;
}

.modal-hint {
  margin: 12px 0 0;
  font-size: 14px;
  color: #64748b;
}

.modal-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-modal-cancel {
  padding: 10px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
}

.btn-modal-cancel:hover {
  border-color: #cbd5e1;
}

.btn-modal-confirm {
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

@media (max-width: 900px) {
  .stock-panel {
    flex-direction: column;
  }

  .industry-col {
    width: 100%;
    max-height: none;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .industry-btn {
    width: calc(50% - 4px);
  }

  .early-holdings {
    width: 100%;
    border-left: none;
    padding-left: 0;
    border-top: 1px solid #e2e8f0;
    padding-top: 12px;
  }

  .chart-wrap {
    width: 100%;
  }

  .live-price {
    font-size: 26px;
  }
}
</style>
