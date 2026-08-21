package environment;

public interface Scope<K, V> {
    void declare(K key, V value);
    void assign(K key, V value);
    V get(K key);
    boolean contains(K key);
}
