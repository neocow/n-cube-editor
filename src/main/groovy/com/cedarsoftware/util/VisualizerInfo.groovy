package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.Axis
import com.cedarsoftware.ncube.Column
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
    ApplicationID appId
    Map<String, Object> scope
    List<Map<String, Object>> nodes = []
    List<Map<String, Object>> edges = []

    long maxLevel
    long nodeCount
    long relInfoCount
    long defaultLevel
    String loadCellValuesLabel

    Map<String,String> allGroups
    Set<String> allGroupsKeys
    String groupSuffix
    Set<String> availableGroupsAllLevels
    Set<String> messages

    Map<String, Set<Map<String, Set<Object>>>> optionalScopeValues = [:]
    Map<String, Set<Map<String, Set<Object>>>> requiredScopeValues = [:]
    Map<String, Set<String>> requiredScopeKeys = [:]
    Map<String, Set<String>> optionalScopeKeys = [:]

    Map<String, Object> networkOverridesBasic
    Map<String, Object> networkOverridesFull
    Map<String, Object> networkOverridesTopNode

    Map<String, List<String>> typesToAddMap = [:]

    VisualizerInfo(){}

    VisualizerInfo(ApplicationID applicationID, Map options)
    {
        appId = applicationID
        scope = options.scope as CaseInsensitiveMap
        loadConfigurations(cubeType)
    }

    protected void init(Map scope)
    {
        maxLevel = 1
        nodeCount = 1
        relInfoCount = 1
        messages = [] as Set
        availableGroupsAllLevels = [] as Set
        this.scope = scope as CaseInsensitiveMap ?: new CaseInsensitiveMap<>()
    }

    protected String getCubeType()
    {
        return CUBE_TYPE_DEFAULT
    }

    boolean addMissingMinimumScope(String key, String value, String messageSuffix, Set<String> messages)
    {
        Map<String, Object> scope = scope
        boolean missingScope
        if (scope.containsKey(key))
        {
            if (!scope[key])
            {
                scope[key] = value
                missingScope = true
            }
            else if (DEFAULT_SCOPE_VALUE == scope[key])
            {
                missingScope = true
            }
        }
        else
        {
            scope[key] = value
            missingScope = true
        }

        if (missingScope)
        {
            messages << "Scope is required for ${key}. ${messageSuffix}".toString()
        }
        return missingScope
    }

    NCube loadConfigurations(String cubeType)
    {
        String configJson = NCubeManager.getResourceAsString(JSON_FILE_PREFIX + VISUALIZER_CONFIG_CUBE_NAME + JSON_FILE_SUFFIX)
        NCube configCube = NCube.fromSimpleJson(configJson)
        configCube.applicationID = appId
        String networkConfigJson = NCubeManager.getResourceAsString(JSON_FILE_PREFIX + VISUALIZER_CONFIG_NETWORK_OVERRIDES_CUBE_NAME + JSON_FILE_SUFFIX)
        NCube networkConfigCube = NCube.fromSimpleJson(networkConfigJson)
        networkConfigCube.applicationID = appId

        networkOverridesBasic = networkConfigCube.getCell([(CONFIG_ITEM): CONFIG_NETWORK_OVERRIDES_BASIC, (CUBE_TYPE): cubeType]) as Map
        networkOverridesFull = networkConfigCube.getCell([(CONFIG_ITEM): CONFIG_NETWORK_OVERRIDES_FULL, (CUBE_TYPE): cubeType]) as Map
        networkOverridesTopNode = networkConfigCube.getCell([(CONFIG_ITEM): CONFIG_NETWORK_OVERRIDES_TOP_NODE, (CUBE_TYPE): cubeType]) as Map
        defaultLevel = configCube.getCell([(CONFIG_ITEM): CONFIG_DEFAULT_LEVEL, (CUBE_TYPE): cubeType]) as long
        allGroups = configCube.getCell([(CONFIG_ITEM): CONFIG_ALL_GROUPS, (CUBE_TYPE): cubeType]) as Map
        allGroupsKeys = new CaseInsensitiveSet(allGroups.keySet())
        String groupSuffix = configCube.getCell([(CONFIG_ITEM): CONFIG_GROUP_SUFFIX, (CUBE_TYPE): cubeType]) as String
        this.groupSuffix = groupSuffix ?: ''
        loadTypesToAddMap(configCube)
        loadCellValuesLabel = getLoadCellValuesLabel()
        return configCube
    }

    List getTypesToAdd(String group)
    {
        return typesToAddMap[allGroups[group]]
    }

    void loadTypesToAddMap(NCube configCube)
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
        LOAD_CELL_VALUES_LABEL
    }

    Set<Object> getOptionalScopeValues( String cubeName, String key)
    {
        return getScopeValues(optionalScopeValues, cubeName, key)
    }

    Set<Object> getRequiredScopeValues(String cubeName, String key)
    {
        return getScopeValues(requiredScopeValues, cubeName, key)
    }

    Set<Object> getScopeValues( Map<String, Set<Map<String, Set<Object>>>> scopeValues, String cubeName, String key)
    {
        //The key to the map scopeValues is a scope key. The scopeValues map contains a set of cube maps.
        //Each cube map has a single entry where the key is the cube name and the value is a set of scope values available on
        //the cube for the scope key in question.
        Set<Map<String, Set<Object>>> valuesByCubeNames = scopeValues[key]
        Map<String, Set<Object>> cubeMap = [:]
        if (valuesByCubeNames)
        {
            cubeMap = valuesByCubeNames.find { Map<String, Set<Object>> cubeValues ->
                cubeValues[cubeName]
            }
        }
        else
        {
            valuesByCubeNames = []
        }

        if (cubeMap)
        {
            return cubeMap[cubeName] as Set<Object>
        }
        else
        {
            Set<Object> values =  getColumnValues(appId, cubeName, key)
            cubeMap = [(cubeName): values]
            valuesByCubeNames << cubeMap
            scopeValues[key] = valuesByCubeNames
            return values
        }
    }

    protected static Set<Object> getColumnValues(ApplicationID applicationID, String cubeName, String axisName)
    {
        NCube cube = NCubeManager.getCube(applicationID, cubeName)
        Set values = new LinkedHashSet()
        Axis axis = cube?.getAxis(axisName)
        if (axis)
        {
            for (Column column : axis.columnsWithoutDefault)
            {
                values.add(column.value)
            }
        }
        return values
    }

    void convertToSingleMessage()
    {
        messages = messages ? [messages.join(DOUBLE_BREAK)] as Set : null
    }
}