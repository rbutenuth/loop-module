package de.codecentric.mule.loop.api;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

public class RemoveLastIteratorTest {

	@Test
	public void emptyIterator() {
		emptyIterator(true);
	}

	@Test
	public void emptyIteratorNextOnly() {
		emptyIterator(false);
	}

	public void emptyIterator(boolean withHasNextCheck) {
		Iterator<Integer> iter = create(0);
		if (withHasNextCheck) {
			assertFalse(iter.hasNext());
		}
		try {
			iter.next();
			fail("Exception missing");
		} catch (NoSuchElementException e) {
			// Expected
		}
	}
	
	@Test
	public void oneElement() {
		oneElement(true);
	}

	@Test
	public void oneElementNextOnly() {
		oneElement(false);
	}

	private void oneElement(boolean withHasNextCheck) {
		Iterator<Integer> iter = create(1);
		if (withHasNextCheck) {
			assertFalse(iter.hasNext());
		}
		try {
			iter.next();
			fail("Exception missing");
		} catch (NoSuchElementException e) {
			// Expected
		}
	}

	@Test
	public void twoElements() {
		twoElements(true);
	}

	@Test
	public void twoElementsNextOnly() {
		twoElements(false);
	}

	private void twoElements(boolean withHasNextCheck) {
		Iterator<Integer> iter = create(2);
		if (withHasNextCheck) {
			assertTrue(iter.hasNext());
		}
		assertEquals(Integer.valueOf(0), iter.next());
		try {
			iter.next();
			fail("Exception missing");
		} catch (NoSuchElementException e) {
			// Expected
		}
	}

	@Test
	public void tenElements() {
		tenElements(true);
	}

	@Test
	public void tenElementsNextOnly() {
		tenElements(false);
	}

	private void tenElements(boolean withHasNextCheck) {
		Iterator<Integer> iter = create(10);
		for (int i = 0; i < 9; i++) {
			if (withHasNextCheck) {
				assertTrue(iter.hasNext());
			}
			assertEquals(Integer.valueOf(i), iter.next());
		}
		if (withHasNextCheck) {
			assertFalse(iter.hasNext());
		}
		try {
			iter.next();
			fail("Exception missing");
		} catch (NoSuchElementException e) {
			// Expected
		}
	}

	private Iterator<Integer> create(int count) {
		List<Integer> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			list.add(i);
		}
		return new RemoveLastIterator<Integer>(list.iterator());
	}
}
