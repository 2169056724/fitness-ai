package com.lyz.service;

import com.lyz.mapper.ExerciseLibraryMapper;
import com.lyz.model.entity.ExerciseLibrary;
import com.lyz.model.vo.MovementVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动作增强服务
 * 将 AI 输出的纯文本动作列表匹配到动作知识库，补充 GIF/要点/肌群等指导信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseEnrichService {

    private final ExerciseLibraryMapper exerciseLibraryMapper;

    /** 内存中的动作库 (启动时加载, 数据量约50-100条, 很小) */
    private List<ExerciseLibrary> libraryCache = Collections.emptyList();

    /** name -> ExerciseLibrary 精确索引 */
    private Map<String, ExerciseLibrary> nameIndex = Collections.emptyMap();

    /** alias -> ExerciseLibrary 别名索引 */
    private Map<String, ExerciseLibrary> aliasIndex = Collections.emptyMap();

    /**
     * 应用启动时加载全部动作到内存
     */
    @PostConstruct
    public void init() {
        try {
            List<ExerciseLibrary> all = exerciseLibraryMapper.selectAll();
            if (all == null || all.isEmpty()) {
                log.warn("动作知识库为空，请检查 exercise_library 表是否已初始化");
                return;
            }

            this.libraryCache = all;

            // 构建精确名称索引
            Map<String, ExerciseLibrary> nIdx = new HashMap<>();
            Map<String, ExerciseLibrary> aIdx = new HashMap<>();

            for (ExerciseLibrary ex : all) {
                nIdx.put(ex.getName().toLowerCase(), ex);

                // 解析别名
                if (ex.getAliases() != null && !ex.getAliases().isBlank()) {
                    String[] aliases = ex.getAliases().split("[,，]");
                    for (String alias : aliases) {
                        String trimmed = alias.trim().toLowerCase();
                        if (!trimmed.isEmpty()) {
                            aIdx.put(trimmed, ex);
                        }
                    }
                }
            }

            this.nameIndex = nIdx;
            this.aliasIndex = aIdx;

            log.info("动作知识库加载完成: {}条动作, {}个别名", all.size(), aIdx.size());
        } catch (Exception e) {
            log.warn("动作知识库加载失败(表可能不存在): {}", e.getMessage());
        }
    }

    /**
     * 手动刷新缓存 — 当数据库中的 gif_url 等字段更新后调用
     */
    public void refreshCache() {
        log.info("手动刷新动作知识库缓存...");
        init();
    }

    /**
     * 增强动作列表：为每个动作匹配知识库中的 GIF、要点等信息
     *
     * @param rawMovements AI 输出的动作列表 (MovementVO，可能只有 name+detail)
     * @return 增强后的动作列表
     */
    public List<MovementVO> enrichMovements(List<MovementVO> rawMovements) {
        if (rawMovements == null || rawMovements.isEmpty()) {
            return rawMovements;
        }

        List<MovementVO> enriched = new ArrayList<>(rawMovements.size());

        for (MovementVO mv : rawMovements) {
            String movementName = mv.getName();
            if (movementName == null || movementName.isBlank()) {
                enriched.add(mv);
                continue;
            }

            // 三级匹配
            ExerciseLibrary matched = matchExercise(movementName.trim());

            if (matched != null) {
                // 用知识库数据填充（仅填充空字段，不覆盖 AI 已有的内容）
                if (mv.getGifUrl() == null || mv.getGifUrl().isBlank()) {
                    mv.setGifUrl(matched.getGifUrl());
                }
                if (mv.getTips() == null || mv.getTips().isBlank()) {
                    mv.setTips(matched.getTips());
                }
                if (mv.getMuscleGroup() == null || mv.getMuscleGroup().isBlank()) {
                    mv.setMuscleGroup(matched.getMuscleGroup());
                }
                if (mv.getEquipment() == null || mv.getEquipment().isBlank()) {
                    mv.setEquipment(matched.getEquipment());
                }
            }

            enriched.add(mv);
        }

        return enriched;
    }

    /**
     * 三级匹配算法
     * 1. 精确匹配 name
     * 2. 别名 aliases 匹配
     * 3. 模糊匹配 (库中名称包含在输入中, 或输入包含在库中名称中)
     */
    private ExerciseLibrary matchExercise(String inputName) {
        String lower = inputName.toLowerCase();

        // Level 1: 精确匹配
        ExerciseLibrary exact = nameIndex.get(lower);
        if (exact != null) {
            return exact;
        }

        // Level 2: 别名匹配
        ExerciseLibrary aliasMatch = aliasIndex.get(lower);
        if (aliasMatch != null) {
            return aliasMatch;
        }

        // Level 3: 模糊匹配 — 输入包含库中标准名，或库中标准名包含输入
        // 例: 输入 "杠铃深蹲" 包含库中的 "深蹲"
        ExerciseLibrary bestFuzzy = null;
        int bestLen = 0;

        for (ExerciseLibrary lib : libraryCache) {
            String libName = lib.getName().toLowerCase();

            // 输入中包含标准名 (优先匹配更长的标准名)
            if (lower.contains(libName) && libName.length() > bestLen) {
                bestFuzzy = lib;
                bestLen = libName.length();
            }
        }

        if (bestFuzzy != null) {
            return bestFuzzy;
        }

        // 也检查别名的模糊匹配
        for (Map.Entry<String, ExerciseLibrary> entry : aliasIndex.entrySet()) {
            if (lower.contains(entry.getKey()) && entry.getKey().length() > bestLen) {
                bestFuzzy = entry.getValue();
                bestLen = entry.getKey().length();
            }
        }

        return bestFuzzy;
    }
}
