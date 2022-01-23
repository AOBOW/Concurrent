package reentrantlock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

//按顺序运行
@Slf4j(topic = "c.Order1")
public class Order1 {
    static boolean canRun = false;
    public static void main(String[] args) {
        test2();
    }

    public static void test1(){
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        new Thread(() -> {
            lock.lock();
            try {
                while (!canRun){   //wait/await 要用在while中 防止虚假唤醒
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("1");
            }finally {
                lock.unlock();
            }
        }).start();

        new Thread(() -> {
            lock.lock();
            try {
                log.debug("2");
                canRun = true;
                condition.signalAll();
            }finally {
                lock.unlock();
            }
        }).start();
    }

    //park和unpark 方法比较灵活，他俩谁先调用，谁后调用无所谓。
    //并且是以线程为单位进行暂停和恢复，不需要同步对象和运行标记
    public static void test2(){
        //这种park的做法很巧妙  记住
        Thread t1 = new Thread(() -> {
            //这里打印要在park后面  之前如果t2没有执行 需要先阻塞住
            //当没有许可时，当前线程暂停运行；有许可时，用掉这个许可，当前线程恢复运行
            LockSupport.park();
            log.debug("1");
        });
        t1.start();

        Thread t2 = new Thread(() -> {
            //给线程 t1 发放许可(多次连续调用 unpark 只会发放一个许可)
            //这里打印要在unpark前面  因为unpark之后 t1的park阻塞就失效了
            //有可能切换走  所以需要在t1还阻塞的时候打印
            log.debug("2");
            LockSupport.unpark(t1);
        });
        t2.start();
    }
}
