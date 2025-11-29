-- 用户当日推荐计划持久化表（表名 user_plan）
CREATE TABLE IF NOT EXISTS `user_plan` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户id',
  `plan_json` JSON DEFAULT NULL COMMENT '方案详情 JSON',
  `date` DATE NOT NULL COMMENT '方案日期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日推荐计划（仅保存最终选定/生成的方案）';
