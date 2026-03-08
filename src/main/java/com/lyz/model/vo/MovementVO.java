package com.lyz.model.vo;

import lombok.Data;

/**
 * 富动作 VO — 包含 GIF 演示和动作指导信息
 * 替代原有的纯字符串动作格式
 */
@Data
public class MovementVO {
    /** 动作名称 */
    private String name;

    /** 组数/次数 (如 "3组x12次") */
    private String detail;

    /** GIF 动图 URL */
    private String gifUrl;

    /** 动作要点 */
    private String tips;

    /** 主要肌群 */
    private String muscleGroup;

    /** 所需器材 */
    private String equipment;
}
