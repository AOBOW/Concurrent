package AQS;

/**
 * @author AOBO
 * @date 2021-11-27 12:25
 * @description ReentrantLock 原理
 */

/**
 * ReentrantLock
 * 内部实现了公平锁和非公平锁的sync 非公平锁更常用
 * 非公平锁：
 * lock加锁时  先尝试用CAS更改状态(将state由0变1)  成功将owner设置为当前线程 加锁成功
 * 不成功进入acquire  acquire中会调用tryAcquire再次尝试CAS更改状态 试图加锁
 * tryAcquire判断是否能加锁 和是否是重入锁 成功加锁 则结束
 * 还不成功 进入等待队列
 * 等待队列使用双向链表实现的 维护了一个head 一个tail
 * 其中head永远是dummyHead  也就是第一个Node没有对应的线程  head之后是带有线程的等待的Node
 * 链表的知识 使用一个dummyHead  永远维护对头的控制权
 *
 * 进入等待队列的流程  首先是构建一个Node 然后加入等待队列的队尾
 * 在加入等待队列后 执行acquireQueued()方法  这个方法内部是个死循环 一直到加锁成功才能出来
 * 如果该线程是排在第一个的节点(前一个就是head) 会再尝试一次CAS
 * 如果CAS还是没成功 或者不是第一个  会将前一个节点的waitStatus状态由于0改为-1
 * 状态为-1的节点 表示有责任唤醒下一个节点的线程(因为该线程要进入阻塞  需要有一个线程之后唤醒这个线程
 * 告诉这个线程可以重新开始竞争锁  这个唤醒的工作交给前一个节点  所以是否需要unpark是由
 * 当前节点的前驱结点的waitStatus == Node.SIGNAL(-1) 决定的  而不是本节点的waitStatus 决定的)
 * 最后将该节点进行park 调用LockSupport.park(this)  开始进入阻塞状态(等待锁)
 *
 * 所以等待队列中 等待的线程 除了tail的node 其他node的waitStatus都是-1 就是有责换唤醒下一个node的线程
 *
 * 当释放锁的时候  会首先将state设置为0 将ownerThread设置为null
 * 然后检查等待队列里是否有head  head的状态是否为-1  如果是-1唤醒后面的线程 调用LockSupport.unpark
 * 这时会在前面的park的位置被唤醒  重新参与竞争(非公平锁中 会和新来的尝试加锁的线程竞争)
 * (在acquireQueued()的for循环中被唤醒  然后如果是头结点的下一个节点 再去CAS抢锁  然后退出
 * 这个acquireQueued方法的for循环是个死循环  唯一出循环的方式 就是是改线程的节点是头结点的下一个节点
 * 然后CAS 成功抢到锁(改变状态 将自己线程设置为OwnerThread))
 * 然后头结点会设置为当前节点(原来的head节点断开 等待GC) 并将thread设置为null  即head始终是个dummyHead
 *
 * 所以只要进入阻塞队列了  一定是一个一个被唤醒的 每次有锁释放 将头结点的下一个节点里的线程唤醒 来竞争
 * 其中非公平的点体现在于 其他的线程 是可以插队的(lock方法中  一上来就会进行一次加锁的CAS操作)
 * 这个被唤醒的线程是和新来的线程进行竞争
 * 有可能还是会失败  失败后再将head的waitStatus设置为-1  进入park阻塞 等待再次被唤醒
 *
 * 但只有head的下一个线程  有可能被唤醒 有可能进行竞争 如果是排在后面的节点
 * 必须等待前面的释放了才能参与竞争  这也是后面的节点 可能会产生饥饿现象(一直拿不到锁 阻塞着)的原因
 *
 *
 * 公平锁的实现原理：
 * 非公平锁中  上锁的时候  不用检查等待队列  直接尝试CAS 进行加锁
 * 新来的线程可以和等待队列中被唤醒的第一节点的线程 进行竞争
 * 而公平锁中  上锁的时候   是先检查阻塞队列
 * 如果阻塞队列不存在 或者该线程不是阻塞队列的第一个线程  才会尝试CAS加锁
 * 如果有阻塞队并且阻塞队列的第一个线程还不是当前线程  该线程会加入阻塞队列的尾部
 * 而之前说过  阻塞队列中  是公平的  一个一个唤醒的
 * 所以说 公平锁就是有阻塞队列的时候 全部依赖于阻塞队列进行一个一个上锁  没有阻塞队列才直接加锁
 * 每次都只有一个线程在尝试进行加锁 没有竞争  所以也没有饥饿的现象
 *
 *
 * 可重入的原理  用state的状态来控制
 * 如果是同一个线程  再次进行加锁 调用到tryAcquire方法
 * 这时检测到 state > 0 并且当前线程 == ownerThread线程  则表示发生了锁重入 state++
 * 释放的时候  如果是当前线程   会将state--
 * 只有当state变回0了 才重新设置ownerThread为null  彻底释放锁
 * 否则只是释放了一层重入锁 该线程还持有锁
 *
 * 可重入锁的优点是可以一定程度的避免死锁
 *
 * Synchronized也是可重入锁
 * 每一个锁对象拥有一个计数器和一个指向持有该锁的线程的指针。
 * 当执行monitorEnter时 如果目标锁对象的计数器为零 则说明它没有被其他线程所持有
 * JVM会将该锁对象的持有线程设置为当前线程 并且将计数器加1
 * 当目标对象的计数器不为0的情况下  如果锁对象的持有线程是当前线程  JVM将计数器加1
 * 不是当前线程则进入EntryList等待
 * 当执行monitorExit时  JVM将锁对象的计数器减1  计数器为0代表锁被释放
 *
 * 可打断原理
 * 当上的是不可打断的锁 lock()时  调用interrupt 将park状态的线程唤醒了
 * 会执行Thread.interrupted()  返回打断标记 并将打断标记清除 重新设置为false
 * 这是为了让该线程可以再次被park住(如果打断标记为true 则park不生效)
 * (让之前的打断无效  被唤醒的线程会继续执行acquireQueued()中死循环的逻辑)
 * 如果条件不满足 继续被park住
 * 当条件满足了  上锁成功了 出循环后  会重新执行interrupt
 * 将打断标志设置回来(因为之前的打断标记被Thread.interrupted清除了  恢复真实的打断标记
 * 当然了  如果没执行过park 或者是用unpark停止的park(不是用打断中断的)
 * 则acquireQueued()返回的打断标记是false 就不用再重新设置了)
 *
 * 总之不可打断模式 即使它被打断，仍会驻留在 AQS 队列中，一直要等到获得锁后方能得知自己被打断了
 * 打断时  只是记录了打断信息  也就是阻塞过程中 打断后也会重新将打断标记设置回false
 * 即使被打断 仍会驻留在AQS队列中 获得锁后才能继续运行
 * 所以interrupt对直接lock的线程  没有作用
 *
 * 当上的是可打断的锁 tryLock()时
 * 进入的是doAcquireInterruptibly()方法 其他的实现和acquireQueued()都一样
 * 但如果检测到打断  不是记录打断信息继续执行了  而是直接抛出异常
 *
 *
 * 条件变量  类似monitor的Waitset
 * 每个条件变量对应一个conditionObject实例  内部维护了一个等待队列  是用链表实现的
 * await操作
 * 创建当前线程的node加入到conditionObject的等待队列中 状态为-2 表示在条件变量中阻塞
 * 将当前线程的锁释放 ownerThread设置为null  state设置为0(即使当前锁时重入锁 State > 0 也变为0)
 * 同时唤醒等待队列中的下一个节点 竞争锁
 *
 * signal操作
 * 唤醒条件变量对象conditionObject的等待队列中的第一个节点的线程
 * 将这个节点从条件变量的队列中断开  并转移到等待锁的 AQS阻塞队列的队尾
 * 这个node节点状态变为0  前一个节点(原来的tail)  状态变为-1  之后可以唤醒这个线程
 *
 * await  signal 和wait notify一样 必须是锁的持有者 才能调用
 */

public class ReentrantLockTest {
}
