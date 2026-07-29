package com.exam.silver.dto;

/**
 * 問題ページで1つの選択肢を描画するためのView用データ。
 */
public class OptionView {

    private final String id;
    private final String text;
    private final boolean checked;
    private final boolean correct;
    /** 採点結果表示時：選んだのに不正解だった選択肢か */
    private final boolean wrongSelection;

    public OptionView(String id, String text, boolean checked, boolean correct, boolean wrongSelection) {
        this.id = id;
        this.text = text;
        this.checked = checked;
        this.correct = correct;
        this.wrongSelection = wrongSelection;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public boolean isChecked() {
        return checked;
    }

    public boolean isCorrect() {
        return correct;
    }

    public boolean isWrongSelection() {
        return wrongSelection;
    }
}
