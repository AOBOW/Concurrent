package JUC;

import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

//Semaphore 信号量 用来限制能同时访问共享资源的信号量
//Semaphore  类似于线程池

//Semaphore 也是用的AQS框架实现的  构造器中直接将传入的信号量个数设置为AQS的state
//然后使用共享锁的机制  每次acquire就检查state  大于0则减1  表示可以继续执行
//如果state小于0  则加入阻塞队列  在阻塞队列尾部新加节点
//前一个节点状态设置为-1  表示需要通知后面的节点  然后进行park  阻塞当前队列

//每次release  就将state加1(注意 这里没有检查上限 也就是不acquire  直接release  会导致许可上限扩大)
//然后唤醒阻塞队列中头节点的下一个节点(注意这里的的Node类型是Node.SHARED  所以会继续唤醒下一个  如果唤醒成功 则继续)
//该节点unpark 然后继续尝试用CAS将state减1  获得许可  如果成功 就尝试唤醒下一个  没获得许可就又一次park

//注意 ReentrantLock 是每次lock的时候 增加sate 解锁减少state
//而Semaphore 是每次acquire的时候 减少state  减少到0就阻塞  release的时候  增加state

//注意  信号量每次检查的 其实就是state  所以即使同一个线程 多次调用 也没关系

@Slf4j(topic = "c.SemaphoreTest")
public class SemaphoreTest {
    public static void main(String[] args) {
        // 1. 创建 semaphore 对象
        Semaphore semaphore = new Semaphore(3);
        // 2. 10个线程同时运行
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                // 3. 获取许可
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    log.debug("running...");
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.debug("end...");
                } finally {
                    // 4. 释放许可
                    semaphore.release();
                }
            }).start();
        }
    }

    public void test(){
        Semaphore semaphore = new Semaphore(2);
//        semaphore.release();

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
