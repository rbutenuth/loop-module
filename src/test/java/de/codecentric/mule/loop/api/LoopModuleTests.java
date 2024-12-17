package de.codecentric.mule.loop.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.event.Event;

public class LoopModuleTests extends MuleArtifactFunctionalTestCase {

	@Override
	protected String getConfigFile() {
		return "test-flows.xml";
	}

	@Test
	public void repeatUntilPayloadImmediatelyNotEmpty() throws Exception {
		Event event = flowRunner("payload-immediately-not-empty").run();
		String payload = (String) event.getMessage().getPayload().getValue();
		assertEquals("foo", payload);
	}

	@Test
	public void repeatUntil100Iterations() throws Exception {
		Event event = flowRunner("repeat-1000-iterations").run();
		String payload = (String) event.getMessage().getPayload().getValue();
		assertEquals("done", payload);
	}

	@Test
	public void repeatUntilEmptyByNull() throws Exception {
		Event event = flowRunner("repeat-empty-by-null").run();
		String payload = (String) event.getMessage().getPayload().getValue();
		assertEquals("done", payload);
	}

	@Test
	public void repeatUntilEmptyByEmptyArray() throws Exception {
		Event event = flowRunner("repeat-empty-by-array").run();
		@SuppressWarnings("unchecked")
		List<Integer> payload = (List<Integer>) event.getMessage().getPayload().getValue();
		assertEquals(1, payload.size());
		assertEquals(Integer.valueOf(42), payload.get(0));
	}

	@Test
	public void repeatUntilEmptyByEmptyObject() throws Exception {
		Event event = flowRunner("repeat-empty-by-object").run();
		@SuppressWarnings("unchecked")
		Map<String, String> payload = (Map<String, String>) event.getMessage().getPayload().getValue();
		assertEquals(1, payload.size());
		assertEquals("bar", payload.get("foo"));
	}

	@Test
	public void repeatUntilErrorInFirstIteration() throws Exception {
		try {
			flowRunner("repeat-error").run();
			fail("should not be reached");
		} catch (Exception e) {
			assertEquals("nothing to repeat...", e.getMessage());
		}
	}

	@Test
	public void emptyFor() throws Exception {
		Event event = flowRunner("empty-for").run();
		String payload = (String) event.getMessage().getPayload().getValue();
		assertNull(payload);
	}

	@Test
	public void noStackOverflowLoop1000() throws Exception {
		Event event = flowRunner("loop-1000-counter").run();
		String payload = (String) event.getMessage().getPayload().getValue();
		assertEquals("foo", payload);
	}

	@Test
	public void forWithPayload() throws Exception {
		Event event = flowRunner("loop-1000-payload").run();
		Integer payload = (Integer) event.getMessage().getPayload().getValue();
		assertEquals(Integer.valueOf(42 + 2000), payload);
	}

	@Test
	public void forWithPayloadOneIteration() throws Exception {
		Event event = flowRunner("loop-1-payload").run();
		Integer payload = (Integer) event.getMessage().getPayload().getValue();
		assertEquals(Integer.valueOf(42 + 2), payload);
	}

	@Test
	public void loopWithCounterAndErrorInFirstIteration() throws Exception {
		try {
			flowRunner("loop-counter-error-in-first-iteration").run();
			fail("should not be reached");
		} catch (Exception e) {
			assertEquals("nothing to describe...", e.getMessage());
		}
	}

	@Test
	public void loopWithCounterAndErrorInSecondIteration() throws Exception {
		try {
			flowRunner("loop-counter-error-in-second-iteration").run();
			fail("should not be reached");
		} catch (Exception e) {
			assertEquals("counter: 1", e.getMessage());
		}
	}

	@Test
	public void loopErrorInFirstIteration() throws Exception {
		try {
			flowRunner("loop-error-in-first-iteration").run();
			fail("should not be reached");
		} catch (Exception e) {
			assertEquals("nothing to describe...", e.getMessage());
		}
	}

	@Test
	public void loopErrorInSecondIteration() throws Exception {
		try {
			flowRunner("loop-error-in-second-iteration").run();
			fail("should not be reached");
		} catch (Exception e) {
			assertEquals("nothing to describe...", e.getMessage());
		}
	}

	@Test
	public void forEach() throws Exception {
		Collection<Integer> values = new ArrayList<>(100);
		for (int i = 0; i < 100; i++) {
			values.add(i);
		}
		Event event = flowRunner("for-each").withPayload(values).run();
		@SuppressWarnings("unchecked")
		List<Integer> payload = (List<Integer>) event.getMessage().getPayload().getValue();
		for (int i = 0; i < 100; i++) {
			assertEquals(Integer.valueOf(i * i), payload.get(i));
		}
	}

	@Test
	public void forEachWithBadType() throws Exception {
		
		try {
			flowRunner("for-each").withPayload("string instead of collection").run();
			fail("should not be reached");
		} catch (Exception e) {
			assertEquals("Can't loop over java.lang.String, only Collection or Iterator are valid options", e.getMessage());
		}
	}

	@Test
	public void forEachWithError() throws Exception {
		Collection<Integer> values = new ArrayList<>(100);
		for (int i = 0; i < 100; i++) {
			values.add(i);
		}
		try {
			flowRunner("for-each-with-error").withPayload(values).run();
			fail("should not be reached");
		} catch (Exception e) {
			assertEquals("nothing to describe...", e.getMessage());
		}
	}

	@Test
	public void forEachStreaming() throws Exception {
		Collection<Integer> values = new ArrayList<>(100);
		for (int i = 0; i < 100; i++) {
			values.add(i);
		}
		Event event = flowRunner("for-each-streaming").withPayload(values.iterator()).run();
		@SuppressWarnings("unchecked")
		Iterator<Integer> payload = (Iterator<Integer>) event.getMessage().getPayload().getValue();
		for (int i = 0; i < 100; i++) {
			assertTrue(payload.hasNext());
			assertEquals(Integer.valueOf(i * i), payload.next());
		}
		assertFalse(payload.hasNext());
	}

	@Test
	public void forEachStreamingWithError() throws Exception {
		Collection<Integer> values = new ArrayList<>(100);
		for (int i = 0; i < 100; i++) {
			values.add(i);
		}
		Event event = flowRunner("for-each-streaming-with-error").withPayload(values.iterator()).run();
		@SuppressWarnings("unchecked")
		Iterator<Integer> payload = (Iterator<Integer>) event.getMessage().getPayload().getValue();
		assertTrue(payload.hasNext());
		try {
			payload.next();
			fail("should not be reached");
		} catch (Throwable e) {
			assertTrue(e instanceof RuntimeException);
			assertEquals("nothing to describe...", e.getCause().getMessage());
		}
	}

	@Test
	public void whileCountDown() throws Exception {
		Event event = flowRunner("while-countdown").run();
		Integer payload = (Integer) event.getMessage().getPayload().getValue();
		assertEquals(Integer.valueOf(-1), payload);
	}

	@Test
	public void whileCountDownAndCollect() throws Exception {
		Event event = flowRunner("while-countdown-and-collect").run();
		@SuppressWarnings("unchecked")
		List<Integer> payload = (List<Integer>) event.getMessage().getPayload().getValue();
		assertEquals(11, payload.size());
		for (int i = 0; i < 11; i++) {
			assertEquals(Integer.valueOf(10 - i), payload.get(i));
		}
	}

	@Test
	public void whileCountDownPayloadBefore() throws Exception {
		Event event = flowRunner("while-countdown-payload-before").run();
		Integer payload = (Integer) event.getMessage().getPayload().getValue();
		assertEquals(Integer.valueOf(5), payload);
	}

	@Test
	public void whileErrorPayloadNull() throws Exception {
		try {
			flowRunner("while-payload-null").run();
			fail("should not be reached");
		} catch (Exception e) {
			assertEquals("Payload should be Map, but is: null", e.getMessage());
		}
	}

	@Test
	public void whileErrorPayloadArray() throws Exception {
		try {
			flowRunner("while-payload-array").run();
			fail("should not be reached");
		} catch (Exception e) {
			assertEquals("Payload should be Map, but is: class java.util.ArrayList", e.getMessage());
		}
	}

	@Test
	public void whileCountDownAndIterate() throws Exception {
		Event event = flowRunner("while-countdown-and-iterate").run();
		@SuppressWarnings("unchecked")
		Iterator<Integer> payload = (Iterator<Integer>) event.getMessage().getPayload().getValue();
		for (int i = 0; i < 11; i++) {
			assertTrue(payload.hasNext());
			assertEquals(Integer.valueOf(10 - i), payload.next());
		}
		assertFalse(payload.hasNext());
	}

	@Test
	public void whileStreamingError() throws Exception {
		try {
			Event event = flowRunner("while-streaming-error").run();
			@SuppressWarnings("unchecked")
			Iterator<Integer> payload = (Iterator<Integer>) event.getMessage().getPayload().getValue();
			assertTrue(payload.hasNext());
			payload.next();
			fail("should not be reached");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Bumm!"));
			assertTrue(e.getMessage().contains("MY_NAMESPACE:MY_IDENTIFIER"));
		}
	}
}
