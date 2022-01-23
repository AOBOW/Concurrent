package cache;

import Final.Flyweight;
import Final.Pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author AOBO
 * @date 2021-11-27 17:08
 * @description 实现缓存功能
 */

//缓存更新策略
//一定要先更新数据库 再清空缓存
//因为如果先清空缓存  这时上下文切换  有其他线程来读数据 发现缓存是空的
//会连接数据库 并将读到的旧数据写入缓存  然后更新操作的线程去更新数据库
//但此时 不会再清空缓存了 这样之后读到的就一直是缓存中的旧数据 就出问题了

//而先更新数据库  再清空缓存  会造成中间短暂的不一致  先更新了  这时上下文切换
//其他线程来读数据  读到了旧数据  这次读的有问题  但之后  更新操作的线程就将缓存清空了
//这样之后读到的  需要先去数据库中读 存入缓存 之后就都不会有问题了

//但加了读写锁  就都能解决  先更新库再清空缓存中间短暂的不一致问题  也能解决
//但会对性能有问题  读多写少的情况还好 读操作可以并行执行
//但写多读少的情况  实际上性能比较低

//实际上缓存还应该考虑以下几点
//缓存的容量  缓存长久不用的应该过期
//并发度低  可以对不同的表  上不同的锁  或者从新设计key  进行分区

public class CustomerDAOCached {
    private CustomerDAOImpl dao = new CustomerDAOImpl();
    private Map<Integer, Customer> map = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public void updateById(Connection connection, Customer customer) {
        writeLock.lock();
        try {
            //一定要先更新数据库 再清空缓存  但现在上锁了 其实无所谓
            dao.updateById(connection, customer);
            //修改前清空缓存
            map.remove(customer.getId());
        }finally {
            writeLock.unlock();
        }
    }

    public Customer getCustomerById(Connection connection, int id) {
        readLock.lock();
        try {
            //先从缓存中找  找到了就返回
            if (map.containsKey(id)){
                return map.get(id);
            }
        }finally {
            //要加写锁之前 必须先把读锁释放掉  因为不能从读锁重入写锁 但可以从写锁重入读锁
            readLock.unlock();
        }

        writeLock.lock();
        try {
            //DCL双重检查  防止多线程  最开始都在写锁外排队  重复读取
            if (map.containsKey(id)){
                return map.get(id);
            }
            //没找到再查询数据库  这个要放在写锁里 放在外面会多次连接数据库
            Customer customer = dao.getCustomerById(connection, id);
            //缓存中没有 放入缓存
            map.put(id, customer);
            return customer;
        }finally {
            writeLock.unlock();
        }
    }

}

class Test{
    public static void main(String[] args) {
        Connection connection = null;
        try {
            connection = Pool.getConnection();
            CustomerDAOCached dao = new CustomerDAOCached();
            System.out.println(dao.getCustomerById(connection, 2));
            System.out.println(dao.getCustomerById(connection, 2));
            System.out.println(dao.getCustomerById(connection, 2));

            Customer customer = dao.getCustomerById(connection, 2);
            customer.setName("新垣结衣");

            dao.updateById(connection,customer);

            System.out.println(dao.getCustomerById(connection, 2));
            System.out.println(dao.getCustomerById(connection, 2));
            System.out.println(dao.getCustomerById(connection, 2));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
