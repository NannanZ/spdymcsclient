/**
 * 
 */
package net.haibo.spdy.client;

import java.util.ArrayList;


/**
 * Define a counted flyweight alike container.
 * <br>It will returns max number of the created instance by iterate.
 */
public class CountedFlyweight<T> {
    
    public interface Creatable<T> {
        T create();
    }

    private final int sharedCount;
    private Creatable<T> generator;
    public CountedFlyweight(int count, Creatable<T> creater) {
        if (count < 1) throw new RuntimeException("Shared count should be larger than 0");
        this.sharedCount = count;
        this.generator = creater;
    }

    private ArrayList<T> flies = new ArrayList<T>();
    private int iterator = 0;
    public synchronized T next() {
        iterator = (iterator) % (this.sharedCount);
        if (iterator > flies.size()-1) {
            flies.add(this.generator.create());
        }
        return flies.get(iterator++);
    }

    public int current() {
        return iterator;
    }
}