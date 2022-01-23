package AQS;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * @author AOBO
 * @date 2021-11-27 15:49
 * @description ReentrantReadWriteLock  可重入读写锁
 */
//当读操作远远高于写操作时  使用读写锁
//可以让读和读操作并发  提高性能  但读写操作 依旧互斥

//读和读操作 不互斥
//如果调用写锁的时候 有读锁  需要等待读锁释放之后 才能获得写锁  但这时 其他的读锁  不能插队
//如果调用写锁的时候 有写锁  也是互斥的 要等待写锁释放
//如果调用读锁的时候 有写锁  (如果不是本线程的写锁)是互斥的 要等待写锁释放

//读锁是不支持条件变量的 写锁支持条件变量
//同一个线程 先获取读锁 再获取写锁 会阻塞 无法从读锁升级为写锁 会导致获取写锁永久等待
//同一个线程 先获取写锁 再获取读锁是可以的  释放写锁之后  会降级为读锁

/**
 * ReentrantReadWriteLock原理
 *
 * 读写锁用的是用一个sync同步器 因此阻塞队列和state都是同一个
 * 但State虽然是同一个  却一分为二  写锁占了低16位 读锁占了高16位
 *
 * 写锁上锁时
 * 上锁流程和ReentrantLock一样 尝试加锁
 * 如果此时没有读锁 或者其他线程的写锁(自己线程的写锁 可以重入)
 * 加锁成功  将ownerThread改成本线程并且state++
 * 这里是更改state写锁部分的状态(低16位) 0是未加锁 1加锁 大于1锁重入
 * 如果加锁失败  流程也和ReentrantLock一样  加入阻塞队列 但Node的状态为Node.EXCLUSIVE
 * 然后将前一个node的状态设置为-1 进入park()阻塞 等待前一个节点对该线程进行唤醒
 *
 * 写锁解锁时
 * 之前的操作和ReentrantLock一样  先检查State state--
 * 如果写锁部分的state(低16位)不是0  则解锁的是重入锁
 * 如果变成0了(注意这里检查的是写锁部分 低16位  因为可能释放写锁的时候  有自己线程的读锁)
 * 则说明释放写锁 ownerThread设置为null
 * 然后唤醒阻塞队列中head结点的下一个节点
 * 这时 如果下一个节点是写锁(Node.EXCLUSIVE)  那么直接还和以前一样 将下一个节点的线程唤醒 尝试CAS
 * CAS成功后将该节点的线程设置为null 然后设置该节点为新的head结点
 * 但如果下一个节点是读锁(Node.SHARED)  除了将下一个读节点唤醒  CAS将高16位加一 设置新的head之后
 * 再检查下一个节点 如果也是读锁 也进行唤醒  重复上述操作
 * 也就是说  会把阻塞队列中  写锁之前的读锁  全部都唤醒
 *
 * 也就是解锁的时候  下一个是节点是独占节点(写锁) 就只唤醒下一个节点
 * 但如果下一个节点是共享节点(读锁) 就继续唤醒  接下来的读锁都唤醒  一直到队列为空 或者碰到写锁
 * 这就是读读能够并发的原因
 *
 *
 * 读锁上锁时
 * 上锁流程和ReentrantLock不一样了 因为读锁是共享锁 所以走的是 acquireShared 和 tryAcquireShared
 * 此时检查是否有其他线程的写锁 有则加锁失败(其他线程的读锁 和本线程的写锁 都能加读锁)
 * 加锁失败则加入阻塞队列  但此时的Node为Node.SHARED类型和独占锁的node.EXCLUSIVE唤醒流程上有区别
 * 之后和独占锁流程一样 将前一个节点的状态设置为-1  然后本线程park()阻塞 等待前一个节点对该线程唤醒
 * 如果可以加读锁  则改变state 高16位为读锁的个数
 *
 * 读锁释放的时候 会将state状态的高16位减1 如果 减1之后  高位仍然大于1  则说明还有其他读锁在
 * 如果减1之后  state为0了 说明没有锁存在了(读锁写锁都没有 因为读锁解锁后 无论是还有读锁 还是还有写锁
 * 都不需要唤醒后面的节点 只有读锁解锁后  没有任何锁了 才需要唤醒后面的节点)
 * 唤醒后面的节点  流程时和写锁的一样
 * 但基本上此时阻塞队列要不为空  要不阻塞的就是个写锁  然后给写锁唤醒  CAS改状态
 * 成功则更改ownerThread  然后将当前head线程设置为null  成为dummyHead
 *
 *
 * 注意
 * 1.上写锁的时候  才会设置ownerThread  而上读锁不会设置(因为读锁共享  这里只有一个位置)
 * ownerThread就是当前这个线程持有这个锁的独占锁
 *
 * 2.读锁和写锁都是进入同一个阻塞队列 只不过读锁是Node.SHARED 写锁是Node.EXCLUSIVE的
 * 并且唤醒读锁时  后面的读锁一起唤醒  而唤醒写锁时  只唤醒一个写锁
 */
public class ReentrantReadWriteLockTest {
    public static void main(String[] args) {
        DataContainer container = new DataContainer();
        new Thread(() -> {
            container.write(2);
        }, "t1").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            System.out.println(container.read());
        }, "t2").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            System.out.println(container.read());
        }, "t3").start();
    }
}

@Slf4j(topic = "c.DataContainer")
class DataContainer {
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private Object data;

    public Object read() {
        readLock.lock();
        try {
            log.debug("{} 获取读锁", Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return data;
        } finally {
            log.debug("{} 释放读锁", Thread.currentThread().getName());
            readLock.unlock();
        }
    }

    public void write(Object data) {
        writeLock.lock();
        try {
            log.debug("{} 获取写锁", Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.data = data;
        } finally {
            log.debug("{} 释放写锁", Thread.currentThread().getName());
            writeLock.unlock();
        }
    }
}

class CachedData {
    Object data;
    // 是否有效，如果失效，需要重新计算 data
    volatile boolean cacheValid;
    final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    void processCachedData() {
        rwl.readLock().lock();
        if (!cacheValid) {
            // 获取写锁前必须释放读锁
            rwl.readLock().unlock();
            rwl.writeLock().lock();
            try {
                // 判断是否有其它线程已经获取了写锁、更新了缓存, 避免重复更新
                if (!cacheValid) {
                    data = "...";
                    cacheValid = true;
                }
                // 降级为读锁, 释放写锁, 这样能够让其它线程读取缓存
                rwl.readLock().lock();
            } finally {
                rwl.writeLock().unlock();
            }
        }
        // 自己用完数据, 释放读锁
        try {
//            use(data);
        } finally {
            rwl.readLock().unlock();
        }
    }
}