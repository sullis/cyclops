package cyclops.collections;

import com.aol.cyclops2.data.collections.extensions.lazy.LazySetX;
import com.aol.cyclops2.data.collections.extensions.standard.MutableCollectionX;
import cyclops.Streams;
import cyclops.collections.immutable.PVectorX;
import cyclops.function.Monoid;
import cyclops.stream.ReactiveSeq;
import cyclops.control.Trampoline;
import com.aol.cyclops2.types.OnEmptySwitch;
import com.aol.cyclops2.types.To;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;
import org.reactivestreams.Publisher;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public interface SetX<T> extends To<SetX<T>>,Set<T>, MutableCollectionX<T>, OnEmptySwitch<T, Set<T>> {

    /**
     * Create a SetX that contains the Integers between skip and take
     * 
     * @param start
     *            Number of range to skip from
     * @param end
     *            Number for range to take at
     * @return Range SetX
     */
    public static SetX<Integer> range(final int start, final int end) {
        return ReactiveSeq.range(start, end)
                          .toSetX();
    }

    /**
     * Create a SetX that contains the Longs between skip and take
     * 
     * @param start
     *            Number of range to skip from
     * @param end
     *            Number for range to take at
     * @return Range SetX
     */
    public static SetX<Long> rangeLong(final long start, final long end) {
        return ReactiveSeq.rangeLong(start, end)
                          .toSetX();
    }

    /**
     * Unfold a function into a SetX
     * 
     * <pre>
     * {@code 
     *  SetX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return SetX generated by unfolder function
     */
    static <U, T> SetX<T> unfold(final U seed, final Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return ReactiveSeq.unfold(seed, unfolder)
                          .toSetX();
    }

    /**
     * Generate a SetX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate SetX elements
     * @return SetX generated from the provided Supplier
     */
    public static <T> SetX<T> generate(final long limit, final Supplier<T> s) {

        return ReactiveSeq.generate(s)
                          .limit(limit)
                          .toSetX();
    }

    /**
     * Create a SetX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return SetX generated by iterative application
     */
    public static <T> SetX<T> iterate(final long limit, final T seed, final UnaryOperator<T> f) {
        return ReactiveSeq.iterate(seed, f)
                          .limit(limit)
                          .toSetX();

    }

    static <T> Collector<T, ?, SetX<T>> setXCollector() {
        return Collectors.toCollection(() -> SetX.of());
    }

    static <T> Collector<T, ?, Set<T>> defaultCollector() {
        return Collectors.toCollection(() -> new HashSet<>());
    }

    static <T> Collector<T, ?, Set<T>> immutableCollector() {
        return Collectors.collectingAndThen(defaultCollector(), (final Set<T> d) -> Collections.unmodifiableSet(d));

    }

    public static <T> SetX<T> empty() {
        return fromIterable((Set<T>) defaultCollector().supplier()
                                                       .get());
    }

    @SafeVarargs
    public static <T> SetX<T> of(final T... values) {
        final Set<T> res = (Set<T>) defaultCollector().supplier()
                                                      .get();
        for (final T v : values)
            res.add(v);
        return fromIterable(res);
    }
    public static <T> SetX<T> fromIterator(final Iterator<T> it) {
        return fromIterable(()->it);
    }
    public static <T> SetX<T> singleton(final T value) {
        return SetX.<T> of(value);
    }

    /**
     * Construct a SetX from an Publisher
     * 
     * @param publisher
     *            to construct SetX from
     * @return SetX
     */
    public static <T> SetX<T> fromPublisher(final Publisher<? extends T> publisher) {
        return ReactiveSeq.fromPublisher((Publisher<T>) publisher)
                          .toSetX();
    }

    public static <T> SetX<T> fromIterable(final Iterable<T> it) {
        if (it instanceof SetX)
            return (SetX) it;
        if (it instanceof Set)
            return new LazySetX<T>(
                                   (Set) it, defaultCollector());
        return new LazySetX<T>(
                               Streams.stream(it)
                                          .collect(defaultCollector()),
                                          defaultCollector());
    }

    public static <T> SetX<T> fromIterable(final Collector<T, ?, Set<T>> collector, final Iterable<T> it) {
        if (it instanceof SetX)
            return ((SetX) it).withCollector(collector);
        if (it instanceof Set)
            return new LazySetX<T>(
                                   (Set) it, collector);
        return new LazySetX<T>(
                               Streams.stream(it)
                                          .collect(collector),
                               collector);
    }

    @Override
    default SetX<T> materialize() {
        return (SetX<T>)MutableCollectionX.super.materialize();
    }

    @Override
    default SetX<T> take(final long num) {

        return (SetX<T>) MutableCollectionX.super.limit(num);
    }
    @Override
    default SetX<T> drop(final long num) {

        return (SetX<T>) MutableCollectionX.super.skip(num);    
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach4(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops2.util.function.TriFunction, com.aol.cyclops2.util.function.QuadFunction)
     */
    @Override
    default <R1, R2, R3, R> SetX<R> forEach4(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, ? extends Iterable<R2>> stream2,
            Fn3<? super T, ? super R1, ? super R2, ? extends Iterable<R3>> stream3,
            Fn4<? super T, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {
        
        return (SetX)MutableCollectionX.super.forEach4(stream1, stream2, stream3, yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach4(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops2.util.function.TriFunction, com.aol.cyclops2.util.function.QuadFunction, com.aol.cyclops2.util.function.QuadFunction)
     */
    @Override
    default <R1, R2, R3, R> SetX<R> forEach4(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, ? extends Iterable<R2>> stream2,
            Fn3<? super T, ? super R1, ? super R2, ? extends Iterable<R3>> stream3,
            Fn4<? super T, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
            Fn4<? super T, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {
        
        return (SetX)MutableCollectionX.super.forEach4(stream1, stream2, stream3, filterFunction, yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach3(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops2.util.function.TriFunction)
     */
    @Override
    default <R1, R2, R> SetX<R> forEach3(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, ? extends Iterable<R2>> stream2,
            Fn3<? super T, ? super R1, ? super R2, ? extends R> yieldingFunction) {
        
        return (SetX)MutableCollectionX.super.forEach3(stream1, stream2, yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach3(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops2.util.function.TriFunction, com.aol.cyclops2.util.function.TriFunction)
     */
    @Override
    default <R1, R2, R> SetX<R> forEach3(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, ? extends Iterable<R2>> stream2,
            Fn3<? super T, ? super R1, ? super R2, Boolean> filterFunction,
            Fn3<? super T, ? super R1, ? super R2, ? extends R> yieldingFunction) {
        
        return (SetX)MutableCollectionX.super.forEach3(stream1, stream2, filterFunction, yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach2(java.util.function.Function, java.util.function.BiFunction)
     */
    @Override
    default <R1, R> SetX<R> forEach2(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {
        
        return (SetX)MutableCollectionX.super.forEach2(stream1, yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach2(java.util.function.Function, java.util.function.BiFunction, java.util.function.BiFunction)
     */
    @Override
    default <R1, R> SetX<R> forEach2(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, Boolean> filterFunction,
            BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {
        
        return (SetX)MutableCollectionX.super.forEach2(stream1, filterFunction, yieldingFunction);
    }

    SetX<T> withCollector(Collector<T, ?, Set<T>> collector);

    /**
     * coflatMap pattern, can be used to perform lazy reductions / collections / folds and other terminal operations
     * 
     * <pre>
     * {@code 
     *   
     *     SetX.of(1,2,3)
     *           .map(i->i*2)
     *           .coflatMap(s -> s.reduce(0,(a,b)->a+b))
     *      
     *      //SetX[12]
     * }
     * </pre>
     * 
     * 
     * @param fn mapping function
     * @return Transformed Set
     */
    default <R> SetX<R> coflatMap(Function<? super SetX<T>, ? extends R> fn){
        return fn.andThen(r ->  this.<R>unit(r))
                  .apply(this);
    }

    /**
     * Combine two adjacent elements in a SetX using the supplied BinaryOperator
     * This is a stateful grouping and reduction operation. The output of a combination may in turn be combined
     * with it's neighbor
     * <pre>
     * {@code 
     *  SetX.of(1,1,2,3)
                   .combine((a, b)->a.equals(b),Semigroups.intSum)
                   .toListX()
                   
     *  //ListX(3,4) 
     * }</pre>
     * 
     * @param predicate Test to see if two neighbors should be joined
     * @param op Reducer to combine neighbors
     * @return Combined / Partially Reduced SetX
     */
    @Override
    default SetX<T> combine(final BiPredicate<? super T, ? super T> predicate, final BinaryOperator<T> op) {
        return (SetX<T>) MutableCollectionX.super.combine(predicate, op);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.sequence.traits.ConvertableSequence#toListX()
     */
    @Override
    default SetX<T> toSetX() {
        return this;
    }
 
    @Override
    default ReactiveSeq<T> stream() {

        return ReactiveSeq.fromIterable(this);
    }

    @Override
    default <R> SetX<R> unit(final Collection<R> col) {
        return fromIterable(col);
    }

    @Override
    default <R> SetX<R> unit(final R value) {
        return singleton(value);
    }

    @Override
    default <R> SetX<R> unitIterator(final Iterator<R> it) {
        return fromIterable(() -> it);
    }

    @Override
    default <T1> SetX<T1> from(final Collection<T1> c) {
        return SetX.<T1> fromIterable(getCollector(), c);
    }

    public <T> Collector<T, ?, Set<T>> getCollector();

    @Override
    default <X> SetX<X> fromStream(final Stream<X> stream) {
        return new LazySetX<>(
                              stream.collect(getCollector()), getCollector());
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#reverse()
     */
    @Override
    default SetX<T> reverse() {
        return (SetX<T>) MutableCollectionX.super.reverse();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#filter(java.util.function.Predicate)
     */
    @Override
    default SetX<T> filter(final Predicate<? super T> pred) {

        return (SetX<T>) MutableCollectionX.super.filter(pred);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#map(java.util.function.Function)
     */
    @Override
    default <R> SetX<R> map(final Function<? super T, ? extends R> mapper) {

        return (SetX<R>) MutableCollectionX.super.<R> map(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#flatMap(java.util.function.Function)
     */
    @Override
    default <R> SetX<R> flatMap(final Function<? super T, ? extends Iterable<? extends R>> mapper) {

        return (SetX<R>) MutableCollectionX.super.<R> flatMap(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#limit(long)
     */
    @Override
    default SetX<T> limit(final long num) {
        return (SetX<T>) MutableCollectionX.super.limit(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#skip(long)
     */
    @Override
    default SetX<T> skip(final long num) {

        return (SetX<T>) MutableCollectionX.super.skip(num);
    }

    @Override
    default SetX<T> takeRight(final int num) {
        return (SetX<T>) MutableCollectionX.super.takeRight(num);
    }

    @Override
    default SetX<T> dropRight(final int num) {
        return (SetX<T>) MutableCollectionX.super.dropRight(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#takeWhile(java.util.function.Predicate)
     */
    @Override
    default SetX<T> takeWhile(final Predicate<? super T> p) {

        return (SetX<T>) MutableCollectionX.super.takeWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#dropWhile(java.util.function.Predicate)
     */
    @Override
    default SetX<T> dropWhile(final Predicate<? super T> p) {

        return (SetX<T>) MutableCollectionX.super.dropWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#takeUntil(java.util.function.Predicate)
     */
    @Override
    default SetX<T> takeUntil(final Predicate<? super T> p) {

        return (SetX<T>) MutableCollectionX.super.takeUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#dropUntil(java.util.function.Predicate)
     */
    @Override
    default SetX<T> dropUntil(final Predicate<? super T> p) {

        return (SetX<T>) MutableCollectionX.super.dropUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#trampoline(java.util.function.Function)
     */
    @Override
    default <R> SetX<R> trampoline(final Function<? super T, ? extends Trampoline<? extends R>> mapper) {

        return (SetX<R>) MutableCollectionX.super.<R> trampoline(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#slice(long, long)
     */
    @Override
    default SetX<T> slice(final long from, final long to) {

        return (SetX<T>) MutableCollectionX.super.slice(from, to);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#sorted(java.util.function.Function)
     */
    @Override
    default <U extends Comparable<? super U>> SetX<T> sorted(final Function<? super T, ? extends U> function) {

        return (SetX<T>) MutableCollectionX.super.sorted(function);
    }

    @Override
    default SetX<ListX<T>> grouped(final int groupSize) {
        return (SetX) MutableCollectionX.super.grouped(groupSize);
    }

    @Override
    default <K, A, D> SetX<Tuple2<K, D>> grouped(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream) {
        return (SetX) MutableCollectionX.super.grouped(classifier, downstream);
    }

    @Override
    default <K> SetX<Tuple2<K, ReactiveSeq<T>>> grouped(final Function<? super T, ? extends K> classifier) {
        return (SetX) MutableCollectionX.super.grouped(classifier);
    }

    @Override
    default <U> SetX<Tuple2<T, U>> zip(final Iterable<? extends U> other) {
        return (SetX) MutableCollectionX.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#zip(java.lang.Iterable, java.util.function.BiFunction)
     */
    @Override
    default <U, R> SetX<R> zip(final Iterable<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (SetX<R>) MutableCollectionX.super.zip(other, zipper);
    }

    @Override
    default <U, R> SetX<R> zipS(final Stream<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (SetX<R>) MutableCollectionX.super.zipS(other, zipper);
    }


    @Override
    default SetX<PVectorX<T>> sliding(final int windowSize) {
        return (SetX<PVectorX<T>>) MutableCollectionX.super.sliding(windowSize);
    }

    @Override
    default SetX<PVectorX<T>> sliding(final int windowSize, final int increment) {
        return (SetX<PVectorX<T>>) MutableCollectionX.super.sliding(windowSize, increment);
    }

    @Override
    default SetX<T> scanLeft(final Monoid<T> monoid) {
        return (SetX<T>) MutableCollectionX.super.scanLeft(monoid);
    }

    @Override
    default <U> SetX<U> scanLeft(final U seed, final BiFunction<? super U, ? super T, ? extends U> function) {
        return (SetX<U>) MutableCollectionX.super.scanLeft(seed, function);
    }

    @Override
    default SetX<T> scanRight(final Monoid<T> monoid) {
        return (SetX<T>) MutableCollectionX.super.scanRight(monoid);
    }

    @Override
    default <U> SetX<U> scanRight(final U identity, final BiFunction<? super T, ? super U, ? extends U> combiner) {
        return (SetX<U>) MutableCollectionX.super.scanRight(identity, combiner);
    }

    @Override
    default SetX<T> plus(final T e) {
        add(e);
        return this;
    }

    @Override
    default SetX<T> plusAll(final Collection<? extends T> list) {
        addAll(list);
        return this;
    }

    @Override
    default SetX<T> minus(final Object e) {
        remove(e);
        return this;
    }

    @Override
    default SetX<T> minusAll(final Collection<?> list) {
        removeAll(list);
        return this;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#cycle(int)
     */
    @Override
    default ListX<T> cycle(final long times) {

        return this.stream()
                   .cycle(times)
                   .toListX();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#cycle(com.aol.cyclops2.sequence.Monoid, int)
     */
    @Override
    default ListX<T> cycle(final Monoid<T> m, final long times) {

        return this.stream()
                   .cycle(m, times)
                   .toListX();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#cycleWhile(java.util.function.Predicate)
     */
    @Override
    default ListX<T> cycleWhile(final Predicate<? super T> predicate) {

        return this.stream()
                   .cycleWhile(predicate)
                   .toListX();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#cycleUntil(java.util.function.Predicate)
     */
    @Override
    default ListX<T> cycleUntil(final Predicate<? super T> predicate) {

        return this.stream()
                   .cycleUntil(predicate)
                   .toListX();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#zip(java.util.reactiveStream.Stream)
     */
    @Override
    default <U> SetX<Tuple2<T, U>> zipS(final Stream<? extends U> other) {

        return (SetX) MutableCollectionX.super.zipS(other);
    }


    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#zip3(java.util.reactiveStream.Stream, java.util.reactiveStream.Stream)
     */
    @Override
    default <S, U> SetX<Tuple3<T, S, U>> zip3(final Iterable<? extends S> second, final Iterable<? extends U> third) {

        return (SetX) MutableCollectionX.super.zip3(second, third);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#zip4(java.util.reactiveStream.Stream, java.util.reactiveStream.Stream, java.util.reactiveStream.Stream)
     */
    @Override
    default <T2, T3, T4> SetX<Tuple4<T, T2, T3, T4>> zip4(final Iterable<? extends T2> second, final Iterable<? extends T3> third,
            final Iterable<? extends T4> fourth) {

        return (SetX) MutableCollectionX.super.zip4(second, third, fourth);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#zipWithIndex()
     */
    @Override
    default SetX<Tuple2<T, Long>> zipWithIndex() {

        return (SetX<Tuple2<T, Long>>) MutableCollectionX.super.zipWithIndex();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#distinct()
     */
    @Override
    default SetX<T> distinct() {

        return (SetX<T>) MutableCollectionX.super.distinct();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#sorted()
     */
    @Override
    default SetX<T> sorted() {

        return (SetX<T>) MutableCollectionX.super.sorted();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#sorted(java.util.Comparator)
     */
    @Override
    default SetX<T> sorted(final Comparator<? super T> c) {

        return (SetX<T>) MutableCollectionX.super.sorted(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#skipWhile(java.util.function.Predicate)
     */
    @Override
    default SetX<T> skipWhile(final Predicate<? super T> p) {

        return (SetX<T>) MutableCollectionX.super.skipWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#skipUntil(java.util.function.Predicate)
     */
    @Override
    default SetX<T> skipUntil(final Predicate<? super T> p) {

        return (SetX<T>) MutableCollectionX.super.skipUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#limitWhile(java.util.function.Predicate)
     */
    @Override
    default SetX<T> limitWhile(final Predicate<? super T> p) {

        return (SetX<T>) MutableCollectionX.super.limitWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#limitUntil(java.util.function.Predicate)
     */
    @Override
    default SetX<T> limitUntil(final Predicate<? super T> p) {

        return (SetX<T>) MutableCollectionX.super.limitUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#intersperse(java.lang.Object)
     */
    @Override
    default SetX<T> intersperse(final T value) {

        return (SetX<T>) MutableCollectionX.super.intersperse(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#shuffle()
     */
    @Override
    default SetX<T> shuffle() {

        return (SetX<T>) MutableCollectionX.super.shuffle();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#skipLast(int)
     */
    @Override
    default SetX<T> skipLast(final int num) {

        return (SetX<T>) MutableCollectionX.super.skipLast(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#limitLast(int)
     */
    @Override
    default SetX<T> limitLast(final int num) {

        return (SetX<T>) MutableCollectionX.super.limitLast(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.types.OnEmptySwitch#onEmptySwitch(java.util.function.Supplier)
     */
    @Override
    default SetX<T> onEmptySwitch(final Supplier<? extends Set<T>> supplier) {
        if (isEmpty())
            return SetX.fromIterable(supplier.get());
        return this;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#onEmpty(java.lang.Object)
     */
    @Override
    default SetX<T> onEmpty(final T value) {

        return (SetX<T>) MutableCollectionX.super.onEmpty(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#onEmptyGet(java.util.function.Supplier)
     */
    @Override
    default SetX<T> onEmptyGet(final Supplier<? extends T> supplier) {

        return (SetX<T>) MutableCollectionX.super.onEmptyGet(supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#onEmptyThrow(java.util.function.Supplier)
     */
    @Override
    default <X extends Throwable> SetX<T> onEmptyThrow(final Supplier<? extends X> supplier) {

        return (SetX<T>) MutableCollectionX.super.onEmptyThrow(supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#shuffle(java.util.Random)
     */
    @Override
    default SetX<T> shuffle(final Random random) {

        return (SetX<T>) MutableCollectionX.super.shuffle(random);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#ofType(java.lang.Class)
     */
    @Override
    default <U> SetX<U> ofType(final Class<? extends U> type) {

        return (SetX<U>) MutableCollectionX.super.ofType(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#filterNot(java.util.function.Predicate)
     */
    @Override
    default SetX<T> filterNot(final Predicate<? super T> fn) {

        return (SetX<T>) MutableCollectionX.super.filterNot(fn);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#notNull()
     */
    @Override
    default SetX<T> notNull() {

        return (SetX<T>) MutableCollectionX.super.notNull();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#removeAllS(java.util.reactiveStream.Stream)
     */
    @Override
    default SetX<T> removeAllS(final Stream<? extends T> stream) {

        return (SetX<T>) MutableCollectionX.super.removeAllS(stream);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#removeAllS(java.lang.Iterable)
     */
    @Override
    default SetX<T> removeAllS(final Iterable<? extends T> it) {

        return (SetX<T>) MutableCollectionX.super.removeAllS(it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#removeAllS(java.lang.Object[])
     */
    @Override
    default SetX<T> removeAllS(final T... values) {

        return (SetX<T>) MutableCollectionX.super.removeAllS(values);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#retainAllS(java.lang.Iterable)
     */
    @Override
    default SetX<T> retainAllS(final Iterable<? extends T> it) {

        return (SetX<T>) MutableCollectionX.super.retainAllS(it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#retainAllS(java.util.reactiveStream.Stream)
     */
    @Override
    default SetX<T> retainAllS(final Stream<? extends T> seq) {

        return (SetX<T>) MutableCollectionX.super.retainAllS(seq);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#retainAllS(java.lang.Object[])
     */
    @Override
    default SetX<T> retainAllS(final T... values) {

        return (SetX<T>) MutableCollectionX.super.retainAllS(values);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#cast(java.lang.Class)
     */
    @Override
    default <U> SetX<U> cast(final Class<? extends U> type) {

        return (SetX<U>) MutableCollectionX.super.cast(type);
    }


    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.standard.MutableCollectionX#grouped(int, java.util.function.Supplier)
     */
    @Override
    default <C extends Collection<? super T>> SetX<C> grouped(final int size, final Supplier<C> supplier) {

        return (SetX<C>) MutableCollectionX.super.grouped(size, supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.standard.MutableCollectionX#groupedUntil(java.util.function.Predicate)
     */
    @Override
    default SetX<ListX<T>> groupedUntil(final Predicate<? super T> predicate) {

        return (SetX<ListX<T>>) MutableCollectionX.super.groupedUntil(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.standard.MutableCollectionX#groupedWhile(java.util.function.Predicate)
     */
    @Override
    default SetX<ListX<T>> groupedWhile(final Predicate<? super T> predicate) {

        return (SetX<ListX<T>>) MutableCollectionX.super.groupedWhile(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.standard.MutableCollectionX#groupedWhile(java.util.function.Predicate, java.util.function.Supplier)
     */
    @Override
    default <C extends Collection<? super T>> SetX<C> groupedWhile(final Predicate<? super T> predicate, final Supplier<C> factory) {

        return (SetX<C>) MutableCollectionX.super.groupedWhile(predicate, factory);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.standard.MutableCollectionX#groupedUntil(java.util.function.Predicate, java.util.function.Supplier)
     */
    @Override
    default <C extends Collection<? super T>> SetX<C> groupedUntil(final Predicate<? super T> predicate, final Supplier<C> factory) {

        return (SetX<C>) MutableCollectionX.super.groupedUntil(predicate, factory);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.standard.MutableCollectionX#groupedStatefullyUntil(java.util.function.BiPredicate)
     */
    @Override
    default SetX<ListX<T>> groupedStatefullyUntil(final BiPredicate<ListX<? super T>, ? super T> predicate) {

        return (SetX<ListX<T>>) MutableCollectionX.super.groupedStatefullyUntil(predicate);
    }


    @Override
    default <R> SetX<R> retry(final Function<? super T, ? extends R> fn) {
        return (SetX<R>)MutableCollectionX.super.retry(fn);
    }

    @Override
    default <R> SetX<R> retry(final Function<? super T, ? extends R> fn, final int retries, final long delay, final TimeUnit timeUnit) {
        return (SetX<R>)MutableCollectionX.super.retry(fn);
    }

    @Override
    default <R> SetX<R> flatMapS(Function<? super T, ? extends Stream<? extends R>> fn) {
        return (SetX<R>)MutableCollectionX.super.flatMapS(fn);
    }

    @Override
    default <R> SetX<R> flatMapP(Function<? super T, ? extends Publisher<? extends R>> fn) {
        return (SetX<R>)MutableCollectionX.super.flatMapP(fn);
    }

    @Override
    default SetX<T> prependS(Stream<? extends T> stream) {
        return (SetX<T>)MutableCollectionX.super.prependS(stream);
    }

    @Override
    default SetX<T> append(T... values) {
        return (SetX<T>)MutableCollectionX.super.append(values);
    }

    @Override
    default SetX<T> append(T value) {
        return (SetX<T>)MutableCollectionX.super.append(value);
    }

    @Override
    default SetX<T> prepend(T value) {
        return (SetX<T>)MutableCollectionX.super.prepend(value);
    }

    @Override
    default SetX<T> prepend(T... values) {
        return (SetX<T>)MutableCollectionX.super.prepend(values);
    }

    @Override
    default SetX<T> insertAt(int pos, T... values) {
        return (SetX<T>)MutableCollectionX.super.insertAt(pos,values);
    }

    @Override
    default SetX<T> deleteBetween(int start, int end) {
        return (SetX<T>)MutableCollectionX.super.deleteBetween(start,end);
    }

    @Override
    default SetX<T> insertAtS(int pos, Stream<T> stream) {
        return (SetX<T>)MutableCollectionX.super.insertAtS(pos,stream);
    }

    @Override
    default SetX<T> recover(final Function<? super Throwable, ? extends T> fn) {
        return (SetX<T>)MutableCollectionX.super.recover(fn);
    }

    @Override
    default <EX extends Throwable> SetX<T> recover(Class<EX> exceptionClass, final Function<? super EX, ? extends T> fn) {
        return (SetX<T>)MutableCollectionX.super.recover(exceptionClass,fn);
    }

    @Override
    default SetX<T> plusLoop(int max, IntFunction<T> value) {
        return (SetX<T>)MutableCollectionX.super.plusLoop(max,value);
    }

    @Override
    default SetX<T> plusLoop(Supplier<Optional<T>> supplier) {
        return (SetX<T>)MutableCollectionX.super.plusLoop(supplier);
    }

    /**
     * Narrow a covariant Set
     * 
     * <pre>
     * {@code 
     * SetX<? extends Fruit> set = SetX.of(apple,bannana);
     * SetX<Fruit> fruitSet = SetX.narrowK(set);
     * }
     * </pre>
     * 
     * @param setX to narrowK generic type
     * @return SetX with narrowed type
     */
    public static <T> SetX<T> narrow(final SetX<? extends T> setX) {
        return (SetX<T>) setX;
    }

}
