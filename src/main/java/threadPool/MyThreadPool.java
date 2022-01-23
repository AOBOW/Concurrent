package threadPool;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//线程池 拒绝策略这里  是策略模式
//将策略的选择权下放给调用者  由调用者决定用哪种策略
//而不是写死在代码中  将算法 从上下文中提取出来  用拒绝策略 代替if else

@Slf4j(topic = "c.ThreadPoolTest")
public class MyThreadPool {
    public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool(2, 1000,
                TimeUnit.MILLISECONDS, 10, ((queue, task) -> {
                    //1.此时拒绝策略是死等  调用没有时限的put方法
//                    queue.put(task);
                    //2.带超时的等待
//                    queue.put(task, 500, TimeUnit.MILLISECONDS);
                    //3.让调用者放弃任务执行
//                    log.debug("放弃 {}", task);
                    //4.让调用者抛出异常  会打断接下来的任务
                    //后面的任务因为是一个线程的  没法放进队列了
                    // 但不会影响其他线程里已经执行的任务
//                    throw new RuntimeException("任务执行失败" + task);
                    //5.让调用者自己执行
                    task.run();

        }) );
        for (int i = 0; i < 15; i++) {
            int j = i;
            threadPool.execute(() -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("任务执行 {}", j);
            });
        }
    }
}

//拒绝策略
@FunctionalInterface
interface RejectPolicy<T>{
    void reject(BlockingQueue<T> queue, T task);
}

@Slf4j(topic = "c.ThreadPool")
class ThreadPool{
    //任务队列
    private BlockingQueue<Runnable> taskQueue;

    //线程集合  用一个Work类对线程类进行包装
    private HashSet<Worker> workers = new HashSet<>();

    //核心线程数
    private int coreSize;

    //获取任务的超时时间和单位
    private long timeout;
    private TimeUnit unit;

    private RejectPolicy<Runnable> rejectPolicy;

    public ThreadPool(int coreSize, long timeout, TimeUnit unit, int queueCapacity, RejectPolicy<Runnable> rejectPolicy) {
        this.coreSize = coreSize;
        this.timeout = timeout;
        this.unit = unit;
        this.taskQueue = new BlockingQueue<>(queueCapacity);
        this.rejectPolicy = rejectPolicy;
    }

    //执行任务
    public void execute(Runnable task){
        //当任务数没有超过coreSize时  直接交给worker对象执行

        //如果任务数超过了coreSize时  加入任务队列暂存
        synchronized (workers){
            if (workers.size() < coreSize){
                Worker worker = new Worker(task);
                log.debug("新增worker {}， task {}", worker, task);
                workers.add(worker);
                worker.start();
            }else {
//                taskQueue.put(task);
                //添加任务时的情况
                //1.一直等 调用不带时间参数的put
                //2.带超时的等待
                //3.让调用者线程放弃任务
                //4.让调用者抛出异常
                //5.让调用者自己执行
                taskQueue.tryPut(rejectPolicy, task);
            }
        }
    }

    //内部类可以直接用外部类的成员变量
    class Worker extends Thread{
        private Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            //task不为空 执行任务
            //当task执行完毕 再接着从任务队列获取任务 并执行
            //这里如果用不传时间的方法 就会一直等待 用传时间的方法 就会超时后停止
//            while (task != null || (task = taskQueue.take()) != null){
            while (task != null || (task = taskQueue.take(timeout, unit)) != null){
                try {
                    log.debug("正在执行任务 {}", task);
                    task.run();
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    task = null;
                }
            }
            //当自己的任务执行完了 等待队列里也没有等待的任务了
            synchronized (workers){
                log.debug("移除worker {}", this);
                workers.remove(this);
            }
        }
    }
}

//用来存没有线程执行的任务
@Slf4j(topic = "c.BlockingQueue")
class BlockingQueue<T>{
    //1.任务队列  ArrayDeque性能比LinkedList更好
    private Deque<T> deque = new ArrayDeque<>();

    //2.锁 要锁住头和尾的请求
    private ReentrantLock lock = new ReentrantLock();

    //3.生产者条件变量(提交任务的线程)  阻塞队列满了  提交请求的线程阻塞
    private Condition fullWaitSet = lock.newCondition();

    //4.消费者条件变量(线程池中的线程)  阻塞队列空了  拿任务的线程阻塞
    private Condition emptyWaitSet = lock.newCondition();

    //5.容量上限
    private int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    //阻塞获取
    public T take(){
        lock.lock();
        try {
            while (deque.isEmpty()){
                try {
                    //阻塞队列为空 进入消费者条件变量阻塞
                    emptyWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //获取队列头的元素
            T t = deque.removeFirst();
            fullWaitSet.signalAll();
            return t;
        }finally {
            lock.unlock();
        }
    }

    //带超时的阻塞获取
    public T take(long timeout, TimeUnit unit){
        lock.lock();
        try {
            //根据传入的时间单位  将timeout 统一转换为纳秒
            long nanos = unit.toNanos(timeout);
            while (deque.isEmpty()){
                try {
                    //区分是时间耗尽的等待  而不是被唤醒的等待
                    if (nanos <= 0){
                        return null;
                    }

                    //阻塞队列为空 进入消费者条件变量阻塞
                    //awaitNanos返回的是剩余的等待时间  如果等待时间结束就为0
                    nanos = emptyWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //获取队列头的元素
            T t = deque.removeFirst();
            fullWaitSet.signalAll();
            return t;
        }finally {
            lock.unlock();
        }
    }

    //阻塞添加
    public void put(T task){
        lock.lock();
        try {
            while (deque.size() >= capacity){
                try {
                    //阻塞队列满了 进入生产者条件变量阻塞
                    log.debug("等待加入任务队列 {}", task);
                    fullWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //添加到队列头尾部
            deque.addLast(task);
            log.debug("加入任务队列 {}", task);
            emptyWaitSet.signalAll();
        }finally {
            lock.unlock();
        }
    }

    //带超时的阻塞添加
    public boolean put(T task, long timeout, TimeUnit unit) {
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout);
            while (deque.size() >= capacity){
                try {

                    //超过等待添加任务的时间  添加失败
                    if (nanos <= 0){
                        log.debug("超过等待时间 放弃任务 {}", task );
                        return false;
                    }

                    //阻塞队列满了 进入生产者条件变量阻塞
                    log.debug("等待加入任务队列 {}", task );
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //添加到队列头尾部
            deque.addLast(task);
            log.debug("加入任务队列 {}", task);
            emptyWaitSet.signalAll();
        }finally {
            lock.unlock();
        }
        return true;
    }

    public void tryPut(RejectPolicy<T> rejectPolicy, T task){
        lock.lock();
        try {
            //判断队列是否已满
            if (deque.size() >= capacity){
                //将全力下放给rejectPolicy
                rejectPolicy.reject(this, task);
            }else {
                deque.addLast(task);
                log.debug("加入任务队列 {}", task );
                emptyWaitSet.signalAll();
            }
        }finally {
            lock.unlock();
        }
    }

    //获取队列大小
    public int size(){
        lock.lock();
        try {
            return deque.size();
        }finally {
            lock.unlock();
        }
    }
}
