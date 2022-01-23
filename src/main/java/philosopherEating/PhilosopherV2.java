package philosopherEating;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author AOBO
 * @date 2021-11-22 22:41
 * @description 哲学家就餐问题
 */
@Slf4j(topic = "c.PhilosopherV2")
public class PhilosopherV2 extends Thread {
    ChopstickV2 left;
    ChopstickV2 right;

    public PhilosopherV2(String name, ChopstickV2 left, ChopstickV2 right) {
        super(name);
        this.left = left;
        this.right = right;
    }

    @Override
    public void run() {
        //使用tryLock  没有获得锁直接放弃 进行下一次尝试
        //谁都不会因为没有获得锁而阻塞 也就没有死锁了
        while (true) {
            //　尝试获得左手筷子
            if (left.tryLock()){
                try {
                    // 尝试获得右手筷子
                    if (right.tryLock()){
                        try {
                            eat();
                        }finally {
                            right.unlock();
                        }
                    }
                }finally {
                    //要记得释放锁  如果获取右手的筷子失败了  会释放左手筷子的锁  所以不会死锁
                    left.unlock();
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

class ChopstickV2 extends ReentrantLock{
    String name;

    public ChopstickV2(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "筷子{" + name + '}';
    }
}

class Test2{
    public static void main(String[] args) {
        ChopstickV2 c1 = new ChopstickV2("1");
        ChopstickV2 c2 = new ChopstickV2("2");
        ChopstickV2 c3 = new ChopstickV2("3");
        ChopstickV2 c4 = new ChopstickV2("4");
        ChopstickV2 c5 = new ChopstickV2("5");
        new PhilosopherV2("苏格拉底", c1, c2).start();
        new PhilosopherV2("柏拉图", c2, c3).start();
        new PhilosopherV2("亚里士多德", c3, c4).start();
        new PhilosopherV2("赫拉克利特", c4, c5).start();
        new PhilosopherV2("阿基米德", c5, c1).start();
    }
}