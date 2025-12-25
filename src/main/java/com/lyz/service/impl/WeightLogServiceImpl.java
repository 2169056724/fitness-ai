package com.lyz.service.impl;

import com.lyz.mapper.UserWeightLogMapper;
import com.lyz.model.dto.WeightLogDTO;
import com.lyz.model.entity.UserWeightLog;
import com.lyz.service.WeightLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 体重日志服务实现
 */
@Slf4j
@Service
public class WeightLogServiceImpl implements WeightLogService {

    @Autowired
    private UserWeightLogMapper weightLogMapper;

    @Override
    public UserWeightLog logWeight(Long userId, WeightLogDTO dto) {
        // 解析日期，默认当天
        LocalDate recordDate;
        if (dto.getRecordDate() != null && !dto.getRecordDate().isEmpty()) {
            recordDate = LocalDate.parse(dto.getRecordDate(), DateTimeFormatter.ISO_DATE);
        } else {
            recordDate = LocalDate.now();
        }

        // 构建实体
        UserWeightLog record = new UserWeightLog();
        record.setUserId(userId);
        record.setRecordDate(recordDate);
        record.setWeightKg(dto.getWeightKg());

        // 使用insertOrUpdate，同一天覆盖
        weightLogMapper.insertOrUpdate(record);

        // 返回完整记录
        log.info("用户{}记录体重: {} kg, 日期: {}", userId, dto.getWeightKg(), recordDate);
        return weightLogMapper.selectByUserIdAndDate(userId, recordDate);
    }

    @Override
    public List<UserWeightLog> getRecentWeightLogs(Long userId, int days) {
        if (days <= 0) {
            days = 30;
        }
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        return weightLogMapper.selectByUserIdAndDateRange(userId, startDate, endDate);
    }

    @Override
    public UserWeightLog getLatestWeight(Long userId) {
        return weightLogMapper.selectLatestByUserId(userId);
    }
}
