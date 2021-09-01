package project.daihao18.panel.controller;

import com.alipay.api.AlipayApiException;
import lombok.extern.slf4j.Slf4j;
import net.ipip.ipdb.IPFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.utils.JwtTokenUtil;
import project.daihao18.panel.entity.*;
import project.daihao18.panel.service.AdminService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * @ClassName: AdminController
 * @Description:
 * @Author: code18
 * @Date: 2020-10-10 22:10
 */
@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    @Autowired
    private AdminService adminService;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Dashboard

    @GetMapping("/dashboard")
    public Result getDashboardInfo() {
        return adminService.getDashboardInfo();
    }

    @DeleteMapping("/cache")
    public Result cleanRedisCache() {
        return adminService.cleanRedisCache();
    }

    @GetMapping("/notifyRenew")
    public Result notifyRenew() {
        return adminService.notifyRenew();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Settings

    /**
     * 获取站点配置
     * @return
     */
    @GetMapping("/getSiteConfig")
    public Result getSiteConfig() {
        return adminService.getSiteConfig();
    }

    /**
     * 获取注册配置
     * @return
     */
    @GetMapping("/getRegisterConfig")
    public Result getRegisterConfig() {
        return adminService.getRegisterConfig();
    }

    /**
     * 获取支付配置
     * @return
     */
    @GetMapping("/getPaymentConfig")
    public Result getPaymentConfig() {
        return adminService.getPaymentConfig();
    }

    /**
     * 获取其他配置
     * @return
     */
    @GetMapping("/getOtherConfig")
    public Result getOtherConfig() {
        return adminService.getOtherConfig();
    }

    /**
     * 获取三方登录配置
     * @return
     */
    @GetMapping("/getOauthConfig")
    public Result getOauthConfig() {
        return adminService.getOauthConfig();
    }

    /**
     * 获取客户端配置
     * @return
     */
    @GetMapping("/getClientConfig")
    public Result getClientConfig() {
        return adminService.getClientConfig();
    }

    /**
     * 更新配置
     * @param config
     * @return
     */
    @PutMapping("/updateConfig")
    public Result updateConfigByName(@RequestBody Config config) throws AlipayApiException {
        return adminService.updateValueByName(config);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Node

    /**
     * 获取节点列表
     * @param request
     * @return
     */
    @GetMapping("/node/{type}")
    public Result getNode(HttpServletRequest request, @PathVariable("type") String type) {
        return adminService.getNode(request, type);
    }

    /**
     * 根据nodeId获取详情
     * @param nodeId
     * @return
     */
    @GetMapping("/node/{type}/{nodeId}")
    public Result getNodeInfoByTypeAndNodeId(HttpServletRequest request, @PathVariable String type, @PathVariable Integer nodeId) throws IOException, IPFormatException {
        return adminService.getNodeInfoByTypeAndNodeId(request, type, nodeId);
    }

    /**
     * 新增SS节点
     * @param ss
     * @return
     */
    @PostMapping("/node/ss")
    public Result addSsNode(@RequestBody Ss ss) {
        return adminService.addSsNode(ss);
    }

    /**
     * 修改ss节点
     * @param ss
     * @return
     */
    @PutMapping("/node/ss")
    public Result editSsNode(@RequestBody Ss ss) {
        return adminService.editSsNode(ss);
    }

    /**
     * 新增V2ray节点
     * @param v2ray
     * @return
     */
    @PostMapping("/node/v2ray")
    public Result addV2rayNode(@RequestBody V2ray v2ray) {
        return adminService.addV2rayNode(v2ray);
    }

    /**
     * 修改v2ray节点
     * @param v2ray
     * @return
     */
    @PutMapping("/node/v2ray")
    public Result editV2rayNode(@RequestBody V2ray v2ray) {
        return adminService.editV2rayNode(v2ray);
    }

    /**
     * 新增trojan节点
     * @param trojan
     * @return
     */
    @PostMapping("/node/trojan")
    public Result addTrojanNode(@RequestBody Trojan trojan) {
        return adminService.addTrojanNode(trojan);
    }

    /**
     * 修改trojan节点
     * @param trojan
     * @return
     */
    @PutMapping("/node/trojan")
    public Result editTrojanNode(@RequestBody Trojan trojan) {
        return adminService.editTrojanNode(trojan);
    }

    /**
     * 通过id删除node
     * @param id
     * @return
     */
    @DeleteMapping("/node/{type}/{id}")
    public Result deleteNodeByTypeAndId(@PathVariable String type, @PathVariable Integer id) {
        return adminService.deleteNodeByTypeAndId(type, id);
    }

    /**
     * 获取所有审计
     * @return
     */
    @GetMapping("/detects")
    public Result getAllDetects() {
        return adminService.getAllDetects();
    }

    /**
     * 获取Detect列表
     * @param request
     * @return
     */
    @GetMapping("/detect")
    public Result getDetect(HttpServletRequest request) {
        return adminService.getDetect(Integer.parseInt(request.getParameter("pageNo")), Integer.parseInt(request.getParameter("pageSize")));
    }

    /**
     * 新增detect
     * @param detectList
     * @return
     */
    @PostMapping("/detect")
    public Result addDetect(@RequestBody DetectList detectList) {
        return adminService.addDetect(detectList);
    }

    /**
     * 修改detect
     * @param detectList
     * @return
     */
    @PutMapping("/detect")
    public Result editDetect(@RequestBody DetectList detectList) {
        return adminService.editDetect(detectList);
    }

    /**
     * 通过id删除detect
     * @param id
     * @return
     */
    @DeleteMapping("/detect/{id}")
    public Result deleteDetectById(@PathVariable Integer id) {
        return adminService.deleteDetectById(id);
    }

    /**
     * 获取节点指定的审计List
     * @return
     */
    @GetMapping("/nodeWithDetect")
    public Result getNodeWithDetect(HttpServletRequest request) {
        return adminService.getNodeWithDetect(Integer.parseInt(request.getParameter("pageNo")), Integer.parseInt(request.getParameter("pageSize")));
    }

    /**
     * 添加节点指定的审计List
     * @return
     */
    @PostMapping("/nodeWithDetect")
    public Result addNodeWithDetect(@RequestBody Map<String, Object> map) {
        return adminService.addNodeWithDetect(map);
    }

    /**
     * 修改指定节点的审计
     * @param map
     * @return
     */
    @PutMapping("/nodeWithDetect")
    public Result editNodeWithDetect(@RequestBody Map<String, Object> map) {
        return adminService.editNodeWithDetect(map);
    }

    /**
     * 通过id删除nodeWithDetect
     * @param id
     * @return
     */
    @DeleteMapping("/nodeWithDetect/{id}")
    public Result deleteNodeWithDetectById(@PathVariable Integer id) {
        return adminService.deleteNodeWithDetectById(id);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   User

    /**
     * 带参数分页查询用户列表
     * @param request
     * @return
     */
    @GetMapping("/user")
    public Result getUser(HttpServletRequest request) {
        return adminService.getUser(request);
    }

    /**
     * 根据id查询用户
     * @param id
     * @return
     */
    @GetMapping("/user/{id}")
    public Result getUserDetail(@PathVariable Integer id) {
        return adminService.getUserDetail(id);
    }

    /**
     * 根据id更新用户
     * @param user
     * @return
     */
    @PutMapping("/user")
    public Result updateUserById(@RequestBody User user) {
        return adminService.updateUserById(user);
    }

    /**
     * 根据id删除用户
     * @param id
     * @return
     */
    @DeleteMapping("/user/{id}")
    public Result deleteUserById(@PathVariable Integer id) {
        return adminService.deleteUserById(id);
    }

    /**
     * 根据id重置连接密码
     * @param user
     * @return
     */
    @PutMapping("/user/passwd")
    public Result resetPasswdById(@RequestBody User user) {
        return adminService.resetPasswdById(user);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Plan

    /**
     * 分页查询套餐
     * @param request
     * @return
     */
    @GetMapping("/plan")
    public Result getPlan(HttpServletRequest request) {
        return adminService.getPlan(request);
    }

    /**
     * 添加新套餐
     * @param plan
     * @return
     */
    @PostMapping("/plan")
    public Result addPlan(@RequestBody Plan plan) {
        return adminService.addPlan(plan);
    }

    /**
     * 根据id修改套餐
     * @param plan
     * @return
     */
    @PutMapping("/plan")
    public Result updatePlanById(@RequestBody Plan plan) {
        return adminService.updatePlanById(plan);
    }

    /**
     * 根据id删除套餐
     * @param id
     * @return
     */
    @DeleteMapping("/plan/{id}")
    public Result deletePlanById(@PathVariable Integer id) {
        return adminService.deletePlanById(id);
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
        return adminService.getTicket(request);
    }

    /**
     * 回复工单
     * @param request
     * @param ticket
     * @param type
     * @return
     */
    @PostMapping("/ticket/{type}")
    public Result saveTicket(HttpServletRequest request, @RequestBody Ticket ticket, @PathVariable String type) {
        return adminService.saveTicket(JwtTokenUtil.getId(request), ticket, type);
    }

    /**
     * 根据id删除工单
     * @param id
     * @return
     */
    @DeleteMapping("/ticket/{id}")
    public Result deleteTicketById(@PathVariable Integer id) {
        return adminService.deleteTicketById(id);
    }

    /**
     * 根据id获取工单详情
     * @param id
     * @return
     */
    @GetMapping("/ticket/{id}")
    public Result getTicketById(@PathVariable Integer id) {
        return adminService.getTicketById(id);
    }

    /**
     * 关闭工单
     * @param id
     * @return
     */
    @PutMapping("/ticket/{id}")
    public Result closeTicket(@PathVariable Integer id) {
        return adminService.closeTicket(id);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Toturial

    /**
     * 分页查询教程
     * @param request
     * @return
     */
    @GetMapping("/tutorial")
    public Result getTutorial(HttpServletRequest request) {
        return adminService.getTutorial(request);
    }

    /**
     * 添加新教程
     * @param tutorial
     * @return
     */
    @PostMapping("/tutorial")
    public Result addTutorial(@RequestBody Tutorial tutorial) {
        return adminService.addTutorial(tutorial);
    }

    /**
     * 通过id更新教程
     * @param tutorial
     * @return
     */
    @PutMapping("/tutorial")
    public Result updateTutorialById(@RequestBody Tutorial tutorial) {
        return adminService.updateTutorialById(tutorial);
    }

    /**
     * 通过id删除教程
     * @param id
     * @return
     */
    @DeleteMapping("/tutorial/{id}")
    public Result deleteTutorialById(@PathVariable Integer id) {
        return adminService.deleteTutorialById(id);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Announcement

    /**
     * 获取最新的公告
     * @return
     */
    @GetMapping("/announcement")
    public Result getAnnouncement() {
        return adminService.getAnnouncement();
    }

    /**
     * 保存或新增公告,并修改为当前时间
     * @param announcement
     * @return
     */
    @PostMapping("/announcement")
    public Result saveOrUpdateAnnouncement(@RequestBody Announcement announcement) {
        return adminService.saveOrUpdateAnnouncement(announcement);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Order

    /**
     * 分页查询有效订单
     * @param request
     * @return
     */
    @GetMapping("/order")
    public Result getOrder(HttpServletRequest request) {
        return adminService.getOrder(request);
    }

    /**
     * 根据orderId查询订单
     * @param orderId
     * @return
     */
    @GetMapping("/order/{orderId}")
    public Result getOrderByOrderId(@PathVariable String orderId) {
        return adminService.getOrderByOrderId(orderId);
    }

    /**
     * 订单退款
     * @param orderId
     * @return
     */
    @PutMapping("/order/refund/{orderId}")
    public Result refundOrder(@PathVariable String orderId) throws AlipayApiException {
        return adminService.refundOrder(orderId);
    }

    /**
     * 管理员手动取消已支付订单(回滚用户状态)
     * @param orderId
     * @return
     */
    @PutMapping("/order/cancel/{orderId}")
    public Result cancelOrder(@PathVariable String orderId) {
        return adminService.cancelOrder(orderId);
    }

    /**
     * 管理员手动确认订单支付
     * @param orderId
     * @return
     */
    @PutMapping("/order/{orderId}")
    public Result confirmOrder(@PathVariable String orderId) {
        return adminService.confirmOrder(orderId);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Package

    /**
     * 分页查询有效流量包订单
     * @param request
     * @return
     */
    @GetMapping("/package")
    public Result getPackage(HttpServletRequest request) {
        return adminService.getPackage(request);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Commission

    /**
     * 分页查询返利明细
     * @param request
     * @return
     */
    @GetMapping("/commission")
    public Result getCommission(HttpServletRequest request) {
        return adminService.getCommission(Integer.parseInt(request.getParameter("pageNo")), Integer.parseInt(request.getParameter("pageSize")));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Withdraw

    /**
     * 分页查询提现单
     * @param request
     * @return
     */
    @GetMapping("/withdraw")
    public Result getWithdraw(HttpServletRequest request) {
        return adminService.getWithdraw(Integer.parseInt(request.getParameter("pageNo")), Integer.parseInt(request.getParameter("pageSize")));
    }

    /**
     * 确认提现到账
     * @param id
     * @return
     */
    @GetMapping("/withdraw/{id}")
    public Result ackWithdrawById(@PathVariable Integer id) {
        return adminService.ackWithdrawById(id);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////   Schedule

    /**
     * 新增定时任务
     * @param schedule
     * @return
     */
    @PostMapping("/cron/schedule")
    public Result addScheduleTask(@RequestBody Schedule schedule) {
        return adminService.addScheduleTask(schedule);
    }

    /**
     * 更新定时任务
     * @param schedule
     * @return
     */
    @PutMapping("/cron/schedule")
    public Result updateScheduleTask(@RequestBody Schedule schedule) {
        return adminService.updateScheduleTask(schedule);
    }

    /**
     * 根据id删除定时任务
     * @param id
     * @return
     */
    @DeleteMapping("/cron/schedule/{id}")
    public Result deleteScheduleTask(@PathVariable Integer id) {
        return adminService.deleteScheduleTask(id);
    }

    /**
     * 切换定时任务状态
     * @param schedule
     * @return
     */
    @PostMapping("/cron/schedule/toggle")
    public Result toggleScheduleTask(@RequestBody Schedule schedule) {
        return adminService.toggleScheduleTask(schedule);
    }

}