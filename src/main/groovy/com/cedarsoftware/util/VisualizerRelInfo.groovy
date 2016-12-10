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
	boolean hasFields = false
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
		targetCube = NCubeManager.getCube(appId, node.cubeName as String)
		String sourceCubeName = node.sourceCubeName as String
		sourceCube = sourceCubeName ? NCubeManager.getCube(appId, sourceCubeName) : null
		sourceFieldName = node.fromFieldName
		targetId = Long.valueOf(node.id as String)
		targetLevel = Long.valueOf(node.level as String)
		targetScope = node.scope as CaseInsensitiveMap
		scope = node.availableScope as CaseInsensitiveMap
		loadTraits = node.loadTraits as boolean
		hasFields = node.hasFields as boolean
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

	String getDetails()
	{
		boolean isScopedClass = targetScopedName
		String effectiveName = effectiveNameByCubeName
		StringBuilder sb = new StringBuilder()
		String notesLabel = "<b>Note: </b>"

		//Scoped Name
		if (isScopedClass)
		{
			sb.append("<b>Scoped name: </b>${effectiveName}${DOUBLE_BREAK}")
		}

		if (!hasFields)
		{
			sb.append("<b>*** Unable to load fields and traits for ${effectiveName}</b>${DOUBLE_BREAK}")
			notesLabel = "<b>Reason: </b>"
		}

		//Notes
		if (notes)
		{
			sb.append(notesLabel)
			notes.each { String note ->
				sb.append("${note} ")
			}
			sb.append("${DOUBLE_BREAK}")
		}

		//Level
		sb.append("<b>Level: </b>${String.valueOf(targetLevel)}${DOUBLE_BREAK}")

		//Scope
		if (hasFields)
		{
			if (loadTraits)
			{
				sb.append("<b>Utilized scope</b>")
			}
			else
			{
				sb.append("<b>Utilized scope for minimum traits</b>")

			}
			sb.append("<pre><ul>")
			targetScope.each { String key, Object value ->
				sb.append("<li>${key}: ${value}</li>")
			}
			sb.append("</ul></pre>${BREAK}")
		}

		sb.append("<b>Available scope</b>")
		sb.append("<pre><ul>")
		scope.each { String key, Object value ->
			sb.append("<li>${key}: ${value}</li>")
		}
		sb.append("</ul></pre>${BREAK}")

		//Fields
		if (hasFields)
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
			sb.append("<b>Fields and traits</b>")
	}
		else
		{
			sb.append("<b>Fields</b>")
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
				String traitString = traitValue.toString()
				if (traitString.startsWith(HTTP) || traitString.startsWith(HTTPS) || traitString.startsWith(FILE))
				{
					sb.append("<li>${traitName}: <a href=\"${traitString}\" target=\"_blank\">${traitString}</a></li>")
				}
				else
				{
					sb.append("<li>${traitName}: ${traitValue}</li>")
				}
			}
		}
		sb.append("</ul></pre>")
	}

	private void addClassTraits(StringBuilder sb)
	{
		sb.append("<b>Class traits</b>")
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
			String scopedName = sourceTraitMaps[CLASS_TRAITS][R_SCOPED_NAME]
			return !scopedName ?: RPM_CLASS_DOT + sourceFieldRpmType
		}
		return RPM_CLASS_DOT + targetFieldName
	}

    String getSourceMessage()
    {
        if (sourceTraitMaps)
        {
            return sourceScopedName ? ", the target of ${sourceScopedName} on ${getCubeDisplayName(sourceCube.name)}" : ""
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
		node.cubeName = targetCubeName
		node.sourceCubeName = sourceCube ? sourceCube.name : null
		node.scope = targetScope
		node.availableScope = scope
		node.fromFieldName = sourceFieldName == null ? null : sourceFieldName

		if (targetCubeName.startsWith(RPM_CLASS_DOT))
		{
			node.label = getDotSuffix(targetEffectiveName)
			node.detailsTitle = getCubeDetailsTitle(targetCubeName)
			node.title = getCubeDisplayName(targetCubeName)
		}
		else
		{
			String detailsTitle = getCubeDetailsTitle(targetCubeName)
			node.detailsTitle = detailsTitle
			node.title = detailsTitle
		}
		node.details = details
		group ?: setGroupName()
		node.group = nodeGroup
		node.loadTraits = loadTraits
		node.hasFields = hasFields
		return node
	}

	String getEffectiveNameByCubeName()
	{
		String scopeKey = getDotSuffix(targetCube.name)
		return scope[scopeKey] ?: targetEffectiveName
	}

	static String getCubeDisplayName(String cubeName)
	{
		String displayName
		if (cubeName.startsWith(RPM_CLASS_DOT))
		{
			displayName = cubeName - RPM_CLASS_DOT
		}
		else if (cubeName.startsWith(RPM_ENUM_DOT))
		{
			displayName = cubeName - RPM_ENUM_DOT
		}
		return displayName
	}

	String getCubeDetailsTitle(String cubeName)
	{
		String detailsTitle
		if (cubeName.startsWith(RPM_CLASS_DOT))
		{
			String targetScopedName = getTargetScopedName()
			if (targetScopedName)
			{
				detailsTitle = "${getCubeDisplayName(cubeName)} ${getEffectiveNameByCubeName()}"
			}
			else
			{
				detailsTitle = getCubeDisplayName(cubeName)
			}
		}
		else if (cubeName.startsWith(RPM_ENUM_DOT))
		{
			detailsTitle = "Valid values for field ${sourceFieldName} on ${getCubeDisplayName(sourceCube.name)}".toString()
		}
		return detailsTitle
	}
}