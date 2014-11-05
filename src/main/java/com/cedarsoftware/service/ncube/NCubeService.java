package com.cedarsoftware.service.ncube;

import com.cedarsoftware.ncube.ApplicationID;
import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.AxisType;
import com.cedarsoftware.ncube.AxisValueType;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.ReleaseStatus;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * RESTful Ajax/JSON API for editor application
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class NCubeService
{

    public Object[] getNCubes(String pattern, String app, String version, String status)
    {
        return NCubeManager.getNCubes(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), pattern);
    }

    public Object[] getAppNames(String account)
    {
        return NCubeManager.getAppNames(account);
    }

    public Object[] getAppVersions(String app, String status)
    {
        return NCubeManager.getAppVersions(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, ApplicationID.DEFAULT_VERSION, status));
    }

    public void createCube(NCube ncube, String app, String version)
    {
        NCubeManager.createCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name()), ncube);
    }

    public boolean deleteCube(String name, String app, String version)
    {
        return NCubeManager.deleteCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name()), name);
    }

    public void duplicateCube(String newName, String name, String newApp, String app, String newVersion, String version, String status)
    {
        NCubeManager.duplicate(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), new ApplicationID(ApplicationID.DEFAULT_TENANT, newApp, newVersion, status), name, newName);
    }

    public void releaseCubes(String app, String version, String newSnapVer)
    {
        NCubeManager.releaseCubes(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name()));
        NCubeManager.createSnapshotCubes(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name()), newSnapVer);
    }

    public void changeVersionValue(String app, String currVersion, String newSnapVer)
    {
        NCubeManager.changeVersionValue(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, currVersion, ReleaseStatus.SNAPSHOT.name()), newSnapVer);
    }

    public void addAxis(String name, String app, String version, String axisName, String type, String valueType)
    {
        if (StringUtilities.isEmpty(axisName))
        {
            throw new IllegalArgumentException("Axis name cannot be empty.");
        }

        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name());
        NCube ncube = NCubeManager.getCube(id, name);
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not add axis '" + axisName + "', NCube '" + name + "' not found for app: " + id.toString());
        }

        Axis axis = new Axis(axisName, AxisType.valueOf(type), AxisValueType.valueOf(valueType), false, Axis.DISPLAY);
        NCubeManager.updateCube(id, ncube);
    }

    /**
     * Delete the specified axis.
     */
    public void deleteAxis(String name, String app, String version, String axisName)
    {
        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name());
        NCube ncube = NCubeManager.getCube(id, name);
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not delete axis '" + axisName + "', NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }

        if (ncube.getNumDimensions() == 1)
        {
            throw new IllegalArgumentException("Could not delete axis '" + axisName + "' - at least one axis must exist on n-cube.");
        }

        ncube.deleteAxis(axisName);
        NCubeManager.updateCube(id, ncube);
    }

    /**
     * Update the 'informational' part of the Axis (not the columns).
     */
    public void updateAxis(String name, String app, String version, String origAxisName, String axisName, boolean hasDefault, boolean isSorted)
    {
        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name());
        NCube ncube = NCubeManager.getCube(id, name);
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not update axis '" + origAxisName + "', NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }

        // Rename axis
        if (!origAxisName.equalsIgnoreCase(axisName))
        {
            ncube.renameAxis(origAxisName, axisName);
        }

        // Update default column setting (if changed)
        Axis axis = ncube.getAxis(axisName);
        if (axis.hasDefaultColumn() && !hasDefault)
        {   // If it went from having default column to NOT having default column...
            ncube.deleteColumn(axisName, null);
        }
        else if (!axis.hasDefaultColumn() && hasDefault)
        {
            if (axis.getType() != AxisType.NEAREST)
            {
                ncube.addColumn(axisName, null);
            }
        }

        // update preferred column order
        if (axis.getType() != AxisType.RULE)
        {
            axis.setColumnOrder(isSorted ? Axis.SORTED : Axis.DISPLAY);
        }

        NCubeManager.updateCube(id, ncube);
    }

    /**
     * Update the indicated column (by ID) with the passed in String value.  The String value
     * is parsed into DISCRETE, RANGE, SET, NEAREST, or RULE, and to the proper axis value type
     * for the axis.  This allows Strings like "[10, 25]" to be passed into a Range axis, for
     * example, and it will be added as a Range(10, 25) and will go through all the proper
     * "up promotion" before being set into the column.
     */
    public void updateColumnCell(String name, String app, String version, String colId, String value)
    {
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name());
        NCube ncube = NCubeManager.getCube(appId, name);
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not update Column, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }

        long id;
        try
        {
            id = Long.parseLong(colId);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Column ID passed in (" + colId + ") is not a number, attempting to update column on NCube '" + name + "'");
        }

        Axis axis = ncube.getAxisFromColumnId(id);
        if (axis == null)
        {
            throw new IllegalArgumentException("Column ID passed in (" + colId + ") does not match any axis on NCube '" + name + "'");
        }

        ncube.updateColumn(id, axis.convertStringToColumnValue(value));
        NCubeManager.updateCube(appId, ncube);
    }

    /**
     * In-place update of a cell.  'Value' is the final (converted) object type to be stored
     * in the indicated (by colIds) cell.
     */
    public void updateNCube(NCube ncube)
    {
        ApplicationID appId = ncube.getApplicationID();
        NCubeManager.updateCube(appId, ncube);
    }

    public boolean renameCube(String oldName, String newName, String app, String version)
    {
        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name());
        return NCubeManager.renameCube(id, oldName, newName);
    }

    /**
     * Update / Save a single n-cube -or- create / update a batch of n-cubes, represented as a JSON
     * array [] of n-cubes.
     */
    public void updateCube(String name, String app, String version, String json)
    {
        json = json.trim();
        List cubes;
        boolean checkName;
        if (json.startsWith("["))
        {
            checkName = false;
            cubes = getCubes(json);
        }
        else
        {
            checkName = true;
            cubes = new ArrayList();
            cubes.add(NCube.fromSimpleJson(json));
        }

        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name());
        for (Object object : cubes)
        {
            if (object instanceof NCube)
            {
                NCube ncube = (NCube) object;
                if (checkName)
                {
                    if (!StringUtilities.equalsIgnoreCase(name, ncube.getName()))
                    {
                        throw new IllegalArgumentException("The n-cube name cannot be different than selected n-cube.  Use Rename n-cube option from n-cube menu to rename the cube.");
                    }
                }

                if (NCubeManager.doesCubeExist(id, ncube.getName()))
                {
                    NCubeManager.updateCube(id, ncube);
                }
                else
                {
                    NCubeManager.createCube(id, ncube);
                }
            }
            else
            {
                JsonObject map = (JsonObject) object;
                String cubeName = (String) map.get("ncube");
                String cmd = (String) map.get("action");

                if ("delete".equalsIgnoreCase(cmd))
                {
                    NCubeManager.deleteCube(id, cubeName);
                }
            }
        }
    }

    public static List getCubes(String json)
    {
        String lastSuccessful = "";
        try
        {
            JsonObject ncubes = (JsonObject) JsonReader.jsonToMaps(json);
            Object[] cubes = ncubes.getArray();
            List cubeList = new ArrayList(cubes.length);

            for (Object cube : cubes)
            {
                JsonObject ncube = (JsonObject) cube;
                if (ncube.containsKey("action"))
                {
                    cubeList.add(ncube);
                    lastSuccessful = (String) ncube.get("ncube");
                }
                else
                {
                    String json1 = JsonWriter.objectToJson(ncube);
                    NCube nCube = NCube.fromSimpleJson(json1);
                    cubeList.add(nCube);
                    lastSuccessful = nCube.getName();
                }
            }

            return cubeList;
        }
        catch (Exception e)
        {
            String s = "Failed to load n-cubes from passed in JSON, last successful cube read: " + lastSuccessful;
            throw new IllegalArgumentException(s, e);
        }
    }

    /**
     * In-place update of a cell.  'Value' is the final (converted) object type to be stored
     * in the indicated (by colIds) cell.
     */
    public String getTestData(String name, String app, String version, String status)
    {
        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status);
        NCube ncube = NCubeManager.getCube(id, name);
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not find test data '" + name + "' not found for app: " + app + ", version: " + version);
        }

        return NCubeManager.getTestData(id, name);
    }

    /**
     * In-place update of a cell.  'Value' is the final (converted) object type to be stored
     * in the indicated (by colIds) cell.
     */
    public boolean updateTestData(String name, String app, String version, String tests)
    {
        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name());
        NCube ncube = NCubeManager.getCube(id, name);
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not update test data, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }

        return NCubeManager.updateTestData(id, name, tests);
    }
}
