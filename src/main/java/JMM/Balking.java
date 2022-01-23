package JMM;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author AOBO
 * @date 2021-11-23 23:23
 * @description 犹豫模式  防止多次调用  保证某个方法  即使多次调用也只执行一次
 */
//Balking模式  由于在一个线程发现另一个线程或本线程已经在做某一件相同的事
//那么本线程就无需再做了 直接返回
@Slf4j(topic = "c.Balking")
public class Balking {
    private Thread thread;

    private volatile boolean stop = false;

    //判断是否执行过start方法
    private volatile boolean starting = false;

    public void start() {
        //这里需要加synchronized  保证原子性  类似单例懒汉式
        synchronized (this){
            if (!starting){
                return;
            }
            starting = true;
        }
        thread = new Thread(() -> {
            while (true) {
                if (stop) {
                    log.debug("进行关闭前的一些处理工作");  //优雅的结束 不会强行结束 可以结束前做一些处理

                    //这里需要starting是volatile的
                    //因为这是分线程 分线程的修改 需要让主线程看到
                    //前面的starting的可见性可以用synchronized保证
                    //这里的可见性需要用volatile保证
                    starting = false;
                    break;
                }
                try {
                    //每隔两秒进行一次监控处理
                    TimeUnit.SECONDS.sleep(2); //此时有可能被打断  进入catch
                    log.debug("进行监控处理");          //此时有可能被打断  打断标记变为true 进入下一次循环
                } catch (InterruptedException e) {

                    log.debug("准备进入关闭状态 interrupt state {}",
                            Thread.currentThread().isInterrupted());
                }
            }
        });
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            stop = true;
            //加打断时为了立即结束  不加则是完成这次睡眠并打印后结束
            thread.interrupt();
        }
    }
}
