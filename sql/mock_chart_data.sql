-- ================================================
-- 图表测试模拟数据
-- 为 user_id = 1 生成过去 30 天的测试数据
-- 运行此脚本前请确保 user 和 user_profile 表已有 user_id = 1 的记录
-- ================================================

SET NAMES utf8mb4;

-- ================================================
-- 1. 用户体重日志 (user_weight_log)
-- 模拟过去 30 天减重趋势：从 73.5kg 逐步降到 71.2kg
-- ================================================
INSERT INTO `user_weight_log` (`user_id`, `record_date`, `weight_kg`) VALUES
(5, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 73.50),
(5, DATE_SUB(CURDATE(), INTERVAL 28 DAY), 73.30),
(5, DATE_SUB(CURDATE(), INTERVAL 26 DAY), 73.10),
(5, DATE_SUB(CURDATE(), INTERVAL 24 DAY), 72.90),
(5, DATE_SUB(CURDATE(), INTERVAL 22 DAY), 72.80),
(5, DATE_SUB(CURDATE(), INTERVAL 20 DAY), 72.60),
(5, DATE_SUB(CURDATE(), INTERVAL 18 DAY), 72.40),
(5, DATE_SUB(CURDATE(), INTERVAL 16 DAY), 72.50),
(5, DATE_SUB(CURDATE(), INTERVAL 14 DAY), 72.20),
(5, DATE_SUB(CURDATE(), INTERVAL 12 DAY), 72.00),
(5, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 71.80),
(5, DATE_SUB(CURDATE(), INTERVAL 8 DAY), 71.70),
(5, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 71.50),
(5, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 71.40),
(5, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 71.30),
(5, CURDATE(), 71.20)
ON DUPLICATE KEY UPDATE weight_kg = VALUES(weight_kg);

-- ================================================
-- 2. 用户反馈 (user_feedback)
-- 模拟过去 30 天的训练反馈，完成率 60%-95%
-- ================================================
INSERT INTO `user_feedback` (`user_id`, `plan_id`, `feedback_date`, `rating`, `completion_rate`, `notes`, `emotion_tags`, `actual_duration_minutes`, `positive_tags`, `negative_tags`, `pain_areas`) VALUES
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 29 DAY), 4, 75.00, '刚开始恢复训练，有点吃力', '一般', 45, '坚持完成', '体力不足', NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 28 DAY), 4, 80.00, '今天状态好一些', '开心', 50, '状态不错', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 27 DAY), 3, 65.00, '工作太忙只完成了部分', '疲惫', 30, NULL, '时间不够', NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 26 DAY), 5, 90.00, '周末时间充裕', '开心', 60, '精力充沛,状态极佳', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 25 DAY), 4, 85.00, '保持得不错', '满意', 55, '进步明显', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 24 DAY), 4, 82.00, '腿有点酸', '一般', 50, NULL, NULL, '大腿'),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 23 DAY), 3, 70.00, '降低了强度', '疲惫', 40, NULL, '肌肉酸痛', '小腿'),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 22 DAY), 4, 78.00, '恢复中', '一般', 45, NULL, NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 21 DAY), 5, 92.00, '感觉力量提升了', '兴奋', 65, '力量提升,突破自我', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 20 DAY), 4, 88.00, '保持稳定', '满意', 55, '稳定发挥', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 19 DAY), 4, 84.00, '正常训练', '开心', 52, NULL, NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 18 DAY), 3, 68.00, '有点累', '疲惫', 38, NULL, '睡眠不足', NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 17 DAY), 4, 80.00, '调整后恢复', '一般', 48, NULL, NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 16 DAY), 5, 95.00, '今天状态超好！', '兴奋', 70, '精力充沛,状态极佳,突破自我', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 15 DAY), 4, 86.00, '维持良好状态', '满意', 55, '稳定发挥', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 14 DAY), 4, 82.00, '周末继续努力', '开心', 50, NULL, NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 13 DAY), 5, 91.00, '又破了一次PR', '兴奋', 62, '突破自我,力量提升', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 12 DAY), 4, 85.00, '保持节奏', '满意', 54, '进步明显', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 11 DAY), 3, 72.00, '天气不好影响心情', '一般', 42, NULL, '状态一般', NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 10 DAY), 4, 79.00, '恢复正常', '开心', 48, NULL, NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 9 DAY), 4, 83.00, '稳步前进', '满意', 52, '稳定发挥', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 8 DAY), 5, 93.00, '最近状态很好', '兴奋', 65, '精力充沛,状态极佳', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 7 DAY), 4, 87.00, '一周的开始', '开心', 56, NULL, NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 6 DAY), 4, 84.00, '继续保持', '满意', 53, '坚持完成', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 5 DAY), 3, 74.00, '有点疲劳', '疲惫', 44, NULL, '肌肉酸痛', '背部'),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 4 DAY), 4, 81.00, '适当休息后恢复', '一般', 50, NULL, NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 3 DAY), 5, 94.00, '完美训练日！', '兴奋', 68, '精力充沛,突破自我,状态极佳', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 4, 88.00, '保持高强度', '满意', 57, '进步明显', NULL, NULL),
(5, 'PLAN_001', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 4, 85.00, '稳定输出', '开心', 54, '稳定发挥', NULL, NULL),
(5, 'PLAN_001', CURDATE(), 5, 90.00, '今天感觉非常棒', '兴奋', 60, '状态极佳,力量提升', NULL, NULL)
ON DUPLICATE KEY UPDATE 
    rating = VALUES(rating),
    completion_rate = VALUES(completion_rate),
    notes = VALUES(notes);

-- ================================================
-- 3. 用户营养记录 (user_nutrition_record)
-- 模拟过去 30 天的营养摄入，目标热量 2000kcal
-- ================================================
INSERT INTO `user_nutrition_record` (`user_id`, `plan_id`, `record_date`, `total_calories`, `target_calories`, `protein`, `carbohydrate`, `fat`, `breakfast_calories`, `lunch_calories`, `dinner_calories`, `snack_calories`, `exercise_duration`, `estimated_burn`) VALUES
(5, 1, DATE_SUB(CURDATE(), INTERVAL 29 DAY), 1950.00, 2000.00, 120.00, 210.00, 65.00, 450.00, 650.00, 600.00, 250.00, 45, 350.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 28 DAY), 2050.00, 2000.00, 125.00, 220.00, 68.00, 480.00, 680.00, 620.00, 270.00, 50, 380.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 27 DAY), 1850.00, 2000.00, 110.00, 195.00, 62.00, 420.00, 600.00, 580.00, 250.00, 30, 280.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 26 DAY), 2100.00, 2000.00, 130.00, 230.00, 70.00, 500.00, 700.00, 650.00, 250.00, 60, 420.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 25 DAY), 1980.00, 2000.00, 122.00, 212.00, 66.00, 460.00, 660.00, 610.00, 250.00, 55, 390.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 24 DAY), 2020.00, 2000.00, 126.00, 218.00, 67.00, 470.00, 670.00, 630.00, 250.00, 50, 370.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 23 DAY), 1900.00, 2000.00, 115.00, 200.00, 64.00, 440.00, 620.00, 590.00, 250.00, 40, 320.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 22 DAY), 1960.00, 2000.00, 120.00, 208.00, 65.00, 455.00, 645.00, 610.00, 250.00, 45, 345.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 21 DAY), 2080.00, 2000.00, 128.00, 225.00, 69.00, 490.00, 690.00, 640.00, 260.00, 65, 430.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 20 DAY), 1990.00, 2000.00, 123.00, 214.00, 66.00, 462.00, 658.00, 620.00, 250.00, 55, 385.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 19 DAY), 2010.00, 2000.00, 124.00, 216.00, 67.00, 468.00, 665.00, 627.00, 250.00, 52, 375.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 18 DAY), 1870.00, 2000.00, 112.00, 198.00, 63.00, 430.00, 610.00, 580.00, 250.00, 38, 300.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 17 DAY), 1940.00, 2000.00, 118.00, 206.00, 65.00, 448.00, 640.00, 602.00, 250.00, 48, 355.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 16 DAY), 2120.00, 2000.00, 132.00, 232.00, 71.00, 505.00, 710.00, 655.00, 250.00, 70, 450.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 15 DAY), 2000.00, 2000.00, 124.00, 215.00, 67.00, 465.00, 662.00, 623.00, 250.00, 55, 388.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 14 DAY), 1970.00, 2000.00, 121.00, 210.00, 66.00, 457.00, 652.00, 611.00, 250.00, 50, 368.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 13 DAY), 2060.00, 2000.00, 127.00, 222.00, 69.00, 482.00, 685.00, 643.00, 250.00, 62, 415.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 12 DAY), 1985.00, 2000.00, 122.00, 213.00, 66.00, 461.00, 657.00, 617.00, 250.00, 54, 382.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 11 DAY), 1890.00, 2000.00, 114.00, 199.00, 64.00, 436.00, 618.00, 586.00, 250.00, 42, 330.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 1955.00, 2000.00, 119.00, 207.00, 65.00, 452.00, 643.00, 610.00, 250.00, 48, 358.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 9 DAY), 1995.00, 2000.00, 123.00, 214.00, 66.00, 463.00, 660.00, 622.00, 250.00, 52, 378.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 8 DAY), 2090.00, 2000.00, 129.00, 226.00, 70.00, 492.00, 692.00, 646.00, 260.00, 65, 425.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 7 DAY), 2015.00, 2000.00, 125.00, 217.00, 67.00, 470.00, 668.00, 627.00, 250.00, 56, 392.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 1975.00, 2000.00, 121.00, 211.00, 66.00, 458.00, 654.00, 613.00, 250.00, 53, 375.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 1920.00, 2000.00, 116.00, 203.00, 64.00, 443.00, 630.00, 597.00, 250.00, 44, 340.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 1965.00, 2000.00, 120.00, 209.00, 65.00, 455.00, 648.00, 612.00, 250.00, 50, 365.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 2110.00, 2000.00, 131.00, 230.00, 70.00, 502.00, 705.00, 653.00, 250.00, 68, 445.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2030.00, 2000.00, 126.00, 219.00, 68.00, 475.00, 675.00, 630.00, 250.00, 57, 398.00),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1990.00, 2000.00, 123.00, 214.00, 66.00, 462.00, 658.00, 620.00, 250.00, 54, 380.00),
(5, 1, CURDATE(), 2040.00, 2000.00, 127.00, 220.00, 68.00, 478.00, 680.00, 632.00, 250.00, 60, 405.00)
ON DUPLICATE KEY UPDATE 
    total_calories = VALUES(total_calories),
    protein = VALUES(protein),
    carbohydrate = VALUES(carbohydrate),
    fat = VALUES(fat);

-- ================================================
-- 验证数据插入
-- ================================================
SELECT '体重日志记录数' AS `数据表`, COUNT(*) AS `记录数` FROM user_weight_log WHERE user_id = 1
UNION ALL
SELECT '用户反馈记录数', COUNT(*) FROM user_feedback WHERE user_id = 1
UNION ALL
SELECT '营养记录数', COUNT(*) FROM user_nutrition_record WHERE user_id = 1;
