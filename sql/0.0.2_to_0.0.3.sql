ALTER TABLE `user`
ADD COLUMN `tg_id` int(11) NULL COMMENT 'tg的id' AFTER `reg_date`;