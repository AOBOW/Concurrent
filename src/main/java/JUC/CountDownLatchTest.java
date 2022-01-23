package JUC;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//CountDownLatch 用来进行线程同步协作  等待所有线程完成倒计时
//构造参数用来初始化等待计数值  await()用来等待计数归零  countDown()用来让计数减一

//CountDownLatch  也是基于AQS的  用的越是共享锁的机制
//将state设置为初始值  只要state大于0  await()就会被阻塞住
//每次countDown() state的值就会减1

//相比于join  虽然也能达到相同的效果 但join无法和线程池配合
//因为join是等待其他线程结束  而线程池的线程时用完就还回去  执行下一个请求
@Slf4j(topic = "c.CountDownLatchTest")
public class CountDownLatchTest {
    public static void main(String[] args) {
//        CountDownLatch countDownLatch = new CountDownLatch(3);
//
//        new Thread(() -> {
//            log.debug("begin 1");
//            try {
//                TimeUnit.SECONDS.sleep(1);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            countDownLatch.countDown();
//        }).start();
//
//        new Thread(() -> {
//            log.debug("begin 2");
//            try {
//                TimeUnit.SECONDS.sleep(2);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            countDownLatch.countDown();
//        }).start();
//
//        new Thread(() -> {
//            log.debug("begin 3");
//            try {
//                TimeUnit.SECONDS.sleep(3);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            countDownLatch.countDown();
//        }).start();
//
//        try {
//            log.debug("main waiting");
//            countDownLatch.await();
//            log.debug("main waiting over");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        test2();
    }

    //CountDownLatch配合线程池使用
    public static void test1(){
        ExecutorService service = Executors.newFixedThreadPool(4);
        CountDownLatch countDownLatch = new CountDownLatch(3);
        service.execute(() -> {
            log.debug("begin 1");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            countDownLatch.countDown();
        });
        service.execute(() -> {
            log.debug("begin 2");
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            countDownLatch.countDown();
        });
        service.execute(() -> {
            log.debug("begin 3");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            countDownLatch.countDown();
        });
        service.execute(() -> {
            try {
                log.debug("main waiting");
                countDownLatch.await();
                log.debug("main waiting over");
                service.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    //用countDownLatch模拟所有人都加载完  才能开始游戏
    public static void test2(){
        CountDownLatch countDownLatch = new CountDownLatch(10);
        ExecutorService service = Executors.newFixedThreadPool(10);
        Random rand = new Random();
        String[] all = new String[10];
        for (int i = 0; i < 10; i++) {
            int index = i;
            service.execute(() -> {
                for (int j = 0; j <= 100; j++) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(rand.nextInt(100));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    all[index] = j + "%";
                    // \r 覆盖上一条
                    System.out.print("\r" + Arrays.toString(all));
                }
                countDownLatch.countDown();
            });
        }

        try {
            countDownLatch.await();
            System.out.println("\n游戏开始");
            service.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
