package com.lyz.service.builder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.model.dto.ai.HealthConstraints;
import com.lyz.model.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 医疗数据核心处理类
 * 职责：
 * 1. 解析OCR提取的JSON数据
 * 2. 规则引擎：根据指标推导饮食禁忌 (DietConstraints)
 * 3. 兼容旧版：生成字符串形式的 Advice Prompt
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalContextBuilder {

    private final ObjectMapper objectMapper;

    /**
     * 【新核心方法】基于体检数据推导结构化约束
     * @param extractedMedicalData 体检JSON
     * @param gender 性别
     * @return 饮食约束对象
     */
    public HealthConstraints inferConstraints(String extractedMedicalData, Integer gender) {
        HealthConstraints constraints = new HealthConstraints();
        if (StringUtils.isBlank(extractedMedicalData)) {
            return constraints;
        }

        try {
            Map<String, String> data = objectMapper.readValue(extractedMedicalData, new TypeReference<Map<String, String>>() {});

            // 1. 尿酸 (Uric Acid)
            checkUricAcid(data, gender, constraints);
            // 2. 血糖 (Glucose)
            checkBloodSugar(data, constraints);
            // 3. 血脂 (Lipids)
            checkLipids(data, constraints);
            // 4. 血压 (BP)
            checkBloodPressure(data, constraints);

        } catch (Exception e) {
            log.error("解析体检数据失败", e);
        }
        return constraints;
    }

    /**
     * 【兼容方法】生成存入数据库的 Prompt 字符串
     * 现在它内部调用 inferConstraints，保证逻辑不重复
     */
    public String generateMedicalAdvicePrompt(String extractedMedicalData, Integer gender) {
        HealthConstraints constraints = inferConstraints(extractedMedicalData, gender);

        // 如果没有风险，返回空
        if (StringUtils.isBlank(constraints.getRiskWarning())) {
            return "";
        }

        // 将结构化对象转回字符串，供 DB 存储 (旧逻辑依赖)
        StringBuilder sb = new StringBuilder("- Medical Risk & Advice: ");
        sb.append(constraints.getRiskWarning());

        if (!constraints.getForbiddenCategories().isEmpty()) {
            sb.append(" Forbidden: ").append(String.join(", ", constraints.getForbiddenCategories())).append(";");
        }
        if (!constraints.getStrategyTags().isEmpty()) {
            sb.append(" Strategy: ").append(String.join(", ", constraints.getStrategyTags())).append(";");
        }

        return sb.toString();
    }



    private void checkUricAcid(Map<String, String> data, Integer gender, HealthConstraints dc) {
        double val = parseNumber(data.get("uric_acid"));
        if (val <= 0) return;
        // 阈值: 男420, 女360
        double limit = (gender != null && gender == 2) ? 360 : 420;
        if (val > limit) {
            dc.getForbiddenCategories().add("海鲜(贝类/深海鱼)");
            dc.getForbiddenCategories().add("动物内脏(肝/腰)");
            dc.getForbiddenCategories().add("浓肉汤");
            dc.getStrategyTags().add("低嘌呤饮食");
            dc.getTrainingRisks().add("尿酸偏高：痛风发作期严禁运动，缓解期避免高强度跳跃等关节冲击动作");
            appendWarning(dc, "尿酸偏高");
        }
    }

    private void checkBloodSugar(Map<String, String> data, HealthConstraints dc) {
        double glucose = parseNumber(data.get("blood_glucose"));
        double hba1c = parseNumber(data.get("hba1c"));
        if (glucose > 6.1 || hba1c > 6.0) {
            dc.getForbiddenCategories().add("精制糖/甜点");
            dc.getForbiddenCategories().add("含糖饮料");
            dc.getForbiddenCategories().add("精白米面(白粥)");
            dc.getRecommendedElements().add("全谷物/杂豆");
            dc.getStrategyTags().add("低GI饮食");
            dc.getTrainingRisks().add("血糖偏高：严禁空腹进行高强度运动，运动前后注意监测血糖，随身携带糖果防止低血糖");
            appendWarning(dc, "血糖偏高");
        }
    }

    private void checkLipids(Map<String, String> data, HealthConstraints dc) {
        double tg = parseNumber(data.get("triglyceride"));
        double ldl = parseNumber(data.get("ldl"));
        if (tg > 1.7 || ldl > 3.4) {
            dc.getForbiddenCategories().add("油炸食品");
            dc.getForbiddenCategories().add("肥肉/鸡皮");
            dc.getForbiddenCategories().add("奶油/反式脂肪");
            dc.getStrategyTags().add("低脂低油");
            appendWarning(dc, "血脂/胆固醇异常");
        }
    }

    private void checkBloodPressure(Map<String, String> data, HealthConstraints dc) {
        String bpStr = data.get("blood_pressure");
        if (StringUtils.isBlank(bpStr)) return;
        Pattern p = Pattern.compile("(\\d{2,3})");
        Matcher m = p.matcher(bpStr);
        if (m.find()) {
            int sys = Integer.parseInt(m.group(1));
            if (sys >= 140) {
                dc.getForbiddenCategories().add("咸菜/腊肉");
                dc.getForbiddenCategories().add("高钠零食");
                dc.getStrategyTags().add("DASH饮食(限盐)");
                dc.getTrainingRisks().add("血压偏高：严禁大重量憋气动作（瓦尔萨尔瓦动作），避免头部低于心脏的体位（如倒立），推荐中低强度有氧");
                appendWarning(dc, "血压偏高");
            }
        }
    }

    private void appendWarning(HealthConstraints dc, String warning) {
        String current = StringUtils.defaultString(dc.getRiskWarning());
        if (!current.contains(warning)) {
            dc.setRiskWarning(current + " ⚠️" + warning + ";");
        }
    }

    private double parseNumber(String val) {
        if (StringUtils.isBlank(val)) return 0.0;
        try {
            Matcher m = Pattern.compile("(\\d+(\\.\\d+)?)").matcher(val);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception e) {
            // ignore
        }
        return 0.0;
    }
}