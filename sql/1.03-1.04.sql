ALTER TABLE `order`
DROP COLUMN `is_mixed_pay`,
DROP COLUMN `mixed_money_amount`,
CHANGE COLUMN `mixed_pay_amount` `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额' AFTER `price`;

ALTER TABLE `package`
DROP COLUMN `is_mixed_pay`,
DROP COLUMN `mixed_money_amount`,
CHANGE COLUMN `mixed_pay_amount` `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额' AFTER `create_time`;