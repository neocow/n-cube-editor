package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.exception.InvalidCoordinateException
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.RpmVisualizerConstants.*

/**
 * Provides the information used to visualize rpm class cubes.
 */

// TODO: This code needs to be moved out of NCE and pulled-in via Grapes.
@CompileStatic
class RpmVisualizer extends Visualizer
{
	protected RpmVisualizerHelper helper = new RpmVisualizerHelper()
	protected String defaultScopeEffectiveVersion
	protected String defaultScopeDate

	/**
	 * Loads all cell values available for a given cube.
	 *
	 * @param applicationID
	 * @param options (map containing:
	 *            Map node, representing a class,
	 *            VisualizerInfo, information about the visualization
	 * @return node
	 */
	@Override
	Map getCellValues(ApplicationID applicationID, Map options)
	{
		appId = applicationID
		RpmVisualizerRelInfo relInfo = new RpmVisualizerRelInfo(appId, options.node as Map)
		return getCellValues(relInfo, options)
	}

	@Override
	protected RpmVisualizerInfo getVisualizerInfo(Map options)
	{
	    RpmVisualizerInfo visInfo
		Object optionsVisInfo = options.visInfo
		if (optionsVisInfo && optionsVisInfo instanceof RpmVisualizerInfo)
		{
			visInfo = optionsVisInfo as RpmVisualizerInfo
		}
		else
		{
			visInfo = new RpmVisualizerInfo(appId, options)
			String json = NCubeManager.getResourceAsString(JSON_FILE_PREFIX + VISUALIZER_CONFIG_CUBE_NAME + JSON_FILE_SUFFIX)
			NCube configCube = NCube.fromSimpleJson(json)
			visInfo.loadTypesToAddMap(configCube)
		}
		visInfo.scope = options.scope as CaseInsensitiveMap ?: new CaseInsensitiveMap<>()
		return visInfo
	}

	@Override
	protected void loadFirstVisualizerRelInfo(VisualizerInfo visInfo, VisualizerRelInfo relInfo, String startCubeName)
	{
		super.loadFirstVisualizerRelInfo(visInfo, relInfo, startCubeName)
		(relInfo as RpmVisualizerRelInfo).typesToAdd = (visInfo as RpmVisualizerInfo).getTypesToAdd(relInfo.targetCube.name)
	}

	@Override
	protected void processCube(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		if (relInfo.targetCube.name.startsWith(RPM_CLASS))
		{
			processClassCube((RpmVisualizerInfo) visInfo, (RpmVisualizerRelInfo) relInfo)
		}
		else
		{
			processEnumCube((RpmVisualizerInfo) visInfo, (RpmVisualizerRelInfo) relInfo)
		}
	}

	private void processClassCube(RpmVisualizerInfo visInfo, RpmVisualizerRelInfo relInfo)
	{
		String targetCubeName = relInfo.targetCube.name

		boolean cellValuesLoaded = canLoadTargetAsRpmClass(relInfo)
		if (cellValuesLoaded)
		{
			cellValuesLoaded = loadCellValues(visInfo, relInfo)
		}

		if (relInfo.sourceCube)
		{
			visInfo.edges << relInfo.createEdge(visInfo.edges.size())
		}

		if (!visited.add(targetCubeName + relInfo.scope.toString()))
		{
			return
		}

		visInfo.nodes << relInfo.createNode(visInfo)

		if (cellValuesLoaded)
		{
			relInfo.targetCellValues.each { String targetFieldName, Map targetTraits ->
				if (CLASS_TRAITS != targetFieldName)
				{
					String targetFieldRpmType = targetTraits[R_RPM_TYPE]

					if (!helper.isPrimitive(targetFieldRpmType))
					{
						String nextTargetCubeName = ""
						if (targetTraits.containsKey(V_ENUM))
						{
							nextTargetCubeName = RPM_ENUM_DOT + targetTraits[V_ENUM]
						}
						else if (targetFieldRpmType)
						{
							nextTargetCubeName = RPM_CLASS_DOT + targetFieldRpmType
						}
						addToStack(visInfo, relInfo, nextTargetCubeName, targetFieldRpmType, targetFieldName)
					}
				}
			}
		}
	}

	private void processEnumCube(RpmVisualizerInfo visInfo, RpmVisualizerRelInfo relInfo)
	{
		String group = UNSPECIFIED_ENUM
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

		boolean cellValuesLoaded = loadCellValues(visInfo, relInfo)

		if (cellValuesLoaded)
		{
			relInfo.targetCellValues.each { String targetFieldName, Map targetTraits ->
				if (CLASS_TRAITS != targetFieldName)
				{
					try
					{
						String nextTargetCubeName = relInfo.getNextTargetCubeName(targetFieldName)

						if (nextTargetCubeName)
						{
							RpmVisualizerRelInfo nextRelInfo = addToStack(visInfo, relInfo, nextTargetCubeName, relInfo.sourceFieldRpmType, targetFieldName)
							if (nextRelInfo && group == UNSPECIFIED_ENUM)
							{
								group = relInfo.getGroupName(visInfo, nextTargetCubeName) + visInfo.groupSuffix
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

		visInfo.nodes << relInfo.createNode(visInfo, group)

	}

	private RpmVisualizerRelInfo addToStack(VisualizerInfo visInfo, VisualizerRelInfo relInfo, String nextTargetCubeName, String rpmType, String targetFieldName)
	{
		RpmVisualizerRelInfo nextRelInfo = new RpmVisualizerRelInfo()
		super.addToStack(visInfo, relInfo, nextRelInfo, nextTargetCubeName) as RpmVisualizerRelInfo
		NCube nextTargetCube = nextRelInfo.targetCube
		try
		{
			nextRelInfo.scope = getScopeRelativeToSource(nextTargetCube, rpmType, targetFieldName, relInfo.scope)
			nextRelInfo.sourceFieldName = targetFieldName
			nextRelInfo.sourceFieldRpmType = rpmType
			nextRelInfo.sourceCellValues = (relInfo as RpmVisualizerRelInfo).targetCellValues
			nextRelInfo.typesToAdd = (visInfo as RpmVisualizerInfo).getTypesToAdd(nextTargetCube.name)
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Error processing the class for field ${relInfo.sourceFieldName} in class ${nextTargetCube.name}.", e)
		}
		return nextRelInfo
	}

	@Override
	protected RpmVisualizerRelInfo getVisualizerRelInfo()
	{
		return new RpmVisualizerRelInfo()
	}

	private boolean canLoadTargetAsRpmClass(RpmVisualizerRelInfo relInfo)
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
				relInfo.targetCellValues = [(CLASS_TRAITS): [(R_SCOPED_NAME): UNABLE_TO_LOAD + relInfo.sourceFieldName]] as Map
				String msg = getLoadTargetAsRpmClassMessage(relInfo, type)
				relInfo.notes << msg
				relInfo.cellValuesLoaded = false
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

	@Override
	protected boolean isValidStartCube(String cubeName)
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

	@Override
	protected boolean hasMissingMinimumScope(VisualizerInfo visInfo, String startCubeName)
	{
		RpmVisualizerInfo rpmVisInfo = (RpmVisualizerInfo) visInfo
		defaultScopeEffectiveVersion = appId.version.replace('.', '-')
		defaultScopeDate = DATE_TIME_FORMAT.format(new Date())

		boolean hasMissingScope = false
		Map<String, Object> scope = rpmVisInfo.scope
		String messageSuffixScopeKey = "Its default value may be changed as desired."

		if (NCubeManager.getCube(appId, startCubeName).getAxis(AXIS_TRAIT).findColumn(R_SCOPED_NAME))
		{
			String type = getTypeFromCubeName(startCubeName)
			String messageSuffixTypeScopeKey = "${DOUBLE_BREAK}Please replace ${DEFAULT_SCOPE_VALUE} for ${type} with an actual scope value."
			String messageScopeValues = getAvailableScopeValuesMessage(rpmVisInfo, startCubeName, type)
			String messageSuffix = 'The other default scope values may also be changed as desired.'
			if (scope)
			{
				hasMissingScope = rpmVisInfo.addMissingMinimumScope(type, DEFAULT_SCOPE_VALUE, "${messageSuffixTypeScopeKey}${messageScopeValues}", messages) ?: hasMissingScope
				hasMissingScope = rpmVisInfo.addMissingMinimumScope(POLICY_CONTROL_DATE, defaultScopeDate, messageSuffixScopeKey, messages) ?: hasMissingScope
				hasMissingScope = rpmVisInfo.addMissingMinimumScope(QUOTE_DATE, defaultScopeDate, messageSuffixScopeKey, messages) ?: hasMissingScope
				hasMissingScope = rpmVisInfo.addMissingMinimumScope(EFFECTIVE_VERSION, defaultScopeEffectiveVersion, messageSuffixScopeKey, messages) ?: hasMissingScope
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

	protected Set getMandatoryScopeKeys()
	{
		return MANDATORY_SCOPE_KEYS
	}

	@Override
	protected String handleCoordinateNotFoundException(CoordinateNotFoundException e, VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		String reason = super.handleCoordinateNotFoundException(e, visInfo, relInfo)
		(relInfo as RpmVisualizerRelInfo).targetCellValues = [(CLASS_TRAITS): [(R_SCOPED_NAME): reason + relInfo.effectiveNameByCubeName]] as Map
		return reason
	}

	@Override
	protected String handleInvalidCoordinateException(InvalidCoordinateException e, VisualizerInfo visInfo, VisualizerRelInfo relInfo, Set mandatoryScopeKeys)
	{
		String reason = super.handleInvalidCoordinateException(e, visInfo, relInfo, mandatoryScopeKeys)
		(relInfo as RpmVisualizerRelInfo).targetCellValues = [(CLASS_TRAITS): [(R_SCOPED_NAME): reason + relInfo.effectiveNameByCubeName]] as Map
		return reason
	}

	@Override
	protected String handleException(Throwable e, VisualizerRelInfo relInfo)
	{
		String reason = super.handleException(e, relInfo)
		(relInfo as RpmVisualizerRelInfo).targetCellValues = [(CLASS_TRAITS): [(R_SCOPED_NAME): UNABLE_TO_LOAD + reason]] as Map
		return reason
	}

	private static String getTypeFromCubeName(String cubeName)
	{
		return (cubeName - RPM_CLASS_DOT)
	}

	@Override
	protected String getCoordinateNotFoundMessage(VisualizerInfo visInfo, VisualizerRelInfo relInfo, String key, Object value, String effectiveName, String cubeName)
	{
		RpmVisualizerInfo rpmVisInfo = visInfo as RpmVisualizerInfo
		RpmVisualizerRelInfo rpmRelInfo = relInfo as RpmVisualizerRelInfo
		StringBuilder message = new StringBuilder()
		String messageScopeValues = getAvailableScopeValuesMessage(rpmVisInfo, cubeName, key)
		if (cubeName.startsWith(RPM_CLASS_DOT) || cubeName.startsWith(RPM_ENUM_DOT))
		{
			String cubeDisplayName = rpmRelInfo.getCubeDisplayName(cubeName)
			message.append("The scope value ${value} for scope key ${key} cannot be found on axis ${key} in ${cubeDisplayName}${rpmRelInfo.sourceMessage} for ${effectiveName}.")
		}
		else
		{
			message.append("The scope value ${value} for scope key ${key} cannot be found on axis ${key} in cube ${cubeName} for ${effectiveName}.")
		}
		message.append("${DOUBLE_BREAK} Please supply a different value for ${key}.${BREAK}${messageScopeValues}")
	}

	@Override
	protected String getInvalidCoordinateExceptionMessage(VisualizerInfo visInfo, VisualizerRelInfo relInfo, Set<String> missingScope, String effectiveName, String cubeName)
	{
		RpmVisualizerInfo rpmVisInfo = visInfo as RpmVisualizerInfo
		RpmVisualizerRelInfo rpmRelInfo = relInfo as RpmVisualizerRelInfo
		StringBuilder message = new StringBuilder()

		if (cubeName.startsWith(RPM_CLASS_DOT) || cubeName.startsWith(RPM_ENUM_DOT))
		{
			String cubeDisplayName = rpmRelInfo.getCubeDisplayName(cubeName)
			message.append("Additional scope is required to load ${effectiveName} of type ${cubeDisplayName}${rpmRelInfo.sourceMessage}.")
		}
		else
		{
			message.append("Additional scope is required to load cube ${cubeName} for ${effectiveName}${rpmRelInfo.sourceMessage}.")
		}
		message.append("${DOUBLE_BREAK} Please add scope value(s) for the following scope key(s): ${missingScope.join(COMMA_SPACE)}.${BREAK}")
		missingScope.each{ String key ->
			message.append(getAvailableScopeValuesMessage(rpmVisInfo, cubeName, key))
		}
		return message.toString()
	}

	private static String getLoadTargetAsRpmClassMessage(RpmVisualizerRelInfo relInfo, String type) {

		String sourceCubeDisplayName = relInfo.getCubeDisplayName(relInfo.sourceCube.name)
		String targetCubeDisplayName = relInfo.getCubeDisplayName(relInfo.targetCube.name)

		"""\
${sourceCubeDisplayName} points directly to ${targetCubeDisplayName} via field ${relInfo.sourceFieldName}, but \
there is no ${type.toLowerCase()} named ${relInfo.sourceFieldName} on ${type}.  ${DOUBLE_BREAK}Therefore \
it cannot be loaded in the visualization."""
	}
}