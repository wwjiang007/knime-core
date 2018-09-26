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
 *   09.02.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A dialog panel used to set color for nominal values.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class ColorManager2DialogNominal extends JPanel {

    /** Keeps mapping from data cell name to color. */
    private final Map<String, Map<DataCell, ColorAttr>> m_map;

    /** Keeps the all possible column values. */
    private final JList m_columnValues;

    /** list model for column values. */
    private final DefaultListModel m_columnModel;

    private int m_alpha = 255;

    /**
     * Creates an empty nominal dialog.
     */
    ColorManager2DialogNominal() {
        super(new GridLayout());

        // map for key to color mapping
        m_map = new LinkedHashMap<String, Map<DataCell, ColorAttr>>();


        // create list for possible column values
        m_columnModel = new DefaultListModel();
        m_columnValues = new JList(m_columnModel);
        m_columnValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_columnValues.setCellRenderer(new ColorManager2IconRenderer());
        super.add(new JScrollPane(m_columnValues));
    }

    /**
     * Called is a new column is selected. If the column is null every
     *
     * @param column the new selected column
     * @return <code>true</code>, if the call caused any changes
     */
    boolean select(final String column) {

        m_columnModel.removeAllElements();
        Map<DataCell, ColorAttr> map = m_map.get(column);
        boolean flag;
        if (map == null) {
            m_columnModel.removeAllElements();
            m_columnValues.setEnabled(false);
            flag = false;
        } else {
            m_columnValues.setEnabled(true);
            for (DataCell cell : map.keySet()) {
                assert cell != null;
                ColorAttr color = map.get(cell);
                assert color != null;
                m_columnModel.addElement(new ColorManager2Icon(cell, color.getColor()));
            }
            flag = true;
        }
        super.validate();
        super.repaint();
        return flag;
    }

    /**
     * Select new color for the selected attribute value of the selected column.
     *
     * @param column the selected column
     * @param color the new color
     */
    void update(final String column, final ColorAttr color) {
        Object o = m_columnValues.getSelectedValue();
        if (o != null) {
            Map<DataCell, ColorAttr> map = m_map.get(column);
            ColorManager2Icon icon = (ColorManager2Icon)o;
            map.put(icon.getCell(), color);
            icon.setColor(color.getColor());
            super.validate();
            super.repaint();
        }
    }

    /**
     * Apply new colors of a given palette of the selected column.
     *
     * @param column the selected column
     * @param palette the new palette
     */
    void updateWithPalette(final String column, final String[] palette) {
        Map<DataCell, ColorAttr> map = m_map.get(column);
        int i = 0;
        ColorAttr color;
        for (Object o : m_columnModel.toArray()) {
            if (i >= palette.length) {
                i = 0;
            }
            ColorManager2Icon icon = (ColorManager2Icon)o;
            color = ColorAttr.getInstance(Color.decode(palette[i]));
            icon.setColor(color.getColor());
            map.put(icon.getCell(), color);
            i++;
        }

        super.validate();
        super.repaint();

    }

    /**
     * Adds the given set of possible values to the internal structure by the given column name.
     *
     * @param column the column name
     * @param set the set of possible values for this column
     * @param newValues Setting for dealing with new values
     */
    void add(final String column, final Set<DataCell> set, final String newValues) {
        if (set != null && !set.isEmpty()) {
//            Map<DataCell, ColorAttr> newColors = createColorMapping(set, m_map.get(column), newValues);
//            m_map.put(column, newColors);
            Map<DataCell, ColorAttr> map = new HashMap<DataCell, ColorAttr>();
            for (DataCell cell : set) {
                map.put(cell, null);
            }
            m_map.put(column, map);
        }
    }

    /**
     * Create default color mapping for the given set of possible <code>DataCell</code> values.
     *
     * @param set set of values to be assigned a color
     * @param existingSet map containing already existing values and colors
     * @param newValues string representing a setting for dealing with new values
     * @return a map of possible value to color
     */
    static final Map<DataCell, ColorAttr> createColorMapping(final Set<DataCell> set, final Map<DataCell, ColorAttr> existingSet, final String newValues) {
        if (set == null) {
            return Collections.EMPTY_MAP;
        }
        Map<DataCell, ColorAttr> map = new LinkedHashMap<DataCell, ColorAttr>();
        String[] palette = null;
        switch (newValues) {
            case ColorManager2NodeModel.FAIL:
                palette = ColorManager2NodeDialogPane.PALETTE_SET1;
                break;
            case ColorManager2NodeModel.SET1:
                palette = ColorManager2NodeDialogPane.PALETTE_SET1;
                break;
            case ColorManager2NodeModel.SET2:
                palette = ColorManager2NodeDialogPane.PALETTE_SET2;
                break;
            case ColorManager2NodeModel.SET3:
                palette = ColorManager2NodeDialogPane.PALETTE_SET3;
                break;
        }
        // analyze existing color mapping
        // Map holding all colors of the palette and how often they appear in the existing Set
        Map<Color, Integer> existingColors = new LinkedHashMap<Color, Integer>();
        for (String s : palette) {
            existingColors.put(Color.decode(s), 0);
        }
        for (ColorAttr col : existingSet.values()) {
            // check if Color is included in the palette
            Color c = col.getColor();
            // increase count if colors exists in palette
            if (existingColors.get(c) != null) {
                existingColors.put(c, existingColors.get(c) + 1);
            }
        }
//        System.out.println("EC: " + existingColors.values());


        for (DataCell cell : set) {
            Entry<Color, Integer> smallestEntry = existingColors.entrySet().stream().min(Map.Entry.comparingByValue(Integer::compareTo)).get();
//            System.out.println("#"+Integer.toHexString(smallestEntry.getKey().getRGB()).substring(2) + "|" + smallestEntry.getValue());
            existingColors.put(smallestEntry.getKey(), smallestEntry.getValue() + 1);
            map.put(cell, ColorAttr.getInstance(smallestEntry.getKey()));
        }
        return map;
    }
    /**
     * Create default color mapping for the given set of possible <code>DataCell</code> values.
     *
     * @param set Set of values to be assigned a color
     * @param String Setting for dealing with new values
     * @return a map of possible value to color
     */
    static final Map<DataCell, ColorAttr> createColorMapping(final Set<DataCell> set, final String newValues) {
        if (set == null) {
            return Collections.EMPTY_MAP;
        }
        Map<DataCell, ColorAttr> map = new LinkedHashMap<DataCell, ColorAttr>();
        int idx = 0;
        String[] palette = null;
        switch (newValues) {
            case ColorManager2NodeModel.FAIL:
                palette = ColorManager2NodeDialogPane.PALETTE_SET1;
                break;
            case ColorManager2NodeModel.SET1:
                palette = ColorManager2NodeDialogPane.PALETTE_SET1;
                break;
            case ColorManager2NodeModel.SET2:
                palette = ColorManager2NodeDialogPane.PALETTE_SET2;
                break;
            case ColorManager2NodeModel.SET3:
                palette = ColorManager2NodeDialogPane.PALETTE_SET3;
                break;
        }
        for (DataCell cell : set) {
            if (idx >= palette.length) {
                idx = 0;
            }
            Color color = Color.decode(palette[idx]);
            map.put(cell, ColorAttr.getInstance(color));
            idx++;
        }
        return map;
    }

    /**
     * Removes all elements for the internal map.
     */
    void removeAllElements() {
        m_map.clear();
        m_columnModel.removeAllElements();
        this.setEnabled(false);
    }

    /**
     * Save settings that are the current color settings.
     *
     * @param settings to write to
     * @throws InvalidSettingsException if no nominal value are defined on the selected column
     */
    void saveSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        int len = m_columnModel.getSize();
        if (len > 0) {
            DataCell[] vals = new DataCell[len];
            for (int i = 0; i < m_columnModel.getSize(); i++) {
                ColorManager2Icon icon = (ColorManager2Icon)m_columnModel.getElementAt(i);
                vals[i] = icon.getCell();
                Color c = icon.getColor();
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), m_alpha);
                settings.addInt(vals[i].toString(), c.getRGB());
            }
            settings.addDataCellArray(ColorManager2NodeModel.VALUES, vals);
        } else {
            throw new InvalidSettingsException(
                "Make sure the selected column has nominal values defined within the domain.");
        }
    }

    /**
     * Reads the color settings for the given column.
     *
     * @param settings to read from
     * @param column the selected column
     */
    void loadSettings(final NodeSettingsRO settings, final String column) {
        if (column == null) {
            return;
        }
        // saved and assigned values in the settings
        DataCell[] vals = settings.getDataCellArray(ColorManager2NodeModel.VALUES, (DataCell[])null);
        if (vals == null) {
            return;
        }
        // all values from the column
        Map<DataCell, ColorAttr> map = m_map.get(column);
        if (map == null) {
            return;
        }
        // list of all values with unset colors
        ArrayList<DataCell> list = new ArrayList<DataCell>();
        Map<DataCell, ColorAttr> existingSet = new HashMap<DataCell, ColorAttr>();
        // replace all colors from settings in map
        for (DataCell d : map.keySet()) {
            // is value part of the saved values?
            int i = Arrays.asList(vals).indexOf(d);
            if (i != -1) {
//                Color dftColor = map.get(vals[i]).getColor();
                int c = settings.getInt(d.toString(), 0);
                Color color = new Color(c, true);
                m_alpha = color.getAlpha();
                color = new Color(color.getRGB(), false);
                map.put(d, ColorAttr.getInstance(color));
                existingSet.put(d, ColorAttr.getInstance(color));
            } else {
                list.add(d);
            }
        }
        String newValues = null;
        try {
            newValues = settings.getString(ColorManager2NodeModel.NEW_VALUES);
        } catch (InvalidSettingsException e) {
            System.out.println("New value handling setting not available");

        }
        Map<DataCell, ColorAttr> m = createColorMapping(new HashSet<DataCell>(list), existingSet, newValues);
        map.putAll(m);
//        System.out.println("NN: " + m);
    }

    /**
     * @return intermediate alpha value as read from the current settings
     */
    final int getAlpha() {
        return m_alpha;
    }

    /**
     * @param alpha the new alpha value as set by the alpha color panel
     */
    final void setAlpha(final int alpha) {
        m_alpha = alpha;
    }
}
