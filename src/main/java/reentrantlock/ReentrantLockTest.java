package reentrantlock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author AOBO
 * @date 2021-11-22 23:16
 * @description 可重入锁
 */
//ReentrantLock
//可中断(A线程尝试加锁失败 进入阻塞后 B线程可以进行打断
//synchronized不能打断等待  被阻塞了就得一直等着
//注意这里的可中断是中断因为等待锁而阻塞中的线程  而不是获得锁的线程)
//可设置超时时间(tryLock)
//可设置公平锁(默认不公平 通过构造器可以设置为公平  公平锁本意是用来解决饥饿问题
//但实际上tryLock更好  一般不会用公平锁 会降低并发度)
//可重入
//支持多个条件变量
//(相当于支持多个synchronized的monitor中的waitSet 可以有多个等待的位置 condition精准唤醒)

//await前需要获得锁 执行后释放锁 进入condition等待  被唤醒后重新竞争lock锁
//竞争成功后  从await后继续执行


//ReentrantLock相当于是在Java API的层面上实现锁(JAVA源码)
//而Synchronized的monitor模型  是在JVM层面上来实现锁(属于C++源码)  二者的实现方法是可以互相参照的
@Slf4j(topic = "c.ReentrantLockTest")
public class ReentrantLockTest {
    private final static ReentrantLock lock = new ReentrantLock();
    public static void main(String[] args) {
        test3();
    }

    //可中断  lockInterruptibly
    private static void test1(){
        Thread t1 = new Thread(() -> {
            try {
                //可以被打断的
                //如果没有竞争  此方法或获取lock对象的锁
                //如果有竞争 进入阻塞队列
                //但此时可以被其它线程用interrupt进行打断 不再阻塞
                log.debug("尝试获得锁");
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.debug("没有获得锁 返回");
                //如果没有获得锁 需要return 否则执行下面的代码会有安全问题(比如没上锁却要unlock)
                return;
            }

            try {
                log.debug("获得锁");
            } finally {
                lock.unlock();
            }
        }, "t1");

        lock.lock();
        t1.start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        t1.interrupt();
    }

    //没有获得锁 立刻失败  tryLock
    private static void test2(){
        Thread t1 = new Thread(() -> {
            //尝试获得锁  获得锁继续执行  没获得锁立刻失败  返回值是是否获得到了锁
            if (!lock.tryLock()) {
                log.debug("获取不到锁");
                return;   //这里记得一定要return
            }
            try {
                log.debug("获得到了锁");
            } finally {
                lock.unlock();
            }
        }, "t1");

        lock.lock();

        t1.start();
    }

    //有等待时间的  tryLock
    private static void test3(){
        Thread t1 = new Thread(() -> {
            //尝试获得锁  获得锁继续执行  没获得锁等待一段时间  返回值是是否获得到了锁
            try {
                if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                    log.debug("获取不到锁");
                    return;  //这里记得一定要return
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                //被打断的情况下也要return 没加上锁 就不能往下执行
                log.debug("获取不到锁");
                return;
            }

            try {
                log.debug("获得到了锁");
            } finally {
                lock.unlock();
            }
        }, "t1");

        lock.lock();

        t1.start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        lock.unlock();
    }
}
