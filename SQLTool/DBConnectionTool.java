package SQLTool;

import java.sql.*;
import java.util.*;

public class DBConnectionTool {
    private static String dbUrl;
    private static String username;
    private static String password;
    private static String Prefix = "m_";


    // 线程本地事务状态
    private static final ThreadLocal<Connection> localConnection = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> localTransactionOwner = new ThreadLocal<>();

    public static void init(String url, String user, String pass) {
        dbUrl = Objects.requireNonNull(url, "URL为空");
        username = Objects.requireNonNull(user, "用户名为空");
        password = Objects.requireNonNull(pass, "密码为空");
    }


//    获取数据库连接（自动管理事务状态）

    private static Connection getConnection() throws SQLException {
        // 如果已经在事务中 返回事务连接
        Connection transactionConn = localConnection.get();
        if (transactionConn != null) {
            return transactionConn;
        }

        // 获取新的非事务连接
        return DriverManager.getConnection(dbUrl, username, password);
    }


//    开启事务

    public static void beginTransaction() throws SQLException {
        if (localConnection.get() != null) {
            throw new JDBCException("事务已经开始");
        }

        Connection conn = DriverManager.getConnection(dbUrl, username, password);
        conn.setAutoCommit(false);
        localConnection.set(conn);
        localTransactionOwner.set(true);
        ColorLogger.logTransaction(">>> BEGIN TRANSACTION");
    }


//    提交事务

    public static void commitTransaction() throws SQLException {
        Connection conn = localConnection.get();
        if (conn == null) {
            throw new JDBCException("没有可用事务");
        }

        try {
            conn.commit();
        } finally {
            closeTransactionConnection(conn);
        }
        ColorLogger.logTransaction("<<< COMMIT TRANSACTION");
    }


//    回滚事务

    public static void rollbackTransaction() {
        Connection conn = localConnection.get();
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                closeTransactionConnection(conn);
            }
        }
        ColorLogger.logTransaction("!!! ROLLBACK TRANSACTION");
    }


//    关闭事务连接并清理线程本地状态

    private static void closeTransactionConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.setAutoCommit(true); // 恢复自动提交
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            localConnection.remove();
            localTransactionOwner.remove();
        }

    }


//    执行SQL更新

    public static int executeUpdate(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            // 添加日志
            ColorLogger.logSQL(sql, params);
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            bindParameters(ps, params);
            // 记录结果
            int result = ps.executeUpdate();
            ColorLogger.logResult(result);
            return result;
        } catch (SQLException e) {
            ColorLogger.logError(sql, e);
            throw new JDBCException("更新失败: " + sql, e);
        } finally {
            closeStatement(ps);

            // 非事务操作需要关闭连接
            if (localConnection.get() == null && conn != null) {
                closeConnection(conn);
            }
        }

    }


//    执行批处理操作

    public static int executeBatch(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            // 添加批量日志
            ColorLogger.logBatch(sql, Collections.singletonList(params));
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            int i = 1;
            for (Object param : params) {
                ps.setObject(i, param);
                i ++;
            }

            int results = ps.executeUpdate();
            ColorLogger.logResults(results);
            return results;
        } catch (SQLException e) {
            ColorLogger.logError(sql, e);
            rollbackSilently(conn);
            throw new JDBCException("批量执行失败: " + sql, e);
        } finally {
            closeStatement(ps);

            // 非事务操作需要关闭连接
            if (localConnection.get() == null && conn != null) {
                closeConnection(conn);
            }
        }
    }


//    静默回滚

    private static void rollbackSilently(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                // 忽略回滚异常
            }
        }
    }


//    查询并映射结果

    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            // 添加日志
            ColorLogger.logSQL(sql, params);
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            bindParameters(ps, params);
            rs = ps.executeQuery();

            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapper.mapRow(rs));
            }
            ColorLogger.logResult(results.size());
            return results;
        } catch (SQLException e) {
            ColorLogger.logError(sql, e);
            throw new JDBCException("查询失败: " + sql, e);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);

            // 非事务操作需要关闭连接
            if (localConnection.get() == null && conn != null) {
                closeConnection(conn);
            }
        }
    }


//    执行DDL语句（建表、删表等）

    public static int executeDDL(String ddl) {
        Connection conn = null;
        Statement stmt = null;

        try {
            // 添加日志
            ColorLogger.logDDL(ddl);
            conn = getConnection();
            stmt = conn.createStatement();
            int result = stmt.executeUpdate(ddl);
            // 记录DDL结果
            ColorLogger.logDDL(ddl);
            return result == 0 ? 1 : 0; // DDL通常返回0
        } catch (SQLException e) {
            ColorLogger.logError(ddl, e);
            throw new JDBCException("DDL执行失败: " + ddl, e);
        } finally {
            closeStatement(stmt);

            // 非事务操作需要关闭连接
            if (localConnection.get() == null && conn != null) {
                closeConnection(conn);
            }
        }
    }


//    在事务中执行操作

    public static void doInTransaction(TransactionBlock block) {
        try {
            beginTransaction();
            block.execute();
            commitTransaction();
        } catch (Exception e) {
            try {
                rollbackTransaction();
            } catch (Exception rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            throw new JDBCException("事务执行失败", e);
        }
    }

    // 辅助方法
    //设置占位符
    private static void bindParameters(PreparedStatement ps, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    private static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    private static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static String getPrefix() {
        return Prefix;
    }

    public static void setPrefix(String prefix) {
        Prefix = prefix;
    }
}

