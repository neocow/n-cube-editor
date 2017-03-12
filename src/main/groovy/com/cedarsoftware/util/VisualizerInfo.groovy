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
    protected Map selectedNode
    protected Map<Long, Map<String, Object>> nodes
    protected Map<Long, Map<String, Object>> edges

    protected Map<String, Object> inputScope

    protected long maxLevel
    protected  long nodeCount
    protected long relInfoCount
    protected long defaultLevel
    protected String loadCellValuesLabel

    protected  Map<String,String> allGroups
    protected Set<String> allGroupsKeys
    protected String groupSuffix
    protected Set<String> availableGroupsAllLevels
    protected Set<String> messages

    protected Map<String, Object> networkOverridesBasic
    protected Map<String, Object> networkOverridesFull
    protected Map<String, Object> networkOverridesSelectedNode

    protected Map<String, Set<String>> requiredScopeKeysByCube = [:]

    protected Map<String, List<String>> typesToAddMap = [:]

    VisualizerInfo(){}

    protected VisualizerInfo(ApplicationID applicationID)
    {
        appId = applicationID
        loadConfigurations(cubeType)
    }

    protected void init(Map options)
    {
        messages = new LinkedHashSet()
        if (selectedNodeId)
        {
            selectedNode = new LinkedHashMap(nodes[selectedNodeId])
            inputScope = new CaseInsensitiveMap(selectedNode.availableScope as Map)
            if (1l == selectedNodeId)
            {
                nodes = [:]
                edges = [:]
            }
            else
            {
                nodes.remove(selectedNodeId)
                int removed = 1
                removeSourceEdge()
                removeTargets(edges)
                removed += removeTargets(nodes)
                nodeCount -= removed
                relInfoCount -= removed
                //TODO: What to do about max level?
            }
        }
        else
        {
            nodes = [:]
            edges = [:]
            selectedNodeId = 1
            maxLevel = 1
            nodeCount = 1
            relInfoCount = 1
            availableGroupsAllLevels = new LinkedHashSet()
            inputScope = options.scope as CaseInsensitiveMap ?: new CaseInsensitiveMap()
        }
    }

    private int removeTargets(Map<Long, Map> nodes)
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
        return toRemove.size()
    }

    private void removeSourceEdge()
    {
        List<Long> toRemove = []
        edges.each{Long id, Map edge ->
            Long edgeTo = edge.to as Long
            if (selectedNodeId == edgeTo)
            {
                toRemove << edgeTo
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
        networkOverridesSelectedNode = networkConfigCube.getCell([(CONFIG_ITEM): CONFIG_NETWORK_OVERRIDES_SELECTED_NODE, (CUBE_TYPE): cubeType]) as Map
        defaultLevel = configCube.getCell([(CONFIG_ITEM): CONFIG_DEFAULT_LEVEL, (CUBE_TYPE): cubeType]) as long
        allGroups = configCube.getCell([(CONFIG_ITEM): CONFIG_ALL_GROUPS, (CUBE_TYPE): cubeType]) as Map
        allGroupsKeys = new CaseInsensitiveSet(allGroups.keySet())
        String groupSuffix = configCube.getCell([(CONFIG_ITEM): CONFIG_GROUP_SUFFIX, (CUBE_TYPE): cubeType]) as String
        this.groupSuffix = groupSuffix ?: ''
        loadTypesToAddMap(configCube)
        loadCellValuesLabel = getLoadCellValuesLabel()
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

    protected String getLoadCellValuesLabel()
    {
        'cell values'
    }

    protected void convertToSingleMessage()
    {
        if (messages)
        {
            messages = [messages.join(DOUBLE_BREAK)] as Set
        }
    }
}