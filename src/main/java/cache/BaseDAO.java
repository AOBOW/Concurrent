package cache;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author AOBO
 * @date 2021-11-10 22:35
 * @description 封装了针对于数据表的通用的操作
 */
//DA0 database access object
public abstract class BaseDAO<T> {
    //获取父类的泛型
    //因为BaseDAO本身不能实例化 子类继承的时候会传入泛型
    //拿到这个泛型之后就不需要传入Class实例了
    Class<T> clazz;

    //赋值放在代码块或者构造器中  即和对象一起生成  代码块先执行 代码块执行后  构造器执行
    public BaseDAO(){
        //获取当前BaseDAO的子类继承的父类中的泛型
        Type genericSuperclass = this.getClass().getGenericSuperclass();
        ParameterizedType param = (ParameterizedType) genericSuperclass;
        //获取父类的泛型参数
        Type[] actualTypeArguments = param.getActualTypeArguments();
        clazz = (Class<T>) actualTypeArguments[0];
    }

    public int update(Connection connection, String sql, Object...args){
        PreparedStatement ps = null;
        try {
            ps = getPreparedStatement(connection, sql, args);
            return ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            closeResource(ps, null);
        }
        return 0;
    }

    public List<T> select(Connection connection, String sql, Object...args){
        List<T> result = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            ps = getPreparedStatement(connection, sql, args);
            resultSet = ps.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (resultSet.next()){
                T t = clazz.getDeclaredConstructor().newInstance();
                for (int i = 0; i < columnCount; i++){
                    String label = metaData.getColumnLabel(i + 1);
                    if ("photo".equals(label)){
                        continue;
                    }
                    Field field = clazz.getDeclaredField(label);
                    field.setAccessible(true);
                    Object object = resultSet.getObject(i + 1);
                    field.set(t, object);
                }
                result.add(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResource(ps, resultSet);
        }
        return result;
    }

    //用于查询特殊值的通用方法  如果子类没有传入泛型 则编译后会发生泛型擦除
    //这时这里的返回值就只能是Object  但如果不是泛型类 则泛型方法不受影响
    public <E> E getValue(Connection connection, String sql, Object...args){
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            ps = getPreparedStatement(connection, sql, args);
            resultSet = ps.executeQuery();
            if (resultSet.next()){
                return (E)resultSet.getObject(1);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            closeResource(ps, resultSet);
        }
        return null;
    }

    private PreparedStatement getPreparedStatement(Connection connection, String sql, Object...args) throws SQLException {
        PreparedStatement ps = ps = connection.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
        return ps;
    }

    private void closeResource(PreparedStatement ps, ResultSet resultSet){
        if (ps != null){
            try {
                ps.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if (resultSet != null){
            try {
                resultSet.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }
}
