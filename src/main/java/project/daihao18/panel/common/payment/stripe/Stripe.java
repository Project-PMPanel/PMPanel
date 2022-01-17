package project.daihao18.panel.common.payment.stripe;

import cn.hutool.json.JSONUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import project.daihao18.panel.entity.CommonOrder;
import project.daihao18.panel.service.ConfigService;
import project.daihao18.panel.service.PackageService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
@Component
public class Stripe {

    @Autowired
    private ConfigService configService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 通用下单
     *
     * @param order
     * @return
     */
    @Transactional
    public Map<String, Object> create(CommonOrder order) throws StripeException {
        // 获取stripe配置
        Map<String, Object> stripeConfig = JSONUtil.toBean(configService.getValueByName("stripeConfig"), Map.class);
        // 获取订单标题
        // String subject = "Order#";
        // 大写开头为混合支付
        if ("plan".equals(order.getType())) {
            // subject = subject.concat(order.getId());
            order.setId("p_".concat(order.getId()));
        } else {
            // subject = subject.concat(FlowSizeConverterUtil.BytesToGb(packageService.getById(order.getId()).getTransferEnable()) + " GB");
            order.setId("t_".concat(order.getId()));
        }
        com.stripe.Stripe.apiKey = stripeConfig.get("sk_live").toString();
        PaymentMethodCreateParams paymentMethodCreateParams = PaymentMethodCreateParams.builder()
                .setType(PaymentMethodCreateParams.Type.ALIPAY).build();
        PaymentMethod paymentMethod = PaymentMethod.create(paymentMethodCreateParams);
        PaymentIntentCreateParams paymentIntentCreateParams = PaymentIntentCreateParams.builder()
                .addPaymentMethodType("alipay")
                .setAmount(order.getPayAmount().multiply(new BigDecimal("100")).longValue())
                .setCurrency(stripeConfig.get("currency").toString().toUpperCase())
                .setStatementDescriptor(order.getId())
                .putMetadata("out_trade_no", order.getId())
                .setPaymentMethod(paymentMethod.getId())
                .build();
        PaymentIntent create = PaymentIntent.create(paymentIntentCreateParams);

        PaymentIntent resource = PaymentIntent.retrieve(create.getId());
        PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                .setPaymentMethod(create.getPaymentMethod())
                .setReturnUrl(stripeConfig.get("returnUrl").toString())
                .build();
        resource.setLivemode(true);
        PaymentIntent paymentIntent = resource.confirm(confirmParams);
        String url = paymentIntent.getNextAction().getAlipayHandleRedirect().getUrl();
        Map<String, Object> map = new HashMap<>();
        map.put("url", url);
        map.put("type", "link");
        return map;
    }
}
