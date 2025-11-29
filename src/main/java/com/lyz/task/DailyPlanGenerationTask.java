package com.lyz.task;

import com.lyz.mapper.UserProfileMapper;
import com.lyz.model.entity.UserProfile;
import com.lyz.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 每日计划生成定时任务
 * 在凌晨2点（可配置）自动为所有有健康档案的用户生成次日训练计划
 * 
 * 配置说明：
 * - 在 application.yml 中设置 schedule.daily-plan.enabled=false 可禁用定时任务
 * - 在 application.yml 中设置 schedule.daily-plan.cron 可自定义执行时间
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "schedule.daily-plan", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DailyPlanGenerationTask {

    private final RecommendationService recommendationService;
    private final UserProfileMapper userProfileMapper;

    /**
     * 活跃用户定义：7天内有登录记录
     */
    private static final int ACTIVE_DAYS_THRESHOLD = 7;

    /**
     * 每天凌晨2点执行（默认）
     * cron表达式：秒 分 时 日 月 周
     * "0 0 2 * * ?" 表示每天凌晨2点0分0秒执行
     * 可在配置文件中修改执行时间：schedule.daily-plan.cron
     */
    @Scheduled(cron = "${schedule.daily-plan.cron:0 0 2 * * ?}")
    public void generateDailyPlansForAllUsers() {
        log.info("========== 开始执行每日计划生成定时任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 查询所有有健康档案的用户
            List<UserProfile> allProfiles = userProfileMapper.selectAllProfiles();
            
            if (allProfiles == null || allProfiles.isEmpty()) {
                log.info("暂无用户健康档案，跳过计划生成");
                return;
            }

            log.info("共查询到 {} 位用户健康档案", allProfiles.size());
            
            // 2. 过滤出活跃用户（7天内有登录）
            List<UserProfile> activeProfiles = userProfileMapper.selectActiveProfiles(ACTIVE_DAYS_THRESHOLD);
            
            if (activeProfiles == null || activeProfiles.isEmpty()) {
                log.info("暂无活跃用户（{}天内有登录），跳过计划生成", ACTIVE_DAYS_THRESHOLD);
                return;
            }

            log.info("筛选出 {} 位活跃用户（{}天内有登录），开始生成计划", 
                     activeProfiles.size(), ACTIVE_DAYS_THRESHOLD);

            int successCount = 0;
            int failCount = 0;

            // 3. 为每个活跃用户生成计划
            for (UserProfile profile : activeProfiles) {
                try {
                    Long userId = profile.getUserId();
                    log.debug("正在为用户 {} 生成计划...", userId);
                    
                    // 调用推荐服务生成计划（不传递可穿戴设备数据，仅基于历史反馈）
                    recommendationService.generateDailyPlan(userId, null);
                    
                    successCount++;
                    log.debug("用户 {} 计划生成成功", userId);
                    
                    // 避免短时间内大量调用AI接口，添加短暂延迟
                    Thread.sleep(2000); // 每个用户之间间隔2秒
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("用户 {} 计划生成失败", profile.getUserId(), e);
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;
            int inactiveCount = allProfiles.size() - activeProfiles.size();

            log.info("========== 每日计划生成任务完成 ==========");
            log.info("总用户数: {}, 活跃用户: {}, 不活跃用户(跳过): {}", 
                     allProfiles.size(), activeProfiles.size(), inactiveCount);
            log.info("生成结果 - 成功: {}, 失败: {}, 耗时: {}秒", 
                     successCount, failCount, duration);

        } catch (Exception e) {
            log.error("每日计划生成任务执行异常", e);
        }
    }

    /**
     * 测试用定时任务（可选）
     * 每5分钟执行一次，用于开发测试
     * 生产环境请注释掉此方法
     */
    // @Scheduled(cron = "0 */5 * * * ?")
    public void testGeneratePlans() {
        log.info("========== 测试：每5分钟生成计划 ==========");
        generateDailyPlansForAllUsers();
    }
}
