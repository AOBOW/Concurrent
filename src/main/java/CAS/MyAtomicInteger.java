package CAS;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

//自己实现AtomicInteger
public class MyAtomicInteger implements Account{
    private static final Unsafe UNSAFE;

    private volatile int value;

    //偏移量 使用unsafe执行CAS的时候需要用
    private static final long valueOffset;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            valueOffset = UNSAFE.objectFieldOffset(MyAtomicInteger.class.getDeclaredField("value"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //这里需要抛出异常  打断程序 因为UNSAFE是static final的 声明时没赋值
            //必须在静态代码块赋值  所以这里必须附上值 不能有异常还继续执行 】
            //这样可能没赋值  要么成功赋值  要么抛出异常打断运行
            throw new RuntimeException(e);
        }
    }

    public MyAtomicInteger() {
    }

    public MyAtomicInteger(int value) {
        this.value = value;
    }

    public int getValue(){
        return value;
    }

    public void increment(int value){
        while (true){
            int prev = getValue();
            int next = prev + value;
            if (UNSAFE.compareAndSwapInt(this, valueOffset, prev, next)){
                break;
            }
        }
    }

    public void decrement(int value){
        while (true){
            int prev = getValue();
            int next = prev - value;
            if (UNSAFE.compareAndSwapInt(this, valueOffset, prev, next)){
                break;
            }
        }
    }

    @Override
    public Integer getBalance() {
        return getValue();
    }

    @Override
    public void withdraw(Integer amount) {
        decrement(amount);
    }

    public static void main(String[] args) {
        Account account = new AccountCas(10000);
        Account.demo(account);
    }
}
