package CAS;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author AOBO
 * @date 2021-11-24 21:51
 * @description 字段更新器
 */

//AtomicReferenceFieldUpdater
//AtomicIntegerFieldUpdater
//AtomicLongFieldUpdater

//对类的某个字段做原子性的操作
public class AtomicFieldUpdaterTest {

    public static void main(String[] args) {
        Student stu = new Student();

        AtomicReferenceFieldUpdater updater =
                AtomicReferenceFieldUpdater.newUpdater(Student.class, String.class, "name");

        System.out.println(updater.compareAndSet(stu, null, "张三"));
        System.out.println(stu);
    }
}

class Student {
    //利用字段更新器更新  对应的成员变量 必须是volatile的  否则会报错
    volatile String name;

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                '}';
    }
}