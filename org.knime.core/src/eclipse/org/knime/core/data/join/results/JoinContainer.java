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
 *   Jun 20, 2020 (carlwitt): created
 */
package org.knime.core.data.join.results;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.implementation.JoinImplementation;
import org.knime.core.data.join.implementation.OrderedRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;
import org.knime.core.node.ExecutionContext;

import gnu.trove.set.hash.TLongHashSet;

/**
 * Base class for implementations of the {@link JoinResult} interface. Provides result deduplication capabilities for
 * disjunctive joins.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
@SuppressWarnings("javadoc") // only for bogus warnings: 'protected' visibility for malformed doc comments hides...
abstract class JoinContainer<T> implements JoinResult<T> {

    /** The format of the output table for matches, unmatched rows, etc. */
    final Map<ResultType, DataTableSpec> m_outputSpecs = new EnumMap<>(ResultType.class);

    /**
     * Used to determine the output table specs and perform the actual join operation between two matching rows as well
     * as padding unmatched rows with missing values according to the match table spec.
     *
     * @see JoinSpecification#specForMatchTable()
     */
    final JoinSpecification m_joinSpecification;

    /**
     * For creating tables.
     */
    final ExecutionContext m_exec;

    /**
     * Whether to reject duplicate submissions to {@link #offerMatch(DataRow, long, DataRow, long)}.
     * @see #m_seenMatches
     */
    private boolean m_deduplicateMatches = false;
    /**
     * Contains the offsets of the rows that have been passed to {@link #offerMatch(DataRow, long, DataRow, long)} since
     * the first call to {@link #deduplicateMatches()}.
     *
     * Is null if {@link #m_deduplicateMatches} is false.
     */
    private RowOffsetCombinationSet m_seenMatches = null;
    /**
     * Handlers for unmatched rows. Either output rows directly or defer until collected.
     */
    private final Map<InputTable, UnmatchedRowCollector> m_unmatchedRows = new EnumMap<>(InputTable.class);

    /**
     * A mapping for each input table that associates the {@link RowKey} of an input row to the {@link RowKey}s of the
     * output rows that involve this row. The output row can be either a match (when it combines the input row with
     * another row) or an unmatched row. If hiliting is disabled, this field is null.
     */
    private final Map<InputTable, Map<ResultType, Map<RowKey, Set<RowKey>>>> m_hiliteMapping;

    private final CancelChecker m_checkCanceled;

    /**
     * @param joinImplementation contains the {@link JoinSpecification} according to which the tables are combined and
     *            additional configuration such as {@link JoinImplementation#isEnableHiliting()}.
     */
    JoinContainer(final JoinImplementation joinImplementation) {
        m_joinSpecification = joinImplementation.getJoinSpecification();
        m_exec = joinImplementation.getExecutionContext();
        m_checkCanceled = CancelChecker.checkCanceledPeriodically(m_exec);

        m_unmatchedRows.put(InputTable.LEFT, UnmatchedRowCollector.passThrough());
        m_unmatchedRows.put(InputTable.RIGHT, UnmatchedRowCollector.passThrough());

        m_outputSpecs.put(ResultType.LEFT_OUTER, m_joinSpecification.specForUnmatched(InputTable.LEFT));
        m_outputSpecs.put(ResultType.RIGHT_OUTER, m_joinSpecification.specForUnmatched(InputTable.RIGHT));
        m_outputSpecs.put(ResultType.MATCHES, m_joinSpecification.specForMatchTable());

        if (joinImplementation.isEnableHiliting()) {
            m_hiliteMapping = new EnumMap<>(InputTable.class);

            m_hiliteMapping.put(InputTable.LEFT, new EnumMap<>(ResultType.class));
            m_hiliteMapping.get(InputTable.LEFT).put(ResultType.MATCHES, new HashMap<>());
            m_hiliteMapping.get(InputTable.LEFT).put(ResultType.LEFT_OUTER, new HashMap<>());

            m_hiliteMapping.put(InputTable.RIGHT, new EnumMap<>(ResultType.class));
            m_hiliteMapping.get(InputTable.RIGHT).put(ResultType.MATCHES, new HashMap<>());
            m_hiliteMapping.get(InputTable.RIGHT).put(ResultType.RIGHT_OUTER, new HashMap<>());
        } else {
            m_hiliteMapping = null;
        }
    }

    @Override
    public void deferUnmatchedRows(final InputTable side) {
        boolean collectUnmatched = m_joinSpecification.getSettings(side).isRetainUnmatched();
        boolean isAleradyDeferred = m_unmatchedRows.get(side) instanceof DeferredUnmatchedRowCollector;
        if (collectUnmatched && !isAleradyDeferred) {
            final BufferedDataTable table =
                m_joinSpecification.getSettings(side).getTable().orElseThrow(IllegalStateException::new);
            m_unmatchedRows.put(side, new DeferredUnmatchedRowCollector(table, m_checkCanceled));
        }
    }

    @Override
    public void lowMemory() {
        m_unmatchedRows.values().forEach(UnmatchedRowCollector::lowMemory);
    }

    /**
     * Offers a pair of rows for which all join criteria are fulfilled. The match might be rejected if
     * {@link #deduplicateMatches()} was called before and a match with an identical (row offset in left table, row
     * offset in right table) pair was added before.
     *
     * @param left a row from the left input table
     * @param leftOrder left row's offset in the left table
     * @param right a row from the right input table
     * @param rightOrder analogous
     */
    @Override
    public void offerMatch(final DataRow left, final long leftOrder, final DataRow right, final long rightOrder) {

        m_unmatchedRows.get(InputTable.LEFT).matched(leftOrder);
        m_unmatchedRows.get(InputTable.RIGHT).matched(rightOrder);

        if (isRetainMatched() && !isDuplicateMatch(leftOrder, rightOrder)) {
            // if deduplication is on, only add if the row offset combination wasn't added before
            doAddMatch(left, leftOrder, right, rightOrder);
        }
    }

    /**
     * @return true if this method has been called before with the same combination of matching rows (as identified by
     *         their offsets in the containing table)
     */
    private boolean isDuplicateMatch(final long leftOrder, final long rightOrder) {
        return m_deduplicateMatches && !m_seenMatches.put(leftOrder, rightOrder);
    }

    /**
     * Adds this row to the unmatched rows from the left table, if {@link #isRetainUnmatched(InputTable)} for
     * {@link InputTable#LEFT}, otherwise does nothing.
     *
     * @param row a row from the left input table
     * @param offset sort order of the row, e.g., row offset in the table
     */
    @Override
    public void offerLeftOuter(final DataRow row, final long offset) {

        UnmatchedRowCollector handler = m_unmatchedRows.get(InputTable.LEFT);

        // the handler may return false if the row has been matched in an earlier pass over the data
        if (handler.unmatched(row, offset) && isRetainUnmatched(InputTable.LEFT)) {
            doAddLeftOuter(row, offset);
        }
    }

    /**
     * Adds this row to the unmatched rows from the right table, if {@link #isRetainUnmatched(InputTable)} for
     * {@link InputTable#RIGHT}, otherwise does nothing.
     *
     * @param row a row from the right input table
     * @param offset sort order of the row, e.g., row offset in the table
     */
    @Override
    public void offerRightOuter(final DataRow row, final long offset) {

        UnmatchedRowCollector handler = m_unmatchedRows.get(InputTable.RIGHT);

        // the handler may return false if the row has been matched in an earlier pass over the data
        if (handler.unmatched(row, offset) && isRetainUnmatched(InputTable.RIGHT)) {
            doAddRightOuter(row, offset);
        }
    }

    /**
     * Associate an input row to a row in the output.
     *
     * @param resultType whether the output row describes a match or an unmatched row
     * @param outputRowKey the ID of the output row
     * @param side whether the input row comes from the left or the right input table
     * @param inputRowKey the ID of the input row
     */
    void addHiliteMapping(final ResultType resultType, final RowKey outputRowKey, final InputTable side,
        final RowKey inputRowKey) {
        // if hiliting is not enabled, do nothing
        // otherwise associate the output row's key to the input row's key
        if (m_hiliteMapping != null) {
            Set<RowKey> associatedRowKeys =
                m_hiliteMapping.get(side).get(resultType).computeIfAbsent(outputRowKey, key -> new HashSet<>());
            associatedRowKeys.add(inputRowKey);
        }
    }

    @Override
    public boolean isRetainMatched() {
        return m_joinSpecification.isRetainMatched();
    }

    @Override
    public boolean isRetainUnmatched(final InputTable side) {
        return m_joinSpecification.isRetainUnmatched(side);
    }

    @Override
    public Optional<Map<RowKey, Set<RowKey>>> getHiliteMapping(final InputTable side, final ResultType resultType) {

        if (m_hiliteMapping == null) {
            return Optional.empty();
        }

        // combine maps for matches and unmatched rows into a single map
        if (resultType == ResultType.ALL) {
            final Map<RowKey, Set<RowKey>> combinedMap = new HashMap<>();
            // copy all entries from the matches map
            combinedMap.putAll(m_hiliteMapping.get(side).get(ResultType.MATCHES));
            // add all entries of the left/right unmatched output rows
            for (Map<RowKey, Set<RowKey>> unmatchedMap : m_hiliteMapping.get(side).values()) {
                // add each mapping to the combined map
                unmatchedMap.forEach((inputRowKey, outputRowKeys) -> {
                    // the output rows already associated to the given input row
                    Set<RowKey> existingMappings = combinedMap.computeIfAbsent(inputRowKey, key -> new HashSet<>());
                    // extend by the mappings from this result type
                    existingMappings.addAll(outputRowKeys);
                });
            }
            return Optional.of(combinedMap);
        } else {
            return Optional.of(m_hiliteMapping.get(side).get(resultType));
        }
    }

    /**
     * Once no more rows are added to the {@link JoinContainer}, the unmatched rows are collected prior to creating the
     * output tables. If unmatched rows are directly output, e.g., using {@link UnmatchedRowCollector#passThrough()}) as
     * {@link UnmatchedRowCollector}, this will do nothing. When using {@link DeferredUnmatchedRowCollector}, this will now add the
     * unmatched rows that were on hold to the results.
     *
     * @param side whether to collect the left or right unmatched rows
     * @throws CanceledExecutionException
     */
    void collectUnmatchedRows(final InputTable side) throws CanceledExecutionException {
        // collection happens only once, so no offerings anymore: do add the unmatched rows to the container now.
        final RowHandler handler = side.isLeft() ? this::doAddLeftOuter : this::doAddRightOuter;
        m_unmatchedRows.get(side).collectUnmatched(handler);
    }

    void collectUnmatchedRows(final ResultType resultType) throws CanceledExecutionException {
        boolean canContainLeftUnmatched = resultType == ResultType.LEFT_OUTER || resultType == ResultType.ALL;
        if (canContainLeftUnmatched && isRetainUnmatched(InputTable.LEFT)) {
            collectUnmatchedRows(InputTable.LEFT);
        }
        boolean canContainRightUnmatched = resultType == ResultType.RIGHT_OUTER || resultType == ResultType.ALL;
        if (canContainRightUnmatched && isRetainUnmatched(InputTable.RIGHT)) {
            collectUnmatchedRows(InputTable.RIGHT);
        }
    }


    /**
     * Stores whether a given row offset combination was seen before. Is used to determine whether there was a previous
     * invocation of {@link JoinContainer#addMatch(DataRow, long, DataRow, long)},
     * {@link JoinContainer#addLeftOuter(DataRow, long)}, or {@link JoinContainer#addRightOuter(DataRow, long)}. In case
     * there was a previous invocation with the same row offsets and {@link JoinContainer#isDeduplicateResults()}, the
     * join result is rejected.
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     */
    private static class RowOffsetCombinationSet {

        private final TLongHashSet m_set = new TLongHashSet();

        /**
         * @param leftRowOffset a 32-bit unsigned integer, i.e., at most ~4G
         * @param rightRowOffset a 32-bit unsigned integer, i.e., at most ~4G
         * @return whether the combination of row offsets is new, i.e., hasn't been put before
         */
        boolean put(final long leftRowOffset, final long rightRowOffset) {
            // combine both offsets into a single long to avoid Pair<> or similar
            long combinedOffsets = OrderedRow.combinedOffsets(leftRowOffset, rightRowOffset);
            return m_set.add(combinedOffsets);
        }

    }

    /**
     * After the first call to this method, invocations of
     * {@link JoinContainer#offerMatch(DataRow, long, DataRow, long)} will keep track of the row offsets of matching
     * rows and reject future matches that state the same combination of row offsets. For performance reasons, the
     * {@link DataRow}s will not be compared, just their offsets as passed into
     * {@link JoinContainer#offerMatch(DataRow, long, DataRow, long)}
     *
     * This is used for instance when joining under disjunctive conditions, which may lead to duplicate matches that
     * need to be filtered out.
     */
    @Override
    public void deduplicateMatches() {
        if(! m_deduplicateMatches) {
            m_deduplicateMatches = true;
            m_seenMatches = new RowOffsetCombinationSet();
        }
    }

}
