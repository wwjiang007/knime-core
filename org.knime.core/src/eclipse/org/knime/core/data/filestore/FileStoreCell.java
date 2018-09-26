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
 *
 * History
 *   Jun 26, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore;

import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.FileStoreProxy;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.6
 */
public abstract class FileStoreCell extends DataCell implements FlushCallback {

    private FileStoreProxy[] m_fileStoreProxies;
    private boolean m_isFlushedToFileStore;

    /**
     * @since 3.7
     */
    protected FileStoreCell(final FileStore[] fileStores) {
        if(fileStores.length < 1) {
            throw new IllegalArgumentException("FileStoreCell needs at least one fileStore");
        }

        m_fileStoreProxies = Arrays.stream(fileStores).map(FileStoreProxy::new).toArray(FileStoreProxy[]::new);
    }

    protected FileStoreCell(final FileStore fileStore) {
       this(new FileStore[] { fileStore });
    }

    /** Used when read from persisted stream.
     *  */
    protected FileStoreCell() {
        m_isFlushedToFileStore = true;
    }

    /** @return the fileStoreKey of the first fileStore */
    final FileStoreKey getFileStoreKey() {
        return m_fileStoreProxies[0].getFileStoreKey();
    }

    /** @return the first fileStore */
    protected FileStore getFileStore() {
        return m_fileStoreProxies[0].getFileStore();
    }

    /**
     * @since 3.7
     */
    final int getNumFileStores() {
        return m_fileStoreProxies.length;
    }

    /**
     * @since 3.7
     */
    final FileStoreKey[] getFileStoreKeys() {
        return Arrays.stream(m_fileStoreProxies).map(FileStoreProxy::getFileStoreKey).toArray(FileStoreKey[]::new);
    }

    /**
     * @since 3.7
     */
    protected FileStore[] getFileStores() {
        return Arrays.stream(m_fileStoreProxies).map(FileStoreProxy::getFileStore).toArray(FileStore[]::new);
    }


    /**
     * @noreference This method is not intended to be referenced by clients.
     * @deprecated use retrieveFileStoreHandlersFrom(keys, repo) instead
     */
    @Deprecated
    final void retrieveFileStoreHandlerFrom(final FileStoreKey key,
                     final FileStoreHandlerRepository fileStoreHandlerRepository) throws IOException {
        retrieveFileStoreHandlersFrom(new FileStoreKey[] {key}, fileStoreHandlerRepository);
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     * @since 3.7
     */
    final void retrieveFileStoreHandlersFrom(final FileStoreKey[] keys,
        final FileStoreHandlerRepository fileStoreHandlerRepository) throws IOException {
        m_fileStoreProxies = new FileStoreProxy[keys.length];
        int fsIdx = 0;
        for (FileStoreKey key : keys) {
            FileStoreProxy proxy = new FileStoreProxy();
            proxy.retrieveFileStoreHandlerFrom(key, fileStoreHandlerRepository);
            m_fileStoreProxies[fsIdx] = proxy;
            fsIdx++;
        }
        postConstruct();
    }

    /** Called after the cell is deserialized from a stream. Clients
     * can now access the file.
     * @throws IOException If thrown, the cell will be replaced by a missing value in the data stream and
     * an error will be reported to the log.  */
    protected void postConstruct() throws IOException {
        // no op.
    }

    void callFlushIfNeeded() throws IOException {
        if (!m_isFlushedToFileStore) {
            m_isFlushedToFileStore = true;
            flushToFileStore();
        }
    }

    /** Called before the cell is about to be serialized. Subclasses may override it to make sure the content
     * is sync'ed with the file (e.g. in-memory content is written to the FileStore).
     *
     * <p>This method is also called when the file underlying the cell is copied into a another context (from
     * a BufferedDataTable to DataTable).
     * @throws IOException If thrown, the cell will be replaced by a missing value in the data stream and
     * an error will be reported to the log.
     * @since 2.8 */
    protected void flushToFileStore() throws IOException {
        // no op.
    }

    /** @return the isFlushedToFileStore */
    boolean isFlushedToFileStore() {
        return m_isFlushedToFileStore;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(m_fileStoreProxies).forEachOrdered(fsp -> builder.append(fsp.toString() + " "));
        return builder.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        FileStoreProxy[] otherFileStoreProxies = ((FileStoreCell)dc).m_fileStoreProxies;
        for (int fsIdx = 0; fsIdx < m_fileStoreProxies.length; fsIdx++) {
            if (!otherFileStoreProxies[fsIdx].equals(m_fileStoreProxies[fsIdx])) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(m_fileStoreProxies);
    }

}
