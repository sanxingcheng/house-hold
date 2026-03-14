-- 微服务数据库隔离：各服务使用独立数据库，需先执行本脚本创建库（执行一次即可）
-- 使用方式：mysql -u root -p < scripts/init-databases.sql

CREATE DATABASE IF NOT EXISTS household_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS household_wealth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
