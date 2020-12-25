package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.utils.FlowSizeConverterUtil;
import project.daihao18.panel.entity.Plan;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.mapper.PlanMapper;
import project.daihao18.panel.service.PlanService;
import project.daihao18.panel.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: PlanServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:22
 */
@Service
public class PlanServiceImpl extends ServiceImpl<PlanMapper, Plan> implements PlanService {

    @Autowired
    private UserService userService;

    @Override
    public List<Plan> getPlan(Integer userId) {
        QueryWrapper<Plan> planQueryWrapper = new QueryWrapper<>();
        // 已启用不是折扣套餐且没有到达购买限制的，已启用是折扣套餐没有到达购买限制且在折扣时间内的
        planQueryWrapper
                .eq("enable", true)
                .eq("is_discount", false)
                .ne("buy_limit", 0)
                .or()
                .eq("enable", true)
                .eq("is_discount", true)
                .ne("buy_limit", 0)
                .lt("discount_start", LocalDateTime.now())
                .gt("discount_end", LocalDateTime.now())
                .orderByAsc("sort");
        List<Plan> lists = this.list(planQueryWrapper);
        lists.forEach(list -> {
            list.setPackageGb(FlowSizeConverterUtil.BytesToGb(list.getPackagee()));
            list.setTransferEnableGb(FlowSizeConverterUtil.BytesToGb(list.getTransferEnable()));
            list.setMonthsList(Arrays.stream(list.getMonths().split("-")).map(Integer::parseInt).collect(Collectors.toList()));
            list.setPriceList(Arrays.stream(list.getPrice().split("-")).map(BigDecimal::new).collect(Collectors.toList()));
        });
        // 根据套餐月数和设置的价格去计算该用户的过期时间和价格,并重新设置
        User user = userService.getById(userId);
        lists.forEach(list -> {
            Result calcInfo;
            List<BigDecimal> priceList = new ArrayList<>();
            List<Date> expireList = new ArrayList<>();
            for (Integer month : list.getMonthsList()) {
                calcInfo = userService.getChoosePlanInfo(user, list.getMonths(), list.getPrice(), month);
                priceList.add(new BigDecimal(calcInfo.getData().get("calcPrice").toString()));
                expireList.add(DateUtil.parse(calcInfo.getData().get("calcExpire").toString()));
            }
            list.setPriceList(priceList);
            list.setExpireList(expireList);
        });
        return lists;
    }

    @Override
    // @Cacheable(cacheNames = "panel::plan::plans", key = "#pageNo+'-'+#pageSize", unless = "#result == null")
    public Result getPlan(Integer pageNo, Integer pageSize) {
        IPage<Plan> page = new Page<>(pageNo, pageSize);
        QueryWrapper<Plan> planQueryWrapper = new QueryWrapper<>();
        planQueryWrapper
                .orderByAsc("sort");
        page = this.page(page, planQueryWrapper);
        Map<String, Object> map = new HashMap<>();
        List<Plan> records = page.getRecords();
        records.forEach(list -> {
            list.setPackageGb(FlowSizeConverterUtil.BytesToGb(list.getPackagee()));
            list.setTransferEnableGb(FlowSizeConverterUtil.BytesToGb(list.getTransferEnable()));
            list.setMonthsList(Arrays.stream(list.getMonths().split("-")).map(Integer::parseInt).collect(Collectors.toList()));
            list.setPriceList(Arrays.stream(list.getPrice().split("-")).map(BigDecimal::new).collect(Collectors.toList()));
        });
        map.put("data", records);
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return Result.ok().data("data", map);
    }
}