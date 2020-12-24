package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.Config;
import project.daihao18.panel.mapper.ConfigMapper;
import project.daihao18.panel.service.ConfigService;

/**
 * @ClassName: ConfigServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:14
 */
@Service
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements ConfigService {
    @Override
    @Cacheable(cacheNames = "panel::config", key = "#name", unless = "#result == null")
    public String getValueByName(String name) {
        QueryWrapper<Config> configQueryWrapper = new QueryWrapper<>();
        configQueryWrapper.eq("name", name);
        return this.getOne(configQueryWrapper).getValue();
    }

    @Override
    @CacheEvict(cacheNames = "panel::config", key = "#name")
    public boolean updateValueByName(String name, String value) {
        UpdateWrapper<Config> configUpdateWrapper = new UpdateWrapper<>();
        configUpdateWrapper
                .set("value", value)
                .eq("name", name);
        return this.update(configUpdateWrapper);
    }
}