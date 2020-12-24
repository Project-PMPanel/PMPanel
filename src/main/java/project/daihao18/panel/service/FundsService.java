package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.Funds;

import java.util.Map;

/**
 * @InterfaceName: FundsService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:13
 */
public interface FundsService extends IService<Funds> {
    Map<String, Object> getFundsByPage(Integer userId, Integer pageNo, Integer pageSize);

    Map<String, Object> getCommission(Integer pageNo, Integer pageSize);
}