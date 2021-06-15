/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;

/**
 * Test to make sure that only an expected subset of nodes are being re-executed
 * if {@link WebResourceController#reexecuteSinglePage(NodeIDSuffix, Map)} is
 * called.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class BugWEBP803_OnlyResetNodesToBeReexecuted extends WorkflowTestCase {

	/**
	 * Test for the {@link WizardExecutionController}.
	 *
	 * @throws Exception
	 */
	@Test
	public void testWizardPageReexecution() throws Exception {
		loadAndSetWorkflow();
		WorkflowManager wfm = getManager();
		SubNodeContainer page = (SubNodeContainer) wfm.getNodeContainer(wfm.getID().createChild(9));

		WizardExecutionController wec = wfm.setAndGetWizardExecutionController();
		wec.stepFirst();
		waitForPage(page);
		assertThat(wec.hasCurrentWizardPage(), is(true));

		testReexecution(page, wec);
	}

	/**
	 * Test for the {@link CompositeViewController}.
	 *
	 * @throws Exception
	 */
	@Test
	public void testCompositeViewReexecution() throws Exception {
		loadAndSetWorkflow();
		WorkflowManager wfm = getManager();
		SubNodeContainer page = (SubNodeContainer) wfm.getNodeContainer(wfm.getID().createChild(9));

		wfm.executeAllAndWaitUntilDone();

		testReexecution(page, new CompositeViewController(wfm, page.getID()));
	}

	private static void testReexecution(SubNodeContainer page, WebResourceController wrc) {
		wrc.reexecuteSinglePage(createNodeIDSuffix(2), createWizardPageInput());
		waitForPage(page);
		wrc.reexecuteSinglePage(createNodeIDSuffix(2), createWizardPageInput());
		waitForPage(page);

		// get flow variables which contain the the number of executions for each widget
		Map<String, FlowVariable> flowVars = page.getOutgoingFlowObjectStack().getAllAvailableFlowVariables();

		// widgets not being re-executed
		assertThat(flowVars.get("Text Output Widget").getIntValue(), is(1));
		assertThat(flowVars.get("String Widget").getIntValue(), is(1));

		// widgets being re-executed
		assertThat(flowVars.get("Refresh Button Widget").getIntValue(), greaterThan(1));
		assertThat(flowVars.get("Table View").getIntValue(), greaterThan(1));

		WorkflowManager parent = page.getParent();
		SubNodeContainer successorPage = (SubNodeContainer) parent.getNodeContainer(parent.getID().createChild(11));
		assertThat(successorPage.getInternalState(), is(InternalNodeContainerState.CONFIGURED));
	}

	private static void waitForPage(SubNodeContainer page) {
		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS)
				.until(() -> page.getNodeContainerState().isExecuted());
	}

	private static NodeIDSuffix createNodeIDSuffix(int... ids) {
		return new NodeIDSuffix(ids);
	}

	private static Map<String, String> createWizardPageInput() {
		return Map.of("9:0:26",
				"{\"@class\":\"org.knime.js.base.node.viz.pagedTable.PagedTableViewValue\",\"publishSelection\":true,\"subscribeSelection\":true,\"publishFilter\":true,\"subscribeFilter\":true,\"pageSize\":0,\"currentPage\":0,\"hideUnselected\":false,\"selection\":null,\"selectAll\":false,\"selectAllIndeterminate\":false,\"filterString\":null,\"columnFilterStrings\":null,\"currentOrder\":[]}");
	}

}
