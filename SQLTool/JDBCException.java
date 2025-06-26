package SQLTool;

// 自定义异常

public class JDBCException extends RuntimeException {
  public JDBCException(String message) {
    super(message);
  }

  public JDBCException(String message, Throwable cause) {
    super(message, cause);
  }
}
