/*
 Navicat Premium Data Transfer

 Source Server         : vm_mysql
 Source Server Type    : MySQL
 Source Server Version : 90500
 Source Host           : 192.168.234.100:3306
 Source Schema         : health_ai

 Target Server Type    : MySQL
 Target Server Version : 90500
 File Encoding         : 65001

 Date: 11/01/2026 17:46:42
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user_profile
-- ----------------------------
DROP TABLE IF EXISTS `user_profile`;
CREATE TABLE `user_profile`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '关联用户表',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `gender` tinyint NULL DEFAULT NULL COMMENT '性别 0未知 1男 2女',
  `height_cm` decimal(5, 2) NULL DEFAULT NULL COMMENT '身高cm',
  `weight_kg` decimal(5, 2) NULL DEFAULT NULL COMMENT '体重kg',
  `goal` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标',
  `target_weight_kg` decimal(5, 2) NULL DEFAULT NULL COMMENT '目标体重(kg) - 用于减脂/增肌进度跟踪',
  `training_location` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '训练场景偏好（健身房/居家/户外/不限）',
  `available_time_per_day` int NULL DEFAULT NULL COMMENT '每天可运动时间（分钟）',
  `fitness_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '运动基础水平（新手/初级/中级/高级）',
  `training_frequency` int NULL DEFAULT NULL COMMENT '每周训练次数',
  `special_restrictions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '特殊限制/偏好（如：膝盖不好、腰椎问题、不喜欢跳跃等）',
  `medical_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '病史',
  `activity_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日常活动水平',
  `medical_report_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '体检单路径',
  `extracted_medical_data` json NULL COMMENT '识别结果',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `breakfast_time` time NULL DEFAULT NULL COMMENT '早餐时间',
  `lunch_time` time NULL DEFAULT NULL COMMENT '午餐时间',
  `dinner_time` time NULL DEFAULT NULL COMMENT '晚餐时间',
  `snack_time` time NULL DEFAULT NULL COMMENT '加餐时间（可选）',
  `medical_advice_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '预计算的医疗建议提示词（缓存）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `ids_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
