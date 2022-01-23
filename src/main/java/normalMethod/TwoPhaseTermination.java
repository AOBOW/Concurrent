package normalMethod;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author AOBO
 * @date 2021-11-18 23:14
 * @description  两阶段终止模式
 */

//用 两阶段终止模式模拟一个后台日志监控的线程
@Slf4j(topic = "c.TwoPhaseTermination")
public class TwoPhaseTermination {
    private Thread thread;

    public void start() {
        thread = new Thread(() -> {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    log.debug("进行关闭前的一些处理工作");
                    break;
                }
                try {
                    //每隔两秒进行一次监控处理
                    TimeUnit.SECONDS.sleep(2); //此时有可能被打断  进入catch
                    log.debug("进行监控处理");          //此时有可能被打断  打断标记变为true 进入下一次循环
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    log.debug("准备进入关闭状态 interrupt state {}",
                            Thread.currentThread().isInterrupted());
                    //因为sleep  join  wait时如果被打断会抛异常 并且打断标志位会再变回false
                    //所以需要再执行一次打断  将打断标志位重置为true
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
    }
}

class testTwoPhaseTermination{
    public static void main(String[] args) {
        TwoPhaseTermination monitor = new TwoPhaseTermination();
        monitor.start();

        try {
            TimeUnit.SECONDS.sleep(12);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        monitor.stop();
    }
}
