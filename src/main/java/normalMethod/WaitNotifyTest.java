package normalMethod;

//同步监视器对象调用wait()  会让Monitor中Owner指向的线程  进入Monitor的WaitSet等待区 进入Waiting状态
//同时释放锁(清空Owner 唤醒EntryList中的Blocked的等待队列)
//同步监视器对象调用notify()/notifyAll()  会让Monitor唤醒WaitSet中等待的一个/全部线程
//让这些线程进入EntryList  等待当前持有锁的Owner线程释放锁 然后争夺锁

//wait() 实际上调用的是wait(0)  没有时限的等待 如果传入大于0的参数  会等待一段时间  没有唤醒自动结束等待
//其实还有一个两个参数的wait  第二个参数是纳秒  但那个实际上只把第一个毫秒加1(毫无意义)  本质还是调的一个参数的wait()

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * wait和sleep的区别
 * sleep是Thread的静态方法  而wait是Object的非静态方法
 * 也就是所有对象都有wait方法(因为所有对象都可以做同步监视器)
 * sleep不释放锁  wait释放锁
 * sleep可以在任何地方调用  wait只能在synchronized中 由同步监视器对象调用
 *
 * wait(long n) 和 sleep(long n)调用后 线程状态都为TIMED_WAITING
 */
@Slf4j(topic = "c.WaitNotifyTest")
public class WaitNotifyTest {
    //作为锁的对象 一般会声明为final的  防止引用被重新赋给其他对象  锁不住了
    static final Object lock = new Object();

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (lock){
                log.debug("分线程获得锁");
                try {
//                    TimeUnit.SECONDS.sleep(5);
                    lock.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("分线程结束");
            }
        }).start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized (lock){
            log.debug("主线程获得锁");
        }
    }
}

//wait要放在while中 否则会有虚假唤醒的问题 不该叫醒这个线程却叫醒了 然后还没有再次判断 程序继续运行了
@Slf4j(topic = "c.TestCorrectPosture")
class TestCorrectPostureStep {
    static final Object room = new Object();
    static boolean hasCigarette = false;
    static boolean hasTakeout = false;

    public static void main(String[] args) {


        new Thread(() -> {
            synchronized (room) {
                log.debug("有烟没？[{}]", hasCigarette);
                while (!hasCigarette) {   //wait用在while循环中
                    log.debug("没烟，先歇会！");
                    try {
                        room.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("有烟没？[{}]", hasCigarette);
                if (hasCigarette) {
                    log.debug("可以开始干活了");
                } else {
                    log.debug("没干成活...");
                }
            }
        }, "小南").start();

        new Thread(() -> {
            synchronized (room) {
                Thread thread = Thread.currentThread();
                log.debug("外卖送到没？[{}]", hasTakeout);
                while (!hasTakeout) {
                    log.debug("没外卖，先歇会！");
                    try {
                        room.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("外卖送到没？[{}]", hasTakeout);
                if (hasTakeout) {
                    log.debug("可以开始干活了");
                } else {
                    log.debug("没干成活...");
                }
            }
        }, "小女").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            synchronized (room) {
                hasTakeout = true;
                log.debug("外卖到了噢！");
                room.notifyAll();
            }
        }, "送外卖的").start();


    }

}

