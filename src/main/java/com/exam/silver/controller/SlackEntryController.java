package com.exam.silver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Slack側に貼っておくためのリンク用エンドポイント。
 * ブラウザで既にSlackにログイン済みの状態でこのURLを踏むと、
 * 追加のログイン入力なしにSlackの「連携を許可しますか？」の同意画面がすぐに表示され、
 * 承認するとそのままこのアプリにもログインした状態になる。
 *
 * Slack側（Appのホーム画面のリンクや、チャンネルに貼るメッセージなど）には、
 * このアプリのURL + "/login/slack" を設定してください。
 * 例: https://your-app.example.com/login/slack
 */
@Controller
public class SlackEntryController {

    @GetMapping("/login/slack")
    public String slackEntry() {
        return "redirect:/oauth2/authorization/slack";
    }
}
