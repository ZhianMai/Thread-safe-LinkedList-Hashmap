package johnston.hashmap.test;

import johnston.hashmap.MapPair;
import johnston.hashmap.MyHashMapFactory;
import johnston.hashmap.MyHashMapTesting;
import johnston.hashmap.ThreadSafePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MyHashMapImplCorrectnessTest {
  private MyHashMapTesting<String, Integer> hashMap;
  private int globalTestTime;

  @BeforeEach
  public void init() {
    // Use factory to create an object for testing.
    // Select NoSync, SyncKeyword, or ReadWriteLock.
    hashMap = MyHashMapFactory.newMyHashMapTesting(ThreadSafePolicy.ReadWriteLock, 16, 0.5f);
    globalTestTime = 100;
  }

  @Test
  @DisplayName("Test Junit functionality.")
  public void junitSanityCheck() {
    assertTrue(true);
    assertTrue((hashMap != null), "Test object init.");
  }


  @Test
  @DisplayName("Test hash map removeAll().")
  public void testRemoveAll() {
    reset();
    List<String> keys = buildStringInput("Pair ", globalTestTime);
    writeSameValue(keys, 1);

    assertEquals(globalTestTime, hashMap.getTotalPairCount());
    assertEquals(globalTestTime, hashMap.size());
    // printALlBucketSize();
    reset();
    assertEquals(0, hashMap.size());
    assertEquals(0, hashMap.getTotalPairCount());
  }

  @Test
  @DisplayName("Test hash map update existing key.")
  public void testUpdateExistingKey() {
    reset();
    List<String> keys = buildStringInput("Pair ", globalTestTime);
    writeSameValue(keys, 1);
    assertEquals(globalTestTime, hashMap.getTotalPairCount());
    assertEquals(globalTestTime, hashMap.size());

    writeSameValue(keys, 2);
    assertEquals(globalTestTime, hashMap.getTotalPairCount());
    assertEquals(globalTestTime, hashMap.size());
  }

  @Test
  @DisplayName("Test hash map put(), get(), and containsKey().")
  public void testContainsKey() {
    reset();
    List<String> keys = buildStringInput("Pair ", globalTestTime);
    writeSameValue(keys, 1);

    for (String key : keys) {
      assertTrue(hashMap.containsKey(key));
      assertEquals(hashMap.get(key), 1);
    }

    keys = buildStringInput("Bad ", globalTestTime / 2);
    for (String key : keys) {
      assertTrue(!hashMap.containsKey(key));
    }
  }

  @Test
  @DisplayName("Test hash map update value.")
  public void testUpdateValue() {
    reset();
    List<String> keys = buildStringInput("Pair ", globalTestTime);
    writeSameValue(keys, 1);
    writeSameValue(keys, 2);


    for (String key : keys) {
      Integer num = hashMap.get(key);
      assertEquals(num, 2);
    }
  }

  @Test
  @DisplayName("Test hash map get non existing key.")
  public void testNotExistingKey() {
    reset();
    List<String> keys = buildStringInput("Pair ", globalTestTime);
    writeSameValue(keys, 1);
    keys = buildStringInput("Bad ", globalTestTime / 2);

    for (String key : keys) {
      assertEquals(null, hashMap.get(key));
      assertTrue(!hashMap.containsKey(key));
    }
  }

  @Test
  @DisplayName("Test hash map remove value.")
  public void testRemoveValue() {
    reset();
    List<String> keys = buildStringInput("Pair ", globalTestTime);
    writeSameValue(keys, 1);

    for (String key : keys) {
      assertTrue(hashMap.remove(key));
      assertEquals(null, hashMap.get(key));
      assertTrue(!hashMap.containsKey(key));
    }
  }

  @Test
  @DisplayName("Test hash map size.")
  public void testSize() {
    reset();
    List<String> keys = buildStringInput("Pair ", globalTestTime);
    for (int i = 0; i < globalTestTime; i++) {
      assertEquals(i, hashMap.size());
      hashMap.put(keys.get(i), 1);
      assertEquals(i + 1, hashMap.size());
    }
  }

  @Test
  @DisplayName("Test hash map iterator")
  public void testHashMapIterator() {
    reset();
    List<String> keys = buildStringInput("Pair ", 100);
    writeSameValue(keys, 1);
    Set<String> keySet = new HashSet<>(keys);
    int count = 0;
    // printALlBucketSize();

    for (MapPair<String, Integer> mapPair : hashMap) {
      assertTrue(keySet.contains(mapPair.key));
      keySet.remove(mapPair.key);
    }

    Iterator<MapPair> iterator = hashMap.iterator();
    keySet = new HashSet<>(keys);
    MapPair<String, Integer> mapPair;
    while (iterator.hasNext()) {
      mapPair = iterator.next();
      count++;
      assertTrue(keySet.contains(mapPair.key));
      assertEquals(mapPair.getV(), 1);
      keySet.remove(mapPair.key);
    }
    assertEquals(count, 100);
  }

  private void reset() {
    hashMap.removeAll();
  }

  /**
  * This method is to print each bucket size to show if clustered.
  */
  private void printALlBucketSize() {
    int[] allBucketSize = hashMap.getAllBucketSize();
    for (int i = 0; i < allBucketSize.length; i++) {
      System.out.print(allBucketSize[i] + ",");

      if (i != 0 && i % 20 == 0) {
        System.out.println();
      }
    }
    System.out.println();
  }

  private List<String> buildStringInput(String prefix, int count) {
    List<String> result = new ArrayList<>();
    Random rand = new Random();
    int max = 100;

    for (int i = 0; i < count; i++) {
      result.add(prefix + i);
    }
    return result;
  }

  private void writeSameValue(List<String> keys, int val) {
    for (String key : keys) {
      hashMap.put(key, val);
    }
  }
}
