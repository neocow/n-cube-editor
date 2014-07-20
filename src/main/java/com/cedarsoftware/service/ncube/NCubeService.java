package com.cedarsoftware.service.ncube;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.AxisType;
import com.cedarsoftware.ncube.AxisValueType;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.util.CaseInsensitiveSet;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private DataSource dataSource;

    public void setDataSource(DataSource ds)
    {
        dataSource = ds;
    }

    private Connection getConnection()
    {
        return DataSourceUtils.getConnection(dataSource);
    }

    public Object[] getNCubes(String pattern, String app, String version, String status)
    {
        return NCubeManager.getNCubes(getConnection(), app, version, status, pattern, new Date());
    }

    public String getHtml(String name, String app, String version, String status)
    {
        NCube ncube = NCubeManager.loadCube(getConnection(), app, name, version, status, new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not get HTML, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        return ncube.toHtml("trait", "traits", "businessDivisionCode", "bu", "month");
    }

    public String getJson(String name, String app, String version, String status)
    {
        NCube ncube = NCubeManager.loadCube(getConnection(), app, name, version, status, new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not get HTML, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        return ncube.toFormattedJson();
    }

    public Object getCube(String name, String app, String version, String status)
    {
        try
        {
            NCube ncube = NCubeManager.loadCube(getConnection(), app, name, version, status, new Date());
            if (ncube == null)
            {
                throw new IllegalArgumentException("Could not retrieve NCube '" + name + "' not found for app: " + app + ", version: " + version);
            }
            return ncube;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    public Object[] getAppNames()
    {
        return NCubeManager.getAppNames(getConnection(), new Date());
    }

    public Object[] getAppVersions(String app, String status)
    {
        return NCubeManager.getAppVersions(getConnection(), app, status, new Date());
    }

    public void createCube(NCube ncube, String app, String version)
    {
        NCubeManager.createCube(getConnection(), app, ncube, version);
    }

    public boolean deleteCube(String name, String app, String version)
    {
        return NCubeManager.deleteCube(getConnection(), app, name, version, false);
    }

    public Set<String> getReferencedCubeNames(String name, String app, String version, String status)
    {
        Set<String> refs = new CaseInsensitiveSet<String>();
        NCubeManager.getReferencedCubeNames(getConnection(), app, name, version, status, new Date(), refs);
        return refs;
    }

    public Set<String> getRequiredScope(String name, String app, String version, String status)
    {
        NCube ncube = NCubeManager.loadCube(getConnection(), app, name, version, status, new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could get required scope, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        return ncube.getRequiredScope();
    }

    public void duplicateCube(String newName, String name, String newApp, String app, String newVersion, String version, String status)
    {
        NCubeManager.duplicate(getConnection(), newName, name, newApp, app, newVersion, version, status, new Date());
    }

    public void releaseCubes(String app, String version, String newSnapVer)
    {
        Connection connection = getConnection();
        NCubeManager.releaseCubes(connection, app, version);
        NCubeManager.createSnapshotCubes(connection, app, version, newSnapVer);
    }

    public void changeVersionValue(String app, String currVersion, String newSnapVer)
    {
        NCubeManager.changeVersionValue(getConnection(), app, currVersion, newSnapVer);
    }

    public void addAxis(String name, String app, String version, String axisName, String type, String valueType)
    {
        if (StringUtilities.isEmpty(axisName))
        {
            throw new IllegalArgumentException("Axis name cannot be empty.");
        }
        Connection connection = getConnection();
        NCube ncube = NCubeManager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not add axis '" + axisName + "', NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }

        Axis axis = new Axis(axisName, AxisType.valueOf(type), AxisValueType.valueOf(valueType), false, Axis.DISPLAY);
        ncube.addAxis(axis);
        NCubeManager.updateCube(connection, app, ncube, version);
    }

    /**
     * Return the full set of Axis objects for this n-cube.  The Axes are returned in Map form with
     * the column IDs converted to Strings (from Longs) due to the 64-bit Javascript issue.  The axes
     * when sent back still come back in as proper Axis objects, as json-io will convert the String id
     * to a Long when placing the value into the Axis object.
     */
    public List<Map> getAxes(String name, String app, String version, String status) throws IOException
    {
        NCube ncube = NCubeManager.loadCube(getConnection(), app, name, version, status, new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not load axes, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        List<Axis> axes = ncube.getAxes();
        List<Map> axesConverted = new ArrayList<Map>();
        for (Axis axis : axes)
        {
            axesConverted.add(convertAxis(axis));
        }
        return axesConverted;
    }

    /**
     * Convert Axis to Map of Map representation (using json-io) and modify the
     * column ID to a String in the process.  This allows the column ID to work on
     * clients (like Javascript) that cannot support 64-bit values.
     */
    Map convertAxis(Axis axis) throws IOException
    {
        String json = JsonWriter.objectToJson(axis);
        Map axisConverted = JsonReader.jsonToMaps(json);
        Map cols = (Map) axisConverted.get("columns");
        Object[] items = (Object[]) cols.get("@items");
        if (items != null)
        {
            for (Object item : items)
            {
                Map col = (Map) item;
                Long id = (Long) col.get("id");
                col.put("id", id.toString());

            }
        }
        return axisConverted;
    }

    /**
     * Retrieve the specified axis, however, return it as a Map (JsonObject) so that it can be
     * massaged on the way out.  This is done to allow converting of the column IDs from Long to
     * String so that they can round-trip to a Javascript client (which cannot handle a 64-bit long).
     */
    public Map getAxis(String name, String app, String version, String status, String axisName) throws IOException
    {
        NCube ncube = NCubeManager.loadCube(getConnection(), app, name, version, status, new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could get axis '" + axisName + "', NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        Axis axis = ncube.getAxis(axisName);
        return convertAxis(axis);
    }

    /**
     * Delete the specified axis.
     */
    public void deleteAxis(String name, String app, String version, String axisName)
    {
        Connection connection = getConnection();
        NCube ncube = NCubeManager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not delete axis '" + axisName + "', NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }

        if (ncube.getNumDimensions() == 1)
        {
            throw new IllegalArgumentException("Could not delete axis '" + axisName + "' - at least one axis must exist on n-cube.");
        }

        ncube.deleteAxis(axisName);
        NCubeManager.updateCube(connection, app, ncube, version);
    }

    /**
     * Update the 'informational' part of the Axis (not the columns).
     */
    public void updateAxis(String name, String app, String version, String origAxisName, String axisName, boolean hasDefault, boolean isSorted, boolean multiMatch)
    {
        Connection connection = getConnection();
        NCube ncube = NCubeManager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
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
            ncube.addColumn(axisName, null);
        }

        // update preferred column order
        axis.setColumnOrder(isSorted ? Axis.SORTED : Axis.DISPLAY);

        // update multi-match state
        axis.setMultiMatch(multiMatch);

        NCubeManager.updateCube(connection, app, ncube, version);
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
        Connection connection = getConnection();
        NCube ncube = NCubeManager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
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

        // TODO: Finish convertStringToColumnValue()
        // TODO: Make sure this method is used when the inbound column values are sent from the Edit Column box.
        ncube.updateColumn(id, axis.convertStringToColumnValue(value));
        NCubeManager.updateCube(connection, app, ncube, version);
    }

    /**
     * Update an entire set of columns at once.  This method will delete columns no longer in the passed in
     * list, add columns that are new (have negative ids), and update columns with the same id but new value.
     */
    public void updateAxisColumns(String name, String app, String version, Axis updatedAxis)
    {
        Connection connection = getConnection();
        NCube ncube = NCubeManager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not update Column, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        Axis oldAxis = ncube.getAxis(updatedAxis.getName());
        oldAxis.updateColumns(updatedAxis);
        NCubeManager.updateCube(connection, app, ncube, version);
    }

    /**
     * In-place update of a cell.  'Value' is the final (converted) object type to be stored
     * in the indicated (by colIds) cell.
     */
    public Object updateCell(String name, String app, String version, Object[] colIds, Object value)
    {
        Connection connection = getConnection();
        NCube ncube = NCubeManager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not update Column, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }

        Set<Long> ids = new HashSet<Long>();
        for (Object id : colIds)
        {
            try
            {
                ids.add(Long.parseLong((String)id));
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Could not set cell because coordinate passed in references unknown column: " + id + ", NCube '" + name + "'");
            }
        }

        ncube.setCellById(value, ids);
        NCubeManager.updateCube(connection, app, ncube, version);
        return ncube.getCellByIdNoExecute(ids).toString();
    }

    public boolean renameCube(String oldName, String newName, String app, String version)
    {
        Connection connection = getConnection();
        return NCubeManager.renameCube(connection, oldName, newName, app, version);
    }

    /**
     * Update / Save a single n-cube -or- create / update a batch of n-cubes, represented as a JSON
     * array [] of n-cubes.
     */
    public void updateCube(String name, String app, String version, String json)
    {
        json = json.trim();
        List<NCube> cubes;
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

        Connection connection = getConnection();

        for (NCube ncube : cubes)
        {
            if (checkName)
            {
                if (!StringUtilities.equalsIgnoreCase(name, ncube.getName()))
                {
                    throw new IllegalArgumentException("The n-cube name cannot be different than selected n-cube.  Use Rename n-cube option from n-cube menu to rename the cube.");
                }
            }

            if (NCubeManager.doesCubeExist(connection, app, ncube.getName(), version, "SNAPSHOT", new Date()))
            {
                NCubeManager.updateCube(connection, app, ncube, version);
            }
            else
            {
                NCubeManager.createCube(connection, app, ncube, version);
            }
        }
    }

    public static List<NCube> getCubes(String json)
    {
        String lastSuccessful = "";
        try
        {
            JsonObject ncubes = (JsonObject) JsonReader.jsonToMaps(json);
            Object[] cubes = ncubes.getArray();
            List<NCube> cubeList = new ArrayList<NCube>(cubes.length);

            for (Object cube : cubes)
            {
                JsonObject ncube = (JsonObject) cube;
                String json1 = JsonWriter.objectToJson(ncube);
                NCube nCube = NCube.fromSimpleJson(json1);
                cubeList.add(nCube);
                lastSuccessful = nCube.getName();
            }

            return cubeList;
        }
        catch (Exception e)
        {
            String s = "Failed to load n-cubes from passed in JSON, last successful cube read: " + lastSuccessful;
            throw new IllegalArgumentException(s, e);
        }
    }
}
