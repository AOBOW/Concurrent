package CAS;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author AOBO
 * @date 2021-11-24 21:55
 * @description CAS锁
 */
//CAS实现锁的机制  但CASLock只是说明原理 实际情况下不要这么做
@Slf4j(topic = "c.CASLock")
public class CASLock {
    //0代表没加锁  取值为1代表加了锁
    private AtomicInteger state = new AtomicInteger(0);

    public void lock(){
        //这里会不断消耗CPU性能
        while (true){
            //尝试将0变为1  如果其他线程已经将0变为1了 则CAS失败 需要等待解锁
            if (state.compareAndSet(0,1)){
                log.debug("lock");
                break;
            }
        }
    }

    //但这样有个问题。。。 只要调了unlock  必定解锁  无论哪个线程
    public void unlock(){
        log.debug("unlock");
        state.set(0);
    }

    public static void main(String[] args) {
        CASLock lock = new CASLock();
        new Thread(() -> {
            lock.lock();
            try {
                log.debug(Thread.currentThread().getName() + " start");
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        },"t1").start();

        new Thread(() -> {
            lock.lock();
            try {
                log.debug(Thread.currentThread().getName() + " start");
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        },"t2").start();
    }
}
