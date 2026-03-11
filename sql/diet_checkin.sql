-- 饮食打卡记录表
-- 记录用户每餐的执行程度（5级）
CREATE TABLE IF NOT EXISTS `diet_checkin` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `checkin_date` DATE NOT NULL COMMENT '打卡日期',
    `meal_type` TINYINT NOT NULL COMMENT '餐次: 1=早餐 2=午餐 3=晚餐',
    `level` TINYINT NOT NULL COMMENT '执行等级: 1=没吃 2=吃了一点 3=吃了一半左右 4=正常吃了 5=吃撑了',
    `ratio` DECIMAL(3,2) NOT NULL COMMENT '对应比例: 0.00/0.30/0.60/1.00/1.30',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_date_meal` (`user_id`, `checkin_date`, `meal_type`),
    INDEX `idx_user_date` (`user_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食打卡记录表';
