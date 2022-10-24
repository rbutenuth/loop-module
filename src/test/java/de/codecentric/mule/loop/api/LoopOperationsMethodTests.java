package de.codecentric.mule.loop.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LoopOperationsMethodTests {

	@Test
	public void conditionNullIsFalse() throws Exception {
		assertFalse(LoopOperations.evaluateCondition(null));
	}

	@Test
	public void conditionFalseIsFalse() throws Exception {
		assertFalse(LoopOperations.evaluateCondition(Boolean.FALSE));
	}

	@Test
	public void conditionTrueIsTrue() throws Exception {
		assertTrue(LoopOperations.evaluateCondition(Boolean.TRUE));
	}

	@Test
	public void conditionEmptyIsFalse() throws Exception {
		assertFalse(LoopOperations.evaluateCondition(""));
	}

	@Test
	public void conditionNotEmptyIsTrue() throws Exception {
		assertTrue(LoopOperations.evaluateCondition("foo"));
	}
}
