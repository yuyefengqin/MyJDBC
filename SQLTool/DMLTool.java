package SQLTool;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DMLTool {
    private DMLTool() {}

    // 数据库初始化
    public static void init(String url, String user, String pass) {
        DBConnectionTool.init(url, user, pass);
    }

    // 在事务中执行操作
    public static void doInTransaction( TransactionBlock block) {
        DBConnectionTool.doInTransaction(block);
    }

    // 查询所有数据
    public static <T> List<T> findAll(Class<T> entityClass) {
        String tableName = getTableName(entityClass);
        String columns = getColumns(entityClass);
        String sql = String.format("select %s from %s", columns, tableName);
        return DBConnectionTool.query(sql, new EntityMapper<>(entityClass));
    }
    // 分页查询数据
    public static <T> List<T> findAll(Class<T> entityClass, Integer index, Integer offset) {
        String tableName = getTableName(entityClass);
        String columns = getColumns(entityClass);
        String sql = String.format("select %s from %s limit ? offset ?", columns, tableName);
        return DBConnectionTool.query(sql, new EntityMapper<>(entityClass), index, offset);
    }
    // 分页排序查询数据
    public static <T> List<T> findAll(Class<T> entityClass,String columnsName,boolean orderBy, Integer index, Integer offset) {
        String tableName = getTableName(entityClass);
        String columns = getColumns(entityClass);
        String orderByName = ChangeCharTool.toSnakeCase(columnsName);
        String descOrAsc = orderBy ? "asc" : "desc";
        String sql = String.format("select %s from %s order by %s %s limit ? offset ?", columns, tableName, orderByName, descOrAsc);
        return DBConnectionTool.query(sql, new EntityMapper<>(entityClass), index, offset);
    }
    // 按条件查询
    public static <T> List<T> findBy(Class<T> entityClass, String column, Object value) {
        String tableName = getTableName(entityClass);
        String columns = getColumns(entityClass);
        String condition = ChangeCharTool.toSnakeCase(column) + " = ?";
        String sql = String.format("select %s from %s where %s", columns, tableName, condition);
        return DBConnectionTool.query(sql, new EntityMapper<>(entityClass), value);
    }

    // 更新数据
    public static <T> int update(T entity, String whereColumn, Object whereValue) {
        Class<?> clazz = entity.getClass();
        String tableName = getTableName(clazz);

        List<String> columns = getFieldNames(clazz);
        List<Object> values = getFieldValues(entity);

        String setClause = columns.stream()
                .map(col -> ChangeCharTool.toSnakeCase(col) + " = ?")
                .collect(Collectors.joining(", "));

        String whereClause = ChangeCharTool.toSnakeCase(whereColumn) + " = ?";

        String sql = String.format("update %s set %s where %s",
                tableName, setClause, whereClause);

        // 添加where值到参数列表
        values.add(whereValue);
        int result = DBConnectionTool.executeUpdate(sql, values.toArray());
        ColorLogger.logSQL("UPDATE: " + entity.getClass().getSimpleName());
        return result;
    }

    // 删除数据
    public static <T> int delete(Class<T> entityClass, String whereColumn, Object whereValue) {
        String tableName = getTableName(entityClass);
        String whereClause = ChangeCharTool.toSnakeCase(whereColumn) + " = ?";
        String sql = String.format("delete from %s where %s", tableName, whereClause);
        int result = DBConnectionTool.executeUpdate(sql, whereValue);
        ColorLogger.logSQL("DELETE: " + entityClass.getSimpleName());
        return result;
    }

    // 插入单条数据
    public static <T> int insert(T entity) {
        Class<?> clazz = entity.getClass();
        String tableName = getTableName(clazz);
        List<String> columns = getFieldNames(clazz);
        List<Object> values = getFieldValues(entity);

        String columnList = columns.stream()
                .map(ChangeCharTool::toSnakeCase)
                .collect(Collectors.joining(", "));

        String placeholders = "?, ".repeat(columns.size() - 1) + "?";
        String sql = String.format("insert into %s (%s) values (%s)",
                tableName, columnList, placeholders);

        int result = DBConnectionTool.executeUpdate(sql, values.toArray());
        ColorLogger.logSQL("INSERT: " + entity.getClass().getSimpleName());
        return result;
    }

    // 批量插入数据
    public static <T> int batchInsert(List<T> entities) {
        if (entities == null || entities.isEmpty()) return 0;

        T first = entities.get(0);
        Class<?> clazz = first.getClass();
        String tableName = getTableName(clazz);
        List<String> columns = getFieldNames(clazz);

        String columnList = columns.stream()
                .map(ChangeCharTool::toSnakeCase)
                .collect(Collectors.joining(", "));

        String placeholder = "(" + "?, ".repeat(columns.size() - 1) + "?)";
        String placeholders = (placeholder + ", ").repeat(entities.size() - 1) + placeholder;

        String sql = String.format("insert into %s (%s) values %s",
                tableName, columnList, placeholders);

        // 展平参数列表
        List<Object> allValues = new ArrayList<>();
        for (T entity : entities) {
            allValues.addAll(getFieldValues(entity));
        }

        int result = DBConnectionTool.executeBatch(sql, allValues.toArray());
        ColorLogger.logSQL("BATCH INSERT: " + entities.getFirst().getClass().getSimpleName());
        return result;
    }

    // 创建表
    public static <T> int createTable(Class<T> entityClass, String columnsDefinition) {
        String tableName = getTableName(entityClass);
        String sql = String.format("create table if not exists %s (%s)", tableName, columnsDefinition);
        return DBConnectionTool.executeDDL(sql);
    }

    // 删除表
    public static <T> int dropTable(Class<T> entityClass) {
        String tableName = getTableName(entityClass);
        String sql = String.format("drop table if exists %s", tableName);
        return DBConnectionTool.executeDDL(sql);
    }

    // 辅助方法

    public static <T> String getTableName(Class<T> entityClass) {
        return DBConnectionTool.getPrefix() + entityClass.getSimpleName().toLowerCase();
    }

    public static <T> String getColumns(Class<T> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .map(Field::getName)
                .map(ChangeCharTool::toSnakeCase)
                .collect(Collectors.joining(", "));
    }

    public static <T> List<String> getFieldNames(Class<T> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());
    }

    public static <T> List<Object> getFieldValues(T entity) {
        return Arrays.stream(entity.getClass().getDeclaredFields())
                .peek(f -> f.setAccessible(true))
                .map(f -> {
                    try {
                        return f.get(entity);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("无法访问字段: " + f.getName(), e);
                    }
                })
                .collect(Collectors.toList());
    }
}

