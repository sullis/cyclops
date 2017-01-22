package com.aol.cyclops2.internal.stream.spliterators.push;

import cyclops.collections.ListX;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by johnmcclean on 12/01/2017.
 */
public class MultiCastOperator<T> extends BaseOperator<T,T> {




    public MultiCastOperator(Operator<T> source){
        super(source);


    }

    ListX<Consumer<? super T>> registeredOnNext;
    ListX<Consumer<? super Throwable>> registeredOnError;
    ListX<Runnable> registeredOnComplete;
    boolean registered = false;


    @Override
    public StreamSubscription subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onComplete) {
        registeredOnNext.add(onNext);
        registeredOnError.add(onError);
        registeredOnComplete.add(onComplete);
        if(!registered) {
            return source.subscribe(e -> {

                        registeredOnNext.forEach(n->n.accept(e));

                    }
                    , e->registeredOnError.forEach(t->t.accept(e)), ()->registeredOnComplete.forEach(n->n.run()));
        }
        return null;
    }

    @Override
    public void subscribeAll(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onCompleteDs) {

        source.subscribeAll(e -> {

                    registeredOnNext.forEach(n->n.accept(e));

                }
                , e->registeredOnError.forEach(t->t.accept(e)), ()->registeredOnComplete.forEach(n->n.run()));
    }
}
