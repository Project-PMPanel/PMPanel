ALTER TABLE `user`
ADD COLUMN `tg_id` int(11) NULL COMMENT 'tg的id' AFTER `reg_date`;
ALTER TABLE `user`
ADD COLUMN `last_used_date` datetime NULL COMMENT '上次使用时间' AFTER `tg_id`;
ALTER TABLE `user`
ADD COLUMN `checkin_time` datetime NULL COMMENT '上次签到时间' AFTER `last_used_date`;