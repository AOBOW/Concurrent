package JUC;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

//CyclicBarrier 循环栅栏  用来进行线程协作 等待线程满足某个计数
//这个和CountDownLatch最大的不同就是计数可以循环重用
//构造器中可以传入一个Runnable 每次循环结束之后 执行
@Slf4j(topic = "c.CyclicBarrierTest")
public class CyclicBarrierTest {
    public static void main(String[] args) {
        //这里线程池的数量要和每个循环内线程数量保持一直  让一轮里 没有执行完的线程都是await的  都执行完解除await
        //防止下一轮的任务  先执行
        ExecutorService service = Executors.newFixedThreadPool(2);
        //可以传入一个Runnable  每一轮循环的线程都执行完后  运行
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2, () -> {
            log.debug("task1 task2 finish");
        });
        for (int i = 0; i < 3; i++) {
            service.execute(() -> {
                try {
                    log.debug("task1");
                    TimeUnit.SECONDS.sleep(1);
                    cyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
            service.execute(() -> {
                try {
                    log.debug("task2");
                    TimeUnit.SECONDS.sleep(2);
                    cyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
        }
        service.shutdown();
    }
}
