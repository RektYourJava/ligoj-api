/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.api.NodeVo;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.resource.AbstractOrgTest;
import org.ligoj.app.resource.subscription.SubscriptionVo;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link ProjectResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class ProjectResourceTest extends AbstractOrgTest {

	private ProjectResource resource;

	@Autowired
	private ProjectRepository repository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private NodeRepository nodeRepository;

	private Project testProject;

	@BeforeEach
	public void setUpEntities2() {
		resource = new ProjectResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		resource.iamProvider = new IamProvider[] { iamProvider };
		testProject = repository.findByName("MDA");

		// Ensure LDAP cache is loaded
		em.flush();
		em.clear();

		// For coverage issue with JPA
		new Project().setSubscriptions(null);
	}

	@Test
	public void findAll() {
		// create a mock URI info with pagination information
		final UriInfo uriInfo = newFindAllParameters();
		initSpringSecurityContext("fdaugan");
		final TableItem<ProjectLightVo> result = resource.findAll(uriInfo, null, 0);
		Assertions.assertEquals(2, result.getData().size());

		final ProjectLightVo project = result.getData().get(0);
		checkProjectMDA(project);

		Assertions.assertEquals("gStack", result.getData().get(1).getName());

		// KPI, Build, Bug Tracker, Identity x2, KM
		Assertions.assertTrue(result.getData().get(1).getNbSubscriptions() >= 6);
	}

	@Test
	public void findAllNotMemberButDelegateGroupVisible() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setReceiver("user");
		delegate.setDn("cn=gfi-gstack,ou=gfi,ou=project,dc=sample,dc=com");
		delegate.setName("gfi-gStack");
		em.persist(delegate);
		em.flush();
		em.clear();

		// create a mock URI info with pagination information
		final UriInfo uriInfo = newFindAllParameters();
		initSpringSecurityContext("user");
		final TableItem<ProjectLightVo> result = resource.findAll(uriInfo, "gStack", 0);
		Assertions.assertEquals(1, result.getData().size());

		Assertions.assertEquals("gStack", result.getData().get(0).getName());

		// KPI, Build, Bug Tracker, Identity x2, KM
		Assertions.assertTrue(result.getData().get(0).getNbSubscriptions() >= 6);
	}

	@Test
	public void findAllNotMemberButTreeVisible() {
		// create a mock URI info with pagination information
		final UriInfo uriInfo = newFindAllParameters();

		final TableItem<ProjectLightVo> result = resource.findAll(uriInfo, null, 0);
		Assertions.assertEquals(1, result.getData().size());
		Assertions.assertEquals("gStack", result.getData().get(0).getName());

		// KPI, Build, Bug Tracker, Identity x2, KM
		Assertions.assertTrue(result.getData().get(0).getNbSubscriptions() >= 6);
	}

	@Test
	public void findAllNotVisible() {
		// create a mock URI info with pagination information
		final UriInfo uriInfo = newFindAllParameters();

		initSpringSecurityContext("any");
		final TableItem<ProjectLightVo> result = resource.findAll(uriInfo, "MDA", 0);
		Assertions.assertEquals(0, result.getData().size());
	}

	@Test
	public void findAllTeamLeader() {
		// create a mock URI info with pagination information
		final UriInfo uriInfo = newFindAllParameters();

		initSpringSecurityContext("fdaugan");
		final TableItem<ProjectLightVo> result = resource.findAll(uriInfo, "mdA", 0);
		Assertions.assertEquals(1, result.getData().size());

		final ProjectLightVo project = result.getData().get(0);
		checkProjectMDA(project);
	}

	@Test
	public void findAllMember() {
		// create a mock URI info with pagination information
		final UriInfo uriInfo = newFindAllParameters();

		initSpringSecurityContext("alongchu");
		final TableItem<ProjectLightVo> result = resource.findAll(uriInfo, "gStack", 0);
		Assertions.assertEquals(1, result.getData().size());

		final ProjectLightVo project = result.getData().get(0);
		Assertions.assertEquals("gStack", project.getName());
	}

	private void checkProjectMDA(final ProjectLightVo project) {
		Assertions.assertEquals("MDA", project.getName());
		Assertions.assertEquals("Model Driven Architecture implementation of Gfi", project.getDescription());
		Assertions.assertEquals("mda", project.getPkey());
		Assertions.assertNotNull(project.getCreatedDate());
		Assertions.assertNotNull(project.getLastModifiedDate());
		Assertions.assertEquals(DEFAULT_USER, project.getCreatedBy().getId());
		Assertions.assertEquals(DEFAULT_USER, project.getLastModifiedBy().getId());
		Assertions.assertEquals(1, project.getNbSubscriptions());
		Assertions.assertEquals("fdaugan", project.getTeamLeader().getId());
	}

	private UriInfo newFindAllParameters() {
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add("draw", "1");
		uriInfo.getQueryParameters().add("length", "10");
		uriInfo.getQueryParameters().add("columns[0][data]", "name");
		uriInfo.getQueryParameters().add("order[0][column]", "0");
		uriInfo.getQueryParameters().add("order[0][dir]", "desc");
		return uriInfo;
	}

	/**
	 * test {@link ProjectResource#findById(int)}
	 */
	@Test
	public void findByIdInvalid() {
		initSpringSecurityContext("alongchu");
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.findById(0);
		});
	}

	/**
	 * test {@link ProjectResource#findById(int)}
	 */
	@Test
	public void findByIdNotVisible() {
		final Project byName = repository.findByName("gStack");
		initSpringSecurityContext("any");
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.findById(byName.getId());
		});
	}

	/**
	 * test {@link ProjectResource#findById(int)}
	 */
	@Test
	public void findByIdVisibleSinceAdmin() {
		initSpringSecurityContext("admin");
		final Project byName = repository.findByName("gStack");
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.findById(byName.getId());
		});
	}

	/**
	 * test {@link ProjectResource#findById(int)}
	 */
	@Test
	public void findByIdWithSubscription() throws IOException {
		final Project byName = repository.findByName("gStack");
		persistEntities("csv", new Class[] { Event.class }, StandardCharsets.UTF_8.name());

		initSpringSecurityContext("alongchu");
		final ProjectVo project = resource.findById(byName.getId());

		// Check subscription
		Assertions.assertTrue(project.getSubscriptions().size() >= 6);
		for (final SubscriptionVo subscription : project.getSubscriptions()) {
			if (subscription.getStatus() != null) {
				return;
			}
		}
		Assertions.fail("Subscriptions status was expected.");
	}

	/**
	 * test {@link ProjectResource#findById(int)}
	 */
	@Test
	public void findById() {
		initSpringSecurityContext("fdaugan");
		checkProject(resource.findById(testProject.getId()));
	}

	/**
	 * Test {@link ProjectResource#findById(int)} when a subscription has no
	 * parameter.
	 */
	@Test
	public void findByIdNoParameter() {
		// Pre check
		initSpringSecurityContext("fdaugan");
		Assertions.assertEquals(1, resource.findById(testProject.getId()).getSubscriptions().size());
		em.flush();
		em.clear();

		final Subscription subscription = new Subscription();
		subscription.setProject(testProject);
		subscription.setNode(nodeRepository.findOneExpected("service:build:jenkins"));
		subscriptionRepository.saveAndFlush(subscription);
		em.flush();
		em.clear();

		// Post check
		final List<SubscriptionVo> subscriptions = resource.findById(testProject.getId()).getSubscriptions();
		Assertions.assertEquals(2, subscriptions.size());
		Assertions.assertEquals("service:bt:jira:4", subscriptions.get(0).getNode().getId());
		Assertions.assertEquals("service:build:jenkins", subscriptions.get(1).getNode().getId());
		Assertions.assertEquals(0, subscriptions.get(1).getParameters().size());
	}

	/**
	 * test {@link ProjectResource#findByPKey(String)}
	 */
	@Test
	public void findByPKey() {
		initSpringSecurityContext("fdaugan");
		checkProject(resource.findByPKey("mda"));
	}

	/**
	 * test {@link ProjectResource#findByPKey(String)}
	 */
	@Test
	public void findByPKeyNotExists() {
		initSpringSecurityContext("any");
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			checkProject(resource.findByPKey("mda"));
		});
	}

	private void checkProject(final BasicProjectVo project) {
		Assertions.assertEquals("MDA", project.getName());
		Assertions.assertEquals(testProject.getId(), project.getId());
		Assertions.assertEquals("Model Driven Architecture implementation of Gfi", project.getDescription());
		Assertions.assertNotNull(project.getCreatedDate());
		Assertions.assertNotNull(project.getLastModifiedDate());
		Assertions.assertEquals(DEFAULT_USER, project.getCreatedBy().getId());
		Assertions.assertEquals(DEFAULT_USER, project.getLastModifiedBy().getId());
		Assertions.assertEquals("mda", project.getPkey());
		Assertions.assertEquals("fdaugan", project.getTeamLeader().getId());
	}

	private void checkProject(final ProjectVo project) {
		checkProject((BasicProjectVo) project);
		Assertions.assertTrue(project.isManageSubscriptions());

		// Check subscription
		Assertions.assertEquals(1, project.getSubscriptions().size());
		final SubscriptionVo subscription = project.getSubscriptions().iterator().next();
		Assertions.assertNotNull(subscription.getCreatedDate());
		Assertions.assertNotNull(subscription.getLastModifiedDate());
		Assertions.assertNotNull(subscription.getId());
		Assertions.assertEquals(DEFAULT_USER, subscription.getCreatedBy().getId());
		Assertions.assertEquals(DEFAULT_USER, subscription.getLastModifiedBy().getId());

		// Check service (ordered by id)
		final NodeVo service = subscription.getNode();
		Assertions.assertNotNull(service);
		Assertions.assertEquals("JIRA 4", service.getName());
		Assertions.assertNotNull(service.getId());
		Assertions.assertEquals("service:bt:jira", service.getRefined().getId());
		Assertions.assertEquals("service:bt", service.getRefined().getRefined().getId());
		Assertions.assertNull(service.getRefined().getRefined().getRefined());

		// Check subscription values
		Assertions.assertEquals(3, subscription.getParameters().size());
		Assertions.assertEquals("http://localhost:8120", subscription.getParameters().get("service:bt:jira:url"));
		Assertions.assertEquals(10074,
				((Integer) subscription.getParameters().get("service:bt:jira:project")).intValue());
		Assertions.assertEquals("MDA", subscription.getParameters().get("service:bt:jira:pkey"));
	}

	/**
	 * test create
	 */
	@Test
	public void create() {
		final ProjectEditionVo vo = new ProjectEditionVo();
		vo.setName("Name");
		vo.setDescription("Description");
		vo.setPkey("artifact-id");
		vo.setTeamLeader(DEFAULT_USER);
		final int id = resource.create(vo);
		em.clear();

		final Project entity = repository.findOneExpected(id);
		Assertions.assertEquals("Name", entity.getName());
		Assertions.assertEquals("Description", entity.getDescription());
		Assertions.assertEquals("artifact-id", entity.getPkey());
		Assertions.assertEquals(DEFAULT_USER, entity.getTeamLeader());
	}

	/**
	 * test update
	 */
	@Test
	public void updateWithSubscriptions() {
		final ProjectEditionVo vo = new ProjectEditionVo();
		vo.setId(testProject.getId());
		vo.setName("Name");
		vo.setDescription("Description");
		vo.setPkey("artifact-id");
		vo.setTeamLeader(DEFAULT_USER);
		resource.update(vo);
		em.flush();
		em.clear();

		final Project projFromDB = repository.findOne(testProject.getId());
		Assertions.assertEquals("Name", projFromDB.getName());
		Assertions.assertEquals("Description", projFromDB.getDescription());
		Assertions.assertEquals("mda", projFromDB.getPkey());
		Assertions.assertEquals(DEFAULT_USER, projFromDB.getTeamLeader());
	}

	/**
	 * test update
	 */
	@Test
	public void update() {
		create();
		final Project project = repository.findByName("Name");
		final ProjectEditionVo vo = new ProjectEditionVo();
		vo.setId(project.getId());
		vo.setName("Name");
		vo.setDescription("D<small>e</small>s<a href=\"#/\">cription</a>");
		vo.setPkey("artifact-id");
		vo.setTeamLeader(DEFAULT_USER);
		vo.setDisable(true);
		resource.update(vo);
		em.clear();

		final Project projFromDB = repository.findOne(project.getId());
		Assertions.assertEquals("Name", projFromDB.getName());
		Assertions.assertEquals("D<small>e</small>s<a href=\"#/\">cription</a>", projFromDB.getDescription());
		Assertions.assertEquals("artifact-id", projFromDB.getPkey());
		Assertions.assertEquals(DEFAULT_USER, projFromDB.getTeamLeader());
		Assertions.assertTrue(projFromDB.isDisable());
	}

	/**
	 * Create with invalid HTML content.
	 */
	@Test
	public void updateInvalidDescription() {
		create();
		final Project project = repository.findByName("Name");
		final ProjectEditionVo vo = new ProjectEditionVo();
		vo.setId(project.getId());
		vo.setName("Name");
		vo.setDescription("Description<script some=\"..\">Bad there</script>");
		vo.setPkey("artifact-id");
		vo.setTeamLeader(DEFAULT_USER);
		Assertions.assertThrows(ConstraintViolationException.class, () -> resource.update(vo));
		vo.setDescription("Description<script >Bad there</script>");
		Assertions.assertThrows(ConstraintViolationException.class, () -> resource.update(vo));
	}

	/**
	 * Create with invalid HTML content.
	 */
	@Test
	public void updateInvalidDescription2() {
		create();
		final Project project = repository.findByName("Name");
		final ProjectEditionVo vo = new ProjectEditionVo();
		vo.setId(project.getId());
		vo.setName("Name");
		vo.setDescription("Description<script >Bad there</script>");
		vo.setPkey("artifact-id");
		vo.setTeamLeader(DEFAULT_USER);
		Assertions.assertThrows(ConstraintViolationException.class, () -> resource.update(vo));
	}

	/**
	 * test update
	 */
	@Test
	public void deleteNotVisible() {
		em.clear();
		initSpringSecurityContext("mlavoine");
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.delete(testProject.getId());
		});
	}

	/**
	 * test update
	 */
	@Test
	public void deleteNotExists() {
		em.clear();
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.delete(-1);
		});
	}

	@Test
	public void delete() throws Exception {
		final long initCount = repository.count();
		em.clear();
		initSpringSecurityContext("fdaugan");
		resource.delete(testProject.getId());
		em.flush();
		em.clear();
		Assertions.assertEquals(initCount - 1, repository.count());
	}
}
