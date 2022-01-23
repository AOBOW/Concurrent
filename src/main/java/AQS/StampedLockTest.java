package AQS;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

/**
 * @author AOBO
 * @date 2021-11-27 22:33
 * @description StampedLock
 */

//JDK8加入 进一步优化了读的性能  特点是使用读锁、写锁时都必须配合戳使用
//加锁的时候  返回一个戳  解锁的时候 传入这个戳 进行解锁

//StampedLock读的时候  采用的是乐观读  读取完毕后需要做一次戳校验
//如果校验通过  戳没有改变  说明这期间已知没有过写操作 直接返回  一直到这里 都没加锁
//如果校验没有通过  再升级为读锁

//StampedLock  不支持条件变量 不支持可重入  所以StampedLock 并不能取代ReentrantReadWriteLock
@Slf4j(topic = "c.StampedLockTest")
public class StampedLockTest {
    public static void main(String[] args) {
        DataContainerStamped container = new DataContainerStamped(10);
        new Thread(() -> {
            container.read();
        }, "t1").start();

        new Thread(() -> {
            container.write(1);
        }, "t2").start();
    }
}

@Slf4j(topic = "c.DataContainerStamped")
class DataContainerStamped{
    private int data;
    private final StampedLock lock = new StampedLock();

    public DataContainerStamped(int data) {
        this.data = data;
    }

    public void write(int newData){
        long stamp = lock.writeLock();
        try{
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            data = newData;
        }finally {
            lock.unlockWrite(stamp);
        }
    }

    public int read(){
        //乐观读
        long stamp = lock.tryOptimisticRead();
        log.debug("optimistic read locking {}", stamp);
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (lock.validate(stamp)){
            log.debug("read finish {}", stamp);
            return data;
        }

        try {
            //上面校验没通过  升级为读锁
            stamp = lock.readLock();
            log.debug("升级读锁 {}", stamp);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return data;
        }finally {
            lock.unlockRead(stamp);
        }
    }
}