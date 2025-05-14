package de.codecentric.mule.loop.api;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * A wrapper around an {@link Iterator}. All elements - except the last one - are returned.
 * In case the original {@link Iterator} is empty, this one is empty, too. 
 * @param <E> Type of the elements returned.
 */
public class RemoveLastIterator<E> implements Iterator<E> {
	private Iterator<E> inner;
	private Optional<E> nextValue;

	public RemoveLastIterator(Iterator<E> inner) {
		this.inner = inner;
		// fetch lazy on first call of hasNext() or next(), so nextValue is still null.
	}
	
	@Override
	public boolean hasNext() {
		if (nextValue == null) {
			fetch();
		}
		return nextValue.isPresent() && inner.hasNext();
	}

	@Override
	public E next() {
		if (hasNext()) {
			E result = nextValue.get();
			fetch();
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	private void fetch() {
		if (inner.hasNext()) {
			nextValue = Optional.of(inner.next());
		} else {
			nextValue = Optional.empty();
		}
	}
}
