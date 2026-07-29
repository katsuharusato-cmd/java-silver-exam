package com.exam.silver.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 問題1問分のデータ。questions.yml から読み込まれ、アプリ起動中はメモリ上に保持される
 * （PostgreSQLへの同期はやめて、元のシンプルな方式に戻した）。
 */
public class Question {

    private int id;

    /**
     * 分野（数値）。将来、分野ごとの出題比率を実際のJava Silver試験に
     * 準拠させるために使う。番号と分野名の対応は
     * {@link com.exam.silver.model.ExamCategory} を参照。
     */
    private int category;

    /** 問題文（設問の説明） */
    private String description;

    /** ソースコード（無い問題は null のままでOK） */
    private String code;

    /** 選択肢一覧 */
    private List<QuestionOption> options = new ArrayList<>();

    /** 正解の選択肢id一覧（複数可） */
    private List<String> answers = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<QuestionOption> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOption> options) {
        this.options = options;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }

    public boolean isMultipleAnswer() {
        return answers != null && answers.size() > 1;
    }
}
