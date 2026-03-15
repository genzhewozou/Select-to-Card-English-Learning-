package com.english.learn.util;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 艾宾浩斯遗忘曲线工具类。
 * 根据复习次数与熟练度计算下次复习间隔（分钟），进而得到 next_review_at。
 * 间隔策略：熟练度越高、复习次数越多，间隔越长。
 */
public final class EbbinghausUtil {

    private EbbinghausUtil() {
    }

    /** 熟练度 1-5 对应的基础间隔（分钟）：1 最不熟，5 最熟 */
    private static final int[] BASE_INTERVAL_MINUTES = { 30, 60, 120, 360, 720 };

    /**
     * 根据复习次数与熟练度计算下次复习时间。
     *
     * @param reviewCount      已复习次数（本次提交后已 +1）
     * @param proficiencyLevel 熟练度 1-5
     * @return 下次复习时间
     */
    public static LocalDateTime nextReviewAt(int reviewCount, int proficiencyLevel) {
        int level = Math.max(1, Math.min(5, proficiencyLevel));
        int baseMinutes = BASE_INTERVAL_MINUTES[level - 1];
        // 复习次数越多，间隔按倍数增加（简化版：每次约 1.5 倍）
        double multiplier = Math.pow(1.5, Math.min(reviewCount - 1, 10));
        int totalMinutes = (int) (baseMinutes * multiplier);
        totalMinutes = Math.min(totalMinutes, 60 * 24 * 365); // 最多一年
        return LocalDateTime.now().plusMinutes(totalMinutes);
    }
}
