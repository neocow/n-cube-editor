package com.cedarsoftware.util

import com.cedarsoftware.ncube.NCube
import com.google.common.base.Splitter
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.ALL_GROUPS_KEYS
import static com.cedarsoftware.util.VisualizerConstants.ALL_GROUPS_MAP
import static com.cedarsoftware.util.VisualizerConstants.AXIS_FIELD
import static com.cedarsoftware.util.VisualizerConstants.AXIS_NAME
import static com.cedarsoftware.util.VisualizerConstants.AXIS_TRAIT
import static com.cedarsoftware.util.VisualizerConstants.BREAK
import static com.cedarsoftware.util.VisualizerConstants.CLASS_TRAITS
import static com.cedarsoftware.util.VisualizerConstants.DOUBLE_BREAK
import static com.cedarsoftware.util.VisualizerConstants.GROUPS_TO_SHOW_IN_TITLE
import static com.cedarsoftware.util.VisualizerConstants.NODE_LABEL_LINE_LENGTH_MULTIPLIER
import static com.cedarsoftware.util.VisualizerConstants.NODE_LABEL_MAX_LINE_LENGTH
import static com.cedarsoftware.util.VisualizerConstants.RPM_CLASS_DOT
import static com.cedarsoftware.util.VisualizerConstants.RPM_ENUM
import static com.cedarsoftware.util.VisualizerConstants.R_EXISTS
import static com.cedarsoftware.util.VisualizerConstants.R_SCOPED_NAME
import static com.cedarsoftware.util.VisualizerConstants.SPACE
import static com.cedarsoftware.util.VisualizerConstants.UNSPECIFIED
import static com.cedarsoftware.util.VisualizerConstants._ENUM

/**
 * Holds information about a source cube and target cube for purposes of creating a visualization of the cubes and their relationship.
 */

@CompileStatic
class VisualizerRelInfo
{
	boolean loadFieldsAndTraits = true
	Set<String> notes = []
	Map<String, Object> scope
	String group

	long targetId
    NCube targetCube
	Map<String, Object> targetScope
	Map<String, Map<String, Object>> targetTraitMaps
	long targetLevel

	long sourceId
	NCube sourceCube
	Map<String, Object> sourceScope
	Map<String, Map<String, Object>> sourceTraitMaps
	String sourceFieldName
	String sourceFieldRpmType

	Set<String> requiredScopeKeys  = []
	Set<String> optionalScopeKeys  = []

	Set<String> getRequiredScope()
	{
		Set<String> requiredScope = targetCube.getRequiredScope(targetScope, [:] as Map)
		requiredScope.remove(com.cedarsoftware.util.VisualizerConstants.AXIS_FIELD)
		requiredScope.remove(com.cedarsoftware.util.VisualizerConstants.AXIS_NAME)
		requiredScope.remove(com.cedarsoftware.util.VisualizerConstants.AXIS_TRAIT)
		return requiredScope
	}

	String getLabel()
	{
		String nodeGroup = getNodeGroup()
		StringBuilder sb = new StringBuilder()
		if (com.cedarsoftware.util.VisualizerConstants.GROUPS_TO_SHOW_IN_TITLE.contains(nodeGroup))
		{
			String label = com.cedarsoftware.util.VisualizerConstants.ALL_GROUPS_MAP[nodeGroup]
			int len = label.length()
			sb.append(label)
			sb.append('\n')
			sb.append('-' * Math.floor(len * com.cedarsoftware.util.VisualizerConstants.NODE_LABEL_LINE_LENGTH_MULTIPLIER))
			sb.append('\n')
		}

		String labelName = getDotSuffix(getTargetEffectiveName())
		String[] splitName = labelName.split("(?=\\p{Upper})")
		String line = ''
		for (String part : splitName)
		{
			if (line.length() + part.length() < com.cedarsoftware.util.VisualizerConstants.NODE_LABEL_MAX_LINE_LENGTH)
			{
				line += part
			}
			else
			{
				sb.append(line)
				sb.append('\n')
				line = part
			}
		}
		sb.append(line)
		return sb.toString()
	}

	String getTitle()
	{
		boolean loadFieldsAndTraits = loadFieldsAndTraits
		Map<String, Map<String, Object>> traitMaps = targetTraitMaps
		Set<String> notes = notes
		String scopedName = getTargetScopedName()
		StringBuilder sb = new StringBuilder()

		//Scoped Name
		if (scopedName)
		{
			sb.append('<b>scoped name = </b>')
			sb.append(scopedName)
			sb.append(com.cedarsoftware.util.VisualizerConstants.DOUBLE_BREAK)
		}

		//Notes
		if (notes)
		{
			sb.append('<b>Note: </b><br>')
			notes.each { String note ->
				sb.append(' ')
				sb.append(note)
				sb.append(com.cedarsoftware.util.VisualizerConstants.BREAK)
			}
			sb.append(com.cedarsoftware.util.VisualizerConstants.BREAK)
		}

		//Scope
		if (loadFieldsAndTraits)
		{
			sb.append('<b>scope used to load class = </b>')
			sb.append(targetScope.toString() - '{' - '}')
			sb.append(com.cedarsoftware.util.VisualizerConstants.DOUBLE_BREAK)
		}
		sb.append('<b>available scope to load class = </b>')
		sb.append(scope.toString() - '{' - '}')
		sb.append(com.cedarsoftware.util.VisualizerConstants.DOUBLE_BREAK)

		//Level
		sb.append('<b>level = </b>')
		sb.append(String.valueOf(targetLevel))
		sb.append(com.cedarsoftware.util.VisualizerConstants.DOUBLE_BREAK)

		//Fields
		if (loadFieldsAndTraits)
		{
			sb.append("<b>fields = </b>${getFieldDetails(traitMaps)}")
		}

		return sb.toString()
	}

	private static String getFieldDetails(Map<String, Map<String, Object>> traitMaps)
	{
		StringBuilder fieldDetails = new StringBuilder()

		traitMaps.each { String fieldName, v ->
			if (com.cedarsoftware.util.VisualizerConstants.CLASS_TRAITS != fieldName)
			{
				fieldDetails.append(com.cedarsoftware.util.VisualizerConstants.BREAK)
				fieldDetails.append(com.cedarsoftware.util.VisualizerConstants.SPACE)
				fieldDetails.append(fieldName)
			}
		}
		return fieldDetails.toString()
	}

	String getNodeGroup()
	{
		return targetCube.name.startsWith(com.cedarsoftware.util.VisualizerConstants.RPM_ENUM) ? group + com.cedarsoftware.util.VisualizerConstants._ENUM : group
	}

	String getGroupName(String cubeName = targetCube.name)
	{
		Iterable<String> splits = Splitter.on('.').split(cubeName)
		String group = splits[2].toUpperCase()
		return com.cedarsoftware.util.VisualizerConstants.ALL_GROUPS_KEYS.contains(group) ? group : com.cedarsoftware.util.VisualizerConstants.UNSPECIFIED
	}

	String getSourceEffectiveName()
	{
		String scopedName = getSourceScopedName()
		return scopedName == null ? sourceCube.name : scopedName
	}

	String getTargetEffectiveName()
	{
		String scopedName = getTargetScopedName()
		return scopedName == null ? targetCube.name : scopedName
	}

	String getSourceScopedName()
	{
		Map<String, Object> classTraitsTraitMap = sourceTraitMaps[com.cedarsoftware.util.VisualizerConstants.CLASS_TRAITS] as Map
		return classTraitsTraitMap ? classTraitsTraitMap[com.cedarsoftware.util.VisualizerConstants.R_SCOPED_NAME] : null
	}

	String getTargetScopedName()
	{
		Map<String, Object> classTraitsTraitMap = targetTraitMaps[com.cedarsoftware.util.VisualizerConstants.CLASS_TRAITS] as Map
		return classTraitsTraitMap ? classTraitsTraitMap[com.cedarsoftware.util.VisualizerConstants.R_SCOPED_NAME] : null
	}

	private static String getDotSuffix(String value)
	{
		int lastIndexOfDot = value.lastIndexOf('.')
		return lastIndexOfDot == -1 ? value : value.substring(lastIndexOfDot + 1)
	}

	void removeNotExistsFields()
	{
		Iterator<String> i = targetTraitMaps.keySet().iterator()
		while (i.hasNext())
		{
			String fieldName = i.next()
			if (!targetTraitMaps[fieldName][com.cedarsoftware.util.VisualizerConstants.R_EXISTS])
			{
				i.remove()
			}
		}
	}

	String getNextTargetCubeName(String targetFieldName)
	{
		if (sourceCube.getAxis(com.cedarsoftware.util.VisualizerConstants.AXIS_TRAIT).findColumn(com.cedarsoftware.util.VisualizerConstants.R_SCOPED_NAME))
		{
			if (sourceTraitMaps[com.cedarsoftware.util.VisualizerConstants.CLASS_TRAITS][com.cedarsoftware.util.VisualizerConstants.R_SCOPED_NAME] == null)
			{
				return null
			}
			else
			{
				return com.cedarsoftware.util.VisualizerConstants.RPM_CLASS_DOT + sourceFieldRpmType
			}
		}
		return targetFieldName
	}
}