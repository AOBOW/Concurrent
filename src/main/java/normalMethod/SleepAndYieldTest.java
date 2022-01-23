package normalMethod;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

//单核CPU下 如果遇到while(true)  CPU会被占满
//所以在用到while(true)的情况下  一般要加sleep或者yield
//让出CPU资源 防止空转浪费cpu

//sleep或者yield 用于不需要加锁的场景 因为这两个都不释放锁
public class SleepAndYieldTest {

}
class SleepTest1{

    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "t1");

        System.out.println(thread.getState());

        thread.start();

        try {
            //sleep是静态方法 在哪个线程被调用 就在哪个线程休眠
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //调用sleep方法后会从RUNNABLE状态变为TIMED_WAITING状态
        System.out.println(thread.getState());
    }
}

@Slf4j(topic = "c.SleepTest2")
class SleepTest2{
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            try {
                log.debug("enter sleep");

                //timeunit的可读性更好  这个就是外面包了一层 内部调用的还是Thread的sleep
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                log.debug("interrupted");
                e.printStackTrace();
            }
        }, "t1");

        thread.start();

        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //sleep的线程被打断后 会抛出InterruptedException的异常
        log.debug("interrupt");
        thread.interrupt();
    }
}

@Slf4j(topic = "c.Yield1Test")
//yield让线程从运行状态变为就绪状态 让其他线程也能来争夺CPU资源
//但依赖于系统任务调度器  该线程可能再次抢到资源  只是变为了就绪状态
//但阻塞状态的线程不会被分配到资源 需要回到就绪状态才行
class Yield1Test{
    public static void main(String[] args) {
        //优先级有十个级别 默认是5  但优先级仅仅是一个提示作用 CPU可以忽略
        //CPU比较忙 优先级高的线程机会更多  CPU闲时  优先级几乎没有作用
        //yield也是类似 任务调度器也可能被忽略

        Thread t1 = new Thread(() -> {
            int count = 0;
            while (true) {
                log.debug((count++) + "");
            }
        }, "t1");


        Thread t2 = new Thread(() -> {
            int count = 0;
            while (true) {
//                Thread.yield();   //加了yield  t2增长的就慢了很多
                log.debug(count++ + "");
            }
        }, "t2");

        t1.setPriority(Thread.MIN_PRIORITY);
        t2.setPriority(Thread.MAX_PRIORITY);

        t1.start();
        t2.start();
    }
}