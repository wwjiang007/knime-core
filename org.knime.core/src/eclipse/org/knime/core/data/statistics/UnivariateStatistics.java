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
 *   Aug 29, 2022 (Juan Diaz Baquero): created
 */
package org.knime.core.data.statistics;

import static org.knime.core.data.v2.RowReadUtil.readStringValue;
import static org.knime.core.data.v2.TableExtractorUtil.extractData;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.data.statistics.StatisticsExtractors.CentralMomentExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.CountUniqueExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.DoubleSumExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.FirstQuartileExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.KurtosisExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MaximumExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MeanAbsoluteDeviation;
import org.knime.core.data.statistics.StatisticsExtractors.MeanExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MedianExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MinimumExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.QuantileExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.SkewnessExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.StandardDeviationExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.ThirdQuartileExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.VarianceExtractor;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.TableExtractorUtil;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.Pair;

/**
 * Compute univariate statistics for a given double or string column.
 *
 * @author Juan Diaz Baquero
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 * @noextend Not public API, for internal use only.
 * @noreference Not public API, for internal use only.
 */
public final class UnivariateStatistics {

    /**
     * Labels used for every column on the view data
     */
    private static final Set<Statistic> DEFAULT_EXCLUDED_STATISTICS = Set.of(Statistic.K_MOST_COMMON,
        Statistic.QUANTILE_1, Statistic.QUANTILE_5, Statistic.QUANTILE_25, Statistic.QUANTILE_50, Statistic.QUANTILE_75,
        Statistic.QUANTILE_90, Statistic.QUANTILE_95, Statistic.QUANTILE_99, Statistic.SUM, Statistic.STD_DEVIATION,
        Statistic.MEAN_ABSOLUTE_DEVIATION, Statistic.VARIANCE, Statistic.SKEWNESS, Statistic.KURTOSIS);

    private String m_name;

    private String m_type;

    private long m_numberUniqueValues;

    private String m_firstValue;

    private String m_lastValue;

    private String m_commonValues;

    private Double[] m_quantiles = new Double[9];

    private double m_min;

    private double m_max;

    private Optional<Double> m_mean = Optional.empty();

    private Optional<Double> m_meanAbsoluteDeviation = Optional.empty();

    private Optional<Double> m_standardDeviation = Optional.empty();

    private Optional<Double> m_variance = Optional.empty();

    private Optional<Double> m_skewness = Optional.empty();

    private Optional<Double> m_kurtosis = Optional.empty();

    private Optional<Double> m_sum = Optional.empty();

    /**
     * Compute statistics for every column in the input table.
     *
     * @param inputTable The table for whose columns to compute statistics
     * @param exec Execution context
     * @param selectedStatistics The statistics to include
     * @return A table in which each row corresponds to statistics about a column in the input table
     * @throws CanceledExecutionException If cancelled
     */
    public static BufferedDataTable computeStatisticsTable(final BufferedDataTable inputTable,
        final ExecutionContext exec, Collection<Statistic> selectedStatistics) throws CanceledExecutionException {
        var allColumns = inputTable.getSpec().getColumnNames();
        return computeStatisticsTable(inputTable, allColumns, selectedStatistics, exec);
    }

    /**
     * /** Compute statistics for selected columns in the input table.
     *
     * @param inputTable The table for whose columns to compute statistics
     * @param selectedColumns The column names of the input table for which to compute statistics
     * @param selectedStatistics The statistics to include
     * @param exec Execution context
     * @return A table in which each row corresponds to statistics about a selected column in the input table
     * @throws CanceledExecutionException If cancelled
     */
    public static BufferedDataTable computeStatisticsTable(final BufferedDataTable inputTable,
        final String[] selectedColumns, final Collection<Statistic> selectedStatistics, final ExecutionContext exec)
        throws CanceledExecutionException {
        final var statisticsTable = exec.createDataContainer(getStatisticsTableSpec(selectedStatistics));

        final var eligibleCols = Arrays.stream(selectedColumns).filter(name -> {
            var type = inputTable.getDataTableSpec().getColumnSpec(name).getType();
            return type.isCompatible(DoubleValue.class) || type.isCompatible(StringValue.class);
        }).toArray(String[]::new);

        // trivial case -- nothing to do
        if (eligibleCols.length == 0) {
            statisticsTable.close();
            return statisticsTable.getTable();
        }

        // compute statistics for each column individually
        final var selectedColumnTables =
            StatisticsTableUtil.splitTableByColumnNames(inputTable, eligibleCols, true, true, exec);
        for (var columnName : eligibleCols) {
            final var allColumnStatistics = new UnivariateStatistics();
            final var sortedTable = BufferedDataTableSorter.sortTable(selectedColumnTables.get(columnName), 0, exec);
            allColumnStatistics.performStatisticsCalculation(sortedTable, exec);
            statisticsTable.addRowToTable(StatisticsTableUtil.createTableRow(allColumnStatistics, selectedStatistics));
        }
        statisticsTable.close();

        return statisticsTable.getTable();
    }

    public static DataTableSpec getStatisticsTableSpec(Collection<Statistic> selectedStatistics) {
        var colSpecs = Arrays.stream(Statistic.values()) //
            .filter(selectedStatistics::contains) // to preserve original order of enum
            .map(statistic -> new DataColumnSpecCreator(statistic.getName(), statistic.getType()).createSpec()) //
            .toArray(DataColumnSpec[]::new);
        return new DataTableSpec(colSpecs);
    }

    /**
     * Given a single-column table of strings, compute a single-column table of their lengths
     *
     * @param stringTable The input strings
     * @param exec The execution context
     * @return A table of string lengths.
     */
    static BufferedDataTable getStringLengths(final BufferedDataTable stringTable, final ExecutionContext exec) {
        final var spec = stringTable.getSpec();
        final var container = exec.createDataContainer(
            new DataTableSpec(new DataColumnSpecCreator(spec.getColumnNames()[0], IntCell.TYPE).createSpec()));
        try (final var readCursor = stringTable.cursor()) {
            while (readCursor.canForward()) {
                final var row = readCursor.forward();
                final var value = ((StringValue)row.getValue(0)).getStringValue();
                container.addRowToTable(new DefaultRow(row.getRowKey().getString(), value.length()));
            }
        }
        container.close();
        return container.getTable();
    }

    /**
     * Given a single-column table of strings, obtain the first and last string in the table.
     *
     * @param sortedTable The input strings.
     * @return A pair of first and last string in the input table.
     */
    static Pair<String, String> getFirstAndLastString(final BufferedDataTable sortedTable) {
        try (final var readCursor = sortedTable.cursor()) {
            String first = null;
            String last = null;
            if (readCursor.canForward()) {
                first = readStringValue(readCursor.forward(), 0);
            }
            RowRead row = null;
            while (readCursor.canForward()) {
                row = readCursor.forward();
            }
            if (row != null) {
                last = readStringValue(row, 0);
            }
            return new Pair<>(first, last);
        }
    }

    public static List<Statistic> getDefaultStatistics() {
        return Arrays.stream(Statistic.values()).filter(stat -> !DEFAULT_EXCLUDED_STATISTICS.contains(stat)).toList();
    }

    public static String[] getDefaultStatisticsLabels() {
        return getDefaultStatistics().stream().map(Statistic::getName).toArray(String[]::new);
    }

    public static List<Statistic> getAvailableStatistics() {
        return Arrays.stream(Statistic.values()).toList();
    }

    public static String[] getAvailableStatisticsLabels() {
        return Arrays.stream(Statistic.values()).map(Enum::name).toArray(String[]::new);
    }

    /**
     * String-format a list of pairs of data value and absolute frequencies.
     * 
     * @param mostFrequentValues A list of pairs of some unique data values and their respective absolute frequencies.
     * @param type The type of the data values.
     * @param totalNumValues The total number of unique values
     * @return A string of shape "<value> (<absolute-count>; <percentage-count>)"
     */
    public static String[] formatMostFrequentValues(final List<Pair<DataValue, Long>> mostFrequentValues,
        final DataType type, final long totalNumValues) {
        Function<DataValue, String> reader;
        if (type.isCompatible(DoubleValue.class)) {
            reader = dv -> DataValueRendererUtils.formatDouble(((DoubleValue)dv).getDoubleValue());
        } else if (type.isCompatible(StringValue.class)) {
            reader = dv -> ((StringValue)dv).getStringValue();
        } else if (type.isCompatible(CollectionDataValue.class)) {
            reader = dv -> ((CollectionDataValue)dv).stream().map(DataCell::toString)
                .collect(Collectors.joining(", ", "[", "]"));
        } else {
            reader = Object::toString;
        }

        return mostFrequentValues.stream().map(pair -> {
            var value = pair.getFirst();
            var valueReadable = value == null ? "?" : reader.apply(value);
            var absCount = pair.getSecond();
            return String.format("%s (%s; %s)", valueReadable, absCount,
                DataValueRendererUtils.formatPercentage(absCount / (double)totalNumValues));
        }).toArray(String[]::new);
    }

    /**
     * Compute statistics for the given input column.
     *
     * @param inputColumnTable A table containing exactly one column of either {@link DoubleValue} or
     *            {@link StringValue}.
     * @param exec The execution context
     */
    public void performStatisticsCalculation(final BufferedDataTable inputColumnTable, final ExecutionContext exec)
        throws CanceledExecutionException {
        var columnIndex = 0;
        var type = inputColumnTable.getSpec().getColumnSpec(columnIndex).getType();
        var isStringValues = type.isCompatible(StringValue.class);
        // in case of string values, compute some statistics on string lengths
        var sortedInputTable = BufferedDataTableSorter.sortTable(inputColumnTable, 0, exec);
        var numericTable = isStringValues ? getStringLengths(sortedInputTable, exec) : sortedInputTable;

        setName(inputColumnTable.getSpec().getColumnSpec(columnIndex).getName());
        setType(type);

        final var minExtractor = new MinimumExtractor(columnIndex);
        final var maxExtractor = new MaximumExtractor(columnIndex);
        final var qExtractors = new QuantileExtractor[9];
        qExtractors[0] = new QuantileExtractor(columnIndex, 1, 100); // 1%
        qExtractors[1] = new QuantileExtractor(columnIndex, 1, 20); // 5%
        qExtractors[2] = new QuantileExtractor(columnIndex, 1, 10); // 10%
        qExtractors[3] = new FirstQuartileExtractor(columnIndex); // 25%
        qExtractors[4] = new MedianExtractor(columnIndex); // 50%
        qExtractors[5] = new ThirdQuartileExtractor(columnIndex); // 75%
        qExtractors[6] = new QuantileExtractor(columnIndex, 9, 10); // 90%
        qExtractors[7] = new QuantileExtractor(columnIndex, 19, 20); // 95%
        qExtractors[8] = new QuantileExtractor(columnIndex, 99, 100); // 99%
        final var meanExtractor = new MeanExtractor(columnIndex);
        final var sumExtractor = new DoubleSumExtractor(columnIndex);

        // apply extractors
        TableExtractorUtil.extractData(numericTable, minExtractor, maxExtractor, meanExtractor, sumExtractor,
            qExtractors[0], qExtractors[1], qExtractors[2], qExtractors[3], qExtractors[4], qExtractors[5],
            qExtractors[6], qExtractors[7], qExtractors[8]);

        final var mean = meanExtractor.getOutput();
        setMean(mean);
        setSum(numericTable.size() == 0 ? Double.NaN : sumExtractor.getOutput());
        setQuantiles(Stream.of(qExtractors).map(QuantileExtractor::getOutput).toArray(Double[]::new));

        // for unique values, always consider raw values (not string lengths)
        final var countUniqueExtractor = new CountUniqueExtractor();
        TableExtractorUtil.extractData(sortedInputTable, countUniqueExtractor);
        setNumberUniqueValues(countUniqueExtractor.getNumberOfUniqueValues());
        setCommonValues(formatMostFrequentValues(countUniqueExtractor.getMostFrequentValues(10),
            sortedInputTable.getSpec().getColumnSpec(columnIndex).getType(), sortedInputTable.size()));

        if (isStringValues) {
            var firstAndLastString = getFirstAndLastString(sortedInputTable);
            setFirstValue(firstAndLastString.getFirst());
            setLastValue(firstAndLastString.getSecond());
        } else {
            setFirstValue(DataValueRendererUtils.formatDouble(minExtractor.getOutput()));
            setLastValue(DataValueRendererUtils.formatDouble(maxExtractor.getOutput()));
        }

        // further extractors whose initialisation depends on previous results
        final var meanAbsoluteDeviationExtractor = new MeanAbsoluteDeviation(columnIndex, mean);
        final var standardDeviationExtractor = new StandardDeviationExtractor(columnIndex, mean);
        final var biasedVariance = new CentralMomentExtractor(0, mean, 2);
        final var varianceExtractor = new VarianceExtractor(columnIndex, mean);

        extractData(numericTable, meanAbsoluteDeviationExtractor, standardDeviationExtractor, varianceExtractor,
            biasedVariance);

        final var stdDeviation = standardDeviationExtractor.getOutput();
        setMeanAbsoluteDeviation(meanAbsoluteDeviationExtractor.getOutput());
        setStandardDeviation(stdDeviation);
        setVariance(varianceExtractor.getOutput());

        final var skewnessExtractor = new SkewnessExtractor(columnIndex, mean, stdDeviation);
        final var kurtosisExtractor = new KurtosisExtractor(columnIndex, mean, biasedVariance.getOutput());

        extractData(numericTable, skewnessExtractor, kurtosisExtractor);

        setSkewness(skewnessExtractor.getOutput());
        setKurtosis(kurtosisExtractor.getOutput());
    }

    /**
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * The name of the original column
     * 
     * @param name the name to set
     */
    public void setName(final String name) {
        m_name = name;
    }

    /**
     * @return the type
     */
    public String getType() {
        return m_type;
    }

    /**
     * @param type the type to set
     */
    public void setType(final DataType type) {
        m_type = type.toPrettyString();
    }

    /**
     * @return the numberUniqueValues
     */
    public long getNumberUniqueValues() {
        return m_numberUniqueValues;
    }

    /**
     * @param numberUniqueValues
     */
    public void setNumberUniqueValues(final long numberUniqueValues) {
        m_numberUniqueValues = numberUniqueValues;
    }

    /**
     * @return the firstValue
     */
    public String getFirstValue() {
        return m_firstValue;
    }

    /**
     * @param firstValue the firstValue to set
     */
    public void setFirstValue(final String firstValue) {
        m_firstValue = firstValue;
    }

    /**
     * @return the lastValue
     */
    public String getLastValue() {
        return m_lastValue;
    }

    /**
     * @param lastValue the lastValue to set
     */
    public void setLastValue(final String lastValue) {
        m_lastValue = lastValue;
    }

    /**
     * @return the commonValues
     */
    public String getCommonValues() {
        return m_commonValues;
    }

    /**
     * @param items the array of items to concat
     */
    public void setCommonValues(final String[] items) {
        m_commonValues = items.length == 0 ? null : String.join(", ", items);
    }

    /**
     * @return the quantiles
     */
    public Double[] getQuantiles() {
        return m_quantiles;
    }

    /**
     * @param quantiles the quantiles to set
     */
    public void setQuantiles(final Double[] quantiles) {
        m_quantiles = quantiles;
    }

    /**
     * @return the min
     */
    public double getMin() {
        return m_min;
    }

    /**
     * @param min the min to set
     */
    public void setMin(final double min) {
        m_min = min;
    }

    /**
     * @return the max
     */
    public double getMax() {
        return m_max;
    }

    /**
     * @param max the max to set
     */
    public void setMax(final double max) {
        m_max = max;
    }

    /**
     * @return the mean
     */
    public Optional<Double> getMean() {
        return m_mean;
    }

    /**
     * @param mean the mean to set
     */
    public void setMean(final double mean) {
        m_mean = Double.isNaN(mean) ? Optional.empty() : Optional.of(mean);
    }

    /**
     * @return the meanAbsoluteDeviation
     */
    public Optional<Double> getMeanAbsoluteDeviation() {
        return m_meanAbsoluteDeviation;
    }

    /**
     * @param meanAbsoluteDeviation the meanAbsoluteDeviation to set
     */
    public void setMeanAbsoluteDeviation(final double meanAbsoluteDeviation) {
        m_meanAbsoluteDeviation =
            Double.isNaN(meanAbsoluteDeviation) ? Optional.empty() : Optional.of(meanAbsoluteDeviation);
    }

    /**
     * @return the standardDeviation
     */
    public Optional<Double> getStandardDeviation() {
        return m_standardDeviation;
    }

    /**
     * @param standardDeviation the standardDeviation to set
     */
    public void setStandardDeviation(final double standardDeviation) {
        m_standardDeviation = Double.isNaN(standardDeviation) ? Optional.empty() : Optional.of(standardDeviation);

    }

    /**
     * @return the variance
     */
    public Optional<Double> getVariance() {
        return m_variance;
    }

    /**
     * @param variance the variance to set
     */
    public void setVariance(final double variance) {
        m_variance = Double.isNaN(variance) ? Optional.empty() : Optional.of(variance);
    }

    /**
     * @return the skewness
     */
    public Optional<Double> getSkewness() {
        return m_skewness;
    }

    /**
     * @param skewness the skewness to set
     */
    public void setSkewness(final double skewness) {
        m_skewness = Double.isNaN(skewness) ? Optional.empty() : Optional.of(skewness);
    }

    /**
     * @return the kurtosis
     */
    public Optional<Double> getKurtosis() {
        return m_kurtosis;
    }

    /**
     * @param kurtosis the kurtosis to set
     */
    public void setKurtosis(final double kurtosis) {
        m_kurtosis = Double.isNaN(kurtosis) ? Optional.empty() : Optional.of(kurtosis);
    }

    /**
     * @return the sum
     */
    public Optional<Double> getSum() {
        return m_sum;
    }

    /**
     * @param sum the sum to set
     */
    public void setSum(final double sum) {
        m_sum = Double.isNaN(sum) ? Optional.empty() : Optional.of(sum);
    }

    public enum Statistic {
            NAME("Name", StringCell.TYPE), TYPE("Type", StringCell.TYPE),
            NUMBER_UNIQUE_VALUES("# Unique values", LongCell.TYPE), MINIMUM("Minimum", StringCell.TYPE),
            MAXIMUM("Maximum", StringCell.TYPE), K_MOST_COMMON("10 most common values", StringCell.TYPE),
            QUANTILE_1("1% Quantile", DoubleCell.TYPE), QUANTILE_5("5% Quantile", DoubleCell.TYPE),
            QUANTILE_10("10% Quantile", DoubleCell.TYPE), QUANTILE_25("25% Quantile", DoubleCell.TYPE),
            QUANTILE_50("50% Quantile (Median)", DoubleCell.TYPE), QUANTILE_75("75% Quantile", DoubleCell.TYPE),
            QUANTILE_90("90% Quantile", DoubleCell.TYPE), QUANTILE_95("95% Quantile", DoubleCell.TYPE),
            QUANTILE_99("99% Quantile", DoubleCell.TYPE), MEAN("Mean", DoubleCell.TYPE), SUM("Sum", DoubleCell.TYPE),
            MEAN_ABSOLUTE_DEVIATION("Mean Absolute Deviation", DoubleCell.TYPE),
            STD_DEVIATION("Standard Deviation", DoubleCell.TYPE), VARIANCE("Variance", DoubleCell.TYPE),
            SKEWNESS("Skewness", DoubleCell.TYPE), KURTOSIS("Kurtosis", DoubleCell.TYPE);

        private final String m_name;

        private final DataType m_type;

        Statistic(final String name, final DataType type) {
            this.m_name = name;
            this.m_type = type;
        }

        public String getName() {
            return m_name;
        }

        public DataType getType() {
            return m_type;
        }
    }
}
