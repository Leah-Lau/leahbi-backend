package com.leahbi.bizmq;

import com.leahbi.common.ErrorCode;
import com.leahbi.constant.BiConstant;
import com.leahbi.constant.CommonConstant;
import com.leahbi.exception.BusinessException;
import com.leahbi.manager.AIManager;
import com.leahbi.model.entity.Chart;
import com.leahbi.model.enums.ChartStatusEnum;
import com.leahbi.service.ChartService;
import com.leahbi.utils.ExcelUtils;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AIManager aiManager;

    @RabbitListener(ackMode = "MANUAL", queues = {BiConstant.BI_QUEUE_NAME})
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("receiveMessage message = {}", message);
        if(StringUtils.isBlank(message)){
            // 拒绝消息
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }

        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if(chart == null){
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }
        // 修改图表状态为running执行中
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(String.valueOf(ChartStatusEnum.RUNNING.getValue()));
        boolean updateResult = chartService.updateById(updateChart);
        if(!updateResult){
            chartService.handlerUpdateChartError(chartId, "更新图表状态失败");
            return;
        }
        // 调用AI
        String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, handleUserInput(chart));
        // 对返回的数据进行拆分
        String[] split = result.split("【【【【【");
        if(split.length < 3){
            chartService.handlerUpdateChartError(chartId, "AI生成错误");
        }
        String genChart = split[1].trim();
        String genResult = split[2].trim();
        Chart finishedChart = new Chart();
        finishedChart.setId(chartId);
        finishedChart.setGenChart(genChart);
        finishedChart.setGenResult(genResult);
        finishedChart.setStatus(ChartStatusEnum.SUCCEED.getValue());
        boolean finishedResult = chartService.updateById(finishedChart);
        if(!finishedResult){
            chartService.handlerUpdateChartError(chartId, "更新图表成功状态失败");
        }
        channel.basicAck(deliveryTag, false);
    }

    private String handleUserInput(Chart chart){
        String goal = chart.getGoal();
        String data = chart.getChartData();
        String chartType = chart.getChartType();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("分析需求：").append("\n");

        if(StringUtils.isNotBlank(chartType)){
            stringBuilder.append("请使用:").append(chartType).append("\n");
        }
        stringBuilder.append(goal).append("\n");
        stringBuilder.append("原始数据：").append("\n");
        stringBuilder.append(data);
        return stringBuilder.toString();
    }
}
