package CAS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

//原子引用类型  AtomicReference AtomicMarkableReference AtomicStampedReference
//就是用原子类包装一个引用数据类型  然后用volatile和CAS  实现线程安全

//AtomicReference  无法解决ABA问题  就是包装了一下引用数据类型 可以执行原子的CAS操作

//AtomicStampedReference  解决了ABA问题  每次操作都修改版本号
//对比的时候 必须值和版本号都相同 才能修改

//AtomicMarkableReference  解决了ABA问题  相当于AtomicStampedReference的简化版
//不需要知道具体改变过几次 只需要知道是否改变过
//(或者是只有两种状态  只需要判断现在处于哪种状态 是否要改成另一种)
//就是将上面int类型的版本号 换成了一个布尔类型
//当期望的布尔类型 和之前的值都一样时候 就能替换成功

public class AtomicReferenceTest {
    public static void main(String[] args) {
        DecimalAccountCAS account = new DecimalAccountCAS(new BigDecimal("10000"));
        DecimalAccount.demo(account);
    }
}

class DecimalAccountCAS implements DecimalAccount{

    AtomicReference<BigDecimal> balance;

    public DecimalAccountCAS(BigDecimal balance){
        this.balance = new AtomicReference<>(balance);
    }

    @Override
    public BigDecimal getBalance() {
        return balance.get();
    }

    @Override
    public void withdraw(BigDecimal amount) {
        while (true){
            BigDecimal prev = balance.get();
            BigDecimal next = prev.subtract(amount);
            if (balance.compareAndSet(prev, next)){
                break;
            }
        }
    }
}

interface DecimalAccount {
    // 获取余额
    BigDecimal getBalance();

    // 取款
    void withdraw(BigDecimal amount);

    /**
     * 方法内会启动 1000 个线程，每个线程做 -10 元 的操作
     * 如果初始余额为 10000 那么正确的结果应当是 0
     */
    static void demo(DecimalAccount account) {
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            ts.add(new Thread(() -> {
                account.withdraw(BigDecimal.TEN);
            }));
        }
        long start = System.nanoTime();
        ts.forEach(Thread::start);
        ts.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long end = System.nanoTime();
        System.out.println(account.getBalance()
                + " cost: " + (end-start)/1000_000 + " ms");
    }
}
