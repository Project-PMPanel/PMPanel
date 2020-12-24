package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.Permission;
import project.daihao18.panel.mapper.PermissionMapper;
import project.daihao18.panel.service.PermissionService;

import java.util.List;

/**
 * @ClassName: PermissionServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:26
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
    @Override
    @Cacheable(cacheNames = "panel::user::permission", key = "#isAdmin", unless = "#result == null")
    public List<Permission> getPermissionByRole(Integer isAdmin) {
        // 根据role查权限
        QueryWrapper<Permission> permissionQueryWrapper = new QueryWrapper<>();
        if (isAdmin == 0) {
            permissionQueryWrapper.eq("role", "user");
        } else if (isAdmin == 1) {
            permissionQueryWrapper.eq("role", "admin");
        }
        return this.list(permissionQueryWrapper);
    }
}