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
 *   May 19, 2021 (hornm): created
 */
package org.knime.core.node.workflow.def;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBooleanEntry;
import org.knime.core.node.config.base.ConfigByteEntry;
import org.knime.core.node.config.base.ConfigCharEntry;
import org.knime.core.node.config.base.ConfigDoubleEntry;
import org.knime.core.node.config.base.ConfigFloatEntry;
import org.knime.core.node.config.base.ConfigIntEntry;
import org.knime.core.node.config.base.ConfigLongEntry;
import org.knime.core.node.config.base.ConfigShortEntry;
import org.knime.core.node.config.base.ConfigStringEntry;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.ComponentMetadata;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodePort;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowPersistor.ConnectionContainerTemplate;
import org.knime.core.util.Version;
import org.knime.core.util.workflowalizer.AuthorInformation;
import org.knime.shared.workflow.def.AnnotationDataDef;
import org.knime.shared.workflow.def.AuthorInformationDef;
import org.knime.shared.workflow.def.BoundsDef;
import org.knime.shared.workflow.def.ComponentMetadataDef;
import org.knime.shared.workflow.def.ConfigDef;
import org.knime.shared.workflow.def.ConfigMapDef;
import org.knime.shared.workflow.def.ConfigValueBooleanArrayDef;
import org.knime.shared.workflow.def.ConnectionDef;
import org.knime.shared.workflow.def.ConnectionUISettingsDef;
import org.knime.shared.workflow.def.CoordinateDef;
import org.knime.shared.workflow.def.CredentialPlaceholderDef;
import org.knime.shared.workflow.def.JobManagerDef;
import org.knime.shared.workflow.def.NodeAnnotationDef;
import org.knime.shared.workflow.def.NodeLocksDef;
import org.knime.shared.workflow.def.NodeUIInfoDef;
import org.knime.shared.workflow.def.PortDef;
import org.knime.shared.workflow.def.PortTypeDef;
import org.knime.shared.workflow.def.StyleRangeDef;
import org.knime.shared.workflow.def.TemplateInfoDef;
import org.knime.shared.workflow.def.VendorDef;
import org.knime.shared.workflow.def.impl.AnnotationDataDefBuilder;
import org.knime.shared.workflow.def.impl.AuthorInformationDefBuilder;
import org.knime.shared.workflow.def.impl.BoundsDefBuilder;
import org.knime.shared.workflow.def.impl.ComponentMetadataDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigMapDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueBooleanArrayDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueBooleanDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueByteArrayDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueByteDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueCharArrayDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueCharDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueDoubleArrayDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueDoubleDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueFloatArrayDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueFloatDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueIntArrayDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueIntDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueLongArrayDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueLongDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueShortArrayDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueShortDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueStringArrayDefBuilder;
import org.knime.shared.workflow.def.impl.ConfigValueStringDefBuilder;
import org.knime.shared.workflow.def.impl.ConnectionDefBuilder;
import org.knime.shared.workflow.def.impl.ConnectionUISettingsDefBuilder;
import org.knime.shared.workflow.def.impl.CoordinateDefBuilder;
import org.knime.shared.workflow.def.impl.JobManagerDefBuilder;
import org.knime.shared.workflow.def.impl.NodeAnnotationDefBuilder;
import org.knime.shared.workflow.def.impl.NodeLocksDefBuilder;
import org.knime.shared.workflow.def.impl.NodeUIInfoDefBuilder;
import org.knime.shared.workflow.def.impl.PortDefBuilder;
import org.knime.shared.workflow.def.impl.PortTypeDefBuilder;
import org.knime.shared.workflow.def.impl.StyleRangeDefBuilder;
import org.knime.shared.workflow.def.impl.TemplateInfoDefBuilder;
import org.knime.shared.workflow.def.impl.VendorDefBuilder;

/**
 *
 * @author hornm
 */
public class CoreToDefUtil {

    /**
     *
     * TODO use {@link NodeSettingsRO}. The problem is the typed leaf entries (e.g., {@link ConfigBooleanEntry} inherit
     * from {@link AbstractConfigEntry}, whereas {@link NodeSettingsRO}/ {@link ConfigRO}/ {@link ConfigBaseRO} only
     * start from {@link ConfigBase}.
     *
     * @param settings TODO read-only access to a ConfigBase
     * @return the node settings in a representation that can be converted to various formats
     * @throws InvalidSettingsException
     */
    public static ConfigMapDef toConfigMapDef(final ConfigBaseRO settings) throws InvalidSettingsException {

        if (settings == null) {
            return null;
        }

        // TODO don't cast
        ConfigBase config = (ConfigBase)settings;
        return (ConfigMapDef)toConfigDef(config, settings.getKey());
    }

    /**
     *  Converts the ConnectionContainer to def.
     *
     * @param connection a {@link ConnectionContainer}.
     * @return a {@link ConnectionDef}.L
     */
    public static ConnectionDef connectionContainerToConnectionDef(final ConnectionContainer connection) {
        final var uiInfo = Optional.ofNullable(connection.getUIInfo())//
            .map(CoreToDefUtil::toConnectionUISettingsDef)//
            .orElse(null);

        int sourceID = connection.getSource().getIndex();
        int destID = connection.getDest().getIndex();
        switch (connection.getType()) {
            case WFMIN:
                sourceID = -1;
                break;
            case WFMOUT:
                destID = -1;
                break;
            case WFMTHROUGH:
                sourceID = -1;
                destID = -1;
                break;
            default:
                // all handled above
        }

        return new ConnectionDefBuilder()//
            .setSourcePort(connection.getSourcePort())//
            .setSourceID(sourceID)//
            .setDestPort(connection.getDestPort())//
            .setDestID(destID)//
            .setUiSettings(uiInfo)//
            .setDeletable(connection.isDeletable())//
            .build();
    }

    /**
     *
     * @param cct
     * @return
     */
    public static ConnectionDef toConnectionDef(final ConnectionContainerTemplate cct) {

        return new ConnectionDefBuilder()//
                .setDeletable(cct.isDeletable())//
                .setDestID(cct.getDestSuffix())//
                .setDestPort(cct.getDestPort())//
                .setSourceID(cct.getSourceSuffix())//
                .setSourcePort(cct.getSourcePort())//
                .setUiSettings(toConnectionUISettingsDef(cct.getUiInfo()))//
                .build();
    }

    /**
     *
     * @param connectionUIInformation
     * @return
     */
    public static ConnectionUISettingsDef
        toConnectionUISettingsDef(final ConnectionUIInformation connectionUIInformation) {
        if (connectionUIInformation == null) {
            return new ConnectionUISettingsDefBuilder().build();
        }
        List<CoordinateDef> bendPoints = Arrays.stream(connectionUIInformation.getAllBendpoints())//
            .map(p -> createCoordinate(p[0], p[1]))//
            .collect(Collectors.toList());
        return new ConnectionUISettingsDefBuilder()//
            .setBendPoints(bendPoints).build();
    }

    /**
     * Recursive function to create a node settings tree (comprising {@link AbstractConfigEntry}s) from a
     * {@link ConfigDef} tree.
     *
     * @param settings an entity containing the recursive node settings
     * @param key the name of this subtree
     * @throws InvalidSettingsException TODO what about {@link ModelContent}? It's a sibling of {@link NodeSettings}.
     */
    private static ConfigDef toConfigDef(final AbstractConfigEntry settings, final String key)
        throws InvalidSettingsException {

        if (settings instanceof ConfigBase) {
            // this is a subtree, because every class that extends AbstractConfigEntry and is not a subclass of
            // ConfigBase is a leaf class
            ConfigBase subTree = (ConfigBase) settings;

            final Map<String, ConfigDef> children = new LinkedHashMap<>();
            for (String childKey : subTree.keySet()) {
                // some subtrees are arrays in disguise, don't recurse into those
                ConfigDef asArrayDef = tryNodeSettingsAsArray(subTree, childKey);
                if(asArrayDef != null) {
                    children.put(childKey, asArrayDef);
                } else {
                    // recurse
                    ConfigDef subTreeDef = toConfigDef(subTree.getEntry(childKey), childKey);
                    children.put(childKey, subTreeDef);
                }
            }
            return new ConfigMapDefBuilder()//
                    .setKey(key)//
                    .setChildren(children)//
                    .setConfigType("ConfigMap")//
                    .build();
        } else {
            // recursion anchor
            return abstractConfigurationEntryToTypedLeaf(settings)//
                    .orElseThrow(() -> new IllegalStateException(settings.getKey() + settings.toStringValue()));
        }

    }

    /**
     * @param innerNode
     * @return null if no sensible conversion could be made, otherwise an array representation of the matching type,
     *         like {@link ConfigValueBooleanArrayDef}.
     */
    private static ConfigDef tryNodeSettingsAsArray(final ConfigBase innerNode, final String childKey) { // NOSONAR
        // NOSONAR: recommended number of return statements is <= 5 but we just have to cover all the types.

        boolean[] booleanValues = innerNode.getBooleanArray(childKey, null);
        if (booleanValues != null) {
            List<Boolean> asList = IntStream.range(0, booleanValues.length)//
                .mapToObj(idx -> booleanValues[idx])//
                .collect(Collectors.toList());
            return new ConfigValueBooleanArrayDefBuilder()//
                .setArray(asList)//
                .setConfigType("ConfigValueBooleanArray")//
                .build();
        }
        byte[] byteValues = innerNode.getByteArray(childKey, null);
        if (byteValues != null) {
            return new ConfigValueByteArrayDefBuilder()//
                .setArray(byteValues)//
                .setConfigType("ConfigValueByteArray")//
                .build();
        }
        char[] charValues = innerNode.getCharArray(childKey, null);
        if (charValues != null) {
            List<Integer> asList = IntStream.range(0, charValues.length)
                //
                .mapToObj(idx -> Integer.valueOf(charValues[//
                idx])).collect(Collectors.toList());
            return new ConfigValueCharArrayDefBuilder()//
                .setArray(asList)//
                .setConfigType("ConfigValueCharArray")//
                .build();
        }
        double[] doubleValues = innerNode.getDoubleArray(childKey, null);
        if (doubleValues != null) {
            List<Double> asList = IntStream.range(0, doubleValues.length)//
                .mapToObj(idx -> doubleValues[idx])//
                .collect(Collectors.toList());
            return new ConfigValueDoubleArrayDefBuilder()//
                .setArray(asList)//
                .setConfigType("ConfigValueDoubleArray")//
                .build();
        }
        float[] floatValues = innerNode.getFloatArray(childKey, null);
        if (floatValues != null) {
            List<Float> asList = IntStream.range(0, floatValues.length)//
                .mapToObj(idx -> floatValues[idx])//
                .collect(Collectors.toList());
            return new ConfigValueFloatArrayDefBuilder()//
                .setArray(asList)//
                .setConfigType("ConfigValueFloatArray")//
                .build();
        }
        int[] intValues = innerNode.getIntArray(childKey, null);
        if (intValues != null) {
            List<Integer> asList = IntStream.range(0, intValues.length)//
                .mapToObj(idx -> intValues[idx])//
                .collect(Collectors.toList());
            return new ConfigValueIntArrayDefBuilder()//
                .setArray(asList)//
                .setConfigType("ConfigValueIntArray")//
                .build();
        }
        long[] longValues = innerNode.getLongArray(childKey, null);
        if (longValues != null) {
            List<Long> asList = IntStream.range(0, longValues.length)//
                .mapToObj(idx -> longValues[idx])//
                .collect(Collectors.toList());
            return new ConfigValueLongArrayDefBuilder()//
                .setArray(asList)//
                .setConfigType("ConfigValueLongArray")//
                .build();
        }
        short[] shortValues = innerNode.getShortArray(childKey, null);
        if (shortValues != null) {
            List<Integer> asList = IntStream.range(0, shortValues.length)
                //
                .mapToObj((idx -> Integer.valueOf(shortValues[//
                idx]))).collect(Collectors.toList());
            return new ConfigValueShortArrayDefBuilder()//
                .setArray(asList)//
                .setConfigType("ConfigValueShortArray")//
                .build();
        }
        String[] stringValues = innerNode.getStringArray(childKey, (String[])null);
        if (stringValues != null) {
            List<String> asList = IntStream.range(0, stringValues.length)//
                .mapToObj(idx -> stringValues[idx])//
                .collect(Collectors.toList());
            return new ConfigValueStringArrayDefBuilder()//
                .setArray(asList)//
                .setConfigType("ConfigValueStringArray")//
                .build();
        }
        return null;
    }

    private static Optional<ConfigDef> abstractConfigurationEntryToTypedLeaf(final AbstractConfigEntry child) {
        // for children: check whether they are leafs by testing on all leaf types
        if (child instanceof ConfigBooleanEntry) {
            return Optional.of(new ConfigValueBooleanDefBuilder()//
            .setValue(((ConfigBooleanEntry)child).getBoolean())//
            .setConfigType("ConfigValueBoolean")//
            .build());
        } else if (child instanceof ConfigByteEntry) {
            return Optional.of(new ConfigValueByteDefBuilder()//
            .setValue((int)((ConfigByteEntry)child).getByte())//
            .setConfigType("ConfigValueByte")//
            .build());
        } else if (child instanceof ConfigCharEntry) {
            return Optional.of(new ConfigValueCharDefBuilder()//
            .setValue((int)((ConfigCharEntry)child).getChar())//
            .setConfigType("ConfigValueChar")//
            .build());
        } else if (child instanceof ConfigDoubleEntry) {
            return Optional.of(new ConfigValueDoubleDefBuilder()//
            .setValue(((ConfigDoubleEntry)child).getDouble())//
            .setConfigType("ConfigValueDouble")//
            .build());
        } else if (child instanceof ConfigFloatEntry) {
            return Optional.of(new ConfigValueFloatDefBuilder()//
            .setValue(((ConfigFloatEntry)child).getFloat())//
            .setConfigType("ConfigValueFloat")//
            .build());
        } else if (child instanceof ConfigIntEntry) {
            return Optional.of(new ConfigValueIntDefBuilder()//
            .setValue(((ConfigIntEntry)child).getInt())//
            .setConfigType("ConfigValueInt")//
            .build());
        } else if (child instanceof ConfigLongEntry) {
            return Optional.of(new ConfigValueLongDefBuilder()//
            .setValue(((ConfigLongEntry)child).getLong())//
            .setConfigType("ConfigValueLong")//
            .build());
        } /*else if (child instanceof ConfigPasswordEntry) {
            return Optional
                .of(DefaultConfigValuePasswordDef.builder()//
            //
            .setConfigType("ConfigValuePassword")//
            .build());
          } */ else if (child instanceof ConfigShortEntry) {
            return Optional.of(new ConfigValueShortDefBuilder()//
            .setValue((int)((ConfigShortEntry)child).getShort())//
            .setConfigType("ConfigValueShort")//
            .build());
        } else if (child instanceof ConfigStringEntry) {
            return Optional.of(new ConfigValueStringDefBuilder()//
            .setValue(((ConfigStringEntry)child).getString())//
            .setConfigType("ConfigValueString")//
            .build());
        }
        return Optional.empty();
    }

    public static NodeUIInfoDef toNodeUIInfoDef(final NodeUIInformation uiInfoDef) {

        if (uiInfoDef == null) {
            return null;
        }

        int[] bounds = uiInfoDef.getBounds();
        BoundsDef boundsDef = new BoundsDefBuilder()//
            .setLocation(createCoordinate(bounds[0], bounds[1]))//
            .setWidth(bounds[2])//
            .setHeight(bounds[3])//
            .build();

        return new NodeUIInfoDefBuilder()//
            .setBounds(boundsDef)//
            .setHasAbsoluteCoordinates(uiInfoDef.hasAbsoluteCoordinates())//
            .setSymbolRelative(uiInfoDef.isSymbolRelative())//
            .build();
    }

    public static NodeLocksDef toNodeLocksDef(final NodeLocks def) {
        return new NodeLocksDefBuilder()//
            .setHasConfigureLock(def.hasConfigureLock())//
            .setHasDeleteLock(def.hasDeleteLock())//
            .setHasResetLock(def.hasResetLock())//
            .build();
    }

    public static AnnotationDataDef toAnnotationDataDef(final Annotation na) {
        // TODO I've seen this in the wfm wrapper too
        List<StyleRangeDef> styles = Arrays.stream(na.getStyleRanges())
            .map(s -> new StyleRangeDefBuilder().setColor(s.getFgColor()).setFontName(s.getFontName())
                .setFontSize(s.getFontSize()).setFontStyle(s.getFontStyle()).setLength(s.getLength())
                .setStart(s.getStart()).build())
            .collect(Collectors.toList());

        return new AnnotationDataDefBuilder()//
            .setText(na.getText())//
            .setTextAlignment(na.getAlignment().toString())//
            .setBgcolor(na.getBgColor())//
            .setBorderColor(na.getBorderColor())//
            .setBorderSize(na.getBorderSize())//
            .setDefaultFontSize(na.getDefaultFontSize())//
            .setHeight(na.getHeight())//
            .setWidth(na.getWidth())//
            .setLocation(createCoordinate(na.getX(), na.getY()))//
            .setStyles(styles)//
            .build();
    }

    public static NodeAnnotationDef toNodeAnnotationDef(final NodeAnnotation na) {
        return new NodeAnnotationDefBuilder()//
            .setAnnotationDefault(na.getData().isDefault())//
            .setData(toAnnotationDataDef(na))//
            .build();
    }

    public static CoordinateDef createCoordinate(final int x, final int y) {
        return new CoordinateDefBuilder().setX(x).setY(y).build();

    }

    /**
     *  Converts the template info to def.
     *
     * @param info a {@link MetaNodeTemplateInformation}
     * @return a {@link TemplateInfoDef}.
     */
    public static TemplateInfoDef toTemplateInfoDef(final MetaNodeTemplateInformation info) {
        if (info.getSourceURI() != null) {
            return new TemplateInfoDefBuilder() //
                .setUpdatedAt(info.getTimestamp()) //
                .setUri(info.getSourceURI().toString()) //
                .build();
        }
        return new TemplateInfoDefBuilder().setUpdatedAt(info.getTimestamp()).build();
    }

    /**
     * @param p
     * @return
     */
    public static PortDef toPortDef(final NodePort p) {
        PortTypeDef portType = new PortTypeDefBuilder()//
                .setColor(p.getPortType().getColor())//
                .setHidden(p.getPortType().isHidden())//
                .setName(p.getPortType().getName())//
                .setOptional(p.getPortType().isOptional())//
                .setPortObjectClass(p.getPortType().getPortObjectClass().getCanonicalName())//
                .setPortObjectSpecClass(p.getPortType().getPortObjectSpecClass().getCanonicalName())//
                .build();

        return new PortDefBuilder()//
            .setIndex(p.getPortIndex())//
            .setName(p.getPortName())//
            .setPortType(portType)//
            .build();
    }

    public static VendorDef toBundleVendorDef(final NodeAndBundleInformationPersistor p) {
        return new VendorDefBuilder()//
            .setName(p.getBundleName().orElse(null))//
            .setSymbolicName(p.getBundleSymbolicName().orElse(null))//
            .setVendor(p.getBundleVendor().orElse(null))//
            .setVersion(p.getBundleVersion().map(Version::toString).orElse(null))//
            .build();

    }
    public static VendorDef toFeatureVendorDef(final NodeAndBundleInformationPersistor p) {
        return new VendorDefBuilder()//
            .setName(p.getFeatureName().orElse(null))//
            .setSymbolicName(p.getFeatureSymbolicName().orElse(null))//
            .setVendor(p.getFeatureVendor().orElse(null))//
            .setVersion(p.getFeatureVersion().map(Version::toString).orElse(null))//
            .build();
    }

    public static ComponentMetadataDef toComponentMetadataDef(final ComponentMetadata m) {
        return new ComponentMetadataDefBuilder()//
            .setDescription(m.getDescription().orElse(null))//
            .setIcon(m.getIcon().orElse(null))//
//            .setNodeType(m.getNodeType().map(ComponentNodeType::toString).orElse(null))//
            .setInPortNames(m.getInPortNames().map(Arrays::asList).orElse(null))//
            .setInPortDescriptions(m.getInPortDescriptions().map(Arrays::asList).orElse(null))//
            .setOutPortNames(m.getOutPortNames().map(Arrays::asList).orElse(null))//
            .setOutPortDescriptions(m.getOutPortDescriptions().map(Arrays::asList).orElse(null))//
            .build();
    }

    /**
     * @param jobManager
     * @return
     */
    public static JobManagerDef toJobManager(final NodeExecutionJobManager jobManager) {
        if (jobManager == null) {
            return null;
        }

        final NodeSettings ns = new NodeSettings("jobmanager");
        jobManager.save(ns);

        try {
            return new JobManagerDefBuilder()//
                .setFactory(jobManager.getID())//
                .setSettings(CoreToDefUtil.toConfigMapDef(ns))//
                .build();
        } catch (InvalidSettingsException ex) {
            // TODO proper exception handling
            throw new RuntimeException(ex);
        }
    }

//    /**
//     * @param s
//     * @return
//     */
//    public static FlowObjectDef toFlowVariableDef(final FlowVariable variable) {
//        NodeSettings sub = new NodeSettings("FlowVariable");
//        variable.save(sub);
//        try {
//            return new FlowVariableDefBuilder()//
//                .setValue(toConfigMapDef(sub))//
//                .build();
//        } catch (InvalidSettingsException ex) {
//            throw new IllegalArgumentException(
//                "Can not convert flow variable " + variable + " to plain java description.");
//        }
//    }

    /**
     * @param credentials
     * @return
     */
    public static CredentialPlaceholderDef toWorkflowCredentialsDef(final List<Credentials> credentials) {
        // TODO
        throw new NotImplementedException("Credentials storage not implemented yet");
//        return DefaultWorkflowCredentialsDef.builder().build();
    }

    /**
     * @param authorInformation
     * @return
     */
    public static AuthorInformationDef toAuthorInformationDef(final AuthorInformation authorInformation) {
        var builder = new AuthorInformationDefBuilder()//
            .setAuthoredBy(authorInformation.getAuthor())//
            .setAuthoredWhen(authorInformation.getAuthoredDate());//
        authorInformation.getLastEditor().ifPresent(builder::setLastEditedBy);
        authorInformation.getLastEditDate().ifPresent(builder::setLastEditedWhen);

        return builder.build();
    }
}
