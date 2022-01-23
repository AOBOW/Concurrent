package AQS;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author AOBO
 * @date 2021-11-27 10:46
 * @description AQS  AbstractQueueSynchronizer 阻塞式锁和相关的同步器工具的框架
 */

//AQS用state属性表示资源状态(独占模式和共享模式)  state使用volatile加CAS保证其修改时候的原子性
//独占模式只有一个线程可以访问资源 设置ownerThread 共享模式允许多个线程访问资源
//用CAS  compareAndSetState乐观锁机制设置state状态  保证设置state的时候的线程安全
//用一个FIFO的等待队列 存储等待线程  类似Monitor的EntryList  暂停和唤醒采用park  unpark实现
//阻塞队列的头一直是dummyHead
//用条件变量实现等待 唤醒机制 支持多个条件变量  每个条件变量类似一个Monitor的WaitSet

//实现AQS框架 子类要重写下列五个方法 (不重写默认抛异常)
//tryAcquire
//tryRelease
//tryAcquireShared
//tryReleaseShared
//isHeldExclusively

//阻塞版本获取acquire 不成功加入阻塞队列  非阻塞版本尝试获取锁 tryAcquire  尝试一次

//AQS中阻塞和恢复当前线程  用的是park和unpark的机制


//自己实现一个不可重入锁
public class MyLock implements Lock {

    //独占锁
    static class MySync extends AbstractQueuedSynchronizer{
        @Override  //尝试进行一次加锁
        protected boolean tryAcquire(int arg) {
            //用CAS修改state 保证原子性 因为加锁的时候是有竞争的
            if (compareAndSetState(0, 1)){
                //设置持有者线程为当前线程 相当于Monitor中的Owner
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (Thread.currentThread() != getExclusiveOwnerThread() || getState() == 0){
                throw new RuntimeException("当前线程没有持有锁 无法释放锁");
            }
            setExclusiveOwnerThread(null);
            //这里不需要用CAS改变  因为这时没有其他线程竞争
            //这里setState要写在setExclusiveOwnerThread下面
            //因为sate是volatile的 而exclusiveOwnerThread不是
            //所以volatile的变量放在下面修改 可以在state修改之后
            //ownerThread的修改也同步到主存和各个线程  对其他线程可见
            //还能防止指令重排 保证可见性和顺序性
            setState(0);
            return true;
        }

        @Override  //是否持有独占锁
        protected boolean isHeldExclusively() {
            return getState() > 0 && Thread.currentThread() == getExclusiveOwnerThread();
        }

        public Condition newCondition(){
            return new ConditionObject();
        }
    }

    private final MySync sync = new MySync();

    @Override   //加锁  不成功进入等待队列等待
    public void lock() {
        //acquire调用tryAcquire 加锁不成功 放入阻塞队列 (该线程park)
        sync.acquire(1);
    }

    @Override   //加可打断锁
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override   //尝试加锁 只试一次 失败也不会阻塞
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override   //尝试加锁 不成功进入等待队列  带超时时间  时间内锁不上不等了
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override   //解锁
    public void unlock() {
        //release调用tryRelease  解锁成功后对阻塞队列头结点的下一个节点进行唤醒(unpark)
        sync.release(1);
    }

    @Override   //创建条件变量
    public Condition newCondition() {
        return sync.newCondition();
    }
}

@Slf4j(topic = "c.TestMyLock")
class TestMyLock{
    public static void main(String[] args) {
        MyLock lock = new MyLock();
        new Thread(() -> {
            log.debug(Thread.currentThread().getName() + " start");
            lock.lock();
            try {
                log.debug(Thread.currentThread().getName() + " lock");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug(Thread.currentThread().getName() + " unlock");
            }finally {
                lock.unlock();
            }
        }, "t1").start();

        new Thread(() -> {
            log.debug(Thread.currentThread().getName() + " start");
            lock.lock();
            try {
                log.debug(Thread.currentThread().getName() + " lock");

                log.debug(Thread.currentThread().getName() + " unlock");
            }finally {
                lock.unlock();
            }
        }, "t2").start();
    }
}