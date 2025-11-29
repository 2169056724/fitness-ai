-- 用户每日营养记录表
-- 用于存储从计划中提取的营养和热量数据，避免每次解析JSON
CREATE TABLE IF NOT EXISTS `user_nutrition_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `plan_id` BIGINT COMMENT '关联的计划ID（user_plan表）',
    `record_date` DATE NOT NULL COMMENT '记录日期',
    
    -- 总热量
    `total_calories` DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '每日总热量（kcal）',
    `target_calories` DECIMAL(8,2) COMMENT '目标热量（kcal）',
    
    -- 三大营养素（克）
    `protein` DECIMAL(8,2) DEFAULT 0 COMMENT '蛋白质（g）',
    `carbohydrate` DECIMAL(8,2) DEFAULT 0 COMMENT '碳水化合物（g）',
    `fat` DECIMAL(8,2) DEFAULT 0 COMMENT '脂肪（g）',
    
    -- 各餐热量分配
    `breakfast_calories` DECIMAL(7,2) DEFAULT 0 COMMENT '早餐热量',
    `lunch_calories` DECIMAL(7,2) DEFAULT 0 COMMENT '午餐热量',
    `dinner_calories` DECIMAL(7,2) DEFAULT 0 COMMENT '晚餐热量',
    `snack_calories` DECIMAL(7,2) DEFAULT 0 COMMENT '加餐热量',
    
    -- 运动消耗
    `exercise_duration` INT COMMENT '运动时长（分钟）',
    `estimated_burn` DECIMAL(7,2) DEFAULT 0 COMMENT '预估消耗热量（kcal）',
    
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_user_date` (`user_id`, `record_date`),
    INDEX `idx_record_date` (`record_date`),
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日营养记录表';
