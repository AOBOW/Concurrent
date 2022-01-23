package Synchronized;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

//不能错误的理解锁 上锁之后 就能一直执行下去
//线程的调度是CPU做的  即使持有锁 时间片也会被分走
//只不过下次分配到资源的时候  该线程可以直接执行(因为还持有锁)
//而其他因为等待锁被阻塞住的线程 CPU就不会再分配资源(阻塞状态下CPU不会分配资源)
//一直到持有锁的线程 释放锁  被阻塞的线程唤醒
@Slf4j(topic = "c.SynchronizedTest")
public class SynchronizedTest {
    private static Integer i = 0;
    public static void main(String[] args) throws InterruptedException {
        List<Thread> list = new ArrayList<>();
        for (int j = 0; j < 2; j++) {
            Thread thread = new Thread(() -> {
                for (int k = 0; k < 5000; k++) {
                    //这里有线程安全问题  每次的锁不一样  每次Integer++加加之后 就是一个新的Integer
                    synchronized (i) {
                        i++;
                    }
                }
            }, "" + j);
            list.add(thread);
        }
        list.forEach(Thread::start);
        list.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        log.debug("{}", i);
    }
}
