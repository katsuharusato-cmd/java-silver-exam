package com.exam.silver.controller;

import com.exam.silver.dto.CategoryStat;
import com.exam.silver.dto.OptionView;
import com.exam.silver.dto.QuestionListItem;
import com.exam.silver.model.ExamCategory;
import com.exam.silver.model.ExamResult;
import com.exam.silver.model.Question;
import com.exam.silver.model.QuestionOption;
import com.exam.silver.repository.ExamResultRepository;
import com.exam.silver.service.QuestionService;
import com.exam.silver.state.ExamState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping
public class ExamController {

    private final QuestionService questionService;
    private final ExamState examState;
    private final ExamResultRepository examResultRepository;
    private final boolean demoLoginEnabled;
    private final ObjectMapper objectMapper;

    public ExamController(QuestionService questionService, ExamState examState,
                           ExamResultRepository examResultRepository,
                           @org.springframework.beans.factory.annotation.Value("${app.demo-login-enabled:true}") boolean demoLoginEnabled,
                           ObjectMapper objectMapper) {
        this.questionService = questionService;
        this.examState = examState;
        this.examResultRepository = examResultRepository;
        this.demoLoginEnabled = demoLoginEnabled;
        this.objectMapper = objectMapper;
    }

    /**
     * すべての画面共通でログイン状態・ユーザー名をモデルに追加する
     * （ヘッダー表示や、スタートページのログインボタン出し分けに使う）。
     */
    @ModelAttribute
    public void addUserInfo(Model model, @AuthenticationPrincipal OAuth2User principal) {
        model.addAttribute("authenticated", principal != null);
        model.addAttribute("userName", principal != null ? nameOf(principal) : null);
        model.addAttribute("demoLoginEnabled", demoLoginEnabled);
    }

    // ===================== スタートページ =====================

    @GetMapping("/")
    public String start(jakarta.servlet.http.HttpServletRequest request, Model model) {
        Object ex = request.getSession()
                .getAttribute(org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION);
        if (ex instanceof Exception e) {
            model.addAttribute("loginErrorMessage", e.getMessage());
            model.addAttribute("loginErrorType", e.getClass().getSimpleName());
            request.getSession().removeAttribute(
                    org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION);
        }
        return "start";
    }

    @PostMapping("/exam/start")
    public String startExam() {
        List<Integer> order = new ArrayList<>();
        for (Question q : questionService.getAllQuestions()) {
            order.add(q.getId());
        }
        Collections.shuffle(order);
        examState.start(order);

        // 選択肢の表示順も試験開始のたびにランダム化し、以後（この回の試験中は）固定する
        for (Question q : questionService.getAllQuestions()) {
            List<String> optionIds = q.getOptions().stream()
                    .map(QuestionOption::getId)
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(optionIds);
            examState.setOptionOrder(q.getId(), optionIds);
        }

        return "redirect:/exam/question/1";
    }

    @PostMapping("/exam/restart")
    public String restartExam() {
        return startExam();
    }

    // ===================== 受験履歴（＝直近の結果を、結果ページと同じ形で見られるページ） =====================

    @GetMapping("/exam/history")
    public String history(Model model, @AuthenticationPrincipal OAuth2User principal) {
        List<ExamResult> pastResults = principal != null
                ? examResultRepository.findByUserIdOrderByTakenAtDesc(principal.getName())
                : List.of();

        model.addAttribute("hasHistory", !pastResults.isEmpty());
        if (pastResults.isEmpty()) {
            return "history";
        }

        ExamResult latest = pastResults.get(0);
        List<CategoryStat> categoryStats = parseCategoryBreakdown(latest.getCategoryBreakdown());
        List<ExamResult> trend = buildTrend(pastResults);
        Integer previousPercentage = pastResults.size() > 1 ? pastResults.get(1).getPercentage() : null;

        model.addAttribute("total", latest.getTotalCount());
        model.addAttribute("correctCount", latest.getCorrectCount());
        model.addAttribute("percentage", latest.getPercentage());
        model.addAttribute("takenAt", latest.getTakenAt());
        model.addAttribute("categoryStats", categoryStats);
        model.addAttribute("pastResults", pastResults);
        model.addAttribute("trend", trend);
        model.addAttribute("previousPercentage", previousPercentage);
        return "history";
    }

    // ===================== 問題ページ =====================

    @GetMapping("/exam/question/{pos}")
    public String question(@PathVariable int pos, Model model,
                            @AuthenticationPrincipal OAuth2User principal) {
        if (!examState.isStarted()) {
            return "redirect:/";
        }
        if (maybeAutoFinish(principal)) {
            return "redirect:/exam/result";
        }
        int total = examState.getOrder().size();
        pos = clamp(pos, 1, total);
        examState.setLastPosition(pos);

        int questionId = examState.getOrder().get(pos - 1);
        Question q = questionService.getById(questionId);

        buildQuestionModel(model, q, pos, total);
        return "question";
    }

    @PostMapping("/exam/navigate")
    public String navigate(@RequestParam int pos,
                            @RequestParam(required = false) List<String> selected,
                            @RequestParam(required = false) String markedForReview,
                            @RequestParam String action,
                            Model model,
                            @AuthenticationPrincipal OAuth2User principal) {
        if (!examState.isStarted()) {
            return "redirect:/";
        }
        int total = examState.getOrder().size();
        pos = clamp(pos, 1, total);
        int questionId = examState.getOrder().get(pos - 1);

        // 現在の回答と「後で見る」状態を保存
        Set<String> selectedSet = selected == null ? new LinkedHashSet<>() : new LinkedHashSet<>(selected);
        examState.setSelected(questionId, selectedSet);
        examState.setMarkedForReview(questionId, markedForReview != null);

        if (maybeAutoFinish(principal)) {
            return "redirect:/exam/result";
        }

        switch (action) {
            case "next":
                return "redirect:/exam/question/" + findNext(pos, total);
            case "prev":
                return "redirect:/exam/question/" + findPrev(pos, total);
            case "finish":
                markFinishedAndRecord(principal);
                return "redirect:/exam/result";
            case "review":
                return "redirect:/exam/review";
            default:
                return "redirect:/exam/question/" + pos;
        }
    }

    // ===================== 問題一覧（全問題レビュー） =====================

    @GetMapping("/exam/review")
    public String review(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (!examState.isStarted()) {
            return "redirect:/";
        }
        maybeAutoFinish(principal); // 時間切れの場合も一覧は見せてよいのでそのまま継続

        List<Integer> order = examState.getOrder();
        List<QuestionListItem> items = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            int qid = order.get(i);
            Question q = questionService.getById(qid);
            boolean answered = examState.isAnswered(qid);
            boolean marked = examState.isMarkedForReview(qid);
            Boolean correct = null;
            if (examState.isFinished()) {
                correct = isCorrect(q, examState.getSelected(qid));
            }
            items.add(new QuestionListItem(i + 1, answered, marked, correct));
        }
        model.addAttribute("items", items);
        model.addAttribute("total", order.size());
        model.addAttribute("reviewFilterActive", examState.isReviewFilterActive());
        model.addAttribute("finished", examState.isFinished());
        model.addAttribute("remainingMillis", examState.getRemainingMillis());
        model.addAttribute("endTimeMillis", examState.getEndTimeMillis());
        model.addAttribute("backPosition", examState.getLastPosition());
        return "review";
    }

    @PostMapping("/exam/review/filter")
    public String toggleFilter(@RequestParam(required = false) String active) {
        examState.setReviewFilterActive(active != null);
        return "redirect:/exam/review";
    }

    // ===================== 結果ページ =====================

    @GetMapping("/exam/result")
    public String result(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (!examState.isStarted()) {
            return "redirect:/";
        }
        if (!examState.isFinished()) {
            if (examState.isTimeUp()) {
                markFinishedAndRecord(principal);
            } else {
                return "redirect:/exam/question/1";
            }
        } else {
            // 通常の「試験を終了する」経由でも、まだ未保存なら保存する
            markFinishedAndRecord(principal);
        }

        ScoreSummary summary = computeScore();
        List<CategoryStat> categoryStats = toCategoryStats(summary.categoryCounts());

        List<ExamResult> pastResults = principal != null
                ? examResultRepository.findByUserIdOrderByTakenAtDesc(principal.getName())
                : List.of();
        List<ExamResult> trend = buildTrend(pastResults);
        Integer previousPercentage = pastResults.size() > 1 ? pastResults.get(1).getPercentage() : null;

        model.addAttribute("total", summary.total());
        model.addAttribute("correctCount", summary.correctCount());
        model.addAttribute("percentage", summary.percentage());
        model.addAttribute("categoryStats", categoryStats);
        model.addAttribute("pastResults", pastResults);
        model.addAttribute("trend", trend);
        model.addAttribute("previousPercentage", previousPercentage);
        return "result";
    }

    @GetMapping("/exam/finish")
    public String finishByTimer(@AuthenticationPrincipal OAuth2User principal) {
        if (examState.isStarted()) {
            markFinishedAndRecord(principal);
        }
        return "redirect:/exam/result";
    }

    // ===================== 内部ヘルパー =====================

    private boolean maybeAutoFinish(OAuth2User principal) {
        if (examState.isTimeUp()) {
            markFinishedAndRecord(principal);
            return true;
        }
        return false;
    }

    /** 正答数・分野別内訳をまとめて計算した結果 */
    private record ScoreSummary(int correctCount, int total, Map<Integer, int[]> categoryCounts) {
        int percentage() {
            return total == 0 ? 0 : Math.round(correctCount * 100f / total);
        }
    }

    private ScoreSummary computeScore() {
        List<Integer> order = examState.getOrder();
        int total = order.size();
        int correctCount = 0;
        Map<Integer, int[]> categoryCounts = new TreeMap<>(); // category -> [correct, total]
        for (int qid : order) {
            Question q = questionService.getById(qid);
            boolean correct = isCorrect(q, examState.getSelected(qid));
            if (correct) {
                correctCount++;
            }
            int[] counts = categoryCounts.computeIfAbsent(q.getCategory(), k -> new int[2]);
            counts[1]++;
            if (correct) {
                counts[0]++;
            }
        }
        return new ScoreSummary(correctCount, total, categoryCounts);
    }

    private List<CategoryStat> toCategoryStats(Map<Integer, int[]> categoryCounts) {
        return categoryCounts.entrySet().stream()
                .map(e -> new CategoryStat(ExamCategory.nameOf(e.getKey()), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());
    }

    /** グラフ表示用に古い順へ並び替え、直近10回に絞る */
    private List<ExamResult> buildTrend(List<ExamResult> pastResultsDesc) {
        List<ExamResult> trend = new ArrayList<>(pastResultsDesc);
        Collections.reverse(trend);
        if (trend.size() > 10) {
            trend = trend.subList(trend.size() - 10, trend.size());
        }
        return trend;
    }

    private String serializeCategoryCounts(Map<Integer, int[]> categoryCounts) {
        try {
            Map<String, int[]> asStringKeys = new TreeMap<>();
            categoryCounts.forEach((k, v) -> asStringKeys.put(String.valueOf(k), v));
            return objectMapper.writeValueAsString(asStringKeys);
        } catch (Exception e) {
            return null;
        }
    }

    private List<CategoryStat> parseCategoryBreakdown(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Map<String, int[]> parsed = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(TreeMap.class, String.class, int[].class));
            List<CategoryStat> stats = new ArrayList<>();
            parsed.forEach((k, v) -> stats.add(new CategoryStat(ExamCategory.nameOf(Integer.parseInt(k)), v[0], v[1])));
            stats.sort((a, b) -> a.getCategoryName().compareTo(b.getCategoryName()));
            return stats;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 試験を終了状態にし、まだ結果を保存していなければ受験履歴として1件保存する。
     * 結果ページの再読み込みや複数回の終了操作があっても、保存は最初の1回だけ行われる。
     */
    private void markFinishedAndRecord(OAuth2User principal) {
        if (!examState.isFinished()) {
            examState.setFinished(true);
        }
        if (examState.isRecorded()) {
            return;
        }
        ScoreSummary summary = computeScore();

        if (principal != null) {
            ExamResult result = new ExamResult(
                    principal.getName(), // Slackの sub（ユーザーを一意に識別するID）
                    nameOf(principal),
                    emailOf(principal),
                    summary.correctCount(), summary.total(), summary.percentage(),
                    serializeCategoryCounts(summary.categoryCounts()),
                    LocalDateTime.now());
            examResultRepository.save(result);
        }
        examState.setRecorded(true);
    }

    private String nameOf(OAuth2User principal) {
        Object name = principal.getAttribute("name");
        return name != null ? name.toString() : principal.getName();
    }

    private String emailOf(OAuth2User principal) {
        Object email = principal.getAttribute("email");
        return email != null ? email.toString() : null;
    }

    private void buildQuestionModel(Model model, Question q, int pos, int total) {
        Set<String> selected = examState.getSelected(q.getId());
        boolean finished = examState.isFinished();

        Map<String, QuestionOption> optionsById = new LinkedHashMap<>();
        for (QuestionOption opt : q.getOptions()) {
            optionsById.put(opt.getId(), opt);
        }
        List<String> displayOrder = examState.getOptionOrder(q.getId());
        List<QuestionOption> orderedOptions = new ArrayList<>();
        if (displayOrder != null) {
            for (String optId : displayOrder) {
                QuestionOption opt = optionsById.get(optId);
                if (opt != null) {
                    orderedOptions.add(opt);
                }
            }
        } else {
            // 万一シャッフル順が保存されていない場合はquestions.ymlの順序のままにする
            orderedOptions.addAll(q.getOptions());
        }

        List<OptionView> optionViews = new ArrayList<>();
        for (QuestionOption opt : orderedOptions) {
            boolean checked = selected.contains(opt.getId());
            boolean correctOpt = q.getAnswers().contains(opt.getId());
            boolean wrongSelection = finished && checked && !correctOpt;
            optionViews.add(new OptionView(opt.getId(), opt.getText(), checked, finished && correctOpt, wrongSelection));
        }

        model.addAttribute("question", q);
        model.addAttribute("options", optionViews);
        model.addAttribute("pos", pos);
        model.addAttribute("total", total);
        model.addAttribute("maxSelect", Math.max(q.getAnswers().size(), 1));
        model.addAttribute("markedForReview", examState.isMarkedForReview(q.getId()));
        model.addAttribute("finished", finished);
        model.addAttribute("remainingMillis", examState.getRemainingMillis());
        model.addAttribute("endTimeMillis", examState.getEndTimeMillis());
        model.addAttribute("hasPrev", pos > 1);
        model.addAttribute("hasNext", pos < total);
    }

    private boolean isCorrect(Question q, Set<String> selected) {
        if (selected == null) {
            selected = Collections.emptySet();
        }
        Set<String> correct = new LinkedHashSet<>(q.getAnswers());
        return correct.equals(selected);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private int findNext(int pos, int total) {
        if (!examState.isReviewFilterActive()) {
            return clamp(pos + 1, 1, total);
        }
        for (int i = pos + 1; i <= total; i++) {
            if (isMarkedAtPos(i)) return i;
        }
        for (int i = 1; i < pos; i++) {
            if (isMarkedAtPos(i)) return i;
        }
        return pos; // マークされた問題が他に無い場合は現在位置のまま
    }

    private int findPrev(int pos, int total) {
        if (!examState.isReviewFilterActive()) {
            return clamp(pos - 1, 1, total);
        }
        for (int i = pos - 1; i >= 1; i--) {
            if (isMarkedAtPos(i)) return i;
        }
        for (int i = total; i > pos; i--) {
            if (isMarkedAtPos(i)) return i;
        }
        return pos;
    }

    private boolean isMarkedAtPos(int pos) {
        int qid = examState.getOrder().get(pos - 1);
        return examState.isMarkedForReview(qid);
    }
}
