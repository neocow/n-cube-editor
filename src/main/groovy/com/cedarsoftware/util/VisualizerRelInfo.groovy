package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.util.LongHashSet
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Holds information about a source cube and target cube for purposes of creating a visualization of the cubes and their relationship.
 */

@CompileStatic
class VisualizerRelInfo
{
	ApplicationID appId

	Set<String> notes = []
	Map<String, Object> scope

	long targetId
	NCube targetCube
	Map<String, Object> targetScope
	long targetLevel

	long sourceId
	NCube sourceCube
	Map<String, Object> sourceScope
	String sourceFieldName

	boolean cellValuesLoaded
	boolean showCellValuesLink
	boolean showCellValues
	boolean executingCells
	String executeCell
	boolean executeCells

	Set<VisualizerCellInfo> cellInfo

	List<String> typesToAdd

	protected VisualizerHelper helper = new VisualizerHelper()

	VisualizerRelInfo() {}

	VisualizerRelInfo(ApplicationID applicationId, Map node)
	{
		appId  = applicationId
		targetCube = NCubeManager.getCube(appId, node.cubeName as String)
		String sourceCubeName = node.sourceCubeName as String
		sourceCube = sourceCubeName ? NCubeManager.getCube(appId, sourceCubeName) : null
		sourceFieldName = node.fromFieldName
		targetId = Long.valueOf(node.id as String)
		targetLevel = Long.valueOf(node.level as String)
		targetScope = node.scope as CaseInsensitiveMap
		scope = node.availableScope as CaseInsensitiveMap
		showCellValuesLink = node.showCellValuesLink as boolean
		showCellValues = node.showCellValues as boolean
		executeCell = node.executeCell as String
		executeCells = node.executeCells as boolean
		setExecutingFlags()
		cellValuesLoaded = node.cellValuesLoaded as boolean
		typesToAdd = node.typesToAdd as List
	}

	boolean loadCellValues(VisualizerInfo visInfo)
	{
		cellInfo = [] as Set
		cellValuesLoaded = true
		if (showCellValues)
		{
			Map<LongHashSet, Object> cellMap = targetCube.cellMap
			cellMap.each { LongHashSet ids, Object noExecuteCell ->
				Map<String, Object> coordinate = targetCube.getCoordinateFromIds(ids)
				VisualizerCellInfo visCellInfo = new VisualizerCellInfo(String.valueOf(targetId), coordinate)
				try
				{
					visCellInfo.cell = targetCube.getCell(coordinate)

				}
				catch (Exception e)
				{
					visCellInfo.exception = e
				}
				visCellInfo.noExecuteCell = noExecuteCell
				cellInfo << visCellInfo
			}
		}
		return true
	}

	private boolean setExecutingFlags()
	{
		executingCells = executeCells || executeCell
	}

	void setExecuteTriggers(Map node)
	{
		node.executeCells = executeCells
		node.executeCell = executeCell
	}

	void resetExecuteTriggers()
	{
		if (executingCells)
		{
			executeCell = null
			executeCells = false
		}
	}

	Set<String> getRequiredScope()
	{
		return targetCube.getRequiredScope(targetScope, [:] as Map)
	}

	String getDetails(VisualizerInfo visInfo)
	{
		StringBuilder sb = new StringBuilder()
		String targetCubeName = targetCube.name

		//Notes
		if (notes)
		{
			sb.append("<b>Note: </b>")
			notes.each { String note ->
				sb.append("${note} ")
			}
			sb.append("${DOUBLE_BREAK}")
		}

		getDetailsMap(sb, 'Scope', targetScope)
		getDetailsMap(sb, 'Available scope', scope)
		getDetailsSet(sb, 'Required scope keys', visInfo.requiredScopeKeys[targetCubeName])
		getDetailsSet(sb, 'Optional scope keys', visInfo.optionalScopeKeys[targetCubeName])

		//Cell values
		if (cellValuesLoaded && showCellValues)
		{
			addCellValueSection(visInfo, sb)
		}
		return sb.toString()
	}

	private void addCellValueSection(VisualizerInfo visInfo, StringBuilder sb)
	{
		StringBuilder cellValuesBuilder = new StringBuilder()
		StringBuilder linkBuilder = new StringBuilder()
		sb.append("<b>Cell values</b>")
		getCellValues(visInfo, cellValuesBuilder, linkBuilder )
		sb.append(linkBuilder.toString())
		sb.append("""<pre><ul class="cellValues">""")
		sb.append(cellValuesBuilder.toString())
		sb.append("</ul></pre>")
	}

	private void getCellValues(VisualizerInfo visInfo, StringBuilder cellValuesBuilder, StringBuilder linkBuilder)
	{
		boolean hasNonExecutedCells = false
		String id = String.valueOf(targetId)

		cellInfo.each { VisualizerCellInfo visCellInfo ->
		  visCellInfo.getCellValue(visInfo, this, cellValuesBuilder)
	    }

		if (hasNonExecutedCells)
		{
			linkBuilder.append(DOUBLE_BREAK)
			linkBuilder.append("""${SPACE}<a class="executeCell" id="${id}" href="#">Execute all</a>""")
			linkBuilder.append(BREAK)
		}
	}

	static String getDetailsMap(StringBuilder sb, String title, Map<String, Object> map)
	{
		sb.append("<b>${title}</b>")
		sb.append("<pre><ul>")
		if (map)
		{
			map.each { String key, Object value ->
				sb.append("<li>${key}: ${value}</li>")
			}
		}
		else
		{
			sb.append("<li>none</li>")
		}
		sb.append("</ul></pre>${BREAK}")
	}

	static String getDetailsSet(StringBuilder sb, String title, Set<String> set)
	{
		sb.append("<b>${title}</b>")
		sb.append("<pre><ul>")
		if (set)
		{
			set.each { String value ->
				sb.append("<li>${value}</li>")
			}
		}
		else
		{
			sb.append("<li>none</li>")
		}
		sb.append("</ul></pre>${BREAK}")
	}

	String getGroupName(VisualizerInfo visInfo = null, String cubeName = targetCube.name)
	{
		return targetCube.hasRuleAxis() ? RULE_NCUBE : NCUBE
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
		return null
	}

	String getSourceMessage()
	{
		return ''
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

	Map<String, Object> createNode(VisualizerInfo visInfo, String group = null)
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

		node.details = getDetails(visInfo)
		group = group ?: getGroupName(visInfo)
		node.group = group
		node.typesToAdd = visInfo.getTypesToAdd(group)

		node.executeCell = executeCell
		node.executeCells = executeCell
		node.showCellValuesLink = showCellValuesLink
		node.showCellValues = showCellValues
		node.cellValuesLoaded = cellValuesLoaded

		visInfo.availableGroupsAllLevels << group - visInfo.groupSuffix
		long maxLevel = visInfo.maxLevel
		visInfo.maxLevel = maxLevel < targetLevel ? targetLevel : maxLevel
		visInfo.nodeCount += 1

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