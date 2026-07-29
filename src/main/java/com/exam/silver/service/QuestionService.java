package com.exam.silver.service;

import com.exam.silver.model.Question;
import com.exam.silver.model.QuestionBank;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * src/main/resources/questions.yml を読み込み、アプリ起動中はメモリ上に保持するサービス。
 * （PostgreSQLへの同期はやめて、元のシンプルな方式に戻した）
 */
@Service
public class QuestionService {

    private static final String QUESTIONS_FILE = "questions.yml";

    private List<Question> questions;
    private Map<Integer, Question> questionById;

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource(QUESTIONS_FILE).getInputStream()) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            QuestionBank bank = mapper.readValue(in, QuestionBank.class);
            this.questions = bank.getQuestions();
            this.questionById = new LinkedHashMap<>();
            for (Question q : questions) {
                questionById.put(q.getId(), q);
            }
        } catch (Exception e) {
            throw new IllegalStateException("questions.yml の読み込みに失敗しました", e);
        }
    }

    public List<Question> getAllQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public Question getById(int id) {
        return questionById.get(id);
    }

    public int getTotalCount() {
        return questions.size();
    }
}
