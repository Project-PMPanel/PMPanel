package project.daihao18.panel.serviceImpl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.Oauth;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.mapper.OauthMapper;
import project.daihao18.panel.service.OauthService;
import project.daihao18.panel.service.UserService;

import java.util.List;

/**
 * @ClassName: OauthServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:14
 */
@Service
public class OauthServiceImpl extends ServiceImpl<OauthMapper, Oauth> implements OauthService {

    @Autowired
    private UserService userService;

    @Override
    public User getUser(String email) {
        QueryWrapper<Oauth> oauthQueryWrapper = new QueryWrapper<>();
        oauthQueryWrapper
                .eq("email", email)
                .eq("valid", 1);
        Oauth oauth = this.getOne(oauthQueryWrapper);
        if (ObjectUtil.isNotEmpty(oauth)) {
            // 用户存在
            return userService.getUserById(oauth.getUserId(), false);
        } else {
            return null;
        }
    }

    @Override
    public List<Oauth> getAllBindsByUId(Integer uid) {
        QueryWrapper<Oauth> oauthQueryWrapper = new QueryWrapper<>();
        oauthQueryWrapper
                .eq("user_id", uid)
                .eq("valid", 1);
        return this.list(oauthQueryWrapper);
    }

    @Override
    public boolean unBindAccount(Integer uid, String type) {
        UpdateWrapper<Oauth> oauthUpdateWrapper = new UpdateWrapper<>();
        oauthUpdateWrapper
                .set("valid", 0)
                .eq("user_id", uid)
                .eq("oauth_type", type);
        return this.update(oauthUpdateWrapper);
    }
}