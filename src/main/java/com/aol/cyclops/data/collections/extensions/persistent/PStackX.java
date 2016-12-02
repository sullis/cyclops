package com.aol.cyclops.data.collections.extensions.persistent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;
import org.pcollections.ConsPStack;
import org.pcollections.PStack;
import org.reactivestreams.Publisher;

import com.aol.cyclops.Monoid;
import com.aol.cyclops.Reducer;
import com.aol.cyclops.Reducers;
import com.aol.cyclops.control.Matchable.CheckValue1;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.Trampoline;
import com.aol.cyclops.data.collections.extensions.FluentSequenceX;
import com.aol.cyclops.data.collections.extensions.standard.ListX;
import com.aol.cyclops.types.Combiner;
import com.aol.cyclops.types.OnEmptySwitch;
import com.aol.cyclops.types.To;
import com.aol.cyclops.types.Value;

public interface PStackX<T> extends To<PStackX<T>>,PStack<T>, PersistentCollectionX<T>, FluentSequenceX<T>, OnEmptySwitch<T, PStack<T>> {

    /**
     * Narrow a covariant PStackX
     * 
     * <pre>
     * {@code 
     *  PStackX<? extends Fruit> set = PStackX.of(apple,bannana);
     *  PStackX<Fruit> fruitSet = PStackX.narrow(set);
     * }
     * </pre>
     * 
     * @param stackX to narrow generic type
     * @return POrderedSetX with narrowed type
     */
    public static <T> PStackX<T> narrow(final PStackX<? extends T> stackX) {
        return (PStackX<T>) stackX;
    }
    
    /**
     * Create a PStackX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range PStackX
     */
    public static PStackX<Integer> range(final int start, final int end) {
        return ReactiveSeq.range(start, end)
                          .toPStackX();
    }

    /**
     * Create a PStackX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range PStackX
     */
    public static PStackX<Long> rangeLong(final long start, final long end) {
        return ReactiveSeq.rangeLong(start, end)
                          .toPStackX();
    }

    /**
     * Unfold a function into a PStackX
     * 
     * <pre>
     * {@code 
     *  PStackX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</code>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return PStackX generated by unfolder function
     */
    static <U, T> PStackX<T> unfold(final U seed, final Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return ReactiveSeq.unfold(seed, unfolder)
                          .toPStackX();
    }

    /**
     * Generate a PStackX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate PStackX elements
     * @return PStackX generated from the provided Supplier
     */
    public static <T> PStackX<T> generate(final long limit, final Supplier<T> s) {

        return ReactiveSeq.generate(s)
                          .limit(limit)
                          .toPStackX();
    }

    /**
     * Create a PStackX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return PStackX generated by iterative application
     */
    public static <T> PStackX<T> iterate(final long limit, final T seed, final UnaryOperator<T> f) {
        return ReactiveSeq.iterate(seed, f)
                          .limit(limit)
                          .toPStackX();

    }

    /**
     * Construct a PStack from the provided values 
     * 
     * <pre>
     * {@code 
     *  List<String> list = PStacks.of("a","b","c");
     *  
     *  // or
     *  
     *  PStack<String> list = PStacks.of("a","b","c");
     *  
     *  
     * }
     * </pre>
     * 
     * 
     * @param values To add to PStack
     * @return new PStack
     */
    public static <T> PStackX<T> of(final T... values) {
        return new PStackXImpl<>(
                                 ConsPStack.from(Arrays.asList(values)), true);
    }

    /**
     * Construct a PStackX from an Publisher
     * 
     * @param publisher
     *            to construct PStackX from
     * @return PStackX
     */
    public static <T> PStackX<T> fromPublisher(final Publisher<? extends T> publisher) {
        return ReactiveSeq.fromPublisher((Publisher<T>) publisher)
                          .toPStackX();
    }

    public static <T> PStackX<T> fromIterable(final Iterable<T> iterable) {
        if (iterable instanceof PStackX)
            return (PStackX) iterable;
        if (iterable instanceof PStack)
            return new PStackXImpl<>(
                                     (PStack) iterable, true);
        PStack<T> res = ConsPStack.<T> empty();
        final Iterator<T> it = iterable.iterator();
        while (it.hasNext())
            res = res.plus(it.next());

        return new PStackXImpl<>(
                                 res, true);
    }

    /**
     * <pre>
     * {@code 
     *  List<String> list = PStacks.of(Arrays.asList("a","b","c"));
     *  
     *  // or
     *  
     *  PStack<String> list = PStacks.of(Arrays.asList("a","b","c"));
     *  
     *  
     * }
     * 
     * @param values To add to PStack
     * @return
     */
    public static <T> PStackX<T> fromCollection(final Collection<T> values) {
        if (values instanceof PStackX)
            return (PStackX) values;
        if (values instanceof PStack)
            return new PStackXImpl<>(
                                     (PStack) values, true);
        return new PStackXImpl<>(
                                 ConsPStack.from(values), true);
    }

    /**
     * <pre>
     * {@code 
     *     List<String> empty = PStack.empty();
     *    //or
     *    
     *     PStack<String> empty = PStack.empty();
     * }
     * </pre>
     * @return an empty PStack
     */
    public static <T> PStackX<T> empty() {
        return new PStackXImpl<>(
                                 ConsPStack.empty(), true);
    }

    /**
     * Construct a PStack containing a single value
     * </pre>
     * {@code 
     *    List<String> single = PStacks.singleton("1");
     *    
     *    //or
     *    
     *    PStack<String> single = PStacks.singleton("1");
     * 
     * }
     * </pre>
     * 
     * @param value Single value for PVector
     * @return PVector with a single value
     */
    public static <T> PStackX<T> singleton(final T value) {
        return new PStackXImpl<>(
                                 ConsPStack.singleton(value), true);
    }

    /**
     * Reduce (immutable Collection) a Stream to a PStack, note for efficiency reasons,
     * the produced PStack is reversed.
     * 
     * 
     * <pre>
     * {@code 
     *    PStack<Integer> list = PStacks.fromStream(Stream.of(1,2,3));
     * 
     *  //list = [3,2,1]
     * }</pre>
     * 
     * 
     * @param stream to convert to a PVector
     * @return
     */
    public static <T> PStackX<T> fromStream(final Stream<T> stream) {
        return Reducers.<T> toPStackX()
                       .mapReduce(stream)
                       .efficientOpsOff();
    }

    @Override
    default PStackX<T> toPStackX() {
        return this;
    }
    /**
     * coflatMap pattern, can be used to perform lazy reductions / collections / folds and other terminal operations
     * 
     * <pre>
     * {@code 
     *   
     *     PStackX.of(1,2,3)
     *          .map(i->i*2)
     *          .coflatMap(s -> s.reduce(0,(a,b)->a+b))
     *      
     *     //PStackX[12]
     * }
     * </pre>
     * 
     * 
     * @param fn mapping function
     * @return Transformed PStackX
     */
    default <R> PStackX<R> coflatMap(Function<? super PStackX<T>, ? extends R> fn){
       return fn.andThen(r ->  this.<R>unit(r))
                .apply(this);
    }
    /**
    * Combine two adjacent elements in a PStackX using the supplied BinaryOperator
    * This is a stateful grouping & reduction operation. The output of a combination may in turn be combined
    * with it's neighbor
    * <pre>
    * {@code 
    *  PStackX.of(1,1,2,3)
                 .combine((a, b)->a.equals(b),Semigroups.intSum)
                 .toListX()
                 
    *  //ListX(3,4) 
    * }</pre>
    * 
    * @param predicate Test to see if two neighbors should be joined
    * @param op Reducer to combine neighbors
    * @return Combined / Partially Reduced PStackX
    */
    @Override
    default PStackX<T> combine(final BiPredicate<? super T, ? super T> predicate, final BinaryOperator<T> op) {
        return (PStackX<T>) PersistentCollectionX.super.combine(predicate, op);
    }
    


    @Override
    default <R> PStackX<R> unit(final Collection<R> col) {
        if (isEfficientOps())
            return fromCollection(col);
        return fromCollection(col).efficientOpsOff();
    }

    @Override
    default <R> PStackX<R> unit(final R value) {
        return singleton(value);
    }

    @Override
    default <R> PStackX<R> unitIterator(final Iterator<R> it) {
        return fromIterable(() -> it);
    }

    @Override
    default <R> PStackX<R> emptyUnit() {
        if (isEfficientOps())
            return empty();
        return PStackX.<R> empty()
                      .efficientOpsOff();
    }

    default PStack<T> toPStack() {
        return this;
    }

    @Override
    default PStackX<T> plusInOrder(final T e) {
        if (isEfficientOps())
            return plus(e);
        return plus(size(), e);
    }

    @Override
    default ReactiveSeq<T> stream() {

        return ReactiveSeq.fromIterable(this);
    }

    @Override
    default <X> PStackX<X> from(final Collection<X> col) {
        if (isEfficientOps())
            return fromCollection(col);
        return fromCollection(col).efficientOpsOff();
    }

    @Override
    default <T> Reducer<PStack<T>> monoid() {
        if (isEfficientOps())
            return Reducers.toPStackReversed();
        return Reducers.toPStack();

    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#reverse()
     */
    @Override
    default PStackX<T> reverse() {
        PStack<T> reversed = ConsPStack.empty();
        final Iterator<T> it = iterator();
        while (it.hasNext())
            reversed = reversed.plus(0, it.next());
        return fromCollection(reversed);
    }

    PStackX<T> efficientOpsOn();

    PStackX<T> efficientOpsOff();

    boolean isEfficientOps();

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#filter(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> filter(final Predicate<? super T> pred) {
        return (PStackX<T>) PersistentCollectionX.super.filter(pred);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#map(java.util.function.Function)
     */
    @Override
    default <R> PStackX<R> map(final Function<? super T, ? extends R> mapper) {
        return (PStackX<R>) PersistentCollectionX.super.map(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#flatMap(java.util.function.Function)
     */
    @Override
    default <R> PStackX<R> flatMap(final Function<? super T, ? extends Iterable<? extends R>> mapper) {

        return (PStackX) PersistentCollectionX.super.flatMap(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#limit(long)
     */
    @Override
    default PStackX<T> limit(final long num) {

        return (PStackX) PersistentCollectionX.super.limit(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#skip(long)
     */
    @Override
    default PStackX<T> skip(final long num) {

        return (PStackX) PersistentCollectionX.super.skip(num);
    }

    @Override
    default PStackX<T> takeRight(final int num) {
        return (PStackX<T>) PersistentCollectionX.super.takeRight(num);
    }

    @Override
    default PStackX<T> dropRight(final int num) {
        return (PStackX<T>) PersistentCollectionX.super.dropRight(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#takeWhile(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> takeWhile(final Predicate<? super T> p) {

        return (PStackX) PersistentCollectionX.super.takeWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#dropWhile(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> dropWhile(final Predicate<? super T> p) {

        return (PStackX) PersistentCollectionX.super.dropWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#takeUntil(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> takeUntil(final Predicate<? super T> p) {

        return (PStackX) PersistentCollectionX.super.takeUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#dropUntil(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> dropUntil(final Predicate<? super T> p) {
        return (PStackX) PersistentCollectionX.super.dropUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#trampoline(java.util.function.Function)
     */
    @Override
    default <R> PStackX<R> trampoline(final Function<? super T, ? extends Trampoline<? extends R>> mapper) {

        return (PStackX) PersistentCollectionX.super.trampoline(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#slice(long, long)
     */
    @Override
    default PStackX<T> slice(final long from, final long to) {
        return (PStackX) PersistentCollectionX.super.slice(from, to);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#sorted(java.util.function.Function)
     */
    @Override
    default <U extends Comparable<? super U>> PStackX<T> sorted(final Function<? super T, ? extends U> function) {
        return (PStackX) PersistentCollectionX.super.sorted(function);
    }

    @Override
    public PStackX<T> minusAll(Collection<?> list);

    @Override
    public PStackX<T> minus(Object remove);

    /**
     * @param i
     * @param e
     * @return
     * @see org.pcollections.PStack#with(int, java.lang.Object)
     */
    @Override
    public PStackX<T> with(int i, T e);

    /**
     * @param i
     * @param e
     * @return
     * @see org.pcollections.PStack#plus(int, java.lang.Object)
     */
    @Override
    public PStackX<T> plus(int i, T e);

    @Override
    public PStackX<T> plus(T e);

    @Override
    public PStackX<T> plusAll(Collection<? extends T> list);

    /**
     * @param i
     * @param list
     * @return
     * @see org.pcollections.PStack#plusAll(int, java.util.Collection)
     */
    @Override
    public PStackX<T> plusAll(int i, Collection<? extends T> list);

    /**
     * @param i
     * @return
     * @see org.pcollections.PStack#minus(int)
     */
    @Override
    public PStackX<T> minus(int i);

    @Override
    public PStackX<T> subList(int start, int end);

    @Override
    default PStackX<ListX<T>> grouped(final int groupSize) {
        return (PStackX<ListX<T>>) PersistentCollectionX.super.grouped(groupSize);
    }

    @Override
    default <K, A, D> PStackX<Tuple2<K, D>> grouped(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream) {
        return (PStackX) PersistentCollectionX.super.grouped(classifier, downstream);
    }

    @Override
    default <K> PStackX<Tuple2<K, Seq<T>>> grouped(final Function<? super T, ? extends K> classifier) {
        return (PStackX) PersistentCollectionX.super.grouped(classifier);
    }

    @Override
    default <U> PStackX<Tuple2<T, U>> zip(final Iterable<? extends U> other) {
        return (PStackX) PersistentCollectionX.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#zip(java.lang.Iterable, java.util.function.BiFunction)
     */
    @Override
    default <U, R> PStackX<R> zip(final Iterable<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (PStackX<R>) PersistentCollectionX.super.zip(other, zipper);
    }

    @Override
    default <U, R> PStackX<R> zip(final Seq<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (PStackX<R>) PersistentCollectionX.super.zip(other, zipper);
    }

    @Override
    default <U, R> PStackX<R> zip(final Stream<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (PStackX<R>) PersistentCollectionX.super.zip(other, zipper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#permutations()
     */
    @Override
    default PStackX<ReactiveSeq<T>> permutations() {

        return (PStackX<ReactiveSeq<T>>) PersistentCollectionX.super.permutations();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#combinations(int)
     */
    @Override
    default PStackX<ReactiveSeq<T>> combinations(final int size) {

        return (PStackX<ReactiveSeq<T>>) PersistentCollectionX.super.combinations(size);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#combinations()
     */
    @Override
    default PStackX<ReactiveSeq<T>> combinations() {

        return (PStackX<ReactiveSeq<T>>) PersistentCollectionX.super.combinations();
    }

    @Override
    default PStackX<ListX<T>> sliding(final int windowSize) {
        return (PStackX<ListX<T>>) PersistentCollectionX.super.sliding(windowSize);
    }

    @Override
    default PStackX<ListX<T>> sliding(final int windowSize, final int increment) {
        return (PStackX<ListX<T>>) PersistentCollectionX.super.sliding(windowSize, increment);
    }

    @Override
    default <U> PStackX<U> scanLeft(final U seed, final BiFunction<? super U, ? super T, ? extends U> function) {
        return (PStackX<U>) PersistentCollectionX.super.scanLeft(seed, function);
    }

    @Override
    default <U> PStackX<U> scanRight(final U identity, final BiFunction<? super T, ? super U, ? extends U> combiner) {
        return (PStackX<U>) PersistentCollectionX.super.scanRight(identity, combiner);
    }

    @Override
    default PStackX<T> scanLeft(final Monoid<T> monoid) {

        return (PStackX<T>) PersistentCollectionX.super.scanLeft(monoid);

    }

    @Override
    default PStackX<T> scanRight(final Monoid<T> monoid) {
        return (PStackX<T>) PersistentCollectionX.super.scanRight(monoid);

    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#cycle(int)
     */
    @Override
    default PStackX<T> cycle(final int times) {

        return (PStackX<T>) PersistentCollectionX.super.cycle(times);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#cycle(com.aol.cyclops.sequence.Monoid, int)
     */
    @Override
    default PStackX<T> cycle(final Monoid<T> m, final int times) {

        return (PStackX<T>) PersistentCollectionX.super.cycle(m, times);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#cycleWhile(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> cycleWhile(final Predicate<? super T> predicate) {

        return (PStackX<T>) PersistentCollectionX.super.cycleWhile(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#cycleUntil(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> cycleUntil(final Predicate<? super T> predicate) {

        return (PStackX<T>) PersistentCollectionX.super.cycleUntil(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#zipStream(java.util.stream.Stream)
     */
    @Override
    default <U> PStackX<Tuple2<T, U>> zip(final Stream<? extends U> other) {

        return (PStackX) PersistentCollectionX.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#zip(org.jooq.lambda.Seq)
     */
    @Override
    default <U> PStackX<Tuple2<T, U>> zip(final Seq<? extends U> other) {

        return (PStackX) PersistentCollectionX.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#zip3(java.util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    default <S, U> PStackX<Tuple3<T, S, U>> zip3(final Stream<? extends S> second, final Stream<? extends U> third) {

        return (PStackX) PersistentCollectionX.super.zip3(second, third);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#zip4(java.util.stream.Stream, java.util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    default <T2, T3, T4> PStackX<Tuple4<T, T2, T3, T4>> zip4(final Stream<? extends T2> second, final Stream<? extends T3> third,
            final Stream<? extends T4> fourth) {

        return (PStackX) PersistentCollectionX.super.zip4(second, third, fourth);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#zipWithIndex()
     */
    @Override
    default PStackX<Tuple2<T, Long>> zipWithIndex() {

        return (PStackX<Tuple2<T, Long>>) PersistentCollectionX.super.zipWithIndex();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#distinct()
     */
    @Override
    default PStackX<T> distinct() {

        return (PStackX<T>) PersistentCollectionX.super.distinct();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#sorted()
     */
    @Override
    default PStackX<T> sorted() {

        return (PStackX<T>) PersistentCollectionX.super.sorted();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#sorted(java.util.Comparator)
     */
    @Override
    default PStackX<T> sorted(final Comparator<? super T> c) {

        return (PStackX<T>) PersistentCollectionX.super.sorted(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#skipWhile(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> skipWhile(final Predicate<? super T> p) {

        return (PStackX<T>) PersistentCollectionX.super.skipWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#skipUntil(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> skipUntil(final Predicate<? super T> p) {

        return (PStackX<T>) PersistentCollectionX.super.skipUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#limitWhile(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> limitWhile(final Predicate<? super T> p) {

        return (PStackX<T>) PersistentCollectionX.super.limitWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#limitUntil(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> limitUntil(final Predicate<? super T> p) {

        return (PStackX<T>) PersistentCollectionX.super.limitUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#intersperse(java.lang.Object)
     */
    @Override
    default PStackX<T> intersperse(final T value) {

        return (PStackX<T>) PersistentCollectionX.super.intersperse(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#shuffle()
     */
    @Override
    default PStackX<T> shuffle() {

        return (PStackX<T>) PersistentCollectionX.super.shuffle();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#skipLast(int)
     */
    @Override
    default PStackX<T> skipLast(final int num) {

        return (PStackX<T>) PersistentCollectionX.super.skipLast(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#limitLast(int)
     */
    @Override
    default PStackX<T> limitLast(final int num) {

        return (PStackX<T>) PersistentCollectionX.super.limitLast(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.OnEmptySwitch#onEmptySwitch(java.util.function.Supplier)
     */
    @Override
    default PStackX<T> onEmptySwitch(final Supplier<? extends PStack<T>> supplier) {
        if (this.isEmpty())
            return PStackX.fromIterable(supplier.get());
        return this;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#onEmpty(java.lang.Object)
     */
    @Override
    default PStackX<T> onEmpty(final T value) {

        return (PStackX<T>) PersistentCollectionX.super.onEmpty(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#onEmptyGet(java.util.function.Supplier)
     */
    @Override
    default PStackX<T> onEmptyGet(final Supplier<? extends T> supplier) {

        return (PStackX<T>) PersistentCollectionX.super.onEmptyGet(supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#onEmptyThrow(java.util.function.Supplier)
     */
    @Override
    default <X extends Throwable> PStackX<T> onEmptyThrow(final Supplier<? extends X> supplier) {

        return (PStackX<T>) PersistentCollectionX.super.onEmptyThrow(supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#shuffle(java.util.Random)
     */
    @Override
    default PStackX<T> shuffle(final Random random) {

        return (PStackX<T>) PersistentCollectionX.super.shuffle(random);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#ofType(java.lang.Class)
     */
    @Override
    default <U> PStackX<U> ofType(final Class<? extends U> type) {

        return (PStackX<U>) PersistentCollectionX.super.ofType(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#filterNot(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> filterNot(final Predicate<? super T> fn) {

        return (PStackX<T>) PersistentCollectionX.super.filterNot(fn);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#notNull()
     */
    @Override
    default PStackX<T> notNull() {

        return (PStackX<T>) PersistentCollectionX.super.notNull();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#removeAll(java.util.stream.Stream)
     */
    @Override
    default PStackX<T> removeAll(final Stream<? extends T> stream) {

        return (PStackX<T>) PersistentCollectionX.super.removeAll(stream);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#removeAll(java.lang.Iterable)
     */
    @Override
    default PStackX<T> removeAll(final Iterable<? extends T> it) {

        return (PStackX<T>) PersistentCollectionX.super.removeAll(it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#removeAll(java.lang.Object[])
     */
    @Override
    default PStackX<T> removeAll(final T... values) {

        return (PStackX<T>) PersistentCollectionX.super.removeAll(values);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#retainAll(java.lang.Iterable)
     */
    @Override
    default PStackX<T> retainAll(final Iterable<? extends T> it) {

        return (PStackX<T>) PersistentCollectionX.super.retainAll(it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#retainAll(java.util.stream.Stream)
     */
    @Override
    default PStackX<T> retainAll(final Stream<? extends T> seq) {

        return (PStackX<T>) PersistentCollectionX.super.retainAll(seq);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#retainAll(java.lang.Object[])
     */
    @Override
    default PStackX<T> retainAll(final T... values) {

        return (PStackX<T>) PersistentCollectionX.super.retainAll(values);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#cast(java.lang.Class)
     */
    @Override
    default <U> PStackX<U> cast(final Class<? extends U> type) {

        return (PStackX<U>) PersistentCollectionX.super.cast(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.persistent.PersistentCollectionX#patternMatch(java.lang.Object, java.util.function.Function)
     */
    @Override
    default <R> PStackX<R> patternMatch(final Function<CheckValue1<T, R>, CheckValue1<T, R>> case1, final Supplier<? extends R> otherwise) {

        return (PStackX<R>) PersistentCollectionX.super.patternMatch(case1, otherwise);
    }

    @Override
    default <C extends Collection<? super T>> PStackX<C> grouped(final int size, final Supplier<C> supplier) {

        return (PStackX<C>) PersistentCollectionX.super.grouped(size, supplier);
    }

    @Override
    default PStackX<ListX<T>> groupedUntil(final Predicate<? super T> predicate) {

        return (PStackX<ListX<T>>) PersistentCollectionX.super.groupedUntil(predicate);
    }

    @Override
    default PStackX<ListX<T>> groupedStatefullyUntil(final BiPredicate<ListX<? super T>, ? super T> predicate) {

        return (PStackX<ListX<T>>) PersistentCollectionX.super.groupedStatefullyUntil(predicate);
    }

    @Override
    default PStackX<ListX<T>> groupedWhile(final Predicate<? super T> predicate) {

        return (PStackX<ListX<T>>) PersistentCollectionX.super.groupedWhile(predicate);
    }

    @Override
    default <C extends Collection<? super T>> PStackX<C> groupedWhile(final Predicate<? super T> predicate, final Supplier<C> factory) {

        return (PStackX<C>) PersistentCollectionX.super.groupedWhile(predicate, factory);
    }

    @Override
    default <C extends Collection<? super T>> PStackX<C> groupedUntil(final Predicate<? super T> predicate, final Supplier<C> factory) {

        return (PStackX<C>) PersistentCollectionX.super.groupedUntil(predicate, factory);
    }

    @Override
    default PStackX<T> removeAll(final Seq<? extends T> stream) {

        return (PStackX<T>) PersistentCollectionX.super.removeAll(stream);
    }

    @Override
    default PStackX<T> retainAll(final Seq<? extends T> stream) {

        return (PStackX<T>) PersistentCollectionX.super.retainAll(stream);
    }

}
