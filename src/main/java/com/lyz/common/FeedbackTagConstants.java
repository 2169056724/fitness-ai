package com.lyz.common;

import java.util.*;

/**
 * 反馈标签常量定义
 * 前后端、AI Prompt 共用同一套语义编码
 */
public class FeedbackTagConstants {

    // ===================== 正面感受 Positive Tags =====================
    public static final String POSITIVE_SWEATY = "SWEATY"; // 暴汗燃脂
    public static final String POSITIVE_STRENGTH_UP = "STRENGTH_UP"; // 力量提升
    public static final String POSITIVE_CARDIO_SMOOTH = "CARDIO_SMOOTH"; // 有氧畅快
    public static final String POSITIVE_ENERGIZED = "ENERGIZED"; // 精力充沛
    public static final String POSITIVE_CONFIDENT = "CONFIDENT"; // 自信满满
    public static final String POSITIVE_RELAXED = "RELAXED"; // 身心放松

    // ===================== 负面感受 Negative Tags =====================
    public static final String NEGATIVE_TOO_HARD = "TOO_HARD"; // 强度太大
    public static final String NEGATIVE_TOO_EASY = "TOO_EASY"; // 太轻松了
    public static final String NEGATIVE_TOO_LONG = "TOO_LONG"; // 时间太长
    public static final String NEGATIVE_BORING = "BORING"; // 有点枯燥
    public static final String NEGATIVE_NO_TIME = "NO_TIME"; // 没时间
    public static final String NEGATIVE_TIRED = "TIRED"; // 身体疲惫

    // ===================== 酸痛/不适部位 Pain Areas =====================
    public static final String PAIN_KNEE = "KNEE"; // 膝盖
    public static final String PAIN_LOWER_BACK = "LOWER_BACK"; // 腰部/下背
    public static final String PAIN_SHOULDER = "SHOULDER"; // 肩部
    public static final String PAIN_WRIST = "WRIST"; // 手腕
    public static final String PAIN_ANKLE = "ANKLE"; // 脚踝
    public static final String PAIN_NECK = "NECK"; // 颈部
    public static final String PAIN_ELBOW = "ELBOW"; // 肘部
    public static final String PAIN_HIP = "HIP"; // 髋部
    public static final String PAIN_OTHER = "OTHER"; // 其他

    /**
     * 所有有效的正面标签集合
     */
    public static final Set<String> VALID_POSITIVE_TAGS = Set.of(
            POSITIVE_SWEATY, POSITIVE_STRENGTH_UP, POSITIVE_CARDIO_SMOOTH,
            POSITIVE_ENERGIZED, POSITIVE_CONFIDENT, POSITIVE_RELAXED);

    /**
     * 所有有效的负面标签集合
     */
    public static final Set<String> VALID_NEGATIVE_TAGS = Set.of(
            NEGATIVE_TOO_HARD, NEGATIVE_TOO_EASY, NEGATIVE_TOO_LONG,
            NEGATIVE_BORING, NEGATIVE_NO_TIME, NEGATIVE_TIRED);

    /**
     * 所有有效的酸痛部位集合
     */
    public static final Set<String> VALID_PAIN_AREAS = Set.of(
            PAIN_KNEE, PAIN_LOWER_BACK, PAIN_SHOULDER, PAIN_WRIST,
            PAIN_ANKLE, PAIN_NECK, PAIN_ELBOW, PAIN_HIP, PAIN_OTHER);

    /**
     * 表示"时间不足"类标签（用于效率策略判断）
     */
    public static final Set<String> TIME_RELATED_TAGS = Set.of(
            NEGATIVE_NO_TIME, NEGATIVE_TOO_LONG);

    /**
     * 表示"力量进步"类正面标签（用于进阶策略判断）
     */
    public static final Set<String> STRENGTH_PROGRESS_TAGS = Set.of(
            POSITIVE_STRENGTH_UP, POSITIVE_CONFIDENT);

    /**
     * 表示"高能状态"类正面标签（用于判断用户精力充沛）
     */
    public static final Set<String> HIGH_ENERGY_TAGS = Set.of(
            POSITIVE_ENERGIZED, POSITIVE_SWEATY, POSITIVE_CARDIO_SMOOTH);

    /**
     * 标签中文映射（用于日志、Prompt 等）
     */
    private static final Map<String, String> TAG_CN_MAP = new HashMap<>();

    static {
        // 正面
        TAG_CN_MAP.put(POSITIVE_SWEATY, "暴汗燃脂");
        TAG_CN_MAP.put(POSITIVE_STRENGTH_UP, "力量提升");
        TAG_CN_MAP.put(POSITIVE_CARDIO_SMOOTH, "有氧畅快");
        TAG_CN_MAP.put(POSITIVE_ENERGIZED, "精力充沛");
        TAG_CN_MAP.put(POSITIVE_CONFIDENT, "自信满满");
        TAG_CN_MAP.put(POSITIVE_RELAXED, "身心放松");

        // 负面
        TAG_CN_MAP.put(NEGATIVE_TOO_HARD, "强度太大");
        TAG_CN_MAP.put(NEGATIVE_TOO_EASY, "太轻松了");
        TAG_CN_MAP.put(NEGATIVE_TOO_LONG, "时间太长");
        TAG_CN_MAP.put(NEGATIVE_BORING, "有点枯燥");
        TAG_CN_MAP.put(NEGATIVE_NO_TIME, "没时间");
        TAG_CN_MAP.put(NEGATIVE_TIRED, "身体疲惫");

        // 酸痛
        TAG_CN_MAP.put(PAIN_KNEE, "膝盖");
        TAG_CN_MAP.put(PAIN_LOWER_BACK, "腰部");
        TAG_CN_MAP.put(PAIN_SHOULDER, "肩部");
        TAG_CN_MAP.put(PAIN_WRIST, "手腕");
        TAG_CN_MAP.put(PAIN_ANKLE, "脚踝");
        TAG_CN_MAP.put(PAIN_NECK, "颈部");
        TAG_CN_MAP.put(PAIN_ELBOW, "肘部");
        TAG_CN_MAP.put(PAIN_HIP, "髋部");
        TAG_CN_MAP.put(PAIN_OTHER, "其他部位");
    }

    /**
     * 获取标签的中文名称
     */
    public static String getTagCnName(String tagCode) {
        return TAG_CN_MAP.getOrDefault(tagCode, tagCode);
    }

    /**
     * 将标签代码列表转换为中文描述
     */
    public static String toChineseDescription(List<String> tagCodes) {
        if (tagCodes == null || tagCodes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String code : tagCodes) {
            if (sb.length() > 0)
                sb.append("、");
            sb.append(getTagCnName(code));
        }
        return sb.toString();
    }

    /**
     * 验证标签是否有效
     */
    public static boolean isValidTag(String tag) {
        return VALID_POSITIVE_TAGS.contains(tag)
                || VALID_NEGATIVE_TAGS.contains(tag)
                || VALID_PAIN_AREAS.contains(tag);
    }

    /**
     * 判断标签是否为酸痛部位
     */
    public static boolean isPainArea(String tag) {
        return VALID_PAIN_AREAS.contains(tag);
    }

    /**
     * 判断标签是否表示时间不足
     */
    public static boolean isTimeRelatedTag(String tag) {
        return TIME_RELATED_TAGS.contains(tag);
    }

    /**
     * 将酸痛部位代码映射为身体部位名称（用于 AI Prompt）
     */
    public static String mapToBodyPartName(String painAreaCode) {
        return TAG_CN_MAP.getOrDefault(painAreaCode, null);
    }

    /**
     * 判断标签是否表示力量进步
     */
    public static boolean isStrengthProgressTag(String tag) {
        return STRENGTH_PROGRESS_TAGS.contains(tag);
    }

    /**
     * 判断标签是否表示高能状态
     */
    public static boolean isHighEnergyTag(String tag) {
        return HIGH_ENERGY_TAGS.contains(tag);
    }
}
