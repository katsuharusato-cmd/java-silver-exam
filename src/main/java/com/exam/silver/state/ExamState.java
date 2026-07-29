package com.exam.silver.state;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 受験者ごと（HTTPセッションごと）の試験状態を保持するBean。
 * ブラウザで画面遷移しても保持される「今どの問題に何を回答したか」などをここで管理する。
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExamState implements Serializable {

    /** 試験時間（分） */
    public static final int DURATION_MINUTES = 90;

    /** 出題順（問題idのリスト。開始時にシャッフルされ、以後固定） */
    private List<Integer> order;

    /** 各問題への回答（問題id -> 選択された選択肢idの集合） */
    private Map<Integer, Set<String>> answers = new LinkedHashMap<>();

    /** 「あとで見る」チェックが付いている問題id */
    private Set<Integer> markedForReview = new LinkedHashSet<>();

    private boolean started = false;
    private boolean finished = false;

    private long startTimeMillis;
    private long endTimeMillis;
    /** 試験終了時点の残り時間（ミリ秒）。終了後はここでタイマー表示を固定する */
    private long frozenRemainingMillis = -1;

    /** 試験結果をDBへ保存済みかどうか（結果ページの再読み込みで二重保存しないためのフラグ） */
    private boolean recorded = false;

    /** 問題一覧で「後で見る」だけに絞り込むかどうか */
    private boolean reviewFilterActive = false;

    /** 直近に表示していた問題の位置（「全問題レビュー」から「試験に戻る」で戻る先に使う） */
    private int lastPosition = 1;

    /** 各問題の選択肢の表示順（問題id -> 選択肢idの並び）。開始時にシャッフルされ、以後固定 */
    private Map<Integer, List<String>> optionOrder = new LinkedHashMap<>();

    public void start(List<Integer> order) {
        this.order = order;
        this.answers = new LinkedHashMap<>();
        this.markedForReview = new LinkedHashSet<>();
        this.started = true;
        this.finished = false;
        this.reviewFilterActive = false;
        this.recorded = false;
        this.frozenRemainingMillis = -1;
        this.lastPosition = 1;
        this.optionOrder = new LinkedHashMap<>();
        this.startTimeMillis = System.currentTimeMillis();
        this.endTimeMillis = this.startTimeMillis + DURATION_MINUTES * 60_000L;
    }

    public Set<String> getSelected(int questionId) {
        return answers.getOrDefault(questionId, new LinkedHashSet<>());
    }

    public void setSelected(int questionId, Set<String> selected) {
        if (selected == null || selected.isEmpty()) {
            answers.remove(questionId);
        } else {
            answers.put(questionId, selected);
        }
    }

    public boolean isAnswered(int questionId) {
        Set<String> s = answers.get(questionId);
        return s != null && !s.isEmpty();
    }

    public void setMarkedForReview(int questionId, boolean marked) {
        if (marked) {
            markedForReview.add(questionId);
        } else {
            markedForReview.remove(questionId);
        }
    }

    public boolean isMarkedForReview(int questionId) {
        return markedForReview.contains(questionId);
    }

    public long getRemainingMillis() {
        if (finished) {
            if (frozenRemainingMillis < 0) {
                frozenRemainingMillis = Math.max(endTimeMillis - System.currentTimeMillis(), 0);
            }
            return frozenRemainingMillis;
        }
        long remain = endTimeMillis - System.currentTimeMillis();
        return Math.max(remain, 0);
    }

    public boolean isTimeUp() {
        return started && !finished && System.currentTimeMillis() >= endTimeMillis;
    }

    public List<Integer> getOrder() {
        return order;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public boolean isReviewFilterActive() {
        return reviewFilterActive;
    }

    public void setReviewFilterActive(boolean reviewFilterActive) {
        this.reviewFilterActive = reviewFilterActive;
    }

    public boolean isRecorded() {
        return recorded;
    }

    public void setRecorded(boolean recorded) {
        this.recorded = recorded;
    }

    public int getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
    }

    public void setOptionOrder(int questionId, List<String> order) {
        optionOrder.put(questionId, order);
    }

    public List<String> getOptionOrder(int questionId) {
        return optionOrder.get(questionId);
    }

    public Map<Integer, Set<String>> getAnswers() {
        return answers;
    }

    public Set<Integer> getMarkedForReviewSet() {
        return markedForReview;
    }
}
