package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.Axis
import com.cedarsoftware.ncube.Column
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.BREAK
import static com.cedarsoftware.util.VisualizerConstants.DETAILS_CLASS_FORM_CONTROL
import static com.cedarsoftware.util.VisualizerConstants.DETAILS_CLASS_HIGHLIGHTED
import static com.cedarsoftware.util.VisualizerConstants.DETAILS_CLASS_SCOPE_INPUT
import static com.cedarsoftware.util.VisualizerConstants.DETAILS_CLASS_SCOPE_CLICK
import static com.cedarsoftware.util.VisualizerConstants.DETAILS_CLASS_LOAD_CELL_VALUES
import static com.cedarsoftware.util.VisualizerConstants.DOUBLE_BREAK

/**
 * Provides information about the scope used to visualize an n-cube.
 */

@CompileStatic
class VisualizerScopeInfo
{
	protected ApplicationID appId
	protected boolean loadingCellValues
	protected Map<String, Object> inputScope

	protected Map<Long, Map> graphScopeInfo = [:]
	Map<String, Object> scopeDefaults = new CaseInsensitiveMap()


	VisualizerScopeInfo(){}

	protected void init(ApplicationID applicationId, Map options){
		appId = applicationId
		inputScope = options.scope as CaseInsensitiveMap ?: new CaseInsensitiveMap()
	}

	protected void populateScopeDefaults(VisualizerRelInfo relInfo) {}

	protected void addNodeScope(Long targetId, String cubeName, String scopeKey, boolean skipAvailableScopeValues = false, Map coordinate)
	{
		Map nodeScopeInfo = getNodeScopeInfo(targetId)
		Map<String, Set<Object>> nodeScopeAvailableValues = nodeScopeInfo.nodeScopeAvailableValues as CaseInsensitiveMap ?: new CaseInsensitiveMap()
		Map<String, Set<String>> nodeScopeCubeNames = nodeScopeInfo.nodeScopeCubeNames as CaseInsensitiveMap  ?: new CaseInsensitiveMap()
		addAvailableValues(cubeName, scopeKey, nodeScopeAvailableValues, skipAvailableScopeValues, coordinate)
		addCubeNames(scopeKey, nodeScopeCubeNames, cubeName)
		nodeScopeInfo.nodeScopeAvailableValues = nodeScopeAvailableValues
		nodeScopeInfo.nodeScopeCubeNames = nodeScopeCubeNames
	}

	private void addAvailableValues(String cubeName, String scopeKey, Map availableValues, boolean skipAvailableScopeValues, Map coordinate)
	{
		Set<Object> scopeValues = availableValues[scopeKey] as Set ?: new LinkedHashSet()
		if (skipAvailableScopeValues)
		{
			availableValues[scopeKey] = scopeValues
		}
		else
		{
			Set scopeValuesThisCube = getColumnValues(cubeName, scopeKey, coordinate)
			if (availableValues.containsKey(scopeKey))
			{
				availableValues[scopeKey] = scopeValues.intersect(scopeValuesThisCube) as Set
			}
			else
			{
				availableValues[scopeKey] = scopeValuesThisCube
			}
		}
	}

	protected static void addCubeNames(String scopeKey, Map cubeNames, Object valueToAdd)
	{
		Set<Object> values = cubeNames[scopeKey] as Set ?: new LinkedHashSet()
		values << valueToAdd
		cubeNames[scopeKey] = values
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

	protected String createNodeDetailsScopeMessage(VisualizerRelInfo relInfo)
	{
		Map nodeScopeInfo = getNodeScopeInfo(relInfo.targetId)
		Map<String, Set<Object>> nodeScopeAvailableValues = nodeScopeInfo.nodeScopeAvailableValues as CaseInsensitiveMap ?: new CaseInsensitiveMap()
		Map<String, Set<String>> nodeScopeCubeNames = nodeScopeInfo.nodeScopeCubeNames as CaseInsensitiveMap?: new CaseInsensitiveMap()

		StringBuilder sb = new StringBuilder()
		sb.append(getNodeDetailsMessages(relInfo.nodeDetailsMessages))
		sb.append(getNodeScopeMessage(nodeScopeAvailableValues.sort(), nodeScopeCubeNames, relInfo))
		sb.append("${BREAK}")
		return sb.toString()
	}

	protected String createNodeScopeToastMessage(VisualizerRelInfo relInfo)
	{
		Map nodeScopeInfo = getNodeScopeInfo(relInfo.targetId)
		Map<String, Set<Object>> nodeScopeAvailableValues = nodeScopeInfo.nodeScopeAvailableValues as CaseInsensitiveMap ?: new CaseInsensitiveMap()
		Map<String, Set<String>> nodeScopeCubeNames = nodeScopeInfo.nodeScopeCubeNames as CaseInsensitiveMap?: new CaseInsensitiveMap()

		String nodeName = relInfo.getLabel()
		StringBuilder sb = new StringBuilder("""<div id="scopeMessage">""")
		sb.append(BREAK)
		sb.append("<b>${nodeName}</b>")
		sb.append('<hr style="border-top: 1px solid #aaa;margin:2px">')
		sb.append(getNodeDetailsMessages(relInfo.nodeDetailsMessages))
		sb.append(getNodeScopeMessage(nodeScopeAvailableValues.sort(), nodeScopeCubeNames, relInfo))
		sb.append("${DOUBLE_BREAK}")
		sb.append("""<a href="#" title="Reset scope to original defaults" class="scopeReset">Reset scope</a>""")
		sb.append('</div>')
		return sb.toString()
	}

	private static StringBuilder getNodeDetailsMessages(List<String> nodeDetailsMessages)
	{
		StringBuilder sb = new StringBuilder(BREAK)
		if (nodeDetailsMessages)
		{
			nodeDetailsMessages.each { String message ->
				sb.append("${message}")
			}
		}
		return sb
	}

	private StringBuilder getNodeScopeMessage(Map<String, Set<Object>> availableValuesMap, Map<String, Set<String>> cubeNamesMap, VisualizerRelInfo relInfo)
	{
		String nodeName = relInfo.getLabel()
		Map nodeAvailableScope = relInfo.availableTargetScope
		StringBuilder sb = new StringBuilder()
		if (availableValuesMap)
		{
			availableValuesMap.keySet().each { String scopeKey ->
				sb.append(BREAK)
				Set<String> cubeNames = cubeNamesMap[scopeKey]
				cubeNames.remove(null)
				Set<Object> availableValues = availableValuesMap[scopeKey]
				String requiredOrOptional = availableValues.contains(null) ? 'optional' : 'required'
				StringBuilder title = new StringBuilder("Scope key ${scopeKey} is ${requiredOrOptional} to load ${nodeName}")
				title.append(addCubeNamesList('.\n\nFirst encountered on the following cubes, but may also be present on others:', cubeNames))
				sb.append(getScopeMessage(scopeKey, availableValues, title, nodeAvailableScope[scopeKey], relInfo.targetId))
			}
		}
		else{
			sb.append("<b>No scope</b>")
		}
		return sb
	}

	protected StringBuilder getScopeMessage(String scopeKey, Set<Object> availableScopeValues, StringBuilder title, Object providedScopeValue, Long targetId)
	{
		String value
		StringBuilder sb = new StringBuilder()
		String caret = availableScopeValues ? """<span class="caret"></span>""" : ''
		String placeHolder = availableScopeValues ? 'Select or enter value...' : 'Enter value...'
		String currentActionClass = loadingCellValues ? DETAILS_CLASS_LOAD_CELL_VALUES : ''
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
					sb.append("""<li id="${scopeKey}: ${scopeValue}" class="${DETAILS_CLASS_SCOPE_CLICK} ${currentActionClass} ${topNodeClass}" style="color: black;">${scopeValue}</li>""")
				}
				else
				{
					sb.append("""<li id="${scopeKey}: Default" class="${DETAILS_CLASS_SCOPE_CLICK} ${currentActionClass} ${topNodeClass}" style="color: black;">Default</li>""")
				}
			}
			sb.append("""</ul>""")
		}
		sb.append("""</div>""")
		sb.append("""<input id="${scopeKey}" value="${value}" placeholder="${placeHolder}" class="${DETAILS_CLASS_SCOPE_INPUT} ${DETAILS_CLASS_FORM_CONTROL} ${currentActionClass} ${highlightedClass} ${topNodeClass}" style="color: black;" type="text">""")
		sb.append("""</div>""")
		return sb
	}

	protected boolean loadAgain(VisualizerRelInfo relInfo, String scopeKey)
	{
		return false
	}

	protected Map<String, CaseInsensitiveMap> getNodeScopeInfo(Long nodeId)
	{
		Map<String, CaseInsensitiveMap> nodeScopeInfo = graphScopeInfo[nodeId] as Map
		if (!nodeScopeInfo)
		{
			nodeScopeInfo = [nodeScopeAvailableValues: new CaseInsensitiveMap(), nodeScopeCubeNames: new CaseInsensitiveMap()]
			graphScopeInfo[nodeId] = nodeScopeInfo
		}
		return nodeScopeInfo
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
		return loadingCellValues ? "${cellValuesLabel}" : "the ${nodeLabel}"
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
}