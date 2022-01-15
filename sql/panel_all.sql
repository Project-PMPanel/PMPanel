/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80022
 Source Host           : 127.0.0.1:3306
 Source Schema         : panel

 Target Server Type    : MySQL
 Target Server Version : 80022
 File Encoding         : 65001

 Date: 20/12/2020 16:50:59
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for announcement
-- ----------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `markdown_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告';

-- ----------------------------
-- Table structure for config
-- ----------------------------
DROP TABLE IF EXISTS `config`;
CREATE TABLE `config` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面板配置';

-- ----------------------------
-- Table structure for detect_list
-- ----------------------------
DROP TABLE IF EXISTS `detect_list`;
CREATE TABLE `detect_list` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `regex` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计规则';

-- ----------------------------
-- Table structure for funds
-- ----------------------------
DROP TABLE IF EXISTS `funds`;
CREATE TABLE `funds` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `price` decimal(10,2) DEFAULT '0.00',
  `time` datetime DEFAULT CURRENT_TIMESTAMP,
  `related_order_id` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `content` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `content_english` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `funds_userid` (`user_id`),
  CONSTRAINT `funds_userid` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资金明细表';

-- ----------------------------
-- Table structure for node_with_detect
-- ----------------------------
DROP TABLE IF EXISTS `node_with_detect`;
CREATE TABLE `node_with_detect` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(10) DEFAULT NULL,
  `node_id` int DEFAULT NULL,
  `detect_list_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `node_with_detect_detect_list_id` (`detect_list_id`),
  KEY `node_with_detect_node_id` (`node_id`),
  CONSTRAINT `node_with_detect_detect_list_id` FOREIGN KEY (`detect_list_id`) REFERENCES `detect_list` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for oauth
-- ----------------------------
DROP TABLE IF EXISTS `oauth`;
CREATE TABLE `oauth`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'oauth主键',
  `user_id` int(11) NULL COMMENT '用户id',
  `oauth_type` varchar(10) NULL COMMENT '哪个第三方系统的oauth',
  `email` varchar(50) NULL COMMENT '邮箱',
  `uuid` varchar(50) NULL COMMENT '第三方系统唯一识别号',
  `time` datetime(0) NULL COMMENT '绑定时间',
  `valid` int(1) NULL COMMENT '绑定是否有效',
  PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for operate_ip
-- ----------------------------
DROP TABLE IF EXISTS `operate_ip`;
CREATE TABLE `operate_ip` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` int DEFAULT NULL COMMENT '1-网页登录,2-客户端登录,3-订阅',
  `ip` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `time` datetime DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `operate_ip_user_id` (`user_id`),
  CONSTRAINT `operate_ip_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for order
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '订单id',
  `user_id` int DEFAULT '0' COMMENT '用户id',
  `plan_id` int DEFAULT '0' COMMENT '套餐id',
  `month_count` int DEFAULT '0' COMMENT '订购月数',
  `price` decimal(10,2) unsigned DEFAULT '0.00' COMMENT '应付价格',
  `pay_amount` decimal(10, 2) unsigned DEFAULT '0.00' COMMENT '支付金额',
  `pay_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '支付网关支付类型',
  `payer` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付者',
  `is_new_payer` int DEFAULT '0' COMMENT '是否新用户',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '订单创建时间',
  `expire` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '支付后到期时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `trade_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '支付系统订单号',
  `status` int DEFAULT '0' COMMENT '是否支付',
  `user_details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用户购买时用户详情',
  `plan_details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用户购买时套餐详情',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_id` (`order_id`) COMMENT '订单唯一',
  KEY `order_userid` (`user_id`),
  KEY `order_planid` (`plan_id`),
  CONSTRAINT `order_planid` FOREIGN KEY (`plan_id`) REFERENCES `plan` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `order_userid` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单记录';

-- ----------------------------
-- Table structure for package
-- ----------------------------
DROP TABLE IF EXISTS `package`;
CREATE TABLE `package` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `order_id` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `price` decimal(10,2) unsigned DEFAULT '0.00',
  `transfer_enable` bigint DEFAULT '0',
  `expire` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `pay_amount` decimal(10, 2) unsigned DEFAULT '0.00' COMMENT '支付金额',
  `pay_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '支付网关支付类型',
  `payer` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付者',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `trade_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '支付系统订单号',
  `status` int DEFAULT '0' COMMENT '是否支付',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `package_userid` (`user_id`),
  KEY `package_orderid` (`order_id`),
  CONSTRAINT `package_orderid` FOREIGN KEY (`order_id`) REFERENCES `order` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `package_userid` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流量包记录';

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'user/admin/...',
  `permission` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限';

-- ----------------------------
-- Table structure for plan
-- ----------------------------
DROP TABLE IF EXISTS `plan`;
CREATE TABLE `plan` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '套餐名称',
  `name_english` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '名称英文',
  `price` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '套餐价格',
  `months` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '套餐月数',
  `transfer_enable` bigint DEFAULT '0' COMMENT '套餐流量',
  `node_connector` int DEFAULT '0' COMMENT '连接数',
  `node_speedlimit` int DEFAULT '0' COMMENT '限速',
  `node_group` int DEFAULT '0' COMMENT '节点组',
  `package` bigint DEFAULT '0' COMMENT '流量包流量数/元',
  `class` int DEFAULT '0' COMMENT '套餐等级',
  `buy_limit` int DEFAULT '0' COMMENT '购买数量限制',
  `is_discount` int DEFAULT '0' COMMENT '是否是折扣套餐',
  `discount_start` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '折扣开始时间',
  `discount_end` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '折扣结束时间',
  `sort` int DEFAULT '0' COMMENT '排序',
  `enable` int DEFAULT '1' COMMENT '是否启用',
  `enable_renew` int DEFAULT '0' COMMENT '是否允许续费',
  `support_media` int DEFAULT '0' COMMENT '是否支持流媒体解锁',
  `support_directline` int DEFAULT '0' COMMENT '是否提供专线',
  `extra_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '额外信息',
  `extra_info_english` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '额外信息英文',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐';

-- ----------------------------
-- Table structure for schedule
-- ----------------------------
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule` (
  `id` int NOT NULL AUTO_INCREMENT,
  `bean_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `method_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `method_params` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `job_status` int DEFAULT '1',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for shadowsocks
-- ----------------------------
DROP TABLE IF EXISTS `shadowsocks`;
CREATE TABLE `shadowsocks`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '节点id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '节点名称',
  `out_server` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '落地域名',
  `out_port` int(11) NULL DEFAULT NULL COMMENT '落地端口',
  `sub_server` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '订阅域名',
  `sub_port` int(11) NULL DEFAULT NULL COMMENT '订阅端口',
  `method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '加密方式',
  `traffic_rate` float NULL DEFAULT NULL COMMENT '流量倍率',
  `class` int(11) NULL DEFAULT NULL COMMENT '节点等级',
  `speedlimit` int(11) NULL DEFAULT NULL COMMENT '节点限速',
  `heartbeat` datetime NULL DEFAULT NULL COMMENT '心跳',
  `sort` int(1) DEFAULT 0  NULL COMMENT '排序',
  `flag` int(11) NULL DEFAULT NULL COMMENT '启用停止标志位',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for ticket
-- ----------------------------
DROP TABLE IF EXISTS `ticket`;
CREATE TABLE `ticket`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NULL,
  `title` varchar(50) NULL DEFAULT '',
  `content` text NULL,
  `time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `parent_id` int NULL,
  `status` int NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for trojan
-- ----------------------------
DROP TABLE IF EXISTS `trojan`;
CREATE TABLE `trojan`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '节点id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '节点名称',
  `out_server` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '落地域名',
  `out_port` int(11) NULL DEFAULT NULL COMMENT '落地端口',
  `sub_server` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '订阅域名',
  `sub_port` int(11) NULL DEFAULT NULL COMMENT '订阅端口',
  `grpc` int(1) NULL DEFAULT NULL COMMENT '是否开启grpc',
  `traffic_rate` float NULL DEFAULT NULL COMMENT '流量倍率',
  `class` int(11) NULL DEFAULT NULL COMMENT '节点等级',
  `speedlimit` int(11) NULL DEFAULT NULL COMMENT '节点限速',
  `sni` varchar(255) NULL COMMENT 'sni',
  `heartbeat` datetime NULL DEFAULT NULL COMMENT '心跳',
  `sort` int(1) DEFAULT 0  NULL COMMENT '排序',
  `flag` int(11) NULL DEFAULT NULL COMMENT '启用停止标志位',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ----------------------------
-- Table structure for tutorial
-- ----------------------------
DROP TABLE IF EXISTS `tutorial`;
CREATE TABLE `tutorial` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `markdown_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户登陆邮箱',
  `password` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户登陆密码',
  `money` decimal(10,2) unsigned DEFAULT '0.00' COMMENT '可用余额',
  `invite_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '邀请码',
  `invite_count` int DEFAULT '0' COMMENT '邀请次数',
  `invite_cycle_enable` int DEFAULT '0' COMMENT '是否循环返利',
  `invite_cycle_rate` decimal(10,2) unsigned DEFAULT '0.00' COMMENT '循环返利百分比',
  `parent_id` int DEFAULT '1' COMMENT '父ID',
  `link` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '订阅标志',
  `class` int DEFAULT '0' COMMENT '用户等级',
  `enable` int DEFAULT '1' COMMENT '是否启用',
  `expire_in` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间',
  `t` bigint DEFAULT '0' COMMENT '上次使用时间戳',
  `u` bigint DEFAULT '0' COMMENT '上传流量字节',
  `d` bigint DEFAULT '0' COMMENT '下载流量字节',
  `p` bigint DEFAULT '0' COMMENT '过去已用字节',
  `transfer_enable` bigint DEFAULT '0' COMMENT '可用流量字节',
  `passwd` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '后端连接密码uuid',
  `node_speedlimit` int DEFAULT '0' COMMENT '节点限速',
  `node_connector` int DEFAULT '0' COMMENT '节点连接数',
  `node_group` int DEFAULT '0' COMMENT '节点组',
  `is_admin` int DEFAULT '0' COMMENT '是否管理员',
  `reg_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `tg_id` int(11) NULL COMMENT 'tg的id',
  `last_used_date` datetime NULL COMMENT '上次使用时间',
  `checkin_time` datetime NULL COMMENT '上次签到时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `EMAIL_UNIQUE` (`email`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- Table structure for user_monthly_traffic
-- ----------------------------
DROP TABLE IF EXISTS `user_monthly_traffic`;
CREATE TABLE `user_monthly_traffic` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NULL,
  `date` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `u` bigint NULL DEFAULT 0,
  `d` bigint NULL DEFAULT 0,
  PRIMARY KEY (`id`),
CONSTRAINT `user_monthly_traffic_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ----------------------------
-- Table structure for user_traffic_log
-- ----------------------------
DROP TABLE IF EXISTS `user_traffic_log`;
CREATE TABLE `user_traffic_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT '0',
  `u` int DEFAULT '0',
  `d` int DEFAULT '0',
  `type` varchar(50) DEFAULT '',
  `node_id` int DEFAULT '0',
  `rate` float DEFAULT '0',
  `traffic` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `ip` varchar(50) DEFAULT '',
  `log_time` int DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_traffic_log_userid` (`user_id`),
  CONSTRAINT `user_traffic_log_userid` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for v2ray
-- ----------------------------
DROP TABLE IF EXISTS `v2ray`;
CREATE TABLE `v2ray`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '节点id',
  `name` varchar(50) NULL COMMENT '节点名称',
  `out_server` varchar(50) NULL COMMENT '落地域名',
  `out_port` int NULL COMMENT '落地端口',
  `alter_id` int NULL COMMENT 'AlterId',
  `network` varchar(50) NULL COMMENT '传输协议',
  `security` varchar(50) NULL COMMENT '安全性',
  `host` varchar(50) NULL COMMENT '伪装请求域名',
  `path` varchar(50) NULL COMMENT '伪装请求地址',
  `sub_server` varchar(50) NULL COMMENT '订阅域名',
  `sub_port` int NULL COMMENT '订阅端口',
  `sni` varchar(255) NULL COMMENT 'sni',
  `traffic_rate` float NULL COMMENT '流量倍率',
  `class` int NULL COMMENT '节点等级',
  `speedlimit` int NULL COMMENT '节点限速',
  `heartbeat` datetime NULL COMMENT '心跳',
  `sort` int(1) DEFAULT 0  NULL COMMENT '排序',
  `flag` int NULL COMMENT '启用停止标志位',
  PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for online
-- ----------------------------
DROP TABLE IF EXISTS `online`;
CREATE TABLE `online`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int(11) NULL COMMENT '用户id',
  `time` datetime NULL COMMENT '时间',
  `ip` varchar(50) NULL COMMENT 'ip',
  `type` varchar(255) NULL COMMENT '节点类型',
  `node_id` int(11) NULL COMMENT '节点id',
  PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for withdraw
-- ----------------------------
DROP TABLE IF EXISTS `withdraw`;
CREATE TABLE `withdraw` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `amount` decimal(10,2) unsigned DEFAULT '0.00',
  `real_amount` decimal(10,2) unsigned DEFAULT '0.00',
  `account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `withdraw_userid` (`user_id`),
  CONSTRAINT `withdraw_userid` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE `ticket`
ADD CONSTRAINT `ticket_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `ticket` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `oauth`
ADD CONSTRAINT `oauth_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

INSERT INTO `config`(`id`, `name`, `value`) VALUES (1, 'siteName', 'PMPanel');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (2, 'siteUrl', 'http://127.0.0.1');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (3, 'subUrl', 'http://127.0.0.1');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (4, 'regEnable', 'true');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (5, 'inviteOnly', 'false');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (6, 'mailRegEnable', 'false');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (7, 'mailLimit', '10');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (8, 'mailType', 'smtp');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (9, 'mailConfig', '{\"password\":\"\",\"port\":\"\",\"host\":\"\",\"username\":\"\"}');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (10, 'notifyMailType', 'smtp');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (11, 'notifyMailConfig', '{\"password\":\"\",\"port\":\"\",\"host\":\"\",\"username\":\"\"}');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (101, 'enableEmailSuffix', '@qq.com;@gmail.com');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (102, 'inviteCount', '10');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (103, 'inviteRate', '0.1');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (104, 'enableWithdraw', 'false');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (105, 'minWithdraw', '50');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (106, 'withdrawRate', '0.15');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (201, 'alipay', 'none');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (202, 'wxpay', 'none');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (203, 'alipayConfig', '{\"appId\":\"\",\"pId\":\"\",\"isCertMode\":false,\"appPrivateKey\":\"\",\"alipayPublicKey\":\"\",\"appCertPath\":\"\",\"alipayCertPath\":\"\",\"alipayRootCertPath\":\"\",\"serverUrl\":\"https://openapi.alipay.com/gateway.do\",\"domain\":\"http://127.0.0.1\",\"web\":false,\"wap\":false,\"f2f\":false}');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (204, 'stripeConfig', '{\"sk_live\":\"\",\"webhook_secret\":\"\",\"currency\":\"\"}');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (301, 'muSuffix', 'download.windowsupdate.com');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (302, 'userTrafficLogLimitDays', '3');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (401, 'clientConfig', '');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (402, 'mailTemplate', '<div id=\"mail\">\r\n		<div id=\"main\">\r\n			<div id=\"header\">\r\n				{siteName}\r\n			</div>\r\n			<div id=\"title\">\r\n				{title}\r\n			</div>\r\n			<hr>\r\n			<div id=\"content\">\r\n				{content}\r\n			</div>\r\n			<hr>\r\n			<div id=\"footer\">\r\n				<a href=\"{siteUrl}\" style=\"margin-left: 1%;color: #929292;text-decoration:none\">前往{siteName}</a>\r\n			</div>\r\n		</div>\r\n	</div>\r\n\r\n	<style type=\"text/css\">\r\n		#mail {\r\n			background: #48484845;\r\n			padding: 1%;\r\n		}\r\n		#main {\r\n			margin: 0 30% 0 30%;\r\n			line-height: 20px;\r\n			background: white;\r\n		}\r\n		#header {\r\n			width: 100%;\r\n			height: 10%;\r\n			background: #4880c5;\r\n			font-size: 40px;\r\n			line-height: 80px;\r\n			text-align: center;\r\n			color: white;\r\n		}\r\n		#title {\r\n			height: 40px;\r\n			line-height: 40px;\r\n			font-size: 24px;\r\n			margin: 2%;\r\n			padding-left: 2%;\r\n		}\r\n		#content {\r\n			margin: 2% 0 0 2%;\r\n			font-size: 20px;\r\n			line-height: 1.5;\r\n			padding: 1% 0 5% 2%;\r\n		}\r\n		#footer {\r\n			height: 40px;\r\n			line-height: 40px;\r\n			font-size: 18px;\r\n			padding-left: 2%;\r\n		}\r\n	</style>');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (501, 'oauthConfig', '{\"enable\":false,\"google\":{\"redirectUri\":\"\",\"secret\":\"\",\"enable\":false,\"id\":\"\"}}');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (502, 'enableNotifyRenew', 'true');
INSERT INTO `config`(`id`, `name`, `value`) VALUES (503, 'notifyInfo', '');

INSERT INTO `permission`(`id`, `role`, `permission`) VALUES (1, 'user', 'user');
INSERT INTO `permission`(`id`, `role`, `permission`) VALUES (2, 'admin', 'admin');

INSERT INTO `schedule`(`id`, `bean_name`, `method_name`, `method_params`, `cron_expression`, `remark`, `job_status`, `created_time`, `update_time`) VALUES (1, 'dailyJobTaskService', 'syncTraffic', NULL, '0 0 0 * * *', '每日同步流量', 1, '2020-11-11 11:11:11', NULL);
INSERT INTO `schedule`(`id`, `bean_name`, `method_name`, `method_params`, `cron_expression`, `remark`, `job_status`, `created_time`, `update_time`) VALUES (2, 'dailyJobTaskService', 'cleanDB', NULL, '0 0 0 * * *', '每日清理数据库', 1, '2020-11-11 11:11:11', NULL);
INSERT INTO `schedule`(`id`, `bean_name`, `method_name`, `method_params`, `cron_expression`, `remark`, `job_status`, `created_time`, `update_time`) VALUES (3, 'checkJobTaskService', 'checkJob', NULL, '0 * * * * ?', '检查任务', 1, '2020-11-11 11:11:11', NULL);
INSERT INTO `schedule`(`id`, `bean_name`, `method_name`, `method_params`, `cron_expression`, `remark`, `job_status`, `created_time`, `update_time`) VALUES (4, 'checkOrderJobTaskService', 'checkOrderJob', NULL, '*/5 * * * * ?', '支付宝主动查单', 1, '2020-11-11 11:11:11', NULL);
INSERT INTO `schedule`(`id`, `bean_name`, `method_name`, `method_params`, `cron_expression`, `remark`, `job_status`, `created_time`, `update_time`) VALUES (5, 'notifyRenewJobTaskService', 'notifyRenewJob', NULL, '0 0 2 28 * ?', '月底发送续费通知邮件', 1, '2020-11-11 11:11:11', NULL);
INSERT INTO `schedule`(`id`, `bean_name`, `method_name`, `method_params`, `cron_expression`, `remark`, `job_status`, `created_time`, `update_time`) VALUES (6, 'telegramJobTaskService', 'telegramJob', NULL, '*/2 * * * * ?', 'telegram任务', 1, '2020-11-11 11:11:11', NULL);