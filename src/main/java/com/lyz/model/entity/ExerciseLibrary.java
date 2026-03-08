package com.lyz.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动作知识库实体
 */
@Data
public class ExerciseLibrary {
    /** 主键 */
    private Long id;

    /** 标准动作名 */
    private String name;

    /** 别名/关键词(逗号分隔) */
    private String aliases;

    /** 分类: 热身/力量/有氧/拉伸/核心 */
    private String category;

    /** 主要肌群 */
    private String muscleGroup;

    /** GIF动图URL */
    private String gifUrl;

    /** 动作要点 */
    private String tips;

    /** 难度: 1初级/2中级/3高级 */
    private Integer difficulty;

    /** 所需器材 */
    private String equipment;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
