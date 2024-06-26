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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 24, 2024 (wiswedel): created
 */
package org.knime.core.customization;

import java.util.Collections;
import java.util.List;

import org.knime.core.customization.nodesfilter.NodesFilter;
import org.knime.core.customization.nodesfilter.NodesFilter.ScopeEnum;
import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the customization settings for the KNIME AP, currently only determining which nodes are allowed for
 * use and listing in the node repository.
 *
 * Instances of this class are not created directly, only restored from yaml that looks similar to this:
 *
 * <pre>
 * nodesFilter:
 * - scope: view
 *   rule: allow
 *   predicate:
 *     type: pattern
 *     patterns:
 *       - org\\.knime\\.base\\..+
 *     isRegex: true
 * </pre>
 *
 * @since 5.3
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel
 */
public final class APCustomization {

    /**
     * Default (no) customization.
     */
    public static final APCustomization DEFAULT =
            new APCustomization(List.of(NodesFilter.USE_ALL, NodesFilter.VIEW_ALL));

    private final List<NodesFilter> m_nodesFilters;

    /**
     * Only used for deserialization.
     */
    @JsonCreator
    APCustomization(@JsonProperty("nodesFilter") final List<NodesFilter> nodesFilter) {
        m_nodesFilters = Collections
            .unmodifiableList(CheckUtils.checkArgumentNotNull(nodesFilter, "nodeFilters cannot be null"));
    }

    /**
     * Retrieves the list of node customization settings, not null. Only used for testing.
     *
     * @return An unmodifiable list of {@link NodesFilter}.
     */
    List<NodesFilter> getNodesFilters() {
        return m_nodesFilters;
    }

    /**
     * @return customization for nodes
     */
    public Nodes nodes() {
        return new Nodes();
    }

    /**
     * Customizations for nodes.
     */
    public final class Nodes {

        /**
         * Determines if a given node is allowed to be used (instantiated) based on the filter rules.
         *
         * @param factoryId as per {@link org.knime.core.node.NodeFactoryId}
         * @return if the node can be instantiated.
         */
        public boolean isUsageAllowed(final String factoryId) {
            return m_nodesFilters.stream().filter(t -> t.getScope() == ScopeEnum.USE)
                .allMatch(t -> t.isAllowed(factoryId));
        }

        /**
         * Determines if a given node is allowed to be listed ("viewed") in the node repository.
         *
         * @param factoryId as per {@link org.knime.core.node.NodeFactoryId}
         * @return this property.
         */
        public boolean isViewAllowed(final String factoryId) {
            // ignore scope property - not to be used nodes are also not visible.
            return m_nodesFilters.stream().allMatch(t -> t.isAllowed(factoryId));
        }

    }

    @Override
    public String toString() {
        return String.format("APCustomization{nodesFilters=%s}", m_nodesFilters);
    }
}
