package io.roastedroot.go4j;

import java.util.ArrayDeque;
import java.util.Arrays;

public class RefStore {
    // we need to avoid returning 0 to disambiguate with NULL
    private static int OFFSET = 1;
    private static int MIN_CAPACITY = 8;
    private static int count;
    private static ArrayDeque<Integer> emptySlots = new ArrayDeque<>();
    private static Object[] store = new Object[MIN_CAPACITY];

    public RefStore() {}

    private static void increaseCapacity() {
        final int newCapacity = store.length << 1;
        var array = Arrays.copyOf(store, newCapacity);
        store = array;
    }

    public int registerRef(Object obj) {
        int result;
        if (emptySlots.isEmpty()) {
            store[count] = obj;
            count++;

            if (count == store.length) {
                increaseCapacity();
            }
            result = (count - 1);
        } else {
            int emptySlot = emptySlots.pop();
            // just a sanity check
            assert (store[emptySlot] == null);

            store[emptySlot] = obj;
            result = emptySlot;
        }
        return result + OFFSET;
    }

    public void free(int idx) {
        idx = idx - OFFSET;
        store[idx] = null;
        emptySlots.push(idx);
    }

    public Object get(int idx) {
        idx = idx - OFFSET;
        return store[idx];
    }

    public void set(int idx, Object obj) {
        idx = idx - OFFSET;
        store[idx] = obj;
    }
}
