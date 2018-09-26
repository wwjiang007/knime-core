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
 *   Aug 8, 2008 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.data.container.DefaultTableStoreReader.FromFileIterator;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.node.NodeLogger;

/**
 * File iterator to read stream written by a {@link Buffer}.
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("javadoc")
final class BufferFromFileIteratorVersion20 extends FromFileIterator {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(BufferFromFileIteratorVersion20.class);

    /** Associated buffer. */
    private final DefaultTableStoreReader m_tableFormatReader;

    /** Row pointer. */
    private int m_pointer;

    /** Content of the rows that get returned in {@link #next()} when the
     * table is {@link #close()}'d. Will be instantiated lazy. */
    private DataCell[] m_missingCellsForClosedTable;

    /** If an exception has been thrown while reading from this buffer (only
     * if it has been written to disc). If so, further error messages are
     * only written to debug output in order to reduce message spam on the
     * console. */
    private boolean m_hasThrownReadException;

    /** Stream to read from. */
    private DCObjectInputVersion2 m_inStream;

    /** Utility object with designated functionality to deserialize datacell. */
    private DataCellStreamReader m_dataCellStreamReader;

    /** Inits iterator, opens input stream.
     * @param tableFormatReader The associated buffer.
     * @throws IOException If stream reading fails.
     */
    BufferFromFileIteratorVersion20(final DefaultTableStoreReader tableFormatReader) throws IOException {
        m_pointer = 0;
        if (tableFormatReader.getBinFile() == null) {
            throw new IOException("Unable to read table from file, "
                    + "table has been cleared.");
        }
        m_tableFormatReader = tableFormatReader;
        assert m_tableFormatReader.getReadVersion() >= 6 : "Iterator is not backward "
            + "compatible, use instead "
            + BufferFromFileIteratorVersion1x.class.getSimpleName();
        BufferedInputStream bufferedStream =
            new BufferedInputStream(new FileInputStream(tableFormatReader.getBinFile()));
        InputStream in;
        switch (tableFormatReader.getBinFileCompressionFormat()) {
            case Gzip:
                in = new GZIPInputStream(bufferedStream);
                // buffering is important when reading gzip streams
                in = new BufferedInputStream(in);
                break;
            case None:
                in = bufferedStream;
                break;
            default:
                throw new IOException("Unsupported compression format: " + tableFormatReader.getBinFileCompressionFormat());
        }
        m_dataCellStreamReader = new DataCellStreamReader(tableFormatReader);
        m_inStream = new DCObjectInputVersion2(in, m_dataCellStreamReader);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean hasNext() {
        boolean hasNext = m_pointer < m_tableFormatReader.size();
        if (!hasNext && (m_inStream != null)) {
            close();
        }
        return hasNext;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized BlobSupportDataRow next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Iterator at end");
        }
        final DCObjectInputVersion2 inStream = m_inStream;
        int colCount = m_tableFormatReader.getTableSpec().getNumColumns();
        if (inStream == null) { // iterator was closed
            if (m_missingCellsForClosedTable == null) {
                m_missingCellsForClosedTable = new DataCell[colCount];
                Arrays.fill(m_missingCellsForClosedTable,
                        DataType.getMissingCell());
                LOGGER.warn("Invalid access on table, "
                        + "iterator has been closed");
            }
            RowKey key = new RowKey("INVALID_ROW (table is closed) - (Row " + m_pointer + ")");
            m_pointer++;
            return new BlobSupportDataRow(key, m_missingCellsForClosedTable);
        }
        RowKey key;
        try {
            key = readRowKeyAndEndBlock(inStream);
        } catch (Exception throwable) {
            handleReadThrowable(throwable);
            // can't ensure that we generate a unique key but it should
            // cover 99.9% of all cases
            String keyS = "Read_failed__auto_generated_key_" + m_pointer;
            key = new RowKey(keyS);
        }
        DataCell[] cells = new DataCell[colCount];
        for (int i = 0; i < colCount; i++) {
            DataCell nextCell;
            try {
                try {
                    nextCell = m_dataCellStreamReader.readDataCell(m_inStream);
                } finally {
                    m_inStream.endBlock();
                }
            } catch (final Exception e) {
                handleReadThrowable(e);
                nextCell = DataType.getMissingCell();
            }
            cells[i] = nextCell;
        }
        try {
            byte eoRow = inStream.readControlByte();
            if (eoRow != BYTE_ROW_SEPARATOR) {
                throw new IOException("Expected end of row byte, "
                    + "got '" + eoRow + "', (byte " + (int)eoRow + ")");
            }
        } catch (IOException ioe) {
            handleReadThrowable(ioe);
        } finally {
            m_pointer++;
        }
        return new BlobSupportDataRow(key, cells);
    }

    /** Reads a row key from the stream and ends the block. In case of buffers
     * that don't persist their row keys ({@link NoKeyBuffer}), it returns
     * a static key.
     * @param inStream To read from
     * @return The row key as read right from the stream.
     * @throws IOException If reading fails for IO problems.
     */
    private RowKey readRowKeyAndEndBlock(
            final DCObjectInputVersion2 inStream) throws IOException {
        if (!m_tableFormatReader.isReadRowKey()) {
            return DUMMY_ROW_KEY;
        }
        try {
            return inStream.readRowKey();
        } finally {
            inStream.endBlock();
        }
    }

    /** Handle exceptions, make sure to issue errors only once. */
    private void handleReadThrowable(final Throwable throwable) {
        String warnMessage = "Errors while reading row " + (m_pointer + 1)
            + " from file \"" + m_tableFormatReader.getBinFile().getName() + "\": "
            + throwable.getMessage();
        if (!m_hasThrownReadException) {
            warnMessage = warnMessage.concat(
                    "; Suppressing further warnings.");
            LOGGER.error(warnMessage, throwable);
        } else {
            LOGGER.debug(warnMessage, throwable);
        }
        if (!(throwable instanceof IOException)) {
            String messageCoding = throwable.getClass().getSimpleName()
            + " caught, implementation may only throw IOException.";
            if (!m_hasThrownReadException) {
                LOGGER.coding(messageCoding);
            } else {
                LOGGER.debug(messageCoding);
            }
        }
        m_hasThrownReadException = true;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean performClose() throws IOException {
        // already closed (clear has been called before)
        if (m_inStream == null) {
            return false;
        }
        DCObjectInputVersion2 in = m_inStream;
        m_inStream = null;
        in.close();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void finalize() throws Throwable {
        /* This all relates much to bug #63: The temp files are not
         * deleted under windows. It seems that there are open streams
         * when the VM closes. */
        super.finalize();
        close();
    }

    /**
     * Reads the blob from the given blob address.
     * @param blobAddress The address to read from.
     * @param cl The expected class.
     * @return The blob cell being read.
     * @throws IOException If that fails.
     */
    static BlobDataCell readBlobDataCell(final BlobAddress blobAddress, final CellClassInfo cl, final Buffer buffer)
        throws IOException {
        assert buffer.getBufferID() == blobAddress.getBufferID() : "Buffer IDs don't match: " + buffer.getBufferID()
            + " vs. " + blobAddress.getBufferID();
        int column = blobAddress.getColumn();
        int indexInColumn = blobAddress.getIndexOfBlobInColumn();
        boolean isCompress = blobAddress.isUseCompression();
        File inFile = buffer.getBlobFile(indexInColumn, column, false, isCompress);
        InputStream in = new BufferedInputStream(new FileInputStream(inFile));
        if (isCompress) {
            in = new GZIPInputStream(in);
            // that buffering is important
            in = new BufferedInputStream(in);
        }
        Class<? extends DataCell> cellClass = cl.getCellClass();
        DataCellSerializer<? extends DataCell> ser = cl.getSerializer();
        DCObjectInputVersion2 inStream = new DCObjectInputVersion2(in);
        BlobDataCell result;
        try {
            if (ser != null) {
                // the DataType class will reject Serializer that do not
                // have the appropriate return type
                result = (BlobDataCell)inStream.readDataCellPerKNIMESerializer(ser);
            } else {
                inStream.setCurrentClassLoader(cellClass.getClassLoader());
                result = (BlobDataCell)inStream.readDataCellPerJavaSerialization();
            }
            result.setBlobAddress(blobAddress);
            return result;
        } finally {
            inStream.close();
        }
    }

    /** Utility class that separates the logic of reading DataCells from the stream. */
    static class DataCellStreamReader {

        /** Associated buffer. */
        private final DefaultTableStoreReader m_tableFormatReader;

        /** Only memorizes the table reader.
         * @param tableFormatReader associated reader, possibly be null. */
        DataCellStreamReader(final DefaultTableStoreReader tableFormatReader) {
            m_tableFormatReader = tableFormatReader;
        }

        /** Reads a data cell from the argument stream. Does not exception
         * handling, nor stream blocking.
         * @param inStream To read from.
         * @return the data cell being read
         * @throws IOException If exceptions occur.
         */
        DataCell readDataCell(final DCObjectInputVersion2 inStream) throws IOException {
            if(m_tableFormatReader.getReadVersion() > 10) {
                // starting with table version 11 FileStoreCells support multiple FileStores
                return readDataCellVersion2(inStream);
            }
            inStream.setCurrentClassLoader(null);
            byte identifier = inStream.readControlByte();
            if (identifier == BYTE_TYPE_MISSING) {
                return DataType.getMissingCell();
            }
            final boolean isSerialized = identifier == BYTE_TYPE_SERIALIZATION;
            if (isSerialized) {
                identifier = inStream.readControlByte();
            }
            CellClassInfo type = m_tableFormatReader.getTypeForChar(identifier);
            Class<? extends DataCell> cellClass = type.getCellClass();
            boolean isFileStore = FileStoreCell.class.isAssignableFrom(cellClass);
            final FileStoreKey fileStoreKey;
            if (isFileStore) {
                fileStoreKey = inStream.readFileStoreKey();
            } else {
                fileStoreKey = null;
            }
            boolean isBlob = BlobDataCell.class.isAssignableFrom(cellClass);
            final DataCell result;
            if (isBlob) {
                result = m_tableFormatReader.createBlobWrapperCell(inStream.readBlobAddress(), type);
            } else if (isSerialized) {
                ClassLoader cellLoader = cellClass.getClassLoader();
                inStream.setCurrentClassLoader(cellLoader);
                result = inStream.readDataCellPerJavaSerialization();
            } else {
                DataCellSerializer<? extends DataCell> serializer =
                    type.getSerializer();
                assert serializer != null;
                result = inStream.readDataCellPerKNIMESerializer(serializer);
            }

            if (fileStoreKey != null) {
                FileStoreCell fsCell = (FileStoreCell)result;
                FileStoreUtil.retrieveFileStoreHandlersFrom(fsCell,
                        new FileStoreKey[] {fileStoreKey}, m_tableFormatReader.getFileStoreHandlerRepository());
            }

            return result;
        }

        /** Reads a data cell from the argument stream. Does not perform exception
         * handling, nor stream blocking.
         * @since 3.7
         * @param inStream To read from.
         * @return the data cell being read
         * @throws IOException If exceptions occur.
         */
        DataCell readDataCellVersion2(final DCObjectInputVersion2 inStream) throws IOException {
            inStream.setCurrentClassLoader(null);
            byte identifier = inStream.readControlByte();
            if (identifier == BYTE_TYPE_MISSING) {
                return DataType.getMissingCell();
            }
            final boolean isSerialized = identifier == BYTE_TYPE_SERIALIZATION;
            if (isSerialized) {
                identifier = inStream.readControlByte();
            }
            CellClassInfo type = m_tableFormatReader.getTypeForChar(identifier);
            Class<? extends DataCell> cellClass = type.getCellClass();
            boolean isFileStore = FileStoreCell.class.isAssignableFrom(cellClass);
            final FileStoreKey[] fileStoreKeys;
            if (isFileStore) {
                fileStoreKeys = inStream.readFileStoreKeys();
            } else {
                fileStoreKeys = null;
            }
            boolean isBlob = BlobDataCell.class.isAssignableFrom(cellClass);
            final DataCell result;
            if (isBlob) {
                result = m_tableFormatReader.createBlobWrapperCell(inStream.readBlobAddress(), type);
            } else if (isSerialized) {
                ClassLoader cellLoader = cellClass.getClassLoader();
                inStream.setCurrentClassLoader(cellLoader);
                result = inStream.readDataCellPerJavaSerialization();
            } else {
                DataCellSerializer<? extends DataCell> serializer =
                    type.getSerializer();
                assert serializer != null;
                result = inStream.readDataCellPerKNIMESerializer(serializer);
            }

            if (fileStoreKeys != null) {
                FileStoreCell fsCell = (FileStoreCell)result;
                FileStoreUtil.retrieveFileStoreHandlersFrom(fsCell, fileStoreKeys, m_tableFormatReader.getFileStoreHandlerRepository());
            }

            return result;
        }
    } // class DataCellStreamReader

}
