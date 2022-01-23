package Synchronized;

/**
 * @author AOBO
 * @date 2021-11-21 20:08
 * @description 锁的分类
 */

import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;

import java.util.concurrent.TimeUnit;

//CAS  compare and swap  保证交换操作为原子的 不会被打断
/**
 * 轻量级锁：如果一个对象虽然有很多线程访问 但多线程访问的时间是错开的(没有竞争)
 *         则可用轻量级锁优化
 * 线程的栈帧中会生成锁记录对象  每个线程的栈帧都包含锁记录
 * 锁记录包含两部分 对象指针和锁记录对象的地址
 * 当一个线程要上锁时  锁记录对象的对象指针指向同步监视器对象
 * 同时将同步监视器对象的Mark Word 存到锁记录对象中
 * 将锁记录对象的地址 存到同步监视器对象的Mark Word中(就是二者用CAS做一个交换)
 * 交换如果成功了 就是加锁成功了
 *
 * 如果失败了：
 * 1.当其他线程已经持有了Object的轻量级锁(object的mark word上锁状态为00 并且指向另一个锁记录)
 *   说明有竞争  进入锁膨胀的过程(升级为重量级锁 关联monitor)
 * 2.是自己线程的持有了Object的轻量级锁(即object的mark word为00 同时锁记录指向自己线程的锁记录)
 *   则生成一个新的锁记录指向object 且这时锁记录对象地址为null(因为无法再和object用CAS交换了)
 *   用来表示synchronized锁重入的次数  此时为synchronized的锁重入
 *
 * 解锁的时候 如果当前锁记录的地址值为null 则为重入锁 直接删除就行
 * 如果不是  则用CAS再将Mark Word和对象头的地址值做交换 成功则解锁成功
 * 如果这时解锁失败 说明当前对象已经进行锁膨胀 变为重量级锁
 * 则进入重量级锁的解锁流程
 * (就是根据object的对象头的Mark Word记录的monitor的指针
 * 找到monitor 然后monitor的owner清空 唤醒EntryList在阻塞的线程
 * 但还是需要用CAS将原来锁记录中记下的Mark Word还给同步监视器的对象头)
 *
 * 锁膨胀：轻量级锁升级为重量级锁
 * (轻量级锁加锁时 发现已经有其他线程的轻量级锁了
 *  则此时该对象升级为重量级锁 给object关联Monitor
 *  并将持有轻量级锁的线程放入Owner中
 *  新来的请求放入EntryList中并阻塞该线程
 *  解锁后对EntryList中的线程进行唤醒)
 *
 *  重量级锁(或锁膨胀)时 线程的栈帧中也还是要有锁记录的 要记录当前线程指向哪个同步监视器对象的object
 *  只不过无法用CAS将自己的地址和同步监视器对象的对象头的mark word交换了
 *
 *  自旋优化：当重量级锁的时候  不会马上就进入阻塞状态  会先进行自旋操作
 *  就是循环访问该同步监视器对象  看持有锁的线程是否释放锁 如果等到了直接进行加锁
 *  但如果自旋超过一定次数还是没有等到 才会进入阻塞状态
 *  自旋优化是为了减少线程被阻塞和唤醒的开销
 *
 *
 *  偏向锁：如果一个同步监视器对象 一直是一个线程在使用  则将该对象设置为偏向锁
 *  不需要每次都进行轻量级锁的CAS操作(将锁记录的地址和同步监视器对象的信息进行交换)
 *  直接设置为偏向锁  该线程遇到这个锁 直接进入
 *
 *  偏向锁只有在第一次加锁的时候 用CAS操作将线程的ID写到同步监视器对象的对象头的Mark Word上
 *  之后只要没有其他线程来竞争  这个同步监视器对象则一直归该线程所有 只需要检查线程ID是否还是自己
 *
 *  当偏向锁是开启的时   创建一个对象  默认就是偏向锁状态 101(只不过前面的线程ID 开始是0)
 *
 *  线程在使用偏向锁后不会主动释放 在有其他线程也要获取这个锁时
 *  会设置对象的锁为不可偏向状态 并且升级为轻量锁
 *
 *  当偏向锁是开启状态时：
 *  首先是偏向锁
 *  如果在不同的时段 有不同的线程要持有锁 撤销偏向锁 升级为轻量级锁
 *  (Mark Word变为Normal状态 和栈帧的锁记录CAS交换)
 *  撤销偏向状态(但向同一个线程撤销偏向状态超过20次之后  会触发批量重偏向 之后是重偏向 不是升级轻量级锁了)
 *  如果锁住的情况下有竞争  升级为重量级锁(关联Monitor进行加锁)
 *
 *  所以撤销偏向锁的几种情况(对象从可偏向变成不可偏向 以下状态时就算解锁也不能再偏向了，对象头变了)：
 *  1.有其他线程竞争竞争 升级为轻量级锁
 *  2.调用了hashcode 因为偏向锁的对象头无法存hashcode(存了线程ID)
 *    变为normal状态 之后上锁也上轻量级锁
 *  3.调用wait notify时  因为wait notify只有重量级锁才有
 *    调用wait后要加入重量级锁的monitor中的waitSet中
 *
 *  批量重偏向：默认为偏向锁  当在不同时段 有另一个线程访问  从偏向变为轻量级锁
 *  当这种改变达到一定数量后(20次) 证明此时是这个线程一直在独占程序
 *  进行批量重偏向  剩下的线程就不撤销偏向状态变为轻量级锁了
 *  而是从偏向原来的线程  变为偏向新的线程
 *
 *  批量撤销：当撤销偏向超过40次 jvm会认为竞争太激烈了 根本不该偏向 整个类所有对象都为不可偏向的
 *  即使是新建的对象 也是不可偏向的
 *
 *  批量重偏向 和批量撤销的次数 都是统计的一个类中发生的次数
 *  作用的也是这个类中对象的上锁解锁操作
 *
 *
 *  JIT 即时编译器  会对Java代码进行进一步优化
 *  如果局部变量根本不会逃离方法的作用范围
 *  也就是该局部变量根本不会被共享  也就不会有线程安全问题
 *  这时对这个局部变量加锁没有任何意义
 *  这时JIT就会优化同步监视器为该对象的synchronized代码  将锁去掉
 */
@Slf4j(topic = "c.LockType")
public class LockType {
    public static void main(String[] args) {
        //正常情况下对象头的后三位是001  0为偏向状态(关闭)  01为无锁的状态
        Dog dog = new Dog();
        log.debug(ClassLayout.parseInstance(dog).toPrintable());

        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //偏向锁的生效有延迟 JVM刚启动时还没生效
        //需要过一会才能开启偏向锁
        //可以设置JVM参数将延迟设置为0 -XX:BiasedLockingStartupDelay=0
        //偏向锁对象头的后三位是101  1为是否开启偏向锁  01为无锁的状态
        Dog dog1 = new Dog();
        //调用过hashcode后  可偏向的对象会撤销可偏向状态 因为偏向锁的mark word中没有hashcode
        //需要变为normal状态  才有hashcode 且hashcode用的时候才会产生 不用的时候为0
        //但轻量级锁和重量级锁调用hashcode不会有问题  不需要改变
        //因为上轻量级锁的时候 hashCode CAS交换到栈帧的锁记录中
        //上重量级锁的时候 hashCode存在monitor中  解锁的时候会还回到对象的对象头的mark word中
        //而偏向锁的状态中  没有位置存hashcode  所以只能禁用该对象的偏向状态
        //所以对象存了hashCode  就不会上偏向锁了
        dog1.hashCode();
        log.debug(ClassLayout.parseInstance(dog1).toPrintable());

        synchronized (dog1){
            //加锁后 同步监视器对象的对象头加上了线程ID
            log.debug(ClassLayout.parseInstance(dog1).toPrintable());
        }
        //解锁后 同步监视器对象的对象头没有变  还是有线程ID  就是记录了上次进入的线程的ID
        //下次如果还是这个线程 就可以直接进入

        //线程在使用偏向锁后不会主动释放 需要在有其他线程也要获取这个锁时
        //会设置对象的锁为不可偏向状态 并且升级为轻量锁
        log.debug(ClassLayout.parseInstance(dog1).toPrintable());
    }
}

class Dog{

}

//只要有两个锁同时在竞争 那就是重量级锁的状态
//只有一个线程 是偏向锁(开启偏向锁的情况)
//有两个线程  但彼此时间段不同  是轻量级锁
//只有在一个线程持有锁 这时又有一个线程来竞争同一把锁时  会变为重量级锁
@Slf4j(topic = "c.Test")
class Test{
    public static void main(String[] args) {
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Dog dog = new Dog();
        log.debug(ClassLayout.parseInstance(dog).toPrintable());

        new Thread(() -> {
            synchronized (dog){
                log.debug(ClassLayout.parseInstance(dog).toPrintable());
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "t1").start();

        new Thread(() -> {
            log.debug(ClassLayout.parseInstance(dog).toPrintable());
            synchronized (dog){
                log.debug(ClassLayout.parseInstance(dog).toPrintable());

            }
        }, "t2").start();
    }
}
