<template>
  <div class="page">
    <header class="top-bar card">
      <div class="top-left">
        <span class="label">当前时间</span>
        <span class="time-value">{{ currentTime }}</span>
        <span class="label game-time-label">游戏日期</span>
        <span class="time-value">{{ gameDate }}</span>
        <span class="label game-time-label">游戏时间</span>
        <span class="time-value">{{ gameTime }}</span>
        <span v-if="isPaused" class="paused-hint">游戏已暂停</span>
        <span v-else-if="lunchBreak" class="lunch-hint">午盘休市，13:00开盘</span>
        <span v-else-if="marketClosed" class="closed-hint">已收盘</span>
      </div>
      <div class="top-center btn-group">
        <button type="button" class="btn-secondary" @click="togglePauseGame">
          {{ isPaused ? '继续游戏' : '暂停游戏' }}
        </button>
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
          <span class="stat-label">持仓市值</span>
          <span class="stat-value">¥ {{ formatMoney(holdMarketValue) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">用户资产</span>
          <span class="stat-value">¥ {{ formatMoney(userAsset) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">总盈亏</span>
          <span class="stat-value" :class="changeClass(totalProfit)">
            ¥ {{ formatMoney(totalProfit) }}
          </span>
        </div>
        <div class="stat-item">
          <span class="stat-label">资产涨跌幅(%)</span>
          <span class="stat-value" :class="changeClass(profitRate)">
            {{ formatProfitRate(profitRate) }}
          </span>
        </div>
      </div>
      <div class="asset-actions">
        <button
          type="button"
          class="btn-primary"
          :class="{ active: currentBView === 'broker' && currentSubPage === 'stock' }"
          @click="openBrokerView"
        >
          券商
        </button>
        <button
          type="button"
          class="btn-primary"
          :class="{ active: currentBView === 'operateLog' && currentSubPage === 'stock' }"
          @click="openOperateLogView"
        >
          操作日志
        </button>
        <button
          type="button"
          class="btn-primary"
          :class="{ active: currentBView === 'holdList' && currentSubPage === 'stock' }"
          @click="openHoldListView"
        >
          我的持仓
        </button>
        <button
          type="button"
          class="btn-primary"
          :class="{ active: currentBView === 'newsLog' && currentSubPage === 'stock' }"
          @click="openNewsLogView"
        >
          新闻
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
          <p v-if="isPaused" class="paused-banner">游戏已暂停</p>
          <p v-else-if="lunchBreak" class="lunch-banner">午盘休市，13:00开盘</p>
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
            <template v-if="currentBView === 'broker'">
              <button type="button" class="back-btn" @click="backToStockList">← 返回</button>
              <h2 class="panel-title">券商 · 融券做空</h2>

              <section class="broker-section">
                <h3 class="broker-subtitle">借券</h3>
                <div class="broker-borrow-row">
                  <label class="modal-label" for="brokerStockSelect">选择股票</label>
                  <select
                    id="brokerStockSelect"
                    v-model="brokerBorrowStockCode"
                    class="broker-select"
                    @change="onBrokerStockChange"
                  >
                    <option value="">请选择</option>
                    <option v-for="s in STOCK_CATALOG" :key="s.code" :value="s.code">
                      {{ s.name }} ({{ s.code }})
                    </option>
                  </select>
                </div>
                <p v-if="brokerBorrowStockCode" class="modal-hint">
                  券商剩余可借：{{ brokerCanBorrow === null ? '—' : brokerCanBorrow }} 股
                </p>
                <div class="broker-borrow-row">
                  <label class="modal-label" for="brokerBorrowQty">借券股数</label>
                  <input
                    id="brokerBorrowQty"
                    v-model="brokerBorrowQtyInput"
                    type="number"
                    min="1"
                    step="1"
                    class="modal-input"
                    placeholder="请输入借券股数"
                  />
                </div>
                <p v-if="brokerFormError" class="modal-error">{{ brokerFormError }}</p>
                <button type="button" class="btn-primary broker-action-btn" @click="submitBorrowStock">
                  借股票
                </button>
              </section>

              <section class="broker-section">
                <h3 class="broker-subtitle">我的做空持仓</h3>
                <p v-if="shortInfoLoading" class="hold-empty">加载中…</p>
                <p v-else-if="!shortInfoList.length" class="hold-empty">暂无融券负债</p>
                <div v-else class="hold-table-wrap">
                  <ElTable :data="shortInfoList" stripe class="hold-el-table broker-short-table">
                    <ElTableColumn label="股票代码" min-width="96">
                      <template #default="{ row }">
                        <button
                          type="button"
                          class="btn-link stock-code-link"
                          @click.stop="openStockDetailByCode(row.stockCode)"
                        >
                          {{ row.stockCode }}
                        </button>
                      </template>
                    </ElTableColumn>
                    <ElTableColumn prop="shortCount" label="借入总股数" min-width="100" align="right" />
                    <ElTableColumn prop="shortSoldCount" label="已卖出股数" min-width="100" align="right" />
                    <ElTableColumn label="借券基准价" min-width="100" align="right">
                      <template #default="{ row }">
                        {{ formatMoney(row.borrowPrice) }}
                      </template>
                    </ElTableColumn>
                    <ElTableColumn label="借券基准总额" min-width="120" align="right">
                      <template #default="{ row }">
                        {{ formatMoney(row.borrowBaseTotal) }}
                      </template>
                    </ElTableColumn>
                    <ElTableColumn label="操作" min-width="200" fixed="right">
                      <template #default="{ row }">
                        <button
                          type="button"
                          class="btn-link"
                          :disabled="!row.notSoldCount"
                          @click="openBrokerShortSellModal(row)"
                        >
                          融券卖出
                        </button>
                        <button
                          type="button"
                          class="btn-link btn-link-warn"
                          :disabled="!row.shortSoldCount"
                          @click="openBrokerReturnModal(row)"
                        >
                          归还平仓
                        </button>
                      </template>
                    </ElTableColumn>
                  </ElTable>
                </div>
              </section>
            </template>

            <template v-else-if="currentBView === 'operateLog'">
              <button type="button" class="back-btn" @click="backToStockList">← 返回</button>
              <h2 class="panel-title">操作日志</h2>
              <div class="news-log-panel">
                <p v-if="operateLogLoading" class="hold-empty news-log-loading">加载中…</p>
                <p v-else-if="!operateLogList.length" class="hold-empty">暂无操作记录</p>
                <div v-else class="hold-table-wrap">
                  <ElTable :data="operateLogList" stripe class="hold-el-table">
                    <ElTableColumn prop="gameDate" label="游戏日期" min-width="100" />
                    <ElTableColumn prop="gameTime" label="游戏时间" min-width="88" />
                    <ElTableColumn prop="operateType" label="操作类型" min-width="100" />
                    <ElTableColumn prop="stockCode" label="股票代码" min-width="96" />
                    <ElTableColumn label="资金流向" min-width="88" align="center">
                      <template #default="{ row }">
                        <span :class="fundDirectionClass(row.fundDirection)">
                          {{ fundDirectionLabel(row.fundDirection) }}
                        </span>
                      </template>
                    </ElTableColumn>
                    <ElTableColumn label="变动金额" min-width="100" align="right">
                      <template #default="{ row }">
                        {{ formatMoney(row.changeMoney) }}
                      </template>
                    </ElTableColumn>
                    <ElTableColumn label="操作后可用金钱" min-width="120" align="right">
                      <template #default="{ row }">
                        {{ formatMoney(row.afterMoney) }}
                      </template>
                    </ElTableColumn>
                    <ElTableColumn prop="remark" label="操作备注" min-width="200" show-overflow-tooltip />
                  </ElTable>
                </div>
              </div>
            </template>

            <template v-else-if="currentBView === 'newsLog'">
              <button type="button" class="back-btn" @click="backToStockList">← 返回</button>
              <h2 class="panel-title">突发事件新闻</h2>
              <div class="news-log-panel">
                <p v-if="newsLogLoading" class="hold-empty news-log-loading">加载中…</p>
                <p v-else-if="!newsLogList.length" class="hold-empty">
                  暂无突发事件新闻
                </p>
                <div v-else class="hold-table-wrap">
                  <ElTable :data="newsLogList" stripe class="hold-el-table">
                    <ElTableColumn prop="gameDate" label="游戏日期" min-width="110" />
                    <ElTableColumn prop="gameTime" label="游戏时间" min-width="90" />
                    <ElTableColumn prop="stockCode" label="股票代码" min-width="100" />
                    <ElTableColumn prop="eventTitle" label="事件标题" min-width="140" />
                    <ElTableColumn label="涨跌幅" min-width="100" align="right">
                      <template #default="{ row }">
                        <span :class="changeClass(Number(row.changeRate) * 100)">
                          {{ formatEventChangeRate(row.changeRate) }}
                        </span>
                      </template>
                    </ElTableColumn>
                  </ElTable>
                </div>
              </div>
            </template>

            <template v-else-if="currentBView === 'holdList'">
              <button type="button" class="back-btn" @click="backToStockList">← 返回</button>
              <h2 class="panel-title">我的持仓</h2>
              <p v-if="!holdTableData.length" class="hold-empty">暂无持仓，请前往买入股票</p>
              <div v-else class="hold-table-wrap">
                <ElTable
                  :data="holdTableData"
                  stripe
                  class="hold-el-table"
                  @row-click="onHoldRowClick"
                >
                  <ElTableColumn prop="stockName" label="股票名称" min-width="100" />
                  <ElTableColumn prop="stockCode" label="股票代码" min-width="90" />
                  <ElTableColumn prop="holdCount" label="持股数量" min-width="88" align="right" />
                  <ElTableColumn label="买入成本价（平均）" min-width="130" align="right">
                    <template #default="{ row }">
                      {{ formatMoney(row.buyCostPrice) }}
                    </template>
                  </ElTableColumn>
                  <ElTableColumn label="当前实时股价" min-width="110" align="right">
                    <template #default="{ row }">
                      {{ formatMoney(row.currentPrice) }}
                    </template>
                  </ElTableColumn>
                  <ElTableColumn label="持仓市值" min-width="100" align="right">
                    <template #default="{ row }">
                      {{ formatMoney(row.marketValue) }}
                    </template>
                  </ElTableColumn>
                  <ElTableColumn label="盈亏金额" min-width="100" align="right">
                    <template #default="{ row }">
                      <span :class="changeClass(Number(row.profitAmount))">
                        {{ formatSignedMoney(row.profitAmount) }}
                      </span>
                    </template>
                  </ElTableColumn>
                  <ElTableColumn label="盈亏百分比" min-width="100" align="right">
                    <template #default="{ row }">
                      <span :class="changeClass(Number(row.profitRate))">
                        {{ formatPercent(Number(row.profitRate)) }}
                      </span>
                    </template>
                  </ElTableColumn>
                </ElTable>
              </div>
            </template>

            <template v-else-if="currentBView === 'stockDetail' && currentStock">
              <button type="button" class="back-btn" @click="backToStockList">← 返回</button>
              <h2 class="panel-title detail-title">
                {{ currentStock.name }}
                <span class="detail-code">{{ currentStock.code }}</span>
                <span v-if="lunchBreak" class="market-lunch-tag">午盘休市</span>
                <span v-else-if="marketClosed" class="market-closed-tag">已收盘</span>
              </h2>

              <div class="chart-wrap">
                <svg
                  :key="'price-chart-' + chartRenderKey"
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
                  :disabled="marketClosed || lunchBreak"
                  @click="openTradeModal('buy')"
                >
                  买入
                </button>
                <button
                  v-if="currentHolding && currentHolding.quantity > 0"
                  type="button"
                  class="btn-sell"
                  :disabled="marketClosed || lunchBreak"
                  @click="openTradeModal('sell')"
                >
                  卖出
                </button>
              </div>
            </template>

            <template v-else-if="currentBView === 'stockList'">
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
          min="1"
          step="1"
          class="modal-input"
          :placeholder="tradeModal === 'buy' ? '请输入买入股数' : '请输入卖出股数'"
          @input="tradeError = ''"
          @keyup.enter="confirmTrade"
        />
        <p v-if="tradeError" class="modal-error">{{ tradeError }}</p>

        <template v-if="tradeModal === 'buy'">
          <p class="modal-hint">当前股价：¥ {{ modalUnitPrice.toFixed(2) }}</p>
          <p class="modal-hint">
            买入股数：{{ buyQuantityParsed > 0 ? buyQuantityParsed : '—' }}
          </p>
          <p class="modal-hint">
            成交总额：¥ {{ formatMoney(buyTradeTotal) }}
          </p>
          <p class="modal-hint">
            手续费（0.3%）：¥ {{ formatMoney(buyHandFee) }}
          </p>
          <p class="modal-hint modal-hint-strong">
            实际扣款：¥ {{ formatMoney(buyActualDebit) }}
          </p>
        </template>
        <template v-else-if="tradeModal === 'sell'">
          <p class="modal-hint">当前股价：¥ {{ modalUnitPrice.toFixed(2) }}</p>
          <p class="modal-hint">
            卖出股数：{{ buyQuantityParsed > 0 ? buyQuantityParsed : '—' }}
          </p>
          <p class="modal-hint">
            卖出成交总额：¥ {{ formatMoney(sellTradeTotal) }}
          </p>
          <p class="modal-hint">
            卖出手续费（0.3%）：¥ {{ formatMoney(sellHandFee) }}
          </p>
          <p class="modal-hint modal-hint-strong">
            实际到手金额：¥ {{ formatMoney(sellNetReceived) }}
          </p>
        </template>

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

        <template v-if="tradeModal === 'buy'">
          <button type="button" class="btn-leverage modal-leverage-btn" @click="toggleLeverageSlider">
            我要加杠杆🤬
          </button>
          <div v-show="showLeverageSlider" class="leverage-panel">
            <div class="leverage-slider-wrap">
              <span class="leverage-label">杠杆倍数：{{ leverageValue }}</span>
              <ElSlider v-model="leverageValue" :min="1" :max="100" />
            </div>
            <button type="button" class="btn-leverage-confirm" @click="confirmLeverage">
              确定
            </button>
          </div>
        </template>
      </div>
    </div>

    <div
      v-if="brokerShortSellModal"
      class="modal-overlay"
      @click.self="closeBrokerModals"
    >
      <div class="modal-card">
        <h3 class="modal-title">融券卖出 — {{ brokerShortSellModal.stockCode }}</h3>
        <p class="modal-hint">未卖出借券：{{ brokerShortSellModal.notSoldCount }} 股</p>
        <p class="modal-hint">当前股价：¥ {{ brokerModalUnitPrice.toFixed(2) }}</p>
        <label class="modal-label">卖出股数</label>
        <input v-model="brokerModalQtyInput" type="number" class="modal-input" min="1" step="1" />
        <p class="modal-hint">成交总额：¥ {{ formatMoney(brokerShortTradeTotal) }}</p>
        <p class="modal-hint">手续费（0.3%）：¥ {{ formatMoney(brokerShortHandFee) }}</p>
        <p class="modal-hint modal-hint-strong">实际到手：¥ {{ formatMoney(brokerShortNetReceived) }}</p>
        <p v-if="brokerModalError" class="modal-error">{{ brokerModalError }}</p>
        <div class="modal-actions">
          <button type="button" class="btn-modal-cancel" @click="closeBrokerModals">取消</button>
          <button type="button" class="btn-modal-confirm btn-sell" @click="confirmShortSell">
            确认卖出
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="brokerReturnModal"
      class="modal-overlay"
      @click.self="closeBrokerModals"
    >
      <div class="modal-card">
        <h3 class="modal-title">归还平仓 — {{ brokerReturnModal.stockCode }}</h3>
        <p class="modal-hint">已卖出可还：{{ brokerReturnModal.shortSoldCount }} 股</p>
        <p class="modal-hint">当前股价：¥ {{ brokerModalUnitPrice.toFixed(2) }}</p>
        <label class="modal-label">归还股数</label>
        <input v-model="brokerModalQtyInput" type="number" class="modal-input" min="1" step="1" />
        <p class="modal-hint">平仓总花费：¥ {{ formatMoney(brokerReturnTradeTotal) }}</p>
        <p class="modal-hint">手续费（0.3%）：¥ {{ formatMoney(brokerReturnHandFee) }}</p>
        <p class="modal-hint modal-hint-strong">合计扣款：¥ {{ formatMoney(brokerReturnActualDebit) }}</p>
        <p v-if="brokerModalError" class="modal-error">{{ brokerModalError }}</p>
        <div class="modal-actions">
          <button type="button" class="btn-modal-cancel" @click="closeBrokerModals">取消</button>
          <button type="button" class="btn-modal-confirm btn-buy" @click="confirmReturnStock">
            确认归还
          </button>
        </div>
      </div>
    </div>

    <ElDialog
      v-model="eventDialogVisible"
      :title="eventDialogTitle"
      width="520px"
      align-center
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="event-shock-dialog"
    >
      <p class="event-dialog-body">{{ eventDialogBody }}</p>
      <template #footer>
        <ElButton type="primary" @click="closeEventDialog">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import {
  ElSlider,
  ElMessage,
  ElTable,
  ElTableColumn,
  ElDialog,
  ElButton,
} from 'element-plus'
import 'element-plus/es/components/slider/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/table-column/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/button/style/css'

const router = useRouter()

const POSITIVE_INT_REG = /^[1-9]\d*$/
const BUY_HAND_FEE_RATE = 0.003
const POINT_COUNT = 50
const INITIAL_GAME_TIME = '09:30'
const INITIAL_GAME_DATE = '2020-01-02'
const API_BASE = 'http://localhost:8080'

/**
 * 前后端约定（A股交易时段，时间跳跃由前端控制）：
 * - 正常交易：09:30‑11:30，13:00‑15:00
 * - 接口返回 gameTime === "11:30" 时进入午休：停止 2 秒轮询，下次请求传 gameTime="13:00"
 * - 11:30~13:00 不重复传 11:30；由前端直传 13:00 跳过中午段，股价波动仍由后端接口执行
 */
const LUNCH_END_TIME = '13:00'
const MORNING_CLOSE_TIME = '11:30'

const currentTime = ref('')
const gameDate = ref('')
const gameTime = ref('')
const userId = ref(Number(localStorage.getItem('userId')) || 0)
const marketClosed = ref(false)
/** 午盘休市等待阶段（已收到 11:30，尚未完成 13:00 跳转请求） */
const lunchBreak = ref(false)
const isPaused = ref(false)
const stockPrices = ref({})
/** 折线图 5 分钟粒度股价点 { [stockCode]: { time, price }[] } */
const stockChartData = ref({})
const chartRevision = ref(0)
/** 强制重建 SVG，避免切换交易日后残留上一日折线 */
const chartRenderKey = ref(0)

const userMoney = ref(0)
const holdMarketValue = ref(0)
const userAsset = ref(0)
const totalProfit = ref(0)
const profitRate = ref(null)
const currentSubPage = ref('stock')
const currentIndustry = ref('daily')
/** B 区视图：stockList | stockDetail | holdList | newsLog | broker | operateLog */
const currentBView = ref('stockList')
const holdTableData = ref([])
/** 突发事件新闻列表，仅来自 GET /api/event/getUserRecentEvent */
const newsLogList = ref([])
const newsLogLoading = ref(false)
/** 操作日志，仅来自 GET /api/log/getOperateLog */
const operateLogList = ref([])
const operateLogLoading = ref(false)

const shortInfoList = ref([])
const shortInfoLoading = ref(false)
const brokerBorrowStockCode = ref('')
const brokerBorrowQtyInput = ref('')
const brokerCanBorrow = ref(null)
const brokerFormError = ref('')
const brokerShortSellModal = ref(null)
const brokerReturnModal = ref(null)
const brokerModalQtyInput = ref('')
const brokerModalError = ref('')

const eventDialogVisible = ref(false)
const eventDialogTitle = ref('重大突发新闻')
const eventDialogBody = ref('')
const username = ref('游客')
const currentStock = ref(null)
const tradeModal = ref(null)
const tradeQuantityInput = ref('')
const tradeError = ref('')
const showLeverageSlider = ref(false)
const leverageValue = ref(1)

const holdings = ref([])
const stocksByIndustry = ref({})

let timeTimer = null
let gameTimer = null
let lunchResumeTimer = null

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

const STOCK_CATALOG = [
  { name: '噢喵洗涤', code: '601001', industryKey: 'daily' },
  { name: '象印纸业', code: '601002', industryKey: 'daily' },
  { name: '哼哒地产', code: '602001', industryKey: 'realestate' },
  { name: '星连航空', code: '603001', industryKey: 'transport' },
  { name: '瞬风快递', code: '603002', industryKey: 'transport' },
  { name: '木原养殖', code: '604001', industryKey: 'agri' },
  { name: '光头林业', code: '604002', industryKey: 'agri' },
  { name: '手钢钢铁', code: '605001', industryKey: 'metallurgy' },
  { name: '善喜煤炭', code: '605002', industryKey: 'metallurgy' },
  { name: '钟石化石油', code: '605003', industryKey: 'petro' },
  { name: '钟石由石油', code: '605004', industryKey: 'petro' },
  { name: '妙妙电子', code: '606001', industryKey: 'it' },
  { name: '大米信息', code: '606002', industryKey: 'it' },
  { name: '保利工业', code: '607001', industryKey: 'machinery' },
  { name: '红漆机械', code: '607002', industryKey: 'machinery' },
  { name: '汪汪食品', code: '608001', industryKey: 'food' },
  { name: '基米玩具', code: '608002', industryKey: 'food' },
  { name: '康帅傅饮品', code: '608003', industryKey: 'food' },
  { name: '呦一裤服装', code: '609001', industryKey: 'textile' },
  { name: '风柱纺织', code: '609002', industryKey: 'textile' },
  { name: '杨光保险', code: '610001', industryKey: 'service' },
  { name: '专业律所', code: '610002', industryKey: 'service' },
  { name: '摩根银行', code: '610003', industryKey: 'service' },
  { name: '妙妙汽车', code: '611001', industryKey: 'auto' },
  { name: '大米汽车', code: '611002', industryKey: 'auto' },
  { name: '晨咣文具', code: '612001', industryKey: 'office' },
  { name: '德利文具', code: '612002', industryKey: 'office' },
  { name: '怡家家具', code: '613001', industryKey: 'furniture' },
  { name: '美白勺电器', code: '613002', industryKey: 'furniture' },
  { name: '嗨耳兄弟电器', code: '613003', industryKey: 'furniture' },
]

function buildFallbackStockMap() {
  const map = {}
  for (const item of STOCK_CATALOG) {
    if (!map[item.industryKey]) map[item.industryKey] = []
    map[item.industryKey].push({ name: item.name, code: item.code })
  }
  return map
}

const fallbackStockMap = buildFallbackStockMap()

const currentStocks = computed(() => {
  const list =
    stocksByIndustry.value[currentIndustry.value] ||
    fallbackStockMap[currentIndustry.value] ||
    []
  return list.map((s) => ({
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
  return stockChartData.value[currentStock.value.code] ?? []
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

function tradeQtyInputRaw() {
  return String(tradeQuantityInput.value ?? '').trim()
}

/** 详情页成交价：优先折线最新点，否则取行情缓存 */
function resolveDetailPrice() {
  const code = currentStock.value?.code
  if (!code) return 0
  if (detailLatestPrice.value > 0) return detailLatestPrice.value
  const cached = getLatestPrice(code)
  if (cached > 0) return cached
  return Number(currentStock.value?.price) || 0
}

const buyQuantityParsed = computed(() => {
  const raw = tradeQtyInputRaw()
  if (!POSITIVE_INT_REG.test(raw)) return 0
  return Number(raw)
})

const modalUnitPrice = computed(() => {
  chartRevision.value
  return resolveDetailPrice()
})

const brokerModalStockCode = computed(() =>
  brokerShortSellModal.value?.stockCode || brokerReturnModal.value?.stockCode || '',
)

const brokerModalUnitPrice = computed(() => {
  chartRevision.value
  const code = brokerModalStockCode.value
  if (!code) return 0
  const p = getLatestPrice(code)
  return p > 0 ? p : 0
})

const brokerModalQtyParsed = computed(() => {
  const raw = String(brokerModalQtyInput.value ?? '').trim()
  if (!POSITIVE_INT_REG.test(raw)) return 0
  return Number(raw)
})

const brokerShortTradeTotal = computed(() =>
  brokerModalUnitPrice.value * brokerModalQtyParsed.value,
)
const brokerShortHandFee = computed(() =>
  +(brokerShortTradeTotal.value * BUY_HAND_FEE_RATE).toFixed(2),
)
const brokerShortNetReceived = computed(() =>
  +(brokerShortTradeTotal.value - brokerShortHandFee.value).toFixed(2),
)

const brokerReturnTradeTotal = computed(() =>
  brokerModalUnitPrice.value * brokerModalQtyParsed.value,
)
const brokerReturnHandFee = computed(() =>
  +(brokerReturnTradeTotal.value * BUY_HAND_FEE_RATE).toFixed(2),
)
const brokerReturnActualDebit = computed(() =>
  +(brokerReturnTradeTotal.value + brokerReturnHandFee.value).toFixed(2),
)

/** 买入：成交总额（不含手续费） */
const buyTradeTotal = computed(() => {
  if (!currentStock.value || !buyQuantityParsed.value) return 0
  return resolveDetailPrice() * buyQuantityParsed.value
})

/** 买入：手续费 = 成交总额 × 0.3% */
const buyHandFee = computed(() =>
  +(buyTradeTotal.value * BUY_HAND_FEE_RATE).toFixed(2),
)

/** 买入：实际扣款 = 成交总额 + 手续费 */
const buyActualDebit = computed(() =>
  +(buyTradeTotal.value + buyHandFee.value).toFixed(2),
)

/** 卖出：成交总额 */
const sellTradeTotal = computed(() => {
  if (!currentStock.value || !buyQuantityParsed.value) return 0
  return resolveDetailPrice() * buyQuantityParsed.value
})

/** 卖出：手续费 = 成交总额 × 0.3% */
const sellHandFee = computed(() =>
  +(sellTradeTotal.value * BUY_HAND_FEE_RATE).toFixed(2),
)

/** 卖出：实际到手 = 成交总额 - 手续费 */
const sellNetReceived = computed(() =>
  +(sellTradeTotal.value - sellHandFee.value).toFixed(2),
)

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

function getLatestPrice(code) {
  const series = stockChartData.value[code]
  if (!series?.length) return 0
  return series[series.length - 1].price
}

function getChangePercent(code) {
  const series = stockChartData.value[code]
  if (!series || series.length < 2) return 0
  const last = series[series.length - 1].price
  const prev = series[series.length - 2].price
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

function applyBackendStockTick(stock, tickTime) {
  const code = stock.stockCode
  const price = Number(stock.lastTradePrice)
  if (!stockPrices.value[code]) {
    stockPrices.value[code] = { name: stock.stockName }
  } else {
    stockPrices.value[code].name = stock.stockName
  }

  const prevSeries = stockChartData.value[code] ?? []
  const lastPoint = prevSeries[prevSeries.length - 1]
  if (lastPoint && lastPoint.time === tickTime && lastPoint.price === price) {
    return
  }
  const nextPrices = [...prevSeries, { time: tickTime, price }]
  if (nextPrices.length > POINT_COUNT) {
    nextPrices.shift()
  }
  stockChartData.value = { ...stockChartData.value, [code]: nextPrices }
}

/** 新交易日：清空折线历史并重绘空图表（不影响持仓/资金） */
function resetStockChartForNewTradingDay() {
  stockChartData.value = {}
  chartRevision.value += 1
  chartRenderKey.value += 1
}

function sessionKey() {
  return `gameState_${userId.value}`
}

function loadSessionGameState() {
  try {
    const raw = sessionStorage.getItem(sessionKey())
    if (raw) {
      const saved = JSON.parse(raw)
      if (saved.gameDate) gameDate.value = saved.gameDate
      if (saved.gameTime) gameTime.value = saved.gameTime
      if (typeof saved.marketClosed === 'boolean') {
        marketClosed.value = saved.marketClosed
      }
      if (typeof saved.lunchBreak === 'boolean') {
        lunchBreak.value = saved.lunchBreak
      }
      return
    }
  } catch {
    /* ignore */
  }
  gameDate.value = INITIAL_GAME_DATE
  gameTime.value = INITIAL_GAME_TIME
  marketClosed.value = false
  lunchBreak.value = false
}

function persistSessionGameState() {
  sessionStorage.setItem(
    sessionKey(),
    JSON.stringify({
      gameDate: gameDate.value,
      gameTime: gameTime.value,
      marketClosed: marketClosed.value,
      lunchBreak: lunchBreak.value,
    }),
  )
}

function stopLunchResumeTimer() {
  if (lunchResumeTimer) {
    clearTimeout(lunchResumeTimer)
    lunchResumeTimer = null
  }
}

function scheduleLunchResumeCall() {
  stopLunchResumeTimer()
  if (isPaused.value) return
  lunchResumeTimer = setTimeout(() => {
    lunchResumeTimer = null
    refreshMarketData({ requestGameTime: LUNCH_END_TIME })
  }, 2000)
}

function resolveRequestGameTime(requestGameTime) {
  if (requestGameTime) return requestGameTime
  if (lunchBreak.value || gameTime.value === MORNING_CLOSE_TIME) {
    return LUNCH_END_TIME
  }
  return gameTime.value
}

async function loadUserStockCatalog() {
  if (!userId.value) return
  try {
    const res = await fetch(`${API_BASE}/api/stock/getUserAllStock?userId=${userId.value}`)
    const body = await res.json()
    if (body.code !== 200 || !Array.isArray(body.data)) return

    const codeToIndustry = Object.fromEntries(
      STOCK_CATALOG.map((item) => [item.code, item.industryKey]),
    )
    const map = {}
    for (const row of body.data) {
      const industryKey = codeToIndustry[row.stockCode] || 'daily'
      if (!map[industryKey]) map[industryKey] = []
      map[industryKey].push({ name: row.stockName, code: row.stockCode })
    }
    stocksByIndustry.value = map
  } catch (e) {
    console.error('加载用户股票列表失败', e)
  }
}

async function fetchMyAsset() {
  if (!userId.value) return
  try {
    const res = await fetch(`${API_BASE}/api/asset/getMyAsset?userId=${userId.value}`)
    const body = await res.json()
    if (body.code !== 200 || !body.data) return
    const d = body.data
    userMoney.value = Number(d.availableMoney)
    holdMarketValue.value = Number(d.holdMarketValue ?? 0)
    userAsset.value = Number(d.totalAsset)
    totalProfit.value = Number(d.totalProfit ?? 0)
    profitRate.value = d.profitRate === null || d.profitRate === undefined
      ? null
      : Number(d.profitRate)
    const list = Array.isArray(d.holdList) ? d.holdList : []
    holdTableData.value = list.map((row) => ({
      stockName: row.stockName,
      stockCode: row.stockCode,
      holdCount: row.holdCount,
      buyCostPrice: Number(row.buyCostPrice),
      currentPrice: Number(row.currentPrice),
      marketValue: Number(row.marketValue),
      profitAmount: Number(row.profitAmount),
      profitRate: Number(row.profitRate),
    }))
    holdings.value = holdTableData.value.map((h) => ({
      code: h.stockCode,
      name: h.stockName,
      quantity: h.holdCount,
      avgPrice: h.buyCostPrice,
    }))
  } catch (e) {
    console.error('加载资产失败', e)
  }
}

async function refreshMarketData(options = {}) {
  if (!userId.value || isPaused.value) return
  await Promise.all([
    fetchMyAsset(),
    callQueryAndUpdateStockPrice(options),
  ])
}

async function callQueryAndUpdateStockPrice(options = {}) {
  if (!userId.value || marketClosed.value) return

  const effectiveGameTime = resolveRequestGameTime(options.requestGameTime)

  try {
    const res = await fetch(`${API_BASE}/api/stock/queryAndUpdateStockPrice`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: userId.value,
        gameTime: effectiveGameTime,
        gameDate: gameDate.value,
      }),
    })
    const body = await res.json()
    if (body.code !== 200) {
      console.error(body.msg || '股价更新失败')
      return
    }

    const data = body.data
    gameTime.value = data.gameTime
    gameDate.value = data.gameDate
    marketClosed.value = !!data.isTradingEnd

    for (const s of data.stocks || []) {
      applyBackendStockTick(s, data.gameTime)
    }

    if (data.eventOccur) {
      pauseGameForRandomEvent()
      openRandomEventDialog(data)
      if (currentBView.value === 'newsLog') {
        void fetchUserRecentEvents()
      }
    }

    if (currentStock.value) {
      currentStock.value = {
        ...currentStock.value,
        price: getLatestPrice(currentStock.value.code),
      }
    }

    chartRevision.value += 1
    persistSessionGameState()

    if (data.isTradingEnd) {
      lunchBreak.value = false
      stopGameTimer()
      stopLunchResumeTimer()
      return
    }

    if (data.gameTime === MORNING_CLOSE_TIME) {
      lunchBreak.value = true
      stopGameTimer()
      scheduleLunchResumeCall()
      return
    }

    lunchBreak.value = false
    if (!gameTimer) {
      startGameTimer()
    }
  } catch (e) {
    console.error('无法连接后端股价接口', e)
  }
}

function stopGameTimer() {
  if (gameTimer) {
    clearInterval(gameTimer)
    gameTimer = null
  }
}

function startGameTimer() {
  stopGameTimer()
  if (marketClosed.value || lunchBreak.value || isPaused.value) return
  gameTimer = setInterval(() => {
    void refreshMarketData()
  }, 2000)
}

/** 随机事件触发：自动暂停并停止轮询（需用户手动点「继续游戏」恢复） */
function pauseGameForRandomEvent() {
  isPaused.value = true
  stopGameTimer()
  stopLunchResumeTimer()
}

function buildEventDialogBody(tick) {
  const code = tick.affectedStockCode || '—'
  const rate = Number(tick.changeRate)
  let impactLine = `受本次事件影响【${code}】股价变动`
  if (!Number.isNaN(rate)) {
    const pct = (rate * 100).toFixed(2)
    const sign = rate > 0 ? '+' : ''
    impactLine = `受本次事件影响【${code}】股价变动${sign}${pct}%`
  }
  const desc = tick.eventContent || ''
  return desc ? `${desc}\n\n${impactLine}` : impactLine
}

function openRandomEventDialog(tick) {
  eventDialogTitle.value = tick.eventTitle || '重大突发新闻'
  eventDialogBody.value = buildEventDialogBody(tick)
  eventDialogVisible.value = true
}

function closeEventDialog() {
  eventDialogVisible.value = false
}

function togglePauseGame() {
  isPaused.value = !isPaused.value
  if (isPaused.value) {
    stopGameTimer()
    stopLunchResumeTimer()
    return
  }
  if (marketClosed.value) return
  if (lunchBreak.value) {
    scheduleLunchResumeCall()
  } else {
    startGameTimer()
  }
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

function fundDirectionLabel(direction) {
  const d = Number(direction)
  if (d === 1) return '流入'
  if (d === -1) return '流出'
  return '无变动'
}

function fundDirectionClass(direction) {
  const d = Number(direction)
  if (d === 1) return 'fund-dir-in'
  if (d === -1) return 'fund-dir-out'
  return 'fund-dir-none'
}

function formatEventChangeRate(rate) {
  const n = Number(rate)
  if (Number.isNaN(n)) return '0.00%'
  const pct = n * 100
  if (pct > 0) return `+${pct.toFixed(2)}%`
  if (pct < 0) return `${pct.toFixed(2)}%`
  return '0.00%'
}

function formatPercent(val) {
  if (val > 0) return `+${val.toFixed(2)}%`
  if (val < 0) return `${val.toFixed(2)}%`
  return '0.00%'
}

function formatProfitRate(val) {
  if (val === null || val === undefined || Number.isNaN(val)) {
    return '—'
  }
  return formatPercent(val)
}

function formatSignedMoney(val) {
  const n = Number(val)
  if (Number.isNaN(n)) return '0.00'
  if (n > 0) return `+${formatMoney(n)}`
  if (n < 0) return `-${formatMoney(Math.abs(n))}`
  return formatMoney(0)
}

function formatDeltaAmount(val) {
  if (val > 0) return `+${val.toFixed(2)}`
  if (val < 0) return val.toFixed(2)
  return '0.00'
}

function openStockDetail(stock) {
  currentBView.value = 'stockDetail'
  currentStock.value = {
    code: stock.code,
    name: stock.name,
    price: getLatestPrice(stock.code),
  }
}

function onHoldRowClick(row) {
  openStockDetail({
    code: row.stockCode,
    name: row.stockName,
  })
}

function openStockDetailByCode(code) {
  const trimmed = String(code ?? '').trim()
  if (!trimmed) return
  const catalog = STOCK_CATALOG.find((s) => s.code === trimmed)
  const name = stockPrices.value[trimmed]?.name || catalog?.name || trimmed
  openStockDetail({ code: trimmed, name })
}

function openHoldListView() {
  if (currentSubPage.value !== 'stock') {
    currentSubPage.value = 'stock'
  }
  currentBView.value = 'holdList'
  currentStock.value = null
  closeTradeModal()
}

/** 从后端拉取用户最近随机事件（Redis 优先，否则 MySQL stock_event） */
async function fetchUserRecentEvents() {
  if (!userId.value) {
    newsLogList.value = []
    return
  }
  newsLogLoading.value = true
  try {
    const res = await fetch(`${API_BASE}/api/event/getUserRecentEvent?userId=${userId.value}`)
    const body = await res.json()
    if (body.code !== 200 || !Array.isArray(body.data)) {
      newsLogList.value = []
      return
    }
    newsLogList.value = body.data
  } catch (e) {
    console.error('加载突发事件新闻失败', e)
    newsLogList.value = []
  } finally {
    newsLogLoading.value = false
  }
}

async function fetchCanBorrowCount(stockCode) {
  if (!stockCode) {
    brokerCanBorrow.value = null
    return
  }
  try {
    const res = await fetch(
      `${API_BASE}/api/broker/getCanBorrow?stockCode=${encodeURIComponent(stockCode)}`,
    )
    const body = await res.json()
    brokerCanBorrow.value = body.code === 200 ? Number(body.data) : null
  } catch {
    brokerCanBorrow.value = null
  }
}

function onBrokerStockChange() {
  brokerFormError.value = ''
  void fetchCanBorrowCount(brokerBorrowStockCode.value)
}

async function fetchUserShortInfo() {
  if (!userId.value) {
    shortInfoList.value = []
    return
  }
  shortInfoLoading.value = true
  try {
    const res = await fetch(`${API_BASE}/api/broker/getUserShortInfo?userId=${userId.value}`)
    const body = await res.json()
    if (body.code !== 200 || !Array.isArray(body.data)) {
      shortInfoList.value = []
      return
    }
    shortInfoList.value = body.data
  } catch (e) {
    console.error('加载做空持仓失败', e)
    shortInfoList.value = []
  } finally {
    shortInfoLoading.value = false
  }
}

async function refreshBrokerPage() {
  await Promise.all([
    fetchUserShortInfo(),
    fetchCanBorrowCount(brokerBorrowStockCode.value),
  ])
}

function openBrokerView() {
  if (currentSubPage.value !== 'stock') {
    currentSubPage.value = 'stock'
  }
  currentBView.value = 'broker'
  currentStock.value = null
  closeTradeModal()
  closeBrokerModals()
  void refreshBrokerPage()
}

function parsePositiveQty(raw) {
  const s = String(raw ?? '').trim()
  if (!POSITIVE_INT_REG.test(s)) return null
  return Number(s)
}

async function submitBorrowStock() {
  brokerFormError.value = ''
  const qty = parsePositiveQty(brokerBorrowQtyInput.value)
  if (!brokerBorrowStockCode.value) {
    brokerFormError.value = '请选择股票'
    return
  }
  if (qty === null) {
    brokerFormError.value = '请输入正整数借券股数'
    return
  }
  if (brokerCanBorrow.value !== null && qty > brokerCanBorrow.value) {
    brokerFormError.value = '借券股数不能超过券商可借库存'
    return
  }
  try {
    const { data: body } = await axios.post(`${API_BASE}/api/broker/borrowStock`, {
      userId: userId.value,
      stockCode: brokerBorrowStockCode.value,
      borrowCount: qty,
    })
    if (body.code !== 200) {
      brokerFormError.value = body.msg || '借券失败'
      return
    }
    brokerBorrowQtyInput.value = ''
    ElMessage.success('借券成功')
    await refreshBrokerPage()
  } catch (err) {
    brokerFormError.value = err?.response?.data?.msg || '借券请求失败'
  }
}

function openBrokerShortSellModal(row) {
  brokerReturnModal.value = null
  brokerShortSellModal.value = row
  brokerModalQtyInput.value = ''
  brokerModalError.value = ''
}

function openBrokerReturnModal(row) {
  brokerShortSellModal.value = null
  brokerReturnModal.value = row
  brokerModalQtyInput.value = ''
  brokerModalError.value = ''
}

function closeBrokerModals() {
  brokerShortSellModal.value = null
  brokerReturnModal.value = null
  brokerModalQtyInput.value = ''
  brokerModalError.value = ''
}

async function confirmShortSell() {
  if (!brokerShortSellModal.value) return
  const qty = parsePositiveQty(brokerModalQtyInput.value)
  if (qty === null) {
    brokerModalError.value = '请输入正整数股数'
    return
  }
  if (qty > brokerShortSellModal.value.notSoldCount) {
    brokerModalError.value = '不能超过未卖出融券股数'
    return
  }
  const code = brokerShortSellModal.value.stockCode
  try {
    const { data: body } = await axios.post(`${API_BASE}/api/broker/shortSell`, {
      userId: userId.value,
      stockCode: code,
      sellCount: qty,
    })
    if (body.code !== 200) {
      brokerModalError.value = body.msg || '融券卖出失败'
      return
    }
    ElMessage.success('融券卖出成功')
    closeBrokerModals()
    await refreshBrokerPage()
    await fetchMyAsset()
  } catch (err) {
    brokerModalError.value = err?.response?.data?.msg || '请求失败'
  }
}

async function confirmReturnStock() {
  if (!brokerReturnModal.value) return
  const qty = parsePositiveQty(brokerModalQtyInput.value)
  if (qty === null) {
    brokerModalError.value = '请输入正整数股数'
    return
  }
  if (qty > brokerReturnModal.value.shortSoldCount) {
    brokerModalError.value = '不能超过已卖出融券股数'
    return
  }
  const code = brokerReturnModal.value.stockCode
  try {
    const { data: body } = await axios.post(`${API_BASE}/api/broker/returnStock`, {
      userId: userId.value,
      stockCode: code,
      returnCount: qty,
    })
    if (body.code !== 200) {
      brokerModalError.value = body.msg || '归还平仓失败'
      return
    }
    ElMessage.success('归还平仓成功')
    closeBrokerModals()
    await refreshBrokerPage()
    await fetchMyAsset()
  } catch (err) {
    brokerModalError.value = err?.response?.data?.msg || '请求失败'
  }
}

function openNewsLogView() {
  if (currentSubPage.value !== 'stock') {
    currentSubPage.value = 'stock'
  }
  currentBView.value = 'newsLog'
  currentStock.value = null
  closeTradeModal()
  newsLogList.value = []
  void fetchUserRecentEvents()
}

async function fetchOperateLogs() {
  if (!userId.value) {
    operateLogList.value = []
    return
  }
  operateLogLoading.value = true
  try {
    const res = await fetch(`${API_BASE}/api/log/getOperateLog?userId=${userId.value}`)
    const body = await res.json()
    if (body.code !== 200 || !Array.isArray(body.data)) {
      operateLogList.value = []
      return
    }
    operateLogList.value = body.data
  } catch (e) {
    console.error('加载操作日志失败', e)
    operateLogList.value = []
  } finally {
    operateLogLoading.value = false
  }
}

function openOperateLogView() {
  if (currentSubPage.value !== 'stock') {
    currentSubPage.value = 'stock'
  }
  currentBView.value = 'operateLog'
  currentStock.value = null
  closeTradeModal()
  operateLogList.value = []
  void fetchOperateLogs()
}

function backToStockList() {
  currentBView.value = 'stockList'
  currentStock.value = null
  closeTradeModal()
}

function toggleLeverageSlider() {
  showLeverageSlider.value = !showLeverageSlider.value
}

function confirmLeverage() {
  ElMessage.info('目前杠杆暂未开放')
}

function openTradeModal(type) {
  if (marketClosed.value || lunchBreak.value) return
  tradeModal.value = type
  tradeQuantityInput.value = ''
  tradeError.value = ''
}

function closeTradeModal() {
  tradeModal.value = null
  tradeQuantityInput.value = ''
  tradeError.value = ''
  showLeverageSlider.value = false
  leverageValue.value = 1
}

function validateTradeInput() {
  const raw = tradeQtyInputRaw()
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
      tradeError.value = '持股数量不足，无法卖出'
      return null
    }
  }
  if (tradeModal.value === 'buy') {
    const unitPrice = resolveDetailPrice()
    if (!unitPrice || unitPrice <= 0) {
      tradeError.value = '暂无有效股价，请等待行情更新'
      return null
    }
    const tradeTotal = unitPrice * qty
    const handFee = +(tradeTotal * BUY_HAND_FEE_RATE).toFixed(2)
    const actualDebit = +(tradeTotal + handFee).toFixed(2)
    if (actualDebit > userMoney.value) {
      tradeError.value = '可用资金不足，无法完成买入'
      return null
    }
  }
  tradeError.value = ''
  return qty
}

async function confirmTrade() {
  if (!currentStock.value || !tradeModal.value) return
  if (!userId.value) {
    tradeError.value = '请先登录'
    return
  }
  const qty = validateTradeInput()
  if (qty === null) return

  const price = resolveDetailPrice()
  if (!price || price <= 0) {
    tradeError.value = '暂无有效股价，请等待行情更新'
    return
  }

  const { code } = currentStock.value

  try {
    if (tradeModal.value === 'buy') {
      const { data: body } = await axios.post(`${API_BASE}/api/stock/buyStock`, {
        userId: userId.value,
        stockCode: code,
        buyCount: qty,
        handFeeRate: BUY_HAND_FEE_RATE,
      })
      if (body.code !== 200) {
        tradeError.value = body.msg || '买入失败'
        return
      }
      ElMessage.success('买入成功')
      closeTradeModal()
      await fetchMyAsset()
      return
    }

    const { data: body } = await axios.post(`${API_BASE}/api/stock/sellStock`, {
      userId: userId.value,
      stockCode: code,
      sellCount: qty,
    })
    if (body.code !== 200) {
      tradeError.value = body.msg || '卖出失败'
      return
    }
    ElMessage.success('卖出成功')
    closeTradeModal()
    await fetchMyAsset()
  } catch (err) {
    const backendMsg = err?.response?.data?.msg
    tradeError.value = backendMsg || '无法连接后端，请确认 Spring Boot 已启动'
  }
}

async function nextTradingDay() {
  if (!userId.value) {
    alert('请先登录')
    return
  }
  try {
    const res = await fetch(`${API_BASE}/api/game/nextTradeDay`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: userId.value,
        currentGameDate: gameDate.value,
      }),
    })
    const body = await res.json()
    if (body.code !== 200) {
      alert(body.msg || '切换交易日失败')
      return
    }
    isPaused.value = false
    gameDate.value = body.data.gameDate
    gameTime.value = body.data.gameTime
    marketClosed.value = false
    lunchBreak.value = false
    stopLunchResumeTimer()
    resetStockChartForNewTradingDay()
    persistSessionGameState()
    await refreshMarketData()
    startGameTimer()
  } catch {
    alert('无法连接后端，请确认 Spring Boot 已启动')
  }
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/login')
}

function selectSubPage(page) {
  currentSubPage.value = page
  if (page === 'stock') {
    /* keep current B view */
  } else {
    currentBView.value = 'stockList'
    currentStock.value = null
    closeTradeModal()
  }
}

function selectIndustry(key) {
  currentIndustry.value = key
  if (currentBView.value !== 'stockDetail') {
    currentBView.value = 'stockList'
  }
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

async function bootstrapGameSession() {
  if (!userId.value) {
    alert('请先登录')
    router.push('/login')
    return
  }
  loadSessionGameState()
  stockPrices.value = {}
  stockChartData.value = {}
  chartRenderKey.value += 1
  await loadUserStockCatalog()
  await fetchMyAsset()

  if (lunchBreak.value || gameTime.value === MORNING_CLOSE_TIME) {
    lunchBreak.value = true
    await refreshMarketData({ requestGameTime: LUNCH_END_TIME })
  } else {
    await refreshMarketData()
  }

  if (!marketClosed.value && !lunchBreak.value) {
    startGameTimer()
  } else if (lunchBreak.value) {
    scheduleLunchResumeCall()
  }
}

onMounted(() => {
  loadUserInfo()
  username.value = localStorage.getItem('username') || username.value

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
  stopLunchResumeTimer()
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

.closed-hint {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  padding: 2px 10px;
  border-radius: 6px;
  background: #f1f5f9;
}

.paused-hint {
  font-size: 13px;
  font-weight: 700;
  color: #1d4ed8;
  padding: 4px 12px;
  border-radius: 6px;
  background: #dbeafe;
  border: 1px solid #93c5fd;
}

.lunch-hint {
  font-size: 13px;
  font-weight: 700;
  color: #c2410c;
  padding: 4px 12px;
  border-radius: 6px;
  background: #ffedd5;
  border: 1px solid #fdba74;
}

.paused-banner {
  flex: 1 1 100%;
  width: 100%;
  margin: 0;
  padding: 12px 16px;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: #1d4ed8;
  background: #dbeafe;
  border: 1px solid #93c5fd;
  border-radius: 10px;
  box-sizing: border-box;
}

.lunch-banner {
  flex: 1 1 100%;
  width: 100%;
  margin: 0;
  padding: 12px 16px;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: #c2410c;
  background: #ffedd5;
  border: 1px solid #fdba74;
  border-radius: 10px;
  box-sizing: border-box;
}

.market-lunch-tag {
  font-size: 13px;
  color: #c2410c;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  background: #ffedd5;
}

.top-center {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 140px;
}

.btn-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 10px;
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

.fund-dir-in {
  color: #22c55e;
  font-weight: 600;
}

.fund-dir-out {
  color: #ef4444;
  font-weight: 600;
}

.fund-dir-none {
  color: #94a3b8;
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
  flex-wrap: wrap;
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

.btn-leverage {
  min-width: 120px;
  padding: 12px 20px;
  border: 1px solid #f59e0b;
  border-radius: 8px;
  background: #fffbeb;
  color: #b45309;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.btn-leverage:hover {
  background: #fef3c7;
}

.modal-leverage-btn {
  width: 100%;
  margin-top: 14px;
}

.leverage-panel {
  max-width: 100%;
  margin: 12px 0 0;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.leverage-slider-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.leverage-label {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}

.btn-leverage-confirm {
  margin-top: 12px;
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: #42b883;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-leverage-confirm:hover {
  background: #33a06f;
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

.broker-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.broker-section:first-of-type {
  border-top: none;
  margin-top: 8px;
  padding-top: 0;
}

.broker-subtitle {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}

.broker-borrow-row {
  margin-bottom: 10px;
}

.broker-select {
  width: 100%;
  max-width: 360px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
}

.broker-action-btn {
  margin-top: 8px;
}

.btn-link {
  margin-right: 10px;
  padding: 0;
  border: none;
  background: none;
  color: #2563eb;
  font-size: 13px;
  cursor: pointer;
}

.btn-link:disabled {
  color: #94a3b8;
  cursor: not-allowed;
}

.btn-link-warn {
  color: #dc2626;
}

.news-log-panel {
  min-height: 120px;
}

.news-log-loading {
  color: #64748b;
}

.hold-empty {
  margin: 48px 0;
  text-align: center;
  font-size: 16px;
  color: #64748b;
}

.hold-table-wrap {
  flex: 1;
  overflow: auto;
  margin-top: 8px;
}

.hold-el-table {
  width: 100%;
  font-size: 13px;
}

.hold-el-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
}

.hold-el-table :deep(.el-table__row) {
  cursor: pointer;
}

.hold-el-table :deep(.el-table__row:hover) {
  background: #f1f5f9 !important;
}

.broker-short-table :deep(.el-table__row) {
  cursor: default;
}

.broker-short-table :deep(.el-table__row:hover) {
  background: inherit !important;
}

.stock-code-link {
  font-weight: 600;
  padding: 0;
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

.modal-hint-strong {
  font-weight: 600;
  color: #0f172a;
}

.event-dialog-body {
  margin: 0;
  font-size: 15px;
  line-height: 1.65;
  color: #334155;
  white-space: pre-line;
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
