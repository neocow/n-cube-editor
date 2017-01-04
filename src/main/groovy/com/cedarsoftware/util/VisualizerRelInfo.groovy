package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.CommandCell
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.UrlCommandCell
import com.cedarsoftware.ncube.util.LongHashSet
import com.google.common.base.Joiner
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Holds information about a source cube and target cube for purposes of creating a visualization of the cubes and their relationship.
 */

@CompileStatic
class VisualizerRelInfo
{
	protected Joiner.MapJoiner mapJoiner = Joiner.on(", ").withKeyValueSeparator(": ")

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

	Boolean cellValuesLoaded
	boolean showAllCellValues = false
	String processingCoordinate
	boolean executingUrlCommandCells = false
	String executeUrlCommandCell
	boolean executeUrlCommandCells = false
	boolean executingNonUrlCommandCells = false
	String executeNonUrlCommandCell
	boolean executeNonUrlCommandCells = false

	Map<String, Map<String, Object>> targetCellValues
	Map<String, Map<String, Object>> sourceCellValues

	List<String> typesToAdd

	VisualizerRelInfo() {}

	VisualizerRelInfo(ApplicationID appId, Map node)
	{
		targetCube = NCubeManager.getCube(appId, node.cubeName as String)
		String sourceCubeName = node.sourceCubeName as String
		sourceCube = sourceCubeName ? NCubeManager.getCube(appId, sourceCubeName) : null
		sourceFieldName = node.fromFieldName
		targetId = Long.valueOf(node.id as String)
		targetLevel = Long.valueOf(node.level as String)
		targetScope = node.scope as CaseInsensitiveMap
		scope = node.availableScope as CaseInsensitiveMap
		showAllCellValues = node.showAllCellValues as boolean
		executeUrlCommandCell = node.executeUrlCommandCell as String
		executeUrlCommandCells = node.executeUrlCommandCells as boolean
		executeNonUrlCommandCell = node.executeNonUrlCommandCell as String
		executeNonUrlCommandCells = node.executeNonUrlCommandCells as boolean
		setExecutingFlags()
		cellValuesLoaded = node.cellValuesLoaded as Boolean
		typesToAdd = node.typesToAdd as List
	}

	void loadCellValues(ApplicationID appId, VisualizerInfo visInfo)
	{
		targetCellValues = [:]
		cellValuesLoaded = null
		if (showAllCellValues)
		{
			Map<LongHashSet, Object> cellMap = targetCube.getCellMap()
			cellMap.each { LongHashSet ids, Object noExecuteCell ->
				Map<String, Object> coordinate = targetCube.getCoordinateFromIds(ids)
				String processingCoordinate = getCoordinateString(coordinate)
				Object cell
				if (noExecuteCell instanceof CommandCell)
				{
					CommandCell cmd = noExecuteCell as CommandCell
					if (cmd.url)
					{
						if(executeUrlCommandCells || processingCoordinate == executeUrlCommandCell)
						{
							cell = targetCube.getCell(coordinate)
						}
						else
						{
							cell = noExecuteCell
						}
					}
					else if (executeNonUrlCommandCells || processingCoordinate == executeNonUrlCommandCell)
					{
						cell = targetCube.getCell(coordinate)
					}
					else
					{
						cell = noExecuteCell
					}
				}
				else
				{
					cell = targetCube.getCell(coordinate)
				}
				String cellString = cell == null ? 'null' : cell.toString()
				targetCellValues[processingCoordinate] = [(cellString): noExecuteCell]
			}
			targetCellValues.sort()
			cellValuesLoaded = true
		}
	}

	private boolean setExecutingFlags()
	{
		executingUrlCommandCells = executeUrlCommandCell || executeUrlCommandCells
		executingNonUrlCommandCells = executeNonUrlCommandCell || executeNonUrlCommandCells
	}

	void setExecuteTriggers(Map node)
	{
		node.executeUrlCommandCell = executeUrlCommandCell
		node.executeUrlCommandCells = executeUrlCommandCells
		node.executeNonUrlCommandCell = executeNonUrlCommandCell
		node.executeNonUrlCommandCells = executeNonUrlCommandCells
	}

	void resetAllExecuteTriggers(Map node)
	{
		executeUrlCommandCell = null
		executeUrlCommandCells = false
		executeNonUrlCommandCell = null
		executeNonUrlCommandCells = false
		setExecuteTriggers(node)
	}

	void resetExecuteTriggers()
	{
		if (executingUrlCommandCells)
		{
			executeUrlCommandCell = null
			executeUrlCommandCells = false
		}
		else if (executingNonUrlCommandCells)
		{
			executeNonUrlCommandCell = null
			executeNonUrlCommandCells = false
		}
	}

	private String getCoordinateString(Map<String, Object> coordinates)
	{
		coordinates.each {String key, Object value ->
			if (!value)
			{
				coordinates[key] = 'null'
			}
		}
		return mapJoiner.join(coordinates)
	}

	Set<String> getRequiredScope()
	{
		return targetCube.getRequiredScope(targetScope, [:] as Map)
	}

	String getDetails(VisualizerInfo visInfo)
	{
		StringBuilder sb = new StringBuilder()
		String notesLabel = "<b>Note: </b>"
		String targetCubeName = targetCube.name

		if (false == cellValuesLoaded)
		{
			sb.append("<b>*** Unable to load cell value for coordinate ${processingCoordinate}</b>${DOUBLE_BREAK}")
			processingCoordinate = null
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

		getDetailsMap(sb, 'Scope', targetScope)
		getDetailsMap(sb, 'Available scope', scope)
		getDetailsSet(sb, 'Required scope keys', visInfo.requiredScopeKeys[targetCubeName])
		getDetailsSet(sb, 'Optional scope keys', visInfo.optionalScopeKeys[targetCubeName])

		//Cell values
		if (null != cellValuesLoaded && showAllCellValues)
		{
			addCellValueSection(sb)
		}

		return sb.toString()
	}

	private void addCellValueSection(StringBuilder sb)
	{
		StringBuilder cellValuesBuilder = new StringBuilder()
		StringBuilder linkBuilder = new StringBuilder()
		sb.append("<b>Cell values</b>")
		getCellValues(cellValuesBuilder, linkBuilder )
		sb.append(linkBuilder.toString())
		sb.append("<pre><ul>")
		sb.append(cellValuesBuilder.toString())
		sb.append("</ul></pre>")
	}

	private void getCellValues(StringBuilder cellValuesBuilder, StringBuilder linkBuilder)
	{
		boolean hasNonExecutedUrlCommandCells = false
		boolean hasNonExecutedNonUrlCommandCells = false
		boolean hasUrlLinks = false
		boolean hasStringValue = false
		String id = String.valueOf(targetId)

		targetCellValues.each { String coordinate, Map<String, Object> cellValues ->
			cellValues.each { String cellString, Object noExecuteCell ->
				if (noExecuteCell instanceof CommandCell)
				{
					CommandCell cmd = noExecuteCell as CommandCell
					if (cmd.url)
					{
						if (executeUrlCommandCells || coordinate == executeUrlCommandCell)
						{
							//The executed URL cell value is displayed as text.
							cellValuesBuilder.append("<li>${coordinate} <b>==></b> ${cellString}</li>")
							hasStringValue = true
						}
						else
						{
							//The non-executed URL cell value is displayed as a link. If clicked, the cell is executed.
							cellValuesBuilder.append("""<li><a class="executeUrlCommandCell" id="${id}" title="${coordinate}" href="#">${coordinate} <b>==></b>  ${noExecuteCell}</a></li>""")
							hasNonExecutedUrlCommandCells = true
						}
					}
					else if (executeNonUrlCommandCells || coordinate == executeNonUrlCommandCell)
					{
						//The executed non-URL cell value is displayed as text.
						cellValuesBuilder.append("<li>${coordinate} <b>==></b> ${cellString}</li>")
						hasStringValue = true
					}
					else
					{
						//The non-executed non-URL cell value is displayed as a link. If clicked, the cell is executed.
						cellValuesBuilder.append("""<li><a class="executeNonUrlCommandCell" id="${id}" title="${coordinate}" href="#">${coordinate} <b>==></b>  ${noExecuteCell}</a></li>""")
						hasNonExecutedNonUrlCommandCells = true
					}
				}
				else if (cellString.startsWith(HTTP) || cellString.startsWith(HTTPS) || cellString.startsWith(FILE))
				{
					//The executed cell value is displayed as a link. If clicked, the link opens in a new window.
					cellValuesBuilder.append("""<li>${coordinate} <b>==></b>  <a href="#" onclick='window.open("${cellString}");return false;'>${cellString}</a></li>""")
				}
				else
				{
					//The executed cell value is displayed as text.
					cellValuesBuilder.append("<li>${coordinate} <b>==></b> ${cellString}</li>")
				}
			}
		}

		if (hasNonExecutedUrlCommandCells || hasNonExecutedNonUrlCommandCells)
		{
			linkBuilder.append(DOUBLE_BREAK)
			if (hasNonExecutedUrlCommandCells)
			{
				linkBuilder.append("""${SPACE}<a class="executeUrlCommandCells" id="${id}" href="#">Execute all URL cells</a>""")
				linkBuilder.append(BREAK)
			}
			if (hasNonExecutedNonUrlCommandCells)
			{
				linkBuilder.append("""${SPACE}<a class="executeNonUrlCommandCells" id="${id}" href="#">Execute all command cells</a>""")
				linkBuilder.append(BREAK)
			}
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

		node.executeUrlCommandCell = executeUrlCommandCell
		node.executeUrlCommandCells = executeUrlCommandCells
		node.executeNonUrlCommandCell = executeNonUrlCommandCell
		node.executeNonUrlCommandCells = executeNonUrlCommandCells
		node.showAllCellValues = showAllCellValues
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