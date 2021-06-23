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

ALTER TABLE `oauth`
ADD CONSTRAINT `oauth_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE

INSERT INTO `config`(`id`, `name`, `value`) VALUES (501, 'oauthConfig', '{\"enable\":false,\"google\":{\"redirectUri\":\"\",\"secret\":\"\",\"enable\":false,\"id\":\"\"}}');