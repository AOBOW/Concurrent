package GuardedSuspension;

//同步模式之保护性暂停
//FutureTask就属于保护性暂停模式  一个来提供 一个等着获取
//SynchronousQueue 没有大小的阻塞队列  也属于保护性暂停

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

public class GuardedObject {
    //结果
    private Object response;

    //获取结果  timeout  最大等待时间
    public Object get(long timeout){
        synchronized (this){
            long startTime = System.currentTimeMillis();
            long passTime = 0;
            while (response == null && passTime < timeout){
                try {
                    //这里需要用timeout - passTime 防止虚假唤醒的问题
                    //否认可能会让等待时间大于timeout  等待了一半 虚假唤醒 又要等待timeout的时间
                    this.wait(timeout - passTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                passTime = System.currentTimeMillis() - startTime;
            }
        }
        return response;
    }

    //放置结果
    public void put(Object response){
        synchronized (this){
            this.response = response;
            this.notifyAll();
        }
    }
}

@Slf4j(topic = "c.Test")
class test{
    public static void main(String[] args) {
        GuardedObject guardedObject = new GuardedObject();
        //线程1 等待 线程2 的下载结果
        new Thread(() -> {
            log.debug("开始等待");
            Object response = guardedObject.get(3000);
            log.debug("获取结果");
            System.out.println(response);
        }).start();

        new Thread(() -> {
            try {
                guardedObject.put(Downloader.download());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
