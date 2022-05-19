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
	public void forwardPayload() throws Exception {
		Event event = flowRunner("loop-1000-payload").run();
		Integer payload = (Integer) event.getMessage().getPayload().getValue();
		assertEquals(Integer.valueOf(42 + 2000), payload);
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

}
