package SQLTool;

import java.sql.ResultSet;
import java.sql.SQLException;
// 函数式接口定义
public interface RowMapper<T> {
    T mapRow(ResultSet rs) throws SQLException;
}
