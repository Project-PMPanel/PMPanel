package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.common.enums.PayStatusEnum;
import project.daihao18.panel.entity.Package;
import project.daihao18.panel.mapper.PackageMapper;
import project.daihao18.panel.service.PackageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @ClassName: PackageServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:14
 */
@Service
public class PackageServiceImpl extends ServiceImpl<PackageMapper, Package> implements PackageService {
    @Override
    @Transactional
    public void expiredFinishedPackageOrder() {
        UpdateWrapper<Package> packageUpdateWrapper = new UpdateWrapper<>();
        packageUpdateWrapper
                .set("status", PayStatusEnum.INVALID.getStatus())
                .eq("status", PayStatusEnum.SUCCESS.getStatus())
                .lt("expire", LocalDateTime.now());
        this.update(packageUpdateWrapper);
    }

    @Override
    @Transactional
    public boolean updateFinishedPackageOrder(boolean isMixedPay, BigDecimal mixedMoneyAmount, BigDecimal mixedPayAmount, String payType, String payer, Date payTime, Integer status, Integer id) {
        UpdateWrapper<Package> packageUpdateWrapper = new UpdateWrapper<>();
        packageUpdateWrapper
                .set("is_mixed_pay", isMixedPay)
                .set("mixed_money_amount", mixedMoneyAmount)
                .set("mixed_pay_amount", mixedPayAmount)
                .set("pay_type", payType)
                .set("payer", payer)
                .set("pay_time", payTime)
                .set("status", status)
                .eq("id", id)
                .in("status", PayStatusEnum.WAIT_FOR_PAY.getStatus(), PayStatusEnum.CANCELED.getStatus());
        return this.update(packageUpdateWrapper);
    }
}