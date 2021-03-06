package johnston.hashmap.test;

import johnston.hashmap.MyHashMapFactory;
import johnston.hashmap.MyHashMapReentrantImpl;
import johnston.hashmap.MyHashMapTesting;
import johnston.hashmap.ThreadSafePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MyHashMapImplConcurrencyTest {
  private MyHashMapTesting<String, Integer> hashMap;
  private int globalTestTime;
  private Random random;
  private int testFactor;

  @BeforeEach
  public void init() {
    // Use factory to create an object for testing.
    // Select NoSync, SyncKeyword, or ReadWriteLock.
    // The NoSync type would definitely fail all test methods at some point.
    hashMap = MyHashMapFactory.newMyHashMapTesting(ThreadSafePolicy.ReadWriteLock);
    globalTestTime = 1000;
    testFactor = 100;
    random = new Random();
  }

  @Test
  @DisplayName("Test Junit functionality.")
  public void junitSanityCheck() {
    assertTrue(true);
    assertTrue((hashMap != null), "Test object init.");
  }

  @Test
  @DisplayName("Test write data racing.")
  public void writeDataRace() {
    reset();
    int threadCount = 10;
    int testTime = globalTestTime * testFactor;

    // Let multiple threads write data at the same time.
    class ReadWriteThread extends Thread {
      public void run() {
        for (int i = 0; i < testTime; i++) {
          String key = String.valueOf(random.nextDouble()); // Generates unique key.
          hashMap.put(key, i);
          assertTrue(hashMap.containsKey(key));
        }
        System.out.println("Write thread (id: " + this.getId() + ") finished.");
      }
    }

    // Set up threads
    Thread[] threadPool = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threadPool[i] = new ReadWriteThread();
    }

    // Run threads
    for (Thread thread : threadPool) {
      thread.start();
    }

    // Let the main thread waits until all working threads finished.
    try {
      for (Thread thread : threadPool) {
        thread.join();
      }
    } catch (InterruptedException e) {
    }

    // Basic hash map class would cause NullPtrException.
    assertEquals(hashMap.getTotalPairCount(), hashMap.size());
    assertEquals(hashMap.size(), testTime * threadCount);
  }

  @Test
  @DisplayName("Test delete data racing.")
  public void deleteDataRace() {
    reset();
    int threadCount = 10;
    int testTime = globalTestTime * testFactor;

    // Let multiple threads delete data at the same time.
    class DeletionThread extends Thread {
      public void run() {
        List<String> keys = buildStringInput(String.valueOf(random.nextInt()), testTime);
        for (int i = 0; i < testTime; i++) {
          String key = keys.get(i);
          hashMap.put(key, 1);
          assertTrue(hashMap.containsKey(key));
          hashMap.remove(key);
          assertTrue(!hashMap.containsKey(key));
        }
        System.out.println("Deletion thread (id: " + this.getId() + ") finished.");
      }
    }

    // Set up threads
    Thread[] threadPool = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threadPool[i] = new DeletionThread();
    }

    // Run threads
    for (int i = 0; i < threadCount; i++) {
      threadPool[i].start();
    }

    // Let the main thread waits until all working threads finished.
    try {
      for (Thread thread : threadPool) {
        thread.join();
      }
    } catch (InterruptedException e) {
    }

    // Basic linked-list would have size inconsistency.
    assertEquals(hashMap.size(), 0);
    assertEquals(hashMap.getTotalPairCount(), 0);
  }

  private void reset() {
    hashMap.removeAll();
  }

  int finishedThread = 0;
  boolean diff = false;

  @Test
  @DisplayName("Test read-write data racing.")
  public void testReadWriteDataRace() {
    reset();
    int testTime = globalTestTime * testFactor;
    int threadCount = 10;

    class WriteDeleteThread extends Thread {
      public void run() {
        for (int i = 0; i < testTime; i++) {
          hashMap.addAndDelete(String.valueOf(random.nextDouble()), 1);
        }
        System.out.println("WriteDeleteThread thread (id: " + this.getId() + ") finished.");
        finishedThread++;
      }
    }

    Thread[] threadPool = new Thread[threadCount];
    for (int i = 0; i < threadPool.length; i++) {
      threadPool[i] = new WriteDeleteThread();
    }

    Thread readThread = new Thread() {
      public void run() {
        while (finishedThread != threadCount) {
          int size = hashMap.size();
          if (size < 0 || size > 1) {
            diff = true;
            System.out.println(size + " !!!!!!!!!!!!!!!!");
            break;
          }
        }
      }
    };

    for (Thread thread : threadPool) {
      thread.start();
    }
    readThread.start();

    // Let the main thread waits until all working threads finished.
    try {
      for (Thread thread : threadPool) {
        thread.join();
      }
      readThread.join();
    } catch (InterruptedException e) {
    }
    assertTrue(!diff);
  }

  @Test
  @DisplayName("Test heavy read data performance")
  public void testReadPerformance() {
    reset();
    int testTime = globalTestTime;
    int threadCount = 6;
    List<String> keyList = buildStringInput("Test ", testTime);
    writeSameValue(keyList, 1);
    String key = "Test 0";

    class ReadThread extends Thread {
      public void run() {
        for (int i = 0; i < testTime; i++) {
          try {
            hashMap.heavyRead();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        System.out.println("Read thread (id: " + this.getId() + ") finished.");
      }
    }

    Thread[] threadPool = new Thread[threadCount];
    for (int i = 0; i < threadPool.length; i++) {
      threadPool[i] = new ReadThread();
    }

    for (Thread thread : threadPool) {
      thread.start();
    }

    // Let the main thread waits until all working threads finished.
    try {
      for (Thread thread : threadPool) {
        thread.join();
      }
    } catch (InterruptedException e) {
    }
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
  }

  private List<String> buildStringInput(String prefix, int count) {
    List<String> result = new ArrayList<>();
    int max = 100;

    for (int i = 0; i < count; i++) {
      result.add(prefix + i + " " + random.nextInt(max));
    }
    return result;
  }

  private void writeSameValue(List<String> keys, int val) {
    for (String key : keys) {
      hashMap.put(key, val);
    }
  }
}
