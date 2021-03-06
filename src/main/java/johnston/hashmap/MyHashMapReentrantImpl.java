package johnston.hashmap;

import johnston.linkedlist.MyLinkedListBasicImpl;
import johnston.linkedlist.MyLinkedList;
import johnston.linkedlist.MyLinkedListReentrantLockImpl;
import org.apache.commons.codec.digest.MurmurHash3;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is the basic hash map implementation with thread safety.
 * <p>
 * The bucket can use thread-safe singly linked list or basic singly linked list.
 *
 * To improve performance, this class uses read-write lock instead of synchronized keyword.
 * <p>
 * Read-write lock can ensure:
 * -> All read threads do not mutually exclude each other.
 * -> Read threads mutually exclude write threads.
 * -> Write threads mutually exclude each other.
 */
public class MyHashMapReentrantImpl<K, V> implements MyHashMapTesting<K, V> {
  private AtomicInteger size;
  private int capacity;
  private MyLinkedList<MapPair>[] bucketList;
  private final float loadFactor;

  private static final int DEFAULT_CAPACITY = 16;
  private static final int REHASH_FACTOR = 2;
  private static final float DEFAULT_LOAD_FACTOR = 0.5f;
  private static final int THREAD_SLEEP_MILLI_SEC = 20;

  private final ReadWriteLock READ_WRITE_LOCK;
  private final Lock READ_LOCK;
  private final Lock WRITE_LOCK;

  public MyHashMapReentrantImpl(int capacity, float loadFactor) {
    this.capacity = capacity;
    this.size = new AtomicInteger(0);
    this.loadFactor = loadFactor;
    this.bucketList = (MyLinkedList<MapPair>[]) (new MyLinkedList[capacity]);

    // Init read-write lock.
    READ_WRITE_LOCK = new ReentrantReadWriteLock();
    READ_LOCK = READ_WRITE_LOCK.readLock();
    WRITE_LOCK = READ_WRITE_LOCK.writeLock();
  }

  public MyHashMapReentrantImpl() {
    this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Return size of the hash table.
   * <p>
   * Read lock required.
   */
  @Override
  public int size() {
    return this.size.get();
  }

  /**
   * Return if the current hash table is empty.
   * <p>
   * No need to lock because size() has read lock.
   */
  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Return if two given keys have the same hash value. No need to lock.
   */
  @Override
  public boolean isSameHash(K one, K two) {
    return hash(one) == hash(two);
  }

  /**
   * Return the value by given key. If no such key, return null.
   * <p>
   * Read lock required.
   */
  @Override
  public V get(K k) {
    int bucketIdx;
    MapPair<K, V> dummy = new MapPair<>(k, null);
    int pairIdx;
    READ_LOCK.lock();

    try {
      bucketIdx = getIndex(k);
      if (bucketList[bucketIdx] == null) {
        return null;
      }

      pairIdx = bucketList[bucketIdx].getIndex(dummy);

      if (pairIdx == -1) {
        return null;
      }
      return (V) bucketList[bucketIdx].get(pairIdx).getV();

    } finally {
      READ_LOCK.unlock();
    }
  }

  /**
   * Return true if key exists, otherwise false. No need to lock since get() is locked.
   */
  @Override
  public boolean containsKey(K k) {
    return get(k) != null;
  }

  /**
   * If the key exists, update the value, otherwise insert a new pair.
   * <p>
   * Write lock required.
   */
  @Override
  public void put(K k, V v) {
    int bucketIdx;
    MapPair<K, V> newPair = new MapPair<>(k, v);
    WRITE_LOCK.lock();

    try {
      rehash();
      bucketIdx = getIndex(k);

      if (bucketList[bucketIdx] == null) {
        bucketList[bucketIdx] = getNewLinkedList();
        bucketList[bucketIdx].addFirst(newPair);
        size.incrementAndGet();
        return;
      }

      MapPair<K, V> oldPair = bucketList[bucketIdx].get(newPair);

      if (oldPair == null) { // No such pair, add to the bucket at index 0.
        bucketList[bucketIdx].addFirst(newPair);
        size.incrementAndGet();
      } else { // Update old value
        oldPair.setV(v);
      }
    } finally {
      WRITE_LOCK.unlock();
    }
  }

  /**
   * Empty the hash table.
   * <p>
   * Write lock required.
   */
  @Override
  public void removeAll() {
    WRITE_LOCK.lock();
    try {
      size.set(0);
      Arrays.fill(bucketList, null);
    } finally {
      WRITE_LOCK.unlock();
    }
  }

  /**
   * Remove the pair by the given key and return true. If no such keys, return false.
   * <p>
   * Write lock required.
   */
  @Override
  public boolean remove(K k) {
    int bucketIdx;
    MapPair<K, V> dummy = new MapPair<>(k, null);
    WRITE_LOCK.lock();

    try {
      bucketIdx = getIndex(k);

      if (bucketList[bucketIdx] == null) {
        return false;
      }

      if (bucketList[bucketIdx].remove(dummy)) {
        size.decrementAndGet();
        return true;
      } else { // No such value
        return false;
      }
    } finally {
      WRITE_LOCK.unlock();
    }
  }

  /**
   * Return hashcode of the given key. Using MurmurHash function here to avoid
   * primary clustering. MurmurHash is a performance efficient non-cryptographic hash function.
   * No need to lock.
   */
  private int hash(K k) {
    if (k == null) {
      return 0;
    }
    byte[] temp = BigInteger.valueOf(k.hashCode()).toByteArray();
    return MurmurHash3.hash32(temp) & 0x7FFFFFFF; // Ensure > 0
  }

  /**
   * Return bucket index of the given key. No need to lock. The variable bucketList has write lock.
   */
  private int getIndex(K k) {
    int hash = hash(k);
    return hash % bucketList.length;
  }

  /**
   * Double the capacity of the hash table if the load factor is > 0.5. All pairs are guaranteed
   * to be found by given keys after rehashing.
   * <p>
   * No need to lock since all caller functions are locked by write lock.
   */
  private void rehash() {
    if (size() * 1.0f / this.capacity < loadFactor) {
      return;
    }

    capacity *= REHASH_FACTOR;
    MyLinkedList<MapPair>[] oldBucketList = bucketList;
    bucketList = (MyLinkedList<MapPair>[]) (new MyLinkedList[capacity]);

    for (MyLinkedList<MapPair> oldList : oldBucketList) {
      if (oldList == null) {
        continue;
      }

      List<MapPair> pairList = oldList.getAll();
      for (MapPair pair : pairList) {
        int bucketIdx = getIndex((K) pair.key);

        if (bucketList[bucketIdx] == null) {
          bucketList[bucketIdx] = getNewLinkedList();
        }
        bucketList[bucketIdx].addFirst(pair);
      }
    }
  }

  @Override
  public Iterator<MapPair> iterator() {
    return new MyHashMapBasicImpl.MyHashMapIterator<>(this.bucketList);
  }

  /**
   * Iterator class for hash map. It should not guaranteed thread-safety. If the iterator
   * caller does not finish iteration soon, then it will cause write thread starvation.
   */
  static class MyHashMapIterator<MapPair> implements Iterator<MapPair> {
    MyLinkedList<MapPair>[] bucketList;
    int bucketIndex;
    Iterator<MapPair> listIterator;

    public MyHashMapIterator(MyLinkedList<MapPair>[] bucketList) {
      this.bucketList = bucketList;
      if (bucketList == null || bucketList.length == 0) {
        bucketIndex = -1;
      } else {
        bucketIndex = 0;
      }
      listIterator = null;
    }

    @Override
    public boolean hasNext() {
      updateIterator();
      return listIterator == null ? false : listIterator.hasNext();
    }

    @Override
    public MapPair next() {
      updateIterator();
      return listIterator.next();
    }

    private void updateIterator() {
      if (bucketIndex == -1) { // No elements
        listIterator = null;
        return;
      } else if (bucketIndex == bucketList.length) { // The last bucket, no more update.
        return;
      } else if (listIterator != null && listIterator.hasNext()) { // Current iterator not end
        return;
      }
      for (; bucketIndex < bucketList.length; bucketIndex++) { // Get the next bucket iterator
        if (bucketList[bucketIndex] != null) {
          listIterator = bucketList[bucketIndex].iterator();
          if (listIterator.hasNext()) {
            bucketIndex++;
            return;
          }
        }
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("\"MyHashMapReentrantImpl{\"");

    for (MapPair<K, V> pair : this) {
      String temp = "key=" + pair.key + ", val=" + pair.getV() + "; ";
      result.append(temp);
    }

    result.append("}");

    return result.toString();
  }

  /**
   * Place different linked list implementations here
   */
  private MyLinkedList<MapPair> getNewLinkedList() {
    //return new MyLinkedListBasicImpl<>();
    return new MyLinkedListReentrantLockImpl<>();
  }

  /**
   * Method for testing only, won't expose to MyHashMap interface.
   */
  @Override
  public int[] getAllBucketSize() {
    int[] result = new int[bucketList.length];

    for (int i = 0; i < result.length; i++) {
      if (bucketList[i] != null) {
        result[i] = bucketList[i].size();
      }
    }
    return result;
  }

  /**
   * Method for testing only, won't expose to MyHashMap interface.
   */
  @Override
  public int getTotalPairCount() {
    int result = 0;

    for (int i = 0; i < bucketList.length; i++) {
      if (bucketList[i] != null) {
        result += bucketList[i].size();
      }
    }
    return result;
  }

  @Override
  public void addAndDelete(K k, V v) {
    WRITE_LOCK.lock();

    try {
      put(k, v);
      remove(k);
    } finally {
      WRITE_LOCK.unlock();
    }
  }

  /**
   *  Simulates heavy time-consuming read data work.
   */
  @Override
  public void heavyRead() throws InterruptedException {
    READ_LOCK.lock();

    try {
      Thread.sleep(THREAD_SLEEP_MILLI_SEC);
    } finally {
      READ_LOCK.unlock();
    }
  }
}
