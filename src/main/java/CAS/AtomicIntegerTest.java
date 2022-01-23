package CAS;

//原子整数  AtomicBoolean  AtomicInteger AtomicLong
//其中的共享数据(就是被封装的数)  使用volatile修饰的 保证可见性 顺序性
//内部用Unsafe类进行CAS操作 保证原子性

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerTest {
    public static void main(String[] args) {
        AtomicInteger i = new AtomicInteger();

        //自增 并且获取  相当于++i
        System.out.println(i.incrementAndGet());  // 1

        //先获取  再自增 相当于i++
        System.out.println(i.getAndIncrement());  // 1

        System.out.println(i.get());  //2

        //先增加 再获取
        System.out.println(i.addAndGet(2));  //4

        //先获取 再增加
        System.out.println(i.getAndAdd(2));  //4

        System.out.println(i.get());  //6

        //这个传入的函数式接口本质上就是一个值替换另一个值
        //函数式编程  类似策略模式  和comparator的做法差不多  传入比较的方法
        //这里是传入运算的方法  就是将方法抽象成接口
        //参数为读取到的值  结果为要做的运算  先做运算 再获取
        System.out.println(i.updateAndGet(value -> value * 10));  //60

        //参数为读取到的值  结果为要做的运算  先获取 再做运算
        System.out.println(i.getAndUpdate(value -> value / 10));  //60

        System.out.println(i.get());  //6

        //传入一个参数  因为update中 lambda如果要引用外部局部变量 必须是final
        //如果需要传入一个参数 可以用accumulate
        System.out.println(i.accumulateAndGet(10, (p, x) -> p + x)); //16

        System.out.println(i.getAndAccumulate(10, (p, x) -> p + x)); //16

        System.out.println(i.get());  //26

        //自己模仿update操作  while(true)  加CAS  compareAndSet
        while (true){
            int prev = i.get();
            int next = prev * 10;
            if (i.compareAndSet(prev, next)){
                break;
            }
        }
    }
}
