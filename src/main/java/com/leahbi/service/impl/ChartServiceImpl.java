package com.leahbi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leahbi.model.entity.Chart;
import com.leahbi.model.enums.ChartStatusEnum;
import com.leahbi.service.ChartService;
import com.leahbi.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author leah
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2023-09-12 19:03:29
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

    @Override
    public void handlerUpdateChartError(long chartId, String errorMsg) {
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setStatus(ChartStatusEnum.FAILED.getValue());
        chart.setExecMessage(errorMsg);
        boolean result = this.updateById(chart);
        if(!result){
            log.error("更新图表" + chartId + "失败状态失败," + errorMsg);
        }
    }
}




