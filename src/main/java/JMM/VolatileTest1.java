package JMM;

/**
 * @author AOBO
 * @date 2021-11-23 22:19
 * @description JMM  java Memory Model  Java内存模型
 */

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * JAVA内存模型(JMM)分为主存(所有线程都共享的数据)和工作内存(线程私有的  栈的缓存)
 * JMM体现在以下三个方面：
 * 1.原子性：保证指令不会受到线程上下文切换的影响
 * 2.可见性：保证指令不会受到CPU缓存的影响
 * 3.有序性：保证指令不会受到CPU指令并行优化的影响
 *
 * volatile可以保证可见性和有序性  synchronized三个都保证
 * (但synchronized不保证代码块内部的顺序性 即内部还是会有指令重排序)
 */

//volatile修饰成员变量和静态成员变量  因为局部变量本身就是线程私有的 没有可见性的问题 所以不能用volatile

//volatile和synchronized都可解决可见性的问题  但synchronized还需要关联monitor
//而volatile更加轻量  所以在解决可见性上  更推荐volatile
//但volatile不保证原子性  所以一般用于一个写线程 对应多个读线程的情况  而多个读写线程 还是要用synchronized

//volatile的实现方式：
//每次只要volatile的对象有修改  会使各个线程的缓存失效
//再次使用时  需要去主存中重新取最新值  保证了可见性

//synchronized的实现方式
//JAVA内存模型中 synchronized规定  线程加锁时 先清空工作内存
//在主存中拷贝最新变量的副本到工作内存  执行完代码
//将更改后的共享变量值刷新到主内存中  所以这里synchronized也可以解决

@Slf4j(topic = "c.VolatileTest")
public class VolatileTest1 {
    public static void main(String[] args) {
        test1();
    }

    static boolean run1 = true;

    public static void test1(){
        new Thread(() -> {
            log.debug("分线程 start");
            while (run1){
                //这里如果加了打印  不加volatile也可以停止  因为print方法内用了synchronized
//                System.out.println();

            }
        }).start();

        try {
            TimeUnit.MILLISECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //此时改成false  分线程不会结束
        //虽然共用的是一个变量  但分线程读取的是栈中缓存的run变量
        //因为分线程要频繁从主存中读取run的值
        //JIT即时编译器会将run的值缓存到线程工作内存的高速缓存中
        //减少对主存中run的访问 提高效率
        //此时在主线程修改  虽然写入了内存  但分线程的栈看不到修改
        log.debug("停止分线程");
        run1 = false;
    }

    //加了volatile之后  就保证了可见性
    //每次只要volatile的对象有修改  会使各个线程的缓存失效
    //去主存中重新取最新值  保证了可见性
    static volatile boolean run2 = true;

    public static void test2(){
        new Thread(() -> {
            log.debug("分线程 start");
            while (run2){

            }
        }).start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("停止分线程");
        run2 = false;
    }

    final static Object lock = new Object();
    public static void test3(){
        new Thread(() -> {
            log.debug("分线程 start");
            while (true){
                //synchronized也可以解决共享变量的可见性
                //JAVA内存模型中 synchronized规定  线程加锁时 先清空工作内存
                //在主存中拷贝最新变量的副本到工作内存  执行完代码
                //将更改后的共享变量值刷新到主内存中  所以这里synchronized也可以解决
                synchronized (lock){
                    if (!run1){
                        break;
                    }
                }
            }
            //不过不能这么写  因为synchronized保证可见性是在加锁和解锁的过程中和主存同步变量
            //但这样写是一个同步代码块 然后死循环  这时还是没有时机来更新变量
//            synchronized (lock){
//                while (run1){
//
//                }
//            }
        }).start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.debug("停止分线程");
        run1 = false;
    }
}
