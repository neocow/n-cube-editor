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
	long selectedLevel
    boolean loadTraits = false

	Map<String,String> allGroups
    String groupSuffix

    Set<String> selectedGroups
	Set<String> availableGroupsAllLevels = [] as Set
	Set<String> availableScopeKeys = []
	Map<String, Set<Object>> availableScopeValues = [:]
    Map<String, Set<String>> requiredScopeKeys = [:]
    Map<String, Set<String>> optionalScopeKeys = [:]

    Map<String, List<String>> typesToAddMap = [:]

    VisualizerInfo(ApplicationID applicationID, Map options)
    {
        appId = applicationID
        startCubeName = options.startCubeName as String
        scope = options.scope as CaseInsensitiveMap
        Set groups = options.selectedGroups as Set
        selectedGroups = groups == null ? ALL_GROUPS_KEYS : groups; //If null, use all groups. If empty or not empty set, use set.
        String selectedLevel = options.selectedLevel as String
        this.selectedLevel = selectedLevel == null ? DEFAULT_LEVEL : Converter.convert(selectedLevel, long.class) as long
        availableScopeKeys =  options.availableScopeKeys as Set ?: DEFAULT_AVAILABLE_SCOPE_KEYS
        availableScopeValues = options.availableScopeValues as Map ?: loadAvailableScopeValues()
        typesToAddMap = options.typesToAddMap as Map ?: [:]
        loadTraits = options.loadTraits as boolean
        allGroups = ALL_GROUPS_MAP
        groupSuffix = _ENUM
    }

	void trimSelectedLevel()
	{
		selectedLevel = selectedLevel > nodeCount ? nodeCount : selectedLevel
	}

	void trimSelectedGroups()
	{
		selectedGroups = availableGroupsAllLevels.intersect(selectedGroups)
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