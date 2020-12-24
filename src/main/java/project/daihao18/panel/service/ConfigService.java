package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.Config;

/**
 * @InterfaceName: ConfigService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:13
 */
public interface ConfigService extends IService<Config> {
    String getValueByName(String name);

    boolean updateValueByName(String name, String value);
}