package johnston.hashmap;

/**
 * This class provides methods for hash map testing support
 */
public interface MyHashMapTesting<K, V> extends MyHashMap<K, V> {
  public int[] getAllBucketSize();
  public int getTotalPairCount();
  public void addAndDelete(K k, V v);
  public void heavyRead() throws InterruptedException;
}
