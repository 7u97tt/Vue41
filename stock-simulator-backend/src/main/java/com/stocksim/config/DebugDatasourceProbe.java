package com.stocksim.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Debug session 251970: log resolved datasource config and JDBC probe before pool init.
 */
public class DebugDatasourceProbe implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Path LOG_PATH = Path.of("d:\\Cursor\\project\\debug-251970.log");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        String url = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");
        String envUrl = System.getenv("SPRING_DATASOURCE_URL");
        String envUser = System.getenv("SPRING_DATASOURCE_USERNAME");
        String envPass = System.getenv("SPRING_DATASOURCE_PASSWORD");

        // #region agent log
        writeLog("H-B", "DebugDatasourceProbe:resolved-config", Map.of(
                "url", nullToEmpty(url),
                "username", nullToEmpty(username),
                "passwordLength", password == null ? -1 : password.length(),
                "passwordBlank", password == null || password.isBlank(),
                "activeProfiles", String.join(",", env.getActiveProfiles()),
                "envSpringDatasourceUrlSet", envUrl != null,
                "envSpringDatasourceUserSet", envUser != null,
                "envSpringDatasourcePassLength", envPass == null ? -1 : envPass.length()
        ));
        // #endregion

        // #region agent log
        probeJdbc("H-A", url, username, password, "configured-url");
        if (url != null && url.contains("localhost")) {
            String altUrl = url.replace("localhost", "127.0.0.1");
            probeJdbc("H-D", altUrl, username, password, "127.0.0.1-url");
        }
        // #endregion
    }

    private static void probeJdbc(String hypothesisId, String url, String username, String password, String label) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("label", label);
        data.put("url", nullToEmpty(url));
        data.put("username", nullToEmpty(username));
        if (url == null || username == null) {
            data.put("result", "skipped-missing-config");
            writeLog(hypothesisId, "DebugDatasourceProbe:jdbc-probe", data);
            return;
        }
        try {
            DriverManager.getConnection(url, username, password == null ? "" : password).close();
            data.put("result", "success");
        } catch (Exception e) {
            data.put("result", "failure");
            data.put("errorClass", e.getClass().getSimpleName());
            data.put("errorMessage", e.getMessage());
        }
        writeLog(hypothesisId, "DebugDatasourceProbe:jdbc-probe", data);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static void writeLog(String hypothesisId, String location, Map<String, ?> data) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"sessionId\":\"251970\",\"hypothesisId\":\"").append(escape(hypothesisId)).append("\"");
            sb.append(",\"location\":\"").append(escape(location)).append("\"");
            sb.append(",\"message\":\"datasource-debug\"");
            sb.append(",\"timestamp\":").append(System.currentTimeMillis());
            sb.append(",\"data\":{");
            boolean first = true;
            for (Map.Entry<String, ?> e : data.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(escape(e.getKey())).append("\":");
                Object v = e.getValue();
                if (v instanceof Number || v instanceof Boolean) {
                    sb.append(v);
                } else {
                    sb.append('"').append(escape(String.valueOf(v))).append('"');
                }
            }
            sb.append("}}\n");
            Files.writeString(LOG_PATH, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // probe must not break startup
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
