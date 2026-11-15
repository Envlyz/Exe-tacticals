package com.exetacticals.client.recoil;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

public final class FixedRingQueue<T> implements Iterable<T> {

    private final int capacity;
    private final Object[] buffer;

    private int head = 0;
    private int tail = 0;
    private int size = 0;

    public FixedRingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }

        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    public void offer(T value) {
        buffer[tail] = value;
        tail++;

        if (tail == capacity) {
            tail = 0;
        }

        if (size < capacity) {
            size++;
        } else {
            head++;

            if (head == capacity) {
                head = 0;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        if (size == 0) {
            throw new IllegalStateException("Queue empty");
        }

        T value = (T) buffer[head];

        head++;
        if (head == capacity) {
            head = 0;
        }

        size--;
        return value;
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (size == 0) {
            throw new IllegalStateException("Queue empty");
        }

        return (T) buffer[head];
    }

    @SuppressWarnings("unchecked")
    public void forEachOrdered(Consumer<? super T> action) {
        int index = head;

        for (int i = 0; i < size; i++) {
            action.accept((T) buffer[index]);

            index++;
            if (index == capacity) {
                index = 0;
            }
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public int capacity() {
        return capacity;
    }

    public void clear() {
        head = 0;
        tail = 0;
        size = 0;
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return new Iterator<>() {
            private int index = head;
            private int count = 0;

            @Override
            public boolean hasNext() {
                return count < size;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T next() {
                T value = (T) buffer[index];

                index++;
                if (index == capacity) {
                    index = 0;
                }

                count++;
                return value;
            }
        };
    }
}
