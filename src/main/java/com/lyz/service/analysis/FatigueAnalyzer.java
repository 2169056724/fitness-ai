package com.lyz.service.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.model.dto.ai.UserStatus;
import com.lyz.model.entity.UserFeedback;
import com.lyz.model.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能状态分析器 (Pro版)
 * 融合了 ACWR (短期:长期负荷比) 模型与语义归因分析
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
        // 确保 List 是可变的且已排序
        List<UserFeedback> sortedHistory = new ArrayList<>(history);
        sortedHistory.sort((a, b) -> b.getFeedbackDate().compareTo(a.getFeedbackDate()));

        UserFeedback latest = sortedHistory.get(0);
        AnalysisContext context = buildContext(latest, sortedHistory,profile);

        // 3. 提取用户最新备注
        status.setLatestNote(latest.getNotes());

        // 4. 执行分析链 (优先级：伤痛 > 过度训练 > 顺应性 > 进阶)

        // Step A: 伤痛熔断检测 (Safety Breaker)
        if (checkInjury(context, status)) return status;

        // Step B: ACWR 负荷趋势检测 (Trend Analysis)
        if (checkWorkloadTrend(context, status)) return status;

        // Step C: 当日状态归因 (Attribution Analysis)
        checkDailyCondition(context, status);

        return status;
    }

    // ================== 核心检测逻辑 ==================

    /**
     * 检测伤痛与高危部位
     */
    private boolean checkInjury(AnalysisContext ctx, UserStatus status) {
        List<String> riskParts = ctx.tags.stream()
                .filter(TagDictionary::isRiskTag)
                .map(TagDictionary::mapToBodyPart)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!riskParts.isEmpty()) {
            status.setStrategy(UserStatus.Strategy.AVOIDANCE);
            status.setFatigueLevel("SEVERE");
            status.setRiskBodyParts(riskParts);

            String partStr = String.join("、", riskParts);
            status.setAiInstruction(String.format(
                    "【最高优先级】用户报告%s出现疼痛/不适。今日严禁安排涉及%s的动作。建议切换到互补肌群训练（如上肢伤练下肢）或安排低强度康复训练。",
                    partStr, partStr
            ));
            status.setUserMessage("已收到您的" + partStr + "不适反馈，今天我们将避开这些部位，进行安全训练。");
            return true; // 阻断后续分析
        }
        return false;
    }

    /**
     * 基于 ACWR (Acute:Chronic Workload Ratio) 的趋势分析
     * 科学计算：如果短期负荷远超长期负荷，说明有受伤风险
     */
    private boolean checkWorkloadTrend(AnalysisContext ctx, UserStatus status) {
        // 至少需要3天数据才能计算趋势
        if (ctx.history.size() < 3) return false;

        double acuteLoad = calculateAverageLoad(ctx.history, 3,ctx.baseDuration); // 短期(3天)
        double chronicLoad = calculateAverageLoad(ctx.history, 7,ctx.baseDuration); // 长期(7天)

        if (chronicLoad == 0) return false;

        double acwr = acuteLoad / chronicLoad;

        // 运动科学阈值：ACWR > 1.5 为高伤病风险区
        if (acwr > 1.3) {
            status.setStrategy(UserStatus.Strategy.RECOVERY);
            status.setFatigueLevel("MILD");
            status.setAiInstruction(String.format(
                    "【趋势预警】检测到用户短期训练负荷激增 (ACWR=%.1f)。为防止过度训练，今日必须强制安排“减载日(Deload)”，将训练容量降低40%%，侧重柔韧性与恢复。", acwr
            ));
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
     * 当日状态归因 (基于 RPE 与 完成率)
     */
    private void checkDailyCondition(AnalysisContext ctx, UserStatus status) {
        boolean isBusy = ctx.tags.stream().anyMatch(TagDictionary::isBusyTag);

        // 1. 顺应性问题 (没时间/不想练)
        if (isBusy || (ctx.latestRpe <= 3 && ctx.completionRate < 0.6)) {
            status.setStrategy(UserStatus.Strategy.EFFICIENCY);
            status.setAiInstruction("用户反馈时间碎片化或依从性下降。请生成“短时高效”方案，采用HIIT或超级组模式，总时长严格控制在25分钟内。");
            status.setUserMessage("明白您时间紧张，今天我们速战速决！");
            return;
        }

        // 2. 强度过大 (练不动)
        if (ctx.latestRpe >= 4 && ctx.completionRate < 0.8) {
            status.setStrategy(UserStatus.Strategy.RECOVERY);
            status.setAiInstruction("用户昨日感到力竭且未能完成计划。请降低单组次数或减少总组数，提供更容易坚持的方案。");
            status.setUserMessage("昨天辛苦了，今天我们稍微调低一点难度，让身体回回血。");
            return;
        }

        // 3. 强度适中/不足 (默认进阶)
        if (ctx.latestRpe <= 2 && ctx.completionRate > 0.9) {
            status.setStrategy(UserStatus.Strategy.PROGRESS);
            status.setAiInstruction("用户反馈训练过于轻松。请在核心动作上增加重量或尝试进阶变式。");
            status.setUserMessage("状态神勇！今天给您加点挑战。");
            return;
        }

        // 4. 默认维持
        status.setAiInstruction("用户状态平稳。请保持当前训练节奏，可微调动作顺序以保持新鲜感。");
    }

    // ================== 辅助计算与类 ==================

    /**
     * 计算平均训练负荷 (Internal Load = RPE * CompletionRate * BaseDuration)
     */
    private double calculateAverageLoad(List<UserFeedback> history, int days, int duration) {
        return history.stream()
                .limit(days)
                .mapToDouble(f -> {
                    int rpe = f.getRating() != null ? f.getRating() : 3;
                    double completion = f.getCompletionRate() != null ? f.getCompletionRate().doubleValue() / 100.0 : 0.0;
                    // 使用传入的个性化时长
                    return rpe * (duration * completion);
                })
                .average()
                .orElse(0.0);
    }

    private AnalysisContext buildContext(UserFeedback latest, List<UserFeedback> history, UserProfile profile) {
        List<String> tags = Collections.emptyList();
        try {
            if (StringUtils.isNotBlank(latest.getEmotionTags())) {
                tags = objectMapper.readValue(latest.getEmotionTags(), new TypeReference<List<String>>() {});
            }
        } catch (Exception ignored) {}

        double rate = latest.getCompletionRate() != null ? latest.getCompletionRate().doubleValue() / 100.0 : 0.0;
        int rpe = latest.getRating() != null ? latest.getRating() : 3;

        // 获取用户设置的时长，如果为空则用默认值 45
        int userDuration = (profile != null && profile.getAvailableTimePerDay() != null)
                ? profile.getAvailableTimePerDay()
                : BASE_DURATION_MIN;

        return new AnalysisContext(latest, history, tags, rate, rpe, userDuration);
    }

    // 内部上下文对象，传递数据
    private record AnalysisContext(
            UserFeedback latest,
            List<UserFeedback> history,
            List<String> tags,
            double completionRate,
            int latestRpe,
            int baseDuration // 新增字段
    ) {}

    // 标签字典 (静态内部类，也可以抽出去)
    private static class TagDictionary {
        static boolean isRiskTag(String tag) {
            return tag.contains("痛") || tag.contains("伤") || tag.contains("晕") || tag.contains("不适");
        }

        static boolean isBusyTag(String tag) {
            return tag.contains("没时间") || tag.contains("忙") || tag.contains("加班");
        }

        static String mapToBodyPart(String tag) {
            if (tag.contains("膝")) return "膝盖";
            if (tag.contains("腰") || tag.contains("背")) return "下背部";
            if (tag.contains("肩")) return "肩部";
            if (tag.contains("腕")) return "手腕";
            if (tag.contains("踝") || tag.contains("脚")) return "踝关节";
            return null;
        }
    }
}