package com.lyz.model.dto;

import lombok.Data;

/**
 * 饮食打卡请求DTO
 */
@Data
public class DietCheckinDTO {
    /**
     * 餐次: 1=早餐 2=午餐 3=晚餐
     */
    private Integer mealType;

    /**
     * 执行等级: 1=没吃 2=吃了一点 3=吃了一半左右 4=正常吃了 5=吃撑了
     */
    private Integer level;
}
