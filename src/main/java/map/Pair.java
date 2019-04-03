package map;

public class Pair<T, Q> {
    private Q value;
    private T key;

    public Pair(T key, Q value) {
        this.value = value;
        this.key = key;
    }

    public Q getValue() {
        return value;
    }

    public T getKey() {
        return key;
    }
}
