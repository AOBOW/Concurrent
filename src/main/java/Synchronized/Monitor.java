package Synchronized; /**
 * @author AOBO
 * @date 2021-11-21 18:43
 * @description 对象头
 */

/**
 * 每一个对象都有一个对象头 Object Header 一个object body
 * 对象头分两个区域  Mark Word 和 Klass Word
 * Klass Word记录的是对象属于哪个类  指向类对象(.class对象)
 * Mark Word：
 * 正常状态下记录四个信息： hashcode gc的年龄分代 是否偏向锁  加锁状态
 * 偏向锁的时候多记录线程号
 * 轻量级状态下记录锁记录
 * 重量级锁状态下记录monitor 对应的同步监视器
 * 轻量级锁和重量级锁时候 只有锁记录或monitor指针
 * (hashcode gc的年龄分代 是否偏向锁 都没有了 CAS到了锁记录或monitor中)
 *
 *
 * Monitor为监视器或者管程  是操作系统提供的
 * 每个Java对象都可以关联一个Monitor  同一个对象关联同一个Monitor
 * 在给对象上了重量级锁(如synchronized)之后
 * 对象头中的Mark Word就会被设置指向Monitor对象
 * Monitor中还会记下对应对象的hashcode 分代年年龄信息
 * 当释放所得时候  会重置会对象的Mark Word
 *
 * 每次需要上锁时 线程通过同步监视器对象的对象头的Mark Word找到对应的Monitor
 * Monitor中又分Owner EntryList WaitSet
 * Owner记录的是当前持有锁的线程
 * 之后再来请求的线程，被存在EntryList中，暂时挂起Blocked
 * 当Owner中的线程执行完或释放锁 Owner清空 会唤醒EntryList中的线程  再去竞争Owner(非公平)
 * WaitSet中放的是之前获得过锁  但被wait 等待notify的线程
 *
 * synchronized中如果出现异常  也会释放锁
 */
public class Monitor {
}
