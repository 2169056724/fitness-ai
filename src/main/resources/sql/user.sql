-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` VARCHAR(64) NOT NULL COMMENT '微信 openid',
  `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信 unionid',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像',
  `gender` TINYINT DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  `phone` VARCHAR(11) DEFAULT NULL COMMENT '手机号',
  `age` INT DEFAULT NULL COMMENT '年龄',
  `height_cm` DECIMAL(5,2) DEFAULT NULL COMMENT '身高cm',
  `weight_kg` DECIMAL(5,2) DEFAULT NULL COMMENT '体重kg',
  `goal` VARCHAR(32) DEFAULT NULL COMMENT '目标',
  `medical_history` TEXT DEFAULT NULL COMMENT '病史',
  `is_profile_complete` TINYINT NOT NULL DEFAULT 0 COMMENT '资料是否完整 0否 1是',
  `register_source` VARCHAR(32) DEFAULT 'wechat_mp' COMMENT '注册来源',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最近登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_unionid` (`unionid`),
  KEY `idx_phone` (`phone`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

