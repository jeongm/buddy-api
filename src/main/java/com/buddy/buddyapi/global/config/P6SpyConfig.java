package com.buddy.buddyapi.global.config;

import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class P6SpyConfig {

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(P6SpyPrettyFormat.class.getName());
    }

    public static class P6SpyPrettyFormat implements MessageFormattingStrategy {

        // ANSI 색상 코드
        private static final String RESET = "\u001B[0m";
        private static final String BLUE = "\u001B[34m";   // 파란색 (키워드용)
        private static final String CYAN = "\u001B[36m";   // 청록색 (시간 표시용)

        @Override
        public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
            sql = formatSql(category, sql);

            // 실행 시간은 청록색으로 이쁘게
            return String.format("%s[%s] | %d ms |%s %s", CYAN, category, elapsed, RESET, sql);
        }

        private String formatSql(String category, String sql) {
            if (sql == null || sql.trim().isEmpty()) return sql;

            // Only format Statement, PreparedStatement
            if ("statement".equals(category)) {
                String tmpsql = sql.trim().toLowerCase(Locale.ROOT);

                // 1. Hibernate 포맷터로 줄바꿈/들여쓰기 적용
                if (tmpsql.startsWith("create") || tmpsql.startsWith("alter") || tmpsql.startsWith("comment")) {
                    sql = FormatStyle.DDL.getFormatter().format(sql);
                } else {
                    sql = FormatStyle.BASIC.getFormatter().format(sql);
                }

                // 2. 키워드에 파란색 입히기 (Syntax Highlighting)
                return highlightKeywords(sql);
            }
            return sql;
        }

        // 🖍️ 여기가 핵심! 키워드를 찾아서 파란색으로 칠해줍니다.
        private String highlightKeywords(String sql) {
            // 정규식으로 SQL 키워드 찾기 (대소문자 무시)
            // \b는 단어 경계를 의미합니다 (selects 같은 건 안 칠해짐)
            return sql.replaceAll("(?i)\\b(select|from|where|insert|into|values|update|set|delete|join|on|and|or|group by|order by|limit|having|as|is|null|not|in|exists|like)\\b",
                    BLUE + "$1" + RESET);
        }
    }
}
