package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.Permission;

import java.util.List;

/**
 * @InterfaceName: PermissionService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:18
 */
public interface PermissionService extends IService<Permission> {
    List<Permission> getPermissionByRole(Integer isAdmin);
}