package cache;

import java.sql.Connection;
import java.sql.Date;
import java.util.List;

/**
 * @author AOBO
 * @date 2021-11-10 23:06
 * @description 用来定义针对Customers表的常用操作
 */
public interface CustomerDAO {
    //将customer对象添加到数据库中
    void insert(Connection connection, Customer customer);

    //根据指定的id删除表中的一条记录
    void deleteById(Connection connection, int id);

    //根据内存中的customer对象，修改数据表中指定的记录
    void updateById(Connection connection, Customer customer);

    //根据指定的Id 查询对应的customer
    Customer getCustomerById(Connection connection, int id);

    //查询表中的所有记录
    List<Customer> getAllCustomers(Connection connection);

    //返回数据表中数据的条目数
    Long getCount(Connection connection);

    //返回数据表中最大的birth(日期看的是数字 数越大的 日期越大)
    Date getMaxBirth(Connection connection);
}
