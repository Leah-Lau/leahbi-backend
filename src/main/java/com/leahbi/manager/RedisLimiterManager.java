package com.leahbi.manager;

import com.leahbi.common.ErrorCode;
import com.leahbi.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 提供RedisLimiter限流服务
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 进行限流
     * @param key key 区分不同的限流器，比如不同的用户 id 应该分别统计
     */
    public void doRateLimit(String key){
        // 创建限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 设置限流
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);
        // 请求令牌
        boolean result = rateLimiter.tryAcquire(1);
        if(!result){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
