/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.web.templatemodels.individual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import stubs.edu.cornell.mannlib.vitro.webapp.dao.ObjectPropertyDaoStub;
import stubs.edu.cornell.mannlib.vitro.webapp.dao.WebappDaoFactoryStub;
import stubs.freemarker.cache.TemplateLoaderStub;
import stubs.javax.servlet.ServletContextStub;
import stubs.javax.servlet.http.HttpServletRequestStub;
import stubs.javax.servlet.http.HttpSessionStub;
import edu.cornell.mannlib.vitro.testing.AbstractTestClass;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.beans.IndividualImpl;
import edu.cornell.mannlib.vitro.webapp.beans.ObjectProperty;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.dao.WebappDaoFactory;
import edu.cornell.mannlib.vitro.webapp.web.templatemodels.BaseTemplateModel;
import edu.cornell.mannlib.vitro.webapp.web.templatemodels.individual.ObjectPropertyTemplateModel.InvalidConfigurationException;
import freemarker.template.Configuration;

public class ObjectPropertyTemplateModel_PropertyListConfigTest extends
		AbstractTestClass {

	private static File configDir;

	private ObjectPropertyTemplateModel optm;

	private ObjectProperty op;

	private WebappDaoFactoryStub wadf;
	private ObjectPropertyDaoStub opDao;

	private ServletContextStub ctx;
	private HttpSessionStub session;
	private HttpServletRequestStub hreq;
	private VitroRequest vreq;

	private IndividualImpl subject;

	private TemplateLoaderStub tl;

	private StringWriter logMessages;

	/**
	 * In general, we expect no exception, but individual tests may override,
	 * like this:
	 * 
	 * <pre>
	 * thrown.expect(InvalidConfigurationException.class);
	 * thrown.expectMessage(&quot;Bozo&quot;);
	 * </pre>
	 */
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@BeforeClass
	public static void createConfigFiles() throws IOException {
		configDir = createTempDirectory("configDir");
		createConfigFile("constructQueryMissing");
		createConfigFile("constructQueryMultiple");
		createConfigFile("default");
		createConfigFile("notValidXml");
		createConfigFile("postProcessorClassNotFound");
		createConfigFile("postProcessorClassNotSuitable");
		createConfigFile("postProcessorConstructorThrowsException");
		createConfigFile("postProcessorNameEmpty");
		createConfigFile("postProcessorOK");
		createConfigFile("postProcessorWrongConstructor");
		createConfigFile("selectQueryNodeBlank");
		createConfigFile("selectQueryNodeNotFound");
		createConfigFile("selectQuerySubNodes");
		createConfigFile("selectQueryNoSubNodes");
		createConfigFile("selectQueryCollatedValid");
		createConfigFile("selectQueryCollatedNoSelect");
		createConfigFile("selectQueryCollatedNoOrder");
		createConfigFile("templateNodeIsEmpty");
		createConfigFile("templateNodeNotFound");
		createConfigFile("templateDoesNotExist");
	}

	/** Copy a file from the classpath into the temporary config directory. */
	private static void createConfigFile(String shortName) throws IOException {
		String fullName = "testConfig-" + shortName + ".xml";
		Class<?> clazz = ObjectPropertyTemplateModel_PropertyListConfigTest.class;
		String contents = readAll(clazz.getResourceAsStream(fullName));
		createFile(configDir, fullName, contents);
	}

	@Before
	public void setup() {
		logMessages = new StringWriter();

		opDao = new ObjectPropertyDaoStub();
		wadf = new WebappDaoFactoryStub();
		wadf.setObjectPropertyDao(opDao);

		ctx = new ServletContextStub();
		// create paths for all of the files in the temporary config directory.
		ctx.setRealPaths("/config", configDir);
		// add a path to match the hard-coded default path.
		ctx.setRealPath("/config/listViewConfig-default.xml",
				ctx.getRealPath("/config/testConfig-default.xml"));

		session = new HttpSessionStub();
		session.setServletContext(ctx);
		hreq = new HttpServletRequestStub();
		hreq.setSession(session);

		vreq = new VitroRequest(hreq);
		vreq.setWebappDaoFactory(wadf);

		subject = new IndividualImpl();

		BaseTemplateModel.setServletContext(ctx);

		Configuration fmConfig = new Configuration();
		vreq.setAttribute("freemarkerConfig", fmConfig);
		tl = new TemplateLoaderStub();
		tl.createTemplate("propStatement-default.ftl", "");
		fmConfig.setTemplateLoader(tl);
	}

	@AfterClass
	public static void cleanup() {
		if (configDir != null) {
			purgeDirectoryRecursively(configDir);
		}
	}

	// TODO - don't swallow the exception if we can't create a config.
	// TODO - baseTemplateModel shouldn't require the servlet context to be set
	// statically!!! ServletContext shouldn't be a static field.

	// ----------------------------------------------------------------------
	// The tests
	//
	// TODO - remove any tests that are covered by the newer
	// CustomListViewConfigFileTest.
	// ----------------------------------------------------------------------

	//
	// Null arguments
	//

	@Test(expected = NullPointerException.class)
	public void operationIsNull() throws InvalidConfigurationException {
		// TODO This should throw a more predictable NullPointerException
		optm = new NonCollatingOPTM(null, subject, vreq, false);
	}

	@Test(expected = NullPointerException.class)
	public void subjectIsNull() throws InvalidConfigurationException {
		// TODO This should throw a more predictable NullPointerException
		op = buildOperation("default");
		optm = new NonCollatingOPTM(op, null, vreq, false);
	}

	@Test(expected = NullPointerException.class)
	public void requestIsNull() throws InvalidConfigurationException {
		// TODO This should throw a more predictable NullPointerException
		op = buildOperation("default");
		optm = new NonCollatingOPTM(op, subject, null, false);
	}

	//
	// Locating the file.
	//

	@Test
	public void configFileNotSpecified() throws InvalidConfigurationException {
		op = buildOperation("default");
		opDao.setCustomListViewConfigFileName(op, null);
		optm = new NonCollatingOPTM(op, subject, vreq, false);
		assertEquals("uses default config", true, optm.hasDefaultListView());
	}

	// @Ignore
	@Test
	public void configFilePathCantBeTranslated()
			throws InvalidConfigurationException {
		// TODO if we can't translate the path, log the error and use the
		// default.
		captureLogsFromOPTM();
		op = buildOperation("fileHasNoRealPath");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		// Throws an exception because we don't test for a null path from the
		// ServletContext.
		assertLogMessagesContains("no real path", "at java.io.File.<init>");
	}

	@Test
	public void configFileNotFound() throws InvalidConfigurationException {
		captureLogsFromOPTM();

		op = buildOperation("configFileDoesNotExist");

		// There should be a real path, but no file at that location.
		String bogusFilepath = new File(configDir, "doesNotExist")
				.getAbsolutePath();
		String path = "/config/" + opDao.getCustomListViewConfigFileName(op);
		ctx.setRealPath(path, bogusFilepath);

		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("file not found", "Can't find config file");
	}

	@Test
	public void configFileNotValidXml() throws InvalidConfigurationException {
		suppressSyserr();
		captureLogsFromOPTM();

		op = buildOperation("notValidXml");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("not valid XML", "SAXParseException");
	}

	//
	// Problems with the <query-select> node
	//

	@Test
	public void selectQueryNodeIsNotFound()
			throws InvalidConfigurationException {
		captureLogsFromOPTM();

		op = buildOperation("selectQueryNodeNotFound");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("no select query",
				"Missing select query specification");
	}

	@Test
	public void selectQueryNodeIsBlank() throws InvalidConfigurationException {
		captureLogsFromOPTM();

		op = buildOperation("selectQueryNodeBlank");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("blank select query",
				"Missing select query specification");
	}

	//
	// Problems with the <template> node
	//
	@Test
	public void templateNodeNotFound() throws InvalidConfigurationException {
		captureLogsFromOPTM();

		op = buildOperation("templateNodeNotFound");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("no template node",
				"Config file must contain a template element");
	}

	@Test
	public void templateNodeIsEmpty() throws InvalidConfigurationException {
		captureLogsFromOPTM();

		op = buildOperation("templateNodeIsEmpty");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("empty template node",
				"In a config file, the <template> element must not be empty.");
	}

	@Test
	public void templateDoesNotExist() throws InvalidConfigurationException {
		captureLogsFromOPTM();

		op = buildOperation("templateDoesNotExist");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("template doesn't exist",
				"Specified template does not exist");
	}

	//
	// Optional tags in the select query.
	//

	@Test
	public void selectSubNodesCollatedCritical()
			throws InvalidConfigurationException {
		op = buildOperation("selectQuerySubNodes");
		optm = new SimpleCollatingOPTM(op, subject, vreq, false);
		assertSelectQuery("collated, critical",
				"Plain collated plain critical plain collated plain.");
	}

	@Test
	public void selectSubNodesCollatedUncritical()
			throws InvalidConfigurationException {
		op = buildOperation("selectQuerySubNodes");
		optm = new SimpleCollatingOPTM(op, subject, vreq, true);
		assertSelectQuery("collated, UNcritical",
				"Plain collated plain plain collated plain.");
	}

	@Test
	public void selectSubNodesUncollatedCritical()
			throws InvalidConfigurationException {
		op = buildOperation("selectQuerySubNodes");
		optm = new NonCollatingOPTM(op, subject, vreq, false);
		assertSelectQuery("UNcollated, critical",
				"Plain plain critical plain plain.");
	}

	@Test
	public void selectSubNodesUncollatedUncritical()
			throws InvalidConfigurationException {
		op = buildOperation("selectQuerySubNodes");
		optm = new NonCollatingOPTM(op, subject, vreq, true);
		assertSelectQuery("UNcollated, UNcritical", "Plain plain plain plain.");
	}

	@Test
	public void selectNoSubNodesCollatedCritical()
			throws InvalidConfigurationException {
		op = buildOperation("selectQueryNoSubNodes");
		optm = new SimpleCollatingOPTM(op, subject, vreq, false);
		assertSelectQuery("simple collated, critical", "Plain.");
	}

	@Test
	public void collatedNoSubclassSelector()
			throws InvalidConfigurationException {
		thrown.expect(InvalidConfigurationException.class);
		thrown.expectMessage("Query does not select a subclass variable");

		op = buildOperation("selectQueryCollatedNoSelect");
		optm = new CheckingCollatingOPTM(op, subject, vreq, false);
	}

	@Test
	public void collatedNoSubclassOrder() throws InvalidConfigurationException {
		thrown.expect(InvalidConfigurationException.class);
		thrown.expectMessage("Query does not sort first by subclass variable");

		op = buildOperation("selectQueryCollatedNoOrder");
		optm = new CheckingCollatingOPTM(op, subject, vreq, false);
	}

	@Test
	public void collatedValid() throws InvalidConfigurationException {
		// Throws no exception
		op = buildOperation("selectQueryCollatedValid");
		optm = new CheckingCollatingOPTM(op, subject, vreq, false);
	}

	//
	// Construct query
	//

	@Test
	public void constructQueryNodeMissing()
			throws InvalidConfigurationException {
		op = buildOperation("constructQueryMissing");
		optm = new NonCollatingOPTM(op, subject, vreq, true);
		// Not an error.
	}

	@Test
	public void constructQueryMultipleValues()
			throws InvalidConfigurationException {
		op = buildOperation("constructQueryMultiple");
		optm = new NonCollatingOPTM(op, subject, vreq, true);
		assertConstructQueries("multiple construct queries", "ONE", "TWO",
				"THREE");
	}

	//
	// PostProcessor
	//

	@Test
	public void postProcessorNameEmpty() throws InvalidConfigurationException {
		op = buildOperation("postProcessorNameEmpty");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertPostProcessorClass("pp name empty",
				DefaultObjectPropertyDataPostProcessor.class);
	}

	@Test
	public void postProcessorClassNotFound()
			throws InvalidConfigurationException {
		captureLogsFromOPTM();

		op = buildOperation("postProcessorClassNotFound");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("pp class not found",
				"java.lang.ClassNotFoundException");
		assertPostProcessorClass("pp class not found",
				DefaultObjectPropertyDataPostProcessor.class);
	}

	@Test
	public void postProcessorClassIsNotSuitable()
			throws InvalidConfigurationException {
		captureLogsFromOPTM();

		op = buildOperation("postProcessorClassNotSuitable");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("pp doesn't implement required interface",
				"java.lang.ClassCastException");
		assertPostProcessorClass("pp doesn't implement required interface",
				DefaultObjectPropertyDataPostProcessor.class);
	}

	@Test
	public void postProcessorClassHasWrongConstructor()
			throws InvalidConfigurationException {
		captureLogsFromOPTM();

		op = buildOperation("postProcessorWrongConstructor");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("pp has wrong constructor",
				"java.lang.NoSuchMethodException");
		assertPostProcessorClass("pp has wrong constructor",
				DefaultObjectPropertyDataPostProcessor.class);
	}

	@Test
	public void postProcessorConstructorThrowsAnException()
			throws InvalidConfigurationException {
		captureLogsFromOPTM();

		op = buildOperation("postProcessorConstructorThrowsException");
		optm = new NonCollatingOPTM(op, subject, vreq, false);

		assertLogMessagesContains("pp throws an exception",
				"java.lang.reflect.InvocationTargetException");
		assertPostProcessorClass("pp throws an exception",
				DefaultObjectPropertyDataPostProcessor.class);
	}

	@Test
	public void postProcessorOK() throws InvalidConfigurationException {
		op = buildOperation("postProcessorOK");
		optm = new NonCollatingOPTM(op, subject, vreq, false);
		assertPostProcessorClass("pp OK", PostProcessorOK.class);
	}

	// ----------------------------------------------------------------------
	// Helper methods
	// ----------------------------------------------------------------------

	/**
	 * Sets up an operation with name "foobar" and adds it to the
	 * ObjectPropertyDaoStub.
	 * 
	 * The URI will be "http://foobar", and the ListViewConfig file will be
	 * "testConfig-foobar.xml". That file is assumed to exist already, and to be
	 * mapped in the ServletContextStub.
	 */
	private ObjectProperty buildOperation(String name) {
		ObjectProperty property = new ObjectProperty();
		property.setURI("http://" + name);

		opDao.addObjectProperty(property);
		opDao.setCustomListViewConfigFileName(property, "testConfig-" + name
				+ ".xml");

		return property;
	}

	/**
	 * Capture the log for ObjectPropertyTemplateModel and suppress it from the
	 * console.
	 */
	private void captureLogsFromOPTM() {
		captureLogOutput(ObjectPropertyTemplateModel.class, logMessages, true);
	}

	private void assertLogMessagesContains(String message, String expected) {
		if (logMessages.toString().contains(expected)) {
			return;
		}
		fail(message + "\nLOG\n" + logMessages + "\nDOES NOT CONTAIN\n"
				+ expected);
	}

	private void assertSelectQuery(String message, String expected) {
		String actual = "BOGUS";
		try {
			Method m = ObjectPropertyTemplateModel.class.getDeclaredMethod(
					"getSelectQuery", new Class<?>[0]);
			m.setAccessible(true);
			actual = (String) m.invoke(optm, new Object[0]);
		} catch (Exception e) {
			fail(message + " - " + e);
		}

		assertEquals(message, expected, actual);
	}

	@SuppressWarnings("unchecked")
	private void assertConstructQueries(String message, String... expectedArray) {
		Set<String> expected = new HashSet<String>(Arrays.asList(expectedArray));
		Set<String> actual = null;
		try {
			Method m = ObjectPropertyTemplateModel.class.getDeclaredMethod(
					"getConstructQueries", new Class<?>[0]);
			m.setAccessible(true);
			actual = (Set<String>) m.invoke(optm, new Object[0]);
		} catch (Exception e) {
			fail(message + " - " + e);
		}

		assertEqualSets(message, expected, actual);
	}

	private void assertPostProcessorClass(String message, Class<?> expected) {
		try {
			Field configField = ObjectPropertyTemplateModel.class
					.getDeclaredField("config");
			configField.setAccessible(true);
			Object config = configField.get(optm);

			Class<?> configClass = Class
					.forName("edu.cornell.mannlib.vitro.webapp.web."
							+ "templatemodels.individual."
							+ "ObjectPropertyTemplateModel$PropertyListConfig");

			Field ppField = configClass.getDeclaredField("postprocessor");
			ppField.setAccessible(true);
			Object pp = ppField.get(config);

			if (pp == null) {
				assertNull(message + " - postprocessor is null", expected);
			} else {
				assertEquals(message, expected, pp.getClass());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(message + " - " + e);
		}
	}

	// ----------------------------------------------------------------------
	// Supporting classes
	// ----------------------------------------------------------------------

	private static class NonCollatingOPTM extends ObjectPropertyTemplateModel {
		NonCollatingOPTM(ObjectProperty op, Individual subject,
				VitroRequest vreq, boolean editing)
				throws InvalidConfigurationException {
			super(op, subject, vreq, editing);
		}

		@Override
		protected boolean isEmpty() {
			return true;
		}

		@Override
		public boolean isCollatedBySubclass() {
			return false;
		}

	}

	/*
	 * No populated properties and we don't do syntax checking on the select
	 * query.
	 */
	private static class SimpleCollatingOPTM extends
			CollatedObjectPropertyTemplateModel {
		SimpleCollatingOPTM(ObjectProperty op, Individual subject,
				VitroRequest vreq, boolean editing)
				throws InvalidConfigurationException {
			super(op, subject, vreq, editing, Collections
					.<ObjectProperty> emptyList());
		}

		@Override
		protected ConfigError checkQuery(String queryString) {
			return null;
		}

	}

	/** No populated properties but we do check the syntax of the select query. */
	private static class CheckingCollatingOPTM extends
			CollatedObjectPropertyTemplateModel {
		CheckingCollatingOPTM(ObjectProperty op, Individual subject,
				VitroRequest vreq, boolean editing)
				throws InvalidConfigurationException {
			super(op, subject, vreq, editing, Collections
					.<ObjectProperty> emptyList());
		}

	}

	/** Does not implement the required interface. */
	public static class ClassNotSuitable {
		@SuppressWarnings("unused")
		public ClassNotSuitable(ObjectPropertyTemplateModel optm,
				WebappDaoFactory wadf) {
			// Do nothing.
		}

	}

	/** Does not have a constructor with the correct arguments */
	public static class PostProcessorWrongConstructor implements
			ObjectPropertyDataPostProcessor {

		@Override
		public void process(List<Map<String, String>> data) {
			// Do nothing.
		}

	}

	/** Constructor throws an exception */
	public static class PostProcessorThrowsException implements
			ObjectPropertyDataPostProcessor {

		@SuppressWarnings("unused")
		public PostProcessorThrowsException(ObjectPropertyTemplateModel optm,
				WebappDaoFactory wadf) {
			throw new RuntimeException("Constructor throws exception");
		}

		@Override
		public void process(List<Map<String, String>> data) {
			// Do nothing.
		}

	}

	/** Acceptable postprocessor */
	public static class PostProcessorOK implements
			ObjectPropertyDataPostProcessor {

		@SuppressWarnings("unused")
		public PostProcessorOK(ObjectPropertyTemplateModel optm,
				WebappDaoFactory wadf) {
			// Do nothing.
		}

		@Override
		public void process(List<Map<String, String>> data) {
			// Do nothing.
		}

	}
}