package project.daihao18.panel.common.payment.alipay;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipayLogger;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.ijpay.alipay.AliPayApiConfig;
import com.ijpay.alipay.AliPayApiConfigKit;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.common.utils.FlowSizeConverterUtil;
import project.daihao18.panel.entity.CommonOrder;
import project.daihao18.panel.entity.Order;
import project.daihao18.panel.service.ConfigService;
import project.daihao18.panel.service.OrderService;
import project.daihao18.panel.service.PackageService;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: Alipay
 * @Description:
 * @Author: code18
 * @Date: 2020-11-16 22:30
 */
@Data
@Slf4j
@Component
public class Alipay {

    private final static String ALIPAY_NOTIFY_URL = "/api/payment/notify/alipay";

    private final static String RETURN_URI = "/result/success";

    private static AlipayClient alipayClient;

    private Boolean isCertMode;

    @Autowired
    private ConfigService configService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PackageService packageService;

    @PostConstruct
    private void init() throws AlipayApiException {
        // 关闭alipay log
        AlipayLogger.setNeedEnableLogger(false);
        this.refreshAlipayConfig();
    }

    public void refreshAlipayConfig() throws AlipayApiException {
        // 初始化
        String alipay = configService.getValueByName("alipay");
        if ("alipay".equals(alipay)) {
            this.setAliPayApiConfig();
        }
    }

    protected void setAliPayApiConfig() throws AlipayApiException {
        // 获取支付宝配置
        Map<String, Object> map = JSONUtil.toBean(configService.getValueByName("alipayConfig"), Map.class);
        if (
                ObjectUtil.isEmpty(map.get("appId")) ||
                        ObjectUtil.isEmpty(map.get("pId")) ||
                        ObjectUtil.isEmpty(map.get("appPrivateKey")) ||
                        ObjectUtil.isEmpty(map.get("isCertMode")) ||
                        (ObjectUtil.isEmpty(map.get("alipayPublicKey")) && (ObjectUtil.isEmpty(map.get("appCertPath")) || ObjectUtil.isEmpty(map.get("alipayCertPath")) || ObjectUtil.isEmpty(map.get("alipayRootCertPath")))) ||
                        ObjectUtil.isEmpty(map.get("serverUrl")) ||
                        ObjectUtil.isEmpty(map.get("domain")) ||
                        ObjectUtil.isEmpty(map.get("web")) ||
                        ObjectUtil.isEmpty(map.get("wap")) ||
                        ObjectUtil.isEmpty(map.get("f2f"))
        ) {
            return;
        }
        AliPayApiConfig aliPayApiConfig = null;
        if (Boolean.parseBoolean(map.get("isCertMode").toString())) {
            aliPayApiConfig = AliPayApiConfig.builder()
                    .setAppId(map.get("appId").toString())
                    .setCharset("UTF-8")
                    .setPrivateKey(map.get("appPrivateKey").toString())
                    // .setAliPayPublicKey(map.get("alipayPublicKey").toString())
                    .setServiceUrl(map.get("serverUrl").toString())
                    .setSignType("RSA2")
                    // 证书模式
                    .build(map.get("appCertPath").toString(), map.get("alipayCertPath").toString(), map.get("alipayRootCertPath").toString());
            log.info("已设置为证书模式");
        } else {
            aliPayApiConfig = AliPayApiConfig.builder()
                    .setAppId(map.get("appId").toString())
                    .setCharset("UTF-8")
                    .setPrivateKey(map.get("appPrivateKey").toString())
                    .setAliPayPublicKey(map.get("alipayPublicKey").toString())
                    .setServiceUrl(map.get("serverUrl").toString())
                    .setSignType("RSA2")
                    // 密钥模式
                    .build();
            log.info("已设置为公钥模式");
        }
        this.isCertMode = (Boolean) map.get("isCertMode");
        AliPayApiConfigKit.setThreadLocalAliPayApiConfig(aliPayApiConfig);
        alipayClient = AliPayApiConfigKit.getAliPayApiConfig().getAliPayClient();
    }

    /**
     * 通用下单
     *
     * @param order
     * @return
     */
    @Transactional
    public Map<String, Object> create(CommonOrder order, Boolean isMixedPay) throws AlipayApiException {
        // 根据config配置的下单方式来下单
        // 获取支付宝配置
        Map<String, Object> alipayConfig = JSONUtil.toBean(configService.getValueByName("alipayConfig"), Map.class);
        Boolean web = (Boolean) alipayConfig.get("web");
        Boolean wap = (Boolean) alipayConfig.get("wap");
        Boolean f2f = (Boolean) alipayConfig.get("f2f");
        // 设置通知的domain(本站域名)
        order.setDomain(alipayConfig.get("domain").toString());
        // 获取订单标题
        String subject = configService.getValueByName("siteName");
        if ("plan".equals(order.getType())) {
            subject = subject.concat(" - " + orderService.getOrderByOrderId(order.getId()).getPlanDetailsMap().get("name").toString());
        } else {
            subject = subject.concat(" - " + FlowSizeConverterUtil.BytesToGb(packageService.getById(order.getId()).getTransferEnable()) + " GB");
        }
        // 通过请求的是pc 还是 h5来自动选择
        Map<String, Object> result = new HashMap<>();
        result.put("type", "alipay");
        if ("pc".equals(order.getPlatform())) {
            if (web) {
                // 优先选择web
                result = this.createByWEB(order, subject, isMixedPay);
                result.put("type", "link");
                return result;
            } else if (f2f) {
                // 没有web选择f2f
                result = this.createByF2F(order, subject, isMixedPay);
                result.put("type", "qr");
                return result;
            } else {
                // 都没启用直接返回null
                return null;
            }
        } else if ("h5".equals(order.getPlatform())) {
            if (wap) {
                // 优先选择wap
                result = this.createByWAP(order, subject, isMixedPay);
                result.put("type", "link");
                return result;
            } else if (f2f) {
                // 没有wap选择f2f
                result = this.createByF2F(order, subject, isMixedPay);
                result.put("type", "link");
                return result;
            } else {
                // 都没启用直接返回null
                return null;
            }
        }
        return null;
    }


    /**
     * 通过WEB下单
     *
     * @param order
     * @return
     */
    protected Map<String, Object> createByWEB(CommonOrder order, String subject, Boolean isMixedPay) throws AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setSubject(subject);
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        String isMixed = isMixedPay ? "1" : "0";
        model.setOutTradeNo(order.getId() + "_" + isMixed);
        model.setTimeoutExpress("5m");
        model.setTotalAmount(order.getMixedPayAmount().toString());
        model.setDisablePayChannels("credit_group");
        request.setBizModel(model);
        request.setNotifyUrl(order.getDomain() + ALIPAY_NOTIFY_URL);
        request.setReturnUrl(order.getDomain() + RETURN_URI);
        // 获取下单链接
        String url = alipayClient.pageExecute(request, "GET").getBody();
        Map<String, Object> map = new HashMap<>();
        map.put("url", url);
        return map;
    }

    /**
     * 通过WAP下单
     *
     * @param order
     * @return
     */
    protected Map<String, Object> createByWAP(CommonOrder order, String subject, Boolean isMixedPay) throws AlipayApiException {
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setSubject(subject);
        model.setProductCode("QUICK_WAP_WAY");
        String isMixed = isMixedPay ? "1" : "0";
        model.setOutTradeNo(order.getId() + "_" + isMixed);
        model.setTimeoutExpress("5m");
        model.setTotalAmount(order.getMixedPayAmount().toString());
        model.setDisablePayChannels("credit_group");
        request.setBizModel(model);
        request.setNotifyUrl(order.getDomain() + ALIPAY_NOTIFY_URL);
        request.setReturnUrl(order.getDomain() + RETURN_URI);
        // 获取下单链接
        String url = alipayClient.pageExecute(request, "GET").getBody();
        Map<String, Object> map = new HashMap<>();
        map.put("url", url);
        return map;
    }

    /**
     * 通过F2F下单
     *
     * @param order
     * @return
     */
    protected Map<String, Object> createByF2F(CommonOrder order, String subject, Boolean isMixedPay) throws AlipayApiException {
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setSubject(subject);
        String isMixed = isMixedPay ? "1" : "0";
        model.setOutTradeNo(order.getId() + "_" + isMixed);
        // model.setOutTradeNo(order.getId() + "_" + isMixed + "_" + RandomUtil.randomNumbers(4));
        model.setTimeoutExpress("5m");
        model.setTotalAmount(order.getMixedPayAmount().toString());
        model.setDisablePayChannels("credit_group");
        request.setBizModel(model);
        request.setNotifyUrl(order.getDomain() + ALIPAY_NOTIFY_URL);
        request.setReturnUrl(order.getDomain() + RETURN_URI);
        // 获取下单链接
        String url;
        if (isCertMode) {
            url = alipayClient.certificateExecute(request).getQrCode();
        } else {
            url = alipayClient.execute(request).getQrCode();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("url", url);
        return map;
    }

    /**
     * 查单接口
     *
     * @param order
     * @return
     * @throws AlipayApiException
     */
    public AlipayTradeQueryResponse query(Order order) throws AlipayApiException {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(order.getOrderId() + "_0");
        request.setBizModel(model);
        // 设置alipayClient
        // 查2次
        AlipayTradeQueryResponse execute = null;
        if (isCertMode) {
            execute = alipayClient.certificateExecute(request);
        } else {
            execute = alipayClient.execute(request);
        }
        if (!"40004".equals(execute.getCode())) {
            return execute;
        } else {
            model.setOutTradeNo(order.getOrderId() + "_1");
            request.setBizModel(model);
            if (isCertMode) {
                execute = alipayClient.certificateExecute(request);
            } else {
                execute = alipayClient.execute(request);
            }
            if (!"40004".equals(execute.getCode())) {
                return execute;
            }
        }
        return null;
    }
}