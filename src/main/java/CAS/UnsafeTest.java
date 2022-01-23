package CAS;

//Unsafe对象不能直接调用  只能用反射调用
//这个类的Unsafe不是线程不安全 而是这个类直接操作底层(内存 线程)  写代码的时候不要调用
//这个Unsafe是单例的  然后外部又不让调用(报错  用类加载器限制  不让系统类加载器加载的代码加载)

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeTest {
    public static void main(String[] args) {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe= (Unsafe) theUnsafe.get(null);
            System.out.println(unsafe);

            Teacher teacher = new Teacher();
            //1.获取域的偏移地址
            long idOffset = unsafe.objectFieldOffset(Teacher.class.getDeclaredField("id"));
            long nameOffset = unsafe.objectFieldOffset(Teacher.class.getDeclaredField("name"));

            //2.执行CAS操作 进行赋值
            unsafe.compareAndSwapInt(teacher, idOffset, 0, 1);
            unsafe.compareAndSwapObject(teacher, nameOffset, null, "Tom");

            System.out.println(teacher);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

class Teacher{
    volatile int id;
    volatile String name;

    @Override
    public String toString() {
        return "Teacher{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
