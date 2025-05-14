package de.codecentric.mule.loop.api;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A wrapper around an {@link Iterator}. All elements - except the last one - are returned.
 * In case the original {@link Iterator} is empty, this one is empty, too. 
 * @param <E> Type of the elements returned.
 */
public class RemoveLastIterator<E> implements Iterator<E> {
	private Iterator<E> inner;
	private boolean haveFetched;
	private boolean haveNextValue;
	private E nextValue;

	public RemoveLastIterator(Iterator<E> inner) {
		this.inner = inner;
		haveFetched = false;
		haveNextValue = false;
	}
	
	@Override
	public boolean hasNext() {
		if (!haveFetched) {
			fetch();
			haveFetched = true;
		}
		return haveNextValue && inner.hasNext();
	}

	@Override
	public E next() {
		if (hasNext()) {
			E result = nextValue;
			fetch();
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	private void fetch() {
		if (inner.hasNext()) {
			haveNextValue = true;
			nextValue = inner.next();
		} else {
			haveNextValue = false;
			nextValue = null;
		}
	}
}
