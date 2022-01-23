package CAS;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//乐观锁
//CAS compareAndSet  比较并设置值  底层是CPU指令级别的原子性
//每次要修改之前  先比较之前的值和当前的值是否相同
//相同则说明没有被修改过  可以执行更新操作
//不同则说明  不能执行更新操作

//在单核和多核下都能保证原子性  多核状态下 CPU会让总线锁住
//当这个核把此指令执行完了 再开启总线

//CAS 的核心思想就是不断尝试 一直到成功为止的方式保护共享变量的线程安全
//无锁并发 无阻塞并发
//因为不会发生阻塞 所以效率会增加
//但如果竞争激烈 经常发生失败 一直重试  效率反而会低 因为一直占用CPU

//CAS操作必须搭配volatile关键字使用  需要保证共享变量的可见性
//因为每次CAS比较并交换 在比较的时候 需要获取到共享变量的最新结果进行比较
//将共享变量设置为volatile的 每次修改都让其他线程立即可见
//CAS保证原子性  volatile保证可见性和顺序性

//无锁的情况下 相比synchronized 效率更高
//无锁的情况下 即使尝试失败了 线程始终在告诉运行 没有停歇
//而synchronized如果尝试失败 没有获得锁 会发生上下文切换 进入阻塞
//上下文切换的成本是比较高的 因为上下文切换时 要对信息进行保存和恢复
//比如同步监视器对象的对象头的Mark Word存进monitor 或者存进线程的锁记录对象
//(也是CAS存的 对象头存monitor的地址或者锁记录对象的地址)

//但CAS 无锁情况下 因为保持线程一直运行  所以对CPU的消耗大  会一直占用CPU
//CAS只有在多核情况下效率才高(单核情况下效率不如加锁)
//并且线程数尽量不要超过CPU核心数  否则肯定有线程分不到CPU  还是要阻塞 产生上下文切换


//无锁并发：CAS(保证原子性)加volatile(保证可见性顺序性)
//适用于线程数较少 多核CPU的场景下

//乐观锁：CAS(无锁) 不加锁 不怕其他线程修改共享变量 失败了就重新再尝试
//悲观锁: synchronized/lock 事先加锁 事先认为共享变量肯定有可能被修改 事先加锁

public class TestAccount {
    public static void main(String[] args) {
        Account account = new AccountCas(10000);
        Account.demo(account);
    }
}

class AccountCas implements Account {
    //原子整数
    private AtomicInteger balance;

    public AccountCas(int balance) {
        this.balance = new AtomicInteger(balance);
    }

    @Override
    public Integer getBalance() {
        return balance.get();
    }

    @Override
    public void withdraw(Integer amount) {
        while (true){
            //获取余额最新值
            int prev = balance.get();
            //要修改的余额
            int next = prev - amount;
            //真正修改  CAS
            //compareAndSet  比较并设置值  底层是CPU指令级别的原子性
            if (balance.compareAndSet(prev, next)){
                break;
            }
        }
//        也可以简化为：
//        balance.getAndAdd(-1 * amount);
    }
}

class AccountUnsafe implements Account {

    private Integer balance;

    public AccountUnsafe(Integer balance) {
        this.balance = balance;
    }

    @Override
    public Integer getBalance() {
        synchronized (this) {
            return this.balance;
        }
    }

    @Override
    public void withdraw(Integer amount) {
        synchronized (this) {
            this.balance -= amount;
        }
    }
}

interface Account {
    // 获取余额
    Integer getBalance();

    // 取款
    void withdraw(Integer amount);

    /**
     * 方法内会启动 1000 个线程，每个线程做 -10 元 的操作
     * 如果初始余额为 10000 那么正确的结果应当是 0
     */
    static void demo(Account account) {
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            ts.add(new Thread(() -> {
                account.withdraw(10);
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
