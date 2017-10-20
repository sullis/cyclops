package com.aol.cyclops2.data.collections.extensions.standard;

import java.util.Collection;

import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.data.collections.extensions.IndexedSequenceX;

/**
 * @author johnmcclean
 *
 * @param <T> type of data held in this Collection
 */
public interface MutableSequenceX<T> extends CollectionX<T>, IndexedSequenceX<T> {
    @Override
    public MutableSequenceX<T> plus(T e);

    @Override
    public MutableSequenceX<T> plusAll(Iterable<? extends T> list);

    @Override
    public MutableSequenceX<T> insertAt(int i, T e);


    @Override
    public MutableSequenceX<T> insertAt(int i, Iterable<? extends T> list);

    @Override
    public MutableSequenceX<T> removeValue(T e);


    @Override
    public MutableSequenceX<T> removeAll(Iterable<? extends T> list);

    @Override
    public MutableSequenceX<T> removeAt(int i);


}
