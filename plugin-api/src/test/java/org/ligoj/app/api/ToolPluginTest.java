/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link ToolPlugin}
 */
public class ToolPluginTest {

	private final ToolPlugin plugin = new ToolPlugin() {

		@Override
		public String getKey() {
			return "service:s1:t2";
		}

	};

	@Test
	public void checkStatus() throws Exception {
		Assertions.assertTrue(plugin.checkStatus(null));
	}

	@Test
	public void checkStatusNode() throws Exception {
		Assertions.assertTrue(plugin.checkStatus(null, null));
	}

	@Test
	public void checkSubscriptionStatus() throws Exception {
		final SubscriptionStatusWithData data = plugin.checkSubscriptionStatus(0, null, null);
		data.put("some", "value");
		Assertions.assertNotNull(data.getStatus().isUp());
		Assertions.assertEquals(1, data.getData().size());
		Assertions.assertEquals("value", data.getData().get("some"));
	}

	@Test
	public void getLastVersion() throws Exception {
		Assertions.assertNull(plugin.getLastVersion());
	}

	@Test
	public void getVersion() throws Exception {
		Assertions.assertNull(plugin.getVersion(null));
	}
}
