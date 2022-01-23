package normalMethod;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author AOBO
 * @date 2021-11-18 22:20
 * @description Java内的六种状态
 */

/**
 * NEW       对应操作系统五种模型中的新建  new Thread  但没start
 * RUNNABLE  就绪  运行  部分阻塞(比如等待IO)  都是RUNNABLE状态  调用start
 * BLOCKED   等待同步锁
 * WAITING   join  wait  park
 * TIMED_WAITING   sleep  wait(long time)  join(long time)
 * TERMINATED  对应五种模型中的结束状态
 *
 *阻塞状态都不会被分配资源  必须唤醒 回到就绪状态 太能分配资源
 */
@Slf4j(topic = "c.StateTest")
public class StateTest {
    public static void main(String[] args) {
        //线程被创建了  但没有start 为新建状态
        Thread t1 = new Thread(() -> {
            log.debug("t1");
        }, "t1");

        //线程运行中为RUNNABLE状态
        Thread t2 = new Thread(() -> {
            while (true){

            }
        },"t2");
        t2.start();

        //线程执行结束 为终止状态TERMINATED
        Thread t3 = new Thread(() -> {
            log.debug("t3");
        }, "t3");
        t3.start();

        //sleep 后为TIMED_WAITING状态  有时限的等待
        Thread t4 = new Thread(() -> {
            synchronized (StateTest.class){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "t4");
        t4.start();

        //调用join后为 WAITING状态  等待程序执行
        Thread t5 = new Thread(() -> {
            try {
                t2.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t5");
        t5.start();


        //因为t4持有了当前类的锁还没有释放 所以等待锁的状态为BLOCKED
        Thread t6 = new Thread(() -> {
            synchronized (StateTest.class){

            }
        }, "t6");
        t6.start();

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.debug("t1 state : {} ", t1.getState());
        log.debug("t2 state : {} ", t2.getState());
        log.debug("t3 state : {} ", t3.getState());
        log.debug("t4 state : {} ", t4.getState());
        log.debug("t5 state : {} ", t5.getState());
        log.debug("t6 state : {} ", t6.getState());
    }
}
