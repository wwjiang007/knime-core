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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.09.2008 (ohl): created
 */
package org.knime.core.data.renderer;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.vector.bytevector.ByteVectorValue;

/**
 * Renderer for byte vector values showing the string representation.
 *
 * @author ohl, University of Konstanz
 */
@SuppressWarnings("serial")
public class ByteVectorValueStringRenderer extends DefaultDataValueRenderer {
    /**
     * Factory for {@link ByteVectorValueStringRenderer}.
     *
     * @since 2.8
     */
    public static final class Factory extends AbstractDataValueRendererFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new ByteVectorValueStringRenderer();
        }
    }

    private static final String DESCRIPTION = "Byte Vector";

    /**
     * Instance to be used.
     * @deprecated Do not use this singleton instance, renderers are not thread-safe!
     */
    @Deprecated
    public static final ByteVectorValueStringRenderer INSTANCE =
            new ByteVectorValueStringRenderer();

    /**
     * Default Initialization is empty.
     */
    ByteVectorValueStringRenderer() {
        super(DESCRIPTION);
    }

    /**
     * Tries to cast o IntValue and will set the integer in the super class. If
     * that fails, the object's toString() method is used.
     *
     * @param value The object to be rendered, should be an
     *            {@link ByteVectorValue}.
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof ByteVectorValue) {
            super.setValue(value.toString());
        } else {
            super.setValue(value);
        }
    }

}
