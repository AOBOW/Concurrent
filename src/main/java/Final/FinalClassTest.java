package Final;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

//不可变类(没有final的类)  如果不加保护 多线程情况下 会有安全问题
//解决措施 可以加锁 但这样效率比较低  或者使用不可变类的对象

//不可变设计  类设计成final的 不可继承 方法不会被子类覆盖
//属性也尽量设置成final的  只读 不可修改
//比如String就是一个不可变类
//即使是String内部的其他创建新String的方法 也使用的是保护性拷贝
//保护性拷贝  创建副本对象来避免共享的手段
//有修改情况发生时  创建一个新的对象  避免共享

//还有一种无状态的情况也是线程安全的 比如web中的servlet 建议不要设置成员变量

/**
 * final 原理
 * final修饰变量：
 * 会在final对象的写操作之后  加上一个写屏障(保证没有指令能排到写操作之后 赋值操作一定是最后一步)
 * 因为赋值操作  是先取初值(零假空的初始变量 再赋值)  加写屏障保证不会有其他线程在写操作后读到初始值
 *
 * 获取final变量的时候  如果是较小的数值
 * 是从一个工作内存直接复制到需要使用final的工作内存  而不是去主存取 效率更高
 * 如果是较大的数值  就赋值到常量池中
 *
 * 也就是final修饰的基本变量可以完全等价于一个常量  整个JVM生命周期都不会变化
 * 这个值在编译的时候直接写死 可以直接引用
 */
public class FinalClassTest {
    public static void main(String[] args) {
        test1();
    }

    //因为这个是每个线程 对同一个变量做操作 所以很容易发生冲突
    //而不可变类的对象  不能够修改其内部状态(或者是要修改就创建新的)  所以是线程安全的
    private static void test() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                try {
                    Date parse = sdf.parse("1992-11-26");
                    System.out.println(parse.getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static void test1() {
        DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                TemporalAccessor parse = sdf.parse("1992-11-26");
                System.out.println(parse);
            }).start();
        }
    }
}
