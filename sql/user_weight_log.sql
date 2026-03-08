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

 Date: 11/01/2026 17:46:48
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user_weight_log
-- ----------------------------
DROP TABLE IF EXISTS `user_weight_log`;
CREATE TABLE `user_weight_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `record_date` date NOT NULL COMMENT '记录日期',
  `weight_kg` decimal(5, 2) NOT NULL COMMENT '体重(kg)',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `record_date` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户体重日志' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
