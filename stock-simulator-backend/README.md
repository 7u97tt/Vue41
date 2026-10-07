# 炒股模拟器后端

Spring Boot 3 + MyBatis + MySQL + Redis + JDK 17，对接 Vue3 前端。

## 目录结构

```
stock-simulator-backend/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/stocksim/
    │   ├── StockSimulatorApplication.java
    │   ├── common/          # Result、异常处理
    │   ├── config/          # CORS、Redis、拦截器
    │   ├── context/         # 当前登录用户
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── interceptor/     # Token 校验
    │   ├── mapper/
    │   ├── service/
    │   ├── util/
    │   └── vo/
    └── resources/
        ├── application.yml
        ├── mapper/*.xml
        └── sql/
            ├── schema.sql
            └── data.sql
```

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Redis 6+（已配置连接；未启动时 Spring Boot 可能启动失败，请先启动 Redis 或临时注释 `spring-boot-starter-data-redis`）

## 初始化数据库

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
mysql -u root -p < src/main/resources/sql/data.sql
```

修改 `src/main/resources/application.yml` 中的 MySQL / Redis 账号密码。

## 运行

```bash
cd stock-simulator-backend
mvn spring-boot:run
```

服务默认：`http://localhost:8080`

## API 说明

统一响应：`{ "code": 200, "msg": "success", "data": ... }`

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 用户 | POST | `/api/user/register` | 注册 |
| 用户 | POST | `/api/user/login` | 登录，返回 `token` |
| 用户 | GET | `/api/user/info` | 需 Header `token` |
| 用户 | POST | `/api/user/logout` | 需 Header `token` |
| 持仓 | GET | `/api/hold/list` | 需 token |
| 持仓 | POST | `/api/hold/buy` | body: stockCode, quantity, price |
| 持仓 | POST | `/api/hold/sell` | 同上 |
| 股票 | GET | `/api/stock/industries` | 行业列表 |
| 股票 | GET | `/api/stock/list?industry=白酒` | 分类下股票 |
| 股票 | GET | `/api/stock/history/{code}` | 历史价格数组 |
| 新闻 | GET | `/api/news/list` | 全部新闻 |

前端开发地址（Vite 默认）已通过 CORS 放行 `localhost` / `127.0.0.1` 任意端口。

## Vue 对接示例

```javascript
// 登录后保存 token
const res = await fetch('http://localhost:8080/api/user/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
})
const { data } = await res.json()
localStorage.setItem('token', data.token)

// 带 token 请求
fetch('http://localhost:8080/api/user/info', {
  headers: { token: localStorage.getItem('token') }
})
```

买卖时 `price` 传前端模拟的当前价；后端不做股价定时更新。
