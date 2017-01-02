package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.exception.InvalidCoordinateException
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Provides the information used to visualize n-cubes.
 */

@CompileStatic
class Visualizer
{
	protected ApplicationID appId
	protected Set<String> messages = []
	protected Set<String> visited = []
	protected Deque<VisualizerRelInfo> stack = new ArrayDeque<>()

	/**
	 * Provides the information used to visualize n-cubes.
	 *
	 * @param applicationID
	 * @param options - a map containing:
	 *           String startCubeName, name of the starting cube,
	 *           Map scope, the context for which the visualizer is loaded
	 *           VisualizerInfo, information about the visualization
     * @return a map containing status, messages and visualizer information
     */
	Map<String, Object> buildGraph(ApplicationID applicationID, Map options)
	{
		appId = applicationID
		String startCubeName = options.startCubeName as String
		VisualizerInfo visInfo = getVisualizerInfo(options)

		if (!isValidStartCube(startCubeName))
		{
			return [status: STATUS_INVALID_START_CUBE, visInfo: visInfo, message: messages.join(DOUBLE_BREAK)]
		}

		if (hasMissingMinimumScope(visInfo, startCubeName))
		{
			return [status: STATUS_MISSING_START_SCOPE, visInfo: visInfo, message: messages.join(DOUBLE_BREAK)]
		}

		getVisualization(visInfo, startCubeName)

		String message = messages.empty ? null : messages.join(DOUBLE_BREAK)
		return [status: STATUS_SUCCESS, visInfo: visInfo, message: message]
	}

	protected VisualizerInfo getVisualizerInfo(Map options)
	{
		VisualizerInfo visInfo = options.visInfo as VisualizerInfo
		if (!visInfo || visInfo.class.name != VisualizerInfo.class.name)
		{
			visInfo = new VisualizerInfo(appId, options)
		}
		visInfo.scope = options.scope as CaseInsensitiveMap ?: new CaseInsensitiveMap<>()
		return visInfo
	}

	protected void getVisualization(VisualizerInfo visInfo, String startCubeName)
	{
		visInfo.maxLevel = 1
		visInfo.nodeCount = 1
		visInfo.availableGroupsAllLevels = [] as Set
		VisualizerRelInfo relInfo = getVisualizerRelInfo(visInfo, startCubeName)
		stack.push(relInfo)

		while (!stack.empty)
		{
			processCube(visInfo, stack.pop())
		}
	}

	protected VisualizerRelInfo getVisualizerRelInfo(VisualizerInfo visInfo, String startCubeName)
	{
		VisualizerRelInfo relInfo = new VisualizerRelInfo()
		relInfo.targetCube = NCubeManager.getCube(appId, startCubeName)
		relInfo.scope = visInfo.scope
		relInfo.targetLevel = 1
		relInfo.targetId = 1
		relInfo.targetScope = [:]
		return relInfo
	}

	protected void processCube(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		messages << "*** UNDER CONSTRUCTION *** ${DOUBLE_BREAK} Full visualization of non-rpm cubes is not yet available.".toString()

		NCube targetCube = relInfo.targetCube
		String targetCubeName = targetCube.name

		relInfo.addRequiredAndOptionalScopeKeys(visInfo)

		if (relInfo.sourceCube)
		{
			visInfo.edges << relInfo.createEdge(visInfo.edges.size())
		}

		if (!visited.add(targetCubeName + relInfo.scope.toString()))
		{
			return
		}

		visInfo.nodes << relInfo.createNode(visInfo)

		Map<Map, Set<String>> refs = targetCube.referencedCubeNames

		refs.each {Map coordinates, Set<String> cubeNames ->
			cubeNames.each { String cubeName ->
				addToStack(visInfo, relInfo, cubeName, coordinates)
			}
		}
	}

	protected VisualizerRelInfo addToStack(VisualizerInfo visInfo, VisualizerRelInfo relInfo, String nextTargetCubeName, Map coordinates = [:])
	{
		VisualizerRelInfo nextRelInfo
		NCube nextTargetCube = NCubeManager.getCube(appId, nextTargetCubeName)
		if (nextTargetCube)
		{
			try
			{
				nextRelInfo = visualizerRelInfo

				long nextTargetTargetLevel = relInfo.targetLevel + 1
				long maxLevel = visInfo.maxLevel
				visInfo.maxLevel = maxLevel < nextTargetTargetLevel ? nextTargetTargetLevel : maxLevel
				visInfo.nodeCount += 1

				nextRelInfo.targetId = visInfo.nodeCount
				nextRelInfo.targetLevel = nextTargetTargetLevel
				nextRelInfo.targetCube = nextTargetCube
				nextRelInfo.sourceCube = relInfo.targetCube
				nextRelInfo.sourceScope = relInfo.targetScope
				nextRelInfo.sourceId = relInfo.targetId

				nextRelInfo.targetScope = coordinates
				nextRelInfo.scope = relInfo.targetScope
				nextRelInfo.scope.putAll(coordinates)
				nextRelInfo.sourceFieldName = coordinates.toString()

				stack.push(nextRelInfo)
			}
			catch (Exception e)
			{
				throw new IllegalStateException("Error processing ${relInfo.sourceFieldName} in cube ${nextTargetCubeName}.", e)
			}
		}
		else
		{
			messages << "No cube exists with name of ${nextTargetCubeName}. Cube not included in the visualization.".toString()
		}
		return nextRelInfo
	}

	protected VisualizerRelInfo getVisualizerRelInfo()
	{
		return new VisualizerRelInfo()
	}

	protected boolean isValidStartCube(String cubeName)
	{
		return true
	}

	protected boolean hasMissingMinimumScope(VisualizerInfo visInfo, String startCubeName)
	{
		return false
	}

	protected String handleCoordinateNotFoundException(CoordinateNotFoundException e, VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		String cubeName = e.cubeName
		String axisName = e.axisName
		Object value = e.value ?: 'null'

		if (cubeName && axisName)
		{
			String effectiveName = relInfo.effectiveNameByCubeName
			String msg = getCoordinateNotFoundMessage(visInfo, relInfo, axisName, value, effectiveName, cubeName)
			relInfo.notes << msg
			messages << msg
			return SCOPE_VALUE_NOT_FOUND
		}
		else
		{
			return handleException(e as Exception, relInfo)
		}
	}

	protected String handleInvalidCoordinateException(InvalidCoordinateException e, VisualizerInfo visInfo, VisualizerRelInfo relInfo, Set mandatoryScopeKeys)
	{
		Set<String> missingScope = findMissingScope(relInfo.scope, e.requiredKeys, mandatoryScopeKeys)

		if (missingScope)
		{
			Map<String, Object> expandedScope = new CaseInsensitiveMap<>(visInfo.scope)
			missingScope.each { String key ->
				expandedScope[key] = DEFAULT_SCOPE_VALUE
			}
			visInfo.scope = expandedScope
			String effectiveName = relInfo.effectiveNameByCubeName
			String msg = getInvalidCoordinateExceptionMessage(visInfo, relInfo, missingScope, effectiveName, e.cubeName)
			relInfo.notes << msg
			messages << msg
			return MISSING_SCOPE
		}
		else
		{
			throw new IllegalStateException("InvalidCoordinateException thrown, but no missing scope keys found for ${relInfo.targetCube.name} and scope ${visInfo.scope.toString()}.", e)
		}
	}

	protected static Set<String> findMissingScope(Map<String, Object> scope, Set<String> requiredKeys, Set mandatoryScopeKeys)
	{
		return requiredKeys.findAll { String key ->
            !mandatoryScopeKeys.contains(key) && (scope == null || !scope.containsKey(key))
        }
	}

	protected String handleException(Throwable e, VisualizerRelInfo relInfo)
	{
		Throwable t = getDeepestException(e)
		String effectiveName = relInfo.effectiveNameByCubeName
		String msg = getExceptionMessage(relInfo, effectiveName, e, t)
		relInfo.notes << msg
		messages << msg
		return UNABLE_TO_LOAD
	}

	protected static Throwable getDeepestException(Throwable e)
	{
		while (e.cause != null)
		{
			e = e.cause
		}
		return e
	}

	protected static String getAvailableScopeValuesMessage(VisualizerInfo visInfo, String cubeName, String key)
	{
		Set<Object> scopeValues = visInfo.availableScopeValues[key] ?: visInfo.loadAvailableScopeValues(cubeName, key)
		if (scopeValues) {
			StringBuilder sb = new StringBuilder()
			sb.append("${BREAK}The following values are available for ${key}:${DOUBLE_BREAK}<pre><ul>")
			scopeValues.each{
				String value = it.toString()
				sb.append("""<li><a class="missingScope" title="${key}: ${value}" href="#">${value}</a></li>""")
			}
			sb.append("</ul></pre>")
			return sb.toString()
		}
		return ''
	}

	protected static String getMissingMinimumScopeMessage(Map<String, Object> scope, String messageScopeValues, String messageSuffixType, String messageSuffix )
	{
		"""\
The scope for the following scope keys was added since it was required: \
${DOUBLE_BREAK}${INDENT}${scope.keySet().join(COMMA_SPACE)}\
${messageSuffixType} ${messageSuffix} \
${BREAK}${messageScopeValues}"""
	}

	protected static String getExceptionMessage(VisualizerRelInfo relInfo, String effectiveName, Throwable e, Throwable t)
	{
		String cubeDisplayName = relInfo.getCubeDisplayName(relInfo.targetCube.name)
		"""\
An exception was thrown while loading for ${cubeDisplayName}${relInfo.sourceMessage} for ${effectiveName}. \
${DOUBLE_BREAK}<b>Message:</b> ${DOUBLE_BREAK}${e.message}${DOUBLE_BREAK}<b>Root cause: </b>\
${DOUBLE_BREAK}${t.toString()}${DOUBLE_BREAK}<b>Stack trace: </b>${DOUBLE_BREAK}${t.stackTrace.toString()}"""
	}

	protected String getCoordinateNotFoundMessage(VisualizerInfo visInfo, VisualizerRelInfo relInfo, String key, Object value, String effectiveName, String cubeName)
	{
		StringBuilder message = new StringBuilder()
		String messageScopeValues = getAvailableScopeValuesMessage(visInfo, cubeName, key)
		message.append("The scope value ${value} for scope key ${key} cannot be found on axis ${key} in cube ${cubeName} for ${effectiveName}.")
		message.append("${DOUBLE_BREAK} Please supply a different value for ${key}.${BREAK}${messageScopeValues}")
	}

	protected String getInvalidCoordinateExceptionMessage(VisualizerInfo visInfo, VisualizerRelInfo relInfo, Set<String> missingScope, String effectiveName, String cubeName)
	{
		StringBuilder message = new StringBuilder()
		message.append("Additional scope is required to load cube ${cubeName} for ${effectiveName}${relInfo.sourceMessage}.")
		message.append("${DOUBLE_BREAK} Please add scope value(s) for the following scope key(s): ${missingScope.join(COMMA_SPACE)}.${BREAK}")
		missingScope.each{ String key ->
			message.append(getAvailableScopeValuesMessage(visInfo, cubeName, key))
		}
		return message.toString()
	}
}