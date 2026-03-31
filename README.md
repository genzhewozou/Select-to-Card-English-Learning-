# 划词成卡 · Select-to-Card

> 在文档中**划词 / 选句**即可生成学习卡片，支持 AI 释义、艾宾浩斯复习与错题本。

前后端分离的英语学习系统：上传 Word/TXT 文档，在正文中选中单词或句子一键生成闪卡，可选用 AI 自动生成释义与例句，按熟练度与艾宾浩斯曲线安排复习，并支持文档内高亮已生成卡片、卡片筛选与错题本。

---

## ✨ 功能特性

- **划词成卡**：在文档正文中选中文字即可弹出「生成学习卡片」，正面为选中内容，背面可手填或由 AI 生成
- **结构化释义（可选）**：编辑卡片时可一键调用 AI（内置 JSON schema）生成「义项-例句-同义词-扩展块」，并自动汇总到背面
- **文档内高亮**：正文中已生成卡片的词句会高亮显示，点击可跳转编辑；卡片编辑/列表页支持「在文档中定位」
- **艾宾浩斯复习**：「学习复习」页按下次复习时间展示今日待复习卡片，提交熟练度（1–5）后自动计算下次复习时间
- **错题本**：熟练度 1–2 的卡片单独列表，支持「只复习错题」
- **文档测验（Quiz）**：按文档随机抽取例句出题，作答结果会话入库，支持错题再练
- **卡片筛选与搜索**：按文档、关键词（正面/背面）、熟练度、今日待复习筛选
- **文档管理**：上传 Word(.doc/.docx)、TXT，列表与查看正文

---

## 🛠 技术栈

| 端 | 技术 |
|----|------|
| 后端 | Spring Boot 2.7、Spring Data JPA、MySQL / TiDB、Apache POI（文档解析） |
| 前端 | React 18、TypeScript、Vite、Ant Design 5、React Router 6、Axios |
| 规范 | REST API、统一 JSON 返回（code/message/data） |

---

## 📋 前置要求

- **JDK 8+**（后端）
- **Node.js 18+**、**npm**（前端）
- **MySQL 5.7+** 或 **TiDB**（需先创建数据库，如 `github_sample`）

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone <your-repo-url>
cd english-learning-system
```

### 2. 配置数据库

编辑 `backend/src/main/resources/application.yml`，修改数据源：

```yaml
spring:
  datasource:
    url: jdbc:mysql://<host>:<port>/<database>?useSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: <your-username>
    password: <your-password>
```

- 使用 TiDB 时注意端口（如 4000）与 SSL 参数。
- `serverTimezone=Asia/Shanghai` 建议保留，与「今日复习」逻辑一致。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

- 服务地址：`http://localhost:8080`
- 接口前缀：`/api`，即根路径为 `http://localhost:8080/api`
- JPA 默认 `ddl-auto: update`，首次启动会自动建表

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 浏览器访问：`http://localhost:3000`
- Vite 已配置开发代理，将 `/api` 转发到后端

### 5. 使用说明

- 未登录会跳转登录页；可先注册再登录。
- 后端通过请求头 `X-User-Id` 识别用户（前端登录后会自动携带）；生产环境建议改为 JWT 或 Session。

---

## ⚙️ 配置说明

### 数据库

| 配置项 | 说明 |
|--------|------|
| `spring.datasource.url` | JDBC 地址，建议带 `serverTimezone=Asia/Shanghai` |
| `spring.datasource.username` / `password` | 数据库账号密码 |
| `spring.jpa.hibernate.ddl-auto` | 默认 `update`，表结构随实体自动更新 |

### 可选：开发环境

- 使用 `application-dev.yml` 时在启动参数中加：`--spring.profiles.active=dev`
- 可覆盖数据源、开启 SQL 日志等

### AI 注释（可选）

- 在前端「设置」页填写 API Key、模型名、接口地址（如 OpenAI 或兼容接口）。
- 配置仅保存在浏览器本地，后端不存储；在文档页开启「AI 注释」后，生成卡片弹窗会自动请求生成释义与例句。

### 文件上传

- 默认最大 20MB：`spring.servlet.multipart.max-file-size`、`max-request-size` 可在 `application.yml` 中修改。

---

## 📁 项目结构

```
english-learning-system/
├── backend/                    # Spring Boot 后端
│   └── src/main/
│       ├── java/com/english/learn/
│       │   ├── common/         # 统一返回 Result
│       │   ├── config/         # Web、CORS、AI RestTemplate
│       │   ├── controller/     # REST 控制器
│       │   ├── dto/            # 请求/响应 DTO
│       │   ├── entity/         # JPA 实体
│       │   ├── exception/      # 全局异常处理
│       │   ├── mapper/         # Entity-DTO 转换
│       │   ├── repository/     # JPA Repository
│       │   ├── service/        # 业务逻辑
│       │   └── util/           # 文档解析、艾宾浩斯
│       └── resources/
│           ├── application.yml
│           └── application-dev.yml
└── frontend/                   # React 前端
    └── src/
        ├── components/         # 布局、导航
        ├── pages/              # 登录、文档、卡片、复习、错题本、设置
        ├── services/           # API 封装
        ├── types/              # TypeScript 类型
        └── utils/              # axios、AI 配置存储等
```

---

## 📡 API 概览

统一返回格式：

```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

### 用户

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 注册 |
| POST | `/api/user/login` | 登录 |
| GET  | `/api/user/{id}` | 用户详情 |
| PUT  | `/api/user/{id}` | 更新用户 |

### 文档

| 方法 | 路径 | 说明 |
|------|------|------|
| POST   | `/api/document/upload` | 上传文档（multipart/file） |
| GET    | `/api/document/list` | 文档列表 |
| GET    | `/api/document/{id}` | 文档详情（含 content） |
| DELETE | `/api/document/{id}` | 删除文档 |

### 卡片

| 方法 | 路径 | 说明 |
|------|------|------|
| POST   | `/api/card` | 创建卡片 |
| GET    | `/api/card/list` | 卡片列表，支持 `documentId`、`keyword`、`proficiencyMax`、`dueToday` |
| GET    | `/api/card/{id}` | 卡片详情 |
| PUT    | `/api/card/{id}` | 更新卡片 |
| DELETE | `/api/card/{id}` | 删除卡片 |

### 结构化卡片内容

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/card/{id}/structured/generate` | 调 AI 生成结构化释义并落库 |
| PUT  | `/api/card/{id}/structured` | 手动保存结构化义项树（全量覆盖） |

### 复习

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/api/review/today` | 今日待复习卡片（next_review_at ≤ 当前时间 + 从未复习） |
| GET  | `/api/review/weak` | 错题本（熟练度 1–2） |
| POST | `/api/review/submit` | 提交复习（cardId, proficiencyLevel 1–5） |

### 文档测验（Quiz）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/quiz/session` | 创建测验会话（按文档随机抽题） |
| POST | `/api/quiz/session/{sessionId}/answer` | 提交单题答案 |
| GET  | `/api/quiz/session/{sessionId}` | 获取会话题目（用于恢复进行中的测验） |
| GET  | `/api/quiz/session/{sessionId}/result` | 查看会话结果 |
| GET  | `/api/quiz/session/list` | 最近会话列表（用于回看历史） |
| POST | `/api/quiz/session/{sessionId}/retry-wrong` | 用错题创建新会话 |

### 复习间隔计算规则（2026-03 更新）

- 实现位置：`backend/src/main/java/com/english/learn/util/EbbinghausUtil.java`
- 基础间隔（按熟练度 1-5，单位分钟）：
  - 1: `1200`（20小时）
  - 2: `2160`（1天半）
  - 3: `2880`（2天）
  - 4: `7200`（5天）
  - 5: `10080`（7天）
- 计算公式：
  - `multiplier = 1.3 ^ min(reviewCount - 1, 10)`
  - `totalMinutes = int(baseMinutes * multiplier)`
  - `nextReviewAt = now + totalMinutes`
  - 最大间隔上限为 1 年（`525600` 分钟）

前 6 次复习示例（`reviewCount=1..6`，为相对“当前时间”的下次间隔）：

| 熟练度 | 第1次 | 第2次 | 第3次 | 第4次 | 第5次 | 第6次 |
|------|------|------|------|------|------|------|
| 1 | 20h | 1d2h | 1d9h48m | 1d19h56m | 2d9h7m | 3d2h15m |
| 2 | 1d12h | 1d22h48m | 2d12h50m | 3d7h5m | 4d6h49m | 5d13h39m |
| 3 | 2d | 2d14h24m | 3d9h7m | 4d9h27m | 5d17h5m | 7d10h13m |
| 4 | 5d | 6d12h | 8d10h48m | 10d23h38m | 14d6h43m | 18d13h33m |
| 5 | 7d | 9d2h24m | 11d19h55m | 15d9h5m | 19d23h49m | 25d23h46m |

### AI（可选）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/generate-note` | 仅生成注释文案（不创建卡片），请求体含 frontContent、contextSentence、aiApiKey、aiModel、aiBaseUrl |

---

## 🗄 数据库表（JPA 自动建表）

| 表名 | 说明 |
|------|------|
| `learn_user` | 用户 |
| `learn_document` | 文档（含 content 纯文本） |
| `learn_card` | 卡片（正面/背面、documentId、startOffset/endOffset 等） |
| `learn_card_sense` | 卡片义项（一词多义） |
| `learn_card_example` | 义项下例句（测验题源） |
| `learn_card_synonym` | 义项下同义词（仅展示） |
| `learn_card_global_extra` | 卡片扩展块（搭配、提示、高阶句） |
| `learn_card_progress` | 学习进度（熟练度、复习次数、下次复习时间） |
| `learn_quiz_session` | 测验会话 |
| `learn_quiz_session_item` | 测验题项（作答记录） |

- 下次复习时间由 `EbbinghausUtil.nextReviewAt(reviewCount, proficiencyLevel)` 按艾宾浩斯曲线计算。
- 「今日待复习」使用 SQL 条件 `next_review_at <= NOW()`，与数据源时区（建议 `Asia/Shanghai`）一致。
- 初始化 SQL 已同步到：`backend/src/main/resources/sql/init.sql`

---

## 📄 License

MIT
