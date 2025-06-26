package SQLTool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class ColorLogger {
    // ANSI 颜色代码
    private static final String RESET = "\u001B[0m";
    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    // 背景色
    private static final String BG_BLACK = "\u001B[40m";
    private static final String BG_RED = "\u001B[41m";
    private static final String BG_GREEN = "\u001B[42m";
    private static final String BG_YELLOW = "\u001B[43m";
    private static final String BG_BLUE = "\u001B[44m";
    private static final String BG_PURPLE = "\u001B[45m";
    private static final String BG_CYAN = "\u001B[46m";
    private static final String BG_WHITE = "\u001B[47m";

    // 样式
    private static final String BOLD = "\u001B[1m";//加粗
    private static final String UNDERLINE = "\u001B[4m";//下划线

    private static boolean enabled = true;
    private static boolean colorEnabled = true;
    private static boolean timestampEnabled = true;
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    public static void enableColor() {
        colorEnabled = true;
    }

    public static void disableColor() {
        colorEnabled = false;
    }

    public static void enableTimestamp() {
        timestampEnabled = true;
    }

    public static void disableTimestamp() {
        timestampEnabled = false;
    }

    public static void logSQL(String sql) {
        if (!enabled) return;
        String prefix = timestampEnabled ?
                getColoredTimestamp() + colorText(" SQL: ", CYAN + BOLD) :
                colorText("SQL: ", CYAN + BOLD);
        System.out.println(prefix + colorText(sql, CYAN));
    }

    public static void logSQL(String sql, Object... params) {
        if (!enabled) return;
        logSQL(sql);
        if (params != null && params.length > 0) {
            String paramPrefix = timestampEnabled ?
                    getColoredTimestamp() + colorText(" PARAMS: ", PURPLE + BOLD) :
                    colorText("PARAMS: ", PURPLE + BOLD);
            String paramStr = Arrays.toString(params);
            System.out.println(paramPrefix + colorText(paramStr, PURPLE));
        }
    }

    public static void logTransaction(String action) {
        if (!enabled) return;
        String prefix = timestampEnabled ?
                getColoredTimestamp() : "";
        String coloredAction = colorText(" TRANSACTION: " + action, BLUE + BOLD);
        System.out.println(prefix + coloredAction);
    }

    public static void logDDL(String ddl) {
        if (!enabled) return;
        String prefix = timestampEnabled ?
                getColoredTimestamp() + colorText(" DDL: ", YELLOW + BOLD) :
                colorText("DDL: ", YELLOW + BOLD);
        System.out.println(prefix + colorText(ddl, YELLOW));
    }

    public static void logBatch(String sql, List<Object[]> paramList) {
        if (!enabled) return;
        logSQL("BATCH: " + sql);
        if (paramList != null && !paramList.isEmpty()) {
            for (int i = 0; i < paramList.size(); i++) {
                String batchPrefix = timestampEnabled ?
                        getColoredTimestamp() + colorText(" BATCH[" + (i+1) + "]: ", GREEN + BOLD) :
                        colorText("BATCH[" + (i+1) + "]: ", GREEN + BOLD);
                String paramStr = Arrays.toString(paramList.get(i));
                System.out.println(batchPrefix + colorText(paramStr, GREEN));
            }
        }
    }

    public static void logResult(int rowCount) {
        if (!enabled) return;
        String prefix = timestampEnabled ?
                getColoredTimestamp() + colorText(" RESULT: ", GREEN + BOLD) :
                colorText("RESULT: ", GREEN + BOLD);
        String resultText = rowCount + (rowCount == 1 ? " row affected" : " rows affected");
        System.out.println(prefix + colorText(resultText, GREEN));
    }

    public static void logResults(int rowCounts) {
        if (!enabled) return;
        String prefix = timestampEnabled ?
                getColoredTimestamp() + colorText(" BATCH RESULT: ", BG_GREEN + BLACK + BOLD) :
                colorText("BATCH RESULT: ", BG_GREEN + BLACK + BOLD);
        String resultText = rowCounts + " batches, " + rowCounts + " total rows affected";
        System.out.println(prefix + colorText(resultText, BG_GREEN + BLACK));
    }

    public static void logError(String sql, Throwable e) {
        if (!enabled) return;
        String prefix = timestampEnabled ?
                getColoredTimestamp() + colorText(" ERROR: ", BG_RED + WHITE + BOLD) :
                colorText("ERROR: ", BG_RED + WHITE + BOLD);
        System.out.println(prefix + colorText(e.getMessage(), RED + BOLD));
        System.out.println(colorText("SQL: " + sql, RED));

        // 只打印关键堆栈
        StackTraceElement[] stack = e.getStackTrace();
        for (int i = 0; i < Math.min(3, stack.length); i++) {
            System.out.println(colorText("    at " + stack[i], RED));
        }
        if (stack.length > 3) {
            System.out.println(colorText("    ... " + (stack.length - 3) + " more", RED));
        }
    }

    private static String getColoredTimestamp() {
        return colorText("[" + LocalDateTime.now().format(formatter) + "]", WHITE + BOLD);
    }

    private static String colorText(String text, String colorCode) {
        if (colorEnabled) {
            return colorCode + text + RESET;
        }
        return text;
    }
}
