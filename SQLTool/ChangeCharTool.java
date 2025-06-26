package SQLTool;


import java.util.Locale;

public class ChangeCharTool {
    private ChangeCharTool() {}

    // 下划线变量名转驼峰法变量名: s_id -> sId
    public static String toCamelCase(String str) {
        if (str == null || !str.contains("_")) {
            return str;
        }
        String[] strSplit = str.split("_");
        StringBuilder finStr = new StringBuilder(strSplit[0]);
        for (int i = 1; i < strSplit.length; i++) {
            String s = strSplit[i];
            if (!s.isEmpty()) {
                finStr.append(s.substring(0, 1).toUpperCase(Locale.ROOT))
                        .append(s.substring(1));
            }
        }
        return finStr.toString();
    }

    // 驼峰法变量名转下划线变量名: sId -> s_id
    public static String toSnakeCase(String str) {
        if (str == null || str.contains("_")) {
            return str;
        }

        StringBuilder sb = new StringBuilder();
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}

