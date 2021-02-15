package project.daihao18.panel.service;

import com.alipay.api.AlipayApiException;
import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.entity.Order;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @InterfaceName: OrderService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:13
 */
public interface OrderService extends IService<Order> {

    Order getCurrentPlan(Integer userId);

    boolean deleteOrderByOrderId(String orderId);

    Order getOrderByOrderId(String orderId);

    List<Order> getExpiredFinishedOrder();

    void expiredFinishedOrder();

    List<Order> getFinishedOrder();

    int getBuyCountByUserId(Integer userId);

    boolean updateFinishedOrder(boolean isMixedPay, BigDecimal mixedMoneyAmount, BigDecimal mixedPayAmount, String payType, String payer, Boolean isNewPayer, String tradeNo, Date payTime, Integer status, Integer id);

    Integer getUsedCountByPlanId(Integer planId);

    Result getOrder(HttpServletRequest request);

    List<Order> getCheckedOrder() throws AlipayApiException;

    BigDecimal getMonthIncome();

    BigDecimal getTodayIncome();

    Object getTodayOrderCount();
}