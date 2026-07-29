package com.exam.silver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * 1回分の受験結果。Slackでログインしたユーザーごとに履歴として保存される。
 */
@Entity
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Slackユーザーを一意に識別するID（OpenID Connectの "sub"） */
    private String userId;

    /** 表示用のユーザー名（Slackの表示名） */
    private String userName;

    /** ユーザーのメールアドレス（取得できた場合） */
    private String userEmail;

    private int correctCount;
    private int totalCount;
    private int percentage;

    /** 分野別の正答数・問題数をJSON文字列で保存したもの（例: {"1":[8,10],"2":[5,6]}）。
     *  受験履歴ページで分野別グラフを表示するために使う。 */
    @Column(columnDefinition = "TEXT")
    private String categoryBreakdown;

    private LocalDateTime takenAt;

    public ExamResult() {
    }

    public ExamResult(String userId, String userName, String userEmail,
                       int correctCount, int totalCount, int percentage,
                       String categoryBreakdown, LocalDateTime takenAt) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.correctCount = correctCount;
        this.totalCount = totalCount;
        this.percentage = percentage;
        this.categoryBreakdown = categoryBreakdown;
        this.takenAt = takenAt;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public String getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(String categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }
}
