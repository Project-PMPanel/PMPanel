package project.daihao18.panel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ijpay.alipay.AliPayApi;
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
                // 获取系统订单id,如果长度>15是order订单,反之是package订单
                String id = params.get("out_trade_no");
                if (id.length() == 18 && id.startsWith("2020")) {
                    Order order = orderService.getOrderByOrderId(id.split("_")[0]);
                    if (PayStatusEnum.SUCCESS.getStatus().equals(order.getStatus())) {
                        return "success";
                    }
                    log.info("通知成功,开始处理: {}", params.get("out_trade_no"));
                    redisService.del("panel::user::" + order.getUserId());
                    // 获取是否混合支付信息
                    Boolean isMixedPay = "1".equals(id.split("_")[1]);
                    if (isMixedPay) {
                        order.setIsMixedPay(true);
                        order.setMixedMoneyAmount(order.getPrice().subtract(new BigDecimal(params.get("total_amount"))));
                    } else {
                        order.setIsMixedPay(false);
                        order.setMixedMoneyAmount(BigDecimal.ZERO);
                    }
                    order.setMixedPayAmount(new BigDecimal(params.get("total_amount")));
                    // 查询用户当前套餐
                    Order currentPlan = orderService.getCurrentPlan(order.getUserId());
                    // 查询是否新用户
                    int buyCount = orderService.getBuyCountByUserId(order.getUserId());
                    Boolean isNewPayer = buyCount == 0;
                    // 更新订单
                    if (orderService.updateFinishedOrder(order.getIsMixedPay(), order.getMixedMoneyAmount(), order.getMixedPayAmount(), "支付宝", params.get("buyer_id"), isNewPayer, params.get("trade_no"), DateUtil.parse(params.get("gmt_payment")), PayStatusEnum.SUCCESS.getStatus(), order.getId())) {
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
                                // 判断是循环返利还是首次返利
                                if (inviteUser.getInviteCycleEnable()) {
                                    userService.handleCommission(inviteUser.getId(), order.getMixedPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
                                } else {
                                    // 首次返利,查该用户是否第一次充值
                                    int count = fundsService.count(new QueryWrapper<Funds>().eq("user_id", user.getId()));
                                    if (count == 1) {
                                        // 首次
                                        userService.handleCommission(inviteUser.getId(), order.getMixedPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
                                    } else {
                                        return "success";
                                    }
                                }
                                // 给邀请人新增返利明细
                                Funds inviteFund = new Funds();
                                inviteFund.setUserId(inviteUser.getId());
                                inviteFund.setPrice(order.getMixedPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
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
                    Package pack = packageService.getById(id.split("_")[0]);
                    if (PayStatusEnum.SUCCESS.getStatus().equals(pack.getStatus())) {
                        return "success";
                    }
                    // 获取是否混合支付信息
                    Boolean isMixedPay = "1".equals(id.split("_")[1]);
                    if (isMixedPay) {
                        pack.setIsMixedPay(true);
                        pack.setMixedMoneyAmount(pack.getPrice().subtract(new BigDecimal(params.get("total_amount"))));
                    } else {
                        pack.setIsMixedPay(false);
                        pack.setMixedMoneyAmount(BigDecimal.ZERO);
                    }
                    pack.setMixedPayAmount(new BigDecimal(params.get("total_amount")));
                    // 更新流量包订单
                    if (packageService.updateFinishedPackageOrder(pack.getIsMixedPay(), pack.getMixedMoneyAmount(), pack.getMixedPayAmount(), "支付宝", DateUtil.parse(params.get("gmt_payment")), PayStatusEnum.SUCCESS.getStatus(), pack.getId())) {
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
}