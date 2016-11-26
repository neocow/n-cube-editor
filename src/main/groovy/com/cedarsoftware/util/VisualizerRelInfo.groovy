package com.cedarsoftware.util

import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.RuleInfo
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
import static com.cedarsoftware.util.VisualizerConstants.EFFECTIVE_VERSION_SCOPE_KEY
import static com.cedarsoftware.util.VisualizerConstants.GROUPS_TO_SHOW_IN_TITLE
import static com.cedarsoftware.util.VisualizerConstants.NODE_LABEL_LINE_LENGTH_MULTIPLIER
import static com.cedarsoftware.util.VisualizerConstants.NODE_LABEL_MAX_LINE_LENGTH
import static com.cedarsoftware.util.VisualizerConstants.RPM_CLASS
import static com.cedarsoftware.util.VisualizerConstants.RPM_CLASS_DOT
import static com.cedarsoftware.util.VisualizerConstants.RPM_ENUM
import static com.cedarsoftware.util.VisualizerConstants.RPM_ENUM_DOT
import static com.cedarsoftware.util.VisualizerConstants.R_EXISTS
import static com.cedarsoftware.util.VisualizerConstants.R_SCOPED_NAME
import static com.cedarsoftware.util.VisualizerConstants.SPACE
import static com.cedarsoftware.util.VisualizerConstants.SYSTEM_SCOPE_KEY_PREFIX
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
		requiredScope.remove(AXIS_FIELD)
		requiredScope.remove(AXIS_NAME)
		requiredScope.remove(AXIS_TRAIT)
		return requiredScope
	}

	String getLabel()
	{
		String nodeGroup = nodeGroup
		StringBuilder sb = new StringBuilder()
		if (GROUPS_TO_SHOW_IN_TITLE.contains(nodeGroup))
		{
			String label = ALL_GROUPS_MAP[nodeGroup]
			int len = label.length()
			sb.append(label)
			sb.append('\n')
			sb.append('-' * Math.floor(len * NODE_LABEL_LINE_LENGTH_MULTIPLIER))
			sb.append('\n')
		}

		String labelName = getDotSuffix(targetEffectiveName)
		String[] splitName = labelName.split("(?=\\p{Upper})")
		String line = ''
		for (String part : splitName)
		{
			if (line.length() + part.length() < NODE_LABEL_MAX_LINE_LENGTH)
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
		String scopedName = targetScopedName
		StringBuilder sb = new StringBuilder()

		//Scoped Name
		if (scopedName)
		{
			sb.append('<b>scoped name = </b>')
			sb.append(scopedName)
			sb.append(DOUBLE_BREAK)
		}

		//Notes
		if (notes)
		{
			sb.append('<b>Note: </b><br>')
			notes.each { String note ->
				sb.append(' ')
				sb.append(note)
				sb.append(BREAK)
			}
			sb.append(BREAK)
		}

		//Scope
		if (loadFieldsAndTraits)
		{
			sb.append('<b>scope used to load class = </b>')
			sb.append(targetScope.toString() - '{' - '}')
			sb.append(DOUBLE_BREAK)
		}
		sb.append('<b>available scope to load class = </b>')
		sb.append(scope.toString() - '{' - '}')
		sb.append(DOUBLE_BREAK)

		//Level
		sb.append('<b>level = </b>')
		sb.append(String.valueOf(targetLevel))
		sb.append(DOUBLE_BREAK)

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
			if (CLASS_TRAITS != fieldName)
			{
				fieldDetails.append(BREAK)
				fieldDetails.append(SPACE)
				fieldDetails.append(fieldName)
			}
		}
		return fieldDetails.toString()
	}

	String getNodeGroup()
	{
		if (!group)
		{
			throw new IllegalStateException("Group must be set prior to getting node group.")
		}

		return targetCube.name.startsWith(RPM_ENUM) ? group + _ENUM : group
	}

	String setGroupName(String cubeName = targetCube.name)
	{
		Iterable<String> splits = Splitter.on('.').split(cubeName)
		String group = splits[2].toUpperCase()
		this.group = ALL_GROUPS_KEYS.contains(group) ? group : UNSPECIFIED
	}


	String getSourceEffectiveName()
	{
		String scopedName = sourceScopedName
		return scopedName == null ? sourceCube.name : scopedName
	}

	String getTargetEffectiveName()
	{
		String scopedName = targetScopedName
		return scopedName == null ? targetCube.name : scopedName
	}

	String getSourceScopedName()
	{
		Map<String, Object> classTraitsTraitMap = sourceTraitMaps[CLASS_TRAITS] as Map
		return classTraitsTraitMap ? classTraitsTraitMap[R_SCOPED_NAME] : null
	}

	String getTargetScopedName()
	{
		Map<String, Object> classTraitsTraitMap = targetTraitMaps[CLASS_TRAITS] as Map
		return classTraitsTraitMap ? classTraitsTraitMap[R_SCOPED_NAME] : null
	}

	private static String getDotSuffix(String value)
	{
		int lastIndexOfDot = value.lastIndexOf('.')
		return lastIndexOfDot == -1 ? value : value.substring(lastIndexOfDot + 1)
	}

	String getNextTargetCubeName(String targetFieldName)
	{
		if (sourceCube.getAxis(AXIS_TRAIT).findColumn(R_SCOPED_NAME))
		{
			if (sourceTraitMaps[CLASS_TRAITS][R_SCOPED_NAME] == null)
			{
				return null
			}
			else
			{
				return RPM_CLASS_DOT + sourceFieldRpmType
			}
		}
		return targetFieldName
	}

    String getSourceMessage()
    {
        if (sourceTraitMaps)
        {
            return sourceScopedName ? ", the target of ${sourceScopedName} on ${sourceCube.name}" : ""
        }
        return ''
    }

    void getTraitMaps(VisualizerHelper helper, Map<String, Set<String>> requiredScopeKeyz, Map<String, Set<String>> optionalScopeKeyz)
    {
        targetTraitMaps = [:]
        Map output = [:]
        if (targetCube.name.startsWith(RPM_ENUM))
        {
            helper.loadRpmClassFields(RPM_ENUM, targetCube.name - RPM_ENUM_DOT, scope, targetTraitMaps, output)
        }
        else
        {
            helper.loadRpmClassFields(RPM_CLASS, targetCube.name - RPM_CLASS_DOT, scope, targetTraitMaps, output)
        }
        removeNotExistsFields()
        getRequiredAndOptionalScopeKeys(requiredScopeKeyz, optionalScopeKeyz)
        retainUsedScope(output)
    }

    void retainUsedScope(Map output)
    {
        Set<String> scopeCollector = new CaseInsensitiveSet<>()
        scopeCollector.addAll(requiredScopeKeys)
        scopeCollector.addAll(optionalScopeKeys)
        scopeCollector << EFFECTIVE_VERSION_SCOPE_KEY

        RuleInfo ruleInfo = NCube.getRuleInfo(output)
        Set keysUsed = ruleInfo.getInputKeysUsed()
        scopeCollector.addAll(keysUsed)

        targetScope = new CaseInsensitiveMap(scope)
        cullScope(targetScope.keySet(), scopeCollector)
    }

    void removeNotExistsFields()
    {
        targetTraitMaps.keySet().removeAll { String fieldName -> !targetTraitMaps[fieldName][R_EXISTS] }
    }

    static void cullScope(Set<String> scopeKeys, Set scopeCollector)
    {
        scopeKeys.removeAll { String item -> !(scopeCollector.contains(item) || item.startsWith(SYSTEM_SCOPE_KEY_PREFIX)) }
    }

	/**
	 *  Gets the required and optional scope keys for the target cube from a map
	 *  of scope keys for all cubes processed so far.
	 *  If the map has not already been loaded with scope keys for this cube,
	 *  then get the scope keys for this cube and add them to the map.
	*/
    private void getRequiredAndOptionalScopeKeys(Map<String, Set<String>> requiredScopeKeyz, Map<String, Set<String>> optionalScopeKeyz)
    {
        String cubeName = targetCube.name
        if (requiredScopeKeyz.containsKey(cubeName))
        {
            requiredScopeKeys = requiredScopeKeyz[cubeName]
            optionalScopeKeys = optionalScopeKeyz[cubeName]
        }
        else
        {
            requiredScopeKeys = this.requiredScope
            optionalScopeKeys = targetCube.getOptionalScope(scope, [:])
            requiredScopeKeyz[cubeName] = requiredScopeKeys
            optionalScopeKeyz[cubeName] = optionalScopeKeys
        }
    }
}