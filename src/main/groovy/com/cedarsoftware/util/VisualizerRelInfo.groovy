package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Holds information about a source cube and target cube for purposes of creating a visualization of the cubes and their relationship.
 */

@CompileStatic
class VisualizerRelInfo
{
	Set<String> notes = []
	Map<String, Object> scope
	String group

	long targetId
    NCube targetCube
	Map<String, Object> targetScope
	long targetLevel

	long sourceId
	NCube sourceCube
	Map<String, Object> sourceScope
	String sourceFieldName

	VisualizerRelInfo() {}

	VisualizerRelInfo(ApplicationID appId, Set allGroupsKeys, Map node)
	{
		targetCube = NCubeManager.getCube(appId, node.cubeName as String)
		String sourceCubeName = node.sourceCubeName as String
		sourceCube = sourceCubeName ? NCubeManager.getCube(appId, sourceCubeName) : null
		sourceFieldName = node.fromFieldName
		targetId = Long.valueOf(node.id as String)
		targetLevel = Long.valueOf(node.level as String)
		targetScope = node.scope as CaseInsensitiveMap
		scope = node.availableScope as CaseInsensitiveMap
		group = setGroupName(allGroupsKeys)
	}

	Set<String> getRequiredScope()
	{
		Set<String> requiredScope = targetCube.getRequiredScope(targetScope, [:] as Map)
		return requiredScope
	}

	String getDetails()
	{
		StringBuilder sb = new StringBuilder()
		String notesLabel = "<b>Note: </b>"

		//Notes
		if (notes)
		{
			sb.append(notesLabel)
			notes.each { String note ->
				sb.append("${note} ")
			}
			sb.append("${DOUBLE_BREAK}")
		}

		//Scope
		sb.append("<b>Scope</b>")
		sb.append("<pre><ul>")
		targetScope.each { String key, Object value ->
			sb.append("<li>${key}: ${value}</li>")
		}
		sb.append("</ul></pre>${BREAK}")

		sb.append("<b>Available scope</b>")
		sb.append("<pre><ul>")
		scope.each { String key, Object value ->
			sb.append("<li>${key}: ${value}</li>")
		}
		sb.append("</ul></pre>${BREAK}")

		return sb.toString()
	}

	String getNodeGroup(String groupSuffix)
	{
		if (!group)
		{
			throw new IllegalStateException("Group must be set prior to getting node group.")
		}
		return group
	}

	String setGroupName(Set<String> allGroupsKeys, String cubeName = targetCube.name)
	{
		this.group = UNSPECIFIED
	}

	protected static String getDotSuffix(String value)
	{
		int lastIndexOfDot = value.lastIndexOf('.')
		return lastIndexOfDot == -1 ? value : value.substring(lastIndexOfDot + 1)
	}

	protected static String getDotPrefix(String value) {
		int indexOfDot = value.indexOf('.')
		return indexOfDot == -1 ? value : value.substring(0, value.indexOf('.'))
	}

	String getSourceEffectiveName()
	{
		return sourceCube.name
	}

	String getTargetEffectiveName()
	{
		return targetCube.name
	}

	String getNextTargetCubeName(String targetFieldName)
	{
		return null  //TODO
	}

    String getSourceMessage()
    {
       return ''  //TODO
    }

  	/**
	 *  If the required and optional scope keys have not already been loaded for this cube,
	 *  load them.
	 */
	protected void addRequiredAndOptionalScopeKeys(VisualizerInfo visInfo)
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
		Map<String, Object> edge = [:]
		edge.id = String.valueOf(edgeId + 1)
		edge.from = String.valueOf(sourceId)
		edge.to = String.valueOf(targetId)
		edge.fromName = sourceEffectiveName
		edge.toName = targetEffectiveName
		edge.fromFieldName = sourceFieldName
		edge.level = String.valueOf(targetLevel)
		edge.title = sourceFieldName  //TODO

		return edge
	}

	Map<String, Object> createNode(Set<String> allGroupsKeys, String groupSuffix)
	{
		NCube targetCube = targetCube
		String targetCubeName = targetCube.name
		String sourceCubeName = sourceCube ? sourceCube.name : null
		String sourceFieldName = sourceFieldName

		Map<String, Object> node = [:]
		node.id = String.valueOf(targetId)
		node.level = String.valueOf(targetLevel)
		node.cubeName = targetCubeName
		node.sourceCubeName = sourceCubeName
		node.scope = targetScope
		node.availableScope = scope
		node.fromFieldName = sourceFieldName == null ? null : sourceFieldName
		node.sourceDescription = sourceCubeName ? sourceDescription : null
		String detailsTitle1 = cubeDetailsTitle1

		node.label = nodeLabel
		node.detailsTitle1 = detailsTitle1
		node.detailsTitle2 = cubeDetailsTitle2
		node.title = getCubeDisplayName(targetCubeName)

		node.details = details
		group ?: setGroupName(allGroupsKeys)
		node.group = getNodeGroup(groupSuffix)
		return node
	}

	protected String getNodeLabel()
	{
		targetEffectiveName
	}

	String getEffectiveNameByCubeName()
	{
		return targetCube.name
	}

	String getCubeDisplayName(String cubeName)
	{
		return cubeName
	}

	String getSourceDescription()
	{
		return sourceCube.name
	}

	String getCubeDetailsTitle1()
	{
		return targetCube.name
	}

	String getCubeDetailsTitle2()
	{
		return null
	}
}