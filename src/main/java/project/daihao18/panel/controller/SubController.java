package project.daihao18.panel.controller;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.daihao18.panel.common.utils.CommonUtil;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.service.SubService;
import project.daihao18.panel.service.UserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @ClassName: SubController
 * @Description:
 * @Author: code18
 * @Date: 2020-11-23 13:56
 */
@RestController
@RequestMapping("/subscription")
public class SubController {

    @Autowired
    private SubService subService;

    @Resource
    private UserService userService;

    /**
     * 根据link & type获取订阅内容
     *
     * @param link
     * @param type
     * @param response
     * @return
     */
    @GetMapping(value = "/{link}/{type}", produces = "text/plain")
    public String getSubs(@PathVariable("link") String link, @PathVariable("type") String type, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String suffix = "txt";
        switch (type) {
            case "shadowrocket":
                suffix = "txt";
                break;
            case "clash":
                suffix = "yaml";
                break;
            case "surge4":
                suffix = "conf";
                break;
            case "sip002":
                suffix = "txt";
                break;
            case "sip008":
                suffix = "json";
                break;
        }
        // 如果订阅是文件类型则下载,否则直接将文本输出到浏览器页面
        if ("shadowrocket".equals(type) || "clash".equals(type) || "surge4".equals(type) || "ss".equals(type)) {
            String fileName = type + "_" + System.currentTimeMillis() / 1000 + "." + suffix;
            response.setHeader("content-disposition", "attachment; filename=" + fileName);
            response.setHeader("profile-update-interval", "12");
            User user = userService.getById(CommonUtil.subsDecryptId(link));
            // 无该用户 或者 该用户link不相等 或者 该用户被封禁
            if (!(ObjectUtil.isEmpty(user) || !user.getLink().equals(CommonUtil.subsLinkDecryptId(link)) || !user.getEnable())) {
                response.setHeader("subscription-userinfo", "upload=" + user.getU() + "; download=" + user.getD() + "; total=" + user.getTransferEnable() + "; expire=" + user.getExpireIn().getTime() / 1000);
            }
        }
        return subService.getSubs(link, type, request);
    }
}