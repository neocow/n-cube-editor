package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.RuleInfo
import com.google.common.base.Splitter
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Holds information about a source cube and target cube for purposes of creating a visualization of the cubes and their relationship.
 */

@CompileStatic
class VisualizerRelInfo
{
	boolean loadFieldsAndTraits = true
	boolean loadTraits = false
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

	VisualizerRelInfo() {}

	VisualizerRelInfo(ApplicationID appId, Map options)
	{
		Map node = options.node as Map
		targetCube = NCubeManager.getCube(appId, node.name as String)
		sourceFieldName = node.fromFieldName
		targetId = Long.valueOf(node.id as String)
		targetLevel = Long.valueOf(node.level as String)
		targetScope = node.scope as CaseInsensitiveMap
		scope = node.availableScope as CaseInsensitiveMap
		loadTraits = node.loadTraits as boolean
		group = setGroupName()
	}

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
		String scopedName = targetScopedName
		StringBuilder sb = new StringBuilder()

		//Scoped Name
		if (scopedName)
		{
			sb.append("<b>scoped name = </b>${scopedName}${DOUBLE_BREAK}")
		}

		//Level
		sb.append("<b>level = </b>${String.valueOf(targetLevel)}${DOUBLE_BREAK}")

		//Notes
		if (notes)
		{
			sb.append("<b>note = </b>")
			notes.each { String note ->
				sb.append("${note} ")
			}
			sb.append("${DOUBLE_BREAK}")
		}

		//Scope
		if (loadFieldsAndTraits)
		{
			if (loadTraits)
			{
				sb.append("<b>scope used loading all traits</b>")
			}
			else
			{
				sb.append("<b>scope used loading minimal traits</b>")

			}
			sb.append("<pre><ul>")
			targetScope.each { String key, Object value ->
				sb.append("<li>${key}: ${value}</li>")
			}
			sb.append("</ul></pre>${BREAK}")
		}

		sb.append("<b>available scope</b>")
		sb.append("<pre><ul>")
		scope.each { String key, Object value ->
			sb.append("<li>${key}: ${value}</li>")
		}
		sb.append("</ul></pre>${BREAK}")

		//Fields
		if (loadFieldsAndTraits)
		{
			if (loadTraits)
			{
				addClassTraits(sb)
			}
			addFieldDetails(sb)
		}
		return sb.toString()
	}

	private void addFieldDetails(StringBuilder sb)
	{
		if (loadTraits)
		{
			sb.append("<b>fields and traits</b>")
	}
		else
		{
			sb.append("<b>fields</b>")
		}
		sb.append("<pre><ul>")
		targetTraitMaps.each { String fieldName, v ->
			if (CLASS_TRAITS != fieldName)
			{
				if (loadTraits)
				{
					sb.append("<li><b>${fieldName}</b></li>")
					addTraits(sb, fieldName)
				}
				else
				{
					sb.append("<li>${fieldName}</li>")
				}
			}
		}
		sb.append("</ul></pre>")
	}

	private void addTraits(StringBuilder sb, String fieldName)
	{
		Map<String, Object> traits = targetTraitMaps[fieldName].sort() as Map
		sb.append("<pre><ul>")
		traits.each { String traitName, Object traitValue ->
			if (traitValue != null)
			{
				sb.append("<li>${traitName}: ${traitValue}</li>")
			}
		}
		sb.append("</ul></pre>")
	}

	private void addClassTraits(StringBuilder sb)
	{
		sb.append("<b>class traits</b>")
		addTraits(sb, CLASS_TRAITS)
		sb.append("${BREAK}")
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
		return RPM_CLASS_DOT + targetFieldName
	}

    String getSourceMessage()
    {
        if (sourceTraitMaps)
        {
            return sourceScopedName ? ", the target of ${sourceScopedName} on ${sourceCube.name}" : ""
        }
        return ''
    }

    void getTraitMaps(VisualizerHelper helper, ApplicationID appId, VisualizerInfo visInfo)
    {
        targetTraitMaps = [:]
        Map output = [:]
        if (targetCube.name.startsWith(RPM_ENUM))
        {
            helper.loadRpmClassFields(appId, RPM_ENUM, targetCube.name - RPM_ENUM_DOT, scope, targetTraitMaps, visInfo.loadTraits, output)
        }
        else
        {
            helper.loadRpmClassFields(appId, RPM_CLASS, targetCube.name - RPM_CLASS_DOT, scope, targetTraitMaps, visInfo.loadTraits, output)
        }
        removeNotExistsFields()
		addRequiredAndOptionalScopeKeys(visInfo)
        retainUsedScope(visInfo, output)
    }

    void retainUsedScope(VisualizerInfo visInfo, Map output)
    {
        Set<String> scopeCollector = new CaseInsensitiveSet<>()
        scopeCollector.addAll(visInfo.requiredScopeKeys[targetCube.name])
        scopeCollector.addAll(visInfo.optionalScopeKeys[targetCube.name])
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
	 *  If the required and optional scope keys have not already been loaded for this cube,
	 *  load them.
	 */
	private void addRequiredAndOptionalScopeKeys(VisualizerInfo visInfo)
	{
		String cubeName = targetCube.name
		if (!visInfo.requiredScopeKeys.containsKey(cubeName))
		{
			visInfo.requiredScopeKeys[cubeName] = requiredScope
			visInfo.optionalScopeKeys[cubeName] = targetCube.getOptionalScope(scope, [:])
		}
	}

	Map<String, Object> createEdge(int edgeId)
	{
		String sourceFieldName = sourceFieldName
		Map<String, Map<String, Object>> sourceTraitMaps = sourceTraitMaps

		Map<String, Object> edge = [:]
		String sourceCubeEffectiveName = sourceEffectiveName
		String targetCubeEffectiveName = targetEffectiveName
		edge.id = String.valueOf(edgeId + 1)
		edge.from = String.valueOf(sourceId)
		edge.to = String.valueOf(targetId)
		edge.fromName = sourceCubeEffectiveName
		edge.toName = targetCubeEffectiveName
		edge.fromFieldName = sourceFieldName
		edge.level = String.valueOf(targetLevel)
		if (targetCube.name.startsWith(RPM_ENUM_DOT))
		{
			edge.label = sourceFieldName
		}
		Map<String, Map<String, Object>> sourceFieldTraitMap = sourceTraitMaps[sourceFieldName] as Map
		String vMin = sourceFieldTraitMap[V_MIN] as String ?: '0'
		String vMax = sourceFieldTraitMap[V_MAX] as String ?: '999999'
		edge.title = "Field ${sourceFieldName} with min:max cardinality of ${vMin}:${vMax}".toString()
		return edge
	}

	Map<String, Object> createNode()
	{
		NCube targetCube = targetCube
		String targetCubeName = targetCube.name
		String sourceFieldName = sourceFieldName

		Map<String, Object> node = [:]
		node.id = String.valueOf(targetId)
		node.level = String.valueOf(targetLevel)
		node.name = targetCubeName
		node.scope = targetScope
		node.availableScope = scope
		node.fromFieldName = sourceFieldName == null ? null : sourceFieldName
		if (targetCubeName.startsWith(RPM_CLASS_DOT))
		{
			node.title = "${targetCubeName} for ${getDotSuffix(targetEffectiveName)}".toString()
		}
		else
		{
			node.title = "${targetCubeName} for field ${sourceFieldName}".toString()
		}
		node.desc = title
		group ?: setGroupName()
		if (targetCubeName.startsWith(RPM_CLASS_DOT))
		{
			node.label = label
		}
		node.group = nodeGroup
		node.loadTraits = loadTraits
		return node
	}

}