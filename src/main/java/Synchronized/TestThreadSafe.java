package Synchronized;

import java.util.ArrayList;

//为了保证线程安全 尽量多设计私有方法和使用final
//比如String类设计为final  就是为了不让子类重写 没有机会覆盖原本的方法
public class TestThreadSafe {

    static final int THREAD_NUMBER = 2;
    static final int LOOP_NUMBER = 200;
    public static void main(String[] args) {
        ThreadSafeSubClass test = new ThreadSafeSubClass();
        for (int i = 0; i < THREAD_NUMBER; i++) {
            new Thread(() -> {
                test.method1(LOOP_NUMBER);
            }, "Thread" + (i+1)).start();
        }
    }
}

//这种时候之所以有可能报IndexOutOfBoundsException
//是因为有可能在add的时候发生了上下文切换  两个add都加在了index为0的位置
//elementData[size++] = e;  这里发生上下文切换  size 还都是0  写入了
//然后还没有++  切换走了  另一个线程也在0的位置写入了
//然后执行两次删除 第二次就会报错
class ThreadUnsafe {
    ArrayList<String> list = new ArrayList<>();
    public void method1(int loopNumber) {
        for (int i = 0; i < loopNumber; i++) {
            method2();
            method3();
        }
    }

    private void method2() {
        list.add("1");
    }

    private void method3() {
        list.remove(0);
    }
}

abstract class ThreadSafe {
    //加final 防止方法被重写
    public final void method1(int loopNumber) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < loopNumber; i++) {
            method2(list);
            method3(list);
        }
    }

    public void method2(ArrayList<String> list) {
        list.add("1");
    }

//    abstract void method3(ArrayList<String> list);

    public void method3(ArrayList<String> list) {
        System.out.println(1);
        list.remove(0);
    }
}

//此时又线程不安全了  还是多个线程调用了同一个ArrayList
//所以轻易不要暴露接口出去 如果父类中method3是private  就不会被子类重写
class ThreadSafeSubClass extends ThreadSafe{
    @Override
    public void method3(ArrayList<String> list) {
        System.out.println(2);
        new Thread(() -> {
            list.remove(0);
        }).start();
    }
}