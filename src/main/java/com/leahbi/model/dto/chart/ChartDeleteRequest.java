package com.leahbi.model.dto.chart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ChartDeleteRequest implements Serializable {

    private static final long serialVersionUID = -1454073725445198350L;

    private Long id;

}
