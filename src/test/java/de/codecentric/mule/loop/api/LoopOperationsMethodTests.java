package de.codecentric.mule.loop.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;

public class LoopOperationsMethodTests {
	private LoopOperations operations = new LoopOperations();
	
	@Test
	public void conditionNullIsFalse() throws Exception {
		assertFalse(operations.evaluateCondition(null));
	}

	@Test
	public void conditionFalseIsFalse() throws Exception {
		assertFalse(operations.evaluateCondition(Boolean.FALSE));
	}

	@Test
	public void conditionTrueIsTrue() throws Exception {
		assertTrue(operations.evaluateCondition(Boolean.TRUE));
	}

	@Test
	public void conditionEmptyIsFalse() throws Exception {
		assertFalse(operations.evaluateCondition(""));
	}

	@Test
	public void conditionNotEmptyIsTrue() throws Exception {
		assertTrue(operations.evaluateCondition("foo"));
	}

//	@Test
	public void conditionWithCursorProviderIsTrue() throws Exception {
		CursorProvider<Cursor> cp = new CursorProvider<Cursor>() {
			@Override
			public void releaseResources() {
			}
			
			@Override
			public Cursor openCursor() {
				return null;
			}
			
			@Override
			public boolean isClosed() {
				return false;
			}
			
			@Override
			public void close() {
			}
		};
		assertTrue(operations.evaluateCondition(cp));
	}
}
