package com.lyz.mapper;

import com.lyz.model.entity.ExerciseLibrary;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 动作知识库Mapper
 */
@Mapper
public interface ExerciseLibraryMapper {

    /**
     * 查询全部动作
     */
    List<ExerciseLibrary> selectAll();
}
