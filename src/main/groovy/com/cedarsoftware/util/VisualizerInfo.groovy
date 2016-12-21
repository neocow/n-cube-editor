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
	String startCubeName
	Map<String, Object> scope
	List<Map<String, Object>> nodes = []
	List<Map<String, Object>> edges = []

	long maxLevel = 1
	long nodeCount  = 1
    long defaultLevel
    boolean loadTraits = false

	Map<String,String> allGroups
    String groupSuffix

	Set<String> availableGroupsAllLevels = [] as Set
	Set<String> availableScopeKeys = []
	Map<String, Set<Object>> availableScopeValues = [:]
    Map<String, Set<String>> requiredScopeKeys = [:]
    Map<String, Set<String>> optionalScopeKeys = [:]
    Map<String, Object> networkOverridesBasic = null
    Map<String, Object> networkOverridesFull = null

    Map<String, List<String>> typesToAddMap = [:]

    VisualizerInfo(ApplicationID applicationID, Map options)
    {
        appId = applicationID
        startCubeName = options.startCubeName as String
        scope = options.scope as CaseInsensitiveMap
        availableScopeKeys = options.availableScopeKeys as Set ?:  DEFAULT_AVAILABLE_SCOPE_KEYS
        availableScopeValues = options.availableScopeValues as Map ?: loadAvailableScopeValues()
        typesToAddMap = options.typesToAddMap as Map ?: loadTypesToAddMap()
        loadTraits = options.loadTraits as boolean
        defaultLevel = DEFAULT_LEVEL
        allGroups = ALL_GROUPS_MAP
        groupSuffix = _ENUM
        networkOverridesBasic = options.networkOverridesBasic as Map
        networkOverridesFull = options.networkOverridesFull as Map
        if (!networkOverridesBasic || !networkOverridesFull)
        {
            loadNetworkOverrides()
        }
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

    Map<String, List<String>> loadTypesToAddMap()
    {
        Map<String, List<String>> typesToAddMap = [:]
        NCube cube = NCubeManager.getCube(appId, TYPES_TO_ADD_NCUBE_NAME)
        ALL_EPM_TYPES.each { String sourceType ->
            Map<String, Boolean> map = cube.getMap([(SOURCE_TYPE): sourceType, (TARGET_TYPE): [] as Set]) as Map
            List<String> typesToAdd = map.findAll { String type, Boolean available ->
                available == true
            }.keySet() as List
            typesToAddMap[sourceType] = typesToAdd
        }
        return typesToAddMap
    }

    void loadNetworkOverrides()
    {
        NCube cube = NCubeManager.getCube(appId, NETWORK_OVERRIDES_NCUBE_NAME)
        networkOverridesBasic = cube.getCell([(OVERRIDE_TYPE): OVERRIDE_TYPE_BASIC, (CUBE_TYPE): CUBE_TYPE_RPM]) as Map
        networkOverridesFull = cube.getCell([(OVERRIDE_TYPE): OVERRIDE_TYPE_FULL, (CUBE_TYPE): CUBE_TYPE_RPM]) as Map
    }

    Map<String, Set<Object>> loadAvailableScopeValues()
    {
        Map<String, Set<Object>> valuesByKey = new CaseInsensitiveMap<>()

        //Values for Risk, SourceRisk, Coverage, SourceCoverage, etc.
        DERIVED_SCOPE_KEYS.each { String key ->
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

        return valuesByKey
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