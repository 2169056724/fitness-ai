package com.lyz.model.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 体重记录请求DTO
 */
@Data
public class WeightLogDTO {

    /**
     * 体重(kg) - 必填
     */
    @NotNull(message = "体重不能为空")
    @DecimalMin(value = "20.0", message = "体重不能低于20kg")
    @DecimalMax(value = "300.0", message = "体重不能超过300kg")
    private BigDecimal weightKg;

    /**
     * 记录日期 - 可选，默认当天
     */
    private String recordDate;
}
