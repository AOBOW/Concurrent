package CAS;

/**
 * @author AOBO
 * @date 2021-11-24 23:14
 * @description LongAdder
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Supplier;

//LongAdder 累加器的性能比AtomicInteger累加的性能高很多
//主要的实现方式是在有竞争时 设置多个累加单元 不同的线程 累加到不同的累加单元上
//最后再将结果汇总起来  这样它们在累加的时候操作的不同的Cell变量 因此减少了CAS的重试失败
//从而提高了性能   累加单元cell的个数和CPU的核数有关 随着竞争的变大而增长  但不会超过CPU核数

//LongAdder的源代码中 有几个核心的变量

/**
 * static final int NCPU = Runtime.getRuntime().availableProcessors();
 * CPU上限  这个也是cells的上限  累加单元不能大于CPU的核数  否则一直CAS操作反而效率低(一直空转)
 *
 * transient volatile int cellsBusy;
 * CAS锁  1的时候为加锁  0的时候为解锁 用来在创建cells和给cells扩容的时候加锁
 *
 * transient volatile long base;
 * 基础值  单核的时候使用的基本累加单元  或者没有竞争时 就用这个域累加
 *
 * transient volatile Cell[] cells;
 * 累加单元的数组 累加单元是Cell  存在数组中  数组和cell都是懒加载的 如果没有竞争都是存在base里
 * 开始有竞争了  创建Cell数组 一开始为两个
 * 然后每一个线程分配一个数组里的cell(也是懒加载  发现为空创建)  进行累加
 * 如果有线程没有cell可以分配 对数组扩容(2倍扩容) 最大就扩容到NCPU的个数
 *
 * 最后将base和各个cells里的值  取sum 得到最终值
 *
 * 其中累加单元Cell类有一个注解 是@sun.misc.Contended  这个是 防止缓存行 伪共享  *****
 * 在累加单元的前后加空字节  独占CPU缓存行 保证不同的cell对象存储在不同的CPU缓存行
 * 保证不会因为其中一个缓存行失效 导致其他线程的缓存行也失效 提高效率
 *
 * 因为CPU对缓存的读取效率比内存要高的多 所以在运算时 一般是先将内存的值加载到缓存
 * 缓存是以缓存行为单位的  每一行对应着一块内存  一行是64个字节(8个long)
 * 每一个线程都有自己的缓存  也就是说可能同一份数据 会在几个线程中的缓存多份存储
 * 一个cell类储存了一个int  加上对象头 一共是24个字节 所以就会出现一个缓存行里有两个cell
 * 并且每个线程 各自都有一份缓存 里面的一行都是两个cell
 * CPU要保证自己的数据一致性  如果某个CPU核心改了数据 其他CPU核心对应的整个缓存行必须失效
 * 如果一个线程操作了一个cell  而另一个线程的缓存里也有这个cell  也就需要向另个线程同步
 * 让另一个线程的同一行缓存 全部失效
 * 因为另一个线程所操作的cell可能和失效的cell在同一行  到时其使用的cell也被清楚 需要重新去主存中取
 * 所以每个线程 操作一个cell 但这些cell又写在了个缓存行中
 * 就会频繁的让另一个线程失效  不断去内存取最新的值 影响效率
 *
 * 所以加了@sun.misc.Contended的注解  会在对象前后各加128个空白的字节  就能独占一个缓存行
 * 这样一个线程更改了cell  同一个缓存行里没有其他的cell 这个cell又只有本线程在使用
 * 就不会造成另一个线程的缓存行失效  以此提高效率
 *
 *这里LongAdder用的就是拆分 再合并的思想
 */
public class LongAdderTest {
    private static <T> void demo(Supplier<T> adderSupplier, Consumer<T> action) {
        T adder = adderSupplier.get();
        long start = System.nanoTime();
        List<Thread> ts = new ArrayList<>();
        // 4 个线程，每人累加 50 万
        for (int i = 0; i < 40; i++) {
            ts.add(new Thread(() -> {
                for (int j = 0; j < 500000; j++) {
                    action.accept(adder);
                }
            }));
        }
        ts.forEach(t -> t.start());
        ts.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long end = System.nanoTime();
        System.out.println(adder + " cost:" + (end - start) / 1000_000);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            demo(() -> new LongAdder(), adder -> adder.increment());
        }
        for (int i = 0; i < 5; i++) {
            demo(() -> new AtomicInteger(), adder -> adder.incrementAndGet());
        }
    }
}