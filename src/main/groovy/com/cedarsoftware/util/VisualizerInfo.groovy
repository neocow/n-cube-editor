package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.Axis
import com.cedarsoftware.ncube.Column
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.ReleaseStatus
import com.cedarsoftware.ncube.util.VersionComparator
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Holds information related to the visualization of cubes and their relationships.
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
    long defaultLevel

	Map<String,String> allGroups
    Set<String> allGroupsKeys
    String groupSuffix
	Set<String> availableGroupsAllLevels

	//Set<String> availableScopeKeys = [] TODO: Not needed currently, but will revisit
	Map<String, Set<Object>> availableScopeValues = [:]
    Map<String, Set<String>> requiredScopeKeys = [:]
    Map<String, Set<String>> optionalScopeKeys = [:]

    Map<String, Object> networkOverridesBasic = null
    Map<String, Object> networkOverridesFull = null

    Map<String, List<String>> typesToAddMap = [:]

    VisualizerInfo(){}

    VisualizerInfo(ApplicationID applicationID, Map options)
    {
        appId = applicationID
        scope = options.scope as CaseInsensitiveMap
        loadConfigurations()
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

    List getTypesToAdd(String cubeName)
    {
        if (cubeName.startsWith(RPM_CLASS_DOT))
        {
            String sourceType = cubeName - RPM_CLASS_DOT
            return typesToAddMap[sourceType]
        }
        return null
    }

   void loadTypesToAddMap(NCube configCube)
    {
        typesToAddMap = [:]
        String json = NCubeManager.getResourceAsString(JSON_FILE_PREFIX + TYPES_TO_ADD_CUBE_NAME + JSON_FILE_SUFFIX)
        NCube typesToAddCube = NCube.fromSimpleJson(json)
        Set<String> allTypes = configCube.getCell([(CONFIG_ITEM): CONFIG_ALL_TYPES, (CUBE_TYPE): CUBE_TYPE_RPM]) as Set

        allTypes.each { String sourceType ->
            Map<String, Boolean> map = typesToAddCube.getMap([(SOURCE_TYPE): sourceType, (TARGET_TYPE): [] as Set]) as Map
            List<String> typesToAdd = map.findAll { String type, Boolean available ->
                available == true
            }.keySet() as List
            typesToAddMap[sourceType] = typesToAdd
        }
    }

    void loadConfigurations()
    {
        String json = NCubeManager.getResourceAsString(JSON_FILE_PREFIX + VISUALIZER_CONFIG_CUBE_NAME + JSON_FILE_SUFFIX)
        NCube configCube = NCube.fromSimpleJson(json)

        networkOverridesBasic = configCube.getCell([(CONFIG_ITEM): CONFIG_NETWORK_OVERRIDES_BASIC, (CUBE_TYPE): CUBE_TYPE_RPM]) as Map
        networkOverridesFull = configCube.getCell([(CONFIG_ITEM): CONFIG_NETWORK_OVERRIDES_FULL, (CUBE_TYPE): CUBE_TYPE_RPM]) as Map
        defaultLevel = configCube.getCell([(CONFIG_ITEM): CONFIG_DEFAULT_LEVEL, (CUBE_TYPE): CUBE_TYPE_RPM]) as long
        allGroups = configCube.getCell([(CONFIG_ITEM): CONFIG_ALL_GROUPS, (CUBE_TYPE): CUBE_TYPE_RPM]) as Map
        allGroupsKeys = new LinkedHashSet(allGroups.keySet())
        groupSuffix = configCube.getCell([(CONFIG_ITEM): CONFIG_GROUP_SUFFIX, (CUBE_TYPE): CUBE_TYPE_RPM]) as String
        loadTypesToAddMap(configCube)
        loadAvailableScopeKeysAndValues(configCube)
    }

    void loadAvailableScopeKeysAndValues(NCube configCube)
    {
        Map<String, Set<Object>> valuesByKey = new CaseInsensitiveMap<>()
        Set<String> derivedScopeKeys = configCube.getCell([(CONFIG_ITEM): CONFIG_DERIVED_SCOPE_KEYS, (CUBE_TYPE): CUBE_TYPE_RPM]) as Set

        /*TODO: Not needed currently, but will revisit
        Set<String> derivedSourceScopeKeys = configCube.getCell([(CONFIG_ITEM): CONFIG_DERIVED_SOURCE_SCOPE_KEYS, (CUBE_TYPE): CUBE_TYPE_RPM]) as Set
         Set<String> defaultOptionalScopeKeys = configCube.getCell([(CONFIG_ITEM): CONFIG_DEFAULT_OPTIONAL_SCOPE_KEYS, (CUBE_TYPE): CUBE_TYPE_RPM]) as Set
         availableScopeKeys = derivedScopeKeys + derivedSourceScopeKeys + defaultOptionalScopeKeys + [POLICY_CONTROL_DATE, QUOTE_DATE, EFFECTIVE_VERSION]
         */

        //Values for Risk, SourceRisk, Coverage, SourceCoverage, etc.
        derivedScopeKeys.each { String key ->
            String cubeName = RPM_SCOPE_CLASS_DOT + key + DOT_TRAITS
            Set<Object> values = getColumnValues(appId, cubeName, key)
            valuesByKey[key] = values
            valuesByKey[SOURCE_SCOPE_KEY_PREFIX + key] = values
        }

        //Values for effective version
        valuesByKey[EFFECTIVE_VERSION] = getAllVersions(appId.tenant, appId.app) as Set<Object>

        //Values from ENT.APP
        String latest = NCubeManager.getLatestVersion(ApplicationID.DEFAULT_TENANT, ENT_APP, ReleaseStatus.RELEASE.name())
        ApplicationID entAppAppId = new ApplicationID(ApplicationID.DEFAULT_TENANT, ENT_APP, latest, ReleaseStatus.RELEASE.name(), ApplicationID.HEAD)

        valuesByKey[BUSINESS_DIVISION_CODE] = getColumnValues(entAppAppId, BUSINESS_DIVISION_CUBE_NAME, BUSINESS_DIVISION_CODE)
        Set<Object> stateValues = getColumnValues(entAppAppId, STATE_CUBE_NAME, STATE)
        valuesByKey[STATE] = stateValues
        valuesByKey[LOCATION_STATE] = stateValues

        availableScopeValues = valuesByKey
    }

    Set<Object> loadAvailableScopeValues(String cubeName, String key)
    {
        Set<Object> values = getColumnValues(appId, cubeName, key)
        availableScopeValues[key] = values
        return values
    }


    private static Set<Object> getColumnValues(ApplicationID applicationID, String cubeName, String axisName)
    {
        NCube cube = NCubeManager.getCube(applicationID, cubeName)
        Set values = new LinkedHashSet()
        Axis axis = cube.getAxis(axisName)
        if (axis)
        {
            for (Column column : axis.columnsWithoutDefault)
            {
                values.add(column.value)
            }
        }
        return values
    }

    private static Set<String> getAllVersions(String tenant, String app)
    {
        Map<String, List<String>> versionsMap = NCubeManager.getVersions(tenant, app)
        Set<String> versions = new TreeSet<>(new VersionComparator())
        versions.addAll(versionsMap[ReleaseStatus.RELEASE.name()])
        versions.addAll(versionsMap[ReleaseStatus.SNAPSHOT.name()])
        return versions
    }
}