/*
 Navicat Premium Data Transfer

 Source Server         : Local MySQL
 Source Server Type    : MySQL
 Source Server Version : 50525
 Source Host           : localhost
 Source Database       : cardgame

 Target Server Type    : MySQL
 Target Server Version : 50525
 File Encoding         : utf-8

 Date: 01/12/2014 01:48:49 AM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `session_logs`
-- ----------------------------
DROP TABLE IF EXISTS `session_logs`;
CREATE TABLE `session_logs` (
  `user_id` bigint(20) NOT NULL,
  `sign_in_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `sign_out_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `insert_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00'
) DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `users`
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `registration_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_signed_in` timestamp NOT NULL DEFAULT '2012-01-01 00:00:00',
  `banned_until` timestamp NOT NULL DEFAULT '2012-01-01 00:00:00',
  `points` int(11) NOT NULL,
  `wins` int(11) NOT NULL DEFAULT '0',
  `loses` int(11) NOT NULL DEFAULT '0',
  `leaves` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
