package com.leahbi.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@Slf4j
@RequestMapping("/queue")
public class ThreadController {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("/add")
    public void addThread(String name){
        CompletableFuture.runAsync(() ->{
            log.info("执行人:" + Thread.currentThread().getName() +" 执行任务：" + name);
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },threadPoolExecutor);
    }

    @GetMapping("/get")
    public String getThread(){
        Map<String, Object> map = new HashMap<>();
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        map.put("完成任务数",completedTaskCount);
        int activeCount = threadPoolExecutor.getActiveCount();
        map.put("工作中线程数",activeCount);
        int size = threadPoolExecutor.getQueue().size();
        map.put("队列长度",size);
        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("任务总数",taskCount);
        return JSONUtil.toJsonStr(map);
    }
}
