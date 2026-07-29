package com.exam.silver.dto;

/**
 * 結果ページで「分野別正答率」を表示するためのView用データ。
 */
public class CategoryStat {

    private final String categoryName;
    private final int correctCount;
    private final int totalCount;

    public CategoryStat(String categoryName, int correctCount, int totalCount) {
        this.categoryName = categoryName;
        this.correctCount = correctCount;
        this.totalCount = totalCount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPercentage() {
        return totalCount == 0 ? 0 : Math.round(correctCount * 100f / totalCount);
    }
}
