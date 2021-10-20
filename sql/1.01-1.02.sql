ALTER TABLE `shadowsocks`
ADD COLUMN `sort` int(1) DEFAULT 0 NULL COMMENT '排序' AFTER `heartbeat`;
ALTER TABLE `v2ray`
ADD COLUMN `sort` int(1) DEFAULT 0  NULL COMMENT '排序' AFTER `heartbeat`;
ALTER TABLE `trojan`
ADD COLUMN `sort` int(1) DEFAULT 0  NULL COMMENT '排序' AFTER `heartbeat`;