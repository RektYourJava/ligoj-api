package org.ligoj.app;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.bootstrap.core.SpringUtils;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

/**
 * Test class of {@link AbstractAppTest}
 */
public class TestAbstractJpaTest extends AbstractAppTest {

	/**
	 * Only there fore coverage, no Spring involved.
	 */
	@Test
	public void coverage() {
		iamProvider = Mockito.mock(IamProvider.class);
		IamConfiguration configuration = Mockito.mock(IamConfiguration.class);
		Mockito.when(iamProvider.getConfiguration()).thenReturn(configuration);
		em = Mockito.mock(EntityManager.class);
		getUser();
		getCompany();
		getGroup();
		persistSystemEntities();
	}

	/**
	 * Restore original Spring application context.<br>
	 * TODO Remove this with LB 1.6.1, see ligoj/bootstrap#4
	 */
	@Test
	public void testRestoreAppalicationContextNull() {
		ApplicationContext context = SpringUtils.getApplicationContext();
		applicationContext = null;
		restoreAppalicationContext();
		Assert.assertSame(context, SpringUtils.getApplicationContext());
	}

	/**
	 * Restore original Spring application context.<br>
	 * TODO Remove this with LB 1.6.1, see ligoj/bootstrap#4
	 */
	@Test
	public void testRestoreAppalicationContextNotNull() {
		final ApplicationContext context = SpringUtils.getApplicationContext();
		try {
			applicationContext = Mockito.mock(ApplicationContext.class);
			restoreAppalicationContext();
			Assert.assertSame(applicationContext, SpringUtils.getApplicationContext());
		} finally {
			// Really restore
			SpringUtils.setSharedApplicationContext(context);
		}
	}

}
