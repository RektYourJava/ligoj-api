package org.ligoj.app.resource.security;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.api.GroupOrg;

/**
 * Test class of {@link EmptyGroupRepository}
 */
public class EmptyGroupRepositoryTest {

	@Test
	public void findAll() {
		Assert.assertTrue(new EmptyGroupRepository().findAll().isEmpty());
	}

	@Test
	public void delete() {
		new EmptyGroupRepository().delete(null);
	}

	@Test
	public void findByDepartment() {
		Assert.assertNull(new EmptyGroupRepository().findByDepartment("any"));
	}

	@Test
	public void getTypeName() {
		Assert.assertEquals("group", new EmptyGroupRepository().getTypeName());
	}

	@Test
	public void create() {
		final GroupOrg groupLdap = new EmptyGroupRepository().create("Cn=Some", "Name");
		Assert.assertEquals("Cn=Some", groupLdap.getDn());
		Assert.assertEquals("Name", groupLdap.getName());
		Assert.assertEquals("name", groupLdap.getId());
	}

}
