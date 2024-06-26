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
 *   20.05.2016 (koetter): created
 */
package org.knime.core.node.port.database.connection;

import java.io.File;
import java.io.IOException;
import java.sql.Driver;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseWrappedDriver;

/**
 *
 * @author Tobias Koetter, KNIME.com
 * @since 3.2
 */
@Deprecated
public class PriorityDriverFactory implements DBDriverFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PriorityDriverFactory.class);

    private final DBDriverFactory[] m_factories;

    /**
     * @param factories the {@link DBDriverFactory} implementation in the order they should be used
     * to retrieve a suitable {@link Driver}
     */
    public PriorityDriverFactory(final DBDriverFactory... factories) {
        m_factories = factories;
    }

    /**
     * @return the driverNames
     */
    @Override
    public Set<String> getDriverNames() {
        final Set<String> driverNames = new HashSet<>();
        for (DBDriverFactory factory : m_factories) {
            driverNames.addAll(factory.getDriverNames());
        }
        return driverNames;
    }

    /**
     * @return the factories
     */
    public DBDriverFactory[] getFactories() {
        return m_factories;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Driver getDriver(final DatabaseConnectionSettings settings) throws Exception {
        Exception lastEx = null;
        for (final DBDriverFactory factory : m_factories) {
            try {
                final Driver driver = factory.getDriver(settings);
                if (driver != null) {
                    LOGGER.debug("DB driver: " + settings.getDriver() + " found in driver factory: "
                            + factory.getClass().getCanonicalName() + ". Driver info: "
                            + DatabaseWrappedDriver.getInfo(driver));
                    return driver;
                }
            } catch (Exception e) {
                lastEx = e;
            }
        }
        if (lastEx != null) {
            LOGGER.warn(lastEx);
            throw lastEx;
        }
        throw new Exception("No suiteable driver found for class: " + settings.getDriver());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<File> getDriverFiles(final DatabaseConnectionSettings settings) throws IOException {
        IOException lastEx = null;
        for (final DBDriverFactory factory : m_factories) {
            try {
                final Collection<File> files = factory.getDriverFiles(settings);
                if (!files.isEmpty()) {
                    return files;
                }
            } catch (IOException e) {
                lastEx = e;
            }
        }
        if (lastEx != null) {
            throw lastEx;
        }
        throw new IOException("No suiteable driver found for class: " + settings.getDriver());
    }

}
