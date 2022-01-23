package CAS;

//ABA问题
//CAS compareAndSet  CAS的时候 只能比较出前后的值是否一样  感知不到从A改成B又改回A
//存在一种情况  一开始值是A  另一个线程改成了B  然后又一个线程改回了A
//这时可能以为还是最开始A  也操作成功了
//(虽然这种情况下 大部分是允许的  有些情况下不能让这种CAS操作成功)
//如果希望解决ABA问题  也就是只要其他线程动过共享变量 这次CAS操作就失败

//就不能用AtomicReference  需要用AtomicStampedReference  增加版本号

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

@Slf4j(topic = "c.ABA")
public class ABA {

    static AtomicReference<String> ref = new AtomicReference<>("A");

    public static void main(String[] args) throws InterruptedException {
        log.debug("main start...");
        // 获取值 A
        String prev = ref.get();
        // 如果中间有其它线程干扰，发生了 ABA 现象
        other();
        TimeUnit.SECONDS.sleep(2);
        // 尝试改为 C
        log.debug("change A->C {}", ref.compareAndSet(prev, "C"));
    }

    private static void other() {
        new Thread(() -> {
            log.debug("change A->B {}", ref.compareAndSet(ref.get(), "B"));
        }, "t1").start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            log.debug("change B->A {}", ref.compareAndSet(ref.get(), "A"));
        }, "t2").start();
    }
}

//使用AtomicStampedReference  解决ABA 问题  增加了版本号的改动
//AtomicStampedReference的compareAndSet操作
// 必须prev值和版本号都一致的情况下  才能更新成功
@Slf4j(topic = "c.ABASolution")
class ABASolution {

    static AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);

    public static void main(String[] args) throws InterruptedException {
        log.debug("main start...");
        // 获取值 A
        String prev = ref.getReference();
        // 获取版本号
        int stamp = ref.getStamp();
        log.debug("版本 {}", stamp);
        // 如果中间有其它线程干扰，发生了 ABA 现象
        other();
        TimeUnit.SECONDS.sleep(2);
        // 尝试改为 C
        log.debug("change A->C {}", ref.compareAndSet(prev, "C", stamp, stamp + 1));
    }

    private static void other() {
        new Thread(() -> {
            log.debug("change A->B {}", ref.compareAndSet(ref.getReference(), "B", ref.getStamp(), ref.getStamp() + 1));
            log.debug("更新版本为 {}", ref.getStamp());
        }, "t1").start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            log.debug("change B->A {}", ref.compareAndSet(ref.getReference(), "A", ref.getStamp(), ref.getStamp() + 1));
            log.debug("更新版本为 {}", ref.getStamp());
        }, "t2").start();
    }
}