package com.leahbi.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leahbi.bizmq.BiMessageProducer;
import com.leahbi.common.BaseResponse;
import com.leahbi.common.ErrorCode;
import com.leahbi.common.ResultUtils;
import com.leahbi.constant.CommonConstant;
import com.leahbi.exception.BusinessException;
import com.leahbi.exception.ThrowUtils;
import com.leahbi.manager.AIManager;
import com.leahbi.manager.RedisLimiterManager;
import com.leahbi.model.dto.chart.*;
import com.leahbi.model.dto.file.UploadFileRequest;
import com.leahbi.model.entity.Chart;
import com.leahbi.model.entity.User;
import com.leahbi.model.enums.ChartStatusEnum;
import com.leahbi.model.vo.BiResponse;
import com.leahbi.service.ChartService;
import com.leahbi.service.UserService;
import com.leahbi.utils.ExcelUtils;
import com.leahbi.utils.SqlUtils;
import io.reactivex.rxjava3.core.Completable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private UserService userService;

    @Resource
    private ChartService chartService;

    @Resource
    private AIManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;


    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request){
        if(chartAddRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest,chart);
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        chart.setUserId(userId);
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        Long id = chart.getId();
        return ResultUtils.success(id);
    }

    /**
     * 删除
     *
     * @param chartDeleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody ChartDeleteRequest chartDeleteRequest, HttpServletRequest request){
        if(chartDeleteRequest == null || chartDeleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long id = chartDeleteRequest.getId();
        // 检查是否仍存在
        Chart theChart = chartService.getById(id);
        ThrowUtils.throwIf(theChart == null,ErrorCode.NOT_FOUND_ERROR);
        // 权限判断，仅管理员与本人可删除
        if(!theChart.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @RequestMapping("/update")
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest, HttpServletRequest request){
        if(chartUpdateRequest == null || chartUpdateRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chart,chartUpdateRequest);
        // 鉴权
        User loginUser = userService.getLoginUser(request);
        if(!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 检查该图是否存在
        Long id = chartUpdateRequest.getId();
        Chart theChart = chartService.getById(id);
        if(theChart == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean result = chartService.updateById(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id){
        if(id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if(chart == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest, HttpServletRequest request){
        long current = chartQueryRequest.getCurrent();
        long pageSize = chartQueryRequest.getPageSize();
        User loginUser = userService.getLoginUser(request);
        if(!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, pageSize), getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/myPage")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest, HttpServletRequest request){
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = chartQueryRequest.getCurrent();
        long pageSize = chartQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        Page<Chart> page = chartService.page(new Page<>(current, pageSize), getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(page);
    }

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request){
        if(chartEditRequest == null || chartEditRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        // 判断是否存在
        Chart theChart = chartService.getById(chartEditRequest.getId());
        ThrowUtils.throwIf(theChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 检查权限（本人/管理员）
        User loginUser = userService.getLoginUser(request);
        if(!loginUser.getId().equals(theChart.getUserId()) || !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 智能分析（异步线程池）
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("/genAsync")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                            GenChartByAIRequest genChartByAIRequest, HttpServletRequest request){
        String goal = genChartByAIRequest.getGoal();
        String name = genChartByAIRequest.getName();
        String chartType = genChartByAIRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件大小
        long size = multipartFile.getSize();
        long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1MB");
        // 校验文件后缀名
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> list = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!list.contains(suffix), ErrorCode.PARAMS_ERROR,"文件后缀非法");

        // 获取当前用户
        User loginUser = userService.getLoginUser(request);
        // 限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" +loginUser.getId());

        // 将输入的信息转换成规定的格式
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("分析需求：").append("\n");
        if(StringUtils.isNotBlank(chartType)){
            stringBuilder.append("请使用:").append(chartType).append("\n");
        }
        stringBuilder.append(goal).append("\n");
        String data = ExcelUtils.excelToCsv(multipartFile);
        stringBuilder.append("原始数据：").append("\n");
        stringBuilder.append(data);

        // 插入到数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        // 保存
        boolean saveResult = chartService.save(chart);
        // 校验是否保存成功
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "图表保存失败");

        CompletableFuture.runAsync(() -> {
            System.out.println("threadPoolExecutor.getActiveCount():" + threadPoolExecutor.getActiveCount());
            // 修改图表状态为running执行中
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(String.valueOf(ChartStatusEnum.RUNNING.getValue()));
            boolean updateResult = chartService.updateById(updateChart);
            if(!updateResult){
                chartService.handlerUpdateChartError(chart.getId(), "更新图表状态失败");
                return;
            }
            // 调用AI
            String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, stringBuilder.toString());
            // 对返回的数据进行拆分
            String[] split = result.split("【【【【【");
            if(split.length < 3){
                chartService.handlerUpdateChartError(chart.getId(), "AI生成错误");
            }
            String genChart = split[1].trim();
            String genResult = split[2].trim();
            Chart finishedChart = new Chart();
            finishedChart.setId(chart.getId());
            finishedChart.setGenChart(genChart);
            finishedChart.setGenResult(genResult);
            finishedChart.setStatus(ChartStatusEnum.SUCCEED.getValue());
            boolean finishedResult = chartService.updateById(finishedChart);
            if(!finishedResult){
                chartService.handlerUpdateChartError(chart.getId(), "更新图表成功状态失败");
            }
        }, threadPoolExecutor);

        // 返回对象 对图标信息进行脱敏
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步消息队列）
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("/genAsync/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAIRequest genChartByAIRequest, HttpServletRequest request){
        String goal = genChartByAIRequest.getGoal();
        String name = genChartByAIRequest.getName();
        String chartType = genChartByAIRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件大小
        long size = multipartFile.getSize();
        long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1MB");
        // 校验文件后缀名
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> list = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!list.contains(suffix), ErrorCode.PARAMS_ERROR,"文件后缀非法");

        // 获取当前用户
        User loginUser = userService.getLoginUser(request);
        // 限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" +loginUser.getId());

        // 将输入的信息转换成规定的格式
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("分析需求：").append("\n");
        if(StringUtils.isNotBlank(chartType)){
            stringBuilder.append("请使用:").append(chartType).append("\n");
        }
        stringBuilder.append(goal).append("\n");
        String data = ExcelUtils.excelToCsv(multipartFile);
        stringBuilder.append("原始数据：").append("\n");
        stringBuilder.append(data);

        // 插入到数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        // 保存
        boolean saveResult = chartService.save(chart);
        // 校验是否保存成功
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "图表保存失败");

        biMessageProducer.sendMessage(String.valueOf(chart.getId()));

        // 返回对象 对图标信息进行脱敏
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 获取查询包装类
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest){
        QueryWrapper<Chart> chartQueryWrapper = new QueryWrapper<>();
        if(chartQueryRequest == null){
            return chartQueryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String name = chartQueryRequest.getName();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        chartQueryWrapper.eq(id != null && id > 0,"id",id);
        chartQueryWrapper.like(StringUtils.isNotBlank(goal),"goal",goal);
        chartQueryWrapper.like(StringUtils.isNotBlank(name),"name",name);
        chartQueryWrapper.like(StringUtils.isNotBlank(chartType),"chartType",chartType);
        chartQueryWrapper.eq(ObjectUtils.isNotEmpty(userId),"userId",userId);
        chartQueryWrapper.eq("isDelete",false);
        chartQueryWrapper.orderBy(SqlUtils.validSortField(sortField),sortOrder.equals(CommonConstant.SORT_ORDER_ASC),sortField);
        return chartQueryWrapper;
    }

}
