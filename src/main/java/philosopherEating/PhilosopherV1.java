package philosopherEating;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author AOBO
 * @date 2021-11-22 22:41
 * @description 哲学家就餐问题
 */
@Slf4j(topic = "c.PhilosopherV1")
public class PhilosopherV1 extends Thread {
    Chopstick left;
    Chopstick right;

    public PhilosopherV1(String name, Chopstick left, Chopstick right) {
        super(name);
        this.left = left;
        this.right = right;
    }

    @Override
    public void run() {
        while (true) {
            //　尝试获得左手筷子
            synchronized (left) {
                // 尝试获得右手筷子
                synchronized (right) {
                    eat();
                }
            }
        }
    }

    private void eat() {
        log.debug("eating...");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Chopstick {
    String name;

    public Chopstick(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "筷子{" + name + '}';
    }
}

class Test1{
    public static void main(String[] args) {
        Chopstick c1 = new Chopstick("1");
        Chopstick c2 = new Chopstick("2");
        Chopstick c3 = new Chopstick("3");
        Chopstick c4 = new Chopstick("4");
        Chopstick c5 = new Chopstick("5");
        new PhilosopherV1("苏格拉底", c1, c2).start();
        new PhilosopherV1("柏拉图", c2, c3).start();
        new PhilosopherV1("亚里士多德", c3, c4).start();
        new PhilosopherV1("赫拉克利特", c4, c5).start();
        new PhilosopherV1("阿基米德", c5, c1).start();

        //锁排序的时候 不会产生死锁  如果把上面的阿基米德换成c1  c5 就不会死锁了
        //武林里总有的所排序的警告 就是怕死锁
        //但会产生锁饥饿的现象  有的线程永远也得不到锁
    }
}