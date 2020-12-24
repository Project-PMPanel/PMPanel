package project.daihao18.panel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.DetectList;

import java.util.List;

/**
 * @InterfaceName: DetectListService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:16
 */
public interface DetectListService extends IService<DetectList> {
    IPage<DetectList> getPageDetect(Integer pageNo, Integer pageSize);

    List<DetectList> getAllDetects();
}