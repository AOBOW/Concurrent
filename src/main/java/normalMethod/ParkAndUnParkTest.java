package normalMethod;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

//LockSupport.park() 暂停当前线程   park不释放锁
//LockSupport.unpark(暂停线程对象) 恢复某个线程运行

//但和wait和notify不同 只要unpark过了  之后的park还是会继续执行(可以先unpark 再park 还是可以运行)
//但下次park还是会阻塞  也就是一个unpark对应的能让一次park通过 无论unpark在前还是后

/**
 * 原理：每个线程都有自己的parker对象  其中有一个count标志位
 * park的作用是    检查标志位的值  如果是1  重置为0继续执行
 *                如果是0  在condition中等待 进入WAITING状态
 *                并且在被唤醒后  重置标志位为0
 *                也就是说 park就是无论如何 也要有一次将标志位从1变成0的操作
 *                可以将1变成0  就变换之后继续执行  不能将1变成0 就等待这个标志位变成1  然后执行
 * unpark的作用是  将标志位设置为1  如果condition中有线程在等待
 *                唤醒(唤醒后线程会将变成1的标志位变成0 然后继续执行)
 *
 *
 * 和interrupt不同 当打断标志位被设置为true之后  park就永远不会被阻塞了  一直到打断标志位再次为false
 * 而unpark 每次解除阻塞之后 再调用park 还是会被阻塞住
 * 也就是interrupt和unpark应该是两个系统  先检查interrupt的打断标志位
 */


//wait/notify 和 park/unpark对比
//wait/notify必须用在synchronized中 由同步监视器对象调用  但park/unpark无限制
//park/unpark可以精确的唤醒某一个线程
//park/unpark可以先unpark   而wait/notify 不能先notify 否则wait无法唤醒
@Slf4j(topic = "c.ParkAndUnParkTest")
public class ParkAndUnParkTest {
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            log.debug("start");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("park");
            LockSupport.park();   //此时线程状态为WAITING
            log.debug("resume");

            LockSupport.park();
            log.debug("resume1");
        }, "t1");

        t1.start();

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.debug("unpark");
//        t1.interrupt();   //interrupt之后  park就不会阻塞了
        LockSupport.unpark(t1);
    }
}
