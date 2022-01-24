package ConcurrentCollection;

//CopyOnWriteArraySet 其实内部也是CopyOnWriteArrayList  类似于HashSet内部其实是HashMap

//CopyOnWriteArrayList 采用写时复制技术
//就是增删改这种写入操作时 会加锁 然后底层的array数组会拷贝一份
//这些增删改的操作 都是在拷贝出来的数组上进行的
//不会影响其他线程的读操作 读的时候不加锁 同时读的都是旧数组 并发读  读写分离
//也就是读读并发 读写并发  只有写写互斥  比读写锁更进一步 读写锁只是读读并发 读写还是互斥的(上了读锁不能上写锁)

//CopyOnWriteArrayList比较适合读多写少的场景  因为写的时候要复制 要加锁 消耗大 用空间换时间

//同时在get的时候 有弱一致性  因为读取的是旧数组
//中间可能发生增删改操作产生的新数组对旧数组的覆盖 但get的引用还指向旧数组
//同时迭代器  迭代查询  也会有这个问题 迭代的过程中有增删改  但迭代器遍历的还是旧数组

//注意  新数组替换旧数组 只是变量的指向被替换了 旧数组还没有被销毁
//仍然可能有其他线程的get或遍历操作 指向旧数组的内存地址

//弱一致性其实就相当于数据库中的可重复读 MVCC(多版本并发控制)
//让一个事物中两次读取读到的都是一样的 这就需要弱一致性  不要马上就改变
//高并发和一致性是矛盾的 需要权衡

//HashSet的底层是HashMap  存入的值是key  所有的value都是一个值new object();
//之所以不用null  用一个object 是因为remove的时候  成功返回value 失败返回null
//如果都用null  无法判断是否删除成功
public class CopyOnWriteArrayListTest {
}
