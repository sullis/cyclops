package cyclops.collections.immutable;


import com.aol.cyclops2.data.collections.extensions.lazy.immutable.LazyPSetX;
import com.aol.cyclops2.data.collections.extensions.persistent.PersistentCollectionX;
import cyclops.function.Monoid;
import cyclops.function.Reducer;
import cyclops.Reducers;
import cyclops.stream.ReactiveSeq;
import cyclops.control.Trampoline;
import cyclops.collections.ListX;
import com.aol.cyclops2.types.OnEmptySwitch;
import com.aol.cyclops2.types.To;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;
import org.reactivestreams.Publisher;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;


public interface PSetX<T> extends To<PSetX<T>>,PSet<T>, PersistentCollectionX<T>, OnEmptySwitch<T, PSet<T>> {
    /**
     * Narrow a covariant PSetX
     * 
     * <pre>
     * {@code 
     *  PSetX<? extends Fruit> set = PSetX.of(apple,bannana);
     *  PSetX<Fruit> fruitSet = PSetX.narrowK(set);
     * }
     * </pre>
     * 
     * @param setX to narrowK generic type
     * @return PSetX with narrowed type
     */
    public static <T> PSetX<T> narrow(final PSetX<? extends T> setX) {
        return (PSetX<T>) setX;
    }
    /**
     * Create a PSetX that contains the Integers between skip and take
     * 
     * @param start
     *            Number of range to skip from
     * @param end
     *            Number for range to take at
     * @return Range PSetX
     */
    public static PSetX<Integer> range(final int start, final int end) {
        return ReactiveSeq.range(start, end)
                          .toPSetX();
    }

    /**
     * Create a PSetX that contains the Longs between skip and take
     * 
     * @param start
     *            Number of range to skip from
     * @param end
     *            Number for range to take at
     * @return Range PSetX
     */
    public static PSetX<Long> rangeLong(final long start, final long end) {
        return ReactiveSeq.rangeLong(start, end)
                          .toPSetX();
    }

    /**
     * Unfold a function into a PSetX
     * 
     * <pre>
     * {@code 
     *  PSetX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5) in any order
     * 
     * }</code>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return PSetX generated by unfolder function
     */
    static <U, T> PSetX<T> unfold(final U seed, final Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return ReactiveSeq.unfold(seed, unfolder)
                          .toPSetX();
    }

    /**
     * Generate a PSetX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate PSetX elements
     * @return PSetX generated from the provided Supplier
     */
    public static <T> PSetX<T> generate(final long limit, final Supplier<T> s) {

        return ReactiveSeq.generate(s)
                          .limit(limit)
                          .toPSetX();
    }

    /**
     * Create a PSetX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return PSetX generated by iterative application
     */
    public static <T> PSetX<T> iterate(final long limit, final T seed, final UnaryOperator<T> f) {
        return ReactiveSeq.iterate(seed, f)
                          .limit(limit)
                          .toPSetX();

    }

    public static <T> PSetX<T> of(final T... values) {

        return new LazyPSetX<>(
                               HashTreePSet.from(Arrays.asList(values)));
    }

    public static <T> PSetX<T> empty() {
        return new LazyPSetX<>(
                               HashTreePSet.empty());
    }

    public static <T> PSetX<T> singleton(final T value) {
        return new LazyPSetX<>(
                               HashTreePSet.singleton(value));
    }

    public static <T> PSetX<T> fromIterable(final Iterable<T> iterable) {
        if (iterable instanceof PSetX)
            return (PSetX) iterable;
        if (iterable instanceof PSet)
            return new LazyPSetX<>(
                                   (PSet) iterable);
        PSet<T> res = HashTreePSet.<T> empty();
        final Iterator<T> it = iterable.iterator();
        while (it.hasNext())
            res = res.plus(it.next());

        return new LazyPSetX<>(
                               res);
    }

    /**
     * Construct a PSetX from an Publisher
     * 
     * @param publisher
     *            to construct PSetX from
     * @return PSetX
     */
    public static <T> PSetX<T> fromPublisher(final Publisher<? extends T> publisher) {
        return ReactiveSeq.fromPublisher((Publisher<T>) publisher)
                          .toPSetX();
    }

    public static <T> PSetX<T> fromCollection(final Collection<T> stream) {
        if (stream instanceof PSetX)
            return (PSetX) stream;
        if (stream instanceof PSet)
            return new LazyPSetX<>(
                                   (PSet) stream);
        return new LazyPSetX<>(
                               HashTreePSet.from(stream));
    }

    public static <T> PSetX<T> fromStream(final Stream<T> stream) {
        return Reducers.<T> toPSetX()
                       .mapReduce(stream);
    }
   
    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach4(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops2.util.function.TriFunction, com.aol.cyclops2.util.function.QuadFunction)
     */
    @Override
    default <R1, R2, R3, R> PSetX<R> forEach4(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, ? extends Iterable<R2>> stream2,
            Fn3<? super T, ? super R1, ? super R2, ? extends Iterable<R3>> stream3,
            Fn4<? super T, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {
        
        return (PSetX)PersistentCollectionX.super.forEach4(stream1, stream2, stream3, yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach4(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops2.util.function.TriFunction, com.aol.cyclops2.util.function.QuadFunction, com.aol.cyclops2.util.function.QuadFunction)
     */
    @Override
    default <R1, R2, R3, R> PSetX<R> forEach4(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, ? extends Iterable<R2>> stream2,
            Fn3<? super T, ? super R1, ? super R2, ? extends Iterable<R3>> stream3,
            Fn4<? super T, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
            Fn4<? super T, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {
        
        return (PSetX)PersistentCollectionX.super.forEach4(stream1, stream2, stream3, filterFunction, yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach3(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops2.util.function.TriFunction)
     */
    @Override
    default <R1, R2, R> PSetX<R> forEach3(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, ? extends Iterable<R2>> stream2,
            Fn3<? super T, ? super R1, ? super R2, ? extends R> yieldingFunction) {
        
        return (PSetX)PersistentCollectionX.super.forEach3(stream1, stream2, yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach3(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops2.util.function.TriFunction, com.aol.cyclops2.util.function.TriFunction)
     */
    @Override
    default <R1, R2, R> PSetX<R> forEach3(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, ? extends Iterable<R2>> stream2,
            Fn3<? super T, ? super R1, ? super R2, Boolean> filterFunction,
            Fn3<? super T, ? super R1, ? super R2, ? extends R> yieldingFunction) {
        
        return (PSetX)PersistentCollectionX.super.forEach3(stream1, stream2, filterFunction, yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach2(java.util.function.Function, java.util.function.BiFunction)
     */
    @Override
    default <R1, R> PSetX<R> forEach2(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {
        
        return (PSetX)PersistentCollectionX.super.forEach2(stream1, yieldingFunction);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.data.collections.extensions.CollectionX#forEach2(java.util.function.Function, java.util.function.BiFunction, java.util.function.BiFunction)
     */
    @Override
    default <R1, R> PSetX<R> forEach2(Function<? super T, ? extends Iterable<R1>> stream1,
            BiFunction<? super T, ? super R1, Boolean> filterFunction,
            BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {
        
        return (PSetX)PersistentCollectionX.super.forEach2(stream1, filterFunction, yieldingFunction);
    }
    
    @Override
    default PSetX<T> take(final long num) {
        return limit(num);
    }
    @Override
    default PSetX<T> drop(final long num) {

        return skip(num);
    }
    @Override
    default PSetX<T> toPSetX() {
        return this;
    }
    /**
     * coflatMap pattern, can be used to perform lazy reductions / collections / folds and other terminal operations
     * 
     * <pre>
     * {@code 
     *   
     *     PSetX.of(1,2,3)
     *          .map(i->i*2)
     *          .coflatMap(s -> s.reduce(0,(a,b)->a+b))
     *      
     *     //PSetX[12]
     * }
     * </pre>
     * 
     * 
     * @param fn mapping function
     * @return Transformed PSetX
     */
    default <R> PSetX<R> coflatMap(Function<? super PSetX<T>, ? extends R> fn){
       return fn.andThen(r ->  this.<R>unit(r))
                .apply(this);
    }
  
    /**
    * Combine two adjacent elements in a PSetX using the supplied BinaryOperator
    * This is a stateful grouping & reduction operation. The output of a combination may in turn be combined
    * with it's neighbor
    * <pre>
    * {@code 
    *  PSetX.of(1,1,2,3)
                 .combine((a, b)->a.equals(b),Semigroups.intSum)
                 .toListX()
                 
    *  //ListX(3,4) 
    * }</pre>
    * 
    * @param predicate Test to see if two neighbors should be joined
    * @param op Reducer to combine neighbors
    * @return Combined / Partially Reduced PSetX
    */
    @Override
    default PSetX<T> combine(final BiPredicate<? super T, ? super T> predicate, final BinaryOperator<T> op) {
        return (PSetX<T>) PersistentCollectionX.super.combine(predicate, op);
    }

    @Override
    default <R> PSetX<R> unit(final Collection<R> col) {
        return fromCollection(col);
    }

    @Override
    default <R> PSetX<R> unit(final R value) {
        return singleton(value);
    }

    @Override
    default <R> PSetX<R> unitIterator(final Iterator<R> it) {
        return fromIterable(() -> it);
    }

    @Override
    default <R> PSetX<R> emptyUnit() {
        return empty();
    }

    @Override
    default PSetX<T> materialize() {
        return (PSetX<T>)PersistentCollectionX.super.materialize();
    }

    @Override
    default ReactiveSeq<T> stream() {

        return ReactiveSeq.fromIterable(this);
    }

    default PSet<T> toPSet() {
        return this;
    }

    @Override
    default <X> PSetX<X> from(final Collection<X> col) {
        return fromCollection(col);
    }

    @Override
    default <T> Reducer<PSet<T>> monoid() {
        return Reducers.toPSet();
    }

    /* (non-Javadoc)
     * @see org.pcollections.PSet#plus(java.lang.Object)
     */
    @Override
    public PSetX<T> plus(T e);

    /* (non-Javadoc)
     * @see org.pcollections.PSet#plusAll(java.util.Collection)
     */
    @Override
    public PSetX<T> plusAll(Collection<? extends T> list);

    /* (non-Javadoc)
     * @see org.pcollections.PSet#minus(java.lang.Object)
     */
    @Override
    public PSetX<T> minus(Object e);

    /* (non-Javadoc)
     * @see org.pcollections.PSet#minusAll(java.util.Collection)
     */
    @Override
    public PSetX<T> minusAll(Collection<?> list);

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#reverse()
     */
    @Override
    default PSetX<T> reverse() {
        return (PSetX<T>) PersistentCollectionX.super.reverse();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#filter(java.util.function.Predicate)
     */
    @Override
    default PSetX<T> filter(final Predicate<? super T> pred) {
        return (PSetX<T>) PersistentCollectionX.super.filter(pred);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#map(java.util.function.Function)
     */
    @Override
    default <R> PSetX<R> map(final Function<? super T, ? extends R> mapper) {
        return (PSetX<R>) PersistentCollectionX.super.map(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#flatMap(java.util.function.Function)
     */
    @Override
    default <R> PSetX<R> flatMap(final Function<? super T, ? extends Iterable<? extends R>> mapper) {
        return (PSetX<R>) PersistentCollectionX.super.flatMap(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#limit(long)
     */
    @Override
    default PSetX<T> limit(final long num) {
        return (PSetX<T>) PersistentCollectionX.super.limit(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#skip(long)
     */
    @Override
    default PSetX<T> skip(final long num) {
        return (PSetX<T>) PersistentCollectionX.super.skip(num);
    }

    @Override
    default PSetX<T> takeRight(final int num) {
        return (PSetX<T>) PersistentCollectionX.super.takeRight(num);
    }

    @Override
    default PSetX<T> dropRight(final int num) {
        return (PSetX<T>) PersistentCollectionX.super.dropRight(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#takeWhile(java.util.function.Predicate)
     */
    @Override
    default PSetX<T> takeWhile(final Predicate<? super T> p) {
        return (PSetX<T>) PersistentCollectionX.super.takeWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#dropWhile(java.util.function.Predicate)
     */
    @Override
    default PSetX<T> dropWhile(final Predicate<? super T> p) {
        return (PSetX<T>) PersistentCollectionX.super.dropWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#takeUntil(java.util.function.Predicate)
     */
    @Override
    default PSetX<T> takeUntil(final Predicate<? super T> p) {
        return (PSetX<T>) PersistentCollectionX.super.takeUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#dropUntil(java.util.function.Predicate)
     */
    @Override
    default PSetX<T> dropUntil(final Predicate<? super T> p) {
        return (PSetX<T>) PersistentCollectionX.super.dropUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#trampoline(java.util.function.Function)
     */
    @Override
    default <R> PSetX<R> trampoline(final Function<? super T, ? extends Trampoline<? extends R>> mapper) {
        return (PSetX<R>) PersistentCollectionX.super.trampoline(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#slice(long, long)
     */
    @Override
    default PSetX<T> slice(final long from, final long to) {
        return (PSetX<T>) PersistentCollectionX.super.slice(from, to);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#sorted(java.util.function.Function)
     */
    @Override
    default <U extends Comparable<? super U>> PSetX<T> sorted(final Function<? super T, ? extends U> function) {
        return (PSetX<T>) PersistentCollectionX.super.sorted(function);
    }

    @Override
    default PSetX<ListX<T>> grouped(final int groupSize) {
        return (PSetX<ListX<T>>) PersistentCollectionX.super.grouped(groupSize);
    }

    @Override
    default <K, A, D> PSetX<Tuple2<K, D>> grouped(final Function<? super T, ? extends K> classifier, final Collector<? super T, A, D> downstream) {
        return (PSetX) PersistentCollectionX.super.grouped(classifier, downstream);
    }

    @Override
    default <K> PSetX<Tuple2<K, ReactiveSeq<T>>> grouped(final Function<? super T, ? extends K> classifier) {
        return (PSetX) PersistentCollectionX.super.grouped(classifier);
    }

    @Override
    default <U> PSetX<Tuple2<T, U>> zip(final Iterable<? extends U> other) {
        return (PSetX) PersistentCollectionX.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#zip(java.lang.Iterable, java.util.function.BiFunction)
     */
    @Override
    default <U, R> PSetX<R> zip(final Iterable<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (PSetX<R>) PersistentCollectionX.super.zip(other, zipper);
    }


    @Override
    default <U, R> PSetX<R> zipS(final Stream<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (PSetX<R>) PersistentCollectionX.super.zipS(other, zipper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#permutations()
     */
    @Override
    default PSetX<ReactiveSeq<T>> permutations() {

        return (PSetX<ReactiveSeq<T>>) PersistentCollectionX.super.permutations();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#combinations(int)
     */
    @Override
    default PSetX<ReactiveSeq<T>> combinations(final int size) {

        return (PSetX<ReactiveSeq<T>>) PersistentCollectionX.super.combinations(size);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#combinations()
     */
    @Override
    default PSetX<ReactiveSeq<T>> combinations() {

        return (PSetX<ReactiveSeq<T>>) PersistentCollectionX.super.combinations();
    }

    @Override
    default PSetX<PVectorX<T>> sliding(final int windowSize) {
        return (PSetX<PVectorX<T>>) PersistentCollectionX.super.sliding(windowSize);
    }

    @Override
    default PSetX<PVectorX<T>> sliding(final int windowSize, final int increment) {
        return (PSetX<PVectorX<T>>) PersistentCollectionX.super.sliding(windowSize, increment);
    }

    @Override
    default PSetX<T> scanLeft(final Monoid<T> monoid) {
        return (PSetX<T>) PersistentCollectionX.super.scanLeft(monoid);
    }

    @Override
    default <U> PSetX<U> scanLeft(final U seed, final BiFunction<? super U, ? super T, ? extends U> function) {
        return (PSetX<U>) PersistentCollectionX.super.scanLeft(seed, function);
    }

    @Override
    default PSetX<T> scanRight(final Monoid<T> monoid) {
        return (PSetX<T>) PersistentCollectionX.super.scanRight(monoid);
    }

    @Override
    default <U> PSetX<U> scanRight(final U identity, final BiFunction<? super T, ? super U, ? extends U> combiner) {
        return (PSetX<U>) PersistentCollectionX.super.scanRight(identity, combiner);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#plusInOrder(java.lang.Object)
     */
    @Override
    default PSetX<T> plusInOrder(final T e) {

        return (PSetX<T>) PersistentCollectionX.super.plusInOrder(e);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#cycle(int)
     */
    @Override
    default PStackX<T> cycle(final long times) {

        return this.stream()
                   .cycle(times)
                   .toPStackX();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#cycle(com.aol.cyclops2.sequence.Monoid, int)
     */
    @Override
    default PStackX<T> cycle(final Monoid<T> m, final long times) {

        return this.stream()
                   .cycle(m, times)
                   .toPStackX();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#cycleWhile(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> cycleWhile(final Predicate<? super T> predicate) {

        return this.stream()
                   .cycleWhile(predicate)
                   .toPStackX();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.standard.MutableCollectionX#cycleUntil(java.util.function.Predicate)
     */
    @Override
    default PStackX<T> cycleUntil(final Predicate<? super T> predicate) {

        return this.stream()
                   .cycleUntil(predicate)
                   .toPStackX();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#zip(java.util.reactiveStream.Stream)
     */
    @Override
    default <U> PSetX<Tuple2<T, U>> zipS(final Stream<? extends U> other) {
        return (PSetX) PersistentCollectionX.super.zipS(other);
    }


    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#zip3(java.util.reactiveStream.Stream, java.util.reactiveStream.Stream)
     */
    @Override
    default <S, U> PSetX<Tuple3<T, S, U>> zip3(final Iterable<? extends S> second, final Iterable<? extends U> third) {

        return (PSetX) PersistentCollectionX.super.zip3(second, third);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#zip4(java.util.reactiveStream.Stream, java.util.reactiveStream.Stream, java.util.reactiveStream.Stream)
     */
    @Override
    default <T2, T3, T4> PSetX<Tuple4<T, T2, T3, T4>> zip4(final Iterable<? extends T2> second, final Iterable<? extends T3> third,
            final Iterable<? extends T4> fourth) {

        return (PSetX) PersistentCollectionX.super.zip4(second, third, fourth);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#zipWithIndex()
     */
    @Override
    default PSetX<Tuple2<T, Long>> zipWithIndex() {

        return (PSetX<Tuple2<T, Long>>) PersistentCollectionX.super.zipWithIndex();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#distinct()
     */
    @Override
    default PSetX<T> distinct() {

        return (PSetX<T>) PersistentCollectionX.super.distinct();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#sorted()
     */
    @Override
    default PSetX<T> sorted() {

        return (PSetX<T>) PersistentCollectionX.super.sorted();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#sorted(java.util.Comparator)
     */
    @Override
    default PSetX<T> sorted(final Comparator<? super T> c) {

        return (PSetX<T>) PersistentCollectionX.super.sorted(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#skipWhile(java.util.function.Predicate)
     */
    @Override
    default PSetX<T> skipWhile(final Predicate<? super T> p) {

        return (PSetX<T>) PersistentCollectionX.super.skipWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#skipUntil(java.util.function.Predicate)
     */
    @Override
    default PSetX<T> skipUntil(final Predicate<? super T> p) {

        return (PSetX<T>) PersistentCollectionX.super.skipUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#limitWhile(java.util.function.Predicate)
     */
    @Override
    default PSetX<T> limitWhile(final Predicate<? super T> p) {

        return (PSetX<T>) PersistentCollectionX.super.limitWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#limitUntil(java.util.function.Predicate)
     */
    @Override
    default PSetX<T> limitUntil(final Predicate<? super T> p) {

        return (PSetX<T>) PersistentCollectionX.super.limitUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#intersperse(java.lang.Object)
     */
    @Override
    default PSetX<T> intersperse(final T value) {

        return (PSetX<T>) PersistentCollectionX.super.intersperse(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#shuffle()
     */
    @Override
    default PSetX<T> shuffle() {

        return (PSetX<T>) PersistentCollectionX.super.shuffle();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#skipLast(int)
     */
    @Override
    default PSetX<T> skipLast(final int num) {

        return (PSetX<T>) PersistentCollectionX.super.skipLast(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#limitLast(int)
     */
    @Override
    default PSetX<T> limitLast(final int num) {

        return (PSetX<T>) PersistentCollectionX.super.limitLast(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.types.OnEmptySwitch#onEmptySwitch(java.util.function.Supplier)
     */
    @Override
    default PSetX<T> onEmptySwitch(final Supplier<? extends PSet<T>> supplier) {
        if (this.isEmpty())
            return PSetX.fromIterable(supplier.get());
        return this;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#onEmpty(java.lang.Object)
     */
    @Override
    default PSetX<T> onEmpty(final T value) {

        return (PSetX<T>) PersistentCollectionX.super.onEmpty(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#onEmptyGet(java.util.function.Supplier)
     */
    @Override
    default PSetX<T> onEmptyGet(final Supplier<? extends T> supplier) {

        return (PSetX<T>) PersistentCollectionX.super.onEmptyGet(supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#onEmptyThrow(java.util.function.Supplier)
     */
    @Override
    default <X extends Throwable> PSetX<T> onEmptyThrow(final Supplier<? extends X> supplier) {

        return (PSetX<T>) PersistentCollectionX.super.onEmptyThrow(supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#shuffle(java.util.Random)
     */
    @Override
    default PSetX<T> shuffle(final Random random) {

        return (PSetX<T>) PersistentCollectionX.super.shuffle(random);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#ofType(java.lang.Class)
     */
    @Override
    default <U> PSetX<U> ofType(final Class<? extends U> type) {

        return (PSetX<U>) PersistentCollectionX.super.ofType(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#filterNot(java.util.function.Predicate)
     */
    @Override
    default PSetX<T> filterNot(final Predicate<? super T> fn) {

        return (PSetX<T>) PersistentCollectionX.super.filterNot(fn);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#notNull()
     */
    @Override
    default PSetX<T> notNull() {

        return (PSetX<T>) PersistentCollectionX.super.notNull();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#removeAllS(java.util.reactiveStream.Stream)
     */
    @Override
    default PSetX<T> removeAllS(final Stream<? extends T> stream) {

        return (PSetX<T>) PersistentCollectionX.super.removeAllS(stream);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#removeAllS(java.lang.Iterable)
     */
    @Override
    default PSetX<T> removeAllS(final Iterable<? extends T> it) {

        return (PSetX<T>) PersistentCollectionX.super.removeAllS(it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#removeAllS(java.lang.Object[])
     */
    @Override
    default PSetX<T> removeAllS(final T... values) {

        return (PSetX<T>) PersistentCollectionX.super.removeAllS(values);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#retainAllS(java.lang.Iterable)
     */
    @Override
    default PSetX<T> retainAllS(final Iterable<? extends T> it) {

        return (PSetX<T>) PersistentCollectionX.super.retainAllS(it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#retainAllS(java.util.reactiveStream.Stream)
     */
    @Override
    default PSetX<T> retainAllS(final Stream<? extends T> seq) {

        return (PSetX<T>) PersistentCollectionX.super.retainAllS(seq);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#retainAllS(java.lang.Object[])
     */
    @Override
    default PSetX<T> retainAllS(final T... values) {

        return (PSetX<T>) PersistentCollectionX.super.retainAllS(values);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops2.collections.extensions.persistent.PersistentCollectionX#cast(java.lang.Class)
     */
    @Override
    default <U> PSetX<U> cast(final Class<? extends U> type) {

        return (PSetX<U>) PersistentCollectionX.super.cast(type);
    }



    @Override
    default <C extends Collection<? super T>> PSetX<C> grouped(final int size, final Supplier<C> supplier) {

        return (PSetX<C>) PersistentCollectionX.super.grouped(size, supplier);
    }

    @Override
    default PSetX<ListX<T>> groupedUntil(final Predicate<? super T> predicate) {

        return (PSetX<ListX<T>>) PersistentCollectionX.super.groupedUntil(predicate);
    }

    @Override
    default PSetX<ListX<T>> groupedStatefullyUntil(final BiPredicate<ListX<? super T>, ? super T> predicate) {

        return (PSetX<ListX<T>>) PersistentCollectionX.super.groupedStatefullyUntil(predicate);
    }

    @Override
    default PSetX<ListX<T>> groupedWhile(final Predicate<? super T> predicate) {

        return (PSetX<ListX<T>>) PersistentCollectionX.super.groupedWhile(predicate);
    }

    @Override
    default <C extends Collection<? super T>> PSetX<C> groupedWhile(final Predicate<? super T> predicate, final Supplier<C> factory) {

        return (PSetX<C>) PersistentCollectionX.super.groupedWhile(predicate, factory);
    }

    @Override
    default <C extends Collection<? super T>> PSetX<C> groupedUntil(final Predicate<? super T> predicate, final Supplier<C> factory) {

        return (PSetX<C>) PersistentCollectionX.super.groupedUntil(predicate, factory);
    }

    @Override
    default <R> PSetX<R> retry(final Function<? super T, ? extends R> fn) {
        return (PSetX<R>)PersistentCollectionX.super.retry(fn);
    }

    @Override
    default <R> PSetX<R> retry(final Function<? super T, ? extends R> fn, final int retries, final long delay, final TimeUnit timeUnit) {
        return (PSetX<R>)PersistentCollectionX.super.retry(fn);
    }

    @Override
    default <R> PSetX<R> flatMapS(Function<? super T, ? extends Stream<? extends R>> fn) {
        return (PSetX<R>)PersistentCollectionX.super.flatMapS(fn);
    }

    @Override
    default <R> PSetX<R> flatMapP(Function<? super T, ? extends Publisher<? extends R>> fn) {
        return (PSetX<R>)PersistentCollectionX.super.flatMapP(fn);
    }

    @Override
    default PSetX<T> prependS(Stream<? extends T> stream) {
        return (PSetX<T>)PersistentCollectionX.super.prependS(stream);
    }

    @Override
    default PSetX<T> append(T... values) {
        return (PSetX<T>)PersistentCollectionX.super.append(values);
    }

    @Override
    default PSetX<T> append(T value) {
        return (PSetX<T>)PersistentCollectionX.super.append(value);
    }

    @Override
    default PSetX<T> prepend(T value) {
        return (PSetX<T>)PersistentCollectionX.super.prepend(value);
    }

    @Override
    default PSetX<T> prepend(T... values) {
        return (PSetX<T>)PersistentCollectionX.super.prepend(values);
    }

    @Override
    default PSetX<T> insertAt(int pos, T... values) {
        return (PSetX<T>)PersistentCollectionX.super.insertAt(pos,values);
    }

    @Override
    default PSetX<T> deleteBetween(int start, int end) {
        return (PSetX<T>)PersistentCollectionX.super.deleteBetween(start,end);
    }

    @Override
    default PSetX<T> insertAtS(int pos, Stream<T> stream) {
        return (PSetX<T>)PersistentCollectionX.super.insertAtS(pos,stream);
    }

    @Override
    default PSetX<T> recover(final Function<? super Throwable, ? extends T> fn) {
        return (PSetX<T>)PersistentCollectionX.super.recover(fn);
    }

    @Override
    default <EX extends Throwable> PSetX<T> recover(Class<EX> exceptionClass, final Function<? super EX, ? extends T> fn) {
        return (PSetX<T>)PersistentCollectionX.super.recover(exceptionClass,fn);
    }


    @Override
    default PSetX<T> plusLoop(int max, IntFunction<T> value) {
        return (PSetX<T>)PersistentCollectionX.super.plusLoop(max,value);
    }

    @Override
    default PSetX<T> plusLoop(Supplier<Optional<T>> supplier) {
        return (PSetX<T>)PersistentCollectionX.super.plusLoop(supplier);
    }
}
