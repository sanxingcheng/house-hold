-- ========== 迭代四：account 表按 user_id 水平分为 4 张物理表 ==========
-- ShardingSphere-JDBC 将逻辑表 "account" 路由到 account_0 ~ account_3

CREATE TABLE IF NOT EXISTS account_0 (
  id            BIGINT PRIMARY KEY              COMMENT '账户 ID（应用生成）',
  user_id       BIGINT NOT NULL                 COMMENT '所属用户 ID',
  family_id     BIGINT DEFAULT NULL             COMMENT '所属家庭 ID（冗余，便于家庭维度查询）',
  account_name  VARCHAR(64) NOT NULL            COMMENT '账户名称',
  account_type  VARCHAR(32) NOT NULL            COMMENT '类型: SAVING/CREDIT_CARD/STOCK/FUND/ALIPAY/WECHAT/OTHER',
  balance       BIGINT NOT NULL DEFAULT 0       COMMENT '余额（单位：分）',
  currency      VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '货币',
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_user_id   (user_id),
  KEY idx_family_id (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人账户表（分片 0）';

CREATE TABLE IF NOT EXISTS account_1 LIKE account_0;
CREATE TABLE IF NOT EXISTS account_2 LIKE account_0;
CREATE TABLE IF NOT EXISTS account_3 LIKE account_0;

-- 如需从原 account 表迁移存量数据：
-- INSERT INTO account_0 SELECT * FROM account WHERE user_id % 4 = 0;
-- INSERT INTO account_1 SELECT * FROM account WHERE user_id % 4 = 1;
-- INSERT INTO account_2 SELECT * FROM account WHERE user_id % 4 = 2;
-- INSERT INTO account_3 SELECT * FROM account WHERE user_id % 4 = 3;
-- DROP TABLE IF EXISTS account;

-- 迭代五：家庭共有资产表
CREATE TABLE IF NOT EXISTS family_asset (
  id            BIGINT PRIMARY KEY              COMMENT '资产ID',
  family_id     BIGINT NOT NULL                 COMMENT '所属家庭ID',
  asset_name    VARCHAR(64) NOT NULL            COMMENT '资产名称',
  asset_type    VARCHAR(32) NOT NULL            COMMENT '类型: REAL_ESTATE/VEHICLE/DEPOSIT/INVESTMENT/OTHER',
  amount        BIGINT NOT NULL DEFAULT 0       COMMENT '金额（单位：分）',
  currency      VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '货币',
  remark        VARCHAR(256) DEFAULT NULL       COMMENT '备注',
  created_by    BIGINT NOT NULL                 COMMENT '创建人ID',
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_family_id (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭共有资产表';

CREATE TABLE IF NOT EXISTS wealth_snapshot (
  id                BIGINT PRIMARY KEY              COMMENT '快照 ID',
  owner_type        VARCHAR(16) NOT NULL            COMMENT 'USER 或 FAMILY',
  owner_id          BIGINT NOT NULL                 COMMENT '用户 ID 或家庭 ID',
  total_assets      BIGINT NOT NULL DEFAULT 0       COMMENT '总资产（分）',
  total_liabilities BIGINT NOT NULL DEFAULT 0       COMMENT '总负债（信用卡账单，分）',
  net_worth         BIGINT NOT NULL DEFAULT 0       COMMENT '净资产（分）',
  snapshot_date     DATE NOT NULL                   COMMENT '快照日期（按天聚合）',
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_owner_date (owner_type, owner_id, snapshot_date),
  KEY idx_owner (owner_type, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财富快照表';
