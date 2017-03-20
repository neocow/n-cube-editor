package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import groovy.transform.CompileStatic
import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Provides information to visualize n-cubes.
 */

@CompileStatic
class VisualizerInfo
{
    protected ApplicationID appId
    protected Long selectedNodeId
    protected Map<Long, Map<String, Object>> nodes
    protected Map<Long, Map<String, Object>> edges

    protected Map<String, Object> inputScope

    protected long maxLevel
    protected long nodeIdCounter
    protected long edgeIdCounter
    protected long defaultLevel
    protected String cellValuesLabel
    protected String nodeLabel

    protected  Map<String,String> allGroups
    protected Set<String> allGroupsKeys
    protected String groupSuffix
    protected Set<String> availableGroupsAllLevels
    protected Set<String> messages

    protected Map<String, Object> networkOverridesBasic
    protected Map<String, Object> networkOverridesFull

    protected Map<String, Set<String>> requiredScopeKeysByCube = [:]

    protected Map<String, List<String>> typesToAddMap = [:]

    VisualizerInfo(){}

    protected VisualizerInfo(ApplicationID applicationID)
    {
        appId = applicationID
        loadConfigurations(cubeType)
    }

    protected void init(Map options = null)
    {
        inputScope = options?.scope as CaseInsensitiveMap ?: new CaseInsensitiveMap()
        messages = new LinkedHashSet()
        nodes = [:]
        edges = [:]
        nodeIdCounter = 1
        edgeIdCounter = 0
        selectedNodeId = 1
        maxLevel = 1
        availableGroupsAllLevels = new LinkedHashSet()
    }

    protected void initScopeChange()
    {
        if (1l == selectedNodeId)
        {
            init()
        }
        else
        {
            messages = new LinkedHashSet()
            nodes.remove(selectedNodeId)
            removeSourceEdges()
            removeTargets(edges)
            removeTargets(nodes)
            maxLevel = 1
            availableGroupsAllLevels = new LinkedHashSet()
        }
    }

    private void removeTargets(Map<Long, Map> nodes)
    {
        List<Long> toRemove = []
        nodes.each{Long id, Map node ->
            List<Long> sourceTrail = node.sourceTrail as List
            if (sourceTrail.contains(selectedNodeId))
            {
                toRemove << (node.id as Long)
            }
        }
        nodes.keySet().removeAll(toRemove)
    }

    private void removeSourceEdges()
    {
        List<Long> toRemove = []
        edges.each{Long id, Map edge ->
            if (selectedNodeId == edge.to as Long)
            {
                toRemove << (edge.id as Long)
            }
        }
        edges.keySet().removeAll(toRemove)
    }

    protected String getCubeType()
    {
        return CUBE_TYPE_DEFAULT
    }

    protected NCube loadConfigurations(String cubeType)
    {
        String configJson = NCubeManager.getResourceAsString(JSON_FILE_PREFIX + VISUALIZER_CONFIG_CUBE_NAME + JSON_FILE_SUFFIX)
        NCube configCube = NCube.fromSimpleJson(configJson)
        configCube.applicationID = appId
        String networkConfigJson = NCubeManager.getResourceAsString(JSON_FILE_PREFIX + VISUALIZER_CONFIG_NETWORK_OVERRIDES_CUBE_NAME + JSON_FILE_SUFFIX)
        NCube networkConfigCube = NCube.fromSimpleJson(networkConfigJson)
        networkConfigCube.applicationID = appId

        networkOverridesBasic = networkConfigCube.getCell([(CONFIG_ITEM): CONFIG_NETWORK_OVERRIDES_BASIC, (CUBE_TYPE): cubeType]) as Map
        networkOverridesFull = networkConfigCube.getCell([(CONFIG_ITEM): CONFIG_NETWORK_OVERRIDES_FULL, (CUBE_TYPE): cubeType]) as Map
        defaultLevel = configCube.getCell([(CONFIG_ITEM): CONFIG_DEFAULT_LEVEL, (CUBE_TYPE): cubeType]) as long
        allGroups = configCube.getCell([(CONFIG_ITEM): CONFIG_ALL_GROUPS, (CUBE_TYPE): cubeType]) as Map
        allGroupsKeys = new CaseInsensitiveSet(allGroups.keySet())
        String groupSuffix = configCube.getCell([(CONFIG_ITEM): CONFIG_GROUP_SUFFIX, (CUBE_TYPE): cubeType]) as String
        this.groupSuffix = groupSuffix ?: ''
        loadTypesToAddMap(configCube)
        cellValuesLabel = getCellValuesLabel()
        nodeLabel = getNodeLabel()
        return configCube
    }

    protected List getTypesToAdd(String group)
    {
        return typesToAddMap[allGroups[group]]
    }

    protected void loadTypesToAddMap(NCube configCube)
    {
        typesToAddMap = [:]
        Set<String> allTypes = configCube.getCell([(CONFIG_ITEM): CONFIG_ALL_TYPES, (CUBE_TYPE): cubeType]) as Set
        allTypes.each{String type ->
            Map.Entry<String, String> entry = allGroups.find{ String key, String value ->
                value == type
            }
            typesToAddMap[entry.value] = allTypes as List
        }
    }

    protected String getLoadTarget(boolean showingHidingCellValues)
    {
        return showingHidingCellValues ? "${cellValuesLabel}" : "the ${nodeLabel}"
    }

    protected String getNodeLabel()
    {
        'n-cube'
    }

    protected String getNodesLabel()
    {
        return 'cubes'
    }

    protected String getCellValuesLabel()
    {
        return 'cell values'
    }

}