package de.codecentric.mule.loop.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.event.Event;

public class LoopModuleTests  extends MuleArtifactFunctionalTestCase {

	@Override
	protected String getConfigFile() {
		return "test-flows.xml";
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
		assertEquals(Integer.valueOf(2000), payload);
	}

}
