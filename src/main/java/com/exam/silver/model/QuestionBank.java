package com.exam.silver.model;

import java.util.ArrayList;
import java.util.List;

/**
 * questions.yml のルート要素に対応するクラス。
 */
public class QuestionBank {

    private List<Question> questions = new ArrayList<>();

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}
