# Thread-safe Linked List and Hash Map (in Java)

This repo contains implementations of thread-safe linked list, hash map, and their JUnit tests.

Implementations include: generic, Iterable<>, factory pattern with enum, ReentrantReadWriteLock, multi-threaded testing, rules from <i>Effective Java</i>, and more on the way...

## Update

### Version 1.5
Forgot to implement toString() after implementing the iterator. How could this happen!?

### Version 1.4
- The project already added Maven support for future extension.
- Testing shows that the bucket has obvious "primary clustering". The hash code of the key was key.hashcode(). Object like Integer just returns the integer, and this causes clustered block. Now hashing changed to <i>MurmurHash</i>, a performance efficient, non-cryptographic hash function. It avoids clustered block very well. But can the locality of reference really benefit runtime? The Java HashMap also uses object.hashcode() as well. But if I change to open addressing to handle hash collision, then this step is a must.


### Version 1.3

- Implemented iterator for linked list and hash map. Now they <b>both support for-each loop</b> iteration like Java array!
   - Notice that their iterators are not thread-safe, and in general, iterators should not be thread-safe.
   - If the iterator caller does not finish iterating, then the write thread is starvation since iterator holds the read lock.
   - The hash map iterator is to traverse each bucket's linked list one by one and call each one's iterator.
   - The <i>remove()</i> method in Iterable interface is not implemented due to thread-safety concern. Calling it would throw <i>UnsupportedOperationException</i>, and it's final: not allow to override in subclasses.
 
- Iteration Example:
 ```Java
 private MyLinkedList<String> stringList = new MyLinkedListReentrantLockImpl<>();
 
// Input something...

for (String str : stringList) {
    // do something...
}

Iterator<String> listIterator = stringList.iterator();

while (listIterator.hasNext()) {
  String key = listIterator.next();
  // do something...
}

private MyHashMapTesting<String, Integer> hashMap = 
    MyHashMapFactory.newMyHashMapTesting(ThreadSafePolicy.ReadWriteLock);

// Input something...

for (MapPair<String, Integer> mapPair : hashMap) {
   String key = mapPair.key;
   int val = mapPair.getV();
   // do something...
}

Iterator<MapPair> iterator = hashMap.iterator();
MapPair<String, Integer> mapPair;

while (iterator.hasNext()) {
  mapPair = iterator.next();
  String key = mapPair.key;
  int val = mapPair.getV();
  // do something...

}
 ```
 
- Bug fixed on hash map put(k, v) methods.


### Version 1.2
- Improved the multi-threading read method testing to show that on heavy reading situations, the read-write lock does have significantly better performance than synchronized keyword.
  - The old method was to run <i>hashMap.contains(key)</i> a lot of times, and perhaps the bottleneck is the memory R/W speed that slows down the read-write lock hash map, making it has the same runtime as the synchronized keyword hash map.
  - The new heavy read method simply makes the current thread sleep 20 milliseconds, so no more memory R/W speed bottleneck.
  - The multi-threading heavy read test starts 6 threads, and the result is:
    - Basic benchmark (no thread-safety): 30 sec;
    - Synchronized keyword: 3 min;
    - Read-write lock: 30 sec.
  - It proves that the synchronized keyword hash map allows only one reading thread in the critical section at a time, while the read-write lock hash map allows all reading threads to enter so its runtime time is as fast as the benchmark!
  
### Version 1.1 
 - Renamed hash map testing method interface to <b><i>MyHashMapTesting</i></b>, and it extends <b><i>MyHashMap</i></b> interface. Now all hash map implementation classes are implemented <b>MyhashMapTesting</b> only.

 - Created a factory class <i>MyHashMapFactory</i> for hash map object creation, and the factory class accepts the enum <b><i>ThreadSafePolicy</i></b> to decide which types of hash map objects to get:
   - <i>NoSync</i>: return <i>MyLinkedListBasicImpl</i> object.
   - <i>SyncKeyword</i>: return <i>MyHashMapSyncedImpl</i> object.
   - <i>ReadWriteLock</i>: return <i>MyHashMapReentrantImpl</i> object.

 Example of object creation using factory class:
 ```Java
 // Example of creating hash map object using factory class.
 // Hash map for general use (no debugging methods)
 private MyHashMap<ClassA, ClassB> hashMap;
 private int capacity = 666; // Init hash map capacity
 private float loadFactor = 0.4f; // Init hash map load factor for rehashing
 
 // Create a MyHashMap object without thread-safety policy
 hashMap = MyHashMapFactory.getMyHashMap(ThreadSafePolicy.NoSync);
 
 // Create a MyHashMap object without thread-safety policy and given capacity and loadFactor
 hashMap = MyHashMapFactory.getMyHashMap(ThreadSafePolicy.NoSync, capacity, loadFactor);
 
 // Create a MyHashMap object with thread-safety using synchronized keyword
 hashMap = MyHashMapFactory.getMyHashMap(ThreadSafePolicy.SyncKeyword);
 
 // Createa MyHashMap object with thread-safety using ReentrantReadWriteLock
 hashMap = MyHashMapFactory.getMyHashMap(ThreadSafePolicy.ReadWriteLock);
 
 // Hash map for debugging (interface MyHashMapTesting)
 private MyHashMapTesting<ClassA, ClassB> hashMap;
 
 hashMap = MyHashMapFactory.MyHashMapTesting(ThreadSafePolicy.NoSync);
 hashMap = MyHashMapFactory.MyHashMapTesting(ThreadSafePolicy.NoSync, capacity, loadFactor);
 // Same usage as the MyHashMap above, but with different method names.
 ```
 
 - Applied factory method to Junit testing to reduce redundant codes.
 
 <p align="center">
  <img src="/cover%20img/57ecc01c4c5cd160aa630b58238a86b.jpg" style="width:300px;height:400px;"/>
  <br />
  <i>Disclaimer: run multi-threading test on one's own risk ;)</i>
</p>
 
## Linked List

:link:[link](src/main/java/johnston/linkedlist/)

Interface <i>MyLinkedList</i> provides methods:
 - extends Iterable<V>
 - int size();
 - boolean isEmpty();
 - boolean contains(V v);
 - boolean get(int index);
 - V get(V v);
 - List<V> getAll();
 - int getIndex(V v);
 - MyLinkedList addFirst(V v);
 - MyLinkedList addLast(V v);
 - boolean set(V v, int index);
 - boolean remove(V v);
 - MyLinkedList removeAll();

Interface <i>MyLinkedListTesting</i> extends <i>MyLinkedList</i> interface, and it contains testing methods. It's for development use. 

Interface implementations are:
- <i>MyLinkedListBasicImpl</i>: an implementation of basic singly linked list;
- <i>MyLinkedListReentrantLockImpl</i>: an implementation of thread-safe singly linked list based on basic linked list, using read-write lock provided by <i>Reentrantreadwritelock</i>.

 They are all implemented MyLinkedListTesting, which extends MyLinkedList.
  
Testing contains:
- Linked list correctness test cases;
- Linked list multi-threading test cases.
  
## Hash Map

:link:[link](src/main/java/johnston/hashmap/)

Interface <i>MyHashMap</i> provides methods:
- extends Iterable<V>
- int size();
- boolean isEmpty();
- boolean isSameHash(K one, K two);
- V get(K k);
- boolean containsKey(K k);
- void put(K k, V v);
- void removeAll();
- boolean remove(K k);
  
Interface <i>MyHashMapTesting</i> extends <i>MyHashMap</i> interface, and it contains testing methods. It's for development use. 
  
Interface implementations are:
- <i>MyHashMapBasicImpl</i>: basic hash map without thread-safey;
- <i>MyHashMapSyncedImpl</i>: thread-safe hash map based on <i>MyHashMapBasicImpl</i>, using <i>synchronized</i> keyword.
- <i>MyHashMapReentrantImpl</i>: thread-safe hash map based on <i>MyHashMapBasicImpl</i>, using read-write lock provided by <i>Reentrantreadwritelock</i>.

Testing contains:
- Hash map correctness test cases;
- Hash map multi-threading test cases.

For hash collision, these implementations use separate chaining, and the hash map bucket uses MyLinkedList. 

## Multi-threading test cases

The multi-threading test cases contain write, read-write, write-delete tests, and heavy read performance test. The basic implementations can cause data racing and would eventually fail these tests at some point.

## Notes on implementations
- Development should aim for long-term maintenance instead of one-time pass, and should follow SOLID principles.
- Locking critical sections has high performance overhead, so critical sections should be as small as possible. I left all variable declarations outside the lock.
- Try to avoid unnecessary lock. For example, if method <i>int size()</i> is in critical section, and method <i>boolean isEmpty()</i> just return <i>size() == 0</i>, then <i>boolean isEmpty()</i> does not need to lock since all its work is done in the critical sections.
- The <i>MapPair</i> class overrides equals(Object o) method to make sure that equality condition is keyA.equals(keyB). This method is also final: not allow overriding.
- The <i>MapPair</i> class also needs to override hashCode() since the equals() is overridden, in order to maintain consistent hashing equality.
- For thread-safey reason, the remove() methods in their iterators are both not implemented. Calling it would throw <i>UnsupportedOperationException</i>, and they're final: not allow overriding.
   
## Notes on multi-threading

In general, the ReentrantReadWriteLock has better flexibility than synchronized keywords, such as avoiding starvation, supporting priority, and separating read-write operation.

But the performance test on a single machine shows that ReentrantReadWriteLock does not have significant speedup than synchronized keyword. Maybe the runtime overhead is higher than traditional synchronized keyword. I tried minimizing the critical sections for read-write look, but the performance has no significant improvement, and it's easier to cause errors than locking the whole method.

The hash map buckets use basic or thread-safe linked-list can both pass the multi-threading tests. Since all write operations are locked, so the bucket list is guaranteed only one thread inside to write/delte at each time.
