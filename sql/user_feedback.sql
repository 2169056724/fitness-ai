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

 Date: 11/01/2026 17:46:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user_feedback
-- ----------------------------
DROP TABLE IF EXISTS `user_feedback`;
CREATE TABLE `user_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，自增ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `plan_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划ID',
  `feedback_date` date NOT NULL COMMENT '反馈所属日期，格式 YYYY-MM-DD',
  `rating` tinyint UNSIGNED NULL DEFAULT NULL COMMENT '整体评分 1-5 星',
  `completion_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '完成率百分比，如 85.50，表示 85.5%',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '用户文字反馈，如“腿很酸”“执行顺利”',
  `emotion_tags` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '情绪标签',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `actual_duration_minutes` int NULL DEFAULT NULL COMMENT '实际训练时长(分钟)',
  `positive_tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '正面感受',
  `negative_tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '负面感受',
  `pain_areas` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '酸痛部位',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_date`(`user_id` ASC, `feedback_date` DESC) USING BTREE,
  INDEX `idx_plan`(`plan_id` ASC) USING BTREE,
  INDEX `idx_user_plan`(`user_id` ASC, `plan_id` ASC) USING BTREE,
  INDEX `idx_date`(`feedback_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户每日反馈表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
