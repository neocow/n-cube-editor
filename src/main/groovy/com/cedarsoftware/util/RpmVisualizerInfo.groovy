package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.ReleaseStatus
import com.cedarsoftware.ncube.util.VersionComparator
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.RpmVisualizerConstants.*

/**
 * Provides information to visualize rpm classes.
 */

@CompileStatic
class RpmVisualizerInfo extends VisualizerInfo
{
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

    @Override
    List getTypesToAdd(String group)
    {
        if (!group.endsWith(groupSuffix))
        {
            return typesToAddMap[allGroups[group]]
        }
        return null
    }

   @Override
   void loadTypesToAddMap(NCube configCube)
    {
        typesToAddMap = [:]
        String json = NCubeManager.getResourceAsString(JSON_FILE_PREFIX + TYPES_TO_ADD_CUBE_NAME + JSON_FILE_SUFFIX)
        NCube typesToAddCube = NCube.fromSimpleJson(json)
        Set<String> allTypes = configCube.getCell([(CONFIG_ITEM): CONFIG_ALL_TYPES, (CUBE_TYPE): cubeType]) as Set

        allTypes.each { String sourceType ->
            Map<String, Boolean> map = typesToAddCube.getMap([(SOURCE_TYPE): sourceType, (TARGET_TYPE): [] as Set]) as Map
            List<String> typesToAdd = map.findAll { String type, Boolean available ->
                available
            }.keySet() as List
            typesToAddMap[sourceType] = typesToAdd
        }
    }

    @Override
    protected String getLoadCellValuesLabel()
    {
        LOAD_CELL_VALUES_LABEL
    }

  /*  @Override
    void loadAvailableScopeKeysAndValues(NCube configCube)
    {
        Map<String, Set<Object>> valuesByKey = new CaseInsensitiveMap<>()
        Set<String> allTypes = configCube.getCell([(CONFIG_ITEM): CONFIG_ALL_TYPES, (CUBE_TYPE): cubeType]) as Set

        //Values for Risk, SourceRisk, Coverage, SourceCoverage, etc.
        allTypes.each { String key ->
            String cubeName = RPM_SCOPE_CLASS_DOT + key + DOT_TRAITS
            Set<Object> values = getColumnValues(appId, cubeName, key)
            valuesByKey[key] = values
            valuesByKey[SOURCE_SCOPE_KEY_PREFIX + key] = values
        }

        //Values for effective version
        valuesByKey[EFFECTIVE_VERSION] = getAllVersions(appId.tenant, appId.app) as Set<Object>

        //Values from reference data //TODO: Specify via Visualizer configs which ref data to get instead of hard coding ent.manual.State and ent.manual.BusinessDivision.
        String refDataAppNameJson = NCubeManager.getResourceAsString(JSON_FILE_PREFIX + VISUALIZER_CONFIG_REF_DATA_APP_NAME_CUBE_NAME + JSON_FILE_SUFFIX)
        NCube refDataAppNameCube = NCube.fromSimpleJson(refDataAppNameJson)
        refDataAppNameCube.applicationID = appId
        String refDataAppName = refDataAppNameCube.getCell([(CONFIG_ITEM): CONFIG_REF_DATA_APP_NAME, (CUBE_TYPE): cubeType, (CONFIG_APP_NAME): appId.app]) as String
        String latest = NCubeManager.getLatestVersion(ApplicationID.DEFAULT_TENANT, refDataAppName, ReleaseStatus.RELEASE.name())
        ApplicationID refDataAppId = new ApplicationID(ApplicationID.DEFAULT_TENANT, refDataAppName, latest, ReleaseStatus.RELEASE.name(), ApplicationID.HEAD)

        valuesByKey[BUSINESS_DIVISION_CODE] = getColumnValues(refDataAppId, BUSINESS_DIVISION_CUBE_NAME, BUSINESS_DIVISION_CODE)
        Set<Object> stateValues = getColumnValues(refDataAppId, STATE_CUBE_NAME, STATE)
        valuesByKey[STATE] = stateValues
        valuesByKey[LOCATION_STATE] = stateValues

        optionalScopeValues = valuesByKey
    }*/

   /* private static Set<String> getAllVersions(String tenant, String app)
    {
        Map<String, List<String>> versionsMap = NCubeManager.getVersions(tenant, app)
        Set<String> versions = new TreeSet<>(new VersionComparator())
        versions.addAll(versionsMap[ReleaseStatus.RELEASE.name()])
        versions.addAll(versionsMap[ReleaseStatus.SNAPSHOT.name()])
        return versions
    }*/
}