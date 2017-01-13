package com.cedarsoftware.ncube

import groovy.transform.CompileStatic

@CompileStatic
class TestingHelper
{
    static void loadCubesFromResource(ApplicationID appId, String jsonFilePrefix, Set<String> cubeNames)
    {
        cubeNames.each { String cubeName ->
            if (!NCubeManager.isCubeCached(appId, cubeName))
            {
                String json = NCubeManager.getResourceAsString(jsonFilePrefix + cubeName + '.json')
                NCube cube = NCube.fromSimpleJson(json)
                cube.applicationID = appId
                NCubeManager.addCube(appId, cube)
            }
        }
    }

    static NCubePersister getNCubePersister()
    {
        return new TestingNCubePersister()
    }
}
