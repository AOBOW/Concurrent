package Final;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

//享元模式
//当需要重用数量有限的同一对象时 使用享元模式
//对相同值的对象共享 以达到最小的内存使用
//Integer 维护的-128到127的缓存
//(其他包装类也有 valueOf方法的时候  自动装箱就是valueOf  直接从缓存取)
//Byte Short Long 是-128 - 127
//Character 0 - 128
//Boolean缓存了TRUE和FALSE
//Integer默认是-128 - 127 但最大值可以通过调整虚拟机参数来改变
//String的字符串常量池
//BigDecimal  BigInteger
//数据库连接池 线程池 都属于享元模式

//这些不可变类 单个操作 都是线程安全的 但并不能保证多个方法的组合是线程安全的
//比如又要get 又要set  get和set合在一起 并不是原子的  还是得保护起来
public class Flyweight {
    public static void main(String[] args) {
        Pool pool = new Pool(2);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                Connection connection = null;
                try {
                    connection = pool.borrow();
                    try {
                        TimeUnit.SECONDS.sleep(new Random().nextInt(5));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } finally {
                    if (connection != null){  //连接池的归还或者关闭 要放在finally中
                        pool.free(connection);
                    }
                }
            }).start();
        }
    }
}

