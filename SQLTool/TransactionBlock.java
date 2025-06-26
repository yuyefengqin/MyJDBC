package SQLTool;

import java.sql.SQLException;
// 函数式接口定义
public interface TransactionBlock {
    void execute() throws SQLException;
}
