package com.exam.silver.repository;

import com.exam.silver.model.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    List<ExamResult> findByUserIdOrderByTakenAtDesc(String userId);
}
