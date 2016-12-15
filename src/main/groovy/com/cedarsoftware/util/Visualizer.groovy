package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.exception.InvalidCoordinateException
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Provides the information used to visualize rpm cubes associated with a given rpm cube.
 */

// TODO: This code needs to be moved out of NCE and pulled-in via Grapes.
@CompileStatic
class Visualizer
{
	private ApplicationID appId
	private VisualizerHelper helper = new VisualizerHelper()
	private Set<String> messages = []
	private Set<String> visited = []
	private String defaultScopeEffectiveVersion
	private String defaultScopeDate
	private Deque<VisualizerRelInfo> stack = new ArrayDeque<>()

	/**
	 * Provides the information used to visualize rpm cubes associated with a given rpm cube.
	 *
	 * @param applicationID
	 * @param options - a map containing:
	 *            String startCubeName, name of the starting cube
	 *            Map scope, the context for which the visualizer is loaded
	 *            Set selectedGroups, which groups should be included in the visualization
	 *            String selectedLevel, the depth of traversal from the start cube
	 *            Set<String> availableScopeKeys, scope keys available in the visualization
	 *            Map<String, Set<Object>> availableScopeValues, scope values available in the visualization, by scope key
	 *            boolean loadTraits, whether to load and display traits
     * @return a map containing status, messages and visualizer information
     */
	Map<String, Object> buildGraph(ApplicationID applicationID, Map options)
	{
		appId = applicationID
		defaultScopeEffectiveVersion = applicationID.version.replace('.', '-')
		defaultScopeDate = DATE_TIME_FORMAT.format(new Date())

		VisualizerInfo visInfo = new VisualizerInfo(appId, options)

		if (!isValidStartCube(visInfo.startCubeName))
		{
			return [status: STATUS_INVALID_START_CUBE, visInfo: visInfo, message: messages.join(DOUBLE_BREAK)]
		}

		if (hasMissingMinimumScope(visInfo))
		{
			return [status: STATUS_MISSING_START_SCOPE, visInfo: visInfo, message: messages.join(DOUBLE_BREAK)]
		}

		getRpmVisualization(visInfo)

		String message = messages.empty ? null : messages.join(DOUBLE_BREAK)
		return [status: STATUS_SUCCESS, visInfo: visInfo, message: message]
	}

	/**
	 * Loads all traits available for a given class.
	 *
	 * @param applicationID
	 * @param options (map containing:
	 *            Map node, representing a class
	 *            Map scope, the context for which the visualizer is loaded
	 *            Set<String> availableScopeKeys, scope keys available in the visualization
	 *            Map<String, Set<Object>> availableScopeValues, scope values available in the visualization, by scope key
	 * @return node
     */
	Map getTraits(ApplicationID applicationID, Map options)
	{
		appId = applicationID

		VisualizerInfo visInfo = new VisualizerInfo(appId, options)
		VisualizerRelInfo relInfo = new VisualizerRelInfo(appId, options)
		visInfo.loadTraits = relInfo.loadTraits

		getTraitMaps(visInfo, relInfo)
		visInfo.nodes = [relInfo.createNode()]
		visInfo.availableGroupsAllLevels << relInfo.group

		String message = messages.empty ? null : messages.join(DOUBLE_BREAK)
		return [status: STATUS_SUCCESS, visInfo: visInfo, message: message]
	}

	private void getRpmVisualization(VisualizerInfo visInfo)
	{
		VisualizerRelInfo relInfo = new VisualizerRelInfo()
		relInfo.targetCube = NCubeManager.getCube(appId, visInfo.startCubeName)
		relInfo.scope = visInfo.scope
		relInfo.loadTraits = visInfo.loadTraits
		relInfo.targetLevel = 1
		relInfo.targetId = 1
		stack.push(relInfo)

		while (!stack.empty)
		{
			processCube(visInfo, stack.pop())
		}

		addSets(visInfo.availableScopeKeys, visInfo.requiredScopeKeys.values() as Set)
		addSets(visInfo.availableScopeKeys, visInfo.optionalScopeKeys.values() as Set)
        visInfo.trimSelectedLevel()
        visInfo.trimSelectedGroups()
	}

	private static Set<Set> addSets(Set<String> set, Set<Set> sets)
	{
		sets.each {
			set.addAll(it)
		}
		return sets
	}

	private void processCube(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		if (relInfo.targetCube.name.startsWith(RPM_CLASS))
		{
			processClassCube(visInfo, relInfo)
		}
		else
		{
			processEnumCube(visInfo, relInfo)
		}
	}

	private void processClassCube(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		String targetCubeName = relInfo.targetCube.name

		boolean hasFields = canLoadTargetAsRpmClass(relInfo)
		if (hasFields)
		{
			hasFields = getTraitMaps(visInfo, relInfo)
		}

		if (relInfo.sourceCube)
		{
			visInfo.edges << relInfo.createEdge(visInfo.edges.size())
		}

		if (!visited.add(targetCubeName + relInfo.scope.toString()))
		{
			return
		}

		visInfo.nodes << relInfo.createNode()
		visInfo.availableGroupsAllLevels << relInfo.group

		if (hasFields)
		{
			relInfo.targetTraitMaps.each { String targetFieldName, Map targetTraits ->
				if (CLASS_TRAITS != targetFieldName)
				{
					String targetFieldRpmType = targetTraits[R_RPM_TYPE]

					if (!helper.isPrimitive(targetFieldRpmType))
					{
						NCube nextTargetCube
						String nextTargetCubeName = ""
						if (targetTraits.containsKey(V_ENUM))
						{
							nextTargetCubeName = RPM_ENUM_DOT + targetTraits[V_ENUM]
							nextTargetCube = NCubeManager.getCube(appId, nextTargetCubeName)
						}
						else if (targetFieldRpmType)
						{
							nextTargetCubeName = RPM_CLASS_DOT + targetFieldRpmType
							nextTargetCube = NCubeManager.getCube(appId, nextTargetCubeName)
						}

						if (nextTargetCube)
						{
							addToStack(visInfo, relInfo, nextTargetCube, targetFieldRpmType, targetFieldName)
						}
						else
						{
							messages << "No cube exists with name of ${nextTargetCubeName}. Cube not included in the visualization.".toString()
						}
					}
				}
			}
		}
	}

	private void processEnumCube(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		relInfo.group = UNSPECIFIED
		String targetCubeName = relInfo.targetCube.name
		String sourceFieldRpmType = relInfo.sourceFieldRpmType

		if (!targetCubeName.startsWith(RPM_ENUM))
		{
			throw new IllegalStateException("Cube is not an rpm.enum cube: ${targetCubeName}.")
		}

		if (relInfo.sourceCube && (!sourceFieldRpmType || helper.isPrimitive(sourceFieldRpmType)))
		{
			return
		}

		boolean hasFields = getTraitMaps(visInfo, relInfo)

		if (hasFields)
		{
			relInfo.targetTraitMaps.each { String targetFieldName, Map targetTraits ->
				if (CLASS_TRAITS != targetFieldName)
				{
					try
					{
						String nextTargetCubeName = relInfo.getNextTargetCubeName(targetFieldName)

						if (nextTargetCubeName)
						{
							NCube nextTargetCube = NCubeManager.getCube(appId, nextTargetCubeName)
							if (nextTargetCube)
							{
								addToStack(visInfo, relInfo, nextTargetCube, relInfo.sourceFieldRpmType, targetFieldName)

								if (relInfo.group == UNSPECIFIED)
								{
									relInfo.groupName = nextTargetCubeName
								}
							}
							else
							{
								messages << "No cube exists with name of ${nextTargetCubeName}. Cube not included in the visualization.".toString()
							}
						}
					}
					catch (Exception e)
					{
						throw new IllegalStateException("Error processing the cube for enum field ${targetFieldName} in enum ${targetCubeName}.", e)
					}
				}
			}
		}

		visInfo.edges << relInfo.createEdge(visInfo.edges.size())

		if (!visited.add(targetCubeName + relInfo.scope.toString()))
		{
			return
		}

		visInfo.nodes << relInfo.createNode()
		visInfo.availableGroupsAllLevels << relInfo.group
	}

	private void addToStack(VisualizerInfo visInfo, VisualizerRelInfo relInfo, NCube nextTargetCube, String rpmType, String targetFieldName)
	{
		try
		{
			NCube nextSourceCube = relInfo.targetCube

			VisualizerRelInfo nextRelInfo = new VisualizerRelInfo()
			nextRelInfo.targetCube = nextTargetCube
			nextRelInfo.scope = getScopeRelativeToSource(nextTargetCube, rpmType, targetFieldName, relInfo.scope)
			nextRelInfo.sourceCube = nextSourceCube
			nextRelInfo.sourceScope = relInfo.targetScope
			nextRelInfo.sourceFieldName = targetFieldName
			nextRelInfo.sourceFieldRpmType = rpmType
			nextRelInfo.sourceTraitMaps = relInfo.targetTraitMaps
			nextRelInfo.sourceId = relInfo.targetId
			nextRelInfo.loadTraits = visInfo.loadTraits

			long nextTargetTargetLevel = relInfo.targetLevel + 1
			nextRelInfo.targetLevel = nextTargetTargetLevel

			long maxLevel = visInfo.maxLevel
			visInfo.maxLevel = maxLevel < nextTargetTargetLevel ? nextTargetTargetLevel : maxLevel
			visInfo.nodeCount += 1
			nextRelInfo.targetId = visInfo.nodeCount

			stack.push(nextRelInfo)
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Error processing the class for field ${relInfo.sourceFieldName} in class ${nextTargetCube.name}.", e)
		}
	}

	private boolean canLoadTargetAsRpmClass(VisualizerRelInfo relInfo)
	{
		//When the source cube points directly to the target cube (source cube and target cube are both rpm.class),
		//check if the source field name matches up with the scoped name of the target. If not, the target cube cannot be
		//loaded as an rpm.class.
		NCube sourceCube = relInfo.sourceCube
		NCube targetCube = relInfo.targetCube

		if (sourceCube && sourceCube.name.startsWith(RPM_CLASS_DOT) && targetCube.name.startsWith(RPM_CLASS_DOT) &&
				targetCube.getAxis(AXIS_TRAIT).findColumn(R_SCOPED_NAME))
		{
			String type = relInfo.sourceFieldRpmType
			NCube classTraitsCube = NCubeManager.getCube(appId, RPM_SCOPE_CLASS_DOT + type + DOT_CLASS_TRAITS)
			String sourceFieldName = relInfo.sourceFieldName
			if (!classTraitsCube.getAxis(type).findColumn(sourceFieldName))
			{
				relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): UNABLE_TO_LOAD + relInfo.sourceFieldName]] as Map
				String msg = getLoadTargetAsRpmClassMessage(relInfo, type)
				relInfo.notes << msg
				relInfo.hasFields = false
				return false
			}
		}
		return true
	}


	/**
	 * Sets the basic scope required to load a target class based on scoped source class, source field name, target class name, and current scope.
	 * Retains all other scope.
	 * If the source class is not a scoped class, returns the scope unchanged.
	 *
	 * @param targetCube String target cube
	 * @param targetTraitsMaps Map fields and traits of the target class
	 * @param sourceTraitsMap Map fields and traits of the source class
	 * @param sourceFieldName String source field name
	 * @param scope Map<String, Object> scope
	 *
	 * @return Map new scope
	 *
	 */
	private static Map<String, Object> getScopeRelativeToSource(NCube targetCube, String sourceFieldRpmType, String targetFieldName, Map scope)
	{
		Map<String, Object> newScope = new CaseInsensitiveMap<>(scope)

		if (targetCube.name.startsWith(RPM_ENUM))
		{
			newScope[SOURCE_FIELD_NAME] = targetFieldName
		}
		else if (targetCube.getAxis(AXIS_TRAIT).findColumn(R_SCOPED_NAME))
		{
			String newScopeKey = sourceFieldRpmType
			String oldValue = scope[newScopeKey]
			if (oldValue)
			{
				newScope[SOURCE_SCOPE_KEY_PREFIX + sourceFieldRpmType] = oldValue
			}
			newScope[newScopeKey] = targetFieldName
		}
		return newScope
	}

	private Map<String, Object> getDefaultScope(String type)
	{
		Map<String, Object> scope = new CaseInsensitiveMap<>()
		if (type)
		{
			scope[type] = DEFAULT_SCOPE_VALUE
			scope[POLICY_CONTROL_DATE] = defaultScopeDate
			scope[QUOTE_DATE] = defaultScopeDate
		}
		scope[EFFECTIVE_VERSION] = defaultScopeEffectiveVersion
		return scope
	}

	private boolean isValidStartCube(String cubeName)
	{
		if (!cubeName.startsWith(RPM_CLASS_DOT))
		{
			messages << "Starting cube for visualization must begin with 'rpm.class', n-cube ${cubeName} does not.".toString()
			return false
		}

		NCube cube = NCubeManager.getCube(appId, cubeName)
		if (!cube.getAxis(AXIS_FIELD) || !cube.getAxis(AXIS_TRAIT) )
		{
			messages << "Cube ${cubeName} is not a valid rpm class since it does not have both a field axis and a traits axis.".toString()
			return false
		}
		return true
	}

	private boolean hasMissingMinimumScope(VisualizerInfo visInfo)
	{
		boolean hasMissingScope = false
		String cubeName = visInfo.startCubeName
		Map<String, Object> scope = visInfo.scope
		String messageSuffixScopeKey = "Its default value may be changed as desired."

		if (NCubeManager.getCube(appId, cubeName).getAxis(AXIS_TRAIT).findColumn(R_SCOPED_NAME))
		{
			String type = getTypeFromCubeName(cubeName)
			String messageSuffixTypeScopeKey = "${DOUBLE_BREAK}Please replace ${DEFAULT_SCOPE_VALUE} for ${type} with an actual scope value."
			String messageScopeValues = getAvailableScopeValuesMessage(visInfo, cubeName, type)
			String messageSuffix = 'The other default scope values may also be changed as desired.'
			if (scope)
			{
				hasMissingScope = visInfo.addMissingMinimumScope(type, DEFAULT_SCOPE_VALUE, "${messageSuffixTypeScopeKey}${messageScopeValues}", messages) ?: hasMissingScope
				hasMissingScope = visInfo.addMissingMinimumScope(POLICY_CONTROL_DATE, defaultScopeDate, messageSuffixScopeKey, messages) ?: hasMissingScope
				hasMissingScope = visInfo.addMissingMinimumScope(QUOTE_DATE, defaultScopeDate, messageSuffixScopeKey, messages) ?: hasMissingScope
				hasMissingScope = visInfo.addMissingMinimumScope(EFFECTIVE_VERSION, defaultScopeEffectiveVersion, messageSuffixScopeKey, messages) ?: hasMissingScope
			}
			else
			{
				hasMissingScope = true
				Map<String, Object> defaultScope = getDefaultScope(type)
				visInfo.scope = defaultScope
				String msg = getMissingMinimumScopeMessage(defaultScope, messageScopeValues, messageSuffixTypeScopeKey, messageSuffix)
				messages << msg
			}
		}
		else{
			if (scope)
			{
				hasMissingScope = visInfo.addMissingMinimumScope(EFFECTIVE_VERSION, defaultScopeEffectiveVersion, messageSuffixScopeKey, messages) ?: hasMissingScope
			}
			else
			{
				hasMissingScope = false
				visInfo.scope = getDefaultScope(null)
			}
		}
		return hasMissingScope
	}

	private boolean getTraitMaps(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		try
		{
            relInfo.getTraitMaps(helper, appId, visInfo)
			relInfo.hasFields = true
		}
		catch (Exception e)
		{
			relInfo.hasFields = false
			Throwable t = getDeepestException(e)
			if (t instanceof InvalidCoordinateException)
			{
				handleInvalidCoordinateException(t as InvalidCoordinateException, visInfo, relInfo)
			}
			else if (t instanceof CoordinateNotFoundException)
			{
				handleCoordinateNotFoundException(t as CoordinateNotFoundException, visInfo, relInfo)
			}
			else
			{
				handleException(t, relInfo)
			}
		}
		return relInfo.hasFields
	}

	private void handleCoordinateNotFoundException(CoordinateNotFoundException e, VisualizerInfo visInfo, VisualizerRelInfo relInfo)
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
			relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): SCOPE_VALUE_NOT_FOUND + effectiveName]] as Map
		}
		else
		{
			handleException(e as Exception, relInfo)
		}
	}

	private void handleInvalidCoordinateException(InvalidCoordinateException e, VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		Set<String> missingScope = findMissingScope(relInfo.scope, e.requiredKeys)

		if (missingScope)
		{
			Map<String, Object> expandedScope = new CaseInsensitiveMap<>(visInfo.scope)
			missingScope.each { String key ->
				expandedScope[key] = DEFAULT_SCOPE_VALUE
			}
			visInfo.scope = expandedScope
			String effectiveName = relInfo.effectiveNameByCubeName
			relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): MISSING_SCOPE + effectiveName]] as Map
			String msg = getInvalidCoordinateExceptionMessage(visInfo, relInfo, missingScope, effectiveName, e.cubeName)
			relInfo.notes << msg
			messages << msg
		}
		else
		{
			throw new IllegalStateException("InvalidCoordinateException thrown, but no missing scope keys found for ${relInfo.targetCube.name} and scope ${visInfo.scope.toString()}.", e)
		}
	}

	private static Set<String> findMissingScope(Map<String, Object> scope, Set<String> requiredKeys)
	{
		return requiredKeys.findAll { String key ->
            !MANDATORY_RPM_SCOPE_KEYS.contains(key) && (scope == null || !scope.containsKey(key))
        }
	}

	private void handleException(Throwable e, VisualizerRelInfo relInfo)
	{
		Throwable t = getDeepestException(e)
		String effectiveName = relInfo.effectiveNameByCubeName
		String msg = getExceptionMessage(relInfo, effectiveName, e, t)
		relInfo.notes << msg
		messages << msg
		relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): UNABLE_TO_LOAD + effectiveName]] as Map
	}

	private static Throwable getDeepestException(Throwable e)
	{
		while (e.cause != null)
		{
			e = e.cause
		}
		return e
	}

	private static String getTypeFromCubeName(String cubeName)
	{
		return (cubeName - RPM_CLASS_DOT)
	}

	private static String getAvailableScopeValuesMessage(VisualizerInfo visInfo, String cubeName, String key)
	{
		Set<Object> scopeValues = visInfo.availableScopeValues[key] ?: visInfo.loadAvailableScopeValues(cubeName, key)
		if (scopeValues) {
			StringBuilder sb = new StringBuilder()
			sb.append("${BREAK}The following values are available for ${key}:${DOUBLE_BREAK}<pre><ul>")
			scopeValues.each{
				String value = it.toString()
				sb.append("<li><a class=\"missingScope\" title=\"${key}: ${value}\" href=\"#\">${value}</a></li>")
			}
			sb.append("</ul></pre>")
			return sb.toString()
		}
		return ''
	}

	private static String getMissingMinimumScopeMessage(Map<String, Object> scope, String messageScopeValues, String messageSuffixType, String messageSuffix )
	{
		"""\
The scope for the following scope keys was added since it was required: \
${DOUBLE_BREAK}${INDENT}${scope.keySet().join(COMMA_SPACE)}\
${messageSuffixType} ${messageSuffix} \
${BREAK}${messageScopeValues}"""
	}

	private static String getExceptionMessage(VisualizerRelInfo relInfo, String effectiveName, Throwable e, Throwable t)
	{
		String cubeDisplayName = relInfo.getCubeDisplayName(relInfo.targetCube.name)
		"""\
An exception was thrown while loading fields and traits for ${cubeDisplayName}${relInfo.sourceMessage} for ${effectiveName}. \
${DOUBLE_BREAK}<b>Message:</b> ${DOUBLE_BREAK}${e.message}${DOUBLE_BREAK}<b>Root cause: </b>\
${DOUBLE_BREAK}${t.toString()}${DOUBLE_BREAK}<b>Stack trace: </b>${DOUBLE_BREAK}${t.stackTrace.toString()}"""
	}

	private static String getCoordinateNotFoundMessage(VisualizerInfo visInfo, VisualizerRelInfo relInfo, String key, Object value, String effectiveName, String cubeName)
	{
		StringBuilder message = new StringBuilder()
		String messageScopeValues = getAvailableScopeValuesMessage(visInfo, cubeName, key)
		if (cubeName.startsWith(RPM_CLASS_DOT) || cubeName.startsWith(RPM_ENUM_DOT))
		{
			String cubeDisplayName = relInfo.getCubeDisplayName(cubeName)
			message.append("The scope value ${value} for scope key ${key} cannot be found on axis ${key} in ${cubeDisplayName}${relInfo.sourceMessage} for ${effectiveName}.")
		}
		else
		{
			message.append("The scope value ${value} for scope key ${key} cannot be found on axis ${key} in cube ${cubeName} for ${effectiveName}.")
		}
		message.append("${DOUBLE_BREAK} Please supply a different value for ${key}.${BREAK}${messageScopeValues}")
	}

	private static String getInvalidCoordinateExceptionMessage(VisualizerInfo visInfo, VisualizerRelInfo relInfo, Set<String> missingScope, String effectiveName, String cubeName)
	{
		StringBuilder message = new StringBuilder()

		if (cubeName.startsWith(RPM_CLASS_DOT) || cubeName.startsWith(RPM_ENUM_DOT))
		{
			String cubeDisplayName = relInfo.getCubeDisplayName(cubeName)
			message.append("Additional scope is required to load ${effectiveName} of type ${cubeDisplayName}${relInfo.sourceMessage}.")
		}
		else
		{
			message.append("Additional scope is required to load cube ${cubeName} for ${effectiveName}${relInfo.sourceMessage}.")
		}
		message.append("${DOUBLE_BREAK} Please add scope value(s) for the following scope key(s): ${missingScope.join(COMMA_SPACE)}.${BREAK}")
		missingScope.each{ String key ->
			message.append(getAvailableScopeValuesMessage(visInfo, cubeName, key))
		}
		return message.toString()
	}

	private static String getLoadTargetAsRpmClassMessage(VisualizerRelInfo relInfo, String type) {

		String sourceCubeDisplayName = relInfo.getCubeDisplayName(relInfo.sourceCube.name)
		String targetCubeDisplayName = relInfo.getCubeDisplayName(relInfo.targetCube.name)

		"""\
${sourceCubeDisplayName} points directly to ${targetCubeDisplayName} via field ${relInfo.sourceFieldName}, but \
there is no ${type.toLowerCase()} named ${relInfo.sourceFieldName} on ${type}.  ${DOUBLE_BREAK}Therefore \
it cannot be loaded in the visualization."""
	}
}