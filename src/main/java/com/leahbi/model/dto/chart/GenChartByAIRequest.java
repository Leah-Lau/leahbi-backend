package com.leahbi.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenChartByAIRequest implements Serializable {

    private static final long serialVersionUID = 2282650684813688339L;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表名称
     */
    private String name;

    /**
     * 图表类型
     */
    private String chartType;

}
