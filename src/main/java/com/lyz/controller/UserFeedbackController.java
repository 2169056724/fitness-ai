package com.lyz.controller;

import com.lyz.common.FeedbackTagConstants;
import com.lyz.common.Result;
import com.lyz.common.UserContext;
import com.lyz.model.dto.UserFeedbackDTO;
import com.lyz.model.vo.UserFeedbackVO;
import com.lyz.service.UserFeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@Slf4j
public class UserFeedbackController {

    @Autowired
    private UserFeedbackService userFeedbackService;

    /**
     * 创建用户反馈
     * 支持新版结构化标签 (positiveTags, negativeTags, painAreas)
     * 同时兼容旧版 tagList 字段
     */
    @PostMapping
    public Result<UserFeedbackVO> createFeedback(@RequestBody UserFeedbackDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("创建用户反馈, userId={}, dto={}", userId, dto);

        // 参数校验
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            return Result.error(400, "疲劳度评分必须在1-5之间");
        }

        // 自动填充日期
        if (dto.getFeedbackDate() == null) {
            dto.setFeedbackDate(LocalDate.now());
        }

        // 向后兼容：如果使用旧版 tagList，自动迁移到新字段
        migrateFromLegacyTagList(dto);

        // 验证标签有效性
        validateTags(dto);

        UserFeedbackVO vo = userFeedbackService.createFeedback(userId, dto);
        return Result.success("反馈提交成功", vo);
    }

    /**
     * 获取今日反馈
     */
    @GetMapping("/today")
    public Result<UserFeedbackVO> getTodayFeedback() {
        Long userId = UserContext.getUserId();
        UserFeedbackVO vo = userFeedbackService.getTodayFeedback(userId);
        if (vo == null) {
            return Result.success("今日尚未提交反馈", null);
        }
        return Result.success(vo);
    }

    /**
     * 从旧版 tagList 迁移到新的结构化字段
     */
    @SuppressWarnings("deprecation")
    private void migrateFromLegacyTagList(UserFeedbackDTO dto) {
        List<String> tagList = dto.getTagList();
        if (tagList == null || tagList.isEmpty()) {
            return;
        }

        // 如果新字段已有数据，则不覆盖
        if (hasNewStructuredTags(dto)) {
            return;
        }

        List<String> positive = new ArrayList<>();
        List<String> negative = new ArrayList<>();
        List<String> pain = new ArrayList<>();

        for (String tag : tagList) {
            // 尝试匹配新版枚举
            if (FeedbackTagConstants.VALID_POSITIVE_TAGS.contains(tag)) {
                positive.add(tag);
            } else if (FeedbackTagConstants.VALID_NEGATIVE_TAGS.contains(tag)) {
                negative.add(tag);
            } else if (FeedbackTagConstants.VALID_PAIN_AREAS.contains(tag)) {
                pain.add(tag);
            } else {
                // 旧版中文标签兼容映射
                String mapped = mapLegacyChineseTag(tag);
                if (mapped != null) {
                    if (FeedbackTagConstants.VALID_POSITIVE_TAGS.contains(mapped)) {
                        positive.add(mapped);
                    } else if (FeedbackTagConstants.VALID_NEGATIVE_TAGS.contains(mapped)) {
                        negative.add(mapped);
                    } else if (FeedbackTagConstants.VALID_PAIN_AREAS.contains(mapped)) {
                        pain.add(mapped);
                    }
                } else {
                    log.warn("未识别的标签: {}", tag);
                }
            }
        }

        if (!positive.isEmpty())
            dto.setPositiveTags(positive);
        if (!negative.isEmpty())
            dto.setNegativeTags(negative);
        if (!pain.isEmpty())
            dto.setPainAreas(pain);
    }

    private boolean hasNewStructuredTags(UserFeedbackDTO dto) {
        return (dto.getPositiveTags() != null && !dto.getPositiveTags().isEmpty())
                || (dto.getNegativeTags() != null && !dto.getNegativeTags().isEmpty())
                || (dto.getPainAreas() != null && !dto.getPainAreas().isEmpty());
    }

    /**
     * 旧版中文标签映射到新版枚举
     */
    private String mapLegacyChineseTag(String chineseTag) {
        if (chineseTag == null)
            return null;

        // 正面
        if (chineseTag.contains("暴汗") || chineseTag.contains("燃脂"))
            return FeedbackTagConstants.POSITIVE_SWEATY;
        if (chineseTag.contains("力量"))
            return FeedbackTagConstants.POSITIVE_STRENGTH_UP;
        if (chineseTag.contains("有氧") || chineseTag.contains("畅快"))
            return FeedbackTagConstants.POSITIVE_CARDIO_SMOOTH;
        if (chineseTag.contains("充沛") || chineseTag.contains("精力"))
            return FeedbackTagConstants.POSITIVE_ENERGIZED;

        // 负面
        if (chineseTag.contains("太难") || chineseTag.contains("强度大"))
            return FeedbackTagConstants.NEGATIVE_TOO_HARD;
        if (chineseTag.contains("太轻") || chineseTag.contains("轻松"))
            return FeedbackTagConstants.NEGATIVE_TOO_EASY;
        if (chineseTag.contains("太长") || chineseTag.contains("时间长"))
            return FeedbackTagConstants.NEGATIVE_TOO_LONG;
        if (chineseTag.contains("枯燥") || chineseTag.contains("无聊"))
            return FeedbackTagConstants.NEGATIVE_BORING;
        if (chineseTag.contains("没时间") || chineseTag.contains("忙") || chineseTag.contains("加班"))
            return FeedbackTagConstants.NEGATIVE_NO_TIME;
        if (chineseTag.contains("累") || chineseTag.contains("疲"))
            return FeedbackTagConstants.NEGATIVE_TIRED;

        // 酸痛部位
        if (chineseTag.contains("膝"))
            return FeedbackTagConstants.PAIN_KNEE;
        if (chineseTag.contains("腰") || chineseTag.contains("背"))
            return FeedbackTagConstants.PAIN_LOWER_BACK;
        if (chineseTag.contains("肩"))
            return FeedbackTagConstants.PAIN_SHOULDER;
        if (chineseTag.contains("腕") || chineseTag.contains("手"))
            return FeedbackTagConstants.PAIN_WRIST;
        if (chineseTag.contains("踝") || chineseTag.contains("脚"))
            return FeedbackTagConstants.PAIN_ANKLE;
        if (chineseTag.contains("颈") || chineseTag.contains("脖"))
            return FeedbackTagConstants.PAIN_NECK;
        if (chineseTag.contains("肘"))
            return FeedbackTagConstants.PAIN_ELBOW;
        if (chineseTag.contains("髋") || chineseTag.contains("胯"))
            return FeedbackTagConstants.PAIN_HIP;

        return null;
    }

    /**
     * 验证标签有效性 (仅警告，不阻断)
     */
    private void validateTags(UserFeedbackDTO dto) {
        validateTagList(dto.getPositiveTags(), "正面标签", FeedbackTagConstants.VALID_POSITIVE_TAGS);
        validateTagList(dto.getNegativeTags(), "负面标签", FeedbackTagConstants.VALID_NEGATIVE_TAGS);
        validateTagList(dto.getPainAreas(), "酸痛部位", FeedbackTagConstants.VALID_PAIN_AREAS);
    }

    private void validateTagList(List<String> tags, String category, java.util.Set<String> validSet) {
        if (tags == null)
            return;
        for (String tag : tags) {
            if (!validSet.contains(tag)) {
                log.warn("{}包含未知标签: {}", category, tag);
            }
        }
    }
}
