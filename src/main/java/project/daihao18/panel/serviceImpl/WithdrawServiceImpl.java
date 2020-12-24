package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.Withdraw;
import project.daihao18.panel.mapper.WithdrawMapper;
import project.daihao18.panel.service.WithdrawService;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: WithdrawServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:14
 */
@Service
public class WithdrawServiceImpl extends ServiceImpl<WithdrawMapper, Withdraw> implements WithdrawService {
    @Override
    public Map<String, Object> getWithdraw(Integer pageNo, Integer pageSize) {
        IPage<Withdraw> page = new Page<>(pageNo, pageSize);
        QueryWrapper<Withdraw> withdrawQueryWrapper = new QueryWrapper<>();
        withdrawQueryWrapper.orderByDesc("create_time");
        page = this.page(page, withdrawQueryWrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("data", page.getRecords());
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return map;
    }
}