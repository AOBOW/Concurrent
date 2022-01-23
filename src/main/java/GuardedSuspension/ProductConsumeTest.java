package GuardedSuspension;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

//生产者 消费者模式
@Slf4j(topic = "c.MessageQueue")
public class ProductConsumeTest {
    public static void main(String[] args) {
        MessageQueue messageQueue = new MessageQueue(2);
        for (int i = 0; i < 3; i++) {
            final int id = i;
            new Thread(() -> {
                messageQueue.put(new Message(id,"消息" + id));
            }, "生产者" + i).start();
        }
        new Thread(() -> {
           while (true){
               //消费者每隔一秒取一次消息
               try {
                   TimeUnit.SECONDS.sleep(1);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
               messageQueue.take();
           }
        }, "消费者").start();
    }
}

@Slf4j(topic = "c.MessageQueue")
//消息队列类  JAVA线程之间通信
class MessageQueue{
    //消息队列集合
    private final Queue<Message> list;
    //队列容量
    private final int capacity;

    public MessageQueue(int capacity) {
        this.capacity = capacity;
        list = new LinkedList<>();  //懒加载
    }

    //获取消息
    public synchronized Message take(){
        synchronized (list){
            while (list.isEmpty()){
                try {
                    log.debug("队列为空， 消费者线程等待");
                    list.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            list.notifyAll();
            log.debug("消费消息, {}", list.peek());
            return list.poll();
        }
    }

    //存入消息
    public void put(Message message){
        synchronized (list){
            while (list.size() >= capacity){
                try {
                    log.debug("队列已满， 生产者线程等待");
                    list.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            list.offer(message);
            log.debug("产生消息, {}", message);
            list.notifyAll();
        }
    }
}

final class Message{
    private int id;
    private Object value;

    public Message(int id, Object value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", value=" + value +
                '}';
    }
}
