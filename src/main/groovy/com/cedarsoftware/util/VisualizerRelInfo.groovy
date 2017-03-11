package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.Axis
import com.cedarsoftware.ncube.Column
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.util.LongHashSet
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Provides information to visualize a source cube, a target cube
 * and their relationship.
 */

@CompileStatic
class VisualizerRelInfo
{
	protected ApplicationID appId

	protected List<String> nodeDetailsMessages = []
	protected Map<String, Object> availableTargetScope = new CaseInsensitiveMap()
	Map<String, Set<Object>> availableScopeValues = new CaseInsensitiveMap()
	Map<String, Set<String>> scopeCubeNames = new CaseInsensitiveMap()

	protected boolean showingHidingCellValues

	protected long targetId
	protected NCube targetCube
	protected Map<String, Object> targetScope = new CaseInsensitiveMap()
	protected long targetLevel
	protected String nodeLabelPrefix = ''

	protected long sourceId
	protected NCube sourceCube
	protected Map<String, Object> sourceScope
	protected String sourceFieldName

	protected boolean cubeLoaded
	protected boolean showCellValuesLink
	protected boolean showCellValues
	protected boolean loadAgain

	protected List<VisualizerCellInfo> cellInfo

	protected List<String> typesToAdd

	protected VisualizerHelper helper = new VisualizerHelper()

	VisualizerRelInfo() {}

	protected VisualizerRelInfo(ApplicationID applicationId)
	{
		appId = applicationId
	}

	protected init(Map options, VisualizerInfo visInfo)
	{
		Map node = options.node as Map
		if (node)
		{
			targetCube = NCubeManager.getCube(appId, node.cubeName as String)
			String sourceCubeName = node.sourceCubeName as String
			sourceCube = sourceCubeName ? NCubeManager.getCube(appId, sourceCubeName) : null
			sourceFieldName = node.fromFieldName
			targetId = Long.valueOf(node.id as String)
			targetLevel = Long.valueOf(node.level as String)
			availableTargetScope = node.availableScope as CaseInsensitiveMap ?:  new CaseInsensitiveMap()
			availableScopeValues = node.availableScopeValues as CaseInsensitiveMap ?:  new CaseInsensitiveMap()
			scopeCubeNames = node.scopeCubeNames as CaseInsensitiveMap ?:  new CaseInsensitiveMap()
			showingHidingCellValues = node.showingHidingCellValues as boolean
			if (!showingHidingCellValues)
			{
				availableTargetScope.keySet().removeAll(availableScopeValues.keySet())
				availableScopeValues = new CaseInsensitiveMap()
				scopeCubeNames = new CaseInsensitiveMap()
			}
			showCellValues = node.showCellValues as boolean
			showCellValuesLink = node.showCellValuesLink as boolean
			cubeLoaded = node.cubeLoaded as boolean
			typesToAdd = node.typesToAdd as List
		}
		else
		{
			targetId = 1
			targetLevel = 1
			targetCube = NCubeManager.getCube(appId, options.startCubeName as String)
			addRequiredScopeKeys(visInfo)
			showCellValuesLink = true
		}
		visInfo.selectedNodeId = targetId
		populateScopeDefaults(visInfo)
	}

	protected boolean loadCube(VisualizerInfo visInfo)
	{
		cellInfo = []
		cubeLoaded = true
		if (showCellValues)
		{
			Map<LongHashSet, Object> cellMap = targetCube.cellMap
			cellMap.each { LongHashSet ids, Object noExecuteCell ->
				Map<String, Object> coordinate = availableTargetScope as CaseInsensitiveMap ?: new CaseInsensitiveMap()
				coordinate.putAll(targetCube.getCoordinateFromIds(ids))
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
			cellInfo.sort { VisualizerCellInfo cellInfo ->
				cellInfo.coordinate.toString()
			}
		}
		return true
	}

	protected Set<String> getRequiredScope()
	{
		return targetCube.getRequiredScope(availableTargetScope, new CaseInsensitiveMap())
	}

	protected String getDetails(VisualizerInfo visInfo)
	{
		StringBuilder sb = new StringBuilder()

		getDetailsMap(sb, 'Scope', targetScope)
		getDetailsMap(sb, 'Available scope', availableTargetScope)
		getDetailsSet(sb, 'Axes', targetCube.axisNames)

		//Cell values
		if (cubeLoaded && showCellValues)
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
		sb.append("""<pre><ul class="${DETAILS_CLASS_CELL_VALUES}">""")
		sb.append(cellValuesBuilder.toString())
		sb.append("</ul></pre>")
	}

	private void getCellValues(VisualizerInfo visInfo, StringBuilder cellValuesBuilder, StringBuilder linkBuilder)
	{
		Long id = 0l

		if (cellInfo)
		{
			cellInfo.each { VisualizerCellInfo visCellInfo ->
				visCellInfo.getCellValue(visInfo, this, id++, cellValuesBuilder)
			}

			linkBuilder.append(DOUBLE_BREAK)
			linkBuilder.append("""<a href="#" title="Expand all cell details" class="${DETAILS_CLASS_EXPAND_ALL}"">Expand all</a>""")
			linkBuilder.append("${SPACE}${SPACE}")
			linkBuilder.append("""<a href="#" title="Collapse all cell details" class="${DETAILS_CLASS_COLLAPSE_ALL}"">Collapse all</a>""")
			linkBuilder.append(BREAK)
		}
		else
		{
			cellValuesBuilder.append('none')
		}
	}

	protected static void getDetailsMap(StringBuilder sb, String title, Map<String, Object> map)
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

	protected static void getDetailsSet(StringBuilder sb, String title, Collection<String> set)
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

	protected String getGroupName(VisualizerInfo visInfo = null, String cubeName = targetCube.name)
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

	/**
	 *  If the required scope keys have not already been loaded for this cube,
	 *  load them.
	 */
	protected void addRequiredScopeKeys(VisualizerInfo visInfo)
	{
		String cubeName = targetCube.name
		if (!visInfo.requiredScopeKeysByCube.containsKey(cubeName))
		{
			visInfo.requiredScopeKeysByCube[cubeName] = requiredScope
			//visInfo.allOptionalScopeKeysByCube[cubeName] = targetCube.getOptionalScope(availableTargetScope, new CaseInsensitiveMap())
		}
	}

	protected Map<String, Object> createEdge(int edgeCount)
	{
		String sourceFieldName = sourceFieldName
		Map<String, Object> edge = [:]
		edge.id = String.valueOf(edgeCount + 1)
		edge.from = String.valueOf(sourceId)
		edge.to = String.valueOf(targetId)
		edge.fromName = getLabel(sourceCube.name)
		edge.toName = getLabel(targetCube.name)
		edge.fromFieldName = sourceFieldName
		edge.level = String.valueOf(targetLevel)
		edge.title = sourceFieldName
		return edge
	}

	protected Map<String, Object> createNode(VisualizerInfo visInfo, String group = null)
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
		node.availableScope = availableTargetScope
		node.availableScopeValues = availableScopeValues
		node.scopeCubeNames = scopeCubeNames
		node.fromFieldName = sourceFieldName
		node.sourceDescription = sourceCubeName ? sourceDescription : null
		node.title = getCubeDisplayName(targetCubeName)
		group = group ?: getGroupName(visInfo)
		node.group = group
		node.showCellValuesLink = showCellValuesLink
		node.showCellValues = showCellValues
		node.cubeLoaded = cubeLoaded
		String label = getLabel(targetCubeName)
		node.label = nodeLabelPrefix + label
		node.detailsTitle1 = cubeDetailsTitle1
		node.detailsTitle2 = cubeDetailsTitle2
		node.typesToAdd = visInfo.getTypesToAdd(group)

		if (targetId == visInfo.selectedNodeId)
		{
			node.details = getDetails(visInfo)
		}

		visInfo.availableGroupsAllLevels << group - visInfo.groupSuffix
		long maxLevel = visInfo.maxLevel
		visInfo.maxLevel = maxLevel < targetLevel ? targetLevel : maxLevel
		visInfo.nodeCount += 1
		return node
	}

	protected void populateScopeDefaults(VisualizerInfo visInfo) {}

	protected void addNodeScope(String cubeName, String scopeKey, boolean skipAvailableScopeValues = false, Map coordinate)
	{
		availableScopeValues = availableScopeValues ?: new CaseInsensitiveMap<String, Set<Object>>()
		scopeCubeNames = scopeCubeNames ?: new CaseInsensitiveMap<String, Set<String>>()
		addAvailableValues(cubeName, scopeKey, skipAvailableScopeValues, coordinate)
		addCubeNames(scopeKey, cubeName)
	}

	private void addAvailableValues(String cubeName, String scopeKey, boolean skipAvailableScopeValues, Map coordinate)
	{
		Set<Object> scopeValues = availableScopeValues[scopeKey] as Set ?: new LinkedHashSet()
		if (skipAvailableScopeValues)
		{
			availableScopeValues[scopeKey] = scopeValues
		}
		else
		{
			Set scopeValuesThisCube = getColumnValues(cubeName, scopeKey, coordinate)
			if (availableScopeValues.containsKey(scopeKey))
			{
				availableScopeValues[scopeKey] = scopeValues.intersect(scopeValuesThisCube) as Set
			}
			else
			{
				availableScopeValues[scopeKey] = scopeValuesThisCube
			}
		}
	}

	protected void addCubeNames(String scopeKey, String valueToAdd)
	{
		Set<String> values = scopeCubeNames[scopeKey] as Set ?: new LinkedHashSet()
		values << valueToAdd
		scopeCubeNames[scopeKey] = values
	}

	protected Set<Object> getColumnValues(String cubeName, String axisName, Map coordinate)
	{
		NCube cube = NCubeManager.getCube(appId, cubeName)
		return getAllColumnValues(cube, axisName)
	}

	protected static Set<Object> getAllColumnValues(NCube cube, String axisName)
	{
		Set values = new LinkedHashSet()
		Axis axis = cube.getAxis(axisName)
		if (axis)
		{
			for (Column column : axis.columns)
			{
				values.add(column.value)
			}
		}
		return values
	}

	protected String createNodeDetailsScopeMessage()
	{
		availableScopeValues =  availableScopeValues ?: new CaseInsensitiveMap<String, Set<Object>> ()
		scopeCubeNames = scopeCubeNames ?: new CaseInsensitiveMap<String, Set<String>> ()

		StringBuilder sb = new StringBuilder()
		sb.append(getNodeDetailsMessageSet())
		sb.append(nodeScopeMessage)
		sb.append("""<a href="#" title="Reset scope to original defaults" class="scopeReset">Reset scope</a>""")
		sb.append("${DOUBLE_BREAK}")
		return sb.toString()
	}

	private StringBuilder getNodeDetailsMessageSet()
	{
		StringBuilder sb = new StringBuilder()
		if (nodeDetailsMessages)
		{
			nodeDetailsMessages.each { String message ->
				sb.append("${message}")
			}
		}
		return sb
	}

	private StringBuilder getNodeScopeMessage()
	{
		String nodeName = getLabel()
		StringBuilder sb = new StringBuilder()
		if (availableScopeValues)
		{
			Map sortedMap = availableScopeValues.sort()
			sortedMap.keySet().each { String scopeKey ->
				Set<String> cubeNames = scopeCubeNames[scopeKey]
				cubeNames.remove(null)
				Set<Object> availableValues = availableScopeValues[scopeKey]
				String requiredOrOptional = availableValues.contains(null) ? 'optional' : 'required'
				StringBuilder title = new StringBuilder("Scope key ${scopeKey} is ${requiredOrOptional} to load ${nodeName}")
				title.append(addCubeNamesList('.\n\nFirst encountered on the following cubes, but may also be present on others:', cubeNames))
				sb.append(getScopeMessage(scopeKey, availableValues, title, availableTargetScope[scopeKey]))
				sb.append(BREAK)
			}
		}
		else{
			sb.append("<b>No scope</b>")
			sb.append(BREAK)
		}
		return sb
	}

	protected StringBuilder getScopeMessage(String scopeKey, Set<Object> availableScopeValues, StringBuilder title, Object providedScopeValue)
	{
		String value
		StringBuilder sb = new StringBuilder()
		String caret = availableScopeValues ? """<span class="caret"></span>""" : ''
		String placeHolder = availableScopeValues ? 'Select or enter value...' : 'Enter value...'
		String topNodeClass = targetId == 1l ? 'topNode' : ''
		String highlightedClass = ''

		if (availableScopeValues.contains(null))
		{
			value = providedScopeValue ?: 'Default'
		}
		else
		{
			value = providedScopeValue ?: ''
		}

		sb.append("""<div class="input-group" title="${title}">""")
		sb.append("""<div class="input-group-btn">""")
		sb.append("""<button type="button" class="btn btn-default dropdown-toggle"  data-toggle="dropdown">${scopeKey} ${caret}</button>""")
		if (availableScopeValues)
		{
			highlightedClass = availableScopeValues.contains(providedScopeValue) ? highlightedClass : DETAILS_CLASS_HIGHLIGHTED
			sb.append("""<ul class="dropdown-menu">""")
			availableScopeValues.each {Object scopeValue ->
				if (scopeValue)
				{
					sb.append("""<li id="${scopeKey}: ${scopeValue}" class="${DETAILS_CLASS_SCOPE_CLICK} ${topNodeClass}" style="color: black;">${scopeValue}</li>""")
				}
				else
				{
					sb.append("""<li id="${scopeKey}: Default" class="${DETAILS_CLASS_SCOPE_CLICK} ${topNodeClass}" style="color: black;">Default</li>""")
				}
			}
			sb.append("""</ul>""")
		}
		sb.append("""</div>""")
		sb.append("""<input id="${scopeKey}" value="${value}" placeholder="${placeHolder}" class="${DETAILS_CLASS_SCOPE_INPUT} ${DETAILS_CLASS_FORM_CONTROL} ${highlightedClass} ${topNodeClass}" style="color: black;" type="text">""")
		sb.append("""</div>""")
		return sb
	}

	protected void setLoadAgain(VisualizerInfo visInfo, String scopeKey)
	{
		loadAgain = false
	}

	private static StringBuilder addCubeNamesList(String prefix, Set<String> cubeNames)
	{
		StringBuilder sb = new StringBuilder()
		if (cubeNames)
		{
			sb.append("${prefix}\n\n")
			cubeNames.each { String cubeName ->
				sb.append("  - ${cubeName}\n")
			}
		}
		return sb
	}

	protected String getLoadTarget()
	{
		return showingHidingCellValues ? "${cellValuesLabel}" : "the ${nodeLabel}"
	}

	protected String getNodesLabel()
	{
		return 'cubes'
	}

	protected String getNodeLabel()
	{
		return 'cube'
	}

	protected String getCellValuesLabel()
	{
		return 'cell values'
	}

	protected boolean includeUnboundScopeKey(VisualizerInfo visInfo, String scopeKey)
	{
		return true
	}

	protected String getLabel(String cubeName = targetCube.name)
	{
		cubeName
	}

	protected String getCubeDisplayName(String cubeName)
	{
		return cubeName
	}

	protected String getSourceDescription()
	{
		return sourceCube.name
	}

	protected String getCubeDetailsTitle1()
	{
		return targetCube.name
	}

	protected String getCubeDetailsTitle2()
	{
		return null
	}
}