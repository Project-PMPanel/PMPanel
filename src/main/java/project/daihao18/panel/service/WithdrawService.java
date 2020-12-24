package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.Withdraw;

import java.util.Map;

/**
 * @InterfaceName: WithdrawService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:13
 */
public interface WithdrawService extends IService<Withdraw> {
    Map<String, Object> getWithdraw(Integer pageNo, Integer pageSize);
}