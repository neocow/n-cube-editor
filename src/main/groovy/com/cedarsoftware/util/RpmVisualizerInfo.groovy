package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.ReleaseStatus
import com.cedarsoftware.ncube.util.VersionComparator
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.RpmVisualizerConstants.*

/**
 * Holds information related to the visualization of cubes and their relationships.
 */

@CompileStatic
class RpmVisualizerInfo extends VisualizerInfo
{
    Map<String, List<String>> typesToAddMap = [:]

    RpmVisualizerInfo(){}

    RpmVisualizerInfo(ApplicationID applicationID, Map options)
    {
        super(applicationID, options)
    }

    @Override
    protected String getCubeType()
    {
        return CUBE_TYPE_RPM
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
                available
            }.keySet() as List
            typesToAddMap[sourceType] = typesToAdd
        }
    }

    @Override
    protected String getLoadAllCellValuesLabel()
    {
        LOAD_CELL_VALUES_LABEL
    }

    @Override
    void loadAvailableScopeKeysAndValues(NCube configCube)
    {
        Map<String, Set<Object>> valuesByKey = new CaseInsensitiveMap<>()
        Set<String> derivedScopeKeys = configCube.getCell([(CONFIG_ITEM): CONFIG_DERIVED_SCOPE_KEYS, (CUBE_TYPE): CUBE_TYPE_RPM]) as Set

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

    private static Set<String> getAllVersions(String tenant, String app)
    {
        Map<String, List<String>> versionsMap = NCubeManager.getVersions(tenant, app)
        Set<String> versions = new TreeSet<>(new VersionComparator())
        versions.addAll(versionsMap[ReleaseStatus.RELEASE.name()])
        versions.addAll(versionsMap[ReleaseStatus.SNAPSHOT.name()])
        return versions
    }
}