package threadPool;

/**
 * @author AOBO
 * @date 2021-11-25 23:42
 * @description 线程池
 */

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


//线程池使用int的高3位表示线程池状态(一共五种状态)  低29位表示线程数量
//SHUTDOWN状态  000  不会接收新任务 正在执行的任务继续 阻塞队列中的任务也会处理完  调用shutdown()方法
//STOP状态      001  不会接收新任务 正在执行的任务打断 阻塞队列中的任务也不会处理  调用shutdownnow()方法
//之所以要把两个状态用一个原子变量(ctl)来表示  是为了用一次CAS原子操作对两个状态进行赋值
//直接用位运算的或操作  就可以将两个状态合并  高三位是状态  低29位是线程数量

/**
 * 线程池
 *
 * 线程池的七个参数：
 * public ThreadPoolExecutor(int corePoolSize,
 *                           int maximumPoolSize,
 *                           long keepAliveTime,
 *                           TimeUnit unit,
 *                           BlockingQueue<Runnable> workQueue,
 *                           ThreadFactory threadFactory，
 *                           RejectedExecutionHandler handler) {)
 *
 *  corePoolSize     常驻线程数量(核心线程数量  也是池子中的最小线程数)
 *  maximumPoolSize  最大线程数量(核心线程数加救急线程数)
 *  keepAliveTime    救急线程空闲状态存活时间(就是大于corePoolSize数量 小于maximumPoolSize数量的线程 在无事可做的阶段存活的时间)
 *  unit             救急线程存活时间的单位
 *  workQueue        阻塞队列(核心线程池没有线程可以分配  再来的请求 存在阻塞队列中)
 *  threadFactory    线程工厂 用于创建线程时可以给线程起一个好的名字
 *  handler          拒绝策略
 *
 *  在创建线程池时  最开始线程池中的线程数为0
 *  当有请求到达后 线程池开始创建线程  所以线程池的线程创建也是懒加载的
 *  先去核心线程池去拿线程  核心池没有线程就新建核心线程
 *  如果核心线程池的核心线程都被占用了  请求被放在阻塞队列 进行等待
 *  当核心池都被占用  阻塞队列也满了  则创建救急线程 最多创建到最大线程数这么多
 *  这时要注意 新建的救急线程 会优先提供给新来的请求 而不会优先提供给阻塞队列中等待的请求
 *  如果核心池都被占用  阻塞队列也满了  新创建的线程也达到了最大线程数  则执行拒绝策略中的策略
 *
 *  核心线程  ->  阻塞队列  ->  救急线程  ->  拒绝策略
 *
 *  只有阻塞队列是有界队列时 才会有救急线程 如果阻塞队列时无界队列
 *  则不会有救急线程 也不会执行拒绝策略  永远是核心线程执行
 *
 *  当线程执行完请求  核心线程不会被销毁  留在线程池等待再次执行请求
 *  而救急线程如果空闲时间超过keepAliveTime  就会被销毁
 *
 *  四种基本的拒绝策略：
 *  1.AbortPolicy(默认策略)：直接抛出异常  阻止系统正常运行
 *  2.CallerRunsPolicy:调用者模式  将任务回退给调用者 使用调用者线程直接运行任务(直接run)
 *  3.DiscardOldPolicy:抛弃阻塞队列中等待最久的请求 将新来的请求放入等待队列
 *  4.DiscardPolicy:不做任何处理  直接丢弃任务(如果允许任务丢失 这是最好的一种策略)
 *
 *  很多第三方框架  也会提供自己的拒绝策略
 *  Dubbo(RPC框架)：抛异常并记日志
 *  Netty:创建新线程执行任务
 *  ActiveMQ(消息队列):带超时的等待
 *  PinPoint(链路追踪框架)：使用各个拒绝策略链  逐个尝试
 */

//线程池/连接池的好处：
//1.降低资源消耗  一直用的是重复的线程 用完就还回来  降低线程创建和销毁过程中的消耗
//2.提高响应速度  请求到达时 不需要等待线程创建 立即执行
//3.方便管理(最大数量  存活时间  拒绝策略等)

//线程池中执行的任务 如果出现异常  如果不处理  是不会抛出异常的  看不出来出了异常
//如果需要知道异常
//1.可以在任务中用try  catch进行捕获
//2.用submit方法 传入callable接口 返回future对象 future的get方法也可以得到异常信息


/**
 * TomeCat 服务器中 处理IO操作的线程池 改写了JDK的线程池  不是 核心线程 -> 阻塞队列 ->救急线程  ->拒绝策略了
 * 而是改成了  核心线程 -> 救急线程  ->  阻塞队列  ->  拒绝策略
 * 并且TomCat中 线程池的线程时守护线程
 */

@Slf4j(topic = "c.TheadPoolTest")
public class TheadPoolTest {
    //正常情况下  JDK提供的线程池 都不推荐使用 一般用自定义的线程池
    //因为fixed和single等待队列无界   cached最大线程数无界  对请求数量都没有限制
    public static void main(String[] args) {
        test5();
    }

    //newFixedThreadPool  创建固定大小的线程池  适用于任务量已知的场景
    //无救急线程  只有核心线程  且阻塞队列是无界的
    public static void test1(){
        ExecutorService service = Executors.newFixedThreadPool(2,
                new ThreadFactory() {
            AtomicInteger num = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "线程" + num.getAndIncrement());
            }
        });

        service.execute(() -> {
            log.debug("1");
        });

        service.execute(() -> {
            log.debug("2");
        });

        service.execute(() -> {
            log.debug("3");
        });
        //这个方法不会结束 因为核心线程都是用户线程 并且用完之后不销毁 一直维持运行的状态
        //需要关闭线程池才能结束
    }

    //newCachedThreadPool  缓存线程池  没有核心线程  适用于任务比较密集 但任务执行时间短的情况
    //创建的全部都是救急线程 空闲存活时间60秒  并且没有上限 Integer.MAX_VALUE
    public static void test2(){
        ExecutorService service = Executors.newCachedThreadPool();

        service.execute(() -> {
            log.debug("1");
        });

        service.execute(() -> {
            log.debug("2");
        });

        service.execute(() -> {
            log.debug("3");
        });
    }

    //阻塞队列为SynchronousQueue<>()  这个队列 没有容量
    //放进去的时候 如果没有人来取 就放不进去  类似于保护性暂停的guardObject
    //一手交钱 一手交货 没有存储
    public static void test3(){
        SynchronousQueue<Integer> integers = new SynchronousQueue<>();
        new Thread(() -> {
            try {
                log.debug("putting {} ", 1);
                integers.put(1);
                log.debug("{} putted...", 1);

                log.debug("putting...{} ", 2);
                integers.put(2);
                log.debug("{} putted...", 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"t1").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        new Thread(() -> {
            try {
                log.debug("taking {}", 1);
                integers.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"t2").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try {
                log.debug("taking {}", 2);
                integers.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"t3").start();
    }

    //newSingleThreadPool 只有一个核心线程
    //阻塞队列是没有界限的队列
    //用于执行的多个任务是串行的情况  一个 一个执行 没执行的在阻塞队列中等待
    //相比单线程好处是 不会因为其中一个任务出现异常  打断整个程序
    //即使一个线程被打断了结束了  线程池还会创建一个线程
    //使用单线程的线程池  即使一个线程出错了 后面还是会创建新的线程继续执行

    //相比于固定大小线程池规定核心数为1  SingleThreadPool的好处是
    //SingleThreadPool 无法修改参数  而固定大小线程池 直接返回的是ThreadPoolExecutor对象
    //所以强转之后 是可以更改其内部属性的  setCorePoolSize
    //而SingleThreadPoll 创建的时候 外面包了一层FinalizableDelegatedExecutorService
    //这个FinalizableDelegatedExecutorService 是一种装饰器模式
    //修饰对象聚合了一个被修饰对象  然后实现相同接口  对外间接调用被修饰对象  达到对原有方法的修饰
    //对外只暴露线程池结构的基础方法  隐藏了实现类中的具体方法  符合迪米特法则
    public static void test4(){
        ExecutorService service = Executors.newSingleThreadExecutor();

        service.execute(() -> {
            log.debug("1");
            int i = 1 / 0;
        });

        service.execute(() -> {
            log.debug("2");
        });

        service.execute(() -> {
            log.debug("3");
        });
    }

    public static void test5(){
        ExecutorService service = Executors.newFixedThreadPool(2);

        //execute() 传入runnable  submit()  传入callable或runnable 可以有返回结果
        Future<String> future = service.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "ok";
            }
        });
        try {
            //future相当于保护性暂停模式
            System.out.println(future.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //invokeAll  接收任务的集合 返回future的集合
        try {
            List<Future<String>> futures = service.invokeAll(Arrays.asList(
                    () -> {
                        TimeUnit.SECONDS.sleep(1);
                        return "1";
                    },
                    () -> {
                        TimeUnit.SECONDS.sleep(2);
                        return "2";
                    },
                    () -> {
                        TimeUnit.SECONDS.sleep(3);
                        return "3";
                    }
            ));

            futures.forEach(f -> {
                try {
                    System.out.println(f.get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //invokeAny 传入一个任务集合  有一个执行完了 直接返回 其他还未执行完的任务的都不执行了
        //返回最先得到的结果  其他还未执行完的都不执行了
        try {
            String result = service.invokeAny(Arrays.asList(
                    () -> {
                        TimeUnit.SECONDS.sleep(1);
                        log.debug("1");
                        return "1";
                    },
                    () -> {
                        TimeUnit.SECONDS.sleep(2);
                        log.debug("2");
                        return "2";
                    },
                    () -> {
                        TimeUnit.SECONDS.sleep(3);
                        log.debug("3");
                        return "3";
                    }
            ));

            System.out.println(result);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //不接受新任务  但会把正在执行和阻塞队列中的代码执行完  同时此方法不会阻塞调用该方法的线程
        //状态变为SHUTDOWN状态  只打断空闲的线程
        service.shutdown();

        //不接受新任务  正在执行的任务打断执行  返回队列中还未执行的任务
        //状态变为STOP状态  所有的线程都打断
//        List<Runnable> tasks = service.shutdownNow();

        //调用Shutdown 方法后线程不会等待所有任务结束
        //因此如果它想在线程池 TERMINATED 后做些事情，可以利用此方法等待
        try {
            service.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //线程池中执行的任务 如果出现异常  如果不处理  是不会抛出异常的  看不出来出了异常
    //如果需要知道异常  可以在任务中用try  catch进行捕获
    //或者用submit方法 传入callable接口 返回future对象 future的get方法也可以得到异常信息
    public static void test6(){
        ExecutorService service = Executors.newFixedThreadPool(2);
        Future<String> future = service.submit(() -> {
            int i = 1 / 0;
            return "OK";
        });
        try {
            log.debug("{}", future.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
