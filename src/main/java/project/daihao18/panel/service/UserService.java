package project.daihao18.panel.service;

import cn.hutool.core.date.DateTime;
import com.alipay.api.AlipayApiException;
import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.entity.Package;
import project.daihao18.panel.entity.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * @InterfaceName: UserService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:21
 */
public interface UserService extends IService<User> {

    User getById(Integer id);

    User getUserById(Integer id, Boolean originGetById);

    Integer getIdByEmail(String email);

    User getUserByInviteCode(String inviteCode);

    Result getSiteConfig();

    Result register(User user);

    Result findPass(User user);

    List<User> listNoMultiUser();

    boolean cleanExpiredUserData();

    Result refreshInfo(Integer userId);

    Result getAnnouncement();

    Result resetInviteCode(Integer userId);

    Result getPlan(Integer userId);

    Result getCurrentPlan(Integer userId);

    Result addOrder(User user, Order order);

    Result deleteOrderByOrderId(Integer userId, String orderId);

    Result cancelOrderByOrderId(Integer userId, String orderId);

    Result getOrder(HttpServletRequest request);

    Result getOrderByOrderId(Integer userId, String orderId);

    Result getChoosePlanInfo(User user, String monthString, String priceString, Integer monthCount);

    Result addRenewOrder(User user, Order order);

    Result getChooseRenewPlanInfo(User user, String orderId, Integer monthCount);

    Result addPackageOrder(Integer userId, Package pack);

    Result deletePackageOrderById(Integer userId, Integer id);

    Result cancelPackageOrderById(Integer userId, Integer id);

    Result getPackageOrder(HttpServletRequest request);

    Result getPackageOrderById(Integer userId, Integer id);

    Result getPaymentConfig();

    Result payOrder(HttpServletRequest request, CommonOrder order) throws AlipayApiException;

    boolean updateUserAfterBuyOrder(Order order, boolean isNewBuy);

    boolean updateUserAfterBuyPackageOrder(Order currentOrder, Package pack);

    Result getFunds(HttpServletRequest request);

    Result submitWithdraw(User user, Withdraw withdraw);

    User getMuUserByNodeServer(String port);

    boolean handleCommission(Integer userId, BigDecimal commission);

    Result getTutorialsByType(String type);

    Result getTicket(HttpServletRequest request);

    Result saveTicket(Integer userId, Ticket ticket, String type);

    Result deleteTicketById(Integer userId, Integer id);

    Result getTicketById(Integer userId, Integer id);

    Result closeTicket(Integer userId, Integer id);

    Result changePass(User user, User requestUser);

    Result changeEmail(String oldCheckCode, String newCheckCode, String oldEmail, String newEmail, Integer userId);

    Result getUserByPageAndQueryParam(HttpServletRequest request, Integer pageNo, Integer pageSize, boolean cacheFlag);

    Integer getRegisterCountByDateToNow(DateTime beginDate);

    List<User> getExpiredUser();

    Object getMonthPaidUserCount();

    Result getTrafficDetails(Integer userId);

    User getUserByUUID(String uuid);

    User getUserByTgId(Integer tgId);

    List<User> getAdmins();

    Result getTGConfig();

    Result unBindTG(Integer id);

    List<User> getTGUsers();
}