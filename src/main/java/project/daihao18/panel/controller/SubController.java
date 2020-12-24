package project.daihao18.panel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.daihao18.panel.service.SubService;

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
        }
        String fileName = type + "_" + System.currentTimeMillis() / 1000 + "." + suffix;
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        return subService.getSubs(link, type, request);
    }
}