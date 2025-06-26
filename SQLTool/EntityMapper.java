package SQLTool;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

// 实体映射器
public class EntityMapper<T> implements RowMapper<T> {
    private final Class<T> entityClass;
    private final List<Field> fields;

    public EntityMapper(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.fields = Arrays.asList(entityClass.getDeclaredFields());
        this.fields.forEach(f -> f.setAccessible(true));
    }

    @Override
    public T mapRow(ResultSet rs) throws SQLException {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            for (Field field : fields) {
                String columnName = ChangeCharTool.toSnakeCase(field.getName());
                Object value = rs.getObject(columnName);
                field.set(entity, value);
            }
            return entity;
        } catch (Exception e) {
            throw new SQLException("实体映射失败: " + entityClass.getName(), e);
        }
    }
}
