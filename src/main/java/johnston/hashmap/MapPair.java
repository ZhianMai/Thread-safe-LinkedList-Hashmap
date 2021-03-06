package johnston.hashmap;

public class MapPair<K, V> {
  public final K key;
  private V val;

  public MapPair(K key, V val) {
    this.key = key;
    this.val = val;
  }

  public V getV() {
    return val;
  }

  public void setV(V val) {
    this.val = val;
  }

  /**
   * Guarantee that given a key, the hash map would return the corresponding key-val pair
   * iff two keys are equals.
   *
   * Overriding is not allowed.
   */
  @Override
  public final boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof MapPair)) {
      return false;
    }

    return ((MapPair<K, V>) o).key.equals(this.key);
  }

  /**
   * The equals() method is overridden, so hashCode() needs to override to maintain
   * equality consistence in hashing object of MapPair class.
   */
  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return "MapPair{" +
        "key=" + key +
        ", val=" + val +
        '}';
  }
}
