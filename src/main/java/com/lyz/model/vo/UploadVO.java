package com.lyz.model.vo;

import lombok.Data;

@Data
public class UploadVO {
    private String type;          // avatar | report
    private String url;           // 访问URL（直链）
}


