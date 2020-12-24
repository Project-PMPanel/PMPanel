package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.entity.Plan;

import java.util.List;

/**
 * @InterfaceName: PlanService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:16
 */
public interface PlanService extends IService<Plan> {
    List<Plan> getPlan(Integer userId);

    Result getPlan(Integer pageNo, Integer pageSize);
}