package com.exam.silver.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Slack Appの登録をしなくてもアプリを一通り試せるようにするための、
 * 開発・検証用の簡易ログイン機能。
 *
 * 名前を入力するだけで、実際のSlackログインと同じ形（OAuth2User）の
 * 「なりすまし」認証情報をセッションに設定する。
 * ExamController側は本物のSlackログインとの違いを一切意識しなくてよい
 * （@AuthenticationPrincipal OAuth2User principal がそのまま使える）。
 *
 * application.yml の app.demo-login-enabled を false にすると、
 * このログイン方法自体を無効化できる（本番でSlackログインだけにしたい場合）。
 */
@Controller
public class DemoLoginController {

    private final boolean demoLoginEnabled;

    public DemoLoginController(@Value("${app.demo-login-enabled:true}") boolean demoLoginEnabled) {
        this.demoLoginEnabled = demoLoginEnabled;
    }

    @GetMapping("/login/demo")
    public String demoLoginDisabledGuard() {
        // GETで直接叩かれた場合はスタートページへ（フォームはstart.htmlに埋め込み済み）
        return "redirect:/";
    }

    @PostMapping("/login/demo")
    public String demoLogin(@RequestParam String name,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        if (!demoLoginEnabled || !StringUtils.hasText(name)) {
            return "redirect:/";
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        String trimmedName = name.trim();
        // 同じ名前を入力すれば同じ人として扱う（＝履歴が積み上がる）ように、
        // 名前を正規化した文字列からIDを安定的に作る（ランダムにはしない）。
        String normalizedName = trimmedName.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", "");
        String fakeSub = "demo-" + normalizedName;
        attributes.put("sub", fakeSub);
        attributes.put("name", trimmedName);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        OAuth2User principal = new DefaultOAuth2User(authorities, attributes, "sub");

        AbstractAuthenticationToken authentication = new DemoAuthenticationToken(principal, authorities);
        authentication.setAuthenticated(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        return "redirect:/";
    }

    /**
     * OAuth2AuthenticationToken は登録済みクライアントID文字列を要求するため、
     * デモログイン専用の単純なトークン実装を使う（principalがOAuth2Userである点は同じ）。
     */
    private static class DemoAuthenticationToken extends AbstractAuthenticationToken {
        private final OAuth2User principal;

        DemoAuthenticationToken(OAuth2User principal, List<GrantedAuthority> authorities) {
            super(authorities);
            this.principal = principal;
        }

        @Override
        public Object getCredentials() {
            return "";
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }
    }
}
