package com.sap.sse.common.scalablevalue;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * In a sequence of {@link ComparableScalableValueWithDistance} objects, tells the contiguous sub-sequence with the
 * greatest and the contiguous sub-sequence with the least sum, according to the
 * {@link ComparableScalableValueWithDistance#add(ScalableValue) add} and the
 * {@link ComparableScalableValueWithDistance#compareTo(Object) compareTo} methods.
 * <p>
 * 
 * The sequence is mutable. In particular, elements can be added at any position, also before the start or after the
 * end, and elements can be removed at least from the beginning of the sequence. Updating the sub-sequences with minimal
 * and maximal sums happens with complexity O(1) when adding to the end of the sequence, so with constant effort
 * regardless the size of the sequence. When inserting into or removing from the sequence at arbitrary positions,
 * constant effort can no longer be guaranteed as changes may need to get propagated onwards to following elements.
 * <p>
 * 
 * See also <a href="https://en.wikipedia.org/wiki/Maximum_subarray_problem">here</a> for a description of the
 * algorithm.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class KadaneExtremeSubarraysFinder<ValueType, AveragesTo extends Comparable<AveragesTo>, T extends ComparableScalableValueWithDistance<ValueType, AveragesTo>>
implements Serializable, Iterable<T> {
    private static final long serialVersionUID = 2109193559337714286L;
    
    /**
     * The elements constituting the full sequence in which to find the contiguous sub-sequences
     */
    private final List<T> sequence;
    
    /**
     * The element at index <tt>i</tt> holds the maximum value of the sum of any contiguous sub-sequence ending at index
     * <tt>i</tt>. Outside of {@code synchronized} blocks it holds as many elements as {@link #sequence}. The element
     * at index {@code i} is computed as {@code maxSumEndingAt.get(i-1)+sequence.get(i), max(sequence.get(i))}. This covers
     * the two cases extending the complete induction. Either, the sequence with the maximum sum ending at index {@code i}
     * includes prior elements; or the single element {@code sequence.get(i)} is greater than the sum of it and the maximum
     * sum ending at the previous element {@code i-1}.
     */
    private final List<ScalableValueWithDistance<ValueType, AveragesTo>> maxSumEndingAt;
    
    /**
     * The maximum of the sums of any contiguous sub-sequence
     */
    private ScalableValueWithDistance<ValueType, AveragesTo> maxSum;
    
    /**
     * Index of the first element in {@link #sequence} of the contiguous sub-sequence having the maximum sum
     */
    private int startIndexInclusiveOfMaxSumSequence;
    
    /**
     * Index of the element after the last element in {@link #sequence} of the contiguous sub-sequence having the maximum sum
     */
    private int endIndexExclusiveOfMaxSumSequence;
    
    /**
     * See {@code #maxSumEndingAt}, only for the minimum.
     */
    private final List<ScalableValueWithDistance<ValueType, AveragesTo>> minSumEndingAt;
    
    private ScalableValueWithDistance<ValueType, AveragesTo> minSum;
    
    /**
     * Index of the first element in {@link #sequence} of the contiguous sub-sequence having the minium sum
     */
    private int startIndexInclusiveOfMinSumSequence;
    
    /**
     * Index of the element after the last element in {@link #sequence} of the contiguous sub-sequence having the minimum sum
     */
    private int endIndexExclusiveOfMinSumSequence;
    
    public KadaneExtremeSubarraysFinder() {
        sequence = new LinkedList<>();
        maxSumEndingAt = new LinkedList<>();
        minSumEndingAt = new LinkedList<>();
        maxSum = null;
        minSum = null;
        startIndexInclusiveOfMaxSumSequence = -1;
        endIndexExclusiveOfMaxSumSequence = -1;
        startIndexInclusiveOfMinSumSequence = -1;
        endIndexExclusiveOfMinSumSequence = -1;
    }

    public synchronized void add(int index, T t) {
        sequence.add(index, t);
        final ScalableValueWithDistance<ValueType, AveragesTo> newMaxSumEndingAtIndex;
        final ScalableValueWithDistance<ValueType, AveragesTo> sum = index == 0 ? null : t.add(maxSumEndingAt.get(index-1));
        if (index == 0 || compare(t, sum) >= 0) {
            newMaxSumEndingAtIndex = t; // one-element sum consisting of element at "index" is the maximum
        } else {
            newMaxSumEndingAtIndex = sum;
        }
        maxSumEndingAt.add(index, newMaxSumEndingAtIndex);
        update(index+1, newMaxSumEndingAtIndex);
    }

    private int compare(final ScalableValueWithDistance<ValueType, AveragesTo> a, final ScalableValueWithDistance<ValueType, AveragesTo> b) {
        return a.divide(1).compareTo(b.divide(1));
    }
    
    /**
     * For each element in {@link #sequence} starting at index {@code i}, this method checks whether the {@link #maxSumEndingAt}{@code [i]}
     * still is the maximum of {@link #maxSumEndingAt}{@code [i-1]+sequence[i]} and {@link #sequence}{@code [i]}. If yes, any change to
     * elements with index less than {@code i} do not have to be carried forward any further. Otherwise, {@link #maxSumEndingAt}{@code [i]}
     * is updated, and the process continues at {@code i+1} "recursively" (implemented iteratively, without recursion).
     * @param newMaxSumEndingAtIndex 
     */
    private void update(int i, ScalableValueWithDistance<ValueType, AveragesTo> maxSumEndingAtPreviousIndex) {
        final ListIterator<T> sequenceIter = sequence.listIterator(i);
        final ListIterator<ScalableValueWithDistance<ValueType, AveragesTo>> maxSumEndingAtIter = maxSumEndingAt.listIterator(i);
        boolean finished = false;
        while (sequenceIter.hasNext() && !finished) {
            final T next = sequenceIter.next();
            final ScalableValueWithDistance<ValueType, AveragesTo> nextMaxSumEndingAt = maxSumEndingAtIter.next();
            final ScalableValueWithDistance<ValueType, AveragesTo> sum = next.add(maxSumEndingAtPreviousIndex);
            final ScalableValueWithDistance<ValueType, AveragesTo> newMaxSumEndingAt = compare(next, sum) >= 0 ?
                    next : sum;
            if (compare(nextMaxSumEndingAt, newMaxSumEndingAt) != 0) {
                maxSumEndingAtIter.remove();
                maxSumEndingAtIter.add(newMaxSumEndingAt);
                maxSumEndingAtPreviousIndex = newMaxSumEndingAt;
            } else {
                finished = true; // no more changes to propagate
            }
        }
    }

    public synchronized void remove(int index) {
        sequence.remove(index);
        final ScalableValueWithDistance<ValueType, AveragesTo> maxSumEndingAtIndex = maxSumEndingAt.remove(index);
        update(index+1, maxSumEndingAtIndex);
    }
    
    public synchronized void add(T t) {
        add(sequence.size(), t);
    }
    
    public synchronized void remove(T t) {
        remove(sequence.indexOf(t));
    }
    
    public ScalableValueWithDistance<ValueType, AveragesTo> getMaxSum() {
        return maxSum;
    }
    
    public ScalableValueWithDistance<ValueType, AveragesTo> getMinSum() {
        return minSum;
    }
    
    public int getStartIndexInclusiveOfMaxSumSequence() {
        return startIndexInclusiveOfMaxSumSequence;
    }

    public int getEndIndexExclusiveOfMaxSumSequence() {
        return endIndexExclusiveOfMaxSumSequence;
    }

    public int getStartIndexInclusiveOfMinSumSequence() {
        return startIndexInclusiveOfMinSumSequence;
    }

    public int getEndIndexExclusiveOfMinSumSequence() {
        return endIndexExclusiveOfMinSumSequence;
    }

    @Override
    public Iterator<T> iterator() {
        return sequence.iterator();
    }
}
