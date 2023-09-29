package com.leahbi.service;

import com.leahbi.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author leah
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2023-09-12 19:03:29
*/
public interface ChartService extends IService<Chart> {

    void handlerUpdateChartError(long chartId, String errorMsg);

}
