-- 柠檬连锁ERP 初始化脚本（MySQL 8）
-- 默认账号：admin / admin123（超级管理员）、test / admin123（普通用户，只有查看权限，数据权限为本部门及以下）
-- 工作流引擎的 ACT_ 开头的表由 Flowable 启动时自动创建，不在本脚本中

DROP TABLE IF EXISTS ai_report;
DROP TABLE IF EXISTS ai_cleanse_suggestion;
DROP TABLE IF EXISTS ai_cleanse_task;
DROP TABLE IF EXISTS ai_message;
DROP TABLE IF EXISTS ai_conversation;
DROP TABLE IF EXISTS ai_usage;
DROP TABLE IF EXISTS ai_config;
DROP TABLE IF EXISTS bi_alert_rule;
DROP TABLE IF EXISTS bi_dashboard;
DROP TABLE IF EXISTS bi_report;
DROP TABLE IF EXISTS bi_etl_log;
DROP TABLE IF EXISTS bi_fact_finance;
DROP TABLE IF EXISTS bi_fact_inventory;
DROP TABLE IF EXISTS bi_fact_purchase;
DROP TABLE IF EXISTS bi_fact_sales;
DROP TABLE IF EXISTS fin_commission_line;
DROP TABLE IF EXISTS fin_commission;
DROP TABLE IF EXISTS fin_commission_rule;
DROP TABLE IF EXISTS fin_account_rule;
DROP TABLE IF EXISTS scm_delivery_package_item;
DROP TABLE IF EXISTS scm_delivery_package;
DROP TABLE IF EXISTS scm_promotion_usage;
DROP TABLE IF EXISTS scm_promotion;
DROP TABLE IF EXISTS fin_fund_transfer;
DROP TABLE IF EXISTS fin_invoice;
DROP TABLE IF EXISTS fin_invoice_line;
DROP TABLE IF EXISTS fin_invoice_item;
DROP TABLE IF EXISTS fin_advance;
DROP TABLE IF EXISTS fin_advance_apply;
DROP TABLE IF EXISTS fin_offset;
DROP TABLE IF EXISTS fin_netting_item;
DROP TABLE IF EXISTS fin_netting;
DROP TABLE IF EXISTS fin_io_doc_item;
DROP TABLE IF EXISTS fin_io_doc;
DROP TABLE IF EXISTS fin_io_category;
DROP TABLE IF EXISTS fin_settlement_item;
DROP TABLE IF EXISTS fin_settlement;
DROP TABLE IF EXISTS fin_bill;
DROP TABLE IF EXISTS fin_voucher_entry;
DROP TABLE IF EXISTS fin_voucher;
DROP TABLE IF EXISTS fin_period;
DROP TABLE IF EXISTS fin_account_opening;
DROP TABLE IF EXISTS fin_exchange_rate;
DROP TABLE IF EXISTS fin_currency;
DROP TABLE IF EXISTS fin_account;
DROP TABLE IF EXISTS scm_delivery;
DROP TABLE IF EXISTS scm_delivery_item;
DROP TABLE IF EXISTS scm_delivery_pick;
DROP TABLE IF EXISTS scm_trade_draft_item;
DROP TABLE IF EXISTS scm_trade_draft;
DROP TABLE IF EXISTS scm_trade_doc_item;
DROP TABLE IF EXISTS scm_trade_doc;
DROP TABLE IF EXISTS scm_order_item;
DROP TABLE IF EXISTS scm_order_change_item;
DROP TABLE IF EXISTS scm_order_change;
DROP TABLE IF EXISTS scm_order;
DROP TABLE IF EXISTS scm_customer;
DROP TABLE IF EXISTS scm_supplier_price_tier;
DROP TABLE IF EXISTS scm_price_history;
DROP TABLE IF EXISTS scm_price_adjust_item;
DROP TABLE IF EXISTS scm_price_adjust_scheme;
DROP TABLE IF EXISTS scm_price_adjust;
DROP TABLE IF EXISTS scm_price_scheme_item;
DROP TABLE IF EXISTS scm_price_scheme;
DROP TABLE IF EXISTS scm_price_list;
DROP TABLE IF EXISTS scm_price_control;
DROP TABLE IF EXISTS scm_customer_price;
DROP TABLE IF EXISTS scm_price_memory;
DROP TABLE IF EXISTS scm_supplier_price;
DROP TABLE IF EXISTS scm_supplier;
DROP TABLE IF EXISTS wms_doc_sequence;
DROP TABLE IF EXISTS wms_check_item;
DROP TABLE IF EXISTS wms_check;
DROP TABLE IF EXISTS wms_assembly_item;
DROP TABLE IF EXISTS wms_assembly;
DROP TABLE IF EXISTS wms_stock_doc_item;
DROP TABLE IF EXISTS wms_stock_doc;
DROP TABLE IF EXISTS wms_serial_log;
DROP TABLE IF EXISTS wms_stock_flow;
DROP TABLE IF EXISTS wms_serial;
DROP TABLE IF EXISTS wms_reservation;
DROP TABLE IF EXISTS store_info;
DROP TABLE IF EXISTS store_region;
DROP TABLE IF EXISTS wms_stock;
DROP TABLE IF EXISTS wms_batch;
DROP TABLE IF EXISTS wms_variant_value;
DROP TABLE IF EXISTS wms_attribute_value;
DROP TABLE IF EXISTS wms_attribute;
DROP TABLE IF EXISTS wms_material_unit;
DROP TABLE IF EXISTS wms_material_image;
DROP TABLE IF EXISTS wms_material_warehouse_limit;
DROP TABLE IF EXISTS wms_price_level;
DROP TABLE IF EXISTS wms_material;
DROP TABLE IF EXISTS wms_material_category;
DROP TABLE IF EXISTS wms_location;
DROP TABLE IF EXISTS wms_warehouse;
DROP TABLE IF EXISTS wf_copy;
DROP TABLE IF EXISTS wf_model;
DROP TABLE IF EXISTS sys_change_log;
DROP TABLE IF EXISTS sys_attachment;
DROP TABLE IF EXISTS sys_notice_recipient;
DROP TABLE IF EXISTS sys_notice_target;
DROP TABLE IF EXISTS sys_notice;
DROP TABLE IF EXISTS sys_message;
DROP TABLE IF EXISTS sys_job_log;
DROP TABLE IF EXISTS sys_job;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_role_dept;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user_store;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_tenant;
DROP TABLE IF EXISTS sys_code_rule;
DROP TABLE IF EXISTS sys_biz_rule;
DROP TABLE IF EXISTS sys_im_config;
DROP TABLE IF EXISTS sys_im_user;
DROP TABLE IF EXISTS sys_im_dept;
DROP TABLE IF EXISTS sys_miniapp_user;
DROP TABLE IF EXISTS sys_custom_value;
DROP TABLE IF EXISTS sys_custom_field;
DROP TABLE IF EXISTS sys_print_template;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_tenant_package_menu;
DROP TABLE IF EXISTS sys_tenant_package;

-- 租户：SaaS 模式（lemon.tenant.enabled）下业务表按 tenant_id 隔离；租户 1 是平台租户（运营方），
-- 未开启 SaaS 模式时所有数据都属于它。菜单、字典、定时任务等平台共享的表没有 tenant_id
CREATE TABLE sys_tenant_package (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '套餐名称',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_package_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '租户套餐';

CREATE TABLE sys_tenant_package_menu (
    package_id BIGINT NOT NULL COMMENT '套餐ID',
    menu_id    BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (package_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '套餐可用菜单';

CREATE TABLE sys_tenant (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(64)  NOT NULL COMMENT '租户名称，登录时填写',
    package_id    BIGINT                DEFAULT NULL COMMENT '套餐，平台租户为空（不受限）',
    contact_name  VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    expire_time   DATETIME              DEFAULT NULL COMMENT '到期时间，为空不限',
    account_limit INT                   DEFAULT NULL COMMENT '账号数量上限，为空不限',
    admin_user_id BIGINT                DEFAULT NULL COMMENT '租户管理员（创建租户时开通）',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time   DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_name (name),
    KEY idx_tenant_package (package_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '租户';

-- 编码规则：租户修改过的单据编号规则，未修改的单据使用各模块声明的默认规则（DocNoCatalog）
CREATE TABLE sys_code_rule (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_key    VARCHAR(16) NOT NULL COMMENT '规则标识（默认前缀），如 CG',
    prefix      VARCHAR(8)  NOT NULL COMMENT '前缀',
    date_format VARCHAR(16) NOT NULL COMMENT '日期格式 YYYYMMDD / YYMMDD / YYYYMM / YYMM / YYYY / NONE',
    seq_length  INT         NOT NULL COMMENT '流水位数',
    reset_cycle VARCHAR(8)  NOT NULL COMMENT '流水重置周期 DAY / MONTH / YEAR / NEVER',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_rule (rule_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '编码规则';

-- 系统参数（参数设置）：平台级，所有租户共用；只保存修改过的参数，参数定义见 framework/setting/Setting
CREATE TABLE sys_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key   VARCHAR(64)  NOT NULL COMMENT '参数标识，如 captcha.mode',
    config_value VARCHAR(500) NOT NULL COMMENT '参数值',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统参数';

-- 打印模板：租户修改过的单据打印配置（JSON，由前端解释），未修改的单据使用默认版式；上传了 JRXML 的单据按 JRXML 打印。
-- 同一单据可有多份打印格式（每行一份，如销售出库单的「发货单（不含价格）」），每个模板标识有且只有一份默认格式，有多份启用的格式时打印前选择
CREATE TABLE sys_print_template (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    template_key VARCHAR(64) NOT NULL COMMENT '模板标识，如 scm.purchaseOrder；_common 为通用设置',
    name         VARCHAR(32) NOT NULL COMMENT '格式名称，同一模板标识内唯一；只有一份格式时为「标准格式」',
    is_default   TINYINT     NOT NULL DEFAULT 1 COMMENT '是否默认格式 1是 0否：每个模板标识只有一份，只有一份启用的格式时直接按它打印',
    sort         INT         NOT NULL DEFAULT 0 COMMENT '排序（打印时选择格式的顺序）',
    status       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用（默认格式不能停用）',
    config       TEXT        NOT NULL COMMENT '模板配置 JSON',
    jrxml        MEDIUMTEXT COMMENT '自定义 JRXML（Jaspersoft Studio 7），为空时按配置生成默认版式',
    background   MEDIUMTEXT COMMENT '套打底图（data URL）：设计套打位置时对照，不打印',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_print_template (template_key, name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '打印模板';

CREATE TABLE sys_biz_rule (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_key    VARCHAR(64)  NOT NULL COMMENT '参数标识，如 scm.purchaseIn.overRate',
    rule_value  VARCHAR(255) NOT NULL COMMENT '参数值',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_rule (rule_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务参数（按租户）：各模块的业务控制规则，只保存修改过的';

CREATE TABLE sys_im_config (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    platform          VARCHAR(16)  NOT NULL COMMENT '平台 DINGTALK钉钉 WECOM企业微信 FEISHU飞书',
    enabled           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否启用 1是 0否',
    corp_id           VARCHAR(64)           DEFAULT NULL COMMENT '企业ID（企业微信 corpid）',
    app_key           VARCHAR(64)           DEFAULT NULL COMMENT '应用标识（钉钉 Client ID / AppKey，飞书 App ID）',
    app_secret        VARCHAR(128)          DEFAULT NULL COMMENT '应用密钥（钉钉 Client Secret，企业微信应用 Secret，飞书 App Secret），不返回前端',
    agent_id          VARCHAR(32)           DEFAULT NULL COMMENT '应用 AgentId（钉钉、企业微信，发送工作通知用）',
    home_url          VARCHAR(255)          DEFAULT NULL COMMENT '系统访问地址（外网可访问的前端地址），登录回调和消息链接使用',
    login_enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '扫码登录与应用内免登 1开启 0关闭',
    message_enabled   TINYINT      NOT NULL DEFAULT 1 COMMENT '站内消息同步推送给已绑定的成员 1开启 0关闭',
    sync_create       TINYINT      NOT NULL DEFAULT 0 COMMENT '同步通讯录时自动创建部门和账号 1是 0否（否则只按手机号绑定已有账号）',
    robot_webhook     VARCHAR(500)          DEFAULT NULL COMMENT '群机器人 Webhook 地址',
    robot_secret      VARCHAR(128)          DEFAULT NULL COMMENT '群机器人加签密钥（钉钉、飞书），不返回前端',
    robot_categories  VARCHAR(64)           DEFAULT NULL COMMENT '推送到群机器人的消息类别（逗号分隔，字典 sys_message_category）',
    last_sync_time    DATETIME              DEFAULT NULL COMMENT '最近同步通讯录时间',
    last_sync_result  VARCHAR(255)          DEFAULT NULL COMMENT '最近同步结果',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_config (platform, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '第三方集成（钉钉、企业微信、飞书）的应用配置，按租户';

CREATE TABLE sys_im_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '系统用户',
    platform    VARCHAR(16)  NOT NULL COMMENT '平台 DINGTALK WECOM FEISHU',
    im_user_id  VARCHAR(64)  NOT NULL COMMENT '平台中的成员 userid',
    im_name     VARCHAR(64)           DEFAULT NULL COMMENT '平台中的成员姓名',
    create_time DATETIME              DEFAULT NULL COMMENT '绑定时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_user_member (platform, im_user_id, tenant_id),
    UNIQUE KEY uk_im_user_user (user_id, platform, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户与钉钉 / 企业微信 / 飞书成员的绑定';

CREATE TABLE sys_im_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    dept_id     BIGINT       NOT NULL COMMENT '系统部门',
    platform    VARCHAR(16)  NOT NULL COMMENT '平台 DINGTALK WECOM FEISHU',
    im_dept_id  VARCHAR(64)  NOT NULL COMMENT '平台中的部门 ID',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_dept (platform, im_dept_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '同步通讯录时系统部门与平台部门的对应关系';

CREATE TABLE sys_miniapp_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '系统用户',
    openid      VARCHAR(64)  NOT NULL COMMENT '微信小程序 openid',
    create_time DATETIME              DEFAULT NULL COMMENT '绑定时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_miniapp_user_openid (openid, tenant_id),
    UNIQUE KEY uk_miniapp_user_user (user_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户与微信小程序的绑定（登录时按 openid 跨租户查询）';

CREATE TABLE sys_user_setting (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id       BIGINT      NOT NULL COMMENT '用户ID',
    setting_key   VARCHAR(32) NOT NULL COMMENT '设置项标识：ui 界面设置（主题、主题色、布局、多标签页、紧凑模式等）',
    setting_value TEXT                 DEFAULT NULL COMMENT '设置内容（JSON 对象）',
    create_time   DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_setting (user_id, setting_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户个人设置（只能本人读写）';

CREATE TABLE sys_custom_field (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type     VARCHAR(32)  NOT NULL COMMENT '业务类型（与附件相同的业务记录类型，如 scm_customer、wms_material）',
    field_key    VARCHAR(32)  NOT NULL COMMENT '字段标识，字母开头的字母、数字、下划线',
    label        VARCHAR(32)  NOT NULL COMMENT '显示名称',
    field_type   VARCHAR(16)  NOT NULL DEFAULT 'TEXT' COMMENT '类型(字典 sys_custom_field_type) TEXT文本 TEXTAREA多行文本 NUMBER数字 DATE日期 SELECT下拉 BOOL是否',
    options      VARCHAR(1000)         DEFAULT NULL COMMENT '下拉选项，逗号或换行分隔',
    required     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否必填',
    show_in_list TINYINT      NOT NULL DEFAULT 0 COMMENT '是否在列表中显示',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '说明（显示为输入提示）',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_field (biz_type, field_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义字段：为单据、主数据扩展字段，不改表结构';

CREATE TABLE sys_custom_value (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type    VARCHAR(32)   NOT NULL COMMENT '业务类型',
    biz_id      BIGINT        NOT NULL COMMENT '业务记录ID',
    field_key   VARCHAR(32)   NOT NULL COMMENT '字段标识',
    field_value VARCHAR(1000) NOT NULL COMMENT '值（文本；数字、日期、是否按约定格式）',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_value (biz_type, biz_id, field_key, tenant_id),
    KEY idx_custom_value_field (biz_type, field_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义字段的值';

CREATE TABLE sys_dept (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '上级部门ID，0为顶级',
    name        VARCHAR(64) NOT NULL COMMENT '部门名称',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    leader_user_id BIGINT               DEFAULT NULL COMMENT '负责人（用户ID），审批人「部门主管」据此计算',
    phone       VARCHAR(32)          DEFAULT NULL COMMENT '联系电话',
    email       VARCHAR(128)         DEFAULT NULL COMMENT '邮箱',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_dept_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门';

CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    dept_id     BIGINT                DEFAULT NULL COMMENT '部门ID',
    store_id    BIGINT                DEFAULT NULL COMMENT '所属门店（主属门店），开单默认门店',
    store_scope VARCHAR(16)  NOT NULL DEFAULT 'ALL' COMMENT '可管辖门店(字典 sys_store_scope) ALL全部门店 ASSIGNED所属门店 + 指定门店（sys_user_store）',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    pwd_update_time DATETIME              DEFAULT NULL COMMENT '最近设置密码的时间（密码有效期）',
    pwd_reset   TINYINT      NOT NULL DEFAULT 0 COMMENT '1 密码由管理员设置，登录后提示本人修改',
    nickname    VARCHAR(64)           DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(32)           DEFAULT NULL COMMENT '手机号',
    avatar      VARCHAR(255)          DEFAULT NULL COMMENT '头像URL',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username, tenant_id),
    KEY idx_user_dept_id (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64) NOT NULL COMMENT '角色名称',
    code        VARCHAR(64) NOT NULL COMMENT '角色编码',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    data_scope  TINYINT     NOT NULL DEFAULT 3 COMMENT '数据权限(字典 sys_data_scope) 1全部 2自定义 3本部门 4本部门及以下 5仅本人',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色';

CREATE TABLE sys_menu (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为顶级',
    name        VARCHAR(64) NOT NULL COMMENT '菜单名称',
    type        CHAR(1)     NOT NULL COMMENT '类型 M目录 C菜单 F按钮',
    path        VARCHAR(200)         DEFAULT NULL COMMENT '路由地址',
    component   VARCHAR(200)         DEFAULT NULL COMMENT '组件路径(相对 src/views，不带 .vue)',
    perms       VARCHAR(100)         DEFAULT NULL COMMENT '权限标识',
    icon        VARCHAR(64)          DEFAULT NULL COMMENT '图标',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    visible     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否显示 1显示 0隐藏',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单权限';

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    -- 按角色反查用户（删除角色前检查、角色关联用户）
    KEY idx_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联';

CREATE TABLE sys_user_store (
    user_id  BIGINT NOT NULL COMMENT '用户ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    PRIMARY KEY (user_id, store_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户管辖门店（门店范围为指定门店时，所属门店之外另外可管辖的门店）';

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    -- 删除菜单时清理关联
    KEY idx_menu_id (menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联';

CREATE TABLE sys_role_dept (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    PRIMARY KEY (role_id, dept_id),
    -- 删除部门时清理关联
    KEY idx_dept_id (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色自定义数据权限（部门）';

CREATE TABLE sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '字典名称',
    type        VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    kind        VARCHAR(16)  NOT NULL DEFAULT 'SYSTEM' COMMENT '种类 SYSTEM系统字典（取值由程序内置，全平台共用，只能改名称、颜色、排序） BUSINESS业务字典（每个租户各自维护，可新增取值）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典类型';

CREATE TABLE sys_dict_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID：系统字典只有平台（1）一份；业务字典每个租户一份，新租户开通时从平台复制',
    dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    dict_label  VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value  VARCHAR(100) NOT NULL COMMENT '字典键值',
    tag_color   VARCHAR(32)           DEFAULT NULL COMMENT '标签颜色(ant-design-vue Tag color)',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    builtin     TINYINT      NOT NULL DEFAULT 0 COMMENT '内置取值 1是：程序依赖该取值，不能删除、不能改键值（系统字典的取值全部内置）',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    ext_value   VARCHAR(32)           DEFAULT NULL COMMENT '扩展值：只用于业务模块声明了扩展值的字典（如开票类型 scm_invoice_kind 的税率 %），其他字典为空',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type_value (dict_type, dict_value, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典数据';

CREATE TABLE sys_oper_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    title          VARCHAR(64)           DEFAULT NULL COMMENT '模块标题',
    oper_type      TINYINT      NOT NULL DEFAULT 0 COMMENT '操作类型(字典 sys_oper_type)',
    method         VARCHAR(200)          DEFAULT NULL COMMENT '类名.方法名',
    request_method VARCHAR(10)           DEFAULT NULL COMMENT '请求方式',
    url            VARCHAR(255)          DEFAULT NULL COMMENT '请求地址',
    ip             VARCHAR(64)           DEFAULT NULL COMMENT 'IP',
    location       VARCHAR(128)          DEFAULT NULL COMMENT 'IP 归属地',
    operator       VARCHAR(64)           DEFAULT NULL COMMENT '操作人',
    params         TEXT COMMENT '请求参数',
    result         TEXT COMMENT '返回结果',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
    error_msg      VARCHAR(2000)         DEFAULT NULL COMMENT '错误信息',
    cost_time      BIGINT                DEFAULT NULL COMMENT '耗时(毫秒)',
    oper_time      DATETIME              DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_oper_time (oper_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志';

CREATE TABLE sys_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '接收人',
    category    VARCHAR(16)  NOT NULL COMMENT '类别(字典 sys_message_category) APPROVAL审批 REMIND提醒 ALERT预警 SYSTEM系统 NOTICE公告',
    title       VARCHAR(128) NOT NULL COMMENT '标题',
    content     VARCHAR(500)          DEFAULT NULL COMMENT '内容',
    link        VARCHAR(255)          DEFAULT NULL COMMENT '前端路由，点击跳转',
    dedup_key   VARCHAR(128)          DEFAULT NULL COMMENT '去重键：同一用户相同去重键只发送一次（定时提醒）',
    read_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读 1是 0否',
    read_time   DATETIME              DEFAULT NULL COMMENT '阅读时间',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_message_user (user_id, read_flag),
    KEY idx_message_dedup (dedup_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内消息';

CREATE TABLE sys_notice (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    title        VARCHAR(128) NOT NULL COMMENT '标题',
    type         VARCHAR(32)  NOT NULL COMMENT '类型(业务字典 sys_notice_type)',
    content      MEDIUMTEXT COMMENT '正文(Markdown)',
    scope_type   VARCHAR(16)  NOT NULL DEFAULT 'ALL' COMMENT '发布范围(字典 sys_notice_scope) ALL全员 DEPT指定部门(含下级) ROLE指定角色',
    pinned       TINYINT      NOT NULL DEFAULT 0 COMMENT '置顶 1是 0否',
    expire_date  DATE                  DEFAULT NULL COMMENT '有效期至(含当天)，为空长期有效，过期后员工不再看到',
    status       VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 sys_notice_status) DRAFT草稿 PUBLISHED已发布 WITHDRAWN已撤回',
    publish_time DATETIME              DEFAULT NULL COMMENT '最近发布时间',
    publish_by   BIGINT                DEFAULT NULL COMMENT '发布人',
    create_by    BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_notice_status (status, publish_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告';

CREATE TABLE sys_notice_target (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    notice_id   BIGINT NOT NULL COMMENT '公告ID',
    target_id   BIGINT NOT NULL COMMENT '部门ID(指定部门)或角色ID(指定角色)',
    PRIMARY KEY (id),
    KEY idx_notice_target (notice_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告发布范围';

CREATE TABLE sys_notice_recipient (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT   NOT NULL DEFAULT 1 COMMENT '租户ID',
    notice_id   BIGINT   NOT NULL COMMENT '公告ID',
    user_id     BIGINT   NOT NULL COMMENT '接收人(发布时按范围确定)',
    read_time   DATETIME          DEFAULT NULL COMMENT '阅读时间，未读为空',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notice_recipient (notice_id, user_id, tenant_id),
    KEY idx_notice_recipient_user (user_id, read_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告接收人与阅读记录';

CREATE TABLE sys_attachment (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type     VARCHAR(32)  NOT NULL COMMENT '业务类型（AttachmentType.code），如 scm_purchase',
    biz_id       BIGINT       NOT NULL COMMENT '业务记录ID',
    file_name    VARCHAR(255) NOT NULL COMMENT '原文件名',
    file_size    BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    content_type VARCHAR(128)          DEFAULT NULL COMMENT '文件类型',
    storage_path VARCHAR(255) NOT NULL COMMENT '相对附件目录的存储路径',
    create_by    BIGINT                DEFAULT NULL COMMENT '上传人',
    create_time  DATETIME              DEFAULT NULL COMMENT '上传时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_attachment_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务附件';

CREATE TABLE sys_change_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type    VARCHAR(32)  NOT NULL COMMENT '业务类型（RecordType.code），如 wms_material',
    biz_id      BIGINT       NOT NULL COMMENT '业务记录ID',
    field_name  VARCHAR(64)  NOT NULL COMMENT '字段',
    field_label VARCHAR(64)           DEFAULT NULL COMMENT '字段名称',
    old_value   VARCHAR(500)          DEFAULT NULL COMMENT '旧值',
    new_value   VARCHAR(500)          DEFAULT NULL COMMENT '新值',
    create_by   BIGINT                DEFAULT NULL COMMENT '修改人',
    create_time DATETIME              DEFAULT NULL COMMENT '修改时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_change_log_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字段变更历史';

CREATE TABLE sys_job (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    job_key        VARCHAR(64)  NOT NULL COMMENT '任务标识（JobHandler.key）',
    name           VARCHAR(64)  NOT NULL COMMENT '任务名称',
    cron           VARCHAR(64)  NOT NULL COMMENT 'Cron 表达式（秒 分 时 日 月 周）',
    params         VARCHAR(255)          DEFAULT NULL COMMENT '参数 key=value，多个用逗号分隔',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    last_run_time  DATETIME              DEFAULT NULL COMMENT '最近执行时间',
    last_status    VARCHAR(16)           DEFAULT NULL COMMENT '最近执行结果 SUCCESS成功 FAILED失败',
    last_message   VARCHAR(500)          DEFAULT NULL COMMENT '最近执行说明',
    remark         VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time    DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_key (job_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务';

CREATE TABLE sys_job_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    job_key      VARCHAR(64)  NOT NULL COMMENT '任务标识',
    trigger_type VARCHAR(16)  NOT NULL COMMENT '触发方式 SCHEDULE定时 MANUAL手动',
    start_time   DATETIME     NOT NULL COMMENT '开始时间',
    cost_ms      BIGINT                DEFAULT NULL COMMENT '耗时（毫秒）',
    status       VARCHAR(16)  NOT NULL COMMENT '结果 SUCCESS成功 FAILED失败',
    message      VARCHAR(500)          DEFAULT NULL COMMENT '执行说明或错误信息',
    create_by    BIGINT                DEFAULT NULL COMMENT '手动执行人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_job_log_key (job_key, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务执行日志';

CREATE TABLE sys_login_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id  BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    username   VARCHAR(64)          DEFAULT NULL COMMENT '用户名',
    ip         VARCHAR(64)          DEFAULT NULL COMMENT 'IP',
    location   VARCHAR(128)         DEFAULT NULL COMMENT 'IP 归属地',
    browser    VARCHAR(64)          DEFAULT NULL COMMENT '浏览器',
    os         VARCHAR(64)          DEFAULT NULL COMMENT '操作系统',
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
    msg        VARCHAR(255)         DEFAULT NULL COMMENT '提示信息',
    login_time DATETIME             DEFAULT NULL COMMENT '登录时间',
    PRIMARY KEY (id),
    -- 首页今日登录数、近 7 天趋势只需这两列，覆盖索引不回表
    KEY idx_login_time_status (login_time, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志';

CREATE TABLE wf_model (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    model_key     VARCHAR(64)  NOT NULL COMMENT '流程标识（BPMN 流程 ID），发布后不可修改',
    name          VARCHAR(64)  NOT NULL COMMENT '流程名称',
    category      VARCHAR(64)           DEFAULT NULL COMMENT '分类(字典 wf_category)',
    icon          VARCHAR(64)           DEFAULT NULL COMMENT '图标',
    sort          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用（停用后不能发起）',
    version       INT                   DEFAULT NULL COMMENT '最新发布的版本号，NULL 为未发布',
    definition_id VARCHAR(64)           DEFAULT NULL COMMENT '最新发布的流程定义ID',
    deploy_time   DATETIME              DEFAULT NULL COMMENT '最近发布时间',
    design_json   MEDIUMTEXT COMMENT '流程设计（表单 + 节点树）JSON 草稿',
    remark        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time   DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_key (model_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '流程模型';

CREATE TABLE wf_copy (
    id                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    process_instance_id VARCHAR(64) NOT NULL COMMENT '流程实例ID',
    process_name        VARCHAR(255)         DEFAULT NULL COMMENT '流程标题（快照）',
    process_key         VARCHAR(64)          DEFAULT NULL COMMENT '流程标识',
    node_id             VARCHAR(64) NOT NULL COMMENT '抄送节点ID',
    node_name           VARCHAR(64)          DEFAULT NULL COMMENT '抄送节点名称',
    start_user_id       BIGINT               DEFAULT NULL COMMENT '发起人',
    user_id             BIGINT      NOT NULL COMMENT '被抄送人',
    create_time         DATETIME             DEFAULT NULL COMMENT '抄送时间',
    update_time         DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    -- 抄送我的：按用户查询，按 id 倒序
    KEY idx_copy_user_id (user_id, id),
    -- 流程详情中的抄送记录
    KEY idx_copy_instance_id (process_instance_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '流程抄送';

-- ---------------------------------------------------------------- 门店：区域、门店资料。仓库、结算账户、员工通过 store_id 归属门店，单据的门店就是其仓库的门店

CREATE TABLE store_region (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级区域，0 为顶级',
    code        VARCHAR(32)  NOT NULL COMMENT '区域编码',
    name        VARCHAR(64)  NOT NULL COMMENT '区域名称',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_region_code (code, tenant_id),
    KEY idx_region_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '区域（多级）';

CREATE TABLE store_info (
    id                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id            BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    code                 VARCHAR(32)   NOT NULL COMMENT '门店编码（编码规则 MD）',
    name                 VARCHAR(64)   NOT NULL COMMENT '门店名称',
    short_name           VARCHAR(32)            DEFAULT NULL COMMENT '简称',
    type                 VARCHAR(16)   NOT NULL COMMENT '门店类型(字典 store_type) HQ总部（每个租户唯一） DIRECT直营店 FRANCHISE加盟店 DC配送中心',
    region_id            BIGINT                 DEFAULT NULL COMMENT '所属区域',
    supply_store_id      BIGINT                 DEFAULT NULL COMMENT '上级配送门店（总部或配送中心），要货默认由它供货',
    price_scheme_id      BIGINT                 DEFAULT NULL COMMENT '零售价方案（scm_price_scheme），为空按总部价格',
    status               VARCHAR(16)   NOT NULL DEFAULT 'OPEN' COMMENT '状态(字典 store_status) PREPARING筹备 OPEN营业 SUSPENDED停业 CLOSED闭店；停业、闭店不能开单',
    contact              VARCHAR(64)            DEFAULT NULL COMMENT '联系人',
    phone                VARCHAR(32)            DEFAULT NULL COMMENT '电话',
    address              VARCHAR(255)           DEFAULT NULL COMMENT '地址',
    longitude            DECIMAL(10, 6)         DEFAULT NULL COMMENT '经度',
    latitude             DECIMAL(10, 6)         DEFAULT NULL COMMENT '纬度',
    business_hours       VARCHAR(64)            DEFAULT NULL COMMENT '营业时间，如 09:00-22:00',
    area                 DECIMAL(12, 2)         DEFAULT NULL COMMENT '营业面积（平方米），用于坪效',
    default_warehouse_id BIGINT                 DEFAULT NULL COMMENT '默认仓库（本门店的仓库）',
    default_account_id   BIGINT                 DEFAULT NULL COMMENT '默认结算账户（本门店的现金 / 银行末级科目）',
    dept_id              BIGINT                 DEFAULT NULL COMMENT '对应部门（可选）',
    franchisee           VARCHAR(64)            DEFAULT NULL COMMENT '加盟商名称（加盟店）',
    contract_start       DATE                   DEFAULT NULL COMMENT '加盟合同开始日期',
    contract_end         DATE                   DEFAULT NULL COMMENT '加盟合同结束日期',
    deposit              DECIMAL(18, 2)         DEFAULT NULL COMMENT '加盟保证金',
    settle_mode          VARCHAR(16)            DEFAULT NULL COMMENT '加盟结算方式(字典 store_settle_mode) PREPAID预付货款 CREDIT月结',
    delivery_scheme_id   BIGINT                 DEFAULT NULL COMMENT '配送价方案（加盟店向总部进货的价格），为空按价格列配送价',
    sort                 INT           NOT NULL DEFAULT 0 COMMENT '排序',
    remark               VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    create_time          DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time          DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_code (code, tenant_id),
    KEY idx_store_region (region_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '门店';

-- ---------------------------------------------------------------- 仓库管理

CREATE TABLE wms_warehouse (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(32)  NOT NULL COMMENT '仓库编码',
    name        VARCHAR(64)  NOT NULL COMMENT '仓库名称',
    type        VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT '仓库类型(字典 wms_warehouse_type) NORMAL普通 RAW原料 FINISHED成品 DEFECT不良品 CONSIGN_OUT客户寄售仓 CONSIGN_IN供应商寄售仓（不良品、寄售仓不计入可用库存，供应商寄售仓不计入存货成本）',
    partner_id  BIGINT                DEFAULT NULL COMMENT '寄售仓的客户（客户寄售仓）/ 供应商（供应商寄售仓）',
    store_id    BIGINT                DEFAULT NULL COMMENT '所属门店（store_info.id）：单据的门店就是其仓库的门店，按门店的数据权限据此过滤；已有库存或单据后不能修改',
    address     VARCHAR(255)          DEFAULT NULL COMMENT '地址',
    contact     VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    phone       VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_code (code, tenant_id),
    KEY idx_warehouse_store (store_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '仓库';

CREATE TABLE wms_location (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    warehouse_id BIGINT       NOT NULL COMMENT '所属仓库',
    code         VARCHAR(32)  NOT NULL COMMENT '库位编码，仓库内唯一，如 A-01-02',
    name         VARCHAR(64)           DEFAULT NULL COMMENT '库位名称',
    zone         VARCHAR(32)           DEFAULT NULL COMMENT '区域，如 A 区、冷藏区；拣货路径按 区域 → 排序 → 编码',
    loc_type     VARCHAR(16)  NOT NULL DEFAULT 'STORAGE' COMMENT '类型(字典 wms_location_type) STORAGE存储位 PICK拣货位 RECEIVE收货暂存 SHIP发货暂存 DEFECT不良品位',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序（拣货路径顺序）',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_location_code (warehouse_id, code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库位';

CREATE TABLE wms_material (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code           VARCHAR(64)   NOT NULL COMMENT '物料编码',
    name           VARCHAR(128)  NOT NULL COMMENT '物料名称',
    spec           VARCHAR(128)           DEFAULT NULL COMMENT '规格型号',
    unit           VARCHAR(16)            DEFAULT NULL COMMENT '计量单位',
    barcode        VARCHAR(64)            DEFAULT NULL COMMENT '条码（基本单位），扫码录入物料',
    category       VARCHAR(64)            DEFAULT NULL COMMENT '分类编码(wms_material_category.code，多级分类)',
    brand          VARCHAR(64)            DEFAULT NULL COMMENT '品牌(业务字典 wms_material_brand)',
    batch_enabled  TINYINT       NOT NULL DEFAULT 0 COMMENT '是否启用批次管理 1是 0否',
    serial_enabled TINYINT       NOT NULL DEFAULT 0 COMMENT '是否启用序列号管理 1是 0否',
    safety_stock   DECIMAL(18, 4)         DEFAULT NULL COMMENT '安全库存（库存下限）：按总库存预警时所有仓库的合计低于该值时预警；分仓库预警时为各仓库下限之和（保存时计算）',
    max_stock      DECIMAL(18, 4)         DEFAULT NULL COMMENT '库存上限：按总库存预警时所有仓库的合计高于该值时预警；分仓库预警时为各仓库上限之和（保存时计算）',
    alert_mode     VARCHAR(16)   NOT NULL DEFAULT 'TOTAL' COMMENT '库存预警方式(字典 wms_stock_alert_mode) TOTAL按总库存 WAREHOUSE分仓库（wms_material_warehouse_limit）',
    reorder_point  DECIMAL(18, 4)         DEFAULT NULL COMMENT '再订货点：预计库存（可用 + 在途 + 在制 − 待发 − 待领）不高于此值时建议补货，为空不参与',
    reorder_qty    DECIMAL(18, 4)         DEFAULT NULL COMMENT '再订货量（经济批量），补货建议不少于此数量',
    default_warehouse_id BIGINT       DEFAULT NULL COMMENT '默认仓库：录入出入库单、订单时默认带出',
    default_location_id  BIGINT       DEFAULT NULL COMMENT '默认库位（默认仓库中的）',
    ref_cost       DECIMAL(18, 4)         DEFAULT NULL COMMENT '参考成本价：没有核算单价时（首次核算的其他入库等）按此计价',
    retail_price      DECIMAL(18, 4)      DEFAULT NULL COMMENT '零售价（含税，基本单位，本位币）：价格列 0',
    wholesale_price1  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价1（含税）：价格列 1，名称在价格列设置中修改',
    wholesale_price2  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价2（含税）',
    wholesale_price3  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价3（含税）',
    wholesale_price4  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价4（含税）',
    wholesale_price5  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价5（含税）',
    wholesale_price6  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价6（含税）',
    wholesale_price7  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价7（含税）',
    wholesale_price8  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价8（含税）',
    is_template    TINYINT       NOT NULL DEFAULT 0 COMMENT '是否多规格款式 1是：按规格属性生成 SKU，款式本身不能出入库',
    template_id    BIGINT                 DEFAULT NULL COMMENT 'SKU 所属的款式（物料）',
    variant_text   VARCHAR(255)           DEFAULT NULL COMMENT 'SKU 的规格属性值，如 红色 / XL',
    image_url      VARCHAR(255)           DEFAULT NULL COMMENT '主图 URL（wms_material_image 排在第一张的图片），由图片维护自动更新',
    status         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark         VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    search_key   VARCHAR(1000)          DEFAULT NULL COMMENT '检索列：编码、名称、规格、条码去掉分隔符的小写串、数字串、拼音首字母（空格分隔），由程序计算，下拉和列表关键字匹配',
    create_time    DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_code (code, tenant_id),
    KEY idx_material_barcode (barcode),
    KEY idx_material_template (template_id),
    KEY idx_material_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料';

CREATE TABLE wms_material_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级分类ID，0为一级分类',
    ancestors   VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '祖级ID路径（逗号分隔，从 0 开始，如 0,3,7），查询下级分类用',
    code        VARCHAR(16)  NOT NULL COMMENT '分类编码：物料、促销、提成规则、凭证科目设置等按编码关联，保存后不能修改',
    name        VARCHAR(64)  NOT NULL COMMENT '分类名称（同一上级下不重复）',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '同级排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用（停用后连同下级不能再被新物料选用）',
    builtin     TINYINT      NOT NULL DEFAULT 0 COMMENT '内置分类 1是：程序依赖（原材料、半成品、成品、低值易耗品），不能删除',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_category_code (code, tenant_id),
    KEY idx_material_category_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料分类（多级，每个租户一份）';

CREATE TABLE wms_price_level (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    level       INT         NOT NULL COMMENT '价格列 0零售价 1-8批发价1-8（对应 wms_material.retail_price / wholesale_price1-8）',
    name        VARCHAR(32) NOT NULL COMMENT '显示名称',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用：停用的列在物料表单、列表、导入导出和价格选择中隐藏',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_level (level, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格列设置：物料的零售价、批发价1-8的名称和是否启用（按租户）';

CREATE TABLE wms_attribute (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(32) NOT NULL COMMENT '规格属性名称，如 颜色、尺码',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attribute_name (name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '规格属性：多规格物料（款式）按属性值组合生成 SKU';

CREATE TABLE wms_attribute_value (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    attribute_id BIGINT      NOT NULL COMMENT '规格属性',
    attr_value   VARCHAR(32) NOT NULL COMMENT '属性值，如 红色、XL',
    code         VARCHAR(16)          DEFAULT NULL COMMENT '编码，生成 SKU 编码时使用，为空用属性值',
    sort         INT         NOT NULL DEFAULT 0 COMMENT '排序',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attribute_value (attribute_id, attr_value, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '规格属性值';

CREATE TABLE wms_variant_value (
    id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    template_id  BIGINT NOT NULL COMMENT '款式',
    material_id  BIGINT NOT NULL COMMENT 'SKU',
    attribute_id BIGINT NOT NULL COMMENT '规格属性',
    value_id     BIGINT NOT NULL COMMENT '属性值',
    create_time  DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME        DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_variant_value (material_id, attribute_id, tenant_id),
    KEY idx_variant_template (template_id),
    KEY idx_variant_value (value_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SKU 的规格属性值';

CREATE TABLE wms_material_unit (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT         NOT NULL COMMENT '物料',
    unit        VARCHAR(16)    NOT NULL COMMENT '辅助单位，如 箱',
    ratio       DECIMAL(18, 6) NOT NULL COMMENT '换算率：1 辅助单位 = ratio 基本单位',
    barcode     VARCHAR(64)             DEFAULT NULL COMMENT '包装条码，扫码时按换算率带出数量',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_unit (material_id, unit, tenant_id),
    KEY idx_material_unit_barcode (barcode)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料辅助计量单位：库存、成本都按基本单位，单据可按辅助单位录入后换算';

CREATE TABLE wms_material_warehouse_limit (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    min_qty      DECIMAL(18, 4)          DEFAULT NULL COMMENT '库存下限（安全库存）：该仓库库存低于时预警，为空不检查',
    max_qty      DECIMAL(18, 4)          DEFAULT NULL COMMENT '库存上限：该仓库库存高于时预警，为空不检查',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_warehouse_limit (material_id, warehouse_id, tenant_id),
    KEY idx_material_limit_warehouse (warehouse_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料分仓库的库存上下限（物料的库存预警方式为分仓库时使用，没有设置的仓库不预警）';

CREATE TABLE wms_material_image (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT       NOT NULL COMMENT '物料',
    url         VARCHAR(255) NOT NULL COMMENT '图片访问 URL（公开文件 /api/files/material/...）',
    file_name   VARCHAR(255)          DEFAULT NULL COMMENT '原文件名',
    file_size   BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，最小的为主图',
    create_by   BIGINT                DEFAULT NULL COMMENT '上传人',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_material_image (material_id, sort)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料（商品）图片：排在第一张的为主图，同步到 wms_material.image_url';

CREATE TABLE wms_batch (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id     BIGINT      NOT NULL COMMENT '物料',
    batch_no        VARCHAR(64) NOT NULL COMMENT '批次号',
    production_date DATE                 DEFAULT NULL COMMENT '生产日期',
    expiry_date     DATE                 DEFAULT NULL COMMENT '有效期至',
    create_time     DATETIME             DEFAULT NULL COMMENT '首次入库时间',
    update_time     DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch (material_id, batch_no, tenant_id),
    -- 批次追踪：按批次号查询、临期预警
    KEY idx_batch_no (batch_no),
    KEY idx_batch_expiry (expiry_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '批次';

CREATE TABLE wms_stock (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定库位',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    batch_no     VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号，未启用批次管理为空串',
    qty          DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '数量，不会小于 0',
    frozen_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '冻结数量，不能出库',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock (warehouse_id, location_id, material_id, batch_no, tenant_id),
    KEY idx_stock_material (material_id, batch_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存';

CREATE TABLE wms_reservation (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    warehouse_id   BIGINT         NOT NULL COMMENT '仓库',
    material_id    BIGINT         NOT NULL COMMENT '物料',
    qty            DECIMAL(18, 4) NOT NULL COMMENT '剩余预留数量（出库核销、释放后减少，0 表示已结束）',
    source_type    VARCHAR(16)    NOT NULL COMMENT '来源类型 SALES销售订单',
    source_id      BIGINT         NOT NULL COMMENT '来源单据',
    source_item_id BIGINT                  DEFAULT NULL COMMENT '来源明细',
    source_no      VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by      BIGINT                  DEFAULT NULL COMMENT '预留人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_reservation_slot (warehouse_id, material_id),
    KEY idx_reservation_source (source_type, source_id),
    KEY idx_reservation_item (source_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存预留：仓库中可用库存不能低于全部剩余预留';

CREATE TABLE wms_serial (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id  BIGINT      NOT NULL COMMENT '物料',
    serial_no    VARCHAR(64) NOT NULL COMMENT '序列号',
    warehouse_id BIGINT      NOT NULL COMMENT '当前（或最后所在）仓库',
    location_id  BIGINT      NOT NULL DEFAULT 0 COMMENT '当前（或最后所在）库位，0 为未指定',
    batch_no     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号',
    status       VARCHAR(16) NOT NULL COMMENT '状态(字典 wms_serial_status) IN_STOCK在库 OUT已出库',
    create_time  DATETIME             DEFAULT NULL COMMENT '首次入库时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_serial (material_id, serial_no, tenant_id),
    KEY idx_serial_no (serial_no),
    -- 出库、调拨时选择某个库存位置上的在库序列号
    KEY idx_serial_slot (warehouse_id, location_id, material_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '序列号';

CREATE TABLE wms_stock_flow (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type     VARCHAR(16)    NOT NULL COMMENT '业务类型(字典 wms_biz_type) IN入库 OUT出库 TRANSFER调拨 CHECK盘点',
    doc_id       BIGINT         NOT NULL COMMENT '单据ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '单据号',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    batch_no     VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '变动数量，正数增加、负数减少',
    biz_price    DECIMAL(18, 6)          DEFAULT NULL COMMENT '业务单价（不含税，本位币）：采购入库、销售出库等来源单据的单价，没有为空',
    cost_price   DECIMAL(18, 6)          DEFAULT NULL COMMENT '成本单价（实时移动加权平均），供应商寄售仓的库存不计价为空',
    cost_amount  DECIMAL(18, 2)          DEFAULT NULL COMMENT '成本金额，与数量同号',
    partner_id   BIGINT                  DEFAULT NULL COMMENT '往来单位（供应商 / 客户）',
    partner_name VARCHAR(128)            DEFAULT NULL COMMENT '往来单位名称快照',
    cost_estimated TINYINT               NOT NULL DEFAULT 0 COMMENT '1 升级前的历史流水：成本按参考成本价回填，是近似值',
    create_by    BIGINT                  DEFAULT NULL COMMENT '操作人',
    create_time  DATETIME                DEFAULT NULL COMMENT '发生时间',
    PRIMARY KEY (id),
    -- 批次追踪、物料台账
    KEY idx_flow_material (material_id, batch_no),
    -- 动态盘点：物料在仓库某段时间内是否有过变动、最近变动时间（盘点单选择商品）
    KEY idx_flow_material_wh (material_id, warehouse_id, create_time, biz_type),
    KEY idx_flow_doc (biz_type, doc_id),
    KEY idx_flow_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存流水';

CREATE TABLE wms_material_cost (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT         NOT NULL COMMENT '物料',
    qty         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '计价数量：全部仓库（不含供应商寄售仓）的库存合计',
    amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '库存金额',
    unit_cost   DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '移动加权平均单价；数量为 0 时保留最后的单价',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wms_material_cost (material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料实时库存成本（移动加权平均，按物料不分仓库），随库存变动在同一事务中更新';

CREATE TABLE wms_serial_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id  BIGINT      NOT NULL COMMENT '物料',
    serial_no    VARCHAR(64) NOT NULL COMMENT '序列号',
    biz_type     VARCHAR(16) NOT NULL COMMENT '业务类型(字典 wms_biz_type)',
    doc_id       BIGINT      NOT NULL COMMENT '单据ID',
    doc_no       VARCHAR(32) NOT NULL COMMENT '单据号',
    direction    TINYINT     NOT NULL COMMENT '方向 1入 -1出',
    warehouse_id BIGINT      NOT NULL COMMENT '仓库',
    location_id  BIGINT      NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号',
    create_by    BIGINT               DEFAULT NULL COMMENT '操作人',
    create_time  DATETIME             DEFAULT NULL COMMENT '发生时间',
    PRIMARY KEY (id),
    -- 序列号追踪：按物料 + 序列号查询完整轨迹
    KEY idx_serial_log (material_id, serial_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '序列号轨迹';

CREATE TABLE wms_stock_doc (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no          VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind            VARCHAR(16)    NOT NULL COMMENT '单据种类 IN入库 OUT出库 TRANSFER调拨',
    biz_type        VARCHAR(32)             DEFAULT NULL COMMENT '入库类型(字典 wms_in_type) / 出库类型(字典 wms_out_type)，调拨为空',
    io_category     VARCHAR(32)             DEFAULT NULL COMMENT '收发类别：其他入库为字典 wms_in_category，其他出库、报废出库为字典 wms_out_category（业务字典），成本凭证按类别取对方科目；业务单据生成的为空',
    warehouse_id    BIGINT         NOT NULL COMMENT '仓库；调拨为调出仓库',
    to_warehouse_id BIGINT                  DEFAULT NULL COMMENT '调入仓库，仅调拨',
    biz_date        DATE           NOT NULL COMMENT '业务日期',
    status          VARCHAR(16)    NOT NULL COMMENT '状态(字典 wms_doc_status) DRAFT草稿 APPROVING审批中 TRANSIT在途（在途调拨已调出未调入） DONE已完成',
    total_qty       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    source_no       VARCHAR(32)             DEFAULT NULL COMMENT '来源单号：由采购入库、销售出库等业务单据自动生成时填写',
    in_transit      TINYINT        NOT NULL DEFAULT 0 COMMENT '在途调拨 1是：确认时只调出，调入仓库收货后才入库',
    receive_by      BIGINT                  DEFAULT NULL COMMENT '调入收货人',
    receive_date    DATE                    DEFAULT NULL COMMENT '调入收货日期：在途调拨收货的业务日期，调入流水按此日期统计',
    receive_time    DATETIME                DEFAULT NULL COMMENT '调入收货时间',
    red_of          BIGINT                  DEFAULT NULL COMMENT '红字冲销单：被冲销的原单据',
    reversed        TINYINT        NOT NULL DEFAULT 0 COMMENT '已被冲销 1是',
    partner_id      BIGINT                  DEFAULT NULL COMMENT '往来单位（业务单据生成时由来源单据传入）',
    partner_name    VARCHAR(128)            DEFAULT NULL COMMENT '往来单位名称快照',
    transfer_pricing VARCHAR(16)            DEFAULT NULL COMMENT '调拨价格（仅调拨） SAME同价调拨 REF_COST参考成本价 AVG_COST库存均价 MIN_SALE最低售价 PRICE_LEVEL价格列：差价调拨的快速计算依据，调拨价只是内部结算价，不影响存货成本',
    price_level     TINYINT                 DEFAULT NULL COMMENT '调拨价格依据为价格列时的价格列：0零售价 1-8批发价',
    price_factor    DECIMAL(10, 4)          DEFAULT NULL COMMENT '差价调拨的价格系数（调拨价 = 依据价 × 系数），同价调拨为空',
    cost_amount     DECIMAL(18, 2)          DEFAULT NULL COMMENT '调出成本金额（调拨确认时生成，红字为负数）',
    transfer_amount DECIMAL(18, 2)          DEFAULT NULL COMMENT '调拨金额（调拨价 × 数量合计，确认时生成）',
    fee             DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '调拨费用（配送费，本公司承担，计入当期费用），0为没有',
    fee_settle      VARCHAR(16)             DEFAULT NULL COMMENT '调拨费用付给承运商的方式(字典 scm_freight_settle) CASH现付 MONTHLY月结',
    fee_account_id  BIGINT                  DEFAULT NULL COMMENT '现付调拨费用的付款账户（现金 / 银行末级科目）',
    fee_carrier_id  BIGINT                  DEFAULT NULL COMMENT '调拨费用的承运商（供应商），月结时必填',
    fee_io_id       BIGINT                  DEFAULT NULL COMMENT '现付调拨费用生成的费用支出单',
    fee_bill_id     BIGINT                  DEFAULT NULL COMMENT '月结调拨费用生成的承运商应付单',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例（发布了出入库审批流程时，手工单据确认前审批）',
    dept_id         BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by       BIGINT                  DEFAULT NULL COMMENT '制单人',
    confirm_by      BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time    DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_doc_no (doc_no, tenant_id),
    -- 各单据列表：按种类查询，按 id 倒序
    KEY idx_stock_doc_kind (kind, id),
    KEY idx_stock_doc_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出入库单（入库、出库、调拨）';

CREATE TABLE wms_stock_doc_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id          BIGINT         NOT NULL COMMENT '单据ID',
    material_id     BIGINT         NOT NULL COMMENT '物料',
    warehouse_id    BIGINT                  DEFAULT NULL COMMENT '本行仓库（入库、出库），为空取表头仓库；调拨不用',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位；调拨为调出库位，0 为未指定',
    to_location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '调入库位，仅调拨',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期，仅入库新批次',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至，仅入库新批次',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    cost_price      DECIMAL(18, 6)          DEFAULT NULL COMMENT '成本单价：其他入库、期初入库填写的入库成本，业务单据传入的成本；为空按当前移动加权平均价',
    biz_price       DECIMAL(18, 6)          DEFAULT NULL COMMENT '业务单价（不含税，本位币），业务单据生成时传入',
    transfer_price  DECIMAL(18, 6)          DEFAULT NULL COMMENT '调拨价（调入成本价 / 内部结算价，仅调拨）：同价调拨确认时等于调出成本价，差价调拨可填写、不填按调出成本价',
    transfer_amount DECIMAL(18, 2)          DEFAULT NULL COMMENT '调拨金额 = 数量 × 调拨价（确认时生成）',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_stock_doc_item_doc (doc_id),
    -- 删除物料前检查是否被单据引用
    KEY idx_stock_doc_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出入库单明细';

CREATE TABLE wms_assembly (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    assembly_no     VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind            VARCHAR(16)    NOT NULL COMMENT '种类(字典 wms_assembly_kind) ASSEMBLE组装 DISASSEMBLE拆卸',
    warehouse_id    BIGINT         NOT NULL COMMENT '仓库',
    biz_date        DATE           NOT NULL COMMENT '业务日期',
    material_id     BIGINT         NOT NULL COMMENT '成套件（组装得到 / 拆卸的物料）',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '成套件库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '成套件批次号',
    production_date DATE                    DEFAULT NULL COMMENT '成套件生产日期（组装入库新批次）',
    expiry_date     DATE                    DEFAULT NULL COMMENT '成套件有效期至（组装入库新批次）',
    serial_nos      TEXT COMMENT '成套件序列号，逗号分隔',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '成套件数量',
    status          VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿 DONE已完成',
    out_doc_id      BIGINT                  DEFAULT NULL COMMENT '生成的出库单（组装为子件出库，拆卸为成套件出库）',
    out_doc_no      VARCHAR(32)             DEFAULT NULL COMMENT '生成的出库单号',
    in_doc_id       BIGINT                  DEFAULT NULL COMMENT '生成的入库单（组装为成套件入库，拆卸为子件入库）',
    in_doc_no       VARCHAR(32)             DEFAULT NULL COMMENT '生成的入库单号',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id         BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by       BIGINT                  DEFAULT NULL COMMENT '制单人',
    confirm_by      BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time    DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assembly_no (assembly_no, tenant_id),
    KEY idx_assembly_date (biz_date),
    KEY idx_assembly_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组装拆卸单：组装消耗子件得到成套件，拆卸相反；确认后生成出库、入库单（类型 ASSEMBLY），成本核算按子件成本计算成套件成本';

CREATE TABLE wms_assembly_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    assembly_id     BIGINT         NOT NULL COMMENT '组装拆卸单',
    material_id     BIGINT         NOT NULL COMMENT '子件',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期（拆卸入库新批次）',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至（拆卸入库新批次）',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_assembly_item_assembly (assembly_id),
    KEY idx_assembly_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组装拆卸单的子件';

CREATE TABLE wms_check (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)  NOT NULL COMMENT '单据号',
    warehouse_id BIGINT       NOT NULL COMMENT '盘点仓库',
    biz_date     DATE         NOT NULL COMMENT '盘点日期',
    status       VARCHAR(16)  NOT NULL COMMENT '状态(字典 wms_doc_status) DRAFT草稿 APPROVING审批中（盘点差异审批） DONE已完成',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    freeze       TINYINT      NOT NULL DEFAULT 0 COMMENT '冻结仓库 1是：盘点完成前该仓库不能出入库、调拨',
    flow_instance_id VARCHAR(64)        DEFAULT NULL COMMENT '差异审批流程实例',
    dept_id      BIGINT                DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                DEFAULT NULL COMMENT '制单人',
    confirm_by   BIGINT                DEFAULT NULL COMMENT '确认人',
    confirm_time DATETIME              DEFAULT NULL COMMENT '确认时间',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_check_doc_no (doc_no, tenant_id),
    KEY idx_check_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '盘点单';

CREATE TABLE wms_check_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    check_id    BIGINT         NOT NULL COMMENT '盘点单ID',
    material_id BIGINT         NOT NULL COMMENT '物料',
    location_id BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no    VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    book_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '账面数量：草稿为保存时的库存，完成后为确认时的库存',
    actual_qty  DECIMAL(18, 4)          DEFAULT NULL COMMENT '实盘数量，草稿中可为空',
    serial_nos  TEXT COMMENT '实盘序列号，逗号分隔（序列号物料）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    cost_price  DECIMAL(18, 6)          DEFAULT NULL COMMENT '盘盈的成本单价，为空按确认时的当前均价；盘亏按当前均价',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_check_item_check (check_id),
    KEY idx_check_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '盘点单明细';

CREATE TABLE wms_doc_sequence (
    seq_key   VARCHAR(32) NOT NULL COMMENT '单据号前缀 + 日期，如 RK20261014',
    seq_value BIGINT      NOT NULL COMMENT '当天已使用的最大流水号',
    tenant_id BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (seq_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '单据号流水';

-- ---------------------------------------------------------------- 供应链管理

CREATE TABLE scm_supplier (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code         VARCHAR(32)  NOT NULL COMMENT '供应商编码',
    name         VARCHAR(128) NOT NULL COMMENT '供应商名称',
    level        VARCHAR(16)           DEFAULT NULL COMMENT '等级(字典 scm_supplier_level) A/B/C/D',
    contact      VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    phone        VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    email        VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    address      VARCHAR(255)          DEFAULT NULL COMMENT '地址',
    tax_no       VARCHAR(32)           DEFAULT NULL COMMENT '纳税人识别号',
    bank_name    VARCHAR(128)          DEFAULT NULL COMMENT '开户行',
    bank_account VARCHAR(64)           DEFAULT NULL COMMENT '银行账号',
    payment_days INT          NOT NULL DEFAULT 0 COMMENT '账期（天），应付单到期日 = 单据日期 + 账期',
    logistics    TINYINT      NOT NULL DEFAULT 0 COMMENT '物流公司（承运商） 1是 0否：运费的承运商只列出物流公司',
    routes       VARCHAR(1000)         DEFAULT NULL COMMENT '营运路线（物流公司），可写途经站点，如 青岛北—青岛西—诸城—济南',
    customer_id  BIGINT                DEFAULT NULL COMMENT '关联客户（同一单位既是供应商又是客户，一对一）：查询应收时可挂接该供应商的应付，可做应收应付对冲',
    merged_to    BIGINT                DEFAULT NULL COMMENT '已合并到的供应商（资料合并后本资料停用，业务数据已改为指向该供应商）',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    search_key   VARCHAR(1000)          DEFAULT NULL COMMENT '检索列：编码、名称、联系人、电话去掉分隔符的小写串、数字串、拼音首字母（空格分隔），由程序计算，下拉和列表关键字匹配',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_code (code, tenant_id),
    UNIQUE KEY uk_supplier_customer (customer_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商';

CREATE TABLE scm_supplier_price (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    supplier_id BIGINT         NOT NULL COMMENT '供应商',
    material_id BIGINT         NOT NULL COMMENT '物料',
    price       DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate    DECIMAL(5, 2)  NOT NULL DEFAULT 13 COMMENT '税率（%）',
    lead_days   INT            NOT NULL DEFAULT 0 COMMENT '采购提前期（天）',
    min_qty     DECIMAL(18, 4)          DEFAULT NULL COMMENT '最小起订量',
    is_default  TINYINT        NOT NULL DEFAULT 0 COMMENT '是否该物料的默认供应商 1是 0否，补货建议按默认供应商生成采购订单',
    valid_from  DATE                    DEFAULT NULL COMMENT '有效期起，为空不限',
    valid_to    DATE                    DEFAULT NULL COMMENT '有效期止，过期后开单不再取该价格',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_price (supplier_id, material_id, tenant_id),
    KEY idx_supplier_price_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供货价格';

CREATE TABLE scm_supplier_price_tier (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    price_id    BIGINT         NOT NULL COMMENT '供货价格',
    min_qty     DECIMAL(18, 4) NOT NULL COMMENT '起订数量（达到该数量适用本档单价）',
    price       DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_price_tier_price (price_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供货价格阶梯价';

CREATE TABLE scm_price_list (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id    BIGINT         NOT NULL COMMENT '物料',
    customer_id    BIGINT                  DEFAULT NULL COMMENT '客户专属价：指定客户时只适用该客户（优先于客户等级）',
    customer_level VARCHAR(16)             DEFAULT NULL COMMENT '客户等级(字典 scm_customer_level)，为空适用全部客户',
    min_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '起订数量',
    price          DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate       DECIMAL(5, 2)  NOT NULL DEFAULT 13 COMMENT '税率（%）',
    valid_from     DATE                    DEFAULT NULL COMMENT '有效期起',
    valid_to       DATE                    DEFAULT NULL COMMENT '有效期止',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_price_list_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售价目表：取价时优先匹配客户专属价，其次客户等级，再取满足起订数量的最大一档';

CREATE TABLE scm_price_control (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id        BIGINT         NOT NULL COMMENT '物料',
    min_sale_price     DECIMAL(18, 4)          DEFAULT NULL COMMENT '最低销售价（含税，基本单位，本位币），为空不控制',
    max_purchase_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '最高采购价（含税，基本单位，本位币），为空不控制',
    control_mode       VARCHAR(8)     NOT NULL DEFAULT 'WARN' COMMENT '控制方式(字典 scm_price_control_mode) WARN提示 BLOCK禁止保存',
    remark             VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time        DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_control (material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格控制：销售订单单价不低于最低销售价，采购订单单价不高于最高采购价（手工录入的订单）';

-- 门店价格方案（一组商品零售价 / 会员价覆盖，门店绑定方案）、调价单（立即或定时生效，保留调价历史）
CREATE TABLE scm_price_scheme (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(32)  NOT NULL COMMENT '方案编码（留空按编码规则 JGFA 生成）',
    name        VARCHAR(64)  NOT NULL COMMENT '方案名称',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(字典 sys_common_status) 1启用 0停用：停用的方案不参与取价、不能被门店绑定',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_scheme_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '门店价格方案：一组商品的价格覆盖，门店绑定一个方案（store_info.price_scheme_id），开单时方案价优先于总部零售价';

CREATE TABLE scm_price_scheme_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    scheme_id    BIGINT         NOT NULL COMMENT '价格方案',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    retail_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '零售价（含税、基本单位、本位币），为空按总部零售价',
    member_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '会员价，为空不覆盖',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_scheme_item (scheme_id, material_id, tenant_id),
    KEY idx_price_scheme_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格方案明细：方案 × 物料的价格覆盖';

CREATE TABLE scm_price_adjust (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    adjust_no        VARCHAR(32)  NOT NULL COMMENT '调价单号（编码规则 TJ）',
    target           VARCHAR(16)  NOT NULL DEFAULT 'HQ' COMMENT '调价对象(字典 scm_price_adjust_target) HQ总部价 SCHEME价格方案（方案见 scm_price_adjust_scheme）',
    effective_time   DATETIME              DEFAULT NULL COMMENT '定时生效时间，为空审核后立即生效',
    status           VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 scm_price_adjust_status) DRAFT草稿 APPROVING审批中 APPROVED待生效 EFFECTIVE已生效 VOIDED已作废',
    reason           VARCHAR(255)          DEFAULT NULL COMMENT '调价原因',
    flow_instance_id VARCHAR(64)           DEFAULT NULL COMMENT '审批流程实例ID',
    approve_time     DATETIME              DEFAULT NULL COMMENT '审核时间',
    effect_time      DATETIME              DEFAULT NULL COMMENT '实际生效时间',
    create_by        BIGINT                DEFAULT NULL COMMENT '制单人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_adjust_no (adjust_no, tenant_id),
    KEY idx_price_adjust_due (status, effective_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '调价单：调整总部零售价或门店价格方案，审核后立即或定时生效，生效时记录调价历史';

CREATE TABLE scm_price_adjust_scheme (
    id        BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    adjust_id BIGINT NOT NULL COMMENT '调价单',
    scheme_id BIGINT NOT NULL COMMENT '价格方案',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_adjust_scheme (adjust_id, scheme_id, tenant_id),
    KEY idx_price_adjust_scheme_scheme (scheme_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '调价单调整的价格方案（调价对象为价格方案时）';

CREATE TABLE scm_price_adjust_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    adjust_id    BIGINT         NOT NULL COMMENT '调价单',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    retail_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '新零售价，为空不调整',
    member_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '新会员价（只用于价格方案），为空不调整',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_price_adjust_item_adjust (adjust_id),
    KEY idx_price_adjust_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '调价单明细';

CREATE TABLE scm_price_history (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    adjust_id        BIGINT         NOT NULL COMMENT '调价单',
    material_id      BIGINT         NOT NULL COMMENT '物料',
    target           VARCHAR(16)    NOT NULL COMMENT '调价对象(字典 scm_price_adjust_target)',
    scheme_id        BIGINT                  DEFAULT NULL COMMENT '价格方案，总部价为空',
    old_retail_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '调整前零售价',
    new_retail_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '调整后零售价',
    old_member_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '调整前会员价',
    new_member_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '调整后会员价',
    effect_time      DATETIME       NOT NULL COMMENT '生效时间',
    PRIMARY KEY (id),
    KEY idx_price_history_material (material_id, effect_time),
    KEY idx_price_history_adjust (adjust_id),
    KEY idx_price_history_scheme (scheme_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '调价历史：调价单生效时每个物料（价格方案时每个方案 × 物料）一行，只追加';

CREATE TABLE scm_customer_price (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    customer_id BIGINT         NOT NULL COMMENT '客户',
    material_id BIGINT         NOT NULL COMMENT '物料',
    price       DECIMAL(18, 4) NOT NULL COMMENT '执行价（含税，基本单位，本位币）',
    tax_rate    DECIMAL(5, 2)           DEFAULT NULL COMMENT '税率（%），为空时开单不改税率',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_price (customer_id, material_id, tenant_id),
    KEY idx_customer_price_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户执行价：开单默认价最优先';

CREATE TABLE scm_price_memory (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    kind        VARCHAR(16)    NOT NULL COMMENT '种类 SALES销售（销售出库） PURCHASE采购（采购入库）',
    partner_id  BIGINT         NOT NULL COMMENT '客户 / 供应商',
    material_id BIGINT         NOT NULL COMMENT '物料',
    last_date   DATE           NOT NULL COMMENT '最新日期',
    last_price  DECIMAL(18, 4) NOT NULL COMMENT '最新价格（含税，基本单位，本位币）',
    min_date    DATE           NOT NULL COMMENT '最低价日期',
    min_price   DECIMAL(18, 4) NOT NULL COMMENT '最低价格',
    max_date    DATE           NOT NULL COMMENT '最高价日期',
    max_price   DECIMAL(18, 4) NOT NULL COMMENT '最高价格',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_memory (kind, partner_id, material_id, tenant_id),
    KEY idx_price_memory_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格记忆：销售出库、采购入库确认后登记往来单位 × 物料的最新 / 最低 / 最高价';

CREATE TABLE scm_customer (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code         VARCHAR(32)    NOT NULL COMMENT '客户编码',
    name         VARCHAR(128)   NOT NULL COMMENT '客户名称',
    level        VARCHAR(16)             DEFAULT NULL COMMENT '客户等级(字典 scm_customer_level)',
    price_level  INT            NOT NULL DEFAULT 0 COMMENT '价格等级：开单默认价取物料的价格列 0零售价 1-8批发价1-8',
    contact      VARCHAR(64)             DEFAULT NULL COMMENT '联系人',
    phone        VARCHAR(32)             DEFAULT NULL COMMENT '联系电话',
    address      VARCHAR(255)            DEFAULT NULL COMMENT '收货地址',
    tax_no       VARCHAR(32)             DEFAULT NULL COMMENT '纳税人识别号',
    credit_limit DECIMAL(18, 2)          DEFAULT NULL COMMENT '信用额度，审核销售订单时校验未完成订单金额',
    payment_days INT            NOT NULL DEFAULT 0 COMMENT '账期（天），应收单到期日 = 单据日期 + 账期',
    channel      VARCHAR(16)    NOT NULL DEFAULT 'DIRECT' COMMENT '渠道(字典 scm_channel) DIRECT直销 DEALER经销商',
    region       VARCHAR(128)            DEFAULT NULL COMMENT '区域(业务字典 scm_region)，经销商为授权区域，进销存报表按区域汇总',
    owner_id     BIGINT                  DEFAULT NULL COMMENT '负责销售：新建销售订单时的默认业务员',
    merged_to    BIGINT                  DEFAULT NULL COMMENT '已合并到的客户（资料合并后本资料停用，业务数据已改为指向该客户）',
    status       TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    search_key   VARCHAR(1000)          DEFAULT NULL COMMENT '检索列：编码、名称、联系人、电话去掉分隔符的小写串、数字串、拼音首字母（空格分隔），由程序计算，下拉和列表关键字匹配',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户';

CREATE TABLE scm_order (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_no         VARCHAR(32)    NOT NULL COMMENT '订单号',
    kind             VARCHAR(16)    NOT NULL COMMENT '订单种类 PURCHASE采购 SALES销售',
    partner_id       BIGINT         NOT NULL COMMENT '供应商 / 客户',
    order_date       DATE           NOT NULL COMMENT '订单日期',
    delivery_date    DATE                    DEFAULT NULL COMMENT '交货日期',
    warehouse_id     BIGINT                  DEFAULT NULL COMMENT '默认收货 / 发货仓库',
    status           VARCHAR(16)    NOT NULL COMMENT '状态(字典 scm_order_status)',
    total_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    total_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计',
    tax_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    source           VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源(字典 scm_order_source) MANUAL手工 REORDER补货建议 DIRECT直接开单（隐式订单）',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例ID',
    discount_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '促销优惠合计（原价 - 成交价），已从金额中扣除',
    whole_discount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '整单优惠（含税，含抹零）：促销之后按行金额分摊到各行金额，已从金额中扣除',
    invoice_kind     VARCHAR(16)             DEFAULT NULL COMMENT '发票类型(业务字典 scm_invoice_kind)：明细未填写税率时取它的税率；出入库单显示所属订单的发票类型',
    close_adjust     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '关闭时的尾差调整（订单币种）：部分出入库的行按 行金额 × 已出入库数量 / 订单数量 调整已出入库金额，生成应收 / 应付调整单',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    version          INT            NOT NULL DEFAULT 1 COMMENT '版本：每次变更单生效后加 1',
    currency         VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种：订单单价、金额按此币种',
    exchange_rate    DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率（1 外币 = 汇率 × 本位币），出入库、应收应付按此折算本位币',
    salesman_id      BIGINT                  DEFAULT NULL COMMENT '业务员（销售订单，计算销售提成）：默认客户的负责销售，没有时为制单人',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by        BIGINT                  DEFAULT NULL COMMENT '制单人',
    approve_by       BIGINT                  DEFAULT NULL COMMENT '审核人',
    approve_time     DATETIME                DEFAULT NULL COMMENT '审核时间',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no, tenant_id),
    KEY idx_order_kind (kind, id),
    KEY idx_order_partner (partner_id),
    KEY idx_order_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购 / 销售订单';

CREATE TABLE scm_order_change (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    change_no        VARCHAR(32)  NOT NULL COMMENT '变更单号',
    order_id         BIGINT       NOT NULL COMMENT '订单',
    version          INT          NOT NULL COMMENT '生效后的订单版本',
    reason           VARCHAR(255) NOT NULL COMMENT '变更原因',
    status           VARCHAR(16)  NOT NULL DEFAULT 'APPROVING' COMMENT '状态(字典 scm_change_status) APPROVING审批中 APPROVED已生效 REJECTED已驳回',
    old_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '变更前价税合计',
    new_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '变更后价税合计',
    old_whole_discount DECIMAL(18, 2)       DEFAULT NULL COMMENT '变更前的整单优惠',
    new_whole_discount DECIMAL(18, 2)       DEFAULT NULL COMMENT '变更后的整单优惠（按变更后未出入库部分重新分摊）',
    flow_instance_id VARCHAR(64)           DEFAULT NULL COMMENT '审批流程实例',
    approve_time     DATETIME              DEFAULT NULL COMMENT '生效时间',
    create_by        BIGINT                DEFAULT NULL COMMENT '申请人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_change_no (change_no, tenant_id),
    KEY idx_order_change_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单变更单：已审核订单的数量、单价、交期变更，审批通过后生效并升版本';

CREATE TABLE scm_order_change_item (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    change_id         BIGINT         NOT NULL COMMENT '变更单',
    action            VARCHAR(8)     NOT NULL COMMENT '操作 ADD新增 MODIFY修改 REMOVE删除',
    order_item_id     BIGINT                  DEFAULT NULL COMMENT '订单明细（新增行为空，生效后回填）',
    material_id       BIGINT         NOT NULL COMMENT '物料',
    old_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '原数量',
    new_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '新数量',
    old_price         DECIMAL(18, 4)          DEFAULT NULL COMMENT '原单价',
    new_price         DECIMAL(18, 4)          DEFAULT NULL COMMENT '新单价',
    tax_rate          DECIMAL(5, 2)           DEFAULT NULL COMMENT '税率（新增行）',
    old_delivery_date DATE                    DEFAULT NULL COMMENT '原交货日期',
    new_delivery_date DATE                    DEFAULT NULL COMMENT '新交货日期',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_change_item_change (change_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单变更明细';

CREATE TABLE scm_order_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id      BIGINT         NOT NULL COMMENT '订单ID',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '订购数量',
    unit_name     VARCHAR(16)             DEFAULT NULL COMMENT '录入单位（辅助单位），为空表示基本单位；数量、单价均按基本单位保存',
    unit_ratio    DECIMAL(18, 6)          DEFAULT NULL COMMENT '录入单位的换算率',
    price         DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate      DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '税率（%）',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '价税合计',
    tax_amount    DECIMAL(18, 2) NOT NULL COMMENT '税额',
    done_qty      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已入库 / 已出库数量',
    return_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已退货数量',
    replenish_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '退货后需要补发（补收）的数量，计入未完成数量',
    done_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已出入库金额（订单币种，补发的退货扣减）：最后一批取 行金额 − 已出入库金额',
    return_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已退款的退货金额（订单币种，不补发的退货）',
    discount_share DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '分摊到本行的整单优惠（行金额已扣除）',
    done_share    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已出入库部分已分摊的整单优惠（订单变更时不变，剩余的重新分摊）',
    close_adjust  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '订单关闭时本行的尾差调整（订单币种，已计入已出入库金额）',
    close_adjust_net DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '尾差调整的不含税金额（本位币）：凭证按物料分类取数',
    orig_price    DECIMAL(18, 4)          DEFAULT NULL COMMENT '原价（促销前的含税单价），没有优惠时为空',
    promotion_id  BIGINT                  DEFAULT NULL COMMENT '应用的折扣促销 / 赠品来源的买赠促销',
    gift          TINYINT        NOT NULL DEFAULT 0 COMMENT '1 赠品（单价为 0，由买赠促销自动生成）',
    delivery_date DATE                    DEFAULT NULL COMMENT '交货日期',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细';

-- 促销：按期间、客户等级、物料 / 分类设置折扣、满减、买赠，手工新建 / 修改销售订单时自动应用（先折扣，再满减）
CREATE TABLE scm_promotion (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)    NOT NULL COMMENT '促销名称',
    promo_type        VARCHAR(16)    NOT NULL COMMENT '类型(字典 scm_promo_type) DISCOUNT折扣 REDUCE满减 GIFT买赠',
    begin_date        DATE           NOT NULL COMMENT '开始日期（按订单日期匹配）',
    end_date          DATE           NOT NULL COMMENT '结束日期',
    customer_level    VARCHAR(16)             DEFAULT NULL COMMENT '客户等级(字典 scm_customer_level)，为空适用全部客户',
    material_id       BIGINT                  DEFAULT NULL COMMENT '适用物料，为空按分类',
    material_category VARCHAR(16)             DEFAULT NULL COMMENT '适用物料分类编码(wms_material_category.code，含下级分类)，物料和分类都为空适用全部物料',
    min_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '折扣：明细数量达到时才打折',
    discount_rate     DECIMAL(5, 2)           DEFAULT NULL COMMENT '折扣：成交价 = 原价 × 折扣率%，如 90 为九折',
    threshold_amount  DECIMAL(18, 2)          DEFAULT NULL COMMENT '满减：适用物料的金额（折扣后）达到此金额',
    reduce_amount     DECIMAL(18, 2)          DEFAULT NULL COMMENT '满减：减免金额，按行金额分摊到单价',
    buy_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '买赠：适用物料每购买此数量',
    gift_material_id  BIGINT                  DEFAULT NULL COMMENT '买赠：赠品物料',
    gift_qty          DECIMAL(18, 4)          DEFAULT NULL COMMENT '买赠：每次赠送数量',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    create_by         BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_promotion_date (end_date, begin_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '促销活动';

CREATE TABLE scm_promotion_usage (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    promotion_id    BIGINT         NOT NULL COMMENT '促销',
    order_id        BIGINT         NOT NULL COMMENT '销售订单',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    gift_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '赠品数量',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_promotion_usage (promotion_id),
    KEY idx_promotion_usage_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '促销应用记录（订单保存时重算）';

CREATE TABLE scm_trade_doc (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind         VARCHAR(16)    NOT NULL COMMENT '种类(字典 scm_trade_kind) PURCHASE_IN采购入库 PURCHASE_RETURN采购退货 SALES_OUT销售出库 SALES_RETURN销售退货',
    order_id     BIGINT         NOT NULL COMMENT '订单ID',
    partner_id   BIGINT         NOT NULL COMMENT '供应商 / 客户',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    biz_date     DATE           NOT NULL COMMENT '业务日期',
    total_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计',
    tax_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    currency     VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '订单币种',
    exchange_rate DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率（取订单汇率）；单据金额为本位币',
    amount_fc    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计（原币）',
    wms_doc_id   BIGINT                  DEFAULT NULL COMMENT '生成的出入库单ID',
    wms_doc_no   VARCHAR(32)             DEFAULT NULL COMMENT '生成的出入库单号',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    replenish    TINYINT        NOT NULL DEFAULT 0 COMMENT '退货是否补发（补收） 1补发 0退款',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本单分摊的整单优惠（本位币，含税）：优惠前合计 = 价税合计 + 本项',
    discount_adjust DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '采购入库修改的订单优惠与按订单分摊之差（本位币，含税），0为与订单一致',
    freight      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '运费，0为没有',
    freight_bearer VARCHAR(16)           DEFAULT NULL COMMENT '运费承担(字典 scm_freight_bearer)：采购入库 SUPPLIER供应商代垫 COMPANY本公司承担；销售出库 COMPANY本公司承担 CUSTOMER代垫运费（客户承担）；销售退货 CUSTOMER客户代垫；采购退货 SUPPLIER代垫运费（供应商承担）',
    freight_cost VARCHAR(16)             DEFAULT NULL COMMENT '采购运费入账方式 INVENTORY计入存货成本 EXPENSE计入当期费用（本公司承担时按业务参数）',
    carrier_supplier_id BIGINT           DEFAULT NULL COMMENT '运费的承运商（供应商）：采购入库本公司承担、月结时必填',
    freight_bill_id BIGINT               DEFAULT NULL COMMENT '运费应付单（采购入库运费应付、月结的承运商应付）',
    freight_settle VARCHAR(16)           DEFAULT NULL COMMENT '付给承运商的方式(字典 scm_freight_settle，销售出库、采购退货) CASH现付 MONTHLY月结',
    freight_account_id BIGINT            DEFAULT NULL COMMENT '现付运费的付款账户（现金 / 银行末级科目）',
    freight_partner_bill_id BIGINT       DEFAULT NULL COMMENT '往来单位的代垫运费单据：销售出库代垫运费应收、销售退货红字应收、采购退货红字应付',
    freight_io_id BIGINT                 DEFAULT NULL COMMENT '现付运费生成的费用支出单',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    post_by      BIGINT                  DEFAULT NULL COMMENT '审核人（保存即审核时为制单人，草稿审核时为审核人）',
    post_time    DATETIME                DEFAULT NULL COMMENT '审核时间（出入库、生成应收应付的时间）',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_doc_no (doc_no, tenant_id),
    KEY idx_trade_doc_kind (kind, id),
    KEY idx_trade_doc_order (order_id),
    KEY idx_trade_doc_date (biz_date),
    KEY idx_trade_doc_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 采购退货 / 销售出库 / 销售退货';

CREATE TABLE scm_trade_doc_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id          BIGINT         NOT NULL COMMENT '单据ID',
    order_item_id   BIGINT         NOT NULL COMMENT '订单明细ID（直接开单为隐式订单的明细）',
    material_id     BIGINT         NOT NULL COMMENT '物料',
    warehouse_id    BIGINT                  DEFAULT NULL COMMENT '本行仓库（仅销售出库可选），为空取表头仓库',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期，仅采购入库',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至，仅采购入库',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    price           DECIMAL(18, 4) NOT NULL COMMENT '含税单价（取订单，已扣除整单优惠的分摊）',
    tax_rate        DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '税率（%）',
    amount          DECIMAL(18, 2) NOT NULL COMMENT '价税合计',
    tax_amount      DECIMAL(18, 2) NOT NULL COMMENT '税额',
    gross_price     DECIMAL(18, 4)          DEFAULT NULL COMMENT '优惠前含税单价（整单优惠分摊前，本位币），为空同单价',
    discount_share  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本行分摊的整单优惠（本位币，含税）',
    freight_share   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本行分摊的采购运费（本位币，计入存货成本）',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_trade_doc_item_doc (doc_id),
    KEY idx_trade_doc_item_order_item (order_item_id),
    KEY idx_trade_doc_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 退货、销售出库 / 退货明细';

CREATE TABLE scm_trade_draft (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '单据号（审核后沿用为出入库单号）',
    kind         VARCHAR(16)    NOT NULL COMMENT '种类(字典 scm_trade_kind) PURCHASE_IN采购入库 PURCHASE_RETURN采购退货 SALES_OUT销售出库 SALES_RETURN销售退货',
    order_id     BIGINT                  DEFAULT NULL COMMENT '引用的订单，直接开单为空',
    source_doc_id BIGINT                 DEFAULT NULL COMMENT '退货引用的原出入库单（仅用于表单回显）',
    partner_id   BIGINT         NOT NULL COMMENT '供应商 / 客户',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    biz_date     DATE           NOT NULL COMMENT '业务日期',
    salesman_id  BIGINT                  DEFAULT NULL COMMENT '直接开单的销售出库：业务员',
    replenish    TINYINT        NOT NULL DEFAULT 0 COMMENT '退货是否补发（补收） 1补发 0退款',
    whole_discount DECIMAL(18, 2)        DEFAULT NULL COMMENT '直接开单的整单优惠（含税）',
    invoice_kind   VARCHAR(16)           DEFAULT NULL COMMENT '直接开单的发票类型(业务字典 scm_invoice_kind)，审核时记在隐式订单上；引用订单时为空（取订单的）',
    order_discount DECIMAL(18, 2)        DEFAULT NULL COMMENT '引用订单的采购入库：本单的订单优惠（含税，订单币种），为空按订单分摊',
    freight      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '运费',
    freight_bearer VARCHAR(16)           DEFAULT NULL COMMENT '运费承担(字典 scm_freight_bearer)，含义同出入库单',
    carrier_supplier_id BIGINT           DEFAULT NULL COMMENT '运费的承运商（供应商）',
    freight_settle VARCHAR(16)           DEFAULT NULL COMMENT '付给承运商的方式(字典 scm_freight_settle) CASH现付 MONTHLY月结',
    freight_account_id BIGINT            DEFAULT NULL COMMENT '现付运费的付款账户',
    total_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计（按录入单价估算，审核后以出入库单为准）',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_draft_no (doc_no, tenant_id),
    KEY idx_trade_draft_kind (kind, id),
    KEY idx_trade_draft_order (order_id),
    KEY idx_trade_draft_partner (partner_id),
    KEY idx_trade_draft_warehouse (warehouse_id),
    KEY idx_trade_draft_dept (dept_id),
    KEY idx_trade_draft_create_by (create_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 退货、销售出库 / 退货的草稿（不影响库存、往来和凭证，审核后生成出入库单并删除）';

CREATE TABLE scm_trade_draft_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    draft_id        BIGINT         NOT NULL COMMENT '草稿ID',
    order_item_id   BIGINT                  DEFAULT NULL COMMENT '订单明细ID，直接开单为空',
    material_id     BIGINT                  DEFAULT NULL COMMENT '物料（引用订单时为订单明细的物料）',
    warehouse_id    BIGINT                  DEFAULT NULL COMMENT '本行仓库（仅销售出库可选），为空取表头仓库',
    location_id     BIGINT                  DEFAULT NULL COMMENT '库位',
    batch_no        VARCHAR(64)             DEFAULT NULL COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期，仅采购入库',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至，仅采购入库',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    price           DECIMAL(18, 4)          DEFAULT NULL COMMENT '直接开单：含税单价',
    tax_rate        DECIMAL(5, 2)           DEFAULT NULL COMMENT '直接开单：税率（%）',
    gift            TINYINT        NOT NULL DEFAULT 0 COMMENT '直接开单的销售出库：1 买赠的赠品行',
    unit_name       VARCHAR(16)             DEFAULT NULL COMMENT '直接开单：录入时使用的单位（仅显示）',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    sort            INT            NOT NULL DEFAULT 0 COMMENT '行号',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_trade_draft_item_draft (draft_id),
    KEY idx_trade_draft_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 退货、销售出库 / 退货草稿的明细';

CREATE TABLE scm_delivery (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    delivery_no  VARCHAR(32)  NOT NULL COMMENT '发货通知单号',
    order_id     BIGINT       NOT NULL COMMENT '销售订单',
    customer_id  BIGINT       NOT NULL COMMENT '客户',
    warehouse_id BIGINT       NOT NULL COMMENT '发货仓库',
    ship_date    DATE         NOT NULL COMMENT '计划发货日期',
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态(字典 scm_delivery_status) PENDING待拣货 PICKED已拣货 SHIPPED已出库 CANCELLED已取消',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    trade_doc_id BIGINT                DEFAULT NULL COMMENT '生成的销售出库单',
    pick_by      BIGINT                DEFAULT NULL COMMENT '拣货人',
    pick_time    DATETIME              DEFAULT NULL COMMENT '拣货时间',
    dept_id      BIGINT                DEFAULT NULL COMMENT '部门',
    create_by    BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_no (delivery_no, tenant_id),
    KEY idx_delivery_order (order_id),
    KEY idx_delivery_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发货通知单';

-- 装箱：已拣货的发货通知按箱录入箱号、重量、体积和箱内物料，每个通知明细装箱合计不超过通知数量
CREATE TABLE scm_delivery_package (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    delivery_id BIGINT         NOT NULL COMMENT '发货通知',
    box_no      VARCHAR(48)    NOT NULL COMMENT '箱号：发货通知号-序号',
    seq         INT            NOT NULL COMMENT '箱序号',
    weight      DECIMAL(10, 3)          DEFAULT NULL COMMENT '毛重（kg）',
    volume      DECIMAL(10, 4)          DEFAULT NULL COMMENT '体积（m³）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '装箱人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_package_box (box_no, tenant_id),
    KEY idx_delivery_package (delivery_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发货装箱';

CREATE TABLE scm_delivery_package_item (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    package_id       BIGINT         NOT NULL COMMENT '箱',
    delivery_item_id BIGINT         NOT NULL COMMENT '发货通知明细',
    material_id      BIGINT         NOT NULL COMMENT '物料',
    qty              DECIMAL(18, 4) NOT NULL COMMENT '数量',
    PRIMARY KEY (id),
    KEY idx_delivery_package_item (package_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '装箱明细';

CREATE TABLE scm_delivery_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    delivery_id   BIGINT         NOT NULL COMMENT '发货通知',
    order_item_id BIGINT         NOT NULL COMMENT '订单明细',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '通知数量',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_delivery_item_delivery (delivery_id),
    KEY idx_delivery_item_order_item (order_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发货通知明细';

CREATE TABLE scm_delivery_pick (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    delivery_id      BIGINT         NOT NULL COMMENT '发货通知',
    delivery_item_id BIGINT         NOT NULL COMMENT '通知明细',
    material_id      BIGINT         NOT NULL COMMENT '物料',
    location_id      BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 表示未分库位',
    batch_no         VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    qty              DECIMAL(18, 4) NOT NULL COMMENT '拣货数量',
    serial_nos       TEXT                    DEFAULT NULL COMMENT '序列号，逗号分隔',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_delivery_pick_delivery (delivery_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '拣货明细（待拣货时为先到期先出的建议）';

-- ---------------------------------------------------------------- 财务管理

CREATE TABLE fin_account (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(32)  NOT NULL COMMENT '科目编码：一级 4 位，下级在上级编码后加 2 位',
    name        VARCHAR(64)  NOT NULL COMMENT '科目名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级科目，0 为一级科目',
    category    VARCHAR(16)  NOT NULL COMMENT '类别(字典 fin_account_category) ASSET资产 LIABILITY负债 EQUITY权益 COST成本 PROFIT损益',
    direction   VARCHAR(8)   NOT NULL COMMENT '余额方向 DEBIT借 CREDIT贷',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    aux_items   VARCHAR(64)           DEFAULT NULL COMMENT '辅助核算项目（逗号分隔，可多选） DEPT部门 CUSTOMER客户 SUPPLIER供应商 MATERIAL存货；手工凭证使用该科目时必填',
    store_id    BIGINT                DEFAULT NULL COMMENT '所属门店：现金、银行的末级科目（结算账户）按门店归属，用户只能使用可管辖门店的账户；为空表示各门店共用',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_code (code, tenant_id),
    KEY idx_account_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会计科目';

CREATE TABLE fin_currency (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(8)  NOT NULL COMMENT '币种代码，如 USD',
    name        VARCHAR(32) NOT NULL COMMENT '名称',
    symbol      VARCHAR(8)           DEFAULT NULL COMMENT '符号',
    is_base     TINYINT     NOT NULL DEFAULT 0 COMMENT '本位币 1是（只有一个，CNY）',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_currency_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '币种';

CREATE TABLE fin_exchange_rate (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    currency    VARCHAR(8)     NOT NULL COMMENT '币种',
    period      INT            NOT NULL COMMENT '期间 yyyyMM：当月业务的记账汇率，期末调汇也取该月汇率',
    rate        DECIMAL(18, 6) NOT NULL COMMENT '汇率：1 外币 = rate 本位币',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exchange_rate (currency, period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '汇率（按月）';

CREATE TABLE fin_account_opening (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id  BIGINT         NOT NULL COMMENT '末级科目',
    debit       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期初借方余额',
    credit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期初贷方余额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_opening (account_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '科目期初余额';

CREATE TABLE fin_period (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period       INT         NOT NULL COMMENT '会计期间 yyyyMM，如 202610',
    status       VARCHAR(16) NOT NULL COMMENT '状态 OPEN未结账 CLOSED已结账',
    voucher_seq  INT         NOT NULL DEFAULT 0 COMMENT '本期已使用的最大凭证号',
    close_by     BIGINT               DEFAULT NULL COMMENT '结账人',
    close_time   DATETIME             DEFAULT NULL COMMENT '结账时间',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_period (period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会计期间';

CREATE TABLE fin_voucher (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period           INT            NOT NULL COMMENT '会计期间 yyyyMM',
    voucher_seq      INT            NOT NULL COMMENT '期间内凭证号',
    voucher_no       VARCHAR(16)    NOT NULL COMMENT '凭证字号，如 记-0001',
    voucher_date     DATE           NOT NULL COMMENT '凭证日期',
    summary          VARCHAR(255)            DEFAULT NULL COMMENT '摘要（第一条分录）',
    total_debit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '借方合计',
    total_credit     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '贷方合计',
    attachment_count INT            NOT NULL DEFAULT 0 COMMENT '附单据数',
    status           VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_voucher_status) DRAFT未审核 APPROVING审批中 AUDITED已审核 POSTED已记账',
    source_type      VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源(字典 fin_voucher_source) MANUAL手工 AR应收 AP应付 RECEIPT收款 PAYMENT付款 CLOSE结转损益',
    source_no        VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    flow_instance_id VARCHAR(64)            DEFAULT NULL COMMENT '审批流程实例ID',
    reversal_of      BIGINT                  DEFAULT NULL COMMENT '红字冲销凭证：被冲销的原凭证',
    reversed         TINYINT        NOT NULL DEFAULT 0 COMMENT '已被红字冲销 1是',
    create_by        BIGINT                  DEFAULT NULL COMMENT '制单人',
    audit_by         BIGINT                  DEFAULT NULL COMMENT '审核人',
    audit_time       DATETIME                DEFAULT NULL COMMENT '审核时间',
    post_by          BIGINT                  DEFAULT NULL COMMENT '记账人',
    post_time        DATETIME                DEFAULT NULL COMMENT '记账时间',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_voucher_seq (period, voucher_seq, tenant_id),
    KEY idx_voucher_period_status (period, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '记账凭证';

CREATE TABLE fin_voucher_entry (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    voucher_id  BIGINT         NOT NULL COMMENT '凭证ID',
    seq         INT            NOT NULL COMMENT '分录行号',
    summary     VARCHAR(255)   NOT NULL COMMENT '摘要',
    account_id  BIGINT         NOT NULL COMMENT '科目（末级）',
    dept_id     BIGINT                  DEFAULT NULL COMMENT '部门（辅助核算）',
    customer_id BIGINT                  DEFAULT NULL COMMENT '客户（辅助核算）',
    supplier_id BIGINT                  DEFAULT NULL COMMENT '供应商（辅助核算）',
    material_id BIGINT                  DEFAULT NULL COMMENT '存货（辅助核算）',
    debit       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '借方金额',
    credit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '贷方金额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_entry_voucher (voucher_id),
    KEY idx_entry_account (account_id),
    KEY idx_entry_customer (customer_id),
    KEY idx_entry_supplier (supplier_id),
    KEY idx_entry_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '凭证分录';

CREATE TABLE fin_bill (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    bill_no        VARCHAR(32)    NOT NULL COMMENT '单号',
    kind           VARCHAR(8)     NOT NULL COMMENT '种类 AR应收 AP应付',
    partner_id     BIGINT         NOT NULL COMMENT '客户 / 供应商',
    source_type    VARCHAR(32)    NOT NULL COMMENT '来源(字典 fin_bill_source) SALES_OUT销售出库 SALES_RETURN销售退货 PURCHASE_IN采购入库 PURCHASE_RETURN采购退货 MANUAL期初 / 手工 等',
    source_id      BIGINT                  DEFAULT NULL COMMENT '来源单据ID',
    source_no      VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    expense_account_id BIGINT                DEFAULT NULL COMMENT '费用类应付单（运费等）的借方费用科目',
    dept_id        BIGINT                  DEFAULT NULL COMMENT '部门（费用的辅助核算）',
    bill_date      DATE           NOT NULL COMMENT '单据日期',
    due_date       DATE           NOT NULL COMMENT '到期日 = 单据日期 + 账期',
    amount         DECIMAL(18, 2) NOT NULL COMMENT '价税合计，退货为负数（红字）',
    tax_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    settled_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已核销金额（与金额同号）',
    invoiced_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已开票 / 已收票金额（与金额同号）',
    bad_debt_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '坏账核销金额（计入已核销）',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '收付款时的折让 / 抹零金额（计入已核销）',
    currency        VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种；金额、已核销等为本位币（按记账汇率折算）',
    exchange_rate   DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '记账汇率',
    amount_fc       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币金额',
    settled_fc      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币已核销',
    status         VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_bill_status) UNSETTLED未核销 PARTIAL部分核销 SETTLED已核销',
    voucher_id     BIGINT                  DEFAULT NULL COMMENT '生成的凭证',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by      BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_no (bill_no, tenant_id),
    KEY idx_bill_partner (kind, partner_id, status),
    KEY idx_bill_source (source_type, source_id),
    KEY idx_bill_voucher (voucher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应收 / 应付单';

CREATE TABLE fin_settlement (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settle_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    kind        VARCHAR(16)    NOT NULL COMMENT '种类 RECEIPT收款 PAYMENT付款',
    partner_id  BIGINT         NOT NULL COMMENT '客户 / 供应商',
    settle_date DATE           NOT NULL COMMENT '收付款日期',
    account_id  BIGINT         NOT NULL COMMENT '结算科目（库存现金 / 银行存款的末级科目）',
    method      VARCHAR(16)             DEFAULT NULL COMMENT '结算方式(字典 fin_settle_method)',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额 = 各核销金额合计',
    currency    VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种',
    exchange_rate DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '收付款汇率；金额 = 原币 × 汇率（实收实付本位币）',
    amount_fc   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币金额',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '折让 / 抹零合计（不含在金额中，单据的已核销 = 金额 + 折让）',
    discount_fc     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '折让合计（原币）',
    advance_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '多收 / 多付转预收预付的金额（不含在金额中）',
    advance_id      BIGINT                  DEFAULT NULL COMMENT '多收 / 多付生成的预收预付单',
    advance_offset  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '冲抵预收 / 预付合计（原币，不含在金额中）：实收实付 = 核销合计 - 冲抵',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '生成的凭证',
    status       VARCHAR(16)    NOT NULL DEFAULT 'APPROVED' COMMENT '状态 DRAFT草稿 APPROVED已生效 APPROVING审批中 REJECTED已拒绝（付款审批）',
    flow_instance_id VARCHAR(64)        DEFAULT NULL COMMENT '审批流程实例ID',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_settle_no (settle_no, tenant_id),
    KEY idx_settlement_partner (kind, partner_id),
    KEY idx_settlement_voucher (voucher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收款 / 付款单';

CREATE TABLE fin_settlement_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settlement_id BIGINT         NOT NULL COMMENT '收付款单ID',
    bill_id       BIGINT         NOT NULL COMMENT '核销的应收 / 应付单',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '核销金额（红字单据为负数）',
    amount_fc     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '核销原币金额（本位币单据与金额相同）',
    discount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '折让 / 抹零金额（与单据同号），单据的已核销 = 核销金额 + 折让',
    discount_fc   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '折让原币金额（外币按单据记账汇率折算为折让）',
    advance_offset DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其中由预收 / 预付冲抵的金额（原币），核销金额只含实收实付的部分',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_settlement_item_settlement (settlement_id),
    KEY idx_settlement_item_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收付款核销明细';

CREATE TABLE fin_settlement_advance (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settlement_id BIGINT         NOT NULL COMMENT '收付款单',
    advance_id    BIGINT         NOT NULL COMMENT '冲抵的预收 / 预付款',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '冲抵金额（原币）',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_settlement_advance_settlement (settlement_id),
    KEY idx_settlement_advance_advance (advance_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收付款冲抵的预收预付（草稿、审批中时登记，生效时冲抵）';

CREATE TABLE fin_offset (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    offset_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    kind        VARCHAR(8)     NOT NULL COMMENT '方向 AR应收冲账（客户） AP应付冲账（供应商）',
    partner_id  BIGINT         NOT NULL COMMENT '客户 / 供应商',
    offset_date DATE           NOT NULL COMMENT '冲账日期',
    handler_id  BIGINT                  DEFAULT NULL COMMENT '经办人',
    item        VARCHAR(32)    NOT NULL COMMENT '冲账项目(业务字典 fin_offset_item)',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '冲账金额（正数），复核后生成金额为其负数的红字应收 / 应付单',
    begin_date  DATE                    DEFAULT NULL COMMENT '冲账项目期间：开始日期',
    end_date    DATE                    DEFAULT NULL COMMENT '冲账项目期间：结束日期',
    ticket_no   VARCHAR(64)             DEFAULT NULL COMMENT '票据编号',
    account_id  BIGINT                  DEFAULT NULL COMMENT '凭证对方科目，为空取凭证科目设置（应收冲账 / 应付冲账）',
    status      VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 fin_offset_status) DRAFT待复核 REVIEWED已复核',
    bill_id     BIGINT                  DEFAULT NULL COMMENT '复核生成的红字应收 / 应付单',
    review_by   BIGINT                  DEFAULT NULL COMMENT '复核人',
    review_time DATETIME                DEFAULT NULL COMMENT '复核时间',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_offset_no (offset_no, tenant_id),
    KEY idx_offset_partner (kind, partner_id),
    KEY idx_offset_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '冲账单（垫付运费、返利、三包赔偿等抵扣往来款）';

CREATE TABLE fin_netting (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    netting_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    customer_id  BIGINT         NOT NULL COMMENT '客户',
    supplier_id  BIGINT         NOT NULL COMMENT '供应商（关联该客户的供应商）',
    netting_date DATE           NOT NULL COMMENT '对冲日期',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '对冲金额（正数）= 核销的应收合计 = 核销的应付合计（本位币）',
    voucher_id   BIGINT                  DEFAULT NULL COMMENT '凭证：借 应付账款（供应商）/ 贷 应收账款（客户）',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_netting_no (netting_no, tenant_id),
    KEY idx_netting_customer (customer_id),
    KEY idx_netting_supplier (supplier_id),
    KEY idx_netting_voucher (voucher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应收应付对冲：关联的客户与供应商之间用应付抵减应收（不收付现金）';

CREATE TABLE fin_netting_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    netting_id  BIGINT         NOT NULL COMMENT '对冲单',
    bill_id     BIGINT         NOT NULL COMMENT '核销的应收 / 应付单',
    kind        VARCHAR(8)     NOT NULL COMMENT '种类 AR应收 AP应付',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '核销金额（与单据金额同号，红字单据为负数）',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_netting_item_netting (netting_id),
    KEY idx_netting_item_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应收应付对冲的核销明细';

CREATE TABLE fin_io_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)  NOT NULL COMMENT '类别名称',
    code        VARCHAR(32)           DEFAULT NULL COMMENT '内置类别标识（程序使用，不能删除、不能改方向）：SALES_FREIGHT销售运费 FREIGHT_ADVANCE代垫运费 TRANSFER_FEE调拨费用',
    direction   VARCHAR(8)   NOT NULL COMMENT '方向(字典 fin_fund_direction) IN收入 OUT支出',
    account_id  BIGINT                DEFAULT NULL COMMENT '凭证科目（末级）：收入记贷方、支出记借方',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_io_category (direction, name, tenant_id),
    UNIQUE KEY uk_io_category_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收支类别（其他收入、费用支出的分类）';

CREATE TABLE fin_io_doc (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no        VARCHAR(32)    NOT NULL COMMENT '单号',
    direction     VARCHAR(8)     NOT NULL COMMENT '方向(字典 fin_fund_direction) IN其他收入 OUT费用支出',
    doc_date      DATE           NOT NULL COMMENT '日期',
    ticket_no     VARCHAR(64)             DEFAULT NULL COMMENT '票据编号',
    partner_type  VARCHAR(16)             DEFAULT NULL COMMENT '关联单位类型(字典 fin_io_partner_type) CUSTOMER客户 SUPPLIER供应商 OTHER其他',
    partner_id    BIGINT                  DEFAULT NULL COMMENT '客户 / 供应商',
    partner_name  VARCHAR(128)            DEFAULT NULL COMMENT '其他单位的名称',
    settle_method VARCHAR(32)             DEFAULT NULL COMMENT '结算方式(业务字典 fin_settle_method)',
    account_id    BIGINT         NOT NULL COMMENT '结算账户（现金 / 银行末级科目）',
    dept_id       BIGINT                  DEFAULT NULL COMMENT '费用归属部门',
    amount        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '金额合计',
    status        VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 fin_io_status) DRAFT待确认 CONFIRMED已确认',
    voucher_id    BIGINT                  DEFAULT NULL COMMENT '确认生成的凭证',
    confirm_by    BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time  DATETIME                DEFAULT NULL COMMENT '确认时间',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    source_type   VARCHAR(32)             DEFAULT NULL COMMENT '来源单据类型(字典 fin_bill_source)：由业务单据生成时填写（如销售出库、采购退货现付的运费），手工录入为空',
    source_id     BIGINT                  DEFAULT NULL COMMENT '来源单据ID',
    source_no     VARCHAR(32)             DEFAULT NULL COMMENT '来源单据号',
    create_by     BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_io_doc_no (doc_no, tenant_id),
    KEY idx_io_doc_date (doc_date),
    KEY idx_io_doc_partner (partner_id),
    KEY idx_io_doc_source (source_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '其他收支单（其他收入单、费用支出单）';

CREATE TABLE fin_io_doc_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id      BIGINT         NOT NULL COMMENT '收支单',
    category_id BIGINT         NOT NULL COMMENT '收支类别',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_io_item_doc (doc_id),
    KEY idx_io_item_category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '其他收支单明细';

CREATE TABLE fin_invoice (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    kind           VARCHAR(8)     NOT NULL COMMENT '种类 OUTPUT销项（开给客户） INPUT进项（供应商开来）',
    invoice_no     VARCHAR(32)    NOT NULL COMMENT '发票号码',
    invoice_type   VARCHAR(16)    NOT NULL DEFAULT 'SPECIAL' COMMENT '发票类型(字典 fin_invoice_type) SPECIAL增值税专用发票 NORMAL普通发票 E_SPECIAL数电票（专票） E_NORMAL数电票（普票）',
    partner_id     BIGINT         NOT NULL COMMENT '客户 / 供应商',
    invoice_date   DATE           NOT NULL COMMENT '开票日期',
    amount         DECIMAL(18, 2) NOT NULL COMMENT '价税合计（红字发票为负数）',
    tax_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    matched_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已勾稽应收应付单的金额，导入的发票可以之后再勾稽',
    seller_tax_no  VARCHAR(32)             DEFAULT NULL COMMENT '销售方纳税人识别号',
    seller_name    VARCHAR(128)            DEFAULT NULL COMMENT '销售方名称',
    buyer_tax_no   VARCHAR(32)             DEFAULT NULL COMMENT '购买方纳税人识别号',
    buyer_name     VARCHAR(128)            DEFAULT NULL COMMENT '购买方名称',
    source         VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL手工登记 XML数电票文件导入 EXCEL发票清单导入',
    status         VARCHAR(16)    NOT NULL DEFAULT 'VALID' COMMENT '状态 VALID有效 VOID已作废',
    certify_period INT                     DEFAULT NULL COMMENT '进项发票认证（勾选抵扣）期间 yyyyMM',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by      BIGINT                  DEFAULT NULL COMMENT '登记人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_no (kind, invoice_no, tenant_id),
    KEY idx_invoice_partner (kind, partner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票登记：与应收应付单勾稽，未开票 / 未收票的部分为暂估';

CREATE TABLE fin_invoice_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    invoice_id  BIGINT         NOT NULL COMMENT '发票',
    bill_id     BIGINT         NOT NULL COMMENT '应收 / 应付单',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '勾稽金额（价税合计）',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_invoice_item_invoice (invoice_id),
    KEY idx_invoice_item_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票勾稽的应收应付单';

CREATE TABLE fin_invoice_line (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    invoice_id  BIGINT         NOT NULL COMMENT '发票',
    seq         INT            NOT NULL COMMENT '行号',
    item_name   VARCHAR(200)   NOT NULL COMMENT '项目名称（含税收分类简称）',
    spec        VARCHAR(128)            DEFAULT NULL COMMENT '规格型号',
    unit        VARCHAR(32)             DEFAULT NULL COMMENT '单位',
    qty         DECIMAL(18, 6)          DEFAULT NULL COMMENT '数量',
    price       DECIMAL(18, 8)          DEFAULT NULL COMMENT '单价（不含税）',
    amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '金额（不含税）',
    tax_rate    DECIMAL(5, 2)           DEFAULT NULL COMMENT '税率（%），如 13',
    tax_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_invoice_line_invoice (invoice_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票明细行：导入数电票时保存';

CREATE TABLE fin_advance (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    advance_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    kind         VARCHAR(16)    NOT NULL COMMENT '种类 RECEIPT预收（客户） PAYMENT预付（供应商）',
    partner_id   BIGINT         NOT NULL COMMENT '客户 / 供应商',
    advance_date DATE           NOT NULL COMMENT '收付款日期',
    account_id   BIGINT         NOT NULL COMMENT '结算科目（库存现金 / 银行存款）',
    method       VARCHAR(16)             DEFAULT NULL COMMENT '结算方式(字典 fin_settle_method)',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '金额',
    used_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已冲抵应收应付的金额',
    currency     VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种',
    exchange_rate DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率；金额 = 原币金额 × 汇率',
    amount_fc    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币金额',
    used_fc      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币已冲抵',
    voucher_id   BIGINT                  DEFAULT NULL COMMENT '收付款凭证',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_advance_no (advance_no, tenant_id),
    KEY idx_advance_partner (kind, partner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预收 / 预付款：收付时记预收账款 / 预付账款，冲抵应收应付单时转出';

CREATE TABLE fin_advance_apply (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    advance_id  BIGINT         NOT NULL COMMENT '预收 / 预付款',
    bill_id     BIGINT         NOT NULL COMMENT '冲抵的应收 / 应付单',
    settlement_id BIGINT                DEFAULT NULL COMMENT '在收付款中冲抵时的收付款单（凭证并入收付款凭证）',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '冲抵金额',
    amount_fc   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '冲抵原币金额',
    bill_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '单据一方的本位币核销金额（计入单据的已核销，外币单据可能与预收预付一方的冲抵金额不同）',
    apply_date  DATE           NOT NULL COMMENT '冲抵日期',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '转账凭证',
    create_by   BIGINT                  DEFAULT NULL COMMENT '操作人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_advance_apply_advance (advance_id),
    KEY idx_advance_apply_bill (bill_id),
    KEY idx_advance_apply_settlement (settlement_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预收预付冲抵应收应付';

-- 账户调拨：银行账户之间、银行与现金之间转账，生成凭证（借 转入账户、财务费用，贷 转出账户）
CREATE TABLE fin_fund_transfer (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    transfer_no     VARCHAR(32)    NOT NULL COMMENT '调拨单号',
    transfer_date   DATE           NOT NULL COMMENT '调拨日期',
    from_account_id BIGINT         NOT NULL COMMENT '转出科目',
    to_account_id   BIGINT         NOT NULL COMMENT '转入科目',
    amount          DECIMAL(18, 2) NOT NULL COMMENT '调拨金额',
    fee             DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '手续费（计入财务费用）',
    fee_bearer      VARCHAR(8)     NOT NULL DEFAULT 'FROM' COMMENT '手续费支付账户(字典 fin_fee_bearer) FROM转出账户支付 TO转入账户支付',
    handler_id      BIGINT                  DEFAULT NULL COMMENT '经办人',
    ticket_no       VARCHAR(64)             DEFAULT NULL COMMENT '票据号码（转账回单、支票号等）',
    voucher_id      BIGINT                  DEFAULT NULL COMMENT '凭证',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transfer_no (transfer_no, tenant_id),
    KEY idx_transfer_date (transfer_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '账户调拨';

CREATE TABLE fin_commission_rule (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)    NOT NULL COMMENT '规则名称',
    basis             VARCHAR(16)    NOT NULL COMMENT '提成依据(字典 fin_commission_basis) SALES销售额（不含税） PROFIT毛利 RECEIPT回款',
    salesman_id       BIGINT                  DEFAULT NULL COMMENT '适用业务员，为空适用全部',
    material_category VARCHAR(32)             DEFAULT NULL COMMENT '适用物料分类编码(wms_material_category.code，含下级分类)，为空适用全部；回款不区分分类',
    threshold         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '业绩门槛：业务员当月该依据的业绩达到此金额时适用（阶梯提成）',
    rate              DECIMAL(6, 3)  NOT NULL COMMENT '提成比例（%）',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售提成规则：同一业务员、分类、依据取最具体（指定业务员 > 指定分类）且门槛最高的已达成规则';

CREATE TABLE fin_commission (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period       INT            NOT NULL COMMENT '期间 yyyyMM',
    status       VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿 CONFIRMED已确认（计入工资表）',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '提成合计（含调整）',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '计算人',
    confirm_by   BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_commission_period (period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售提成表：按期间计算各业务员的业绩和提成';

CREATE TABLE fin_commission_line (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    commission_id     BIGINT         NOT NULL COMMENT '提成表',
    salesman_id       BIGINT         NOT NULL COMMENT '业务员',
    sales_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '销售额（不含税，扣除退货）',
    profit_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '毛利（销售额 - 成本）',
    receipt_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '回款',
    commission_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '按规则计算的提成',
    adjust_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '手工调整',
    final_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '应发提成 = 提成 + 调整',
    detail            TEXT COMMENT '计算明细（JSON）：依据、分类、业绩、规则、比例、提成',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注（调整原因）',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_commission_line (commission_id, salesman_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售提成表明细：每个业务员一行';

CREATE TABLE fin_account_rule (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    role        VARCHAR(32) NOT NULL COMMENT '用途（AccountRole），如 RECEIVABLE 应收账款、INVENTORY 存货',
    dim_value   VARCHAR(32) NOT NULL DEFAULT '' COMMENT '维度取值：物料分类 / 职位类别，空串为该用途的默认科目',
    account_id  BIGINT      NOT NULL COMMENT '科目（末级）',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_rule (role, dim_value, tenant_id),
    KEY idx_account_rule_account (account_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '凭证科目设置：业务单据自动生成凭证时各用途使用的科目，没有设置的用途取预置科目';

-- ---------------------------------------------------------------- 商务智能（数据仓库的事实表由同步任务全量重建）

CREATE TABLE bi_fact_sales (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_date    DATE           NOT NULL COMMENT '业务日期',
    period      INT            NOT NULL COMMENT '月份 yyyyMM',
    doc_no      VARCHAR(32)    NOT NULL COMMENT '出库 / 退货单号',
    customer_id BIGINT         NOT NULL COMMENT '客户',
    owner_id    BIGINT                  DEFAULT NULL COMMENT '客户的负责销售',
    material_id BIGINT         NOT NULL COMMENT '物料',
    category    VARCHAR(64)             DEFAULT NULL COMMENT '物料分类',
    warehouse_id BIGINT        NOT NULL COMMENT '仓库',
    dept_id     BIGINT                  DEFAULT NULL COMMENT '制单部门',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '数量，退货为负',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '销售额（不含税），退货为负',
    cost        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '销售成本 = 数量 × 存货单位成本',
    profit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '毛利',
    PRIMARY KEY (id),
    KEY idx_fact_sales_period (period),
    KEY idx_fact_sales_customer (customer_id),
    KEY idx_fact_sales_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售事实表';

CREATE TABLE bi_fact_purchase (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_date     DATE           NOT NULL COMMENT '业务日期',
    period       INT            NOT NULL COMMENT '月份 yyyyMM',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '入库 / 退货单号',
    supplier_id  BIGINT         NOT NULL COMMENT '供应商',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    category     VARCHAR(64)             DEFAULT NULL COMMENT '物料分类',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '数量，退货为负',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '采购额（不含税），退货为负',
    PRIMARY KEY (id),
    KEY idx_fact_purchase_period (period),
    KEY idx_fact_purchase_supplier (supplier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购事实表';

CREATE TABLE bi_fact_inventory (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    snap_date    DATE           NOT NULL COMMENT '快照日期（同步当天）',
    period       INT            NOT NULL COMMENT '月份 yyyyMM',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    category     VARCHAR(64)             DEFAULT NULL COMMENT '物料分类',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '库存数量',
    amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '库存金额 = 数量 × 存货单位成本',
    PRIMARY KEY (id),
    KEY idx_fact_inventory_date (snap_date),
    KEY idx_fact_inventory_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存快照事实表（每天一份，保留历史）';

CREATE TABLE bi_fact_finance (
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id  BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period     INT            NOT NULL COMMENT '会计期间',
    account_id BIGINT         NOT NULL COMMENT '科目（末级）',
    category   VARCHAR(16)    NOT NULL COMMENT '科目类别',
    dept_id    BIGINT                  DEFAULT NULL COMMENT '辅助核算部门',
    debit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '借方发生额',
    credit     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '贷方发生额',
    PRIMARY KEY (id),
    KEY idx_fact_finance_period (period, account_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '财务事实表（已记账凭证按期间、科目、部门汇总，不含结转损益凭证）';

CREATE TABLE bi_etl_log (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    trigger_type VARCHAR(16)   NOT NULL COMMENT '触发方式 MANUAL手动 SCHEDULE定时',
    start_time   DATETIME      NOT NULL COMMENT '开始时间',
    end_time     DATETIME               DEFAULT NULL COMMENT '结束时间',
    status       VARCHAR(16)   NOT NULL COMMENT '状态(字典 bi_etl_status) RUNNING运行中 SUCCESS成功 FAILED失败',
    row_counts   VARCHAR(500)           DEFAULT NULL COMMENT '各事实表行数，如 sales=120,purchase=80',
    message      VARCHAR(1000)          DEFAULT NULL COMMENT '失败原因',
    create_by    BIGINT                 DEFAULT NULL COMMENT '操作人，定时任务为空',
    create_time  DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据同步日志';

CREATE TABLE bi_report (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)   NOT NULL COMMENT '报表名称',
    dataset     VARCHAR(32)   NOT NULL COMMENT '数据集（分析主题，BiCube）',
    config      VARCHAR(4000) NOT NULL COMMENT '分析配置 JSON：行维度、列维度、指标、筛选、图表类型',
    shared      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否共享给所有人 1是 0仅自己',
    remark      VARCHAR(255)           DEFAULT NULL COMMENT '说明',
    create_by   BIGINT                 DEFAULT NULL COMMENT '创建人',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_report_owner (create_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义报表';

CREATE TABLE bi_dashboard (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)   NOT NULL COMMENT '仪表盘名称',
    widgets     VARCHAR(4000) NOT NULL COMMENT '组件 JSON：[{reportId, span, chart}]',
    shared      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否共享给所有人',
    remark      VARCHAR(255)           DEFAULT NULL COMMENT '说明',
    create_by   BIGINT                 DEFAULT NULL COMMENT '创建人',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_dashboard_owner (create_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义仪表盘';

CREATE TABLE bi_screen_share (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name             VARCHAR(64)  NOT NULL COMMENT '名称，如 一车间电视',
    token            CHAR(32)     NOT NULL COMMENT '访问令牌（128 位随机数），链接中使用',
    screens          VARCHAR(128) NOT NULL COMMENT '大屏（逗号分隔） WAREHOUSE仓储物流 SALES销售经营，多个时轮播',
    interval_seconds INT          NOT NULL DEFAULT 60 COMMENT '轮播间隔（秒）',
    expire_time      DATETIME              DEFAULT NULL COMMENT '有效期至，为空长期有效',
    status           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    last_access_time DATETIME              DEFAULT NULL COMMENT '最近访问时间',
    create_by        BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_screen_share_token (token)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大屏投屏链接：门店、仓库的电视免登录查看指定的大屏（只读）';

CREATE TABLE bi_alert_rule (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)    NOT NULL COMMENT '规则名称',
    metric      VARCHAR(32)    NOT NULL COMMENT '监控指标（BiAlertMetric）',
    operator    VARCHAR(4)     NOT NULL COMMENT '比较 GT大于 GE不小于 LT小于 LE不大于',
    threshold   DECIMAL(18, 4) NOT NULL COMMENT '阈值',
    level       VARCHAR(16)    NOT NULL COMMENT '级别(字典 bi_alert_level) INFO提示 WARN警告 DANGER严重',
    enabled     TINYINT        NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '处理建议',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预警规则';

-- ---------------------------------------------------------------- 智能（AI）：模型配置、用量、AI 问数的会话与消息
CREATE TABLE ai_config (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    provider            VARCHAR(16)   NOT NULL COMMENT '服务商 DEEPSEEK QWEN ZHIPU MOONSHOT DOUBAO CUSTOM（AiProvider）',
    base_url            VARCHAR(255)           DEFAULT NULL COMMENT '接口地址（不含 /chat/completions），为空时用服务商的默认地址',
    api_key             VARCHAR(512)           DEFAULT NULL COMMENT 'API Key（AES-GCM 加密，enc: 开头），不返回前端',
    chat_model          VARCHAR(64)            DEFAULT NULL COMMENT '对话 / 工具调用模型',
    vision_model        VARCHAR(64)            DEFAULT NULL COMMENT '识图模型，为空时智能录入只支持 PDF、Excel、文本',
    enabled             TINYINT       NOT NULL DEFAULT 0 COMMENT '总开关 1启用 0停用',
    monthly_token_limit BIGINT                 DEFAULT NULL COMMENT '每月 token 上限（输入 + 输出），为空不限',
    chat_enabled        TINYINT       NOT NULL DEFAULT 0 COMMENT 'AI 问数 1开启 0关闭',
    extract_enabled     TINYINT       NOT NULL DEFAULT 0 COMMENT '智能录入 1开启 0关闭',
    cleanse_enabled     TINYINT       NOT NULL DEFAULT 0 COMMENT '数据整理 1开启 0关闭',
    report_enabled      TINYINT       NOT NULL DEFAULT 0 COMMENT '智能报告 1开启 0关闭',
    temperature         DECIMAL(3, 2) NOT NULL DEFAULT 0.30 COMMENT '温度',
    max_tokens          INT                    DEFAULT NULL COMMENT '单次回答的最大 token 数，为空由服务商决定',
    create_time         DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_config (tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大模型配置（国产模型，OpenAI 兼容协议），每个租户一条';

CREATE TABLE ai_usage (
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id           BIGINT               DEFAULT NULL COMMENT '调用人，没有登录用户时为空',
    feature           VARCHAR(16) NOT NULL COMMENT '功能 CHAT问数 EXTRACT录入 CLEANSE整理 REPORT报告 TEST测试连接（AiFeature）',
    model             VARCHAR(64)          DEFAULT NULL COMMENT '模型',
    prompt_tokens     INT         NOT NULL DEFAULT 0 COMMENT '输入 token',
    completion_tokens INT         NOT NULL DEFAULT 0 COMMENT '输出 token',
    create_time       DATETIME             DEFAULT NULL COMMENT '调用时间',
    PRIMARY KEY (id),
    KEY idx_ai_usage_time (tenant_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大模型调用的 token 用量';

CREATE TABLE ai_conversation (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '所属用户（只有本人能访问）',
    title       VARCHAR(100)          DEFAULT NULL COMMENT '标题（默认为第一条提问）',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '最近提问时间',
    PRIMARY KEY (id),
    KEY idx_ai_conversation_user (user_id, update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 问数的会话';

CREATE TABLE ai_message (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    conversation_id BIGINT      NOT NULL COMMENT '会话',
    role            VARCHAR(16) NOT NULL COMMENT '角色 USER提问 ASSISTANT回答 TOOL工具结果（AiMessageRole）',
    content         TEXT COMMENT '提问 / 回答的文字；工具消息为调用参数',
    tool_call_id    VARCHAR(64)          DEFAULT NULL COMMENT '工具消息：对应的工具调用 ID',
    tool_name       VARCHAR(64)          DEFAULT NULL COMMENT '工具消息：工具名称',
    tool_calls      TEXT COMMENT '回答：请求调用的工具（JSON），列表不返回',
    tool_result     MEDIUMTEXT COMMENT '工具消息：回传给模型的结果（JSON），列表不返回',
    tokens          INT                  DEFAULT NULL COMMENT '回答消耗的 token',
    create_time     DATETIME             DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ai_message_conversation (conversation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 问数的消息';

CREATE TABLE ai_cleanse_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    source_key      VARCHAR(64)  NOT NULL COMMENT '数据类型（AiCleanseSource.key，如 wms.material）',
    keyword         VARCHAR(64)           DEFAULT NULL COMMENT '编码或名称关键字（整理范围），为空表示全部',
    status          VARCHAR(16)  NOT NULL COMMENT '状态 RUNNING进行中 COMPLETED已完成 FAILED失败 CANCELLED已取消（AiCleanseTaskStatus）',
    phase           VARCHAR(16)  NOT NULL COMMENT '阶段 LOADING读取 DUPLICATE查重 NORMALIZE规范化 DONE结束（AiCleansePhase）',
    running_key     VARCHAR(64)           DEFAULT NULL COMMENT '进行中时为数据类型，结束后清空（唯一约束：同一数据类型同时只有一个进行中的任务）',
    record_count    INT          NOT NULL DEFAULT 0 COMMENT '读取的记录数',
    truncated       TINYINT      NOT NULL DEFAULT 0 COMMENT '记录超过上限，只分析了一部分 1是 0否',
    group_count     INT          NOT NULL DEFAULT 0 COMMENT '疑似重复候选组数',
    total_steps     INT          NOT NULL DEFAULT 0 COMMENT '模型调用总步数',
    done_steps      INT          NOT NULL DEFAULT 0 COMMENT '已完成步数',
    failed_steps    INT          NOT NULL DEFAULT 0 COMMENT '模型输出不合格而跳过的步数',
    duplicate_count INT          NOT NULL DEFAULT 0 COMMENT '疑似重复建议数（待停用的记录）',
    normalize_count INT          NOT NULL DEFAULT 0 COMMENT '规范化建议数',
    missing_count   INT          NOT NULL DEFAULT 0 COMMENT '缺失字段提示数',
    discarded_count INT          NOT NULL DEFAULT 0 COMMENT '校验不通过而丢弃的建议数',
    tokens          INT          NOT NULL DEFAULT 0 COMMENT '消耗的 token',
    error_key       VARCHAR(100)          DEFAULT NULL COMMENT '失败原因的消息键',
    error_args      VARCHAR(1000)         DEFAULT NULL COMMENT '失败原因的参数（JSON 数组）',
    create_by       BIGINT                DEFAULT NULL COMMENT '发起人',
    create_time     DATETIME              DEFAULT NULL COMMENT '发起时间',
    update_time     DATETIME              DEFAULT NULL COMMENT '最近进展时间',
    finish_time     DATETIME              DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_cleanse_task_running (running_key, tenant_id),
    KEY idx_ai_cleanse_task_source (tenant_id, source_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据整理任务（主数据查重与规范化）';

CREATE TABLE ai_cleanse_suggestion (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    task_id        BIGINT       NOT NULL COMMENT '整理任务',
    kind           VARCHAR(16)  NOT NULL COMMENT '种类 DUPLICATE疑似重复 NORMALIZE规范化 MISSING缺失字段（AiCleanseKind）',
    group_no       INT                   DEFAULT NULL COMMENT '疑似重复的组号',
    record_id      BIGINT       NOT NULL COMMENT '记录ID',
    record_code    VARCHAR(64)           DEFAULT NULL COMMENT '记录编码',
    record_name    VARCHAR(255)          DEFAULT NULL COMMENT '记录名称',
    field          VARCHAR(16)           DEFAULT NULL COMMENT '字段 NAME SPEC CATEGORY UNIT CONTACT ADDRESS（AiCleanseField），疑似重复为空',
    old_value      VARCHAR(255)          DEFAULT NULL COMMENT '生成建议时的原值',
    new_value      VARCHAR(255)          DEFAULT NULL COMMENT '建议值',
    keep_record_id BIGINT                DEFAULT NULL COMMENT '疑似重复：建议保留的记录',
    keep_code      VARCHAR(64)           DEFAULT NULL COMMENT '保留记录的编码',
    keep_name      VARCHAR(255)          DEFAULT NULL COMMENT '保留记录的名称',
    reason         VARCHAR(500)          DEFAULT NULL COMMENT '理由（模型给出）',
    confidence     INT                   DEFAULT NULL COMMENT '置信度 0～100',
    status         VARCHAR(16)  NOT NULL COMMENT '状态 PENDING待处理 APPLIED已采纳 IGNORED已忽略（AiSuggestionStatus）',
    handled_by     BIGINT                DEFAULT NULL COMMENT '采纳 / 忽略人',
    handled_time   DATETIME              DEFAULT NULL COMMENT '采纳 / 忽略时间',
    create_time    DATETIME              DEFAULT NULL COMMENT '生成时间',
    PRIMARY KEY (id),
    KEY idx_ai_cleanse_suggestion_task (task_id, kind, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据整理建议';

CREATE TABLE ai_report (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    period_type       VARCHAR(8)   NOT NULL COMMENT '期间类型 WEEK周报 MONTH月报（AiReportPeriodType）',
    period_start      DATE         NOT NULL COMMENT '期间开始',
    period_end        DATE         NOT NULL COMMENT '期间结束（含）',
    title             VARCHAR(100) NOT NULL COMMENT '标题',
    content           MEDIUMTEXT COMMENT '报告正文（Markdown），列表不返回',
    user_id           BIGINT                DEFAULT NULL COMMENT '生成人，定时任务自动生成时为空（自动报告只有全部数据权限的用户能看）',
    sources           VARCHAR(500)          DEFAULT NULL COMMENT '包含的数据来源标识，逗号分隔',
    snapshot          MEDIUMTEXT COMMENT '交给模型的数据快照（JSON），列表不返回',
    status            VARCHAR(16)  NOT NULL COMMENT '状态 GENERATING生成中 DONE已完成 FAILED失败（AiReportStatus）',
    error             VARCHAR(500)          DEFAULT NULL COMMENT '失败原因',
    prompt_tokens     INT                   DEFAULT NULL COMMENT '输入 token',
    completion_tokens INT                   DEFAULT NULL COMMENT '输出 token',
    create_time       DATETIME              DEFAULT NULL COMMENT '生成时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ai_report_period (tenant_id, period_type, period_start),
    KEY idx_ai_report_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能报告（周报 / 月报）';

INSERT INTO sys_tenant (id, name, admin_user_id, status, remark, create_time, update_time) VALUES
(1, '柠檬科技', 1, 1, '平台租户（运营方），不受套餐限制', NOW(), NOW());

INSERT INTO sys_dept (id, parent_id, name, sort, leader_user_id, status, create_time, update_time) VALUES
(1,   0,  '柠檬科技', 1, 1,    1, NOW(), NOW()),
(10,  1,  '研发部',   1, NULL, 1, NOW(), NOW()),
(11,  1,  '市场部',   2, NULL, 1, NOW(), NOW()),
(12,  1,  '财务部',   3, NULL, 1, NOW(), NOW()),
(100, 10, '前端组',   1, NULL, 1, NOW(), NOW()),
(101, 10, '后端组',   2, NULL, 1, NOW(), NOW());

INSERT INTO sys_user (id, dept_id, store_id, username, password, nickname, status, create_time, update_time) VALUES
(1, 1,  1, 'admin', '$2a$10$1KjM59xYPcOlGbBaXNIRGOKMOubiCF1YaqU7b47kEXtfe159apzGW', '超级管理员', 1, NOW(), NOW()),
(2, 10, 1, 'test',  '$2a$10$1KjM59xYPcOlGbBaXNIRGOKMOubiCF1YaqU7b47kEXtfe159apzGW', '测试用户', 1, NOW(), NOW());

INSERT INTO sys_role (id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(1, '超级管理员', 'admin', 1, 1, 1, '拥有全部权限', NOW(), NOW()),
(2, '普通用户', 'common', 2, 4, 1, '只读权限，只能查看本部门及以下的数据', NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1,   0, '系统管理', 'M', '/system', NULL, NULL, 'SettingOutlined', 10, 1, 1, NOW(), NOW()),
(100, 1, '用户管理', 'C', 'user', 'system/user/index', 'sys:user:list', 'UserOutlined', 1, 1, 1, NOW(), NOW()),
(104, 1, '部门管理', 'C', 'dept', 'system/dept/index', 'sys:dept:list', 'ApartmentOutlined', 2, 1, 1, NOW(), NOW()),
(101, 1, '角色管理', 'C', 'role', 'system/role/index', 'sys:role:list', 'TeamOutlined', 3, 1, 1, NOW(), NOW()),
(102, 1, '菜单管理', 'C', 'menu', 'system/menu/index', 'sys:menu:list', 'MenuOutlined', 4, 1, 1, NOW(), NOW()),
(103, 1, '字典管理', 'C', 'dict', 'system/dict/index', 'sys:dict:list', 'BookOutlined', 5, 1, 1, NOW(), NOW()),
(105, 1, '租户管理', 'C', 'tenant', 'system/tenant/index', 'sys:tenant:list', 'ShopOutlined', 6, 1, 1, NOW(), NOW()),
(106, 1, '租户套餐', 'C', 'tenant-package', 'system/tenant-package/index', 'sys:tenantPackage:list', 'GiftOutlined', 7, 1, 1, NOW(), NOW()),
(107, 1, '编码规则', 'C', 'code-rule', 'system/code-rule/index', 'sys:codeRule:list', 'NumberOutlined', 8, 1, 1, NOW(), NOW()),
(108, 1, '打印模板', 'C', 'print-template', 'system/print-template/index', 'sys:printTemplate:list', 'PrinterOutlined', 9, 1, 1, NOW(), NOW()),
(109, 1, '参数设置', 'C', 'config', 'system/config/index', 'sys:config:list', 'ControlOutlined', 10, 1, 1, NOW(), NOW()),
(1000, 100, '用户新增', 'F', NULL, NULL, 'sys:user:add', NULL, 1, 1, 1, NOW(), NOW()),
(1001, 100, '用户修改', 'F', NULL, NULL, 'sys:user:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1002, 100, '用户删除', 'F', NULL, NULL, 'sys:user:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1003, 100, '重置密码', 'F', NULL, NULL, 'sys:user:resetPwd', NULL, 4, 1, 1, NOW(), NOW()),
(1010, 101, '角色新增', 'F', NULL, NULL, 'sys:role:add', NULL, 1, 1, 1, NOW(), NOW()),
(1011, 101, '角色修改', 'F', NULL, NULL, 'sys:role:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1012, 101, '角色删除', 'F', NULL, NULL, 'sys:role:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1013, 101, '数据权限', 'F', NULL, NULL, 'sys:role:dataScope', NULL, 4, 1, 1, NOW(), NOW()),
(1014, 101, '分配用户', 'F', NULL, NULL, 'sys:role:assignUser', NULL, 5, 1, 1, NOW(), NOW()),
(1020, 102, '菜单新增', 'F', NULL, NULL, 'sys:menu:add', NULL, 1, 1, 1, NOW(), NOW()),
(1021, 102, '菜单修改', 'F', NULL, NULL, 'sys:menu:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1022, 102, '菜单删除', 'F', NULL, NULL, 'sys:menu:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1030, 103, '字典新增', 'F', NULL, NULL, 'sys:dict:add', NULL, 1, 1, 1, NOW(), NOW()),
(1031, 103, '字典修改', 'F', NULL, NULL, 'sys:dict:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1032, 103, '字典删除', 'F', NULL, NULL, 'sys:dict:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1040, 104, '部门新增', 'F', NULL, NULL, 'sys:dept:add', NULL, 1, 1, 1, NOW(), NOW()),
(1041, 104, '部门修改', 'F', NULL, NULL, 'sys:dept:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1042, 104, '部门删除', 'F', NULL, NULL, 'sys:dept:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1050, 105, '租户新增', 'F', NULL, NULL, 'sys:tenant:add', NULL, 1, 1, 1, NOW(), NOW()),
(1051, 105, '租户修改', 'F', NULL, NULL, 'sys:tenant:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1052, 105, '租户删除', 'F', NULL, NULL, 'sys:tenant:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1060, 106, '套餐新增', 'F', NULL, NULL, 'sys:tenantPackage:add', NULL, 1, 1, 1, NOW(), NOW()),
(1061, 106, '套餐修改', 'F', NULL, NULL, 'sys:tenantPackage:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1062, 106, '套餐删除', 'F', NULL, NULL, 'sys:tenantPackage:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1070, 107, '规则修改', 'F', NULL, NULL, 'sys:codeRule:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1080, 108, '模板修改', 'F', NULL, NULL, 'sys:printTemplate:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1090, 109, '参数修改', 'F', NULL, NULL, 'sys:config:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2,   0, '系统监控', 'M', '/monitor', NULL, NULL, 'FundProjectionScreenOutlined', 11, 1, 1, NOW(), NOW()),
(110, 2, '在线用户', 'C', 'online', 'monitor/online/index', 'monitor:online:list', 'UsergroupAddOutlined', 1, 1, 1, NOW(), NOW()),
(111, 2, '操作日志', 'C', 'operlog', 'monitor/operlog/index', 'monitor:operlog:list', 'FileTextOutlined', 2, 1, 1, NOW(), NOW()),
(112, 2, '登录日志', 'C', 'loginlog', 'monitor/loginlog/index', 'monitor:loginlog:list', 'LoginOutlined', 3, 1, 1, NOW(), NOW()),
(113, 2, '服务监控', 'C', 'server', 'monitor/server/index', 'monitor:server:list', 'CloudServerOutlined', 4, 1, 1, NOW(), NOW()),
(114, 2, '缓存监控', 'C', 'cache', 'monitor/cache/index', 'monitor:cache:list', 'DatabaseOutlined', 5, 1, 1, NOW(), NOW()),
(115, 2, '定时任务', 'C', 'job', 'monitor/job/index', 'monitor:job:list', 'FieldTimeOutlined', 6, 1, 1, NOW(), NOW()),
(1100, 110, '强制退出', 'F', NULL, NULL, 'monitor:online:kick', NULL, 1, 1, 1, NOW(), NOW()),
(1110, 111, '日志删除', 'F', NULL, NULL, 'monitor:operlog:remove', NULL, 1, 1, 1, NOW(), NOW()),
(1120, 112, '日志删除', 'F', NULL, NULL, 'monitor:loginlog:remove', NULL, 1, 1, 1, NOW(), NOW()),
(1140, 114, '清除缓存', 'F', NULL, NULL, 'monitor:cache:remove', NULL, 1, 1, 1, NOW(), NOW()),
(1150, 115, '任务修改', 'F', NULL, NULL, 'monitor:job:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1151, 115, '立即执行', 'F', NULL, NULL, 'monitor:job:run', NULL, 2, 1, 1, NOW(), NOW()),
(3,   0, '流程中心', 'M', '/workflow', NULL, NULL, 'NodeIndexOutlined', 9, 1, 1, NOW(), NOW()),
(120, 961, '发起申请', 'C', 'start', 'workflow/start/index', 'wf:start:list', 'SendOutlined', 2, 1, 1, NOW(), NOW()),
(121, 961, '我的待办', 'C', 'todo', 'workflow/todo/index', 'wf:task:todo', 'ScheduleOutlined', 1, 1, 1, NOW(), NOW()),
(122, 961, '我的已办', 'C', 'done', 'workflow/done/index', 'wf:task:done', 'CheckSquareOutlined', 4, 1, 1, NOW(), NOW()),
(123, 961, '我的申请', 'C', 'mine', 'workflow/mine/index', 'wf:instance:mine', 'SolutionOutlined', 3, 1, 1, NOW(), NOW()),
(124, 961, '抄送我的', 'C', 'copy', 'workflow/copy/index', 'wf:copy:list', 'CopyOutlined', 5, 1, 1, NOW(), NOW()),
(125, 962, '流程模型', 'C', 'model', 'workflow/model/index', 'wf:model:list', 'PartitionOutlined', 1, 1, 1, NOW(), NOW()),
(126, 962, '流程实例', 'C', 'instance', 'workflow/instance/index', 'wf:instance:list', 'DeploymentUnitOutlined', 2, 1, 1, NOW(), NOW()),
(1250, 125, '模型新增', 'F', NULL, NULL, 'wf:model:add', NULL, 1, 1, 1, NOW(), NOW()),
(1251, 125, '模型修改', 'F', NULL, NULL, 'wf:model:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1252, 125, '模型发布', 'F', NULL, NULL, 'wf:model:deploy', NULL, 3, 1, 1, NOW(), NOW()),
(1253, 125, '模型删除', 'F', NULL, NULL, 'wf:model:remove', NULL, 4, 1, 1, NOW(), NOW()),
(1260, 126, '终止流程', 'F', NULL, NULL, 'wf:instance:cancel', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(1, '通用状态', 'sys_common_status', 1, '启用/停用', NOW(), NOW()),
(2, '显示状态', 'sys_show_hide', 1, '菜单显示/隐藏', NOW(), NOW()),
(3, '菜单类型', 'sys_menu_type', 1, '目录/菜单/按钮', NOW(), NOW()),
(4, '操作类型', 'sys_oper_type', 1, '操作日志的操作类型', NOW(), NOW()),
(5, '成功失败', 'sys_success_fail', 1, '日志结果', NOW(), NOW()),
(6, '数据权限', 'sys_data_scope', 1, '角色的数据权限范围', NOW(), NOW()),
(7, '流程分类', 'wf_category', 1, '流程模型的分类', NOW(), NOW()),
(8, '流程状态', 'wf_status', 1, '流程实例的状态', NOW(), NOW()),
(9, '审批动作', 'wf_task_action', 1, '审批记录中的处理结果', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_common_status', '启用', '1', 'green', 1, 1, NOW(), NOW()),
('sys_common_status', '停用', '0', 'red', 2, 1, NOW(), NOW()),
('sys_show_hide', '显示', '1', 'blue', 1, 1, NOW(), NOW()),
('sys_show_hide', '隐藏', '0', 'default', 2, 1, NOW(), NOW()),
('sys_menu_type', '目录', 'M', 'blue', 1, 1, NOW(), NOW()),
('sys_menu_type', '菜单', 'C', 'green', 2, 1, NOW(), NOW()),
('sys_menu_type', '按钮', 'F', 'orange', 3, 1, NOW(), NOW()),
('sys_oper_type', '其他', '0', 'default', 0, 1, NOW(), NOW()),
('sys_oper_type', '新增', '1', 'green', 1, 1, NOW(), NOW()),
('sys_oper_type', '修改', '2', 'blue', 2, 1, NOW(), NOW()),
('sys_oper_type', '删除', '3', 'red', 3, 1, NOW(), NOW()),
('sys_oper_type', '授权', '4', 'purple', 4, 1, NOW(), NOW()),
('sys_oper_type', '导出', '5', 'cyan', 5, 1, NOW(), NOW()),
('sys_oper_type', '强退', '6', 'orange', 6, 1, NOW(), NOW()),
('sys_oper_type', '清空', '7', 'red', 7, 1, NOW(), NOW()),
('sys_oper_type', '导入', '8', 'geekblue', 8, 1, NOW(), NOW()),
('sys_success_fail', '成功', '1', 'green', 1, 1, NOW(), NOW()),
('sys_success_fail', '失败', '0', 'red', 2, 1, NOW(), NOW()),
('sys_data_scope', '全部数据', '1', 'red', 1, 1, NOW(), NOW()),
('sys_data_scope', '自定义数据', '2', 'purple', 2, 1, NOW(), NOW()),
('sys_data_scope', '本部门数据', '3', 'blue', 3, 1, NOW(), NOW()),
('sys_data_scope', '本部门及以下数据', '4', 'cyan', 4, 1, NOW(), NOW()),
('sys_data_scope', '仅本人数据', '5', 'default', 5, 1, NOW(), NOW()),
('wf_category', '采购', 'PURCHASE', 'blue', 1, 1, NOW(), NOW()),
('wf_category', '销售', 'SALES', 'green', 2, 1, NOW(), NOW()),
('wf_category', '库存', 'INVENTORY', 'cyan', 3, 1, NOW(), NOW()),
('wf_category', '财务', 'FINANCE', 'gold', 4, 1, NOW(), NOW()),
('wf_category', '其他', 'OTHER', 'default', 5, 1, NOW(), NOW()),
('wf_status', '审批中', 'RUNNING', 'processing', 1, 1, NOW(), NOW()),
('wf_status', '已通过', 'APPROVED', 'green', 2, 1, NOW(), NOW()),
('wf_status', '已拒绝', 'REJECTED', 'red', 3, 1, NOW(), NOW()),
('wf_status', '已撤销', 'CANCELED', 'default', 4, 1, NOW(), NOW()),
('wf_task_action', '同意', 'APPROVE', 'green', 1, 1, NOW(), NOW()),
('wf_task_action', '拒绝', 'REJECT', 'red', 2, 1, NOW(), NOW()),
('wf_task_action', '转交', 'TRANSFER', 'blue', 3, 1, NOW(), NOW());

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2);

-- 普通用户：只能查看各管理页面，没有任何按钮权限；可以使用流程中心发起、审批自己的流程
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 100), (2, 104), (2, 101), (2, 102), (2, 103), (2, 109),
(2, 2), (2, 110), (2, 111), (2, 112), (2, 113), (2, 114), (2, 115),
(2, 3), (2, 120), (2, 121), (2, 122), (2, 123), (2, 124), (2, 125), (2, 126);

-- 打印格式示例：销售出库单除标准格式（默认）外预置「发货单（不含价格）」，给同行的发货单、代发第三方时只显示数量，
-- 标题为「发货单」，隐藏单价、税率、金额列和金额类表头字段。新租户由 SysPrintTenantInitializer 从平台复制
INSERT INTO sys_print_template (template_key, name, is_default, sort, status, config, create_time, update_time) VALUES
('scm.salesOut', '标准格式', 1, 0, 1, '{}', NOW(), NOW()),
('scm.salesOut', '发货单（不含价格）', 0, 1, 1, '{"title":"发货单","orientation":"portrait","paper":"A4","margin":10,"fontSize":10,"showCompany":true,"showNote":true,"fields":[{"label":"订单号"},{"label":"客户"},{"label":"仓库"},{"label":"业务日期"},{"label":"仓库单据"},{"label":"发票类型","hidden":true},{"label":"优惠前合计","hidden":true},{"label":"整单优惠","hidden":true},{"label":"原单优惠分摊","hidden":true},{"label":"优惠后合计","hidden":true},{"label":"价税合计","hidden":true},{"label":"运费","hidden":true},{"label":"运费承担","hidden":true},{"label":"含代垫运费合计","hidden":true},{"label":"原币","hidden":true}],"columns":[{"title":"序号"},{"title":"物料编码"},{"title":"物料名称"},{"title":"规格型号"},{"title":"单位"},{"title":"仓库"},{"title":"库位"},{"title":"批次"},{"title":"数量"},{"title":"含税单价","hidden":true},{"title":"税率","hidden":true},{"title":"价税合计","hidden":true},{"title":"序列号"}],"signs":[{"label":"制单"},{"label":"仓管"},{"label":"客户签收"}]}', NOW(), NOW());

-- ---------------------------------------------------------------- 仓库管理：菜单、字典、示例数据

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(4,   0, '仓库管理', 'M', '/wms', NULL, NULL, 'ShopOutlined', 2, 1, 1, NOW(), NOW()),
(130, 911, '入库单', 'C', 'in', 'wms/in/index', 'wms:in:list', 'ImportOutlined', 1, 1, 1, NOW(), NOW()),
(131, 911, '出库单', 'C', 'out', 'wms/out/index', 'wms:out:list', 'ExportOutlined', 2, 1, 1, NOW(), NOW()),
(132, 911, '调拨单', 'C', 'transfer', 'wms/transfer/index', 'wms:transfer:list', 'SwapOutlined', 3, 1, 1, NOW(), NOW()),
(133, 911, '盘点单', 'C', 'check', 'wms/check/index', 'wms:check:list', 'AuditOutlined', 4, 1, 1, NOW(), NOW()),
(134, 912, '库存查询', 'C', 'stock', 'wms/stock/index', 'wms:stock:list', 'DatabaseOutlined', 1, 1, 1, NOW(), NOW()),
(135, 912, '库存流水', 'C', 'flow', 'wms/flow/index', 'wms:flow:list', 'HistoryOutlined', 2, 1, 1, NOW(), NOW()),
(136, 912, '批次与序列号', 'C', 'trace', 'wms/trace/index', 'wms:trace:list', 'BarcodeOutlined', 3, 1, 1, NOW(), NOW()),
(137, 914, '物料管理', 'C', 'material', 'wms/material/index', 'wms:material:list', 'AppstoreOutlined', 1, 1, 1, NOW(), NOW()),
(138, 914, '仓库管理', 'C', 'warehouse', 'wms/warehouse/index', 'wms:warehouse:list', 'HomeOutlined', 2, 1, 1, NOW(), NOW()),
(139, 914, '库位管理', 'C', 'location', 'wms/location/index', 'wms:location:list', 'EnvironmentOutlined', 3, 1, 1, NOW(), NOW()),
(1300, 130, '入库单新增', 'F', NULL, NULL, 'wms:in:add', NULL, 1, 1, 1, NOW(), NOW()),
(1301, 130, '入库单修改', 'F', NULL, NULL, 'wms:in:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1302, 130, '入库单删除', 'F', NULL, NULL, 'wms:in:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1303, 130, '入库确认', 'F', NULL, NULL, 'wms:in:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1310, 131, '出库单新增', 'F', NULL, NULL, 'wms:out:add', NULL, 1, 1, 1, NOW(), NOW()),
(1311, 131, '出库单修改', 'F', NULL, NULL, 'wms:out:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1312, 131, '出库单删除', 'F', NULL, NULL, 'wms:out:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1313, 131, '出库确认', 'F', NULL, NULL, 'wms:out:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1320, 132, '调拨单新增', 'F', NULL, NULL, 'wms:transfer:add', NULL, 1, 1, 1, NOW(), NOW()),
(1321, 132, '调拨单修改', 'F', NULL, NULL, 'wms:transfer:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1322, 132, '调拨单删除', 'F', NULL, NULL, 'wms:transfer:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1323, 132, '调拨确认', 'F', NULL, NULL, 'wms:transfer:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1330, 133, '盘点单新增', 'F', NULL, NULL, 'wms:check:add', NULL, 1, 1, 1, NOW(), NOW()),
(1331, 133, '盘点单修改', 'F', NULL, NULL, 'wms:check:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1332, 133, '盘点单删除', 'F', NULL, NULL, 'wms:check:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1333, 133, '盘点确认', 'F', NULL, NULL, 'wms:check:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1370, 137, '物料新增', 'F', NULL, NULL, 'wms:material:add', NULL, 1, 1, 1, NOW(), NOW()),
(1371, 137, '物料修改', 'F', NULL, NULL, 'wms:material:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1372, 137, '物料删除', 'F', NULL, NULL, 'wms:material:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1380, 138, '仓库新增', 'F', NULL, NULL, 'wms:warehouse:add', NULL, 1, 1, 1, NOW(), NOW()),
(1381, 138, '仓库修改', 'F', NULL, NULL, 'wms:warehouse:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1382, 138, '仓库删除', 'F', NULL, NULL, 'wms:warehouse:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1390, 139, '库位新增', 'F', NULL, NULL, 'wms:location:add', NULL, 1, 1, 1, NOW(), NOW()),
(1391, 139, '库位修改', 'F', NULL, NULL, 'wms:location:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1392, 139, '库位删除', 'F', NULL, NULL, 'wms:location:remove', NULL, 3, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(11, '入库类型', 'wms_in_type', 1, '入库单的业务类型', NOW(), NOW()),
(12, '出库类型', 'wms_out_type', 1, '出库单的业务类型', NOW(), NOW()),
(13, '单据状态', 'wms_doc_status', 1, '仓库单据的状态', NOW(), NOW()),
(14, '库存业务类型', 'wms_biz_type', 1, '库存流水的业务类型', NOW(), NOW()),
(15, '序列号状态', 'wms_serial_status', 1, '序列号是否在库', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_in_type', '采购入库', 'PURCHASE', 'blue', 1, 1, NOW(), NOW()),
('wms_in_type', '销售退货', 'SALES_RETURN', 'orange', 3, 1, NOW(), NOW()),
('wms_in_type', '其他入库', 'OTHER', 'default', 4, 1, NOW(), NOW()),
('wms_out_type', '销售出库', 'SALES', 'green', 1, 1, NOW(), NOW()),
('wms_out_type', '采购退货', 'PURCHASE_RETURN', 'orange', 3, 1, NOW(), NOW()),
('wms_out_type', '其他出库', 'OTHER', 'default', 4, 1, NOW(), NOW()),
('wms_doc_status', '草稿', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('wms_doc_status', '已完成', 'DONE', 'green', 2, 1, NOW(), NOW()),
('wms_biz_type', '入库', 'IN', 'green', 1, 1, NOW(), NOW()),
('wms_biz_type', '出库', 'OUT', 'orange', 2, 1, NOW(), NOW()),
('wms_biz_type', '调拨', 'TRANSFER', 'blue', 3, 1, NOW(), NOW()),
('wms_biz_type', '盘点', 'CHECK', 'purple', 4, 1, NOW(), NOW()),
('wms_serial_status', '在库', 'IN_STOCK', 'green', 1, 1, NOW(), NOW()),
('wms_serial_status', '已出库', 'OUT', 'default', 2, 1, NOW(), NOW());

-- 普通用户：仓库管理各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 4), (2, 130), (2, 131), (2, 132), (2, 133), (2, 134), (2, 135), (2, 136), (2, 137), (2, 138), (2, 139);

-- 示例基础资料（没有库存，库存只能通过单据产生）
INSERT INTO wms_warehouse (id, code, name, store_id, address, contact, sort, status, create_time, update_time) VALUES
(1, 'WH01', '总部成品仓', 1, '上海市浦东新区 1 号库', '张三', 1, 1, NOW(), NOW()),
(2, 'WH02', '原材料仓', 1, '上海市浦东新区 2 号库', '李四', 2, 1, NOW(), NOW());

INSERT INTO wms_location (warehouse_id, code, name, sort, status, create_time, update_time) VALUES
(1, 'A-01', 'A 区 1 号货架', 1, 1, NOW(), NOW()),
(1, 'A-02', 'A 区 2 号货架', 2, 1, NOW(), NOW()),
(2, 'R-01', '原料区 1 排', 1, 1, NOW(), NOW());

INSERT INTO wms_material (code, name, spec, unit, category, batch_enabled, serial_enabled, safety_stock, status, create_time, update_time) VALUES
('M0001', '柠檬原浆', '5L/桶', '桶', 'RAW', 1, 0, 20, 1, NOW(), NOW()),
('M0002', '包装纸箱', '40×30×20cm', '个', 'PACKING', 0, 0, 100, 1, NOW(), NOW()),
('P0001', '智能榨汁机', 'LJ-200', '台', 'FINISHED', 0, 1, 5, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 门店：菜单、字典、总部

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(17,  0, '门店', 'M', '/store', NULL, NULL, 'ShopOutlined', 0, 1, 1, NOW(), NOW()),
(971, 17, '门店资料', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(972, 17, '价格管理', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(420, 971, '门店管理', 'C', 'store', 'store/store/index', 'store:store:list', 'ShopOutlined', 1, 1, 1, NOW(), NOW()),
(421, 971, '区域管理', 'C', 'region', 'store/region/index', 'store:region:list', 'GlobalOutlined', 2, 1, 1, NOW(), NOW()),
(4200, 420, '门店新增', 'F', NULL, NULL, 'store:store:add', NULL, 1, 1, 1, NOW(), NOW()),
(4201, 420, '门店修改', 'F', NULL, NULL, 'store:store:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4202, 420, '门店删除', 'F', NULL, NULL, 'store:store:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4210, 421, '区域新增', 'F', NULL, NULL, 'store:region:add', NULL, 1, 1, 1, NOW(), NOW()),
(4211, 421, '区域修改', 'F', NULL, NULL, 'store:region:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4212, 421, '区域删除', 'F', NULL, NULL, 'store:region:remove', NULL, 3, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(220, '门店类型', 'store_type', 'SYSTEM', 1, '总部每个租户唯一', NOW(), NOW()),
(221, '门店状态', 'store_status', 'SYSTEM', 1, '停业、闭店的门店不能开单', NOW(), NOW()),
(222, '加盟结算方式', 'store_settle_mode', 'SYSTEM', 1, '加盟店向总部进货的结算方式', NOW(), NOW()),
(223, '门店范围', 'sys_store_scope', 'SYSTEM', 1, '用户可管辖的门店：全部门店，或所属门店 + 指定的门店', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('store_type', '总部', 'HQ', 'red', 1, 1, NOW(), NOW()),
('store_type', '直营店', 'DIRECT', 'blue', 2, 1, NOW(), NOW()),
('store_type', '加盟店', 'FRANCHISE', 'purple', 3, 1, NOW(), NOW()),
('store_type', '配送中心', 'DC', 'cyan', 4, 1, NOW(), NOW()),
('store_status', '筹备', 'PREPARING', 'orange', 1, 1, NOW(), NOW()),
('store_status', '营业', 'OPEN', 'green', 2, 1, NOW(), NOW()),
('store_status', '停业', 'SUSPENDED', 'default', 3, 1, NOW(), NOW()),
('store_status', '闭店', 'CLOSED', 'red', 4, 1, NOW(), NOW()),
('store_settle_mode', '预付货款', 'PREPAID', 'blue', 1, 1, NOW(), NOW()),
('store_settle_mode', '月结', 'CREDIT', 'orange', 2, 1, NOW(), NOW()),
('sys_store_scope', '全部门店', 'ALL', 'green', 1, 1, NOW(), NOW()),
('sys_store_scope', '指定门店', 'ASSIGNED', 'blue', 2, 1, NOW(), NOW());

-- 普通用户只读；总经理全部
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 17), (2, 971), (2, 972), (2, 420), (2, 421),
(3, 17), (3, 971), (3, 972), (3, 420), (3, 421), (3, 4200), (3, 4201), (3, 4202), (3, 4210), (3, 4211), (3, 4212);

-- 总部门店（新租户由 StoreTenantInitializer 创建）：默认仓库为总部成品仓，默认结算账户为库存现金
INSERT INTO store_info (id, code, name, type, status, default_warehouse_id, default_account_id, sort, create_time, update_time) VALUES
(1, 'MD000', '总部', 'HQ', 'OPEN', 1, 1, 0, NOW(), NOW());

-- 价格管理：价格方案、调价单
-- 定时任务「调价单定时生效」（scm.priceAdjust，每 5 分钟）由 ScmPriceAdjustJob 在首次启动时写入 sys_job，无需 INSERT
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(425, 972, '价格方案', 'C', 'price-scheme', 'scm/price-scheme/index', 'scm:priceScheme:list', 'TagsOutlined', 1, 1, 1, NOW(), NOW()),
(426, 972, '调价单', 'C', 'price-adjust', 'scm/price-adjust/index', 'scm:priceAdjust:list', 'RiseOutlined', 2, 1, 1, NOW(), NOW()),
(4250, 425, '价格方案新增', 'F', NULL, NULL, 'scm:priceScheme:add', NULL, 1, 1, 1, NOW(), NOW()),
(4251, 425, '价格方案修改', 'F', NULL, NULL, 'scm:priceScheme:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4252, 425, '价格方案删除', 'F', NULL, NULL, 'scm:priceScheme:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4260, 426, '调价单新增', 'F', NULL, NULL, 'scm:priceAdjust:add', NULL, 1, 1, 1, NOW(), NOW()),
(4261, 426, '调价单修改', 'F', NULL, NULL, 'scm:priceAdjust:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4262, 426, '调价单删除', 'F', NULL, NULL, 'scm:priceAdjust:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4263, 426, '调价单审核', 'F', NULL, NULL, 'scm:priceAdjust:audit', NULL, 4, 1, 1, NOW(), NOW());
-- 普通用户只读；总经理全部
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 425), (2, 426),
(3, 425), (3, 426), (3, 4250), (3, 4251), (3, 4252), (3, 4260), (3, 4261), (3, 4262), (3, 4263);

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(240, '调价单状态', 'scm_price_adjust_status', 'SYSTEM', 1, '草稿 → 审批中 → 待生效 → 已生效；待生效的可以作废', NOW(), NOW()),
(241, '调价对象', 'scm_price_adjust_target', 'SYSTEM', 1, '调价单调整总部零售价还是门店价格方案', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('scm_price_adjust_status', '草稿', 'DRAFT', 'default', 1, 1, 1, NOW(), NOW()),
('scm_price_adjust_status', '审批中', 'APPROVING', 'processing', 2, 1, 1, NOW(), NOW()),
('scm_price_adjust_status', '待生效', 'APPROVED', 'blue', 3, 1, 1, NOW(), NOW()),
('scm_price_adjust_status', '已生效', 'EFFECTIVE', 'green', 4, 1, 1, NOW(), NOW()),
('scm_price_adjust_status', '已作废', 'VOIDED', 'red', 5, 1, 1, NOW(), NOW()),
('scm_price_adjust_target', '总部价', 'HQ', 'purple', 1, 1, 1, NOW(), NOW()),
('scm_price_adjust_target', '价格方案', 'SCHEME', 'cyan', 2, 1, 1, NOW(), NOW());
-- 调价单审批（内置流程，未发布）：发布后调价单提交即进入审批，通过后待生效 / 生效
INSERT INTO wf_model (model_key, name, category, sort, status, design_json, remark, create_time, update_time) VALUES
('scm_price_adjust', '调价单审批', 'SALES', 30, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}}', '内置流程（未发布）：部门主管审批。发布后调价单提交即进入审批，通过后到生效时间生效；流程变量 target 调价对象、itemCount 调价商品数、adjustNo 调价单号', NOW(), NOW());

-- ---------------------------------------------------------------- 供应链管理：菜单、字典

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(5,   0, '供应链管理', 'M', '/scm', NULL, NULL, 'DeploymentUnitOutlined', 1, 1, 1, NOW(), NOW()),
(140, 901, '采购订单', 'C', 'purchase', 'scm/purchase/index', 'scm:purchase:list', 'ShoppingCartOutlined', 3, 1, 1, NOW(), NOW()),
(141, 901, '采购入库', 'C', 'purchase-in', 'scm/purchaseIn/index', 'scm:purchaseIn:list', 'ImportOutlined', 4, 1, 1, NOW(), NOW()),
(142, 901, '采购退货', 'C', 'purchase-return', 'scm/purchaseReturn/index', 'scm:purchaseReturn:list', 'RollbackOutlined', 5, 1, 1, NOW(), NOW()),
(143, 902, '销售订单', 'C', 'sales', 'scm/sales/index', 'scm:sales:list', 'ShoppingOutlined', 3, 1, 1, NOW(), NOW()),
(144, 902, '销售出库', 'C', 'sales-out', 'scm/salesOut/index', 'scm:salesOut:list', 'ExportOutlined', 5, 1, 1, NOW(), NOW()),
(145, 902, '销售退货', 'C', 'sales-return', 'scm/salesReturn/index', 'scm:salesReturn:list', 'UndoOutlined', 6, 1, 1, NOW(), NOW()),
(147, 903, '供应商', 'C', 'supplier', 'scm/supplier/index', 'scm:supplier:list', 'BankOutlined', 1, 1, 1, NOW(), NOW()),
(149, 903, '客户', 'C', 'customer', 'scm/customer/index', 'scm:customer:list', 'ContactsOutlined', 3, 1, 1, NOW(), NOW()),
(1400, 140, '采购订单新增', 'F', NULL, NULL, 'scm:purchase:add', NULL, 1, 1, 1, NOW(), NOW()),
(1401, 140, '采购订单修改', 'F', NULL, NULL, 'scm:purchase:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1402, 140, '采购订单删除', 'F', NULL, NULL, 'scm:purchase:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1403, 140, '采购订单审核', 'F', NULL, NULL, 'scm:purchase:approve', NULL, 4, 1, 1, NOW(), NOW()),
(1404, 140, '采购订单关闭', 'F', NULL, NULL, 'scm:purchase:close', NULL, 5, 1, 1, NOW(), NOW()),
(1410, 141, '采购入库', 'F', NULL, NULL, 'scm:purchaseIn:add', NULL, 1, 1, 1, NOW(), NOW()),
(1420, 142, '采购退货', 'F', NULL, NULL, 'scm:purchaseReturn:add', NULL, 1, 1, 1, NOW(), NOW()),
(1430, 143, '销售订单新增', 'F', NULL, NULL, 'scm:sales:add', NULL, 1, 1, 1, NOW(), NOW()),
(1431, 143, '销售订单修改', 'F', NULL, NULL, 'scm:sales:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1432, 143, '销售订单删除', 'F', NULL, NULL, 'scm:sales:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1433, 143, '销售订单审核', 'F', NULL, NULL, 'scm:sales:approve', NULL, 4, 1, 1, NOW(), NOW()),
(1434, 143, '销售订单关闭', 'F', NULL, NULL, 'scm:sales:close', NULL, 5, 1, 1, NOW(), NOW()),
(1440, 144, '销售出库', 'F', NULL, NULL, 'scm:salesOut:add', NULL, 1, 1, 1, NOW(), NOW()),
(1450, 145, '销售退货', 'F', NULL, NULL, 'scm:salesReturn:add', NULL, 1, 1, 1, NOW(), NOW()),
(1470, 147, '供应商新增', 'F', NULL, NULL, 'scm:supplier:add', NULL, 1, 1, 1, NOW(), NOW()),
(1471, 147, '供应商修改', 'F', NULL, NULL, 'scm:supplier:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1472, 147, '供应商删除', 'F', NULL, NULL, 'scm:supplier:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1490, 149, '客户新增', 'F', NULL, NULL, 'scm:customer:add', NULL, 1, 1, 1, NOW(), NOW()),
(1491, 149, '客户修改', 'F', NULL, NULL, 'scm:customer:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1492, 149, '客户删除', 'F', NULL, NULL, 'scm:customer:remove', NULL, 3, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(16, '供应商等级', 'scm_supplier_level', 1, '供应商评估的评定等级', NOW(), NOW()),
(17, '客户等级', 'scm_customer_level', 1, '客户分级', NOW(), NOW()),
(18, '订单状态', 'scm_order_status', 1, '采购、销售订单的状态', NOW(), NOW()),
(19, '出入库种类', 'scm_trade_kind', 1, '采购入库 / 退货、销售出库 / 退货', NOW(), NOW()),
(22, '订单来源', 'scm_order_source', 1, '手工录入、补货建议生成或直接开单', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_supplier_level', 'A 优秀', 'A', 'green', 1, 1, NOW(), NOW()),
('scm_supplier_level', 'B 良好', 'B', 'blue', 2, 1, NOW(), NOW()),
('scm_supplier_level', 'C 合格', 'C', 'orange', 3, 1, NOW(), NOW()),
('scm_supplier_level', 'D 待改进', 'D', 'red', 4, 1, NOW(), NOW()),
('scm_customer_level', '重点客户', 'VIP', 'gold', 1, 1, NOW(), NOW()),
('scm_customer_level', '普通客户', 'NORMAL', 'blue', 2, 1, NOW(), NOW()),
('scm_customer_level', '潜在客户', 'POTENTIAL', 'default', 3, 1, NOW(), NOW()),
('scm_order_status', '草稿', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('scm_order_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('scm_order_status', '已审核', 'APPROVED', 'blue', 3, 1, NOW(), NOW()),
('scm_order_status', '部分完成', 'PARTIAL', 'cyan', 4, 1, NOW(), NOW()),
('scm_order_status', '已完成', 'COMPLETED', 'green', 5, 1, NOW(), NOW()),
('scm_order_status', '已关闭', 'CLOSED', 'default', 6, 1, NOW(), NOW()),
('scm_trade_kind', '采购入库', 'PURCHASE_IN', 'blue', 1, 1, NOW(), NOW()),
('scm_trade_kind', '采购退货', 'PURCHASE_RETURN', 'orange', 2, 1, NOW(), NOW()),
('scm_trade_kind', '销售出库', 'SALES_OUT', 'green', 3, 1, NOW(), NOW()),
('scm_trade_kind', '销售退货', 'SALES_RETURN', 'red', 4, 1, NOW(), NOW()),
('scm_order_source', '手工', 'MANUAL', 'default', 1, 1, NOW(), NOW()),
('scm_order_source', '补货建议', 'REORDER', 'cyan', 2, 1, NOW(), NOW());

-- 普通用户：供应链各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 5), (2, 140), (2, 141), (2, 142), (2, 143), (2, 144), (2, 145), (2, 147), (2, 149);

-- 示例往来单位
INSERT INTO scm_supplier (id, code, name, level, contact, phone, address, payment_days, status, create_time, update_time) VALUES
(1, 'S001', '上海鲜果供应链有限公司', 'A', '王经理', '021-55550001', '上海市闵行区', 30, 1, NOW(), NOW()),
(2, 'S002', '华东包装材料厂', 'B', '赵经理', '0571-55550002', '浙江省杭州市', 60, 1, NOW(), NOW());

INSERT INTO scm_customer (id, code, name, level, contact, phone, address, credit_limit, payment_days, status, create_time, update_time) VALUES
(1, 'C001', '鲜饮连锁（上海）有限公司', 'VIP', '陈店长', '021-66660001', '上海市黄浦区南京东路 100 号', 500000, 30, 1, NOW(), NOW()),
(2, 'C002', '优品商贸有限公司', 'NORMAL', '孙经理', '010-66660002', '北京市朝阳区建国路 88 号', 100000, 15, 1, NOW(), NOW());

INSERT INTO scm_supplier_price (supplier_id, material_id, price, tax_rate, lead_days, min_qty, is_default, create_time, update_time) VALUES
(1, 1, 120, 13, 3, 10, 1, NOW(), NOW()),
(2, 2, 2.5, 13, 5, 500, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 财务管理：菜单、字典、会计科目

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(7,   0, '财务管理', 'M', '/fin', NULL, NULL, 'AccountBookOutlined', 4, 1, 1, NOW(), NOW()),
(180, 931, '凭证管理', 'C', 'voucher', 'fin/voucher/index', 'fin:voucher:list', 'FileTextOutlined', 1, 1, 1, NOW(), NOW()),
(181, 931, '账簿查询', 'C', 'book', 'fin/book/index', 'fin:book:list', 'ReadOutlined', 2, 1, 1, NOW(), NOW()),
(182, 931, '财务报表', 'C', 'report', 'fin/report/index', 'fin:report:list', 'PieChartOutlined', 3, 1, 1, NOW(), NOW()),
(183, 932, '应收管理', 'C', 'ar', 'fin/ar/index', 'fin:ar:list', 'RiseOutlined', 1, 1, 1, NOW(), NOW()),
(184, 932, '应付管理', 'C', 'ap', 'fin/ap/index', 'fin:ap:list', 'FallOutlined', 2, 1, 1, NOW(), NOW()),
(185, 932, '收款单', 'C', 'receipt', 'fin/receipt/index', 'fin:receipt:list', 'TransactionOutlined', 3, 1, 1, NOW(), NOW()),
(186, 932, '付款单', 'C', 'payment', 'fin/payment/index', 'fin:payment:list', 'PayCircleOutlined', 4, 1, 1, NOW(), NOW()),
(190, 936, '会计科目', 'C', 'account', 'fin/account/index', 'fin:account:list', 'OrderedListOutlined', 2, 1, 1, NOW(), NOW()),
(191, 936, '期末结账', 'C', 'period', 'fin/period/index', 'fin:period:list', 'LockOutlined', 1, 1, 1, NOW(), NOW()),
(1800, 180, '凭证新增', 'F', NULL, NULL, 'fin:voucher:add', NULL, 1, 1, 1, NOW(), NOW()),
(1801, 180, '凭证修改', 'F', NULL, NULL, 'fin:voucher:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1802, 180, '凭证删除', 'F', NULL, NULL, 'fin:voucher:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1803, 180, '凭证审核', 'F', NULL, NULL, 'fin:voucher:audit', NULL, 4, 1, 1, NOW(), NOW()),
(1804, 180, '凭证记账', 'F', NULL, NULL, 'fin:voucher:post', NULL, 5, 1, 1, NOW(), NOW()),
(1830, 183, '应收单新增', 'F', NULL, NULL, 'fin:ar:add', NULL, 1, 1, 1, NOW(), NOW()),
(1831, 183, '应收单删除', 'F', NULL, NULL, 'fin:ar:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1832, 183, '应收生成凭证', 'F', NULL, NULL, 'fin:ar:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1840, 184, '应付单新增', 'F', NULL, NULL, 'fin:ap:add', NULL, 1, 1, 1, NOW(), NOW()),
(1841, 184, '应付单删除', 'F', NULL, NULL, 'fin:ap:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1842, 184, '应付生成凭证', 'F', NULL, NULL, 'fin:ap:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1850, 185, '收款单新增', 'F', NULL, NULL, 'fin:receipt:add', NULL, 1, 1, 1, NOW(), NOW()),
(1851, 185, '收款单删除', 'F', NULL, NULL, 'fin:receipt:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1852, 185, '收款生成凭证', 'F', NULL, NULL, 'fin:receipt:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1860, 186, '付款单新增', 'F', NULL, NULL, 'fin:payment:add', NULL, 1, 1, 1, NOW(), NOW()),
(1861, 186, '付款单删除', 'F', NULL, NULL, 'fin:payment:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1862, 186, '付款生成凭证', 'F', NULL, NULL, 'fin:payment:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1900, 190, '科目新增', 'F', NULL, NULL, 'fin:account:add', NULL, 1, 1, 1, NOW(), NOW()),
(1901, 190, '科目修改', 'F', NULL, NULL, 'fin:account:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1902, 190, '科目删除', 'F', NULL, NULL, 'fin:account:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1910, 191, '结账', 'F', NULL, NULL, 'fin:period:close', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(40, '科目类别', 'fin_account_category', 1, '会计科目的类别', NOW(), NOW()),
(41, '凭证状态', 'fin_voucher_status', 1, '记账凭证的状态', NOW(), NOW()),
(42, '凭证来源', 'fin_voucher_source', 1, '手工或业务生成', NOW(), NOW()),
(43, '核销状态', 'fin_bill_status', 1, '应收应付单的核销状态', NOW(), NOW()),
(44, '应收应付来源', 'fin_bill_source', 1, '应收应付单的来源', NOW(), NOW()),
(45, '结算方式', 'fin_settle_method', 1, '收付款的结算方式', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_account_category', '资产', 'ASSET', 'blue', 1, 1, NOW(), NOW()),
('fin_account_category', '负债', 'LIABILITY', 'orange', 2, 1, NOW(), NOW()),
('fin_account_category', '所有者权益', 'EQUITY', 'purple', 3, 1, NOW(), NOW()),
('fin_account_category', '成本', 'COST', 'cyan', 4, 1, NOW(), NOW()),
('fin_account_category', '损益', 'PROFIT', 'green', 5, 1, NOW(), NOW()),
('fin_voucher_status', '未审核', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('fin_voucher_status', '已审核', 'AUDITED', 'blue', 2, 1, NOW(), NOW()),
('fin_voucher_status', '已记账', 'POSTED', 'green', 3, 1, NOW(), NOW()),
('fin_voucher_source', '手工', 'MANUAL', 'default', 1, 1, NOW(), NOW()),
('fin_voucher_source', '应收', 'AR', 'green', 2, 1, NOW(), NOW()),
('fin_voucher_source', '应付', 'AP', 'orange', 3, 1, NOW(), NOW()),
('fin_voucher_source', '收款', 'RECEIPT', 'cyan', 4, 1, NOW(), NOW()),
('fin_voucher_source', '付款', 'PAYMENT', 'gold', 5, 1, NOW(), NOW()),
('fin_voucher_source', '结转损益', 'CLOSE', 'red', 9, 1, NOW(), NOW()),
('fin_bill_status', '未核销', 'UNSETTLED', 'orange', 1, 1, NOW(), NOW()),
('fin_bill_status', '部分核销', 'PARTIAL', 'blue', 2, 1, NOW(), NOW()),
('fin_bill_status', '已核销', 'SETTLED', 'green', 3, 1, NOW(), NOW()),
('fin_bill_source', '销售出库', 'SALES_OUT', 'green', 1, 1, NOW(), NOW()),
('fin_bill_source', '销售退货', 'SALES_RETURN', 'red', 2, 1, NOW(), NOW()),
('fin_bill_source', '采购入库', 'PURCHASE_IN', 'blue', 3, 1, NOW(), NOW()),
('fin_bill_source', '采购退货', 'PURCHASE_RETURN', 'orange', 4, 1, NOW(), NOW()),
('fin_bill_source', '期初 / 手工', 'MANUAL', 'default', 5, 1, NOW(), NOW()),
('fin_settle_method', '银行转账', 'TRANSFER', 'default', 1, 1, NOW(), NOW()),
('fin_settle_method', '现金', 'CASH', 'default', 2, 1, NOW(), NOW()),
('fin_settle_method', '承兑汇票', 'DRAFT', 'default', 3, 1, NOW(), NOW()),
('fin_settle_method', '支付宝 / 微信', 'ONLINE', 'default', 4, 1, NOW(), NOW());

-- 普通用户：财务管理各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 7), (2, 180), (2, 181), (2, 182), (2, 183), (2, 184), (2, 185), (2, 186), (2, 190), (2, 191);

-- 会计科目（企业会计准则常用科目）：自动生成凭证使用的科目见 AccountRole（可在「凭证科目设置」中修改），未设置时按编码取预置科目，需保持为末级科目
INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(1,  '1001',   '库存现金',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(2,  '1002',   '银行存款',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(3,  '100201', '工商银行',               2,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(4,  '100202', '招商银行',               2,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(5,  '1122',   '应收账款',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(6,  '1123',   '预付账款',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(7,  '1221',   '其他应收款',             0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(8,  '1403',   '原材料',                 0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(9,  '1405',   '库存商品',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(10, '1601',   '固定资产',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(11, '1602',   '累计折旧',               0,  'ASSET',     'CREDIT', 1, NOW(), NOW()),
(12, '1606',   '固定资产清理',           0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(13, '1901',   '待处理财产损溢',         0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(20, '2001',   '短期借款',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(21, '2202',   '应付账款',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(22, '2203',   '预收账款',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(23, '2211',   '应付职工薪酬',           0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(24, '2221',   '应交税费',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(25, '222101', '应交增值税（进项税额）', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(26, '222102', '应交增值税（销项税额）', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(27, '2241',   '其他应付款',             0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(30, '4001',   '实收资本',               0,  'EQUITY',    'CREDIT', 1, NOW(), NOW()),
(31, '4103',   '本年利润',               0,  'EQUITY',    'CREDIT', 1, NOW(), NOW()),
(32, '4104',   '利润分配',               0,  'EQUITY',    'CREDIT', 1, NOW(), NOW()),
(40, '5001',   '生产成本',               0,  'COST',      'DEBIT',  1, NOW(), NOW()),
(41, '5101',   '制造费用',               0,  'COST',      'DEBIT',  1, NOW(), NOW()),
(50, '6001',   '主营业务收入',           0,  'PROFIT',    'CREDIT', 1, NOW(), NOW()),
(51, '6051',   '其他业务收入',           0,  'PROFIT',    'CREDIT', 1, NOW(), NOW()),
(52, '6301',   '营业外收入',             0,  'PROFIT',    'CREDIT', 1, NOW(), NOW()),
(53, '6401',   '主营业务成本',           0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(54, '6403',   '税金及附加',             0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(55, '6601',   '销售费用',               0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(56, '6602',   '管理费用',               0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(57, '660201', '工资',                   56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(58, '660202', '办公费',                 56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(59, '660203', '折旧费',                 56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(60, '660204', '差旅费',                 56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(61, '6603',   '财务费用',               0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(62, '6711',   '营业外支出',             0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(63, '6801',   '所得税费用',             0,  'PROFIT',    'DEBIT',  1, NOW(), NOW());
-- 现金、银行的末级科目（结算账户）归属总部门店
UPDATE fin_account SET store_id = 1 WHERE code IN ('1001', '100201', '100202');
-- 往来科目按客户 / 供应商辅助核算
UPDATE fin_account SET aux_items = 'CUSTOMER' WHERE code IN ('1122', '2203');
UPDATE fin_account SET aux_items = 'SUPPLIER' WHERE code IN ('1123', '2202');

-- ---------------------------------------------------------------- 商务智能：菜单、字典、预警规则

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(11,  0,  '商务智能', 'M', '/bi', NULL, NULL, 'DotChartOutlined', 8, 1, 1, NOW(), NOW()),
(260, 11, '经营驾驶舱', 'C', 'cockpit', 'bi/cockpit/index', 'bi:cockpit:list', 'DashboardOutlined', 1, 1, 1, NOW(), NOW()),
(261, 11, '多维分析', 'C', 'olap', 'bi/olap/index', 'bi:olap:list', 'AppstoreOutlined', 2, 1, 1, NOW(), NOW()),
(262, 11, '自定义报表', 'C', 'report', 'bi/report/index', 'bi:report:list', 'TableOutlined', 3, 1, 1, NOW(), NOW()),
(263, 11, '自定义仪表盘', 'C', 'board', 'bi/board/index', 'bi:board:list', 'LayoutOutlined', 4, 1, 1, NOW(), NOW()),
(264, 11, '决策支持', 'C', 'decision', 'bi/decision/index', 'bi:decision:list', 'ExperimentOutlined', 5, 1, 1, NOW(), NOW()),
(265, 11, '数据仓库', 'C', 'etl', 'bi/etl/index', 'bi:etl:list', 'DatabaseOutlined', 6, 1, 1, NOW(), NOW()),
(2620, 262, '报表新增', 'F', NULL, NULL, 'bi:report:add', NULL, 1, 1, 1, NOW(), NOW()),
(2621, 262, '报表修改', 'F', NULL, NULL, 'bi:report:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2622, 262, '报表删除', 'F', NULL, NULL, 'bi:report:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2630, 263, '仪表盘新增', 'F', NULL, NULL, 'bi:board:add', NULL, 1, 1, 1, NOW(), NOW()),
(2631, 263, '仪表盘修改', 'F', NULL, NULL, 'bi:board:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2632, 263, '仪表盘删除', 'F', NULL, NULL, 'bi:board:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2640, 264, '预警规则维护', 'F', NULL, NULL, 'bi:alert:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2650, 265, '立即同步', 'F', NULL, NULL, 'bi:etl:run', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(95, '同步状态', 'bi_etl_status', 1, '数据仓库同步任务的状态', NOW(), NOW()),
(96, '预警级别', 'bi_alert_level', 1, '预警规则的级别', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, remark, create_time, update_time) VALUES
('bi_etl_status', '运行中', 'RUNNING', 'processing', 1, 1, NULL, NOW(), NOW()),
('bi_etl_status', '成功', 'SUCCESS', 'green', 2, 1, NULL, NOW(), NOW()),
('bi_etl_status', '失败', 'FAILED', 'red', 3, 1, NULL, NOW(), NOW()),
('bi_alert_level', '提示', 'INFO', 'blue', 1, 1, NULL, NOW(), NOW()),
('bi_alert_level', '警告', 'WARN', 'orange', 2, 1, NULL, NOW(), NOW()),
('bi_alert_level', '严重', 'DANGER', 'red', 3, 1, NULL, NOW(), NOW());

-- 普通用户：商务智能各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 11), (2, 260), (2, 261), (2, 262), (2, 263), (2, 264), (2, 265);

INSERT INTO bi_alert_rule (id, name, metric, operator, threshold, level, enabled, remark, create_time, update_time) VALUES
(1, '应收账款逾期', 'AR_OVERDUE', 'GT', 0, 'WARN', 1, '催收逾期款项，必要时暂停发货', NOW(), NOW()),
(2, '库存低于安全库存', 'STOCK_BELOW_SAFETY', 'GT', 0, 'WARN', 1, '及时补货（可在补货建议中生成采购订单）', NOW(), NOW()),
(3, '呆滞物料', 'SLOW_MOVING', 'GT', 0, 'INFO', 1, '90 天没有出库的物料，考虑促销或调拨', NOW(), NOW()),
(4, '本月毛利率偏低', 'GROSS_MARGIN', 'LT', 15, 'DANGER', 1, '检查销售价格与采购成本', NOW(), NOW());

-- ---------------------------------------------------------------- 模块联动：凭证来源、应收来源、出库类型、订单来源、流程分类

INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(64, '222103', '应交个人所得税', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW());

-- 账务修正：补充会计科目；期初余额菜单
INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(65, '1408', '委托加工物资', 0, 'ASSET', 'DEBIT', 1, NOW(), NOW()),
(66, '5401', '合同履约成本', 0, 'COST', 'DEBIT', 1, NOW(), NOW()),
(67, '2205', '合同负债', 0, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(68, '660205', '培训费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(192, 936, '期初余额', 'C', 'opening', 'fin/opening/index', 'fin:opening:list', 'FlagOutlined', 3, 1, 1, NOW(), NOW()),
(1920, 192, '期初修改', 'F', NULL, NULL, 'fin:opening:edit', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 192);

-- 站内消息类别
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(101, '消息类别', 'sys_message_category', 1, '站内消息的类别', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_message_category', '审批', 'APPROVAL', 'blue', 1, 1, NOW(), NOW()),
('sys_message_category', '提醒', 'REMIND', 'orange', 2, 1, NOW(), NOW()),
('sys_message_category', '预警', 'ALERT', 'red', 3, 1, NOW(), NOW()),
('sys_message_category', '系统', 'SYSTEM', 'default', 4, 1, NOW(), NOW()),
('sys_message_category', '公告', 'NOTICE', 'cyan', 5, 1, NOW(), NOW());

-- 内置流程示例：未发布，业务单据仍直接审核；在「流程管理」中按需调整并发布后启用
INSERT INTO wf_model (model_key, name, category, sort, status, design_json, remark, create_time, update_time) VALUES
('scm_purchase_order', '采购订单审批', 'PURCHASE', 1, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按订单金额", "next": {"id": "cc_acct", "type": "CC", "name": "抄送会计", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 5 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "50000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：部门主管 → 金额 ≥ 5 万元时总经理 → 抄送会计。发布后采购订单「审核」改为提交审批，审批通过后变为已审核；可在流程设计中调整审批人后发布', NOW(), NOW()),
('scm_sales_order', '销售订单审批', 'SALES', 2, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "销售主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按订单金额", "next": {"id": "cc_wh", "type": "CC", "name": "抄送仓库管理员", "assignee": {"type": "ROLE", "ids": [6], "names": ["仓库管理员"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 20 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "200000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：销售主管 → 金额 ≥ 20 万元时总经理 → 抄送仓库。发布后销售订单「审核」改为提交审批（先校验信用额度），审批通过后变为已审核；可在流程设计中调整审批人后发布', NOW(), NOW()),
('fin_voucher', '凭证审批', 'FINANCE', 8, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "财务主管审核", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}}', '内置流程（未发布）：财务主管审核。发布后凭证不能直接审核，由制单人提交审批，通过后变为已审核；可在流程设计中调整审批人后发布', NOW(), NOW()),
('fin_payment', '付款审批', 'FINANCE', 9, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "财务主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按付款金额", "next": {"id": "cc_cashier", "type": "CC", "name": "抄送出纳", "assignee": {"type": "ROLE", "ids": [10], "names": ["出纳"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 10 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "100000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：财务主管 → 金额 ≥ 10 万元时总经理 → 抄送出纳。发布后付款单需审批，通过后核销应付单；可在流程设计中调整审批人后发布', NOW(), NOW()),
('wms_check', '盘点差异审批', 'INVENTORY', 14, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "仓库主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "acct", "type": "APPROVER", "name": "会计审核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "cc_gm", "type": "CC", "name": "抄送总经理", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "next": null}}}}}', '内置流程（未发布）：仓库主管 → 会计。发布后盘点单确认需审批，通过后才调整库存；流程变量 gainQty 盘盈数量、lossQty 盘亏数量、diffLines 差异行数', NOW(), NOW()),
('scm_order_change', '订单变更审批', 'SALES', 18, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按金额变化", "next": null, "branches": [{"id": "amount_check_yes", "name": "增加或减少 ≥ 5 万元", "defaultBranch": false, "conditions": [[{"field": "amountDelta", "operator": "GE", "value": "50000"}], [{"field": "amountDelta", "operator": "LE", "value": "-50000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：部门主管 → 金额变化 ≥ 5 万元时总经理。发布后已审核订单的变更单需审批，通过后生效；流程变量 kind 订单种类、amountDelta 金额变化', NOW(), NOW()),
('wms_stock_doc', '出入库审批', 'INVENTORY', 19, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "仓库主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "biz_check", "type": "CONDITION", "name": "按业务类型", "next": null, "branches": [{"id": "biz_check_yes", "name": "报废、其他出入库", "defaultBranch": false, "conditions": [[{"field": "bizType", "operator": "IN", "value": "SCRAP,SCRAP_IN,OTHER"}]], "next": {"id": "acct", "type": "APPROVER", "name": "会计审核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "biz_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：仓库主管 → 报废、其他出入库时会计。发布后手工出入库单、调拨单确认前需审批，通过后更新库存；流程变量 kind（IN / OUT / TRANSFER）、bizType、totalQty', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_voucher_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('sys_approve_status', '已生效', 'APPROVED', 'green', 1, 1, NOW(), NOW()),
('sys_approve_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('sys_approve_status', '已拒绝', 'REJECTED', 'red', 3, 1, NOW(), NOW()),
('sys_approve_status', '已撤回', 'CANCELED', 'default', 4, 1, NOW(), NOW()),
('sys_approve_status', '已生效', 'DONE', 'green', 5, 1, NOW(), NOW()),
('sys_approve_status', '草稿', 'DRAFT', 'default', 6, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(102, '审批状态', 'sys_approve_status', 1, '需审批生效的业务记录（人事异动、付款、项目费用）的状态', NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(270, 902, '销售价目表', 'C', 'price-list', 'scm/priceList/index', 'scm:price:list', 'TagsOutlined', 2, 1, 1, NOW(), NOW()),
(2700, 270, '价目表维护', 'F', NULL, NULL, 'scm:price:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 270);

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(271, 902, '发货通知', 'C', 'delivery', 'scm/delivery/index', 'scm:delivery:list', 'SendOutlined', 4, 1, 1, NOW(), NOW()),
(272, 902, '欠交清单', 'C', 'backorder', 'scm/backorder/index', 'scm:backorder:list', 'FieldTimeOutlined', 8, 1, 1, NOW(), NOW()),
(273, 913, '库存预留', 'C', 'reservation', 'wms/reservation/index', 'wms:reservation:list', 'PushpinOutlined', 1, 1, 1, NOW(), NOW()),
(1435, 143, '预留库存', 'F', NULL, NULL, 'scm:sales:reserve', NULL, 6, 1, 1, NOW(), NOW()),
(2710, 271, '发货通知新增', 'F', NULL, NULL, 'scm:delivery:add', NULL, 1, 1, 1, NOW(), NOW()),
(2711, 271, '拣货', 'F', NULL, NULL, 'scm:delivery:pick', NULL, 2, 1, 1, NOW(), NOW()),
(2730, 273, '释放预留', 'F', NULL, NULL, 'wms:reservation:release', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 271), (2, 272), (2, 273);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(107, '发货通知状态', 'scm_delivery_status', 1, '发货通知单的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_delivery_status', '待拣货', 'PENDING', 'orange', 1, 1, NOW(), NOW()),
('scm_delivery_status', '已拣货', 'PICKED', 'blue', 2, 1, NOW(), NOW()),
('scm_delivery_status', '已出库', 'SHIPPED', 'green', 3, 1, NOW(), NOW()),
('scm_delivery_status', '已取消', 'CANCELLED', 'default', 4, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1304, 130, '入库冲销', 'F', NULL, NULL, 'wms:in:reverse', NULL, 5, 1, 1, NOW(), NOW()),
(1314, 131, '出库冲销', 'F', NULL, NULL, 'wms:out:reverse', NULL, 5, 1, 1, NOW(), NOW()),
(1324, 132, '调拨冲销', 'F', NULL, NULL, 'wms:transfer:reverse', NULL, 5, 1, 1, NOW(), NOW()),
(1325, 132, '调拨收货', 'F', NULL, NULL, 'wms:transfer:receive', NULL, 6, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(108, '仓库类型', 'wms_warehouse_type', 1, '不良品仓的库存不计入 MRP、齐套、可承诺量', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_warehouse_type', '普通仓', 'NORMAL', 'default', 1, 1, NOW(), NOW()),
('wms_warehouse_type', '原料仓', 'RAW', 'cyan', 2, 1, NOW(), NOW()),
('wms_warehouse_type', '成品仓', 'FINISHED', 'green', 3, 1, NOW(), NOW()),
('wms_warehouse_type', '不良品仓', 'DEFECT', 'red', 4, 1, NOW(), NOW()),
('wms_doc_status', '在途', 'TRANSIT', 'blue', 3, 1, NOW(), NOW()),
('wms_doc_status', '审批中', 'APPROVING', 'processing', 4, 1, NOW(), NOW()),
('wms_out_type', '报废出库', 'SCRAP', 'red', 8, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(274, 901, '补货建议', 'C', 'reorder', 'scm/reorder/index', 'scm:reorder:list', 'AlertOutlined', 6, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 274);

INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(69, '222104', '未交增值税', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(70, '222105', '应交城市维护建设税', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(71, '222106', '应交教育费附加', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(72, '222107', '应交地方教育附加', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(276, 932, '发票管理', 'C', 'invoice', 'fin/invoice/index', 'fin:invoice:list', 'ProfileOutlined', 6, 1, 1, NOW(), NOW()),
(277, 932, '预收预付', 'C', 'advance', 'fin/advance/index', 'fin:advance:list', 'WalletOutlined', 5, 1, 1, NOW(), NOW()),
(281, 935, '经营分析', 'C', 'analysis', 'fin/analysis/index', 'fin:analysis:list', 'BarChartOutlined', 2, 1, 1, NOW(), NOW()),
(2760, 276, '发票登记', 'F', NULL, NULL, 'fin:invoice:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2770, 277, '预收预付登记', 'F', NULL, NULL, 'fin:advance:add', NULL, 1, 1, 1, NOW(), NOW()),
(1834, 183, '坏账核销', 'F', NULL, NULL, 'fin:ar:baddebt', NULL, 9, 1, 1, NOW(), NOW()),
(1805, 180, '红字冲销', 'F', NULL, NULL, 'fin:voucher:reverse', NULL, 9, 1, 1, NOW(), NOW()),
(1912, 191, '增值税结转', 'F', NULL, NULL, 'fin:period:tax', NULL, 9, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 276), (2, 277), (2, 281);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(110, '发票类型', 'fin_invoice_type', 1, '增值税专用发票、普通发票', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_invoice_type', '增值税专用发票', 'SPECIAL', 'red', 1, 1, NOW(), NOW()),
('fin_invoice_type', '普通发票', 'NORMAL', 'blue', 2, 1, NOW(), NOW()),
('fin_invoice_type', '数电票（专票）', 'E_SPECIAL', 'volcano', 3, 1, NOW(), NOW()),
('fin_invoice_type', '数电票（普票）', 'E_NORMAL', 'geekblue', 4, 1, NOW(), NOW()),
('fin_voucher_source', '预收预付', 'ADVANCE', 'cyan', 13, 1, NOW(), NOW()),
('fin_voucher_source', '坏账', 'BAD_DEBT', 'red', 14, 1, NOW(), NOW()),
('fin_voucher_source', '红字冲销', 'REVERSAL', 'red', 15, 1, NOW(), NOW()),
('fin_voucher_source', '税费', 'TAX', 'purple', 16, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1405, 140, '采购订单变更', 'F', NULL, NULL, 'scm:purchase:change', NULL, 6, 1, 1, NOW(), NOW()),
(1436, 143, '销售订单变更', 'F', NULL, NULL, 'scm:sales:change', NULL, 7, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(118, '变更单状态', 'scm_change_status', 1, '订单变更单的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_change_status', '审批中', 'APPROVING', 'processing', 1, 1, NOW(), NOW()),
('scm_change_status', '已生效', 'APPROVED', 'green', 2, 1, NOW(), NOW()),
('scm_change_status', '已驳回', 'REJECTED', 'red', 3, 1, NOW(), NOW());

INSERT INTO fin_currency (code, name, symbol, is_base, status, sort, create_time, update_time) VALUES
('CNY', '人民币', '¥', 1, 1, 1, NOW(), NOW()),
('USD', '美元', '$', 0, 1, 2, NOW(), NOW()),
('EUR', '欧元', '€', 0, 1, 3, NOW(), NOW()),
('HKD', '港币', 'HK$', 0, 1, 4, NOW(), NOW()),
('JPY', '日元', '¥', 0, 1, 5, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(286, 936, '币种汇率', 'C', 'currency', 'fin/currency/index', 'fin:currency:list', 'DollarOutlined', 4, 1, 1, NOW(), NOW()),
(2860, 286, '汇率维护与期末调汇', 'F', NULL, NULL, 'fin:currency:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 286);
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_voucher_source', '汇兑', 'FX', 'geekblue', 19, 1, NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_warehouse_type', '客户寄售仓', 'CONSIGN_OUT', 'purple', 5, 1, NOW(), NOW()),
('wms_warehouse_type', '供应商寄售仓', 'CONSIGN_IN', 'geekblue', 6, 1, NOW(), NOW()),
('wms_in_type', '寄售入库', 'CONSIGN_IN', 'geekblue', 9, 1, NOW(), NOW());

-- 内置角色：按岗位预置菜单和按钮权限，可在「角色管理」中调整
INSERT INTO sys_role (id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(3, '总经理', 'gm', 3, 1, 1, '查看全部业务和商务智能，审核关闭订单，审核招聘、假勤、工时', NOW(), NOW()),
(4, '采购员', 'purchaser', 4, 3, 1, '请购、询价、采购订单与收退货、供应商、寄售结算，可查看库存和物料', NOW(), NOW()),
(5, '销售员', 'sales', 5, 5, 1, '报价、销售订单、发货通知、客户与商机跟进，只能看本人的数据', NOW(), NOW()),
(6, '仓库管理员', 'warehouse', 6, 1, 1, '出入库、调拨、盘点、批次序列号、库存冻结预留，采购收货、销售出库、拣货和发运，生产领料和完工入库', NOW(), NOW()),
(9, '会计', 'accountant', 9, 1, 1, '财务全部功能：凭证、往来、资产、成本、预算、结账，可查看采购销售单据', NOW(), NOW()),
(10, '出纳', 'cashier', 10, 1, 1, '收付款、预收预付、银行对账、报销和借款付款，可查看账簿和往来', NOW(), NOW()),
(13, '普通员工', 'staff', 13, 5, 1, '员工自助（请假加班、工资条）、发起和处理审批、费用报销和借款', NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 3), (3, 4), (3, 5), (3, 7), (3, 11), (3, 120), (3, 121), (3, 122), (3, 123), (3, 124), (3, 130), (3, 131), (3, 132), (3, 133), (3, 134), (3, 135), (3, 136), (3, 137), (3, 138), (3, 139), (3, 140), (3, 141), (3, 142), (3, 143), (3, 144), (3, 145), (3, 147), (3, 149), (3, 180), (3, 181), (3, 182), (3, 183), (3, 184), (3, 185), (3, 186), (3, 190), (3, 191), (3, 192), (3, 260), (3, 261), (3, 262), (3, 263), (3, 264), (3, 265), (3, 270), (3, 271), (3, 272), (3, 273), (3, 274), (3, 276), (3, 277), (3, 281), (3, 286), (3, 1403), (3, 1404), (3, 1433), (3, 1434), (3, 2620), (3, 2621), (3, 2622), (3, 2630), (3, 2631), (3, 2632), (3, 2640), (3, 2650);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(4, 3), (4, 4), (4, 5), (4, 120), (4, 121), (4, 122), (4, 123), (4, 124), (4, 134), (4, 135), (4, 137), (4, 138), (4, 140), (4, 141), (4, 142), (4, 147), (4, 274), (4, 1400), (4, 1401), (4, 1402), (4, 1403), (4, 1404), (4, 1405), (4, 1410), (4, 1420), (4, 1470), (4, 1471), (4, 1472);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(5, 3), (5, 4), (5, 5), (5, 120), (5, 121), (5, 122), (5, 123), (5, 124), (5, 134), (5, 137), (5, 143), (5, 144), (5, 145), (5, 149), (5, 270), (5, 271), (5, 272), (5, 273), (5, 1430), (5, 1431), (5, 1432), (5, 1433), (5, 1434), (5, 1435), (5, 1436), (5, 1490), (5, 1491), (5, 1492), (5, 2710);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(6, 3), (6, 4), (6, 5), (6, 120), (6, 121), (6, 122), (6, 123), (6, 124), (6, 130), (6, 131), (6, 132), (6, 133), (6, 134), (6, 135), (6, 136), (6, 137), (6, 138), (6, 139), (6, 140), (6, 141), (6, 142), (6, 143), (6, 144), (6, 145), (6, 147), (6, 149), (6, 271), (6, 273), (6, 1300), (6, 1301), (6, 1302), (6, 1303), (6, 1304), (6, 1310), (6, 1311), (6, 1312), (6, 1313), (6, 1314), (6, 1320), (6, 1321), (6, 1322), (6, 1323), (6, 1324), (6, 1325), (6, 1330), (6, 1331), (6, 1332), (6, 1333), (6, 1370), (6, 1371), (6, 1372), (6, 1380), (6, 1381), (6, 1382), (6, 1390), (6, 1391), (6, 1392), (6, 1410), (6, 1420), (6, 1440), (6, 1450), (6, 2711), (6, 2730);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(9, 3), (9, 4), (9, 5), (9, 7), (9, 120), (9, 121), (9, 122), (9, 123), (9, 124), (9, 134), (9, 137), (9, 140), (9, 141), (9, 142), (9, 143), (9, 144), (9, 145), (9, 147), (9, 149), (9, 180), (9, 181), (9, 182), (9, 183), (9, 184), (9, 185), (9, 186), (9, 190), (9, 191), (9, 192), (9, 276), (9, 277), (9, 281), (9, 286), (9, 1800), (9, 1801), (9, 1802), (9, 1803), (9, 1804), (9, 1805), (9, 1830), (9, 1831), (9, 1832), (9, 1834), (9, 1840), (9, 1841), (9, 1842), (9, 1850), (9, 1851), (9, 1852), (9, 1860), (9, 1861), (9, 1862), (9, 1900), (9, 1901), (9, 1902), (9, 1910), (9, 1912), (9, 1920), (9, 2760), (9, 2770), (9, 2860);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(10, 3), (10, 7), (10, 120), (10, 121), (10, 122), (10, 123), (10, 124), (10, 181), (10, 183), (10, 184), (10, 185), (10, 186), (10, 276), (10, 277), (10, 286), (10, 1850), (10, 1851), (10, 1860), (10, 1861), (10, 2770);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(13, 3), (13, 961), (13, 120), (13, 121), (13, 122), (13, 123), (13, 124);

-- 菜单分组：模块下的二级目录，不填路由地址，页面地址保持不变
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(901, 5, '采购', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(902, 5, '销售', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(903, 5, '往来单位', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(911, 4, '库存作业', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(912, 4, '库存查询', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(913, 4, '库存控制', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(914, 4, '基础资料', 'M', NULL, NULL, NULL, NULL, 4, 1, 1, NOW(), NOW()),
(931, 7, '总账', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(932, 7, '应收应付', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(933, 7, '资金与费用', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(935, 7, '分析', 'M', NULL, NULL, NULL, NULL, 5, 1, 1, NOW(), NOW()),
(936, 7, '期末与设置', 'M', NULL, NULL, NULL, NULL, 6, 1, 1, NOW(), NOW()),
(961, 3, '我的事务', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(962, 3, '流程管理', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, c.parent_id FROM sys_role_menu rm JOIN sys_menu c ON c.id = rm.menu_id
WHERE c.parent_id IN (901, 902, 903, 911, 912, 913, 914, 931, 932, 933, 935, 936, 961, 962)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = c.parent_id);

-- 通用流程（可在「发起申请」中直接发起，未发布）
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wf_category', '行政', 'ADMIN', 'cyan', 9, 1, NOW(), NOW());
INSERT INTO wf_model (model_key, name, category, icon, sort, status, design_json, remark, create_time, update_time) VALUES
('oa_travel', '出差申请', 'ADMIN', 'CarOutlined', 21, 1, '{"form": [{"field": "destination", "label": "出差地点", "type": "INPUT", "required": true, "placeholder": "如：上海"}, {"field": "startDate", "label": "开始日期", "type": "DATE", "required": true, "placeholder": ""}, {"field": "endDate", "label": "结束日期", "type": "DATE", "required": true, "placeholder": ""}, {"field": "days", "label": "出差天数", "type": "NUMBER", "required": true, "placeholder": ""}, {"field": "budget", "label": "预计费用（元）", "type": "NUMBER", "required": false, "placeholder": ""}, {"field": "reason", "label": "出差事由", "type": "TEXTAREA", "required": true, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "days_check", "type": "CONDITION", "name": "按天数和费用", "next": {"id": "cc_hr", "type": "CC", "name": "抄送人事专员", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}, "next": null}, "branches": [{"id": "days_check_yes", "name": "≥ 5 天或费用 ≥ 1 万元", "defaultBranch": false, "conditions": [[{"field": "days", "operator": "GE", "value": "5"}], [{"field": "budget", "operator": "GE", "value": "10000"}]], "next": {"id": "leader2", "type": "APPROVER", "name": "上级主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 2}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "days_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程：部门主管 → 出差 ≥ 5 天或费用 ≥ 1 万元时上级主管 → 抄送人事。可在「发起申请」中直接发起', NOW(), NOW()),
('oa_seal', '用印申请', 'ADMIN', 'SafetyCertificateOutlined', 22, 1, '{"form": [{"field": "sealType", "label": "印章类型", "type": "SELECT", "required": true, "placeholder": "", "options": ["公章", "合同专用章", "财务专用章", "部门章"]}, {"field": "fileName", "label": "文件名称", "type": "INPUT", "required": true, "placeholder": ""}, {"field": "copies", "label": "用印份数", "type": "NUMBER", "required": true, "placeholder": ""}, {"field": "purpose", "label": "用途说明", "type": "TEXTAREA", "required": true, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "seal_check", "type": "CONDITION", "name": "按印章类型", "next": {"id": "cc_hr", "type": "CC", "name": "抄送人事专员", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}, "next": null}, "branches": [{"id": "seal_check_yes", "name": "公章、合同专用章", "defaultBranch": false, "conditions": [[{"field": "sealType", "operator": "IN", "value": "公章,合同专用章"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "seal_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程：部门主管 → 公章、合同章时总经理 → 抄送人事行政。可在「发起申请」中直接发起', NOW(), NOW()),
('oa_goods', '物品领用', 'ADMIN', 'ShoppingOutlined', 23, 1, '{"form": [{"field": "goods", "label": "物品名称", "type": "INPUT", "required": true, "placeholder": "如：笔记本电脑、显示器"}, {"field": "qty", "label": "数量", "type": "NUMBER", "required": true, "placeholder": ""}, {"field": "useDate", "label": "领用日期", "type": "DATE", "required": false, "placeholder": ""}, {"field": "purpose", "label": "用途", "type": "TEXTAREA", "required": false, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "cc_wh", "type": "CC", "name": "抄送仓库管理员", "assignee": {"type": "ROLE", "ids": [6], "names": ["仓库管理员"]}, "next": null}}}}', '内置流程：部门主管 → 抄送仓库管理员。可在「发起申请」中直接发起', NOW(), NOW()),
('oa_general', '通用审批', 'OTHER', 'FileTextOutlined', 24, 1, '{"form": [{"field": "title", "label": "审批事项", "type": "INPUT", "required": true, "placeholder": ""}, {"field": "content", "label": "详细说明", "type": "TEXTAREA", "required": true, "placeholder": ""}, {"field": "amount", "label": "涉及金额（元）", "type": "NUMBER", "required": false, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "leader2", "type": "APPROVER", "name": "上级主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 2}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}}}', '内置流程：部门主管 → 上级主管（依次审批）。可在「发起申请」中直接发起', NOW(), NOW());

-- ---------------------------------------------------------------- 资金管理：账户调拨（应收票据、应付票据科目供手工凭证使用）
INSERT INTO fin_account (code, name, parent_id, category, direction, status, create_time, update_time) VALUES
('1121', '应收票据', 0, 'ASSET', 'DEBIT', 1, NOW(), NOW()),
('2201', '应付票据', 0, 'LIABILITY', 'CREDIT', 1, NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_voucher_source', '账户调拨', 'TRANSFER', 'cyan', 21, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(194, 933, '账户调拨', 'C', 'transfer', 'fin/transfer/index', 'fin:transfer:list', 'SwapOutlined', 5, 1, 1, NOW(), NOW()),
(1940, 194, '调拨登记', 'F', NULL, NULL, 'fin:transfer:edit', NULL, 1, 1, 1, NOW(), NOW());

-- 普通用户只读；会计、出纳可操作。「资金与费用」分组（933）在上面按已授权页面补授权时还没有页面（账户调拨、出纳管理、其他收支都在后面登记），
-- 这里给能看到这些页面的角色（普通用户、总经理、会计、出纳）授权分组
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 194), (9, 194), (9, 1940), (10, 194), (10, 1940),
(2, 933), (3, 933), (9, 933), (10, 933);

-- 周转材料科目（低值易耗品分类的存货科目）
INSERT INTO fin_account (code, name, parent_id, category, direction, status, create_time, update_time) VALUES
('1411', '周转材料', 0, 'ASSET', 'DEBIT', 1, NOW(), NOW());

-- ---------------------------------------------------------------- 促销
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(149, '促销类型', 'scm_promo_type', 1, '促销活动的类型', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_promo_type', '折扣', 'DISCOUNT', 'blue', 1, 1, NOW(), NOW()),
('scm_promo_type', '满减', 'REDUCE', 'orange', 2, 1, NOW(), NOW()),
('scm_promo_type', '买赠', 'GIFT', 'magenta', 3, 1, NOW(), NOW());

-- 销售：价目表之后为促销活动
UPDATE sys_menu SET sort = sort + 1 WHERE parent_id = 902 AND sort >= 3;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(312, 902, '促销活动', 'C', 'promotion', 'scm/promotion/index', 'scm:promotion:list', 'GiftOutlined', 3, 1, 1, NOW(), NOW()),
(3120, 312, '促销维护', 'F', NULL, NULL, 'scm:promotion:edit', NULL, 1, 1, 1, NOW(), NOW());

-- 普通用户、总经理查看；销售员维护促销
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 312), (3, 312), (5, 312), (5, 3120);

-- ---------------------------------------------------------------- 客户的销售渠道、直接开单的订单来源
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(152, '销售渠道', 'scm_channel', 1, '客户的销售渠道', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_channel', '直销', 'DIRECT', 'default', 1, 1, NOW(), NOW()),
('scm_channel', '经销商', 'DEALER', 'purple', 2, 1, NOW(), NOW()),
('scm_order_source', '直接开单', 'DIRECT', 'blue', 10, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 仓储作业：库位类型与区域、上架策略
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(153, '库位类型', 'wms_location_type', 1, '存储位、拣货位、收发货暂存位、不良品位', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_location_type', '存储位', 'STORAGE', 'blue', 1, 1, NOW(), NOW()),
('wms_location_type', '拣货位', 'PICK', 'green', 2, 1, NOW(), NOW()),
('wms_location_type', '收货暂存', 'RECEIVE', 'cyan', 3, 1, NOW(), NOW()),
('wms_location_type', '发货暂存', 'SHIP', 'purple', 4, 1, NOW(), NOW()),
('wms_location_type', '不良品位', 'DEFECT', 'red', 5, 1, NOW(), NOW());

-- 装箱（发货通知的按钮）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(2713, 271, '装箱', 'F', NULL, NULL, 'scm:delivery:pack', NULL, 3, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(6, 2713);

-- 期初入库：系统启用时导入的期初库存
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_in_type', '期初入库', 'OPENING', 'gold', 10, 1, NOW(), NOW());

-- 规格属性（多规格物料）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(342, 914, '规格属性', 'C', 'attribute', 'wms/attribute/index', 'wms:attribute:list', 'TagsOutlined', 4, 1, 1, NOW(), NOW()),
(3420, 342, '规格属性维护', 'F', NULL, NULL, 'wms:attribute:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 342), (3, 342), (4, 342), (6, 342), (6, 3420);
INSERT INTO wms_attribute (id, name, sort, status, create_time, update_time) VALUES
(1, '颜色', 1, 1, NOW(), NOW()),
(2, '尺码', 2, 1, NOW(), NOW());
INSERT INTO wms_attribute_value (attribute_id, attr_value, code, sort, create_time, update_time) VALUES
(1, '黑色', 'BK', 1, NOW(), NOW()),
(1, '白色', 'WH', 2, NOW(), NOW()),
(1, '红色', 'RD', 3, NOW(), NOW()),
(1, '蓝色', 'BL', 4, NOW(), NOW()),
(2, 'S', 'S', 1, NOW(), NOW()),
(2, 'M', 'M', 2, NOW(), NOW()),
(2, 'L', 'L', 3, NOW(), NOW()),
(2, 'XL', 'XL', 4, NOW(), NOW()),
(2, 'XXL', 'XXL', 5, NOW(), NOW());

-- 组装拆卸单
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(343, 911, '组装拆卸', 'C', 'assembly', 'wms/assembly/index', 'wms:assembly:list', 'BuildOutlined', 5, 1, 1, NOW(), NOW()),
(3430, 343, '组装拆卸维护', 'F', NULL, NULL, 'wms:assembly:add', NULL, 1, 1, 1, NOW(), NOW()),
(3431, 343, '组装拆卸确认', 'F', NULL, NULL, 'wms:assembly:confirm', NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 343), (3, 343), (6, 343), (6, 3430), (6, 3431), (9, 343);
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(156, '组装拆卸种类', 'wms_assembly_kind', 1, '组装、拆卸', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_assembly_kind', '组装', 'ASSEMBLE', 'blue', 1, 1, NOW(), NOW()),
('wms_assembly_kind', '拆卸', 'DISASSEMBLE', 'orange', 2, 1, NOW(), NOW()),
('wms_in_type', '组装拆卸', 'ASSEMBLY', 'cyan', 11, 1, NOW(), NOW()),
('wms_out_type', '组装拆卸', 'ASSEMBLY', 'cyan', 11, 1, NOW(), NOW());

-- 销售提成：规则与按月计算（销售 → 销售提成）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(344, 902, '销售提成', 'C', 'commission', 'fin/commission/index', 'fin:commission:list', 'PercentageOutlined', 20, 1, 1, NOW(), NOW()),
(3440, 344, '提成规则维护', 'F', NULL, NULL, 'fin:commission:rule', NULL, 1, 1, 1, NOW(), NOW()),
(3441, 344, '提成计算与确认', 'F', NULL, NULL, 'fin:commission:edit', NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 344), (3, 344), (9, 344), (9, 3440), (9, 3441);
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(157, '提成依据', 'fin_commission_basis', 1, '销售提成的计算依据', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_commission_basis', '销售额', 'SALES', 'blue', 1, 1, NOW(), NOW()),
('fin_commission_basis', '毛利', 'PROFIT', 'green', 2, 1, NOW(), NOW()),
('fin_commission_basis', '回款', 'RECEIPT', 'gold', 3, 1, NOW(), NOW());

-- 价格控制方式
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(158, '价格控制方式', 'scm_price_control_mode', 1, '超出最低销售价 / 最高采购价时的处理', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_price_control_mode', '提示', 'WARN', 'orange', 1, 1, NOW(), NOW()),
('scm_price_control_mode', '禁止保存', 'BLOCK', 'red', 2, 1, NOW(), NOW());

-- 自定义字段
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(345, 1, '自定义字段', 'C', 'custom-field', 'system/custom-field/index', 'sys:customField:list', 'FormOutlined', 11, 1, 1, NOW(), NOW()),
(3450, 345, '自定义字段维护', 'F', NULL, NULL, 'sys:customField:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 345);
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(159, '自定义字段类型', 'sys_custom_field_type', 1, '自定义字段的输入类型', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_custom_field_type', '文本', 'TEXT', 'default', 1, 1, NOW(), NOW()),
('sys_custom_field_type', '多行文本', 'TEXTAREA', 'default', 2, 1, NOW(), NOW()),
('sys_custom_field_type', '数字', 'NUMBER', 'blue', 3, 1, NOW(), NOW()),
('sys_custom_field_type', '日期', 'DATE', 'cyan', 4, 1, NOW(), NOW()),
('sys_custom_field_type', '下拉选择', 'SELECT', 'purple', 5, 1, NOW(), NOW()),
('sys_custom_field_type', '是否', 'BOOL', 'green', 6, 1, NOW(), NOW());

-- 业务参数
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(346, 1, '业务参数', 'C', 'biz-rule', 'system/biz-rule/index', 'sys:bizRule:list', 'SlidersOutlined', 12, 1, 1, NOW(), NOW()),
(3460, 346, '业务参数修改', 'F', NULL, NULL, 'sys:bizRule:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 346);

-- 第三方集成：钉钉、企业微信、飞书（扫码登录、消息推送、通讯录同步）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(350, 1, '第三方集成', 'C', 'im', 'system/im/index', 'sys:im:list', 'ApiOutlined', 13, 1, 1, NOW(), NOW()),
(3500, 350, '集成配置修改', 'F', NULL, NULL, 'sys:im:edit', NULL, 1, 1, 1, NOW(), NOW()),
(3501, 350, '通讯录同步与绑定', 'F', NULL, NULL, 'sys:im:sync', NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(160, '集成平台', 'sys_im_platform', 1, '第三方集成的平台', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_im_platform', '钉钉', 'DINGTALK', 'blue', 1, 1, NOW(), NOW()),
('sys_im_platform', '企业微信', 'WECOM', 'green', 2, 1, NOW(), NOW()),
('sys_im_platform', '飞书', 'FEISHU', 'cyan', 3, 1, NOW(), NOW());

-- 业务字典：各租户维护自己的业务字典取值（系统管理员可见）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(352, 1, '业务字典', 'C', 'biz-dict', 'system/bizDict/index', 'sys:bizdict:list', 'TagsOutlined', 5, 1, 1, NOW(), NOW()),
(3520, 352, '业务字典维护', 'F', NULL, NULL, 'sys:bizdict:edit', NULL, 1, 1, 1, NOW(), NOW());

-- 收发类别：手工其他入库 / 其他出库的用途分类（业务字典，月末成本凭证可按类别设置对方科目）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(161, '入库类别', 'wms_in_category', 'BUSINESS', 1, '其他入库的用途分类，可在凭证科目设置中按类别指定对方科目', NOW(), NOW()),
(162, '出库类别', 'wms_out_category', 'BUSINESS', 1, '其他出库、报废出库的用途分类，可在凭证科目设置中按类别指定对方科目', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_in_category', '样品退回', 'SAMPLE_RETURN', 'blue', 1, 1, NOW(), NOW()),
('wms_in_category', '受赠入库', 'GIFT', 'green', 2, 1, NOW(), NOW()),
('wms_in_category', '领用退回', 'USE_RETURN', 'cyan', 3, 1, NOW(), NOW()),
('wms_in_category', '其他', 'OTHER', 'default', 9, 1, NOW(), NOW()),
('wms_out_category', '样品出库', 'SAMPLE', 'blue', 1, 1, NOW(), NOW()),
('wms_out_category', '赠品出库', 'GIFT', 'green', 2, 1, NOW(), NOW()),
('wms_out_category', '福利领用', 'WELFARE', 'orange', 3, 1, NOW(), NOW()),
('wms_out_category', '研发领用', 'RD', 'purple', 4, 1, NOW(), NOW()),
('wms_out_category', '办公领用', 'OFFICE', 'cyan', 5, 1, NOW(), NOW()),
('wms_out_category', '其他', 'OTHER', 'default', 9, 1, NOW(), NOW());

-- 凭证科目设置：自动凭证各用途使用的科目
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(340, 936, '凭证科目设置', 'C', 'account-rule', 'fin/account-rule/index', 'fin:accountRule:list', 'NodeIndexOutlined', 5, 1, 1, NOW(), NOW()),
(3400, 340, '科目设置修改', 'F', NULL, NULL, 'fin:accountRule:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 340), (9, 340), (9, 3400);

-- 出纳管理：现金 / 银行日记账、资金日报
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(341, 933, '出纳管理', 'C', 'cashier', 'fin/cashier/index', 'fin:cashier:list', 'WalletOutlined', 0, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 341), (3, 341), (9, 341), (10, 341);

-- 业务报表：收发存汇总（库存查询）、往来对账单（应收应付）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(347, 912, '收发存汇总', 'C', 'in-out', 'wms/inout/index', 'wms:inout:list', 'SwapOutlined', 5, 1, 1, NOW(), NOW()),
(348, 932, '往来对账单', 'C', 'partner-statement', 'fin/partner-statement/index', 'fin:partnerStatement:list', 'AuditOutlined', 8, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 347), (3, 347), (4, 347), (6, 347), (9, 347),
(2, 348), (3, 348), (9, 348), (10, 348);

-- 数据大屏：仓储物流、销售经营，投屏链接
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(349, 11, '数据大屏', 'C', 'screen', 'bi/screen/index', 'bi:screen:list', 'FundProjectionScreenOutlined', 0, 1, 1, NOW(), NOW()),
(3490, 349, '投屏链接管理', 'F', NULL, NULL, 'bi:screen:share', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 349), (3, 3490), (6, 349);

-- 智能（AI）：AI 问数、AI 设置（国产大模型，按租户配置；第二期的智能录入、数据整理、智能报告页面放在本模块下）
-- 顶部导航排在商务智能之后
UPDATE sys_menu SET sort = sort + 1 WHERE parent_id = 0 AND sort >= 9;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(16, 0, '智能', 'M', '/ai', NULL, NULL, 'RobotOutlined', 9, 1, 1, NOW(), NOW()),
(360, 16, 'AI 问数', 'C', 'chat', 'ai/chat/index', 'ai:chat:use', 'MessageOutlined', 1, 1, 1, NOW(), NOW()),
(361, 16, 'AI 设置', 'C', 'settings', 'ai/settings/index', 'ai:config:list', 'SettingOutlined', 99, 1, 1, NOW(), NOW()),
(3610, 361, 'AI 设置修改', 'F', NULL, NULL, 'ai:config:edit', NULL, 1, 1, 1, NOW(), NOW());
-- AI 问数给总经理（工具按用户自己的权限和数据权限查询）；AI 设置只给超级管理员，租户管理员按套餐获得
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 16), (3, 360);
-- 智能录入：上传单据图片 / PDF / 表格识别成单据草稿，在业务页面核对后保存（还需要目标单据的新增权限）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(362, 16, '智能录入', 'C', 'extract', 'ai/extract/index', 'ai:extract:use', 'ScanOutlined', 2, 1, 1, NOW(), NOW());
-- 给总经理、采购员、销售员、会计（各自只能生成有新增权限的单据）；其他 AI 页面也会给这些角色加「智能」模块，用 NOT EXISTS 避免重复
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 16 AS menu_id UNION ALL SELECT 362) m ON 1 = 1
WHERE r.id IN (3, 4, 5, 9) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);
-- 数据整理：物料、客户、供应商的查重与规范化建议，采纳后调用业务模块的修改 / 停用（还需要对应资料的查看、修改权限）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(363, 16, '数据整理', 'C', 'cleanse', 'ai/cleanse/index', 'ai:cleanse:list', 'ClearOutlined', 3, 1, 1, NOW(), NOW()),
(3630, 363, '采纳整理建议', 'F', NULL, NULL, 'ai:cleanse:apply', NULL, 1, 1, 1, NOW(), NOW());
-- 给采购员（供应商）、仓库管理员（物料）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 16 AS menu_id UNION ALL SELECT 363 UNION ALL SELECT 3630) m ON 1 = 1
WHERE r.id IN (4, 6) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);
-- 智能报告：按周 / 月汇总有权限的业务数据，由 AI 撰写经营报告（手动生成或定时任务自动生成）；BI 预警的 AI 分析
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(364, 16, '智能报告', 'C', 'report', 'ai/report/index', 'ai:report:list', 'FileTextOutlined', 4, 1, 1, NOW(), NOW()),
(3640, 364, '生成报告', 'F', NULL, NULL, 'ai:report:generate', NULL, 1, 1, 1, NOW(), NOW());
-- 给总经理（数据权限为全部数据，能看自动生成的全公司报告）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 16 AS menu_id UNION ALL SELECT 364 UNION ALL SELECT 3640) m ON 1 = 1
WHERE r.id IN (3) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- 价格体系：品牌、价格列（零售价、批发价1-8）、客户价格等级与执行价、价格记忆与价格参考
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(175, '商品品牌', 'wms_material_brand', 'BUSINESS', 1, '物料的品牌，进销存报表可按品牌汇总', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_material_brand', '自有品牌', 'OWN', 'blue', 1, 1, NOW(), NOW()),
('wms_material_brand', '其他', 'OTHER', 'default', 9, 1, NOW(), NOW());
INSERT INTO wms_price_level (level, name, status, create_time, update_time) VALUES
(0, '零售价', 1, NOW(), NOW()), (1, '批发价1', 1, NOW(), NOW()), (2, '批发价2', 1, NOW(), NOW()), (3, '批发价3', 1, NOW(), NOW()),
(4, '批发价4', 0, NOW(), NOW()), (5, '批发价5', 0, NOW(), NOW()), (6, '批发价6', 0, NOW(), NOW()), (7, '批发价7', 0, NOW(), NOW()),
(8, '批发价8', 0, NOW(), NOW());
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(365, 902, '商品价格', 'C', 'goods-price', 'scm/goodsPrice/index', 'scm:goodsPrice:list', 'DollarOutlined', 3, 1, 1, NOW(), NOW()),
(3650, 365, '商品价格维护', 'F', NULL, NULL, 'scm:goodsPrice:edit', NULL, 1, 1, 1, NOW(), NOW()),
(3651, 137, '价格列设置', 'F', NULL, NULL, 'wms:material:priceLevel', NULL, 5, 1, 1, NOW(), NOW());
-- 普通用户、总经理、销售员只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 5 AS menu_id UNION ALL SELECT 902 UNION ALL SELECT 365) m ON 1 = 1
WHERE r.id IN (2, 3, 5) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- 进销存报表（供应链下的分组，代码在 bi 模块）：实时汇总出入库单，汇总 → 明细 → 单据穿透；资产状况汇总全公司数据（需要「全部数据」权限）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(966, 5, '进销存报表', 'M', NULL, NULL, NULL, 'BarChartOutlined', 5, 1, 1, NOW(), NOW()),
(370, 966, '销售报表', 'C', 'jxc-sales', 'jxc/sales/index', 'bi:jxc:sales', 'RiseOutlined', 1, 1, 1, NOW(), NOW()),
(371, 966, '进货报表', 'C', 'jxc-purchase', 'jxc/purchase/index', 'bi:jxc:purchase', 'ShoppingCartOutlined', 2, 1, 1, NOW(), NOW()),
(372, 966, '经营状况', 'C', 'jxc-business', 'jxc/business/index', 'bi:jxc:business', 'LineChartOutlined', 3, 1, 1, NOW(), NOW()),
(373, 966, '资产状况', 'C', 'jxc-assets', 'jxc/assets/index', 'bi:jxc:asset', 'AccountBookOutlined', 4, 1, 1, NOW(), NOW()),
(374, 966, '商品分析', 'C', 'jxc-goods', 'jxc/goods/index', 'bi:jxc:goods', 'AppstoreOutlined', 5, 1, 1, NOW(), NOW()),
(375, 966, '客户分析', 'C', 'jxc-rfm', 'jxc/rfm/index', 'bi:jxc:rfm', 'TeamOutlined', 6, 1, 1, NOW(), NOW()),
(376, 966, '积压分析', 'C', 'jxc-overstock', 'jxc/overstock/index', 'bi:jxc:overstock', 'HourglassOutlined', 7, 1, 1, NOW(), NOW()),
(385, 966, '周转分析', 'C', 'jxc-turnover', 'jxc/turnover/index', 'bi:jxc:turnover', 'SyncOutlined', 8, 1, 1, NOW(), NOW()),
(386, 966, '滞销畅销', 'C', 'jxc-sales-rate', 'jxc/salesRate/index', 'bi:jxc:salesRate', 'FireOutlined', 9, 1, 1, NOW(), NOW()),
(387, 966, '库存预警', 'C', 'jxc-stock-alert', 'jxc/stockAlert/index', 'bi:jxc:stockAlert', 'AlertOutlined', 10, 1, 1, NOW(), NOW());
-- 普通用户（除资产状况）、总经理、会计全部；采购员进货报表、商品分析和库存分析（积压、周转、滞销畅销、库存预警）；销售员销售报表和客户分析；
-- 仓库管理员商品分析和库存分析；出纳资产状况（连同分组和供应链模块）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 2 AS role_id, 370 AS menu_id UNION ALL SELECT 2, 371 UNION ALL SELECT 2, 372 UNION ALL SELECT 2, 374 UNION ALL SELECT 2, 375
    UNION ALL SELECT 3, 370 UNION ALL SELECT 3, 371 UNION ALL SELECT 3, 372 UNION ALL SELECT 3, 373 UNION ALL SELECT 3, 374 UNION ALL SELECT 3, 375
    UNION ALL SELECT 9, 370 UNION ALL SELECT 9, 371 UNION ALL SELECT 9, 372 UNION ALL SELECT 9, 373 UNION ALL SELECT 9, 374 UNION ALL SELECT 9, 375
    UNION ALL SELECT 4, 371 UNION ALL SELECT 4, 374 UNION ALL SELECT 5, 370 UNION ALL SELECT 5, 375 UNION ALL SELECT 6, 374 UNION ALL SELECT 10, 373
    UNION ALL SELECT 2, 376 UNION ALL SELECT 3, 376 UNION ALL SELECT 9, 376 UNION ALL SELECT 4, 376 UNION ALL SELECT 6, 376
    UNION ALL SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (385, 386, 387) WHERE r.id IN (2, 3, 4, 6, 9)
    UNION ALL SELECT r.id, 966 FROM sys_role r WHERE r.id IN (2, 3, 4, 5, 6, 9, 10)
    UNION ALL SELECT r.id, 5 FROM sys_role r WHERE r.id IN (2, 3, 4, 5, 6, 9, 10)
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 实时库存成本：库存查询的「查看成本金额」按钮（均价、库存金额、流水的单价金额），授权总经理、仓库管理员、会计
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(3652, 134, '查看成本金额', 'F', NULL, NULL, 'wms:stock:cost', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 3652 FROM sys_role r
WHERE r.id IN (3, 6, 9) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 3652);

-- 应收应付：冲账单（垫付运费、返利、赔偿等抵扣往来款，复核后生成红字应收应付单）、应付坏账（无法支付的应付款转销）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(185, '冲账项目', 'fin_offset_item', 'BUSINESS', 1, '冲账单的项目，如垫付运费、返利、赔偿', NOW(), NOW()),
(186, '冲账单状态', 'fin_offset_status', 'SYSTEM', 1, '冲账单的复核状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_offset_item', '垫付运费', 'FREIGHT', 'cyan', 1, 1, NOW(), NOW()),
('fin_offset_item', '销售返利', 'REBATE', 'volcano', 2, 1, NOW(), NOW()),
('fin_offset_item', '三包赔偿', 'WARRANTY', 'orange', 3, 1, NOW(), NOW()),
('fin_offset_item', '其他', 'OTHER', 'default', 9, 1, NOW(), NOW()),
('fin_offset_status', '待复核', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_offset_status', '已复核', 'REVIEWED', 'green', 2, 1, NOW(), NOW()),
('fin_bill_source', '冲账单', 'OFFSET', 'magenta', 13, 1, NOW(), NOW()),
('fin_bill_source', '订单尾差调整', 'ORDER_ADJUST', 'geekblue', 14, 1, NOW(), NOW());
UPDATE sys_dict_data SET builtin = 1 WHERE dict_type = 'fin_offset_item' AND dict_value = 'OTHER';

-- 运费承担方（销售出库：本公司承担计销售费用、客户承担为代垫运费计入应收；采购入库：供应商代垫计入应付、本公司承担付承运商，都计入存货成本）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(196, '运费承担', 'scm_freight_bearer', 'SYSTEM', 1, '出入库单的运费由谁承担 / 付给谁', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_freight_bearer', '本公司承担', 'COMPANY', 'default', 1, 1, NOW(), NOW()),
('scm_freight_bearer', '客户承担（代垫）', 'CUSTOMER', 'blue', 2, 1, NOW(), NOW()),
('scm_freight_bearer', '供应商代垫', 'SUPPLIER', 'orange', 3, 1, NOW(), NOW()),
('fin_bill_source', '采购运费', 'PURCHASE_FREIGHT', 'cyan', 15, 1, NOW(), NOW());
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(377, 932, '冲账单', 'C', 'offset', 'fin/offset/index', 'fin:offset:list', 'InteractionOutlined', 9, 1, 1, NOW(), NOW()),
(3770, 377, '冲账单新增', 'F', NULL, NULL, 'fin:offset:add', NULL, 1, 1, 1, NOW(), NOW()),
(3771, 377, '冲账单修改', 'F', NULL, NULL, 'fin:offset:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3772, 377, '冲账单删除', 'F', NULL, NULL, 'fin:offset:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3773, 377, '冲账单复核', 'F', NULL, NULL, 'fin:offset:review', NULL, 4, 1, 1, NOW(), NOW()),
(1843, 184, '应付坏账', 'F', NULL, NULL, 'fin:ap:baddebt', NULL, 9, 1, 1, NOW(), NOW());
-- 普通用户、总经理、出纳只读；会计全部
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 2 AS role_id, 377 AS menu_id UNION ALL SELECT 3, 377 UNION ALL SELECT 10, 377
    UNION ALL SELECT 9, 377 UNION ALL SELECT 9, 3770 UNION ALL SELECT 9, 3771 UNION ALL SELECT 9, 3772 UNION ALL SELECT 9, 3773
    UNION ALL SELECT 9, 1843
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 客户区域（业务字典，每个租户一份）：客户、CRM 客户和线索的地区下拉选择，进销存报表按区域汇总
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(187, '客户区域', 'scm_region', 'BUSINESS', 1, '客户的销售区域（经销商为授权区域），CRM 客户、线索的地区也用此字典，进销存报表按区域汇总', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_region', '华东', 'EAST', 'blue', 1, 1, NOW(), NOW()),
('scm_region', '华南', 'SOUTH', 'green', 2, 1, NOW(), NOW()),
('scm_region', '华北', 'NORTH', 'orange', 3, 1, NOW(), NOW()),
('scm_region', '华中', 'CENTRAL', 'cyan', 4, 1, NOW(), NOW()),
('scm_region', '西南', 'SOUTHWEST', 'purple', 5, 1, NOW(), NOW()),
('scm_region', '西北', 'NORTHWEST', 'gold', 6, 1, NOW(), NOW()),
('scm_region', '东北', 'NORTHEAST', 'geekblue', 7, 1, NOW(), NOW()),
('scm_region', '海外', 'OVERSEAS', 'magenta', 8, 1, NOW(), NOW());

-- 内置角色补充：销售经理（销售员的全部功能 + 本部门及以下的数据、价目表和商品价格维护）
INSERT INTO sys_role (id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(17, '销售经理', 'sales_mgr', 17, 4, 1, '销售员的全部功能，可查看本部门及以下的客户、订单和业绩，维护价目表和商品价格', NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 17, menu_id FROM sys_role_menu WHERE role_id = 5;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 17, m.id FROM sys_menu m
WHERE m.id IN (2700, 3650) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = 17 AND x.menu_id = m.id);

-- 商品速查（issue #97，Web 与小程序共用）：按名称、规格、简拼搜索商品，看可用库存和个人设置中选择的一种价格，联查库存分布、售价 / 进价历史。
-- 价格按已有权限过滤（进货类看采购订单 / 采购入库查看权限，库存均价看「查看成本金额」，售价类看销售订单 / 销售出库查看权限），不另设按钮。
-- 授权普通用户、总经理、采购员、销售员、仓库管理员、销售经理（连同销售分组和供应链模块）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(368, 902, '商品速查', 'C', 'quick-lookup', 'scm/quickLookup/index', 'scm:quickLookup:list', 'SearchOutlined', 10, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 5 AS menu_id UNION ALL SELECT 902 UNION ALL SELECT 368) m ON 1 = 1
WHERE r.id IN (2, 3, 4, 5, 6, 17) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- 通知公告：系统管理下维护、发布（超级管理员、租户管理员；普通用户只读），员工在工作台和「全部公告」中查看发给自己的公告
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(353, 1, '通知公告', 'C', 'notice', 'system/notice/index', 'sys:notice:list', 'NotificationOutlined', 14, 1, 1, NOW(), NOW()),
(3530, 353, '公告新增', 'F', NULL, NULL, 'sys:notice:add', NULL, 1, 1, 1, NOW(), NOW()),
(3531, 353, '公告修改', 'F', NULL, NULL, 'sys:notice:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3532, 353, '公告删除', 'F', NULL, NULL, 'sys:notice:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3533, 353, '公告发布与撤回', 'F', NULL, NULL, 'sys:notice:publish', NULL, 4, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 2 AS role_id, 1 AS menu_id UNION ALL SELECT 2, 353
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(188, '公告状态', 'sys_notice_status', 'SYSTEM', 1, '通知公告的状态：草稿、已发布、已撤回', NOW(), NOW()),
(189, '公告发布范围', 'sys_notice_scope', 'SYSTEM', 1, '通知公告发给全员、指定部门（含下级部门）或指定角色', NOW(), NOW()),
(190, '公告类型', 'sys_notice_type', 'BUSINESS', 1, '通知公告的分类', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_notice_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('sys_notice_status', '已发布', 'PUBLISHED', 'green', 2, 1, NOW(), NOW()),
('sys_notice_status', '已撤回', 'WITHDRAWN', 'orange', 3, 1, NOW(), NOW()),
('sys_notice_scope', '全员', 'ALL', 'blue', 1, 1, NOW(), NOW()),
('sys_notice_scope', '指定部门', 'DEPT', 'cyan', 2, 1, NOW(), NOW()),
('sys_notice_scope', '指定角色', 'ROLE', 'purple', 3, 1, NOW(), NOW()),
('sys_notice_type', '通知', 'NOTICE', 'blue', 1, 1, NOW(), NOW()),
('sys_notice_type', '公告', 'ANNOUNCE', 'green', 2, 1, NOW(), NOW()),
('sys_notice_type', '制度', 'POLICY', 'purple', 3, 1, NOW(), NOW()),
('sys_notice_type', '活动', 'EVENT', 'orange', 4, 1, NOW(), NOW());

-- 现金银行：其他收入单、费用支出单（明细为收支类别，确认后生成凭证，进入出纳日记账和资金日报），收支类别（每个租户一份，标明收 / 支并关联科目）；
-- 账户调拨的手续费承担方（转出 / 转入账户）
INSERT INTO fin_account (code, name, parent_id, category, direction, status, create_time, update_time) VALUES
('660206', '业务招待费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW()),
('660207', '水电费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW()),
('660208', '租赁费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW()),
('660209', '快递费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW());
INSERT INTO fin_io_category (name, direction, account_id, sort, status, create_time, update_time)
SELECT g.name, g.direction, a.id, g.sort, 1, NOW(), NOW()
FROM (
    SELECT '废料销售' AS name, 'IN' AS direction, '6051' AS code, 1 AS sort
    UNION ALL SELECT '佣金收入', 'IN', '6051', 2
    UNION ALL SELECT '房租收入', 'IN', '6051', 3
    UNION ALL SELECT '其他收入', 'IN', '6301', 9
    UNION ALL SELECT '差旅费', 'OUT', '660204', 1
    UNION ALL SELECT '业务招待费', 'OUT', '660206', 2
    UNION ALL SELECT '办公费', 'OUT', '660202', 3
    UNION ALL SELECT '房租', 'OUT', '660208', 4
    UNION ALL SELECT '水电费', 'OUT', '660207', 5
    UNION ALL SELECT '快递费', 'OUT', '660209', 6
    UNION ALL SELECT '工资', 'OUT', '660201', 7
    UNION ALL SELECT '银行手续费', 'OUT', '6603', 8
    UNION ALL SELECT '其他支出', 'OUT', '6711', 9
) g
LEFT JOIN fin_account a ON a.code = g.code AND a.tenant_id = 1;
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(201, '收支方向', 'fin_fund_direction', 'SYSTEM', 1, '收支类别、其他收支单的方向：收入、支出', NOW(), NOW()),
(202, '收支单状态', 'fin_io_status', 'SYSTEM', 1, '其他收入单、费用支出单的状态', NOW(), NOW()),
(203, '关联单位类型', 'fin_io_partner_type', 'SYSTEM', 1, '其他收支单的关联单位：客户、供应商或其他', NOW(), NOW()),
(204, '手续费支付账户', 'fin_fee_bearer', 'SYSTEM', 1, '账户调拨的手续费由转出账户还是转入账户支付', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_fund_direction', '收入', 'IN', 'green', 1, 1, NOW(), NOW()),
('fin_fund_direction', '支出', 'OUT', 'orange', 2, 1, NOW(), NOW()),
('fin_io_status', '待确认', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_io_status', '已确认', 'CONFIRMED', 'green', 2, 1, NOW(), NOW()),
('fin_io_partner_type', '客户', 'CUSTOMER', 'blue', 1, 1, NOW(), NOW()),
('fin_io_partner_type', '供应商', 'SUPPLIER', 'purple', 2, 1, NOW(), NOW()),
('fin_io_partner_type', '其他', 'OTHER', 'default', 3, 1, NOW(), NOW()),
('fin_fee_bearer', '转出账户支付', 'FROM', 'default', 1, 1, NOW(), NOW()),
('fin_fee_bearer', '转入账户支付', 'TO', 'default', 2, 1, NOW(), NOW()),
('fin_voucher_source', '其他收支', 'INCOME_EXPENSE', 'lime', 22, 1, NOW(), NOW());
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(391, 933, '其他收支', 'C', 'income-expense', 'fin/income-expense/index', 'fin:io:list', 'DollarCircleOutlined', 7, 1, 1, NOW(), NOW()),
(3910, 391, '收支单新增', 'F', NULL, NULL, 'fin:io:add', NULL, 1, 1, 1, NOW(), NOW()),
(3911, 391, '收支单修改', 'F', NULL, NULL, 'fin:io:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3912, 391, '收支单删除', 'F', NULL, NULL, 'fin:io:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3913, 391, '收支单确认', 'F', NULL, NULL, 'fin:io:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(3914, 391, '收支类别维护', 'F', NULL, NULL, 'fin:io:category', NULL, 5, 1, 1, NOW(), NOW());
-- 普通用户、总经理只读；会计、出纳全部
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 2 AS role_id, 391 AS menu_id UNION ALL SELECT 3, 391
    UNION ALL SELECT 9, 391 UNION ALL SELECT 9, 3910 UNION ALL SELECT 9, 3911 UNION ALL SELECT 9, 3912 UNION ALL SELECT 9, 3913 UNION ALL SELECT 9, 3914
    UNION ALL SELECT 10, 391 UNION ALL SELECT 10, 3910 UNION ALL SELECT 10, 3911 UNION ALL SELECT 10, 3912 UNION ALL SELECT 10, 3913 UNION ALL SELECT 10, 3914
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

ALTER TABLE sys_dept AUTO_INCREMENT = 200;
ALTER TABLE sys_user AUTO_INCREMENT = 100;
ALTER TABLE sys_role AUTO_INCREMENT = 100;
ALTER TABLE sys_menu AUTO_INCREMENT = 10000;
ALTER TABLE sys_dict_type AUTO_INCREMENT = 300;
ALTER TABLE wms_warehouse AUTO_INCREMENT = 100;
ALTER TABLE wms_attribute AUTO_INCREMENT = 100;
ALTER TABLE scm_supplier AUTO_INCREMENT = 100;
ALTER TABLE scm_customer AUTO_INCREMENT = 100;
ALTER TABLE fin_account AUTO_INCREMENT = 1000;
ALTER TABLE bi_alert_rule AUTO_INCREMENT = 100;

-- 应收应付（issue #50）：收付款单确认按钮（业务参数「收付款先存草稿」开启时，草稿确认后才生效），有新增权限的内置角色（会计、出纳）同时授权
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1853, 185, '收款单确认', 'F', NULL, NULL, 'fin:receipt:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1863, 186, '付款单确认', 'F', NULL, NULL, 'fin:payment:confirm', NULL, 4, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 9 AS role_id, 1853 AS menu_id UNION ALL SELECT 9, 1863 UNION ALL SELECT 10, 1853 UNION ALL SELECT 10, 1863
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 进销存单据草稿（issue #68）：采购入库 / 退货、销售出库 / 退货的审核按钮（业务参数「进销存单据先存草稿」开启时，草稿审核后才出入库、生成应收应付），
-- 有新增权限的内置角色（采购员、仓库管理员）和会计同时授权
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1411, 141, '采购入库审核', 'F', NULL, NULL, 'scm:purchaseIn:post', NULL, 2, 1, 1, NOW(), NOW()),
(1421, 142, '采购退货审核', 'F', NULL, NULL, 'scm:purchaseReturn:post', NULL, 2, 1, 1, NOW(), NOW()),
(1441, 144, '销售出库审核', 'F', NULL, NULL, 'scm:salesOut:post', NULL, 2, 1, 1, NOW(), NOW()),
(1451, 145, '销售退货审核', 'F', NULL, NULL, 'scm:salesReturn:post', NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 4 AS role_id, 1411 AS menu_id UNION ALL SELECT 4, 1421
    UNION ALL SELECT 6, 1411 UNION ALL SELECT 6, 1421 UNION ALL SELECT 6, 1441 UNION ALL SELECT 6, 1451
    UNION ALL SELECT 9, 1411 UNION ALL SELECT 9, 1421 UNION ALL SELECT 9, 1441 UNION ALL SELECT 9, 1451
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 销售出库、销售退货、采购退货的运费（issue #72、#73）：付给承运商的方式（现付生成费用支出单 / 月结生成承运商应付），
-- 应收应付来源增加三类运费；收支类别增加内置的「销售运费」「代垫运费」（现付运费自动生成费用支出单时使用，科目与凭证科目设置的默认值一致）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(207, '运费结算方式', 'scm_freight_settle', 'SYSTEM', 1, '销售出库、采购退货的运费付给承运商的方式：现付（生成费用支出单）、月结（生成承运商应付单）', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_freight_settle', '现付', 'CASH', 'green', 1, 1, NOW(), NOW()),
('scm_freight_settle', '月结', 'MONTHLY', 'blue', 2, 1, NOW(), NOW()),
('fin_bill_source', '销售运费', 'SALES_FREIGHT', 'cyan', 16, 1, NOW(), NOW()),
('fin_bill_source', '销售退货运费', 'SALES_RETURN_FREIGHT', 'cyan', 17, 1, NOW(), NOW()),
('fin_bill_source', '采购退货运费', 'PURCHASE_RETURN_FREIGHT', 'cyan', 18, 1, NOW(), NOW()),
('fin_bill_source', '调拨费用', 'TRANSFER_FEE', 'cyan', 19, 1, NOW(), NOW());
INSERT INTO fin_io_category (name, code, direction, account_id, sort, status, remark, create_time, update_time)
SELECT g.name, g.code, 'OUT', a.id, g.sort, 1, g.remark, NOW(), NOW()
FROM (
    SELECT '销售运费' AS name, 'SALES_FREIGHT' AS code, '6601' AS acct, 10 AS sort, '销售出库本公司承担、现付的运费' AS remark
    UNION ALL SELECT '代垫运费', 'FREIGHT_ADVANCE', '1221', 11, '销售出库、采购退货替往来单位垫付、现付的运费'
    UNION ALL SELECT '调拨费用', 'TRANSFER_FEE', '6601', 12, '调拨单的配送费现付给承运商（issue #104）'
) g
LEFT JOIN fin_account a ON a.code = g.acct AND a.tenant_id = 1;

-- 物料分类（多级）：在物料管理页左侧的分类树中维护（按钮「物料分类维护」wms:material:category，授权给有物料修改权限的角色）。
-- 新租户由仓库模块的 WmsTenantInitializer 复制平台的分类；原材料、半成品、成品、低值易耗品为内置分类（凭证科目按编码使用）
INSERT INTO wms_material_category (parent_id, ancestors, code, name, sort, status, builtin, create_time, update_time) VALUES
(0, '0', 'RAW', '原材料', 1, 1, 1, NOW(), NOW()),
(0, '0', 'SEMI', '半成品', 2, 1, 1, NOW(), NOW()),
(0, '0', 'FINISHED', '成品', 3, 1, 1, NOW(), NOW()),
(0, '0', 'PACKING', '包装材料', 4, 1, 0, NOW(), NOW()),
(0, '0', 'OTHER', '其他', 5, 1, 0, NOW(), NOW()),
(0, '0', 'LOW_VALUE', '低值易耗品', 6, 1, 1, NOW(), NOW());
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1374, 137, '物料分类维护', 'F', NULL, NULL, 'wms:material:category', NULL, 6, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, 1374 FROM sys_role_menu rm
WHERE rm.menu_id = 1371 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 1374);

-- 发票类型（开票类型，业务字典，每个租户一份，issue #82）：订单、直接开单的出入库表头选择后明细税率按它统一调整，
-- 扩展值为税率（%），租户可自行增加；明细未填写税率时取发票类型的税率，没有时取业务参数「默认税率」
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(208, '发票类型', 'scm_invoice_kind', 'BUSINESS', 1, '进销存单据表头的发票类型（开票类型），扩展值为税率（%）：选择后明细税率统一调整为该税率', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, ext_value, create_time, update_time) VALUES
('scm_invoice_kind', '增值税专票 13%', 'SPECIAL_13', 'red', 1, 1, '13', NOW(), NOW()),
('scm_invoice_kind', '增值税专票 9%', 'SPECIAL_9', 'volcano', 2, 1, '9', NOW(), NOW()),
('scm_invoice_kind', '增值税专票 6%', 'SPECIAL_6', 'orange', 3, 1, '6', NOW(), NOW()),
('scm_invoice_kind', '增值税普票 13%', 'NORMAL_13', 'blue', 4, 1, '13', NOW(), NOW()),
('scm_invoice_kind', '增值税普票 3%', 'NORMAL_3', 'geekblue', 5, 1, '3', NOW(), NOW()),
('scm_invoice_kind', '增值税普票 1%', 'NORMAL_1', 'cyan', 6, 1, '1', NOW(), NOW()),
('scm_invoice_kind', '不开票', 'NONE', 'default', 7, 1, '0', NOW(), NOW());

-- 字典种类：业务字典由各租户自己维护（可新增取值），其余为系统字典；程序依赖的取值标记为内置（不能删除、不能改键值）
UPDATE sys_dict_type SET kind = 'BUSINESS' WHERE type IN ('wf_category', 'scm_customer_level', 'fin_settle_method');
UPDATE sys_dict_data SET builtin = 1
WHERE dict_type NOT IN (SELECT type FROM sys_dict_type WHERE kind = 'BUSINESS')
   OR (dict_type = 'fin_settle_method' AND dict_value = 'TRANSFER')
   OR dict_type = 'wf_category'
   OR (dict_type = 'scm_customer_level' AND dict_value = 'POTENTIAL');

-- 往来单位合并与关联（issue #95）：客户合并、供应商合并按钮（销售经理、采购员），应收应付对冲的新增、删除按钮（会计）；
-- 凭证来源「应收应付对冲」。供应商的关联客户（scm_supplier.customer_id）、已合并标记（merged_to）、对冲单表见建表语句
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1474, 147, '供应商合并', 'F', NULL, NULL, 'scm:supplier:merge', NULL, 5, 1, 1, NOW(), NOW()),
(1494, 149, '客户合并', 'F', NULL, NULL, 'scm:customer:merge', NULL, 5, 1, 1, NOW(), NOW()),
(1835, 183, '应收应付对冲', 'F', NULL, NULL, 'fin:netting:add', NULL, 10, 1, 1, NOW(), NOW()),
(1836, 183, '删除对冲单', 'F', NULL, NULL, 'fin:netting:remove', NULL, 11, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 17 AS role_id, 1494 AS menu_id UNION ALL SELECT 4, 1474 UNION ALL SELECT 9, 1835 UNION ALL SELECT 9, 1836
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('fin_voucher_source', '应收应付对冲', 'NETTING', 'volcano', 23, 1, 1, NOW(), NOW());

-- 库存上下限与积压分析（issue #111）：物料的库存预警方式（按总库存 / 分仓库，分仓库的上下限见 wms_material_warehouse_limit），
-- 积压分析报表（进销存报表下，菜单 376，见上方进销存报表的菜单与授权）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(209, '库存预警方式', 'wms_stock_alert_mode', 'SYSTEM', 1, '物料的库存上下限针对所有仓库的合计（按总库存），还是每个仓库单独设置（分仓库）', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('wms_stock_alert_mode', '按总库存', 'TOTAL', 'blue', 1, 1, 1, NOW(), NOW()),
('wms_stock_alert_mode', '分仓库', 'WAREHOUSE', 'purple', 2, 1, 1, NOW(), NOW());
