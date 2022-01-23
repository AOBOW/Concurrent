package Final;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * @author AOBO
 * @date 2021-11-27 17:12
 * @description
 */
//自定义数据库连接池
@Slf4j(topic = "c.Pool")
public class Pool {
    //1.连接池大小
    private int poolSize;
    //2.连接对象的数组
    private Connection[] connections;
    //3.连接状态的数组  0表示空闲 1表示繁忙
    private AtomicIntegerArray states;

    public Pool(int poolSize) {
        this.poolSize = poolSize;
        connections = new Connection[poolSize];
        states = new AtomicIntegerArray(poolSize);
        for (int i = 0; i < poolSize; i++) {
            connections[i] = getConnection();
        }
    }

    public Connection borrow() {
        while (true) {
            for (int i = 0; i < poolSize; i++) {
                //用CAS保证线程安全  取连接的时候 很可能有竞争
                if (states.get(i) == 0 && states.compareAndSet(i, 0, 1)) {
                    log.debug("get {}", i);
                    return connections[i];
                }
            }
            //如果没有空闲连接  让当前线程进入等待
            //这里需要加个等待操作  是因为 CAS适合短时间运行的操作
            //如果很长时间获得不到资源  CAS会让CPU一直空转  性能消耗太大
            synchronized (this) {
                try {
                    log.debug("wait");
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //还回来的时候 没有线程的竞争 不用CAS改状态
    public void free(Connection connection) {
        for (int i = 0; i < poolSize; i++) {
            if (connections[i] == connection) {
                states.set(i, 0);
                synchronized (this) {
                    log.debug("free {}", i);
                    this.notifyAll();
                }
                break;
            }
        }
    }

    public static Connection getConnection() {
        InputStream is = null;
        try {
            is = ClassLoader.getSystemClassLoader().getResourceAsStream("jdbc.properties");
            Properties p = new Properties();
            p.load(is);
            String url = p.getProperty("url");
            String user = p.getProperty("user");
            String password = p.getProperty("password");
            String classDriver = p.getProperty("classDriver");

            Class.forName(classDriver);

            return DriverManager.getConnection(url, user, password);

        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
