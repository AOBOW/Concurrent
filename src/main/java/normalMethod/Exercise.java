package normalMethod;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author AOBO
 * @date 2021-11-18 22:47
 * @description 练习
 */
@Slf4j(topic = "c.Exercise")
public class Exercise {
    public static void main(String[] args) {
        log.debug("开始");
        log.debug("开始洗水壶");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("结束洗水壶");

        log.debug("开始烧水");
        Thread thread = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(15);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("结束烧水");
        }, "t1");
        thread.start();

        log.debug("开始拿茶杯");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("结束拿茶杯");

        log.debug("开始拿茶叶");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("结束拿茶叶");

        log.debug("开始洗茶壶");
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("结束洗茶壶");

        log.debug("等待水烧开");
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.debug("泡茶");



    }
}
