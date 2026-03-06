CREATE TABLE IF NOT EXISTS account (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人账户表';

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
