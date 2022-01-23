package ConcurrentCollection;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//线程安全的集合类:
// 1.遗留的线程安全集合类 HashTable  Vector  内部都是synchronized的
// 2.修饰的线程安全集合Collections工具类中的方法 传入线程不安全的集合
//   返回线程安全的集合  返回的集合 每个方法上都加了一个synchronized  这是装饰器模式的思想
// 3.JUC提供的线程安全的集合类  共有三大类
//   Blocking 阻塞队列 大部分实现基于锁
//   CopyOnWrite 写时复制技术 适用于读多写少  写开销比较大 CopyOnWriteArrayList  CopyOnWriteArraySet(本质上就是CopyOnWriteArrayList)
//   Concurrent  内部采用CAS优化 一般性能更高  ConcurrentHashMap
//   但Concurrent的集合类的缺点是有弱一致性的问题(弱一致性  也不一定是缺点  相当于可重复读)
//   遍历时弱一致性 大小(size)弱一致性  读取弱一致性  这些操作并不是100%准确


//对于非安全的集合 会使用fail-fast机制让遍历立刻失败  抛出ConcurrentModificationException 不再继续遍历
//而线程安全的集合 遍历操作不会失败  这种机制叫做fail-safe

//注意 线程安全的集合  是指每个方法都是线程安全的  是原子的  但如果是几个方法的组合 并不能保证原子性
//get是原子的  put是原子的  但先get再put  则不是原子的了  有可能出现线程安全问题


//HashMap的实现原理：
//先对key求hash值  然后根据Hash值和当前数组的长度
//(默认初始长度是16  1<<4  1左移4位  就算是传入的数值
//也会转换成最近的2的n次方 16  32  64  目的是方便后续的计算)
//计算出桶下标(hash值对数组长度取模)  放入对应下标的位置
//因为数组容量是固定的 所以会有哈希冲突的问题  jdk采用的是拉链法(还有开放地址法)
//用一个链表进行连接  jdk8中  后加入链表的元素 会放在链表的尾部(尾插)
//但jdk7中  后加入链表的元素 会加入链表的头部(头插)  之所以jdk8要改成尾插  是因为jdk7的头插 会有死链的情况

//随着存入的元素原来越多
//当元素数量超过阈值时(负载因子(0.75) * 数组长度 所以一开始阈值为12 第一次放第13个元素时)会进行扩容
//让数组长度乘以2 然后将原有的元素 重新计算桶下标(根据扩容后的数组长度对hash值取模)

//同时如果链表过长(超过阈值8) 查询效率会降低 从log(1) 变成log(n)  hashmap会先尝试进行扩容
//但如果hashmap的容量已经达到64了(扩容两次了) 这时链表还是超过阈值(8)
//会将链表的结构 改成红黑树的结构 加快查询效率(JDK8的优化)  同时红黑树结构还能防止DOS攻击

//这里 jdk7中 多线程情况下扩容 可能会出现并发死链的问题
//因为jdk7中 是头插入的机制  桶下标一样时 后加入的放在链表的头
//当扩容的时候  一个线程加入新元素并正在准备扩容的时候 上下文切换  另一个线程也要加入新元素完成了扩容操作
//此时第一个线程继续执行扩容操作  但此时 链表中节点的指向  已经变化了  然后再次进行重新计算桶下标 并插入的操作
//就可能出现环形链表  形成死链  程序会一直卡死在这里

//所以在多线程环境下使用非线程安全的hashmap会有安全问题  虽然jdk8对扩容进行了优化
//用尾插代替头插 这样扩容后一直是一样的顺序 不会在发生死链 但也还是有扩容丢数据之类的线程安全问题

//之所以JDK7会造成死链  就是因为每次加入新的元素 是头插的  这就导致 顺序会发生变化原来是  1->2
//然后扩容时  1和2还在同一个位置  但是1先处理 2再处理 新的数组中 就变成了 2->1
//如果这时  另一个线程上下文切换回来 继续进行扩容  要对1进行处理  1先断开后面的连接 然后 头插  就变成了  1 -> 2  -> 1  死链了

//但如果JDK8中的尾插  原来是 1-> 2  新数组中  还是 1 -> 2  然后另个一数组  1 断开后面的链接  尾插 变成2 -> 1
//然后2再断开链接  尾插  1  ->  2  就永远不会造成死链的现象了


/**
 * ConcurrentHashMap
 * 内部属性：
 * sizeCtl 用来标记各种状态 默认为0 初始化时为-1  扩容时为-(1 + 扩容线程数)  扩容完成后为下一次需要扩容的阈值
 * 这个sizeCtrl的状态 相当于CAS的锁  比如初始化的时候 就设置为-1  别的需要初始化的发现这个是-1 就yield
 * 相当于AQS的state
 *
 * table  hash表  是一个Node[]
 * nextTable  扩容时的新table
 *
 * ForwardingNode  扩容时用 扩容的时候会将array从后往前将每个index下的元素 搬迁到新的table
 * 这是如果一个index下的节点全部搬迁完了  则加一个forwardingNode
 * 其他线程如果这时候要get这个index下的元素  就明白这个index下的元素已经搬迁完了 会去newTable中找对应值
 *
 * TreeBin 为改成红黑树后的头结点
 * TreeNode为改成红黑树后的数值节点
 *
 * 上面这的Node  都是Node类的子类
 *
 * JDK8 中 基本上都改成了懒加载
 * map 在调用构造器时 只计算table的大小(默认16 自己输入大小则会转成2的n次方)
 * 记在sizeCtrl中  第一次要set的时候  才会创建数组
 * 还有记录size的时候  采用和LongAdder一样的方式 用累加单元累加  其中累加单元数组和累加单元也是懒加载
 *
 * concurrentHashMap的get方法中是不加锁的 效率很高
 * get方法中  首先看底层数组是不是空  有底层数组之后  找到key的hash值对应的index(用数组大小取模)
 * 拿到头结点  头结点不是null  并且hash值一样
 * equals也一样(必须两个都判  防止hash值一样(哈希冲突)的情况)  直接返回头结点的值
 * 如果找到的头结点的hash值为负数  则一定是ForwardingNode或者TreeBin
 * ForwardingNode代表现在正在扩容 并且当前index下链表的所有节点都已经放到新的table中了
 * (扩容时  当一个index下的所有节点都放到新的table后  头结点换成ForwardingNode  其hash值为负数)
 * 此时新的table中寻找  如果是TreeBin  则说明已经转换成红黑树  对红黑树进行寻找
 * 如果不是头结点  头结点也不是负数  则对头结点所连的整个链表进行遍历  有则返回  无则为null
 *
 * put方法中  concurrentHashMap不允许有null的key 和 value 这个和HashMap不同 HashMap是允许的
 * put时 进入一个死循环 一直到将值放进map
 * 首先判断table是否为null  是null则创建(懒加载)  且创建时使用CAS确保原子性 不会重复创建
 * 如果其他线程此时也想执行创建table的操作  则会一直yield(但并不会阻塞  不用切换上下文)
 * 一直到创建成功 乐观锁的机制
 * 其次通过hashcode和数组长度取模  找到对应index的头结点  如果头结点为空则用CAS创建该值为头结点
 * 如果头结点此时的状态为ForwardingNode  证明此时其他线程正在进行扩容操作
 * 则下面要做的是帮忙扩容******
 * 因为扩容的时候 只需要锁住头结点  就锁住了这个index下的这一条链表或者红黑树
 * 可以保证这条链表或红黑树的线程安全  就可以帮助其他正在扩容的线程来转移此index下的节点到new table
 * 如果此时不需要初始化底层数组  不需要扩容  头结点也有值 则说明该put有哈希冲突
 * 这时才synchronized这个头结点  也就是锁住这个index下的链表或红黑树
 * 如果是链表 则挨个查找 有相同的就更改值 没有key就插入到尾部(这是JDK8改的 JDK7是头插  会有死链)
 * 如果是红黑树也是这个类似的情况
 *
 * 这里可以看出concurrentHashMap 只有在哈希冲突的时候才上锁
 * 并且也只锁头结点  而不是整个table  并发效率高
 * 同时如果是链表的情况  还会在遍历过程中  记录下该单条链表的长度
 * 单条链表长度大于阈值(8)  进行扩容 如果长度已经达到64 单条链表长度还大于8 则将链表转换成红黑树
 * 最后如果有新加 还会新增size 同时检查是否扩容  这里size的计数原理 类似于LongAdder
 * 是有一个baseCount(没有竞争时加到baseCount上)
 * 同时高并发情况下会新加累加单元(累加单元数组和累加单元 也是懒加载的)
 * 每个线程累加到一个累加单元上  最后进行汇总
 * 要注意的是concurrentHashMap的size是弱一致性的  高并发情况下可能会有误差
 * 和LongAdder一样  这里的cell 也有@sun.misc.Contended的注解
 * 来确保不会有缓存行伪共享 每个cell前后各加128个空字节 确保独占一个缓存行
 * 修改cell的值时  不会造成其他线程的缓存行失效 提高效率
 *
 * 同时要注意的一点  如果size超过阈值(0.75倍的数组长度)需要扩容  扩容是2倍扩容  然后以链表或红黑树为单位
 * 将原来数组中的各个index下的节点 根据hash和新的数组长度取模
 * 找到 new table的index 放进去(各个链表或红黑树搬迁的步骤 是加synchronized锁的 锁住的是头结点)
 * 同时每结束一个index下所以节点的转换 就将头结点换成ForwardingNode
 * 如果在扩容时 其他线程也需要扩容  则其他线程会帮助扩容
 *
 * 也就是扩容的时候 只要有另一个线程要执行put或者扩容等更改table的操作  都会帮助扩容
 *
 * 同时concurrentHashMap中  显示synchronized锁的地方 有两处
 * 一个是put的时候  如果确定加在这条链表或红黑树上  上锁  进行遍历
 * 一个是扩容的时候  将链表或红黑树上的节点  转移到new table上
 * 一个是树化的时候  将链表转换成红黑树
 * 要注意的是  这三次加锁  锁住的都只是头结点 而不是整个table
 *
 * 其余的创建和修改操作 都是用volatile和CAS完成的
 *
 * JDK7和JDK8相比
 * JDK7是维护了一个segment数组  每个segment继承自ReentrantLock 对应一把锁
 * 多个线程访问不同的segment不冲突  加的锁都不一样  这里和JDK8的思想类似
 * 用多把锁来提高并发度  只是JDK8中 是以每个index为单位 synchronized锁住头结点
 * 但segment默认大小为16 创建后不能更改 且不是懒惰初始化
 * JDK7维护一个segment数组 每个segment对应一个HashEntry的数组
 * 每个HashEntry里面  又是数组加链表的结构   实现分段锁
 * 所有segment对应的HashEntry的数组组合起来  是一个HashMap
 * 相当于是在HashMap的外面 又套了一层segment数组 以此来实现分段锁
 *
 * 且segment的个数一定为2的n次方 每次key需要确定去哪个segment的时候
 * 直接位运算(与操作) 找到低4位(16的时候)的值  就是对应的segment坐标
 *
 * put时 找到对应的segment之后  执行segment的put方法  首先先给这个segment锁住
 * 锁住后  对应的HashEntry数组 就相当于一个小的hashmap
 * 还是通过key的hash值和HashEntry数组的大小取模找到坐标
 * 然后在对应坐标上更新或新增(新增时是头插  多线程情况下 扩容时  可能发生死链)
 * 然后更新size 检查是否需要扩容 并进行rehash 搬迁节点
 * 一部分坐标不变的index直接搬迁  另一部分需要改变的是new出一个新节点进行搬迁
 *
 * 这里的扩容 是以segment为单位的  一个segment内的hashEntry数组  发现需要扩容了 进行扩容
 * 其他Segment不受影响
 *
 * get时 volatile的数组 只能保证数组对象改变时 其他线程及时同步 但内部的元素无法保证
 * 所以需要用UNSAFE.getObjectVolatile来操作数组内元素(JDK8 底层其实也是这个方法)
 * get的时候  如果正在扩容 是去旧数组中取值  这点也和JDK8不一样
 * JDK8的是如果头结点是ForwardingNode  说明这个index下已经扩容rehash完成  会去新数组中找值
 *
 * 计算size的时候  对所有segment内的HashEntry数组  进行遍历
 * 但是会先不加锁  先多次循环计算  只有当两次的计算结果一致 说明期间没有其他线程干扰 计数准确 才返回
 * 用多次检查  来确保线程安全  如果超过3次循环 还没有得到一样的结果 则给所有segment加锁 再计算
 *
 *
 *
 *
 *
 * 总结：
 * Java 8 数组（Node） +（ 链表 Node | 红黑树 TreeNode ） 以下数组简称（table），链表简称（bin）
 * 1.初始化，使用 cas 来保证并发安全，懒惰初始化 table
 * 2.树化，当 table.length < 64 时，先尝试扩容，超过 64 时，并且 bin.length > 8 时，
 * 会将链表树化，树化过程会用 synchronized 锁住链表头
 * 3.put，如果该 bin 尚未创建，只需要使用 cas 创建 bin；如果已经有了，锁住链表头进行后续 put 操作，
 * 元素添加至 bin 的尾部
 * 4.get，无锁操作仅需要保证可见性，如果在扩容过程中 get 操作拿到的是 ForwardingNode
 * 它会让 get 操作在新table 进行搜索
 * 5.扩容，扩容时以 bin 为单位进行，需要对 bin 进行 synchronized，但这时妙的是其它竞争线程也不是无事可做，
 * 它们会帮助把其它 bin 进行扩容，扩容时平均只有 1/6 的节点会把复制到新 table 中
 * 6.size，元素个数保存在 baseCount 中，并发时的个数变动保存在 CounterCell[] 当中。最后统计数量时累加即可
 */

public class ConcurrentHashMapTest {
    public static void main(String[] args) {
        demo(
                // 创建 map 集合
                // 创建 ConcurrentHashMap 对不对？
                () -> new ConcurrentHashMap<String, LongAdder>(8,0.75f,8),

                (map, words) -> {
                    for (String word : words) {
                        // computeIfAbsent是如果没有 执行后面的操作 如果有 返回读取到的值
                        //然后用累加器  执行累加操作

                        // 如果缺少一个 key，则计算生成一个 value , 然后将  key value 放入 map
                        //                  a      0
                        LongAdder value = map.computeIfAbsent(word, (key) -> new LongAdder());
                        // 执行累加
                        value.increment(); // 2

                        /*// 检查 key 有没有
                        Integer counter = map.get(word);
                        int newValue = counter == null ? 1 : counter + 1;
                        // 没有 则 put
                        map.put(word, newValue);*/
                    }
                }
        );
        demo3();
    }


    private static void demo2() {

        Map<String, Integer> collect = IntStream.range(1, 27).parallel()
                .mapToObj(idx -> readFromFile(idx))
                .flatMap(list -> list.stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(w -> 1)));
        System.out.println(collect);
    }

    private static void demo3() {

        demo(
                () -> new ConcurrentHashMap<String, Integer>(),
                (map, words) -> {
                    for (String word : words) {
                        // 函数式编程，无需原子变量
                        map.merge(word, 1, Integer::sum);
                    }
                }
        );
    }

    private static <V> void demo(Supplier<Map<String, V>> supplier, BiConsumer<Map<String, V>, List<String>> consumer) {
        Map<String, V> counterMap = supplier.get();
        // key value
        // a   200
        // b   200
        List<Thread> ts = new ArrayList<>();
        for (int i = 1; i <= 26; i++) {
            int idx = i;
            Thread thread = new Thread(() -> {
                List<String> words = readFromFile(idx);
                consumer.accept(counterMap, words);
            });
            ts.add(thread);
        }

        ts.forEach(t -> t.start());
        ts.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println(counterMap);
    }


    public static List<String> readFromFile(int i) {
        ArrayList<String> words = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("src\\main\\java\\tmp\\" + i + ".txt")))) {
            while (true) {
                String word = in.readLine();
                if (word == null) {
                    break;
                }
                words.add(word);
            }
            return words;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void createFiles() {
        String ALPHA = "abcedfghijklmnopqrstuvwxyz";
        int length = ALPHA.length();
        int count = 200;
        List<String> list = new ArrayList<>(length * count);
        for (int i = 0; i < length; i++) {
            char ch = ALPHA.charAt(i);
            for (int j = 0; j < count; j++) {
                list.add(String.valueOf(ch));
            }
        }
        Collections.shuffle(list);
        for (int i = 0; i < 26; i++) {
            try (PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream("src\\main\\java\\tmp\\" + (i+1) + ".txt")))) {
                String collect = list.subList(i * count, (i + 1) * count).stream()
                        .collect(Collectors.joining("\n"));
                out.print(collect);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
