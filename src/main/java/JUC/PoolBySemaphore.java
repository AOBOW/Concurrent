package JUC;

import Final.Pool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * @author AOBO
 * @date 2021-11-27 17:12
 * @description
 */
//自定义数据库连接池  用Semaphore实现
@Slf4j(topic = "c.Pool")
public class PoolBySemaphore {
    //1.连接池大小
    private int poolSize;
    //2.连接对象的数组
    private Connection[] connections;
    //3.连接状态的数组  0表示空闲 1表示繁忙
    private AtomicIntegerArray states;

    private Semaphore semaphore;

    public PoolBySemaphore(int poolSize) {
        this.poolSize = poolSize;
        this.semaphore = new Semaphore(poolSize);
        connections = new Connection[poolSize];
        states = new AtomicIntegerArray(poolSize);
        for (int i = 0; i < poolSize; i++) {
            connections[i] = Pool.getConnection();
        }
    }

    public Connection borrow() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < poolSize; i++) {
            //用CAS保证线程安全  取连接的时候 很可能有竞争
            if (states.get(i) == 0 && states.compareAndSet(i, 0, 1)) {
                log.debug("get {}", i);
                return connections[i];
            }
        }
        return null;
    }

    //还回来的时候 没有线程的竞争 不用CAS改状态
    public void free(Connection connection) {
        for (int i = 0; i < poolSize; i++) {
            if (connections[i] == connection) {
                states.set(i, 0);
                semaphore.release();
                break;
            }
        }
    }
}
