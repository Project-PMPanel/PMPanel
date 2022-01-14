package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.common.enums.PayStatusEnum;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.entity.Package;
import project.daihao18.panel.mapper.PackageMapper;
import project.daihao18.panel.service.PackageService;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public boolean updateFinishedPackageOrder(BigDecimal payAmount, String payType, String payer, Date payTime, Integer status, Integer id) {
        UpdateWrapper<Package> packageUpdateWrapper = new UpdateWrapper<>();
        packageUpdateWrapper
                .set("pay_amount", payAmount)
                .set("pay_type", payType)
                .set("payer", payer)
                .set("pay_time", payTime)
                .set("status", status)
                .eq("id", id)
                .in("status", PayStatusEnum.WAIT_FOR_PAY.getStatus(), PayStatusEnum.CANCELED.getStatus());
        return this.update(packageUpdateWrapper);
    }

    @Override
    public BigDecimal getMonthIncome() {
        Date now = new Date();
        QueryWrapper<Package> packageQueryWrapper = new QueryWrapper<>();
        packageQueryWrapper
                .select("sum(pay_amount) as total")
                .eq("status", 1)
                .gt("pay_time", DateUtil.beginOfMonth(now))
                .lt("pay_time", DateUtil.endOfMonth(now));
        Map<String, Object> map = this.getMap(packageQueryWrapper);
        return ObjectUtil.isNotEmpty(map) ? new BigDecimal(map.get("total").toString()).setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTodayIncome() {
        Date now = new Date();
        QueryWrapper<Package> packageQueryWrapper = new QueryWrapper<>();
        packageQueryWrapper
                .select("sum(pay_amount) as total")
                .eq("status", 1)
                .gt("pay_time", DateUtil.beginOfDay(now));
        Map<String, Object> map = this.getMap(packageQueryWrapper);
        return ObjectUtil.isNotEmpty(map) ? new BigDecimal(map.get("total").toString()).setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    @Override
    public Result getPackage(HttpServletRequest request) {
        Integer pageNo = Integer.parseInt(request.getParameter("pageNo"));
        Integer pageSize = Integer.parseInt(request.getParameter("pageSize"));
        String id = request.getParameter("id");
        String userId = request.getParameter("userId");
        String status = request.getParameter("status");
        IPage<Package> page = new Page<>(pageNo, pageSize);
        QueryWrapper<Package> packageQueryWrapper = new QueryWrapper<>();
        packageQueryWrapper.orderByDesc("create_time");
        if (ObjectUtil.isNotEmpty(id)) {
            packageQueryWrapper.eq("id", id);
        }
        if (ObjectUtil.isNotEmpty(userId)) {
            packageQueryWrapper.eq("user_id", Integer.parseInt(userId));
        }
        if (ObjectUtil.isNotEmpty(status)) {
            packageQueryWrapper.eq("status", Integer.parseInt(status));
        }
        page = this.page(page, packageQueryWrapper);
        List<Package> packages = page.getRecords();
        Map<String, Object> map = new HashMap<>();
        map.put("data", packages);
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return Result.ok().data("data", map);
    }
}