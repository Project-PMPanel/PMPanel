package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.Funds;
import project.daihao18.panel.mapper.FundsMapper;
import project.daihao18.panel.service.ConfigService;
import project.daihao18.panel.service.FundsService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: FundsServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:14
 */
@Service
public class FundsServiceImpl extends ServiceImpl<FundsMapper, Funds> implements FundsService {

    @Autowired
    private ConfigService configService;

    @Override
    public Map<String, Object> getFundsByPage(Integer userId, Integer pageNo, Integer pageSize) {

        IPage<Funds> page = new Page<>(pageNo, pageSize);
        QueryWrapper<Funds> fundsQueryWrapper = new QueryWrapper<>();
        fundsQueryWrapper.eq("user_id", userId).orderByDesc("time");
        page = this.page(page, fundsQueryWrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("data", page.getRecords());
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        // 是否允许全局提现
        if (Boolean.parseBoolean(configService.getValueByName("enableWithdraw"))) {
            map.put("enableWithdraw", true);
            // 把提现费率放进去
            map.put("withdrawRate", new BigDecimal(configService.getValueByName("withdrawRate")).multiply(new BigDecimal("100")));
            // 把最低提现金额放进去
            map.put("minWithdraw", new BigDecimal(configService.getValueByName("minWithdraw")));
        } else {
            map.put("enableWithdraw", false);
        }
        return map;
    }

    @Override
    public Map<String, Object> getCommission(Integer pageNo, Integer pageSize) {
        IPage<Funds> page = new Page<>(pageNo, pageSize);
        QueryWrapper<Funds> fundsQueryWrapper = new QueryWrapper<>();
        fundsQueryWrapper.orderByDesc("time").gt("price", 0);
        page = this.page(page, fundsQueryWrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("data", page.getRecords());
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return map;
    }
}