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

 Date: 11/01/2026 17:46:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user_nutrition_record
-- ----------------------------
DROP TABLE IF EXISTS `user_nutrition_record`;
CREATE TABLE `user_nutrition_record`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `plan_id` bigint NULL DEFAULT NULL COMMENT '关联的计划ID（user_plan表）',
  `record_date` date NOT NULL COMMENT '记录日期',
  `total_calories` decimal(8, 2) NOT NULL DEFAULT 0.00 COMMENT '每日总热量（kcal）',
  `target_calories` decimal(8, 2) NULL DEFAULT NULL COMMENT '目标热量（kcal）',
  `protein` decimal(8, 2) NULL DEFAULT 0.00 COMMENT '蛋白质（g）',
  `carbohydrate` decimal(8, 2) NULL DEFAULT 0.00 COMMENT '碳水化合物（g）',
  `fat` decimal(8, 2) NULL DEFAULT 0.00 COMMENT '脂肪（g）',
  `breakfast_calories` decimal(7, 2) NULL DEFAULT 0.00 COMMENT '早餐热量',
  `lunch_calories` decimal(7, 2) NULL DEFAULT 0.00 COMMENT '午餐热量',
  `dinner_calories` decimal(7, 2) NULL DEFAULT 0.00 COMMENT '晚餐热量',
  `snack_calories` decimal(7, 2) NULL DEFAULT 0.00 COMMENT '加餐热量',
  `exercise_duration` int NULL DEFAULT NULL COMMENT '运动时长（分钟）',
  `estimated_burn` decimal(7, 2) NULL DEFAULT 0.00 COMMENT '预估消耗热量（kcal）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `record_date` ASC) USING BTREE,
  INDEX `idx_user_date`(`user_id` ASC, `record_date` ASC) USING BTREE,
  INDEX `idx_record_date`(`record_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户每日营养记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
