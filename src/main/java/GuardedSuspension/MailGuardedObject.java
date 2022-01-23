package GuardedSuspension;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MailGuardedObject {
    //标识 Guarded Object
    private int id;

    public MailGuardedObject(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

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

//用一个MailBox 解耦结果生产者和结果消费者 中间解耦类 这种方式在RPC框架中经常用到
class MailBoxes{
    private static Map<Integer, MailGuardedObject> map = new ConcurrentHashMap<>();

    private static int id = 1;

    //产生唯一的ID  为了保证线程安全 加synchronized
    private static synchronized int generateId(){
        return id++;
    }

    public static MailGuardedObject createGuardedObject(){
        MailGuardedObject guardedObject = new MailGuardedObject(generateId());
        map.put(guardedObject.getId(), guardedObject);
        return guardedObject;
    }

    public static Set<Integer> getIds(){
        return map.keySet();
    }

    public static MailGuardedObject getMailGuardedObject(int id){
        //返回之后删除
        return map.remove(id);
    }
}

@Slf4j(topic = "c.Person")
class Person{
    public void getMail(){
        MailGuardedObject guardedObject = MailBoxes.createGuardedObject();
        log.debug("收信 id：{}", guardedObject.getId());
        Object mail = guardedObject.get(5000);
        log.debug("收到信 id：{}， 内容为： {}", guardedObject.getId(), mail);
    }
}

@Slf4j(topic = "c.PostMan")
class PostMan{
    private int id;
    private String content;

    public PostMan(int id, String content) {
        this.id = id;
        this.content = content;
    }

    public void postMail(){
        MailGuardedObject mailGuardedObject = MailBoxes.getMailGuardedObject(id);
        mailGuardedObject.put("信息" + id);
    }
}

class TestMail{
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                Person person = new Person();
                person.getMail();
            }).start();
        }

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Integer id : MailBoxes.getIds()) {
            new Thread(() -> {
                PostMan postMan = new PostMan(id, "内容" + id);
                postMan.postMail();
            }).start();
        }
    }
}