package project.daihao18.panel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.internal.util.file.IOUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ijpay.alipay.AliPayApi;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.EventData;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.daihao18.panel.common.enums.PayStatusEnum;
import project.daihao18.panel.common.utils.NotifyLockUtil;
import project.daihao18.panel.entity.Funds;
import project.daihao18.panel.entity.Order;
import project.daihao18.panel.entity.Package;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.service.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * @ClassName: NotifyController
 * @Description:
 * @Author: code18
 * @Date: 2020-11-17 06:22
 */
@Slf4j
@RestController
@RequestMapping("/payment/notify")
public class NotifyController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private UserService userService;

    @Autowired
    private FundsService fundsService;

    @Autowired
    private RedisService redisService;

    /**
     * 支付宝通知
     *
     * @param request
     */
    @PostMapping("/alipay")
    @Transactional
    public String alipayNotify(HttpServletRequest request) throws AlipayApiException {
        synchronized (NotifyLockUtil.class) {
            // 获取支付配置
            Map<String, Object> alipayConfig = JSONUtil.toBean(configService.getValueByName("alipayConfig"), Map.class);

            // 获取支付宝POST过来反馈信息
            Map<String, String> params = AliPayApi.toMap(request);
            log.debug("支付宝回调参数{}", params);
            // 需要除去sign、sign_type两个参数，而sign已经在#rsaCheckV2方法中除去了
            params.remove("sign_type");

            // 根据证书还是密钥模式来验签
            boolean verifyResult = false;
            if ((Boolean) alipayConfig.get("isCertMode")) {
                verifyResult = AlipaySignature.rsaCertCheckV2(params, alipayConfig.get("alipayCertPath").toString(), "utf-8", "RSA2");
            } else {
                verifyResult = AlipaySignature.rsaCheckV2(params, alipayConfig.get("alipayPublicKey").toString(), "utf-8", "RSA2");
            }
            if (!verifyResult) {
                log.info("支付宝主动通知:{}验签失败", params.get("out_trade_no"));
                return "fail";
            }
            // 这里加上商户的业务逻辑程序代码 异步通知可能出现订单重复通知 需要做去重处理
            if ("TRADE_SUCCESS".equals(params.get("trade_status")) || "TRADE_FINISHED".equals(params.get("trade_status"))) {
                String id = params.get("out_trade_no");
                if (id.startsWith("p_")) {
                    Order order = orderService.getOrderByOrderId(id.split("_")[1]);
                    if (PayStatusEnum.SUCCESS.getStatus().equals(order.getStatus())) {
                        return "success";
                    }
                    if (ObjectUtil.notEqual(id.split("_")[1], order.getOrderId())) {
                        return "fail";
                    }
                    log.info("通知成功,开始处理: {}", params.get("out_trade_no"));
                    redisService.del("panel::user::" + order.getUserId());
                    order.setPayAmount(new BigDecimal(params.get("total_amount")));
                    // 查询用户当前套餐
                    Order currentPlan = orderService.getCurrentPlan(order.getUserId());
                    // 查询是否新用户
                    Long buyCount = orderService.getBuyCountByUserId(order.getUserId());
                    Boolean isNewPayer = buyCount == 0;
                    // 更新订单
                    if (orderService.updateFinishedOrder(order.getPayAmount(), "支付宝", params.get("buyer_id"), isNewPayer, params.get("trade_no"), DateUtil.parse(params.get("gmt_payment")), PayStatusEnum.SUCCESS.getStatus(), order.getId())) {
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
                                BigDecimal commission = order.getPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP);
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
                                inviteFund.setPrice(order.getPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
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
                    Package pack = packageService.getById(id.split("_")[1]);
                    if (PayStatusEnum.SUCCESS.getStatus().equals(pack.getStatus())) {
                        return "success";
                    }
                    if (ObjectUtil.notEqual(id.split("_")[1], pack.getId())) {
                        return "fail";
                    }
                    log.info("通知成功,开始处理: {}", params.get("out_trade_no"));
                    redisService.del("panel::user::" + pack.getUserId());
                    pack.setPayAmount(new BigDecimal(params.get("total_amount")));
                    // 更新流量包订单
                    if (packageService.updateFinishedPackageOrder(pack.getPayAmount(), "支付宝", params.get("buyer_id"), DateUtil.parse(params.get("gmt_payment")), PayStatusEnum.SUCCESS.getStatus(), pack.getId())) {
                        pack.setPayType("支付宝");
                        // 查询用户当前套餐
                        Order currentOrder = orderService.getCurrentPlan(pack.getUserId());
                        userService.updateUserAfterBuyPackageOrder(currentOrder, pack);
                        // 新增资金明细表
                        Funds funds = new Funds();
                        funds.setUserId(pack.getUserId());
                        funds.setPrice(BigDecimal.ZERO.subtract(currentOrder.getPrice()));
                        funds.setTime(new Date());
                        funds.setRelatedOrderId(pack.getOrderId());
                        funds.setContent("流量包-" + currentOrder.getPlanDetailsMap().get("transferEnable").toString() + "GB");
                        funds.setContentEnglish("Package-" + currentOrder.getPlanDetailsMap().get("transferEnable").toString() + "GB");
                        fundsService.save(funds);
                        return "success";
                    }
                }
            }
            return "fail";
        }
    }

    /**
     * stripe回调
     *
     * @return
     */
    @PostMapping("/stripe")
    @Transactional
    public String stripeNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            // 获取stripe配置
            Map<String, Object> stripeConfig = JSONUtil.toBean(configService.getValueByName("stripeConfig"), Map.class);
            Stripe.apiKey = stripeConfig.get("sk_live").toString();

            String endpointSecret = stripeConfig.get("webhook_secret").toString();
            String payload = new String(IOUtils.toByteArray(request.getInputStream()), "UTF-8");
            String sigHeader = request.getHeader("Stripe-Signature");

            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            log.debug("stripe event{}", event.toJson());
            String stripeType = event.getType();
            if (stripeType.equals("payment_intent.succeeded")) {
                EventData eventData = event.getData();
                Map<String, Object> params = JSONUtil.toBean(JSONUtil.toBean(eventData.toJson(), Map.class).get("object").toString(), Map.class);
                log.debug("stripe payment_intent.succeeded 回调参数{}", params);
                String id = params.get("statement_descriptor").toString();
                if (id.startsWith("p_")) {
                    Order order = orderService.getOrderByOrderId(id.split("_")[1]);
                    if (PayStatusEnum.SUCCESS.getStatus().equals(order.getStatus())) {
                        return "success";
                    }
                    if (ObjectUtil.notEqual(id.split("_")[1], order.getOrderId())) {
                        return "fail";
                    }
                    log.info("通知成功,开始处理: {}", params.get("statement_descriptor"));
                    redisService.del("panel::user::" + order.getUserId());
                    order.setPayAmount(new BigDecimal(params.get("amount").toString()).divide(new BigDecimal("100")));
                    // 查询用户当前套餐
                    Order currentPlan = orderService.getCurrentPlan(order.getUserId());
                    // 查询是否新用户
                    Long buyCount = orderService.getBuyCountByUserId(order.getUserId());
                    Boolean isNewPayer = buyCount == 0;
                    // 更新订单
                    if (orderService.updateFinishedOrder(order.getPayAmount(), "支付宝", null, isNewPayer, params.get("id").toString(), DateUtil.date(), PayStatusEnum.SUCCESS.getStatus(), order.getId())) {
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
                                BigDecimal commission = order.getPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP);
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
                                inviteFund.setPrice(order.getPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
                                inviteFund.setTime(now);
                                inviteFund.setRelatedOrderId(order.getOrderId());
                                inviteFund.setContent("佣金");
                                inviteFund.setContentEnglish("Commission");
                                fundsService.save(inviteFund);
                                log.info("id为{}的用户获得返利{}元", inviteUser.getId(), inviteFund.getPrice());
                            }
                        }
                        response.setStatus(200);
                        return "success";
                    }
                } else {
                    Package pack = packageService.getById(id.split("_")[1]);
                    if (PayStatusEnum.SUCCESS.getStatus().equals(pack.getStatus())) {
                        return "success";
                    }
                    if (ObjectUtil.notEqual(id.split("_")[1], pack.getId())) {
                        return "fail";
                    }
                    log.info("通知成功,开始处理: {}", params.get("out_trade_no"));
                    redisService.del("panel::user::" + pack.getUserId());
                    pack.setPayAmount(new BigDecimal(params.get("amount").toString()).divide(new BigDecimal("100")));
                    // 更新流量包订单
                    if (packageService.updateFinishedPackageOrder(pack.getPayAmount(), "支付宝", null, DateUtil.date(), PayStatusEnum.SUCCESS.getStatus(), pack.getId())) {
                        pack.setPayType("支付宝");
                        // 查询用户当前套餐
                        Order currentOrder = orderService.getCurrentPlan(pack.getUserId());
                        userService.updateUserAfterBuyPackageOrder(currentOrder, pack);
                        // 新增资金明细表
                        Funds funds = new Funds();
                        funds.setUserId(pack.getUserId());
                        funds.setPrice(BigDecimal.ZERO.subtract(currentOrder.getPrice()));
                        funds.setTime(new Date());
                        funds.setRelatedOrderId(pack.getOrderId());
                        funds.setContent("流量包-" + currentOrder.getPlanDetailsMap().get("transferEnable").toString() + "GB");
                        funds.setContentEnglish("Package-" + currentOrder.getPlanDetailsMap().get("transferEnable").toString() + "GB");
                        fundsService.save(funds);
                        response.setStatus(200);
                        return "success";
                    }
                }
            }
            response.setStatus(500);
            return "fail";
        } catch (Exception e) {
            response.setStatus(500);
            return "fail";
        }
    }
}