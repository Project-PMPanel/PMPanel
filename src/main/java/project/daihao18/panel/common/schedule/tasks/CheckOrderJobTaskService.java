package project.daihao18.panel.common.schedule.tasks;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.common.enums.PayStatusEnum;
import project.daihao18.panel.common.payment.alipay.Alipay;
import project.daihao18.panel.common.utils.NotifyLockUtil;
import project.daihao18.panel.entity.CommonOrder;
import project.daihao18.panel.entity.Funds;
import project.daihao18.panel.entity.Order;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.service.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: CheckJobTaskService
 * @Description:
 * @Author: code18
 * @Date: 2020-11-12 10:53
 */
@Component
@Slf4j
public class CheckOrderJobTaskService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private Alipay alipay;

    @Autowired
    private UserService userService;

    @Autowired
    private FundsService fundsService;

    @Autowired
    private RedisService redisService;


    // 0 * * * * ?
    @Transactional
    public void checkOrderJob() throws AlipayApiException {
        synchronized (NotifyLockUtil.class) {
            // 判断是否开启的支付宝官方支付
            if ("alipay".equals(configService.getValueByName("alipay"))) {
                // 获取过去30m内的所有状态不为1的入站订单(已完成订单不要，已退款订单不要)
                List<Order> orders = orderService.getCheckedOrder();
                for (Order order : orders) {
                    // log.info("{}", order);
                    CommonOrder commonOrder = new CommonOrder();
                    commonOrder.setId(order.getOrderId());
                    AlipayTradeQueryResponse response = alipay.query(commonOrder);
                    if (ObjectUtil.isEmpty(response)) {
                        continue;
                    }
                    if (("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus()))) {
                        // 获取系统订单id,如果长度>15是order订单,反之是package订单
                        String id = response.getOutTradeNo();
                        Order existOrder = orderService.getOrderByOrderId(id.split("_")[0]);
                        if (PayStatusEnum.SUCCESS.getStatus().equals(existOrder.getStatus())) {
                            continue;
                        }
                        if (ObjectUtil.notEqual(id.split("_")[0], order.getOrderId())) {
                            continue;
                        }
                        log.info("交易成功,开始补单: {}", response.getOutTradeNo());
                        redisService.del("panel::user::" + order.getUserId());
                        // 获取是否混合支付信息
                        Boolean isMixedPay = "1".equals(id.split("_")[1]);
                        if (isMixedPay) {
                            existOrder.setIsMixedPay(true);
                            existOrder.setMixedMoneyAmount(existOrder.getPrice().subtract(new BigDecimal(response.getTotalAmount())));
                        } else {
                            existOrder.setIsMixedPay(false);
                            existOrder.setMixedMoneyAmount(BigDecimal.ZERO);
                        }
                        existOrder.setMixedPayAmount(new BigDecimal(response.getTotalAmount()));
                        // 查询用户当前套餐
                        Order currentPlan = orderService.getCurrentPlan(existOrder.getUserId());
                        // 查询是否新用户
                        Long buyCount = orderService.getBuyCountByUserId(existOrder.getUserId());
                        Boolean isNewPayer = buyCount == 0;
                        // 更新订单
                        if (orderService.updateFinishedOrder(existOrder.getIsMixedPay(), existOrder.getMixedMoneyAmount(), existOrder.getMixedPayAmount(), "支付宝", response.getBuyerUserId(), isNewPayer, response.getTradeNo(), response.getSendPayDate(), PayStatusEnum.SUCCESS.getStatus(), existOrder.getId())) {
                            userService.updateUserAfterBuyOrder(existOrder, ObjectUtil.isEmpty(currentPlan));
                            Date now = new Date();
                            // 给该用户新增资金明细表
                            Funds funds = new Funds();
                            funds.setUserId(existOrder.getUserId());
                            funds.setPrice(BigDecimal.ZERO.subtract(existOrder.getPrice()));
                            funds.setTime(now);
                            funds.setRelatedOrderId(existOrder.getOrderId());
                            funds.setContent(existOrder.getPlanDetailsMap().get("name").toString());
                            funds.setContentEnglish(existOrder.getPlanDetailsMap().get("nameEnglish").toString());
                            fundsService.save(funds);

                            // 如果有邀请人,给他加余额,并且给他新增一笔资金明细
                            User user = userService.getById(existOrder.getUserId());
                            if (ObjectUtil.isNotEmpty(user.getParentId())) {
                                User inviteUser = userService.getById(user.getParentId());
                                // 用户有等级的话,给返利
                                if (ObjectUtil.isNotEmpty(inviteUser) && inviteUser.getClazz() > 0) {
                                    redisService.del("panel::user::" + inviteUser.getId());
                                    BigDecimal commission = existOrder.getMixedPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP);
                                    // 判断是循环返利还是首次返利
                                    if (inviteUser.getInviteCycleEnable()) {
                                        log.info("id为{}的用户开始循环返利,原余额:{}, 返利后余额:{}", inviteUser.getId(), inviteUser.getMoney(), inviteUser.getMoney().add(commission));
                                        userService.handleCommission(inviteUser.getId(), commission);
                                    } else {
                                        // 首次返利,查该用户是否第一次充值
                                        Long count = fundsService.count(new QueryWrapper<Funds>().eq("user_id", user.getId()));
                                        if (count == 1) {
                                            // 首次
                                            log.info("id为{}的用户开始首次返利,原余额:{}, 返利后余额:{}", inviteUser.getId(), inviteUser.getMoney(), inviteUser.getMoney().add(commission));
                                            userService.handleCommission(inviteUser.getId(), commission);
                                        } else {
                                            return;
                                        }
                                    }
                                    // 给邀请人新增返利明细
                                    Funds inviteFund = new Funds();
                                    inviteFund.setUserId(inviteUser.getId());
                                    inviteFund.setPrice(commission);
                                    inviteFund.setTime(now);
                                    inviteFund.setRelatedOrderId(existOrder.getOrderId());
                                    inviteFund.setContent("佣金");
                                    inviteFund.setContentEnglish("Commission");
                                    fundsService.save(inviteFund);
                                    log.info("id为{}的用户获得返利{}元", inviteUser.getId(), inviteFund.getPrice());
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}