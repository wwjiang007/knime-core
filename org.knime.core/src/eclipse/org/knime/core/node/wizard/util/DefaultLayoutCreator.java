/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   11 Nov 2016 (albrecht): created
 */
package org.knime.core.node.wizard.util;

import java.io.IOException;
import java.util.Map;

import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.SubnodeContainerLayoutStringProvider;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * A service interface to create a default view layout from a given set of {@link WizardNode}.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.3
 */
public interface DefaultLayoutCreator {

    /**
     * Creates a default layout structure as a serialized JSON string.
     * @param viewNodes the nodes to include in the layout.
     * @return a default layout structure as JSON string.
     * @throws IOException on creation error.
     */
    public String createDefaultLayout(final Map<NodeIDSuffix, SingleNodeContainer> viewNodes) throws IOException;

    /**
     * Expands nested layouts by inserting the appropriate sub-layouts in an original layout.
     * @param layoutStringProvider the layout provider with an unexpanded layout.
     * @param wfm the {@link WorkflowManager} of the containing {@link SubNodeContainer}.
     * @since 4.2
     */
    public void expandNestedLayout(final SubnodeContainerLayoutStringProvider layoutStringProvider,
        final WorkflowManager wfm);

    /**
     * Creates extra rows/columns at the bottom of the layout for all unreferenced nodes.
     * @param layoutStringProvider the layout provider, who's layout needs to be already expanded.
     * @param allNodes a map of all viewable nodes.
     * @param allNestedViews a map of all {@link SubNodeContainer} which contain nested views.
     * @param containerID the {@link NodeID} of the containing subnode container.
     * @since 4.2
     */
    public void addUnreferencedViews(final SubnodeContainerLayoutStringProvider layoutStringProvider,
        final Map<NodeIDSuffix, NativeNodeContainer> allNodes, final Map<NodeIDSuffix, SubNodeContainer> allNestedViews,
        final NodeID containerID);

    /**
     * Updates a layout.
     * @param layoutStringProvider the layout provider, who's layout needs to be already expanded.
     * @since 4.2
     */
    public void updateLayout(final SubnodeContainerLayoutStringProvider layoutStringProvider);
}
