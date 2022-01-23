package reentrantlock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

//三个线程 交替打印
@Slf4j(topic = "c.Order2")
public class Order2 {
    static int flag = 0;

    public static void main(String[] args) {
        test3();
    }
    public static void test1(){
        new Thread(() -> {
            synchronized (Order2.class){
                for (int i = 0; i < 5; i++) {
                    while (flag != 0){
                        try {
                            Order2.class.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    log.debug("a");
                    flag = 1;
                    Order2.class.notifyAll();
                }
            }
        }).start();

        new Thread(() -> {
            synchronized (Order2.class){
                for (int i = 0; i < 5; i++) {
                    while (flag != 1){
                        try {
                            Order2.class.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    log.debug("b");
                    flag = 2;
                    Order2.class.notifyAll();
                }
            }
        }).start();


        new Thread(() -> {
            synchronized (Order2.class){
                for (int i = 0; i < 5; i++) {
                    while (flag != 2){
                        try {
                            Order2.class.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    log.debug("c");
                    flag = 0;
                    Order2.class.notifyAll();
                }
            }
        }).start();
    }


    static ReentrantLock lock = new ReentrantLock();
    static Condition condition1 = lock.newCondition();
    static Condition condition2 = lock.newCondition();
    static Condition condition3 = lock.newCondition();
    public static void test2(){
        new Thread(() -> {
            lock.lock();
            try {
                for (int i = 0; i < 5; i++) {
                    while (flag != 0){
                        try {
                            condition1.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    log.debug("a");
                    flag = 1;
                    condition2.signalAll();
                }
            }finally {
                lock.unlock();
            }
        }).start();

        new Thread(() -> {
            lock.lock();
            try {
                for (int i = 0; i < 5; i++) {
                    while (flag != 1){
                        try {
                            condition2.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    log.debug("b");
                    flag = 2;
                    condition3.signalAll();
                }
            }finally {
                lock.unlock();
            }
        }).start();


        new Thread(() -> {
            lock.lock();
            try {
                for (int i = 0; i < 5; i++) {
                    while (flag != 2){
                        try {
                            condition3.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    log.debug("c");
                    flag = 0;
                    condition1.signalAll();
                }
            }finally {
                lock.unlock();
            }
        }).start();
    }

    static Thread t1 = null;
    static Thread t2 = null;
    static Thread t3 = null;
    public static void test3(){
        t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                while (flag != 0){
                    LockSupport.park();
                }
                log.debug("a");
                flag = 1;
                LockSupport.unpark(t2);
            }
        });
        t1.start();

        t2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                while (flag != 1){
                    LockSupport.park();
                }
                log.debug("b");
                flag = 2;
                LockSupport.unpark(t3);
            }
        });
        t2.start();

        t3 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                while (flag != 2){
                    LockSupport.park();
                }
                log.debug("c");
                flag = 0;
                LockSupport.unpark(t1);
            }
        });
        t3.start();
    }

}
