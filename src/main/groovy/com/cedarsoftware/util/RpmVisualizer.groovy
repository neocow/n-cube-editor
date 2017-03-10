package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.RpmVisualizerConstants.*

/**
 * Processes an rpm class and all its referenced rpm classes and provides information to
 * visualize the classes and their relationships.
 */

@CompileStatic
class RpmVisualizer extends Visualizer
{
	private RpmVisualizerHelper helper

	/**
	 * Loads details for a given rpm class.
	 *
	 * @param applicationID
	 * @param options - a map containing:
	 *            Map node, representing a class and its scope
	 *            RpmVisualizerInfo visInfo, information about the visualization
	 *            Map scope, the scope used in the visualization
	 * @return a map containing:
	 *           String status, status of the visualization
	 *           RpmVisualizerInfo visInfo, information about the visualization
	 */

	@Override
	Map loadNodeDetails(ApplicationID applicationID, Map options)
	{
		appId = applicationID
		Map node = options.node as Map
		RpmVisualizerInfo visInfo = options.visInfo as RpmVisualizerInfo
		visInfo.appId = applicationID
		visInfo.inputScope = node.availableScope as CaseInsensitiveMap
		VisualizerRelInfo relInfo = getVisualizerRelInfo(options, visInfo)
		return loadNodeDetails(visInfo, relInfo, node)
	}

	@Override
	protected VisualizerInfo getVisualizerInfo(Map options)
	{
		RpmVisualizerInfo visInfo
		Object optionsVisInfo = options.visInfo
		if (optionsVisInfo && optionsVisInfo instanceof RpmVisualizerInfo)
		{
			visInfo = optionsVisInfo as RpmVisualizerInfo
			visInfo.appId = appId
		}
		else
		{
			visInfo = new RpmVisualizerInfo(appId)
		}
		visInfo.init(options)
		return visInfo
	}

	@Override
	protected VisualizerRelInfo getVisualizerRelInfo(Map options, VisualizerInfo visInfo)
	{
		RpmVisualizerRelInfo relInfo = new RpmVisualizerRelInfo(appId)
		relInfo.init(options, visInfo)
		return relInfo
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
		boolean cubeLoaded
		String targetCubeName = relInfo.targetCube.name

		if (canLoadTraitsForTarget(relInfo))
		{
			cubeLoaded = relInfo.loadCube(visInfo)
		}

		if (relInfo.sourceCube)
		{
			visInfo.edges << relInfo.createEdge(visInfo.edges.size())
		}

		if (!visited.add(targetCubeName + relInfo.availableTargetScope.toString()))
		{
			return
		}

		visInfo.nodes << relInfo.createNode(visInfo)

		if (cubeLoaded)
		{
			relInfo.targetTraits.each { String targetFieldName, Map targetTraits ->
				if (CLASS_TRAITS != targetFieldName)
				{
					String targetFieldRpmType = targetTraits[R_RPM_TYPE]
					if (!visualizerHelper.isPrimitive(targetFieldRpmType))
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

		boolean cubeLoaded = relInfo.loadCube(visInfo)
		if (cubeLoaded)
		{
			relInfo.targetTraits.each { String targetFieldName, Map targetTraits ->
				if (CLASS_TRAITS != targetFieldName)
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
			}
		}

		visInfo.edges << relInfo.createEdge(visInfo.edges.size())

		if (!visited.add(targetCubeName + relInfo.availableTargetScope.toString()))
		{
			return
		}

		visInfo.nodes << relInfo.createNode(visInfo, group)
	}

	private RpmVisualizerRelInfo addToStack(RpmVisualizerInfo visInfo, RpmVisualizerRelInfo relInfo, String nextTargetCubeName, String rpmType, String targetFieldName)
	{
		RpmVisualizerRelInfo nextRelInfo = new RpmVisualizerRelInfo(appId)
		super.addToStack(visInfo, relInfo, nextRelInfo, nextTargetCubeName)
		NCube nextTargetCube = nextRelInfo.targetCube
		if (nextTargetCube)
		{
			nextRelInfo.populateScopeRelativeToSource(rpmType, targetFieldName, relInfo.availableTargetScope)
			nextRelInfo.sourceFieldName = targetFieldName
			nextRelInfo.sourceFieldRpmType = rpmType
			nextRelInfo.sourceTraits = relInfo.targetTraits
			nextRelInfo.showCellValuesLink = false
		}
		return nextRelInfo
	}

	private boolean canLoadTraitsForTarget(RpmVisualizerRelInfo relInfo)
	{
		//When the source cube points directly to the target cube (source cube and target cube are both rpm.class),
		//check if the source field name matches up with the scoped name of the target. If not, traits cannot
		//be loaded for the target in the visualization.
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
				relInfo.nodeLabelPrefix = 'Unable to load '
				relInfo.targetTraits = new CaseInsensitiveMap()
				relInfo.nodeDetailsMessages << getLoadTraitsForTargetMessage(relInfo, type)
				relInfo.cubeLoaded = false
				relInfo.showCellValuesLink = false
				return false
			}
		}
		return true
	}

	@Override
	protected boolean isValidStartCube(VisualizerInfo visInfo, String cubeName)
	{
		if (!super.isValidStartCube(visInfo, cubeName))
		{
			return false
		}

		if (!cubeName.startsWith(RPM_CLASS_DOT))
		{
			visInfo.messages << "Starting cube for visualization must begin with 'rpm.class', n-cube ${cubeName} does not.".toString()
			return false
		}

		NCube cube = NCubeManager.getCube(appId, cubeName)
		if (!cube.getAxis(AXIS_FIELD) || !cube.getAxis(AXIS_TRAIT) )
		{
			visInfo.messages << "Cube ${cubeName} is not a valid rpm class since it does not have both a field axis and a traits axis.".toString()
			return false
		}
		return true
	}

	@Override
	protected RpmVisualizerHelper getVisualizerHelper()
	{
		helper =  new RpmVisualizerHelper()
	}

	private static String getLoadTraitsForTargetMessage(RpmVisualizerRelInfo relInfo, String type) {

		String sourceCubeDisplayName = relInfo.getCubeDisplayName(relInfo.sourceCube.name)
		String targetCubeDisplayName = relInfo.getCubeDisplayName(relInfo.targetCube.name)

		"""\
<b>Unable to load the class. ${sourceCubeDisplayName} points directly to ${targetCubeDisplayName} via field ${relInfo.sourceFieldName}, but \
there is no ${type.toLowerCase()} named ${relInfo.sourceFieldName} on ${type}.</b> ${DOUBLE_BREAK}"""
	}
}