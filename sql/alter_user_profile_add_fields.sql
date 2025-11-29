-- =====================================================
-- 健康档案表新增6个字段
-- 执行时间：2025-11-24
-- 说明：参考Keep应用，增强健康档案个性化信息
-- =====================================================

USE health_ai;

-- 1. 新增目标体重字段
ALTER TABLE user_profile 
ADD COLUMN target_weight_kg DECIMAL(5,2) NULL COMMENT '目标体重(kg) - 用于减脂/增肌进度跟踪' 
AFTER goal;

-- 2. 新增训练场景偏好字段
ALTER TABLE user_profile 
ADD COLUMN training_location VARCHAR(20) NULL COMMENT '训练场景偏好（健身房/居家/户外/不限）' 
AFTER target_weight_kg;

-- 3. 新增每天可运动时间字段
ALTER TABLE user_profile 
ADD COLUMN available_time_per_day INT NULL COMMENT '每天可运动时间（分钟）' 
AFTER training_location;

-- 4. 新增运动基础水平字段
ALTER TABLE user_profile 
ADD COLUMN fitness_level VARCHAR(20) NULL COMMENT '运动基础水平（新手/初级/中级/高级）' 
AFTER available_time_per_day;

-- 5. 新增每周训练次数字段
ALTER TABLE user_profile 
ADD COLUMN training_frequency INT NULL COMMENT '每周训练次数' 
AFTER fitness_level;

-- 6. 新增特殊限制/偏好字段
ALTER TABLE user_profile 
ADD COLUMN special_restrictions TEXT NULL COMMENT '特殊限制/偏好（如：膝盖不好、腰椎问题、不喜欢跳跃等）' 
AFTER training_frequency;

-- 查看表结构确认
DESC user_profile;

-- =====================================================
-- 说明：
-- 1. target_weight_kg：目标体重，用于计算减脂/增肌进度
-- 2. training_location：训练场景，决定AI生成的动作类型（器械 vs 徒手）
-- 3. available_time_per_day：每天可运动时间，决定训练计划的时长
-- 4. fitness_level：运动基础，决定动作难度和训练强度
-- 5. training_frequency：每周训练次数，影响训练计划编排
-- 6. special_restrictions：特殊限制，用于避免不适合的动作
-- =====================================================
