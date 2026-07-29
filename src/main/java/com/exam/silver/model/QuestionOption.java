package com.exam.silver.model;

/**
 * 選択肢1つ分のデータ（id: a, b, c... / text: 選択肢の文言）
 */
public class QuestionOption {

    private String id;
    private String text;

    public QuestionOption() {
    }

    public QuestionOption(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
