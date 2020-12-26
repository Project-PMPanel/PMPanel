package project.daihao18.panel.controller;

import com.alipay.api.AlipayApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.response.ResultCodeEnum;
import project.daihao18.panel.common.utils.EmailUtil;
import project.daihao18.panel.common.utils.JwtTokenUtil;
import project.daihao18.panel.entity.Package;
import project.daihao18.panel.entity.*;
import project.daihao18.panel.service.UserService;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @ClassName: UserController
 * @Description:
 * @Author: code18
 * @Date: 2020-10-10 21:56
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Dashboard

    /**
     * 获取用户信息
     * @param request
     * @return
     */
    @GetMapping("/info")
    public Result getInfo(HttpServletRequest request) {
        User user = JwtTokenUtil.getUser(request);
        return Result.ok().data("user", user);
    }

    /**
     * 清理redis缓存
     * @param request
     * @return
     */
    @DeleteMapping("/info")
    public Result refreshInfo(HttpServletRequest request) {
        return userService.refreshInfo(JwtTokenUtil.getUser(request).getId());
    }

    /**
     * 获取公告
     * @return
     */
    @GetMapping("/announcement")
    public Result getAnnouncement() {
        return userService.getAnnouncement();
    }

    /**
     * 重置邀请码
     * @param request
     * @return
     */
    @PutMapping("/inviteCode")
    public Result resetInviteCode(HttpServletRequest request) {
        return userService.resetInviteCode(JwtTokenUtil.getUser(request).getId());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Plan

    /**
     * 获取可供用户订阅的套餐List
     * @return
     */
    @GetMapping("/plan")
    public Result getPlan(HttpServletRequest request) {
        return userService.getPlan(JwtTokenUtil.getId(request));
    }

    /**
     * 获取用户当前激活的套餐
     * @param request
     * @return
     */
    @GetMapping("/currentPlan")
    public Result getCurrentPlan(HttpServletRequest request) {
        return userService.getCurrentPlan(JwtTokenUtil.getId(request));
    }

    /**
     * 新增订单
     * @param request
     * @param order
     * @return
     */
    @PostMapping("/order")
    public Result buyPlan(HttpServletRequest request, @RequestBody Order order) {
        return userService.addOrder(JwtTokenUtil.getUser(request), order);
    }

    /**
     * 根据orderId删除订单
     * @param orderId
     * @return
     */
    @DeleteMapping("/order/{orderId}")
    public Result deleteOrderByOrderId(HttpServletRequest request, @PathVariable String orderId) {
        return userService.deleteOrderByOrderId(JwtTokenUtil.getId(request), orderId);
    }

    /**
     * 根据orderId取消订单
     * @param request
     * @param orderId
     * @return
     */
    @PutMapping("/order/{orderId}")
    public Result cancelOrderByOrderId(HttpServletRequest request, @PathVariable String orderId) {
        return userService.cancelOrderByOrderId(JwtTokenUtil.getId(request), orderId);
    }

    /**
     * 获取用户订单List
     * @param request
     * @return
     */
    @GetMapping("/order")
    public Result getOrder(HttpServletRequest request) {
        return userService.getOrder(request);
    }

    /**
     * 根据orderId查询订单
     * @return
     */
    @GetMapping("/order/{orderId}")
    public Result getOrderByOrderId(HttpServletRequest request, @PathVariable String orderId) {
        return userService.getOrderByOrderId(JwtTokenUtil.getUser(request).getId(), orderId);
    }

    /**
     * 新增续费订单
     * @param request
     * @param order
     * @return
     */
    @PostMapping("/renewOrder")
    public Result addRenewOrder(HttpServletRequest request, @RequestBody Order order) {
        return userService.addRenewOrder(JwtTokenUtil.getUser(request), order);
    }

    /**
     * 计算续费的价格与到期时间
     * @param request
     * @param params
     * @return
     */
    @GetMapping("/renewPlanInfo")
    public Result getChooseRenewPlanInfo(HttpServletRequest request, @RequestParam Map<String, Object> params) {
        Integer monthCount = Integer.parseInt(params.get("monthCount").toString());
        return userService.getChooseRenewPlanInfo(JwtTokenUtil.getUser(request), params.get("orderId").toString(), monthCount);
    }

    /**
     * 新增流量包订单
     * @param request
     * @param pack
     * @return
     */
    @PostMapping("/package")
    public Result buyPackage(HttpServletRequest request, @RequestBody Package pack) {
        return userService.addPackageOrder(JwtTokenUtil.getId(request), pack);
    }

    /**
     * 根据id删除流量包订单
     * @param id
     * @return
     */
    @DeleteMapping("/package/{id}")
    public Result deletePackageOrderById(HttpServletRequest request, @PathVariable Integer id) {
        return userService.deletePackageOrderById(JwtTokenUtil.getId(request), id);
    }

    /**
     * 根据id取消流量包订单
     * @param request
     * @param id
     * @return
     */
    @PutMapping("/package/{id}")
    public Result cancelPackageOrderById(HttpServletRequest request, @PathVariable Integer id) {
        return userService.cancelPackageOrderById(JwtTokenUtil.getId(request), id);
    }

    /**
     * 获取用户流量包订单List
     * @param request
     * @return
     */
    @GetMapping("/package")
    public Result getPackageOrder(HttpServletRequest request) {
        return userService.getPackageOrder(request);
    }

    /**
     * 根据type获取要支付的plan订单或者package订单
     * @param request
     * @param type
     * @param id
     * @return
     */
    @GetMapping("/order/{type}/{id}")
    public Result getOrderByTypeAndId(HttpServletRequest request, @PathVariable String type, @PathVariable String id) {
        if ("plan".equals(type)) {
            return userService.getOrderByOrderId(JwtTokenUtil.getId(request), id);
        } else if ("package".equals(type)) {
            return userService.getPackageOrderById(JwtTokenUtil.getId(request), Integer.parseInt(id));
        } else {
            return Result.setResult(ResultCodeEnum.PARAM_ERROR);
        }
    }

    /**
     * 获取配置的支付方式
     * @return
     */
    @GetMapping("/paymentConfig")
    public Result getPaymentConfig() {
        return userService.getPaymentConfig();
    }

    /**
     * 支付订单(余额支付和获取支付链接)
     * @param request
     * @param order
     * @return
     */
    @PostMapping("/payOrder")
    public Result payOrder(HttpServletRequest request, @RequestBody CommonOrder order) throws AlipayApiException {
        return userService.payOrder(request, order);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Funds

    /**
     * 获取用户资金明细
     * @param request
     * @return
     */
    @GetMapping("/funds")
    public Result getFunds(HttpServletRequest request) {
        return userService.getFunds(request);
    }


    /**
     * 提交提现申请
     * @param request
     * @param withdraw
     * @return
     */
    @PostMapping("/withdraw")
    public Result submitWithdraw(HttpServletRequest request, @RequestBody Withdraw withdraw) {
        return userService.submitWithdraw(JwtTokenUtil.getUser(request), withdraw);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Tutorial

    @GetMapping("/tutorial/{type}")
    public Result getTutorialsByType(@PathVariable String type) {
        return userService.getTutorialsByType(type);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Ticket

    /**
     * 分页查询工单
     * @param request
     * @return
     */
    @GetMapping("/ticket")
    public Result getTicket(HttpServletRequest request) {
        return userService.getTicket(request);
    }

    /**
     * 提交工单
     * @param request
     * @param ticket
     * @param type
     * @return
     */
    @PostMapping("/ticket/{type}")
    public Result saveTicket(HttpServletRequest request, @RequestBody Ticket ticket, @PathVariable String type) {
        return userService.saveTicket(JwtTokenUtil.getId(request), ticket, type);
    }

    /**
     * 根据id删除工单
     * @param request
     * @param id
     * @return
     */
    @DeleteMapping("/ticket/{id}")
    public Result deleteTicketById(HttpServletRequest request, @PathVariable Integer id) {
        return userService.deleteTicketById(JwtTokenUtil.getId(request), id);
    }

    /**
     * 根据id获取工单详情
     * @param request
     * @param id
     * @return
     */
    @GetMapping("/ticket/{id}")
    public Result getTicketById(HttpServletRequest request, @PathVariable Integer id) {
        return userService.getTicketById(JwtTokenUtil.getId(request), id);
    }

    /**
     * 关闭工单
     * @param request
     * @param id
     * @return
     */
    @PutMapping("/ticket/{id}")
    public Result closeTicket(HttpServletRequest request, @PathVariable Integer id) {
        return userService.closeTicket(JwtTokenUtil.getId(request), id);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Usercenter

    /**
     * 旧密码修改新密码
     * @param requestUser
     * @param request
     * @return
     */
    @PutMapping("/password")
    public Result changePass(@RequestBody User requestUser, HttpServletRequest request) {
        return userService.changePass(JwtTokenUtil.getUser(request), requestUser);
    }

    /**
     * 获取修改邮箱的验证码
     * @param request
     * @return
     * @throws MessagingException
     */
    @GetMapping("/getEmailCheckCode")
    public Result getEmailCheckCode(HttpServletRequest request) throws MessagingException {
        return EmailUtil.send(2, "验证邮件", null, true, request.getParameter("email"));
    }

    /**
     * 修改邮箱
     * @param user
     * @param request
     * @return
     */
    @PutMapping("/email")
    public Result changeEmail(@RequestBody User user, HttpServletRequest request) {
        User user1 = JwtTokenUtil.getUser(request);
        return userService.changeEmail(user.getCheckCode(), user.getNewCheckCode(), user1.getEmail(), user.getEmail(), user1.getId());
    }
}