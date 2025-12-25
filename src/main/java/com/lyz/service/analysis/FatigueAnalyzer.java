package com.lyz.service.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.common.FeedbackTagConstants;
import com.lyz.model.dto.ai.UserStatus;
import com.lyz.model.entity.UserFeedback;
import com.lyz.model.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能状态分析器 (Pro版)
 * 融合了 ACWR (短期:长期负荷比) 模型与语义归因分析
 * 使用结构化标签进行精确分析
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FatigueAnalyzer {

    private final ObjectMapper objectMapper;

    // 预设标准训练时长 (分钟)，用于负荷计算
    private static final int BASE_DURATION_MIN = 45;

    /**
     * 主分析入口
     */
    public UserStatus analyze(List<UserFeedback> history, UserProfile profile) {
        // 1. 初始化状态
        UserStatus status = new UserStatus();
        status.setStrategy(UserStatus.Strategy.SUSTAIN); // 默认维持
        status.setFatigueLevel("NONE");

        if (history == null || history.isEmpty()) {
            status.setAiInstruction("新用户或无历史数据，请根据用户档案生成标准的适应性训练计划。");
            return status;
        }

        // 2. 数据预处理 (按时间倒序：Index 0 为最新)
        List<UserFeedback> sortedHistory = new ArrayList<>(history);
        sortedHistory.sort((a, b) -> b.getFeedbackDate().compareTo(a.getFeedbackDate()));

        UserFeedback latest = sortedHistory.get(0);
        AnalysisContext context = buildContext(latest, sortedHistory, profile);

        // 3. 提取用户最新备注
        status.setLatestNote(latest.getNotes());

        // 4. 执行分析链 (优先级：伤痛 > 过度训练 > 顺应性 > 进阶)

        // Step A: 伤痛熔断检测 (Safety Breaker)
        if (checkInjury(context, status))
            return status;

        // Step B: ACWR 负荷趋势检测 (Trend Analysis)
        if (checkWorkloadTrend(context, status))
            return status;

        // Step C: 当日状态归因 (Attribution Analysis)
        checkDailyCondition(context, status);

        return status;
    }

    // ================== 核心检测逻辑 ==================

    /**
     * 检测伤痛与高危部位
     * 直接使用结构化的 painAreas 字段，无需字符串匹配
     */
    private boolean checkInjury(AnalysisContext ctx, UserStatus status) {
        if (ctx.painAreas == null || ctx.painAreas.isEmpty()) {
            return false;
        }

        // 直接使用结构化的酸痛部位
        List<String> riskParts = ctx.painAreas.stream()
                .map(FeedbackTagConstants::mapToBodyPartName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!riskParts.isEmpty()) {
            status.setStrategy(UserStatus.Strategy.AVOIDANCE);
            status.setFatigueLevel("SEVERE");
            status.setRiskBodyParts(ctx.painAreas); // 存储原始代码便于后续处理

            String partStr = String.join("、", riskParts);
            status.setAiInstruction(String.format(
                    "【最高优先级】用户报告%s出现疼痛/不适。今日严禁安排涉及%s的动作。建议切换到互补肌群训练（如上肢伤练下肢）或安排低强度康复训练。",
                    partStr, partStr));
            status.setUserMessage("已收到您的" + partStr + "不适反馈，今天我们将避开这些部位，进行安全训练。");
            return true;
        }
        return false;
    }

    /**
     * 基于 ACWR (Acute:Chronic Workload Ratio) 的趋势分析
     * 现在可以使用 actualDurationMinutes 获取更精确的训练时长
     */
    private boolean checkWorkloadTrend(AnalysisContext ctx, UserStatus status) {
        if (ctx.history.size() < 3)
            return false;

        double acuteLoad = calculateAverageLoad(ctx.history, 3, ctx.baseDuration);
        double chronicLoad = calculateAverageLoad(ctx.history, 7, ctx.baseDuration);

        if (chronicLoad == 0)
            return false;

        double acwr = acuteLoad / chronicLoad;

        // ACWR > 1.3 为高伤病风险区
        if (acwr > 1.3) {
            status.setStrategy(UserStatus.Strategy.RECOVERY);
            status.setFatigueLevel("MILD");
            status.setAiInstruction(String.format(
                    "【趋势预警】检测到用户短期训练负荷激增 (ACWR=%.1f)。为防止过度训练，今日必须强制安排'减载日(Deload)'，将训练容量降低40%%，侧重柔韧性与恢复。", acwr));
            status.setUserMessage("最近练得很猛哦！为了防止过度疲劳，今天我们适当降低一点强度，确保持续进步。");
            return true;
        }

        // ACWR < 0.8 说明训练不足
        if (acwr < 0.8 && ctx.latestRpe < 3) {
            status.setStrategy(UserStatus.Strategy.PROGRESS);
            status.setAiInstruction("【趋势提示】用户近期训练负荷偏低，处于去适应化(Detraining)边缘。请适当增加今日训练强度，引入渐进式超负荷。");
            return true;
        }

        return false;
    }

    /**
     * 当日状态归因 (基于 RPE、完成率和结构化标签)
     * 优化：增加正面标签的分析逻辑
     */
    private void checkDailyCondition(AnalysisContext ctx, UserStatus status) {
        // ==================== 负面信号检测 ====================

        // 使用结构化标签判断时间问题
        boolean hasTimeIssue = ctx.negativeTags != null && ctx.negativeTags.stream()
                .anyMatch(FeedbackTagConstants::isTimeRelatedTag);

        // 1. 顺应性问题 (没时间/不想练)
        if (hasTimeIssue || (ctx.latestRpe <= 3 && ctx.completionRate < 0.6)) {
            status.setStrategy(UserStatus.Strategy.EFFICIENCY);
            status.setAiInstruction("用户反馈时间碎片化或依从性下降。请生成'短时高效'方案，采用HIIT或超级组模式，总时长严格控制在25分钟内。");
            status.setUserMessage("明白您时间紧张，今天我们速战速决！");
            return;
        }

        // 2. 强度过大 (使用结构化标签判断)
        boolean hasTooHard = ctx.negativeTags != null &&
                ctx.negativeTags.contains(FeedbackTagConstants.NEGATIVE_TOO_HARD);
        boolean hasTired = ctx.negativeTags != null &&
                ctx.negativeTags.contains(FeedbackTagConstants.NEGATIVE_TIRED);
        if ((hasTooHard || hasTired || ctx.latestRpe >= 4) && ctx.completionRate < 0.8) {
            status.setStrategy(UserStatus.Strategy.RECOVERY);
            status.setAiInstruction("用户昨日感到力竭且未能完成计划。请降低单组次数或减少总组数，提供更容易坚持的方案。");
            status.setUserMessage("昨天辛苦了，今天我们稍微调低一点难度，让身体回回血。");
            return;
        }

        // ==================== 正面信号检测 ====================

        // 3. 检测力量进步信号：用户反馈力量提升/自信满满 + 高完成率
        boolean hasStrengthProgress = ctx.positiveTags != null && ctx.positiveTags.stream()
                .anyMatch(FeedbackTagConstants::isStrengthProgressTag);

        // 4. 检测高能状态信号：用户反馈精力充沛/暴汗燃脂/有氧畅快
        boolean hasHighEnergy = ctx.positiveTags != null && ctx.positiveTags.stream()
                .anyMatch(FeedbackTagConstants::isHighEnergyTag);

        // 5. 强度不足 (负面标签 TOO_EASY 或 低RPE)
        boolean hasTooEasy = ctx.negativeTags != null &&
                ctx.negativeTags.contains(FeedbackTagConstants.NEGATIVE_TOO_EASY);

        // 进阶条件：(力量进步 || 高能状态 || 太轻松 || 低RPE) + 高完成率
        if ((hasStrengthProgress || hasHighEnergy || hasTooEasy || ctx.latestRpe <= 2) && ctx.completionRate > 0.9) {
            status.setStrategy(UserStatus.Strategy.PROGRESS);

            // 根据正面标签类型生成更个性化的指令
            String instruction;
            String userMsg;
            if (hasStrengthProgress) {
                instruction = "用户反馈力量明显提升。请在核心动作上增加5-10%负重，或增加1-2组训练容量。";
                userMsg = "力量见涨！今天继续突破，给核心动作加点重量。";
            } else if (hasHighEnergy) {
                instruction = "用户处于高能状态。可适当增加训练强度或尝试更高阶的动作变式。";
                userMsg = "状态爆棚！今天可以挑战一下更高强度。";
            } else {
                instruction = "用户反馈训练过于轻松。请在核心动作上增加重量或尝试进阶变式。";
                userMsg = "状态神勇！今天给您加点挑战。";
            }

            status.setAiInstruction(instruction);
            status.setUserMessage(userMsg);
            return;
        }

        // 6. 检测枯燥信号：用户反馈有点枯燥，但完成率尚可
        boolean hasBoring = ctx.negativeTags != null &&
                ctx.negativeTags.contains(FeedbackTagConstants.NEGATIVE_BORING);
        if (hasBoring && ctx.completionRate >= 0.7) {
            status.setStrategy(UserStatus.Strategy.SUSTAIN);
            status.setAiInstruction("用户反馈训练有些枯燥。请调整动作组合，引入新的训练模式（如超级组、金字塔组），保持新鲜感。");
            status.setUserMessage("换点新花样，让训练更有趣！");
            return;
        }

        // 7. 默认维持
        status.setAiInstruction("用户状态平稳。请保持当前训练节奏，可微调动作顺序以保持新鲜感。");
    }

    // ================== 辅助计算与类 ==================

    /**
     * 计算平均训练负荷 (Internal Load = RPE * Duration * CompletionRate)
     * 优先使用 actualDurationMinutes，否则使用默认值
     */
    private double calculateAverageLoad(List<UserFeedback> history, int days, int defaultDuration) {
        return history.stream()
                .limit(days)
                .mapToDouble(f -> {
                    int rpe = f.getRating() != null ? f.getRating() : 3;
                    double completion = f.getCompletionRate() != null ? f.getCompletionRate().doubleValue() / 100.0
                            : 0.0;
                    // 优先使用实际训练时长
                    int duration = f.getActualDurationMinutes() != null ? f.getActualDurationMinutes()
                            : defaultDuration;
                    return rpe * (duration * completion);
                })
                .average()
                .orElse(0.0);
    }

    private AnalysisContext buildContext(UserFeedback latest, List<UserFeedback> history, UserProfile profile) {
        // 解析结构化标签
        List<String> positiveTags = parseJsonList(latest.getPositiveTags());
        List<String> negativeTags = parseJsonList(latest.getNegativeTags());
        List<String> painAreas = parseJsonList(latest.getPainAreas());

        double rate = latest.getCompletionRate() != null ? latest.getCompletionRate().doubleValue() / 100.0 : 0.0;
        int rpe = latest.getRating() != null ? latest.getRating() : 3;

        // 获取用户设置的时长，如果为空则用默认值
        int userDuration = (profile != null && profile.getAvailableTimePerDay() != null)
                ? profile.getAvailableTimePerDay()
                : BASE_DURATION_MIN;

        return new AnalysisContext(latest, history, positiveTags, negativeTags, painAreas, rate, rpe, userDuration);
    }

    /**
     * 解析 JSON 列表
     */
    private List<String> parseJsonList(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("解析标签JSON失败: {}", json);
            return Collections.emptyList();
        }
    }

    // 内部上下文对象，使用结构化标签
    private record AnalysisContext(
            UserFeedback latest,
            List<UserFeedback> history,
            List<String> positiveTags,
            List<String> negativeTags,
            List<String> painAreas,
            double completionRate,
            int latestRpe,
            int baseDuration) {
    }
}
