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
 *   Jan 1, 2023 (wiswedel): created
 */
package org.knime.core.node.message;

import java.util.Objects;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.util.CheckUtils;

/**
 * An {@link Issue} just wrapping some text.
 *
 * @since 5.0
 */
@SuppressWarnings("javadoc")
final class DefaultIssue implements Issue {

    private static final String CFG_PREFORMATTED = "preformatted";

    private final String m_preformatted;

    DefaultIssue(final String preformatted) {
        m_preformatted = CheckUtils.checkArgumentNotNull(preformatted);
    }

    @Override
    public String toPreformatted() {
        return m_preformatted;
    }

    @Override
    public Type getType() {
        return Type.TEXT;
    }

    @Override
    public void saveTo(final ConfigBaseWO config) {
        config.addString(CFG_PREFORMATTED, m_preformatted);
    }


    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DefaultIssue)) {
            return false;
        }
        var m = (DefaultIssue)obj;
        return Objects.equals(m_preformatted, m.m_preformatted);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(m_preformatted).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(CFG_PREFORMATTED, m_preformatted).toString();
    }

    static DefaultIssue load(final ConfigBaseRO config) throws InvalidSettingsException {
        var preformatted = config.getString(CFG_PREFORMATTED);
        return new DefaultIssue(preformatted);
    }
}
