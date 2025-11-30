package com.lyz.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.model.dto.ai.UserStatus;
import com.lyz.model.entity.UserFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 状态分析器：负责分析历史反馈，生成用户状态快照
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FatigueAnalyzer {

    private final ObjectMapper objectMapper;

    /**
     * 分析用户最近的状态
     * @param recentFeedbacks 最近7天的反馈记录（按日期正序排列）
     * @return 结构化的用户状态对象
     */
    public UserStatus analyze(List<UserFeedback> recentFeedbacks) {
        UserStatus status = new UserStatus();

        // 默认初始化
        status.setFatigueLevel("NONE");
        status.setIntensityAdjustment(1.0);
        status.setNeedRestDay(false);
        status.setRecentTrend("表现平稳");

        if (recentFeedbacks == null || recentFeedbacks.isEmpty()) {
            status.setRecentTrend("暂无近期反馈数据，按标准流程进行");
            return status;
        }

        analyzeTrends(recentFeedbacks, status);
        analyzeEmotionAndBodyParts(recentFeedbacks, status);

        // 提取最近一条有内容的备注
        for (int i = recentFeedbacks.size() - 1; i >= 0; i--) {
            String note = recentFeedbacks.get(i).getNotes();
            if (StringUtils.isNotBlank(note)) {
                status.setLatestNote(note);
                break;
            }
        }

        return status;
    }

    private void analyzeTrends(List<UserFeedback> feedbacks, UserStatus status) {
        // 计算平均完成率
        double avgRate = feedbacks.stream()
                .filter(f -> f.getCompletionRate() != null)
                .mapToDouble(f -> f.getCompletionRate().doubleValue())
                .average().orElse(0.0);

        if (avgRate < 60) {
            status.setRecentTrend("近期完成率较低(" + (int)avgRate + "%)，可能强度过大");
            status.setIntensityAdjustment(0.8); // 降级
        } else if (avgRate > 95) {
            status.setRecentTrend("近期完成率极高，状态火热");
            status.setIntensityAdjustment(1.1); // 升级
        }

        // 检查连续低分
        long lowRatingCount = feedbacks.stream()
                .filter(f -> f.getRating() != null && f.getRating() <= 2)
                .count();
        if (lowRatingCount >= 2) {
            status.setFatigueLevel("MILD");
            status.setRecentTrend("连续多次评分较低，建议关注恢复");
        }
    }

    private void analyzeEmotionAndBodyParts(List<UserFeedback> feedbacks, UserStatus status) {
        // 只分析最近 3 天的反馈来决定当天的疲劳部位
        int checkSize = Math.min(feedbacks.size(), 3);
        List<UserFeedback> latestFeedbacks = feedbacks.subList(feedbacks.size() - checkSize, feedbacks.size());

        List<String> detectedParts = new ArrayList<>();
        int fatigueCount = 0;

        for (UserFeedback fb : latestFeedbacks) {
            List<String> tags = parseEmotionTags(fb.getEmotionTags());
            for (String tag : tags) {
                // 扩充关键词，防止漏网之鱼
                if (tag.contains("腿") || tag.contains("下肢") || tag.contains("蹲") || tag.contains("臀")) detectedParts.add("下肢"); // 加个臀
                if (tag.contains("胸") || tag.contains("推") || tag.contains("卧推")) detectedParts.add("胸部");
                if (tag.contains("背") || tag.contains("拉") || tag.contains("划船")) detectedParts.add("背部");
                if (tag.contains("肩") || tag.contains("举")) detectedParts.add("肩部"); // 加个举（推举）

                // 扩充疲劳词
                if (tag.contains("累") || tag.contains("疲") || tag.contains("酸") || tag.contains("痛") || tag.contains("炸") || tag.contains("废")) {
                    fatigueCount++;
                }
            }
        }

        if (!detectedParts.isEmpty()) {
            status.setFatiguedBodyParts(detectedParts.stream().distinct().toList());
        }

        if (fatigueCount >= 2) {
            status.setFatigueLevel("SEVERE");
            status.setNeedRestDay(true);
            status.setIntensityAdjustment(0.6); // 显著降级
            status.setMoodSummary("检测到明显的身体疲劳信号");
        }
    }

    private List<String> parseEmotionTags(String json) {
        List<String> list = new ArrayList<>();
        if (StringUtils.isBlank(json)) return list;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                node.forEach(n -> list.add(n.asText()));
            } else {
                list.add(node.asText());
            }
        } catch (Exception e) {
            // 简单容错
            list.add(json);
        }
        return list;
    }
}