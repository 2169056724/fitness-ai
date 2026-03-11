package com.lyz.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食打卡记录实体
 */
@Data
public class DietCheckin {
    private Long id;
    private Long userId;
    private LocalDate checkinDate;

    /**
     * 餐次: 1=早餐 2=午餐 3=晚餐
     */
    private Integer mealType;

    /**
     * 执行等级: 1=没吃 2=吃了一点 3=吃了一半左右 4=正常吃了 5=吃撑了
     */
    private Integer level;

    /**
     * 对应比例: 0.00/0.30/0.60/1.00/1.30
     */
    private BigDecimal ratio;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
