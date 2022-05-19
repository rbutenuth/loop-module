package de.codecentric.mule.loop.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.event.Event;

public class LoopModuleTests  extends MuleArtifactFunctionalTestCase {

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
	public void repeatUntilErrorInFirstIteration() throws Exception {
		try {
			flowRunner("repeat-error").run();
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
		} catch (Exception e) {
			assertEquals("nothing to describe...", e.getMessage());
		}
	}

	@Test
	public void loopWithCounterAndErrorInSecondIteration() throws Exception {
		try {
			flowRunner("loop-counter-error-in-second-iteration").run();
		} catch (Exception e) {
			assertEquals("counter: 1.", e.getMessage());
		}
	}

	@Test
	public void loopErrorInFirstIteration() throws Exception {
		try {
			flowRunner("loop-error-in-first-iteration").run();
		} catch (Exception e) {
			assertEquals("nothing to describe...", e.getMessage());
		}
	}

	@Test
	public void loopErrorInSecondIteration() throws Exception {
		try {
			flowRunner("loop-error-in-second-iteration").run();
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
	public void forEachWithError() throws Exception {
		Collection<Integer> values = new ArrayList<>(100);
		for (int i = 0; i < 100; i++) {
			values.add(i);
		}
		try {
			flowRunner("for-each-with-error").withPayload(values).run();
		} catch (Exception e) {
			assertEquals("nothing to describe...", e.getMessage());
		}
	}

}
