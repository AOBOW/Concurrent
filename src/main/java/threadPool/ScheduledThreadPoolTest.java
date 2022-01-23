package threadPool;

import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//定时任务  使用ScheduledThreadPoolTest 定时任务线程池
//在这个类出现之前可以用Timer实现 但Timer是单线程串行执行的
//如果一个任务抛出异常  后面的都没法执行了
@Slf4j(topic = "c.ScheduledThreadPoolTest")
public class ScheduledThreadPoolTest {
    public static void main(String[] args) {
        test1();
    }

    private static void test1() {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

        //延时执行任务
        service.schedule(() -> {
            log.debug("task 1");
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 2, TimeUnit.SECONDS);

        service.schedule(() -> {
            log.debug("task 2");
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 2, TimeUnit.SECONDS);

        //定时执行任务  每隔几秒  执行一次
        //但如果前一个任务到了间隔时间还没有执行完  是等待前一个执行完再执行
        //不过延迟时间是从任务开始的时间计算的
        service.scheduleAtFixedRate(() -> {
            log.debug("task 3");
        },3, 1, TimeUnit.SECONDS);

        //定时执行任务  每隔几秒  执行一次
        //但这个是等待前一个任务完全执行完  再计算delay时间  隔一段时间再执行
        service.scheduleWithFixedDelay(() -> {
            log.debug("task 3");
        },3, 1, TimeUnit.SECONDS);
    }

    //期望每周四18:00定时执行
    public static void test2(){
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

        //获取当前时间时间戳
        LocalDateTime now = LocalDateTime.now();
        //当周周四18:00的时间戳
        LocalDateTime time = now.withHour(18).withMinute(0).withSecond(0).withNano(0).with(DayOfWeek.THURSDAY);

        //如果当前时间 大于本周周四  则需要找到下周周四
        if (now.compareTo(time) > 0){
            time = time.plusWeeks(1);
        }

        //initialDelay  当前时间和周四18点的时间差
        long initialDelay = Duration.between(now, time).toMillis();

        //period  一周的时间间隔
        long period = TimeUnit.DAYS.toMillis(7);

        service.scheduleAtFixedRate(() -> {
            log.debug("do task");
        }, initialDelay, period,TimeUnit.MILLISECONDS);
    }
}
