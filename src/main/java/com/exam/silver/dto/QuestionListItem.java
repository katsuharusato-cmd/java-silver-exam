package com.exam.silver.dto;

/**
 * 「全問題レビュー」ページで1行分の問題を表すView用データ。
 */
public class QuestionListItem {

    private final int position;   // 出題順での番号（1-based）
    private final boolean answered;
    private final boolean markedForReview;
    private final Boolean correct; // 採点前は null

    public QuestionListItem(int position, boolean answered, boolean markedForReview, Boolean correct) {
        this.position = position;
        this.answered = answered;
        this.markedForReview = markedForReview;
        this.correct = correct;
    }

    public int getPosition() {
        return position;
    }

    public boolean isAnswered() {
        return answered;
    }

    public boolean isMarkedForReview() {
        return markedForReview;
    }

    public Boolean getCorrect() {
        return correct;
    }
}
