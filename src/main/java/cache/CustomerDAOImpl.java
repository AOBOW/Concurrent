package cache;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.Date;
import java.util.List;

/**
 * @author AOBO
 * @date 2021-11-10 23:17
 * @description 实现类
 */

@Slf4j(topic = "c.CustomerDAOImpl")
public class CustomerDAOImpl extends BaseDAO<Customer> implements CustomerDAO {
    @Override
    public void insert(Connection connection, Customer customer) {
        String sql = "insert into customers(name, email, birth) values(?,?,?)";
        update(connection, sql, customer.getName(), customer.getEmail(), customer.getBirth());
    }

    @Override
    public void deleteById(Connection connection, int id) {
        String sql = "delete from customers where id = ?";
        update(connection, sql, id);
    }

    @Override
    public void updateById(Connection connection, Customer customer) {
        String sql = "update customers set name = ?, email = ?, birth = ? where id = ?";
        update(connection, sql, customer.getName(), customer.getEmail(), customer.getBirth(), customer.getId());
    }

    @Override
    public Customer getCustomerById(Connection connection, int id) {
        log.debug("连接数据库");
        String sql = "select * from customers where id = ?";
        List<Customer> list = select(connection, sql, id);
        if (list.size() > 0){
            return list.get(0);
        }
        return null;
    }

    @Override
    public List<Customer> getAllCustomers(Connection connection) {
        String sql = "select * from customers";
        return select(connection, sql);
    }

    @Override
    public Long getCount(Connection connection) {
        String sql = "select count(*) from customers";
        return getValue(connection, sql);
    }

    @Override
    public Date getMaxBirth(Connection connection) {
        String sql = "select max(birth) from customers";
        return getValue(connection, sql);
    }
}
