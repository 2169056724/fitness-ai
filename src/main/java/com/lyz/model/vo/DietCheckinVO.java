package com.lyz.model.vo;

import lombok.Data;

/**
 * 饮食打卡响应VO
 * 返回今日三餐的打卡状态
 */
@Data
public class DietCheckinVO {
    private CheckinItem breakfast;
    private CheckinItem lunch;
    private CheckinItem dinner;

    @Data
    public static class CheckinItem {
        /**
         * 执行等级: 1=没吃 2=吃了一点 3=吃了一半左右 4=正常吃了 5=吃撑了
         */
        private Integer level;

        /**
         * 等级标签
         */
        private String label;

        /**
         * 等级 emoji
         */
        private String emoji;

        public static CheckinItem from(Integer level) {
            if (level == null)
                return null;
            CheckinItem item = new CheckinItem();
            item.setLevel(level);
            switch (level) {
                case 1 -> {
                    item.setLabel("没吃");
                    item.setEmoji("🚫");
                }
                case 2 -> {
                    item.setLabel("吃了一点");
                    item.setEmoji("🍃");
                }
                case 3 -> {
                    item.setLabel("吃了一半左右");
                    item.setEmoji("🍽️");
                }
                case 4 -> {
                    item.setLabel("正常吃了");
                    item.setEmoji("✅");
                }
                case 5 -> {
                    item.setLabel("吃撑了");
                    item.setEmoji("🫄");
                }
                default -> {
                    item.setLabel("未知");
                    item.setEmoji("❓");
                }
            }
            return item;
        }
    }
}
