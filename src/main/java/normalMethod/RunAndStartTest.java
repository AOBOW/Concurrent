package normalMethod;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "c.RunAndStartTest")
public class RunAndStartTest {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            log.debug("running...");
        });

        //直接执行run只是一个普通方法
        thread.run();

        //新建后  还没有运行的阶段 是NEW的状态
        System.out.println(thread.getState());

        //同一个线程start只能调用一次  如果调start的时候  线程不是新建状态 就抛异常
        thread.start();
//        thread.start();

        //start调用后 是RUNNABLE的状态(就绪和运行 都是RUNNABLE)
        System.out.println(thread.getState());
    }
}
