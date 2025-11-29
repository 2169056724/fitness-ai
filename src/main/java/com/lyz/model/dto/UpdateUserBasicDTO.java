package com.lyz.model.dto;

import lombok.Data;

@Data
public class UpdateUserBasicDTO {

    private String nickname;

    //Minio URL
    private String avatar;
}
