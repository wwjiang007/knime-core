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
 *   Sep 29, 2020 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.data.TableBackend;
import org.knime.core.data.TableBackendRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;

/**
 * Represents the configuration as to which {@link TableBackend} is used in a workflow project
 * (workflow level configuration).
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public final class WorkflowTableBackendSettings {

    /**
     *
     */
    private static final String CFG_TABLE_BACKEND = "tableBackend";
    private static final String CFG_TABLE_BACKEND_CLASS = "class";

    private static final TableBackend DEFAULT_BACKEND = TableBackendRegistry.getInstance().getDefaultBackend();
    private final TableBackend m_tableBackend;

    /**
     *
     */
    WorkflowTableBackendSettings() {
        this(DEFAULT_BACKEND);
    }

    WorkflowTableBackendSettings(final TableBackend tableBackend) {
        m_tableBackend = CheckUtils.checkArgumentNotNull(tableBackend);
    }

    /**
     * @return the tableBackend
     */
    TableBackend getTableBackend() {
        return m_tableBackend;
    }

    static WorkflowTableBackendSettings loadSettingsInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        TableBackend tableBackend;
        if (settings.containsKey(CFG_TABLE_BACKEND)) {
            NodeSettingsRO tableBackendSettings = settings.getNodeSettings(CFG_TABLE_BACKEND);
            String className = CheckUtils.checkSettingNotNull(tableBackendSettings.getString(CFG_TABLE_BACKEND_CLASS),
                "Table Backend Class must not be null");
            try {
                tableBackend = TableBackendRegistry.getInstance().getTableBackend(className);
            } catch (IllegalArgumentException ex) {
                throw new InvalidSettingsException("Table Backend Implementation not found: " + ex.getMessage(), ex);
            }
        } else {
            tableBackend = DEFAULT_BACKEND;
        }
        return new WorkflowTableBackendSettings(tableBackend);
    }

    static WorkflowTableBackendSettings loadSettingsInDialog(final NodeSettingsRO settings) {
        try {
            return loadSettingsInModel(settings);
        } catch (InvalidSettingsException ex) { // NOSONAR
            return new WorkflowTableBackendSettings();
        }
    }

    void saveSettingsTo(final NodeSettingsWO settings) {
        if (!m_tableBackend.equals(DEFAULT_BACKEND)) {
            NodeSettingsWO tableBackendSettings = settings.addNodeSettings(CFG_TABLE_BACKEND);
            tableBackendSettings.addString(CFG_TABLE_BACKEND_CLASS, m_tableBackend.getClass().getName());
        }
    }

    /** Get table backend selected for the current thread / executing workflow. Method resolved corresponding workflow
     * via {@link NodeContext}. If none is detected, the default backend is returned.
     * @return The backend for the current workflow, not null.
     */
    public static TableBackend getTableBackendForCurrentContext() {
        NodeContext context = NodeContext.getContext();
        if (context != null) {
            WorkflowManager wfm = context.getWorkflowManager();
            if (wfm != null) {
                return wfm.getTableBackendSettings().map(WorkflowTableBackendSettings::getTableBackend)
                    .orElse(DEFAULT_BACKEND);
            }
        }
        return DEFAULT_BACKEND;
    }

    @Override
    public String toString() {
        return String.format("backend: %s", m_tableBackend.getClass().getSimpleName());
    }

}
