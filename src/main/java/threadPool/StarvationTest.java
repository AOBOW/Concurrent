package threadPool;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//线程池属于享元模式 让有限的线程  轮流异步处理无限多的任务

//线程池也有可能发生死锁的线程 这种死锁不是锁住了 是因为线程池没有资源了 导致互相等待
//这种情况叫做饥饿
//解决办法是使用不同的线程池

//创建线程池时  太小 不能充分利用资源 容易导致饥饿  太大 会导致频繁的上下文切换 占用更多内存

//线程池创建的时候 核心线程数量的选择：

//1.CPU密集型运算  CPU数量  +  1  因为CPU密集型主要是在计算  所以把核数占满 效率最高
//加一是为了防止操作系统的页缺失故障

//2.I/O密集型运算   线程数 = 核数 * 期望 CPU 利用率 * 总时间(CPU计算时间+等待时间) / CPU 计算时间
@Slf4j(topic = "c.StarvationTest")
public class StarvationTest {

    static final List<String> MENU = Arrays.asList("地三鲜", "宫保鸡丁", "辣子鸡丁", "烤鸡翅");
    static Random RANDOM = new Random();
    static String cooking() {
        return MENU.get(RANDOM.nextInt(MENU.size()));
    }
    public static void main(String[] args) {

    }

    public static void test1(){
        ExecutorService pool = Executors.newFixedThreadPool(2);

        pool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = pool.submit(() -> {
                log.debug("做菜");
                return cooking();
            });
            try {
                log.debug("上菜: {}", f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        pool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = pool.submit(() -> {
                log.debug("做菜");
                return cooking();
            });
            try {
                log.debug("上菜: {}", f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    public static void test2(){
        ExecutorService waiterPool = Executors.newFixedThreadPool(1);
        ExecutorService cookPool = Executors.newFixedThreadPool(1);

        waiterPool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = cookPool.submit(() -> {
                log.debug("做菜");
                return cooking();
            });
            try {
                log.debug("上菜: {}", f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        waiterPool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = cookPool.submit(() -> {
                log.debug("做菜");
                return cooking();
            });
            try {
                log.debug("上菜: {}", f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }
}
