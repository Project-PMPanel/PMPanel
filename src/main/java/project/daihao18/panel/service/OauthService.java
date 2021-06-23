package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.Oauth;
import project.daihao18.panel.entity.User;

import java.util.List;

/**
 * @InterfaceName: OauthService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:13
 */
public interface OauthService extends IService<Oauth> {
    User getUser(String email);

    List<Oauth> getAllBindsByUId(Integer uid);

    boolean unBindAccount(Integer uid, String type);
}