package com.leahbi.bizmq;

import com.leahbi.common.ErrorCode;
import com.leahbi.constant.BiConstant;
import com.leahbi.constant.CommonConstant;
import com.leahbi.exception.BusinessException;
import com.leahbi.manager.AIManager;
import com.leahbi.model.entity.Chart;
import com.leahbi.model.enums.ChartStatusEnum;
import com.leahbi.service.ChartService;
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
public class BiDlxMessageConsumer {

    @Resource
    private ChartService chartService;

    @RabbitListener(ackMode = "MANUAL", queues = {BiConstant.BI_DLX_QUEUE_NAME})
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("receiveDLXMessage message = {}", message);
        System.out.println("This message is rejected："+message);
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
        // 修改图表状态为failed失败
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(String.valueOf(ChartStatusEnum.FAILED.getValue()));
        boolean updateResult = chartService.updateById(updateChart);
        if(!updateResult){
            chartService.handlerUpdateChartError(chartId, "更新图表状态失败");
            return;
        }
        channel.basicAck(deliveryTag, false);
    }
}
