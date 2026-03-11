package com.lyz.service.impl;

import com.lyz.mapper.DietCheckinMapper;
import com.lyz.mapper.UserNutritionRecordMapper;
import com.lyz.model.dto.DietCheckinDTO;
import com.lyz.model.entity.DietCheckin;
import com.lyz.model.entity.UserNutritionRecord;
import com.lyz.model.vo.DietCheckinVO;
import com.lyz.service.DietCheckinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 饮食打卡服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietCheckinServiceImpl implements DietCheckinService {

    private final DietCheckinMapper dietCheckinMapper;
    private final UserNutritionRecordMapper userNutritionRecordMapper;

    /**
     * 等级 → 比例映射
     * 1=没吃(0%), 2=吃了一点(30%), 3=吃了一半(60%), 4=正常吃了(100%), 5=吃撑了(130%)
     */
    private static final Map<Integer, BigDecimal> LEVEL_RATIO = Map.of(
            1, new BigDecimal("0.00"),
            2, new BigDecimal("0.30"),
            3, new BigDecimal("0.60"),
            4, new BigDecimal("1.00"),
            5, new BigDecimal("1.30"));

    @Override
    public void checkin(Long userId, DietCheckinDTO dto) {
        // 1. 参数校验
        if (dto.getMealType() == null || dto.getMealType() < 1 || dto.getMealType() > 3) {
            throw new IllegalArgumentException("餐次必须为1(早餐)、2(午餐)或3(晚餐)");
        }
        if (dto.getLevel() == null || dto.getLevel() < 1 || dto.getLevel() > 5) {
            throw new IllegalArgumentException("执行等级必须在1-5之间");
        }

        BigDecimal ratio = LEVEL_RATIO.get(dto.getLevel());

        // 2. 保存打卡记录
        DietCheckin checkin = new DietCheckin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(LocalDate.now());
        checkin.setMealType(dto.getMealType());
        checkin.setLevel(dto.getLevel());
        checkin.setRatio(ratio);
        checkin.setCreatedAt(LocalDateTime.now());
        checkin.setUpdatedAt(LocalDateTime.now());

        dietCheckinMapper.insertOrUpdate(checkin);
        log.info("用户{}饮食打卡: 餐次={}, 等级={}, 比例={}", userId, dto.getMealType(), dto.getLevel(), ratio);

        // 3. 更新营养记录的实际摄入
        updateNutritionRecord(userId);
    }

    @Override
    public DietCheckinVO getTodayCheckin(Long userId) {
        List<DietCheckin> records = dietCheckinMapper.selectByUserIdAndDate(userId, LocalDate.now());

        DietCheckinVO vo = new DietCheckinVO();
        for (DietCheckin record : records) {
            DietCheckinVO.CheckinItem item = DietCheckinVO.CheckinItem.from(record.getLevel());
            switch (record.getMealType()) {
                case 1 -> vo.setBreakfast(item);
                case 2 -> vo.setLunch(item);
                case 3 -> vo.setDinner(item);
            }
        }
        return vo;
    }

    /**
     * 根据打卡数据更新 user_nutrition_record 的实际摄入
     */
    private void updateNutritionRecord(Long userId) {
        try {
            // 读取当日营养记录（AI生成计划时已写入推荐值）
            UserNutritionRecord record = userNutritionRecordMapper.selectByUserIdAndDate(userId, LocalDate.now());
            if (record == null || record.getTargetCalories() == null) {
                log.warn("用户{}今日无营养记录，跳过更新", userId);
                return;
            }

            // 读取今日所有打卡记录
            List<DietCheckin> checkins = dietCheckinMapper.selectByUserIdAndDate(userId, LocalDate.now());

            // 三餐比例（早:午:晚 = 30:40:30），与 RecommendationServiceImpl 保持一致
            BigDecimal[] mealRatios = {
                    new BigDecimal("0.30"), // 早餐
                    new BigDecimal("0.40"), // 午餐
                    new BigDecimal("0.30") // 晚餐
            };

            BigDecimal targetCal = record.getTargetCalories();
            BigDecimal actualTotal = BigDecimal.ZERO;

            // 默认每餐按100%（未打卡的餐次保持推荐值）
            BigDecimal[] checkinRatios = {
                    new BigDecimal("1.00"),
                    new BigDecimal("1.00"),
                    new BigDecimal("1.00")
            };

            // 用打卡数据覆盖
            for (DietCheckin c : checkins) {
                int idx = c.getMealType() - 1; // 1→0, 2→1, 3→2
                if (idx >= 0 && idx < 3) {
                    checkinRatios[idx] = c.getRatio();
                }
            }

            // 计算每餐实际热量
            BigDecimal breakfastActual = targetCal.multiply(mealRatios[0]).multiply(checkinRatios[0]).setScale(2,
                    RoundingMode.HALF_UP);
            BigDecimal lunchActual = targetCal.multiply(mealRatios[1]).multiply(checkinRatios[1]).setScale(2,
                    RoundingMode.HALF_UP);
            BigDecimal dinnerActual = targetCal.multiply(mealRatios[2]).multiply(checkinRatios[2]).setScale(2,
                    RoundingMode.HALF_UP);

            actualTotal = breakfastActual.add(lunchActual).add(dinnerActual);

            // 更新营养记录
            record.setBreakfastCalories(breakfastActual);
            record.setLunchCalories(lunchActual);
            record.setDinnerCalories(dinnerActual);
            record.setTotalCalories(actualTotal);
            record.setUpdatedAt(LocalDateTime.now());

            userNutritionRecordMapper.insertOrUpdate(record);
            log.info("用户{}营养记录已更新: 实际摄入={}kcal", userId, actualTotal);

        } catch (Exception e) {
            log.warn("更新营养记录失败: {}", e.getMessage());
        }
    }
}
