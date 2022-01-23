package threadPool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

//采用拆分的思想 适用于能够进行任务拆分的CPU密集型运算
//支持递归相关操作  也就是相当于在递归的时候
//将拆分出来的子任务  用多线程来完成 提高递归的效率

//Fork/Join默认会创建于CPU核心数大小相同的线程池

@Slf4j(topic = "c.ForkJoin")
public class ForkJoin {
    public static void main(String[] args) {
        ForkJoinPool poll = new ForkJoinPool();
        Integer result = poll.invoke(new MyTask(1, 100));
        System.out.println(result);
    }
}

//从1 到 n求和
class MyTask extends RecursiveTask<Integer> {

    private int begin;
    private int end;

    public MyTask(int begin, int end){
        this.begin = begin;
        this.end = end;
    }

    @Override
    protected Integer compute() {
        //终止条件
        if (end - begin <= 4){
            int sum = 0;
            for (int i = begin; i <= end; i++) {
                sum += i;
            }
            return sum;
        }
        int mid = begin + (end - begin) / 2;
        MyTask t1 = new MyTask(begin, mid);
        MyTask t2 = new MyTask(mid + 1, end);
        ForkJoinTask<Integer> fork1 = t1.fork();  //让一个线程去执行 拆分
        ForkJoinTask<Integer> fork2 = t2.fork();  //让一个线程去执行 拆分
        return fork1.join() + fork2.join();  //join获取任务结果
    }
}
