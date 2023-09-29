package com.leahbi.model.dto.chart;

import lombok.Data;
import java.io.Serializable;

@Data
public class ChartAddRequest implements Serializable {

    private static final long serialVersionUID = 1340033575022336351L;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表名称
     */
    private String name;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;
}
