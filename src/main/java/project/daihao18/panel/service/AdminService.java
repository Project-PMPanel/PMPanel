package project.daihao18.panel.service;

import com.alipay.api.AlipayApiException;
import net.ipip.ipdb.IPFormatException;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.entity.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * @InterfaceName: AdminService
 * @Description:
 * @Author: code18
 * @Date: 2020-11-28 15:32
 */
public interface AdminService {
    Result getDashboardInfo();

    Result cleanRedisCache();

    Result notifyRenew();

    Result getSiteConfig();

    Result getRegisterConfig();

    Result getPaymentConfig();

    Result getOtherConfig();

    Result getOauthConfig();

    Result getClientConfig();

    Result updateValueByName(Config config) throws AlipayApiException;

    Result getNode(HttpServletRequest request, String type);

    Result getNodeInfoByTypeAndNodeId(HttpServletRequest request, String type, Integer nodeId) throws IOException, IPFormatException;

    Result addSsNode(Ss ss);

    Result editSsNode(Ss ss);

    Result addV2rayNode(V2ray v2ray);

    Result editV2rayNode(V2ray v2ray);

    Result addTrojanNode(Trojan trojan);

    Result editTrojanNode(Trojan trojan);

    Result deleteNodeByTypeAndId(String type, Integer id);

    Result getAllDetects();

    Result getDetect(Integer pageNo, Integer pageSize);

    Result addDetect(DetectList detectList);

    Result editDetect(DetectList detectList);

    Result deleteDetectById(Integer id);

    Result getNodeWithDetect(Integer pageNo, Integer pageSize);

    Result addNodeWithDetect(Map<String, Object> map);

    Result editNodeWithDetect(Map<String, Object> map);

    Result deleteNodeWithDetectById(Integer id);

    Result getUser(HttpServletRequest request);

    Result getUserDetail(Integer id);

    Result updateUserById(User user);

    Result deleteUserById(Integer id);

    Result resetPasswdById(User user);

    Result getPlan(HttpServletRequest request);

    Result addPlan(Plan plan);

    Result updatePlanById(Plan plan);

    Result deletePlanById(Integer id);

    Result getTicket(HttpServletRequest request);

    Result saveTicket(Integer userId, Ticket ticket, String type);

    Result deleteTicketById(Integer id);

    Result getTicketById(Integer id);

    Result closeTicket(Integer id);

    Result getTutorial(HttpServletRequest request);

    Result addTutorial(Tutorial tutorial);

    Result updateTutorialById(Tutorial tutorial);

    Result deleteTutorialById(Integer id);

    Result getAnnouncement();

    Result saveOrUpdateAnnouncement(Announcement announcement);

    Result getOrder(HttpServletRequest request);

    Result getOrderByOrderId(String orderId);

    Result refundOrder(String orderId) throws AlipayApiException;

    Result cancelOrder(String orderId);

    Result confirmOrder(String orderId);

    Result getCommission(Integer pageNo, Integer pageSize);

    Result getWithdraw(Integer pageNo, Integer pageSize);

    Result ackWithdrawById(Integer id);

    Result addScheduleTask(Schedule schedule);

    Result updateScheduleTask(Schedule schedule);

    Result deleteScheduleTask(Integer id);

    Result toggleScheduleTask(Schedule schedule);
}