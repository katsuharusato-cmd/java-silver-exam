package com.exam.silver.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 「分野」番号と表示名の対応表。
 *
 * Java Silver試験の出題範囲に準拠した6分野。
 * questions.yml の各問題に付ける category の数値と、ここのキーを対応させます。
 */
public final class ExamCategory {

    private static final Map<Integer, String> NAMES = new LinkedHashMap<>();

    static {
        NAMES.put(1, "Javaの概要と簡単なJavaプログラムの作成");
        NAMES.put(2, "Javaの基本データ型と文字列の操作");
        NAMES.put(3, "演算子と制御構造");
        NAMES.put(4, "クラスの定義とインスタンスの使用");
        NAMES.put(5, "継承とインタフェースの使用");
        NAMES.put(6, "例外処理");
    }

    private ExamCategory() {
    }

    public static String nameOf(int category) {
        return NAMES.getOrDefault(category, "分野" + category);
    }

    public static Map<Integer, String> all() {
        return NAMES;
    }
}
