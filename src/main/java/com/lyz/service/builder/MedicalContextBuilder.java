package com.lyz.service.builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.model.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 生成训练/饮食推荐用的医疗上下文（病史 + 指标 + 风险建议）的构建器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalContextBuilder {

    private final ObjectMapper objectMapper;

    /**
     * 【新方法】核心逻辑：仅根据 JSON 数据和性别生成医疗建议提示词
     * 这个方法将被 FileParserUtil 和 UserProfileService 调用并存库
     */
    public String generateMedicalAdvicePrompt(String extractedMedicalData, Integer gender) {
        if (StringUtils.isBlank(extractedMedicalData)) {
            return "";
        }

        // 1. 解析 JSON
        Map<String, String> medicalData = parseMedicalData(extractedMedicalData);

        // 2. 生成建议
        String advice = buildMedicalAdvice(medicalData, gender);

        if (StringUtils.isBlank(advice)) {
            return "";
        }

        // 3. 格式化返回
        return "- Medical risk & advice: " + advice;
    }


    /**
     * 推荐服务调用此方法构建完整 Context
     * 现在它优先读取数据库中缓存的字段
     */
    public String build(UserProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator()).append("【病史与体检】").append(System.lineSeparator());
        sb.append("- 病史: ").append(StringUtils.defaultIfBlank(profile.getMedicalHistory(), "无")).append(System.lineSeparator());

        // 简单展示提取的原始指标
        sb.append("- 体检指标: ").append(extractMedicalIndicators(profile.getExtractedMedicalData())).append(System.lineSeparator());

        // 直接使用数据库中存储的预计算 Prompt
        // 如果数据库里有，直接用；如果没有（旧数据），才实时计算兜底
        if (StringUtils.isNotBlank(profile.getMedicalAdvicePrompt())) {
            sb.append(profile.getMedicalAdvicePrompt()).append(System.lineSeparator());
        } else {
            // 兜底：实时计算
            String realtimeAdvice = generateMedicalAdvicePrompt(profile.getExtractedMedicalData(), profile.getGender());
            if (StringUtils.isNotBlank(realtimeAdvice)) {
                sb.append(realtimeAdvice).append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    private Map<String, String> parseMedicalData(String raw) {
        if (StringUtils.isBlank(raw)) return Collections.emptyMap();
        try {
            Map<String, String> parsed = objectMapper.readValue(raw.trim(), new TypeReference<Map<String, String>>() {
            });
            Map<String, String> cleaned = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : parsed.entrySet()) {
                if (entry.getKey() == null) continue;
                String key = entry.getKey().trim();
                String value = entry.getValue() == null ? "" : entry.getValue().trim();
                cleaned.put(key, value);
            }
            return cleaned;
        } catch (Exception e) {
            log.debug("failed to parse medical data from profile, fallback to empty", e);
            return Collections.emptyMap();
        }
    }

    private String buildMedicalAdvice(Map<String, String> medicalData, Integer gender) {
        if (medicalData == null || medicalData.isEmpty()) return "";
        List<String> advice = new ArrayList<>();
        boolean isFemale = Integer.valueOf(2).equals(gender);

        for (Map.Entry<String, String> entry : medicalData.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
            String value = StringUtils.defaultString(entry.getValue()).trim();
            if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) continue;

            switch (key) {
                case "ldl", "ldl-c", "ldlc" -> {
                    Optional<Double> ldlOpt = parseFirstNumber(value);
                    if (ldlOpt.isPresent()) {
                        double ldlVal = ldlOpt.get();
                        String lowerVal = value.toLowerCase(Locale.ROOT);
                        if (lowerVal.contains("mg") || ldlVal > 20) {
                            ldlVal = ldlVal / 38.67;
                        }
                        if (ldlVal >= 4.1) {
                            advice.add("LDL " + value + ": high - limit saturated/fried foods, add fiber, >150min/week cardio.");
                        } else if (ldlVal >= 3.4) {
                            advice.add("LDL " + value + ": borderline - reduce red meat/cream, more fish and whole grains, steady cardio.");
                        } else {
                            advice.add("LDL " + value + ": ok - keep low-sat-fat/high-fiber, avoid trans fat.");
                        }
                    }
                }
                case "hba1c", "hemoglobin_a1c", "a1c" -> {
                    Optional<Double> a1cOpt = parseFirstNumber(value);
                    if (a1cOpt.isPresent()) {
                        double a1c = a1cOpt.get();
                        if (a1c >= 6.5) {
                            advice.add("HbA1c " + value + ": high - low-GI carbs, no sugary drinks/late snacks; avoid fasted high-intensity.");
                        } else if (a1c >= 5.7) {
                            advice.add("HbA1c " + value + ": borderline - control refined carbs, walk 10-15 min post-meal, use moderate cardio + strength.");
                        } else {
                            advice.add("HbA1c " + value + ": ok - keep balanced carb timing and regular sleep.");
                        }
                    }
                }
                case "uric_acid", "ua" -> {
                    Optional<Double> uaOpt = parseFirstNumber(value);
                    if (uaOpt.isPresent()) {
                        double ua = uaOpt.get();
                        String lowerVal = value.toLowerCase(Locale.ROOT);
                        if (lowerVal.contains("mg") || ua < 50) {
                            ua = ua * 59.48;
                        }
                        double high = isFemale ? 360 : 420;
                        double warn = isFemale ? 320 : 380;
                        if (ua >= high) {
                            advice.add("Uric acid " + value + ": high - limit purine foods (organ meat/seafood/beer), drink >2L water, avoid heavy straining lifts.");
                        } else if (ua >= warn) {
                            advice.add("Uric acid " + value + ": slightly high - reduce red meat/beer, hydrate and favor light-moderate cardio.");
                        } else {
                            advice.add("Uric acid " + value + ": ok - keep hydration and limit heavy purine meals.");
                        }
                    }
                }
                case "creatinine", "cr" -> {
                    Optional<Double> crOpt = parseFirstNumber(value);
                    if (crOpt.isPresent()) {
                        double cr = crOpt.get();
                        String lowerVal = value.toLowerCase(Locale.ROOT);
                        if (lowerVal.contains("mg") || cr < 20) {
                            cr = cr * 88.4;
                        }
                        double high = isFemale ? 90 : 110;
                        double warn = isFemale ? 85 : 100;
                        if (cr >= high) {
                            advice.add("Creatinine " + value + ": high - avoid very high protein or dehydration; keep training moderate and monitor kidney load.");
                        } else if (cr >= warn) {
                            advice.add("Creatinine " + value + ": near upper limit - stay hydrated and avoid long breath-hold heavy sets.");
                        } else {
                            advice.add("Creatinine " + value + ": ok - keep hydration and balanced protein.");
                        }
                    }
                }
                case "triglyceride", "tg", "triglycerides" -> {
                    Optional<Double> tgOpt = parseFirstNumber(value);
                    if (tgOpt.isPresent()) {
                        double tg = tgOpt.get();
                        String lowerVal = value.toLowerCase(Locale.ROOT);
                        if (lowerVal.contains("mg") || tg > 20) {
                            tg = tg / 88.57;
                        }
                        if (tg >= 2.3) {
                            advice.add("Triglyceride " + value + ": high - cut fried/sugary/alcohol, control dinner carbs, 150-210 min cardio weekly.");
                        } else if (tg >= 1.7) {
                            advice.add("Triglyceride " + value + ": slightly high - lower sugar/alcohol, add vegetables and fiber.");
                        } else {
                            advice.add("Triglyceride " + value + ": ok - keep carbs split through the day, avoid heavy late-night fat/sugar.");
                        }
                    }
                }
                case "blood_glucose", "glucose", "fasting_glucose" -> {
                    Optional<Double> gluOpt = parseFirstNumber(value);
                    if (gluOpt.isPresent()) {
                        double glu = gluOpt.get();
                        String lowerVal = value.toLowerCase(Locale.ROOT);
                        if (lowerVal.contains("mg") || glu > 20) {
                            glu = glu / 18.0;
                        }
                        if (glu >= 7.0) {
                            advice.add("Blood glucose " + value + ": high (fasting assumed) - low-GI split meals, avoid fasted high-intensity, walk 10-15 min after meals.");
                        } else if (glu >= 6.1) {
                            advice.add("Blood glucose " + value + ": slightly high - cut sweet drinks/late snacks, post-meal activity, keep moderate intensity.");
                        } else {
                            advice.add("Blood glucose " + value + ": ok - keep stable carb intake and meal schedule.");
                        }
                    }
                }
                case "blood_pressure", "bp" -> {
                    Optional<double[]> bpOpt = parseBloodPressure(value);
                    if (bpOpt.isPresent()) {
                        double[] bp = bpOpt.get();
                        double sys = bp[0];
                        double dia = bp[1];
                        if (sys >= 140 || dia >= 90) {
                            advice.add("Blood pressure " + value + ": high - reduce sodium (<5g salt/day), avoid heavy breath-hold lifts, favor moderate cardio.");
                        } else if (sys >= 130 || dia >= 80) {
                            advice.add("Blood pressure " + value + ": elevated - train in moderate heart-rate zone and avoid back-to-back heavy sets.");
                        } else {
                            advice.add("Blood pressure " + value + ": ok - normal training with full warm-up and salt control.");
                        }
                    }
                }
                default -> {
                }
            }
        }
        if (advice.isEmpty()) return "";
        String summary = String.join("; ", advice);
        return summary.length() > 600 ? summary.substring(0, 600) + "..." : summary;
    }

    private Optional<Double> parseFirstNumber(String text) {
        if (StringUtils.isBlank(text)) return Optional.empty();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(text.replace(",", "."));
        if (matcher.find()) {
            try {
                return Optional.of(Double.parseDouble(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<double[]> parseBloodPressure(String text) {
        if (StringUtils.isBlank(text)) return Optional.empty();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([0-9]{2,3})\\s*/\\s*([0-9]{2,3})").matcher(text);
        if (matcher.find()) {
            try {
                double sys = Double.parseDouble(matcher.group(1));
                double dia = Double.parseDouble(matcher.group(2));
                return Optional.of(new double[]{sys, dia});
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        java.util.regex.Matcher fallback = java.util.regex.Pattern.compile("([0-9]{2,3})").matcher(text);
        List<Double> numbers = new ArrayList<>();
        while (fallback.find() && numbers.size() < 2) {
            try {
                numbers.add(Double.parseDouble(fallback.group(1)));
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        if (numbers.size() == 2) {
            return Optional.of(new double[]{numbers.get(0), numbers.get(1)});
        }
        return Optional.empty();
    }

    private String extractMedicalIndicators(String raw) {
        if (StringUtils.isBlank(raw)) return "��";
        String trimmed = raw.trim();
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            String compact = objectMapper.writeValueAsString(node);
            return compact.length() > 400 ? compact.substring(0, 400) + "..." : compact;
        } catch (JsonProcessingException e) {
            return trimmed.length() > 400 ? trimmed.substring(0, 400) + "..." : trimmed;
        }
    }
}


