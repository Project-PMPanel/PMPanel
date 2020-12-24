package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.entity.Tutorial;

import java.util.List;

/**
 * @InterfaceName: TutorialService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:16
 */
public interface TutorialService extends IService<Tutorial> {
    Result getTutorial(int pageNo, int pageSize);

    List<Tutorial> getTutorialsByType(String type);
}