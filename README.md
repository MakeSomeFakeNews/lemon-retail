<p align="center">
  <img src="lemon-web/public/favicon.svg" width="96" alt="柠檬连锁ERP" />
</p>

<h1 align="center">柠檬连锁ERP · Lemon Retail ERP</h1>

<p align="center">
  <b>面向连锁门店的开源 ERP 系统</b><br />
  由柠檬ERP 衍生，总部、门店、仓库一体化管理
</p>

<p align="center">
  <a href="https://github.com/MakeSomeFakeNews/lemon-retail/actions/workflows/ci.yml"><img src="https://github.com/MakeSomeFakeNews/lemon-retail/actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
  <a href="https://github.com/MakeSomeFakeNews/lemon-retail/releases/latest"><img src="https://img.shields.io/github/v/release/MakeSomeFakeNews/lemon-retail" alt="release" /></a>
  <a href="https://github.com/MakeSomeFakeNews/lemon-retail/stargazers"><img src="https://img.shields.io/github/stars/MakeSomeFakeNews/lemon-retail?style=flat&logo=github" alt="stars" /></a>
  <img src="https://img.shields.io/badge/license-Apache--2.0-blue.svg" alt="license" />
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="java" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen.svg" alt="spring boot" />
  <img src="https://img.shields.io/badge/Vue-3.5-42b883.svg" alt="vue" />
  <img src="https://img.shields.io/badge/Ant%20Design%20Vue-4.2-1677ff.svg" alt="antdv" />
  <img src="https://img.shields.io/badge/Flowable-8.0-ff6a00.svg" alt="flowable" />
</p>

<p align="center">
  <a href="https://makesomefakenews.github.io/lemon-retail-website/"><b>官网</b></a> ·
  <a href="https://makesomefakenews.github.io/lemon-retail-website/manual/guide/"><b>用户手册</b></a> ·
  <a href="https://makesomefakenews.github.io/lemon-retail-website/dev/start/"><b>开发文档</b></a> ·
  <a href="#联系方式"><b>联系我们</b></a> ·
  <a href="#english"><b>English</b></a>
</p>

<p align="center">
  <img src="docs/screenshots/dashboard.jpg" alt="柠檬连锁ERP 工作台" />
</p>

## 项目简介

柠檬连锁ERP（Lemon Retail ERP）是一套由[柠檬ERP](https://github.com/MakeSomeFakeNews/lemon-erp) 衍生、面向**多门店连锁商家**的开源进销存系统，前后端分离，开箱即用：总部统一管理商品、价格、采购和财务，门店负责销售、库存和日常经营。

- **进销存闭环**：采购订单 → 采购入库 → 库存 → 销售订单 → 发货拣货 → 销售出库，出入库自动更新库存（实时移动加权平均成本）并生成应收应付
- **业财一体**：应收应付核销、收付款、其他收支、账户调拨、出纳日记账，业务单据一键生成会计凭证，期末结账出报表
- **多价格与促销**：零售价、批发价多级、客户价格等级与执行价、销售价目表、促销活动（折扣、满减、买赠）、整单优惠与抹零
- **报表与分析**：进销存报表（销售、进货、经营状况、资产状况、商品分析、客户 RFM）、经营驾驶舱、多维分析、数据大屏
- **多端使用**：电脑浏览器、微信小程序（审批、库存、扫码作业）、数据大屏投屏，支持中英文界面
- **真正 100% 开源**：全部功能、全部源码开放，个人与企业均可免费商用

后续规划（门店模型、门店价格方案与调价、门店要货与配送、POS 收银、会员营销、门店日结）见 [连锁进销存需求规格](docs/requirements/chain-jxc-requirements.md)。

> 如果这个项目对你有帮助，请点一下右上角的 **Star**，这是对我们最大的鼓励！

## 默认账号


| 租户 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 柠檬科技 | admin | admin123 | 超级管理员，可以浏览全部功能 |
| 柠檬科技 | test | admin123 | 普通用户（只读），数据权限为「本部门及以下」 |

## 为什么选择柠檬连锁ERP

### 与常见方案对比

| | 商业进销存软件 | 「开源版 + 商业版」的开源进销存 | **柠檬连锁ERP** |
| --- | --- | --- | --- |
| 费用 | 按门店、用户数或年费收费，增值应用另计 | 开源版免费，连锁、财务等高级功能需要购买商业版 | **完全免费**，没有付费版本 |
| 源码 | 不提供 | 只开放部分源码 | **全部源码开放**，可以审计、修改 |
| 数据归属 | SaaS 版数据在厂商服务器 | 自己部署 | **私有化部署**，数据在自己的服务器上 |
| 门店数、单据量 | 按套餐限制 | 部分有限制 | **没有授权码，系统本身不限制** |
| 二次开发 | 依赖厂商 | 可以，但闭源部分改不了 | **完全自主**，有完整的开发文档和示例 |

### 核心优势

1. **省钱**：软件本身零费用，不按门店、用户或年收费，只需要一台服务器
2. **业财一体，账实相符**：出入库自动更新库存成本、生成应收应付，业务单据一键生成凭证，月末结账一键完成
3. **贴合国内商家**：多级批发价与客户价格等级、促销、整单抹零、数电票导入、小企业会计准则报表，钉钉 / 企业微信 / 飞书、微信小程序，DeepSeek、通义千问等国产大模型
4. **数据自主可控**：私有化部署在自己的服务器或内网，源码完全开放可审计
5. **拿来就能用**：Docker 一条命令启动；内置示例基础资料、期初启用向导；中英文界面
6. **容易二次开发**：Spring Boot 4 + Vue 3 主流技术栈，Gradle 多模块、依赖单向，单据审批、状态流转、数据权限、明细行等都有公共组件
7. **安全可靠**：菜单 / 按钮 / 接口三级权限和五种数据权限，操作日志与登录日志，会话可强制下线，审批流程留痕
8. **可以做成 SaaS**：内置多租户和租户套餐，软件服务商可以直接用它为多家客户提供在线服务

### 适合谁使用

| 用户 | 能得到什么 |
| --- | --- |
| **连锁门店、零售企业** | 总部统一的商品、价格、采购和财务，库存实时可查，进销存报表与经营分析 |
| **商贸、批发企业** | 采购、销售、库存、多价格体系、客户价格等级、促销、销售提成、应收应付与资金管理 |
| **软件服务商、实施商** | 免授权费的进销存底座，可二次开发后交付客户，或用多租户做成 SaaS 服务 |
| **开发者、学生** | 一个完整的企业级 Java 项目：多模块架构、权限与多租户、工作流、AI 集成、Docker 部署 |

## 功能一览

<table>
  <tr><th width="120">模块</th><th>主要功能</th></tr>
  <tr><td><b>供应链</b></td><td>供应商与供货价格（阶梯价）、客户（信用额度、账期、价格等级）；采购订单、采购入库 / 退货、补货建议（按再订货点生成采购订单）；价目表与商品多价格、客户执行价（Excel 导入导出）、促销、销售提成、销售订单、发货拣货装箱、销售出库 / 退货、欠交跟踪；入库出库可直接开单；订单与直接开单可整单优惠 / 抹零；订单变更（可审批）</td></tr>
  <tr><td><b>进销存报表</b></td><td>销售报表、进货报表（多维汇总，汇总 → 明细 → 单据穿透）、经营状况、资产状况（可选截至日期）、商品分析（畅销、滞销、积压、缺货、周转）、库存预警、客户 RFM 分群</td></tr>
  <tr><td><b>仓库</b></td><td>入库、出库、调拨（在途、同价 / 差价调拨）、盘点、组装拆卸；实时库存成本（移动加权平均，库存均价与金额、经营历史）；批次与序列号追踪、库存预留、收发存汇总；多规格商品（SKU）、库位与上架策略</td></tr>
  <tr><td><b>财务</b></td><td>应收应付与核销（自动核销、折让抹零、多收转预收、冲账单、坏账、应收应付对冲，支持外币）、账龄、数电票、往来对账；出纳日记账与资金日报、其他收支、账户调拨；凭证、账簿、资产负债表 / 利润表 / 现金流量表（小企业会计准则）、期末结账；销售毛利与库龄分析</td></tr>
  <tr><td><b>商务智能</b></td><td>经营驾驶舱、多维分析、自定义报表与仪表盘、决策支持与预警、数据大屏（销售经营、仓储物流，可投屏到电视）</td></tr>
  <tr><td><b>智能（AI）</b></td><td>接入 DeepSeek、通义千问、智谱 GLM、Kimi、豆包等大模型：AI 问数、智能录入（识别图片 / 文件生成单据）、数据整理、智能报告</td></tr>
  <tr><td><b>流程中心</b></td><td>仿钉钉流程设计器（条件分支、并行分支、会签 / 或签），业务单据一键接入审批</td></tr>
  <tr><td><b>系统平台</b></td><td>用户、部门、角色、菜单 / 按钮 / 数据权限；SaaS 多租户与租户套餐；编码规则、打印模板（PDF、套打）、业务参数、自定义字段；钉钉 / 企业微信 / 飞书集成；中英文切换；系统监控与定时任务</td></tr>
</table>

每个功能的详细说明见 [用户手册](https://makesomefakenews.github.io/lemon-retail-website/manual/guide/)。

## 系统截图

**采购订单详情**

![采购订单详情](docs/screenshots/scm-purchase-detail.jpg)

**财务报表（资产负债表）**

![资产负债表](docs/screenshots/fin-report.jpg)

**经营驾驶舱**

![经营驾驶舱](docs/screenshots/bi-cockpit.jpg)

**流程设计器**

![流程设计器](docs/screenshots/wf-designer.jpg)

**微信小程序**

![微信小程序](docs/screenshots/miniapp.jpg)

<details>
<summary><b>更多截图：登录与首页、供应链、仓库</b></summary>

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/login.jpg" alt="登录页" /><br />登录页</td>
    <td width="50%"><img src="docs/screenshots/dashboard-dark.jpg" alt="工作台（深色模式）" /><br />工作台（深色模式）</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/scm-purchase.jpg" alt="采购订单" /><br />采购订单</td>
    <td><img src="docs/screenshots/scm-sales-detail.jpg" alt="销售订单详情" /><br />销售订单详情</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/wms-stock.jpg" alt="库存查询" /><br />库存查询</td>
    <td><img src="docs/screenshots/wms-trace.jpg" alt="批次与序列号追踪" /><br />批次与序列号追踪</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/wms-material.jpg" alt="物料管理" /><br />物料管理</td>
    <td><img src="docs/screenshots/wms-location.jpg" alt="库位管理" /><br />库位管理</td>
  </tr>
</table>

</details>

<details>
<summary><b>更多截图：财务</b></summary>

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/fin-voucher.jpg" alt="凭证管理" /><br />凭证管理</td>
    <td width="50%"><img src="docs/screenshots/fin-voucher-detail.jpg" alt="记账凭证" /><br />记账凭证</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/fin-book.jpg" alt="科目余额表" /><br />科目余额表</td>
    <td><img src="docs/screenshots/fin-ar.jpg" alt="应收管理" /><br />应收管理</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/fin-analysis.jpg" alt="经营分析" /><br />经营分析</td>
    <td></td>
  </tr>
</table>

</details>

<details>
<summary><b>更多截图：商务智能、数据大屏、流程与系统</b></summary>

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/bi-olap.jpg" alt="多维分析" /><br />多维分析</td>
    <td width="50%"><img src="docs/screenshots/bi-board.jpg" alt="自定义仪表盘" /><br />自定义仪表盘</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/screen-warehouse.jpg" alt="仓储物流大屏" /><br />数据大屏（仓储物流）</td>
    <td><img src="docs/screenshots/screen-sales.jpg" alt="销售经营大屏" /><br />数据大屏（销售经营）</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/wf-instance.jpg" alt="流程实例" /><br />流程实例</td>
    <td><img src="docs/screenshots/sys-print-template.jpg" alt="打印模板设计" /><br />打印模板设计</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/sys-user.jpg" alt="用户管理" /><br />用户管理</td>
    <td><img src="docs/screenshots/sys-role.jpg" alt="角色管理" /><br />角色管理</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/sys-dept.jpg" alt="部门管理" /><br />部门管理</td>
    <td><img src="docs/screenshots/monitor-server.jpg" alt="服务监控" /><br />服务监控</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/monitor-cache.jpg" alt="缓存监控" /><br />缓存监控</td>
    <td><img src="docs/screenshots/monitor-job.jpg" alt="定时任务" /><br />定时任务</td>
  </tr>
</table>

</details>

## 系统架构

**业务架构**：蓝色虚线是模块内的业务流程，灰色虚线是跨模块的自动联动（生成下游单据、更新库存、生成应收应付与凭证）。

![柠檬连锁ERP 业务架构图](docs/architecture/business.png)

**技术架构**

![柠檬连锁ERP 技术架构图](docs/architecture/tech.png)

## 快速开始

### 方式一：Docker 一键部署（推荐）

安装了 Docker（含 Compose v2）时，一条命令启动 MySQL、Redis、后端和前端：

```bash
cd docker
cp .env.example .env        # 修改其中的密码和 JWT_SECRET
docker compose up -d --build
```

启动完成后访问 http://localhost（端口见 `.env` 中的 `WEB_PORT`）。构建、推送镜像使用 `./docker/build.sh`，详见 [Docker 部署](https://makesomefakenews.github.io/lemon-retail-website/dev/deploy/docker)。

不想在服务器上编译时，可以直接使用 [GitHub Packages](https://github.com/MakeSomeFakeNews?tab=packages&repo_name=lemon-retail) 中已构建好的镜像（支持 x86 和 ARM）：从 [Releases](https://github.com/MakeSomeFakeNews/lemon-retail/releases/latest) 下载 `lemon-retail-docker-<版本>.zip`，解压后：

```bash
cd docker
cp .env.example .env        # 已填好镜像地址 ghcr.io/makesomefakenews/lemon-retail，修改密码和 JWT_SECRET 即可
docker compose up -d
```

Releases 中还提供后端 jar、前端静态文件和 `init.sql`，可用于不使用 Docker 的传统部署。


### 方式二：源码运行

```bash
# 1. 初始化数据库（会删除并重建全部表）
mysql -uroot -p -e "CREATE DATABASE lemon_retail DEFAULT CHARSET utf8mb4"
mysql -uroot -p lemon_retail < sql/init.sql

# 2. 启动后端（项目根目录执行，默认连接本机 MySQL root / root 和本机 Redis，
#    可用环境变量 DB_URL、DB_USERNAME、DB_PASSWORD、REDIS_HOST、REDIS_PORT、REDIS_PASSWORD 覆盖）
DB_PASSWORD=你的密码 ./gradlew bootRun

# 3. 启动前端，访问 http://localhost:5173（/api 自动代理到 http://localhost:8080）
cd lemon-web && npm install && npm run dev
```

<details>
<summary>打包部署与 Nginx 参考配置</summary>

```bash
./gradlew build                     # 生成 lemon-admin/build/libs/lemon-retail.jar
cd lemon-web && npm run build       # 生成 lemon-web/dist，交给 Nginx 托管

# 生产环境配置全部取环境变量，JWT_SECRET 至少 32 字节
java -jar lemon-retail.jar --spring.profiles.active=prod
```

```nginx
location / {
    root /path/to/lemon-web/dist;
    try_files $uri $uri/ /index.html;
}
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

使用 AI 问数时还需要为 `/api/ai/` 关闭代理缓冲，见 [Nginx 配置](https://makesomefakenews.github.io/lemon-retail-website/dev/deploy/nginx)。

</details>

**默认账号**：`admin` / `admin123`（超级管理员）、`test` / `admin123`（只读用户）。开启 SaaS 多租户（`LEMON_TENANT_ENABLED=true`）后登录需要填写租户「柠檬科技」；设置 `LEMON_DEMO_ENABLED=true` 开启演示模式（只能查看，不能修改数据）。

## 技术栈

| 分类 | 技术 | 环境要求 |
| --- | --- | --- |
| 后端 | Java 17、Spring Boot 4.1、Spring Security + JWT、MyBatis-Plus 3.5、Flowable 8、JasperReports 7 | JDK 17 |
| 数据与缓存 | MySQL、Redis（可切换为内存缓存） | MySQL 8.0+、Redis 6.2+ |
| 前端 | Vue 3.5、Vite 7、Ant Design Vue 4.2、Pinia、Vue Router、ECharts 5、vue-i18n | Node 20+ |
| 小程序 | uni-app（Vue 3），微信小程序 | 微信开发者工具 |
| 构建部署 | Gradle 多模块、Docker / Docker Compose、Nginx | Docker 20.10+、Nginx 1.20+ |

## 项目结构

```
lemon-retail
├── lemon-common        通用层：统一返回、异常、基础实体、工具类
├── lemon-framework     框架层：安全、数据权限、多租户、缓存、日志、模块间契约接口
├── lemon-modules       业务模块
│   ├── lemon-system    系统管理        ├── lemon-scm   供应链管理
│   ├── lemon-monitor   系统监控        ├── lemon-fin   财务管理
│   ├── lemon-workflow  流程中心        ├── lemon-bi    商务智能、进销存报表
│   ├── lemon-dashboard 工作台          └── lemon-ai    智能（AI）
│   └── lemon-wms       仓库管理
├── lemon-admin         启动模块：组装全部业务模块，打包为可执行 jar
├── lemon-web           前端（Vite + Vue 3 + Ant Design Vue）
├── lemon-mp            微信小程序（uni-app + Vue 3）
├── sql/init.sql        建表与初始数据
├── docker              Dockerfile、docker-compose 与镜像构建脚本
└── docs                开发说明、架构图与截图
```

## 联系方式

商务合作、定制开发、部署实施或使用中遇到问题，欢迎联系：

- 电话：17701123202
- 微信：BG7LIF
- 邮箱：zmissu@163.com

也欢迎在 [Issues](https://github.com/MakeSomeFakeNews/lemon-retail/issues) 中反馈问题和建议，或提交 Pull Request 一起完善。

## 真正 100% 开源

柠檬连锁ERP 不是「开源版 + 商业版」的模式，演示环境和文档中看到的全部功能，都在本仓库中完整开源：

- **全部功能开源**：供应链、仓储、财务、进销存报表、商务智能、AI，没有任何模块收费或闭源
- **没有功能限制**：没有授权码和激活，系统本身不限制用户数、租户数和单据量，也没有隐藏的高级功能
- **免费商用**：遵守 [Apache-2.0](LICENSE) 协议，个人与企业都可以免费使用、修改、二次开发和商业部署，使用时保留原作者版权声明与许可证文件即可
- **文档同样开放**：[用户手册](https://makesomefakenews.github.io/lemon-retail-website/manual/guide/) 和 [开发文档](https://makesomefakenews.github.io/lemon-retail-website/dev/start/)、Docker 一键部署脚本全部公开

如果觉得柠檬连锁ERP 不错，请点一个 **Star**，也欢迎推荐给身边开门店、做生意、做软件的朋友。

## English

**Lemon Retail ERP** is a truly 100% open-source **inventory, purchasing and sales system for multi-store retail chains**, derived from Lemon ERP and built with **Spring Boot 4 + Vue 3 + Ant Design Vue 4**. It covers purchasing, sales, pricing and promotions, inventory and warehouse management with real-time moving-average cost, receivables/payables and cash, general ledger, inventory and sales reports, business intelligence (**BI**) and **AI** assistants. Documents flow between modules automatically: stock movements update inventory and generate receivables/payables, and business documents generate accounting vouchers.

- Visual approval workflow (Flowable), SaaS multi-tenancy, menu / button / API permissions and department-level data permissions
- AI assistants with DeepSeek, Qwen, GLM, Kimi or Doubao: natural-language data queries, document recognition, data cleansing and business reports
- WeChat mini program, data screens, DingTalk / WeCom / Feishu login and notifications
- Chinese and English UI, demo mode, Docker one-click deployment
- Every feature and every line of source code is open: no paid edition, no hidden modules, no license keys; Apache-2.0 licensed, free for commercial use

Documentation: https://makesomefakenews.github.io/lemon-retail-website/ · Contact: zmissu@163.com

---

<p align="center">Apache License 2.0 © 2026 柠檬连锁ERP</p>
