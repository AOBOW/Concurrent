package ConcurrentCollection;

//基于链表的阻塞队列
//初始化节点的时候  也是使用dummyHead来占位  新加的第一个节点则加在dummyHead后面 依次向后(尾插法)
//每次take的时候  则将dummyHead后的第一个节点的值返回 断开dummyHead  将第一个节点的item设置为null
//成为新的dummyHead(和AQS中阻塞队列的出队方式类似)

// LinkedBlockingQueue 的高明之处使用了两把锁 put和take的时候 分别用不同的锁
// putLock锁住last  takeLock锁住head  使两个线程可以同时进行put和take
// 同时两把锁的实现也得益于dummyHead  因为takeLock实际上一直锁的是dummyHead
// 当只有dummyHead的时候  也就是说队列为空 take操作就阻塞了  不会上锁  也就是不会出现两个锁锁住同一个node

// put的时候上putLock 相当于锁住tail 然后如果已经达到上限 则调用full condition的await 阻塞 等待take后唤醒
// 有空位则加入队列 count++(该操作用的AtomicInteger 原子的)
// 同时put操作中  如果put之后 发现还小于容量  会自己去唤醒其他线程的put 使用full condition 的 signal
// 如果此时是一个元素  还会调用empty condition的signal(不是signalAll)  让需要take 但为空时的await解开
// 同时take那边 也会有自己唤醒自己的操作 拿出一个后发现还可以拿出
// take和put的套路 都是一样的  自己唤醒自己  有一个空位时 唤醒需要put 但已经满了的await
// LinkedBlockingQueue中使用的都是signal 而不是signalAll  减小竞争


//相比于ArrayBlockingQueue  LinkedBlockingQueue是懒加载的  而ArrayBlockingQueue强制有界  并且会提前生成数组
//并且LinkedBlockingQueue两把锁  而ArrayBlockingQueue是一把锁

//ConcurrentLinkedQueue在实现上 几乎和LinkedBlockingQueue一模一样  唯一的区别是
//ConcurrentLinkedQueue  的两把锁 不是真正的lock或者synchronized  而是乐观锁 CAS实现的
//tomcat的acceptor和poller(一个生产者 接收请求 一个消费者  读取请求) 就是采用了ConcurrentLinkedQueue
public class LinkedBlockingQueueTest {

}
