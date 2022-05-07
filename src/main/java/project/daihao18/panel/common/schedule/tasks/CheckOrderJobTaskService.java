package project.daihao18.panel.common.schedule.tasks;

import cn.hutool.core.date.DateUtil;
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
import project.daihao18.panel.entity.Package;
import project.daihao18.panel.entity.*;
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
    private PackageService packageService;

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
                    commonOrder.setType("plan");
                    AlipayTradeQueryResponse response = alipay.query(commonOrder);
                    if (ObjectUtil.isEmpty(response)) {
                        continue;
                    }
                    if (("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus()))) {
                        handleSuccessfulNotify(response.getOutTradeNo(), response.getTradeNo(), response.getBuyerUserId());
                    }
                }
                List<Package> packages = packageService.getCheckedPackage();
                for (Package pack : packages) {
                    CommonOrder commonOrder = new CommonOrder();
                    commonOrder.setId(pack.getId().toString());
                    commonOrder.setType("package");
                    AlipayTradeQueryResponse response = alipay.query(commonOrder);
                    if (ObjectUtil.isEmpty(response)) {
                        continue;
                    }
                    if (("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus()))) {
                        handleSuccessfulNotify(response.getOutTradeNo(), response.getTradeNo(), response.getBuyerUserId());
                    }
                }
            }
        }
    }

    /**
     * 验签后必须提供系统订单号（p_或者t_开头）、支付提供方订单号、payer可选
     * 默认成功返回success，如需自定义返回，请调用此方法后，自行判断是否为success后再做自定义返回
     *
     * @param outTradeNo
     * @param tradeNo
     * @param payer
     * @return
     */
    private String handleSuccessfulNotify(String outTradeNo, String tradeNo, String payer) {
        if (outTradeNo.startsWith("p_")) {
            Order order = orderService.getOrderByOrderId(outTradeNo.split("_")[1]);
            if (PayStatusEnum.SUCCESS.getStatus().equals(order.getStatus())) {
                return "success";
            }
            if (ObjectUtil.notEqual(outTradeNo.split("_")[1], order.getOrderId())) {
                return "fail";
            }
            log.info("通知成功,开始处理: {}", order.getOrderId());
            redisService.del("panel::user::" + order.getUserId());
            // 查询用户当前套餐
            Order currentPlan = orderService.getCurrentPlan(order.getUserId());
            // 查询是否新用户
            Long buyCount = orderService.getBuyCountByUserId(order.getUserId());
            Boolean isNewPayer = buyCount == 0;
            // 更新订单
            if (orderService.updateFinishedOrder(order.getPrice(), "支付宝", payer, isNewPayer, tradeNo, DateUtil.date(), PayStatusEnum.SUCCESS.getStatus(), order.getId())) {
                order.setPayType("支付宝");
                userService.updateUserAfterBuyOrder(order, ObjectUtil.isEmpty(currentPlan));
                Date now = new Date();
                // 给该用户新增资金明细表
                Funds funds = new Funds();
                funds.setUserId(order.getUserId());
                funds.setPrice(BigDecimal.ZERO.subtract(order.getPrice()));
                funds.setTime(now);
                funds.setRelatedOrderId(order.getOrderId());
                funds.setContent(order.getPlanDetailsMap().get("name").toString());
                funds.setContentEnglish(order.getPlanDetailsMap().get("nameEnglish").toString());
                fundsService.save(funds);

                // 如果有邀请人,给他加余额,并且给他新增一笔资金明细
                User user = userService.getById(order.getUserId());
                if (ObjectUtil.isNotEmpty(user.getParentId())) {
                    User inviteUser = userService.getById(user.getParentId());
                    // 用户有等级的话,给返利
                    if (ObjectUtil.isNotEmpty(inviteUser) && inviteUser.getClazz() > 0) {
                        redisService.del("panel::user::" + inviteUser.getId());
                        BigDecimal commission = order.getPrice().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP);
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
                                return "success";
                            }
                        }
                        // 给邀请人新增返利明细
                        Funds inviteFund = new Funds();
                        inviteFund.setUserId(inviteUser.getId());
                        inviteFund.setPrice(order.getPrice().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
                        inviteFund.setTime(now);
                        inviteFund.setRelatedOrderId(order.getOrderId());
                        inviteFund.setContent("佣金");
                        inviteFund.setContentEnglish("Commission");
                        fundsService.save(inviteFund);
                        log.info("id为{}的用户获得返利{}元", inviteUser.getId(), inviteFund.getPrice());
                    }
                }
                return "success";
            }
        } else {
            Package pack = packageService.getById(outTradeNo.split("_")[1]);
            if (PayStatusEnum.SUCCESS.getStatus().equals(pack.getStatus())) {
                return "success";
            }
            if (ObjectUtil.notEqual(Integer.parseInt(outTradeNo.split("_")[1]), pack.getId())) {
                return "fail";
            }
            log.info("通知成功,开始处理: {}", outTradeNo);
            redisService.del("panel::user::" + pack.getUserId());
            // 更新流量包订单
            if (packageService.updateFinishedPackageOrder(pack.getPrice(), "支付宝", payer, DateUtil.date(), PayStatusEnum.SUCCESS.getStatus(), pack.getId())) {
                pack.setPayType("支付宝");
                // 查询用户当前套餐
                Order currentOrder = orderService.getCurrentPlan(pack.getUserId());
                userService.updateUserAfterBuyPackageOrder(currentOrder, pack);
                // 新增资金明细表
                Funds funds = new Funds();
                funds.setUserId(pack.getUserId());
                funds.setPrice(BigDecimal.ZERO.subtract(pack.getPrice()));
                funds.setTime(new Date());
                funds.setRelatedOrderId(pack.getOrderId());
                funds.setContent("流量包-" + pack.getPrice().multiply(new BigDecimal(currentOrder.getPlanDetailsMap().get("packagee").toString())) + "GB");
                funds.setContentEnglish("Package-" + pack.getPrice().multiply(new BigDecimal(currentOrder.getPlanDetailsMap().get("packagee").toString())) + "GB");
                fundsService.save(funds);
                return "success";
            }
        }
        return "fail";
    }
}