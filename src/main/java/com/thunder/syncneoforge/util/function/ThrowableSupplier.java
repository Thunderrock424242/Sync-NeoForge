package com.thunder.syncneoforge.util.function;

@FunctionalInterface
public interface ThrowableSupplier<T> {
    T get() throws Throwable;
}
