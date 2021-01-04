package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.common.enums.PayStatusEnum;
import project.daihao18.panel.common.payment.alipay.Alipay;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.entity.CommonOrder;
import project.daihao18.panel.entity.Order;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.mapper.OrderMapper;
import project.daihao18.panel.service.OrderService;
import project.daihao18.panel.service.UserService;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: OrderServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:14
 */
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private Alipay alipay;

    @Override
    public Order getCurrentPlan(Integer userId) {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper
                .eq("status", PayStatusEnum.SUCCESS.getStatus())
                .eq("user_id", userId)
                .orderByAsc("expire")
                .last("LIMIT 1");
        Order order = this.getOne(orderQueryWrapper);
        // json转换成map
        if (ObjectUtil.isNotEmpty(order)) {
            order.setUserDetailsMap(JSONUtil.toBean(order.getUserDetails(), Map.class));
            order.setPlanDetailsMap(JSONUtil.toBean(order.getPlanDetails(), Map.class));
            return order;
        } else {
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deleteOrderByOrderId(String orderId) {
        UpdateWrapper<Order> orderUpdateWrapper = new UpdateWrapper<>();
        orderUpdateWrapper.eq("order_id", orderId);
        return this.remove(orderUpdateWrapper);
    }

    @Override
    public Order getOrderByOrderId(String orderId) {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("order_id", orderId);
        Order order = this.getOne(orderQueryWrapper);
        // json转换成map
        if (ObjectUtil.isNotEmpty(order)) {
            order.setUserDetailsMap(JSONUtil.toBean(order.getUserDetails(), Map.class));
            Map<String, Object> planMap = JSONUtil.toBean(order.getPlanDetails(), Map.class);
            planMap.put("monthsList", Arrays.stream(planMap.get("months").toString().split("-")).map(Integer::parseInt).collect(Collectors.toList()));
            planMap.put("priceList", Arrays.stream(planMap.get("price").toString().split("-")).map(BigDecimal::new).collect(Collectors.toList()));
            // 根据套餐月数和设置的价格去计算该用户的过期时间和价格,并重新设置
            User user = userService.getById(order.getUserId());

            Result calcInfo;
            List<BigDecimal> priceList = new ArrayList<>();
            List<String> expireList = new ArrayList<>();
            for (Integer month : (ArrayList<Integer>) planMap.get("monthsList")) {
                calcInfo = userService.getChoosePlanInfo(user, planMap.get("months").toString(), planMap.get("price").toString(), month);
                priceList.add(new BigDecimal(calcInfo.getData().get("calcPrice").toString()));
                expireList.add(calcInfo.getData().get("calcExpire").toString());
            }
            planMap.put("priceList", priceList);
            planMap.put("expireList", expireList);
            order.setPlanDetailsMap(planMap);
            return order;
        } else {
            return null;
        }
    }

    @Override
    public List<Order> getExpiredFinishedOrder() {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper
                .eq("status", PayStatusEnum.SUCCESS.getStatus())
                .lt("expire", LocalDateTime.now());
        return this.list(orderQueryWrapper);
    }

    @Override
    @Transactional
    public void expiredFinishedOrder() {
        UpdateWrapper<Order> orderUpdateWrapper = new UpdateWrapper<>();
        orderUpdateWrapper
                .set("status", PayStatusEnum.INVALID.getStatus())
                .eq("status", PayStatusEnum.SUCCESS.getStatus())
                .lt("expire", LocalDateTime.now());
        this.update(orderUpdateWrapper);
    }

    @Override
    public List<Order> getFinishedOrder() {
        List<Order> finishedOrder = orderMapper.getFinishedOrder();
        if (ObjectUtil.isNotEmpty(finishedOrder)) {
            for (Order order : finishedOrder) {
                // json转换成map
                order.setUserDetailsMap(JSONUtil.toBean(order.getUserDetails(), Map.class));
                order.setPlanDetailsMap(JSONUtil.toBean(order.getPlanDetails(), Map.class));
            }
        }
        return finishedOrder;
    }

    @Override
    public int getBuyCountByUserId(Integer userId) {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("user_id", userId).in("status", 1, 3);
        return this.count(orderQueryWrapper);
    }

    @Override
    @Transactional
    public boolean updateFinishedOrder(boolean isMixedPay, BigDecimal mixedMoneyAmount, BigDecimal mixedPayAmount, String payType, String payer, Boolean isNewPayer, String tradeNo, Date payTime, Integer status, Integer id) {
        UpdateWrapper<Order> orderUpdateWrapper = new UpdateWrapper<>();
        orderUpdateWrapper
                .set("is_mixed_pay", isMixedPay)
                .set("mixed_money_amount", mixedMoneyAmount)
                .set("mixed_pay_amount", mixedPayAmount)
                .set("pay_type", payType)
                .set("payer", payer)
                .set("is_new_payer", isNewPayer)
                .set("trade_no", tradeNo)
                .set("pay_time", payTime)
                .set("status", status)
                .eq("id", id)
                .in("status", PayStatusEnum.WAIT_FOR_PAY.getStatus(), PayStatusEnum.CANCELED.getStatus());
        return this.update(orderUpdateWrapper);
    }

    @Override
    public Integer getUsedCountByPlanId(Integer planId) {
        // 查询当前套餐有多少正在使用的人
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<Order>()
                .eq("plan_id", planId)
                .eq("status", PayStatusEnum.WAIT_FOR_PAY.getStatus())
                .or()
                .eq("status", PayStatusEnum.SUCCESS.getStatus())
                .gt("expire", LocalDateTime.now());
        return this.count(orderQueryWrapper);
    }

    @Override
    public Result getOrder(HttpServletRequest request) {
        Integer pageNo = Integer.parseInt(request.getParameter("pageNo"));
        Integer pageSize = Integer.parseInt(request.getParameter("pageSize"));
        String orderId = request.getParameter("orderId");
        String userId = request.getParameter("userId");
        String status = request.getParameter("status");
        IPage<Order> page = new Page<>(pageNo, pageSize);
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.orderByDesc("create_time");
        if (ObjectUtil.isNotEmpty(orderId)) {
            orderQueryWrapper.eq("order_id", orderId);
        }
        if (ObjectUtil.isNotEmpty(userId)) {
            orderQueryWrapper.eq("user_id", Integer.parseInt(userId));
        }
        if (ObjectUtil.isNotEmpty(status)) {
            orderQueryWrapper.eq("status", Integer.parseInt(status));
        }
        page = this.page(page, orderQueryWrapper);
        List<Order> orders = page.getRecords();
        orders.forEach(order -> {
            // json转换成map
            if (ObjectUtil.isNotEmpty(order)) {
                order.setUserDetailsMap(JSONUtil.toBean(order.getUserDetails(), Map.class));
                order.setPlanDetailsMap(JSONUtil.toBean(order.getPlanDetails(), Map.class));
            }
        });
        Map<String, Object> map = new HashMap<>();
        map.put("data", orders);
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return Result.ok().data("data", map);
    }

    @Override
    public List<Order> getCheckedOrder() throws AlipayApiException {
        Date now = new Date();
        // 查5分钟前的订单
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.lt("create_time", DateUtil.offsetMinute(now, -5)).in("status", 0, 2);
        List<Order> orders = this.list(orderQueryWrapper);
        for (Order order : orders) {
            // 关闭支付宝订单
            CommonOrder commonOrder = new CommonOrder();
            commonOrder.setId(order.getOrderId());
            AlipayTradeCloseResponse close = alipay.close(commonOrder);
            log.debug("closeResponse: {}", close.toString());
        }
        // 关闭本地订单
        UpdateWrapper<Order> orderUpdateWrapper = new UpdateWrapper<>();
        orderUpdateWrapper.set("status", 2).lt("create_time", DateUtil.offsetMinute(now, -5)).eq("status", 0);
        this.update(orderUpdateWrapper);
        // 返回需要查询的订单
        orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper
                .eq("`status`", 0)
                .or()
                .eq("`status`", 2)
                .between("create_time", DateUtil.offsetMinute(now, -30), now);
        return this.list(orderQueryWrapper);
    }

    @Override
    public BigDecimal getMonthIncome() {
        Date now = new Date();
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper
                .select("sum(mixed_pay_amount) as total")
                .eq("status", 1)
                .gt("pay_time", DateUtil.beginOfMonth(now))
                .lt("pay_time", DateUtil.endOfMonth(now));
        Map<String, Object> map = this.getMap(orderQueryWrapper);
        return ObjectUtil.isNotEmpty(map) ? new BigDecimal(map.get("total").toString()).setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTodayIncome() {
        Date now = new Date();
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper
                .select("sum(mixed_pay_amount) as total")
                .eq("status", 1)
                .gt("pay_time", DateUtil.beginOfDay(now));
        Map<String, Object> map = this.getMap(orderQueryWrapper);
        return ObjectUtil.isNotEmpty(map) ? new BigDecimal(map.get("total").toString()).setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    @Override
    public Object getTodayOrderCount() {
        Date now = new Date();
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper
                .eq("status", 1)
                .gt("pay_time", DateUtil.beginOfDay(now));
        return this.count(orderQueryWrapper);
    }

    @Override
    public Object getTodayNewOrderCount() {
        Date now = new Date();
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper
                .eq("is_new_payer", 1)
                .eq("status", 1)
                .gt("pay_time", DateUtil.beginOfDay(now));
        return this.count(orderQueryWrapper);
    }
}