package normalMethod; /**
 * @author AOBO
 * @date 2021-11-18 23:29
 * @description interrupt方法
 */

import lombok.extern.slf4j.Slf4j;
import sun.applet.Main;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * interrupt 方法被调用后
 * 如果该线程处于阻塞状态(sleep wait join等 但park不是)  则抛出异常
 * 因为阻塞状态时也无法进行检查  所以直接抛出异常 同时在异常中打断标志位会被设置回false
 *
 * 如果该线程处于正常运行的状态  则打算标志位会被设置为true
 * 线程中可以根据监测打断标志位 来决定是否停止线程
 * isInterrupted() 可以返回当前打断标志位的状态
 * Thead.interrupted() 可以返回当前线程打断标志位的状态  同时返回后再设置为false
 *
 * 用interrupt停止线程之所以被推荐使用 是因为该方法可以优雅的结束线程
 * 不像stop(直接停止线程 可能导致锁或资源没有释放)或者system.exit()(直接杀死进程)
 *
 * 同时interrupt可以打断park的阻塞状态  使park()后的代码继续执行
 * 同时只要打断标志为True park就无法阻塞线程
 *
 * 线程在接收到interrupt的打断标记之后 可以进行一些后续处理(如解锁 释放资源等)
 * 自主决定是否关闭线程
 */

@Slf4j(topic = "c.InterruptTest")
public class InterruptTest {
    public static void main(String[] args) {
        test4();
    }
    //阻塞状态被打断 会抛出异常 同时打断标志位会被重置为false
    private static void test1(){
        Thread t1 = new Thread(()->{
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t1");
        t1.start();

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t1.interrupt();
        log.debug(" 打断状态: {}", t1.isInterrupted());
    }

    //运行被打断  打断标志位会被置为true  可以通过isInterrupted检查
    private static void test2(){
        Thread t1 = new Thread(()->{
            while (true){
                if (Thread.currentThread().isInterrupted()){
                    log.debug("检测到打断，准备结束线程");
                    break;
                }
            }
        }, "t1");
        t1.start();

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t1.interrupt();
    }

    //park()可以阻塞线程  然后内部会一致检测打断标志位 当被打断时 继续执行
    private static void test3() {
        Thread t1 = new Thread(() -> {
            log.debug("park...");
            LockSupport.park();
            log.debug("unpark...");
            log.debug("打断状态：{}", Thread.currentThread().isInterrupted());
        }, "t1");
        t1.start();

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t1.interrupt();
    }

    //Thread.interrupted()返回结果后 会重置打断标志位
    private static void test4() {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                log.debug("park...");
                LockSupport.park();
                log.debug("打断状态：{}", Thread.interrupted());
            }
        });
        t1.start();


        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t1.interrupt();
    }
}

