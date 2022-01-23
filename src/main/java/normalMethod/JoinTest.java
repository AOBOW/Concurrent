package normalMethod;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

//join底层就是wait方法
/**
 * Join底层用了保护性暂停Guarded Suspension的原理  在a 线程调用b.join()
 * b.join()在调用时  先用synchronized锁住 同步监视器为调用join的线程b本身
 * 所以调用b线程本身的wait方法(会释放b的锁)  让a线程进入WAITING状态
 * 同时在一个线程执行结束后 会自动调用线程本身的notify()  所以b线程结束后  会自动调用b.notify()
 * 从而通知wait的a线程结束WAINTING状态  这时b线程的isAlive为false  从而结束join
 *
 *     public final synchronized void join(long millis) throws InterruptedException {
 *         long base = System.currentTimeMillis();
 *         long now = 0;
 *
 *         if (millis < 0) {
 *             throw new IllegalArgumentException("timeout value is negative");
 *         }
 *
 *         if (millis == 0) {
 *             while (isAlive()) {   //是调用者轮询检查线程 alive 状态
 *                 wait(0);    //这里调了wait()  所以join会释放调用者的锁
 *             }
 *         } else {
 *             while (isAlive()) {
 *                 long delay = millis - now;
 *                 if (delay <= 0) {
 *                     break;
 *                 }
 *                 wait(delay);
 *                 now = System.currentTimeMillis() - base;
 *             }
 *         }
 *     }
 */

@Slf4j(topic = "c.JoinTest")
public class JoinTest {
    static int i = 0;
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            try {
//                Thread.currentThread().join();
                TimeUnit.SECONDS.sleep(1);
                log.debug("开始赋值");
                i = 10;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t1");

        thread.start();

        try {
            //主线程同步等待thread线程
            //线程必须启动了 才能调用join  调用join后 调用join的线程执行 原线程阻塞 一直到调用线程执行完
            //所以说  永远不能在自己的线程 调用自己的join
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("结果：{}" ,i);
    }
}

@Slf4j(topic = "c.JoinTest1")
class JoinTest1{
    static int r1 = 0;
    static int r2 = 0;

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            r1 = 10;
        });
        Thread t2 = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            r2 = 20;
        });
        t1.start();
        t2.start();
        long start = System.currentTimeMillis();
        log.debug("join begin");
        try {
            t1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("t1 join end");
        try {
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("t2 join end");
        long end = System.currentTimeMillis();
        //最后的时间是两秒 不是三秒 因为t2在 t1.join的时候没有阻塞  已经运行一秒了
        //这里主要是因为多核 并行 单核的话 是三秒  因为单核是假的并行  微观串行 宏观并行
        log.debug("r1: {} r2: {} cost: {}", r1, r2, end - start);
    }
}

@Slf4j(topic = "c.JoinTest2")
class JoinTest2{
    static int r = 0;
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            r = 10;
        });

        long start = System.currentTimeMillis();
        t1.start();

        // 线程执行结束会导致 join 结束
        log.debug("join begin");
        try {
            //有时效的join  时间到了 如果分线程还没有结束  不等待了
            //如果分线程没到时间就结束了  也不会继续等了
            t1.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        log.debug("r: {}, cost: {}", r,  end - start);
    }
}