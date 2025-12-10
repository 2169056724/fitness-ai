package com.lyz.util;

import com.alibaba.fastjson2.JSONObject;
import com.lyz.config.MinioConfig;
import com.lyz.mapper.UserProfileMapper;
import com.lyz.model.entity.UserProfile;
import com.lyz.service.builder.MedicalContextBuilder;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileParserUtil {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final UserProfileMapper profileMapper;
    private final MedicalContextBuilder medicalContextBuilder;

    // 仅提取训练/饮食决策需要的核心健康指标
    private static final Pattern BLOOD_PRESSURE_PATTERN = Pattern.compile(
            "(?:(?:血压|BP|blood\\s*pressure|高压|低压)[：:\\s]*)?(\\d{2,3})\\s*[/-]\\s*(\\d{2,3})\\s*(?:mmHg|mm\\s*Hg|毫米汞柱)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BLOOD_GLUCOSE_PATTERN = Pattern.compile(
            "(?:血糖|GLU|glucose|空腹血糖|餐后血糖|随机血糖)[：:，,\\s\\-;()（）]*" +
                    "(?:[^\\d]{0,8})?" +
                    "(\\d+(?:\\.\\d+)?)\\s*(?:mmol[/\\s]*L|mg[/\\s]*dL|mM)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TRIGLYCERIDE_PATTERN = Pattern.compile(
            "(?:甘油三酯|TG|triglyceride|TRIG)[：:，,\\s\\-;()（）]*" +
                    "(?:[^\\d]{0,8})?" +
                    "(\\d+(?:\\.\\d+)?)\\s*(?:mmol[/\\s]*L|mg[/\\s]*dL)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LDL_PATTERN = Pattern.compile(
            "(?:低密度脂蛋白|LDL|低密度)[：:，,\\s\\-;()（）]*" +
                    "(?:[^\\d]{0,8})?" +
                    "(\\d+(?:\\.\\d+)?)\\s*(?:mmol[/\\s]*L|mg[/\\s]*dL)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern URIC_ACID_PATTERN = Pattern.compile(
            "(?:尿酸|UA|uric\\s*acid|URIC)[：:，,\\s\\-;()（）]*" +
                    "(?:[^\\d]{0,8})?" +
                    "(\\d+(?:\\.\\d+)?)\\s*(?:\\u00b5mol/L|umol/L|mg/dL|mg/dl)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CREATININE_PATTERN = Pattern.compile(
            "(?:肌酐|CREA|creatinine|Cr)[：:，,\\s\\-;()（）]*" +
                    "(?:[^\\d]{0,8})?" +
                    "(\\d+(?:\\.\\d+)?)\\s*(?:\\u00b5mol/L|umol/L|mg/dL|mg/dl)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HB_A1C_PATTERN = Pattern.compile(
            "(?:糖化血红蛋白|HbA1c|HbA1C|HBA1C)[：:，,\\s\\-;()（）]*" +
                    "(?:[^\\d]{0,8})?" +
                    "(\\d+(?:\\.\\d+)?)\\s*(?:%|％)?",
            Pattern.CASE_INSENSITIVE
    );

    @Async("fileParserExecutor")
    public void parseAndExtract(String objectKey, Long userId) {
        String tempFilePath = null;
        try {
            log.info("开始处理文件: objectKey={}, userId={}", objectKey, userId);

            String ext = getFileExtension(objectKey).toLowerCase();
            if (!"pdf".equals(ext) && !"doc".equals(ext) && !"docx".equals(ext)) {
                log.warn("文件类型不支持（仅 PDF/Word）: objectKey={}, ext={}", objectKey, ext);
                return;
            }

            tempFilePath = downloadFromMinio(objectKey);
            String text = extractText(tempFilePath, objectKey);

            if (text == null || text.trim().isEmpty()) {
                log.warn("提取文本为空: objectKey={}", objectKey);
                return;
            }

            String previewText = text.length() > 500 ? text.substring(0, 500) + "..." : text;
            log.info("文本提取完成: objectKey={}, length={}, preview={}", objectKey, text.length(), previewText);

            JSONObject medicalData = extractKeywords(text);
            String medicalJson = medicalData.toJSONString();

            // ==================================
            // 1. 准备更新对象
            UserProfile profileUpdate = new UserProfile();
            profileUpdate.setUserId(userId);
            profileUpdate.setExtractedMedicalData(medicalJson);
            profileUpdate.setMedicalReportPath(objectKey);
            profileUpdate.setUpdatedAt(LocalDateTime.now());

            // 2. 为了生成准确建议，需要知道用户性别
            Integer gender = 0; // 默认为未知
            try {
                UserProfile existingProfile = profileMapper.getByUserId(userId);
                if (existingProfile != null && existingProfile.getGender() != null) {
                    gender = existingProfile.getGender();
                }
            } catch (Exception e) {
                log.warn("解析文件时获取用户性别失败，将使用默认阈值: userId={}", userId);
            }

            // 3. 调用 Builder 生成提示词字符串
            String advicePrompt = medicalContextBuilder.generateMedicalAdvicePrompt(medicalJson, gender);
            profileUpdate.setMedicalAdvicePrompt(advicePrompt); // 设置缓存字段

            // 4. 落库
            profileMapper.upsertProfile(profileUpdate);
            log.info("体检报告解析并生成建议完成: userId={}, adviceLength={}", userId, advicePrompt.length());
        } catch (Exception e) {
            log.error("文件解析失败: objectKey={}, userId={}", objectKey, userId, e);
        } finally {
            if (tempFilePath != null) {
                try {
                    Files.deleteIfExists(Paths.get(tempFilePath));
                } catch (Exception e) {
                    log.warn("删除临时文件失败: {}", tempFilePath, e);
                }
            }
        }
    }

    private String downloadFromMinio(String objectKey) throws Exception {
        String ext = getFileExtension(objectKey);
        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFile = tempDir + File.separator + "medical_" + UUID.randomUUID() + "." + ext;

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectKey)
                        .build()
        );
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return tempFile;
    }

    private String extractText(String filePath, String objectKey) throws Exception {
        String ext = getFileExtension(objectKey).toLowerCase();

        switch (ext) {
            case "pdf":
                return extractFromPdf(filePath);
            case "doc":
            case "docx":
                return extractFromWord(filePath);
            default:
                log.warn("不支持的文件类型: {}", ext);
                return null;
        }
    }

    private String extractFromPdf(String filePath) {
        try {
            byte[] pdfBytes = Files.readAllBytes(Paths.get(filePath));
            try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(doc.getNumberOfPages());
                String text = stripper.getText(doc);
                if (text != null && !text.trim().isEmpty()) {
                    return text;
                }
                throw new RuntimeException("PDF 为扫描件，请上传电子版");
            }
        } catch (Exception e) {
            throw new RuntimeException("PDF 文本提取失败，请上传电子版: " + filePath, e);
        }
    }

    private String extractFromWord(String filePath) throws Exception {
        String ext = getFileExtension(filePath).toLowerCase();
        Path path = Paths.get(filePath);

        if ("docx".equals(ext)) {
            try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(path))) {
                StringBuilder text = new StringBuilder();
                for (XWPFParagraph paragraph : doc.getParagraphs()) {
                    String paraText = paragraph.getText();
                    if (paraText != null && !paraText.trim().isEmpty()) {
                        text.append(paraText).append("\n");
                    }
                }
                return text.toString();
            }
        } else if ("doc".equals(ext)) {
            try (InputStream is = Files.newInputStream(path);
                 HWPFDocument doc = new HWPFDocument(is);
                 WordExtractor extractor = new WordExtractor(doc)) {
                return extractor.getText();
            }
        } else {
            throw new RuntimeException("不支持的 Word 文件格式: " + ext);
        }
    }

    /**
     * 仅提取训练/饮食推荐需要的核心指标：blood_pressure、creatinine、uric_acid、blood_glucose、hba1c、triglyceride、ldl。
     */
    private JSONObject extractKeywords(String text) {
        JSONObject data = new JSONObject();
        int extractedCount = 0;

        String normalized = normalizeText(text);

        String cleanText = normalized
                .replaceAll("[，；;]", " ")
                .replaceAll("[：:\"`'、，]", "")
                .replaceAll("\\r\\n|\\r|\\n", " ")
                .replaceAll("\\s+", " ");
        log.debug("清洗后文本长度: {}", cleanText.length());

        Matcher bpMatcher = BLOOD_PRESSURE_PATTERN.matcher(cleanText);
        if (bpMatcher.find()) {
            try {
                Integer systolic = parseIntSafe(bpMatcher.group(1));
                Integer diastolic = parseIntSafe(bpMatcher.group(2));
                if (systolic != null && diastolic != null
                        && inRange(systolic, 70, 250)
                        && inRange(diastolic, 40, 150)) {
                    data.put("blood_pressure", systolic + "/" + diastolic + " mmHg");
                    extractedCount++;
                    log.debug("提取到血压: {}/{}", systolic, diastolic);
                }
            } catch (Exception e) {
                log.warn("血压解析失败", e);
            }
        }

        Matcher creaMatcher = CREATININE_PATTERN.matcher(cleanText);
        if (creaMatcher.find()) {
            try {
                Double value = parseDoubleSafe(creaMatcher.group(1));
                String unit = extractUnit(creaMatcher.group(0), "μmol/L");
                if (value != null && inRange(value, 30, 1500)) {
                    data.put("creatinine", trimZero(value) + " " + unit);
                    extractedCount++;
                    log.debug("提取到肌酐: {}", value);
                }
            } catch (Exception e) {
                log.warn("肌酐解析失败", e);
            }
        }

        Matcher uaMatcher = URIC_ACID_PATTERN.matcher(cleanText);
        if (uaMatcher.find()) {
            try {
                Double value = parseDoubleSafe(uaMatcher.group(1));
                String unit = extractUnit(uaMatcher.group(0), "μmol/L");
                if (value != null && inRange(value, 80, 1500)) {
                    data.put("uric_acid", trimZero(value) + " " + unit);
                    extractedCount++;
                    log.debug("提取到尿酸: {}", value);
                }
            } catch (Exception e) {
                log.warn("尿酸解析失败", e);
            }
        }

        Matcher bgMatcher = BLOOD_GLUCOSE_PATTERN.matcher(cleanText);
        if (bgMatcher.find()) {
            try {
                Double value = parseDoubleSafe(bgMatcher.group(1));
                String unit = extractUnit(bgMatcher.group(0), "mmol/L");
                if (value != null && inRange(value, 2.0, 40.0)) {
                    data.put("blood_glucose", trimZero(value) + " " + unit);
                    extractedCount++;
                    log.debug("提取到空腹血糖: {}", value);
                }
            } catch (Exception e) {
                log.warn("血糖解析失败", e);
            }
        }

        Matcher hba1cMatcher = HB_A1C_PATTERN.matcher(cleanText);
        if (hba1cMatcher.find()) {
            try {
                Double value = parseDoubleSafe(hba1cMatcher.group(1));
                if (value != null && inRange(value, 3.0, 20.0)) {
                    data.put("hba1c", trimZero(value) + " %");
                    extractedCount++;
                    log.debug("提取到 HbA1c: {}", value);
                }
            } catch (Exception e) {
                log.warn("HbA1c 解析失败", e);
            }
        }

        Matcher tgMatcher = TRIGLYCERIDE_PATTERN.matcher(cleanText);
        if (tgMatcher.find()) {
            try {
                Double value = parseDoubleSafe(tgMatcher.group(1));
                String unit = extractUnit(tgMatcher.group(0), "mmol/L");
                if (value != null && inRange(value, 0.1, 20.0)) {
                    data.put("triglyceride", trimZero(value) + " " + unit);
                    extractedCount++;
                    log.debug("提取到甘油三酯: {}", value);
                }
            } catch (Exception e) {
                log.warn("甘油三酯解析失败", e);
            }
        }

        Matcher ldlMatcher = LDL_PATTERN.matcher(cleanText);
        if (ldlMatcher.find()) {
            try {
                Double value = parseDoubleSafe(ldlMatcher.group(1));
                String unit = extractUnit(ldlMatcher.group(0), "mmol/L");
                if (value != null && inRange(value, 0.1, 15.0)) {
                    data.put("ldl", trimZero(value) + " " + unit);
                    extractedCount++;
                    log.debug("提取到 LDL: {}", value);
                }
            } catch (Exception e) {
                log.warn("LDL 解析失败", e);
            }
        }

        log.info("核心指标提取完成，共提取 {} 个字段", extractedCount);
        return data;
    }

    private String normalizeText(String raw) {
        if (raw == null) {
            return "";
        }
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        s = s.replace('\u2215', '/')
                .replace('\u2044', '/')
                .replace('\uFF0F', '/');
        s = s.replace('\u2212', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\uFF0D', '-');
        s = s.replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\u2060", "")
                .replace("\uFEFF", "")
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ');
        return s;
    }

    private String extractUnit(String matchedText, String defaultUnit) {
        if (matchedText.matches(".*mmol[/\\s]*L.*")) {
            return "mmol/L";
        } else if (matchedText.matches(".*mg[/\\s]*dL.*")) {
            return "mg/dL";
        } else if (matchedText.matches(".*(?:\\u00b5|μ|u)mol[/\\s]*L.*")) {
            return "μmol/L";
        } else if (matchedText.matches(".*%.*")) {
            return "%";
        }
        return defaultUnit;
    }

    private String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * 安全解析整数
     */
    private Integer parseIntSafe(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全解析小数，兼容逗号小数点
     */
    private Double parseDoubleSafe(String val) {
        try {
            return Double.parseDouble(val.replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    private boolean inRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * 去掉末尾无意义的 .0
     */
    private String trimZero(Double value) {
        if (value == null) {
            return "";
        }
        if (value % 1 == 0) {
            return String.valueOf(value.intValue());
        }
        return value.toString();
    }
}
