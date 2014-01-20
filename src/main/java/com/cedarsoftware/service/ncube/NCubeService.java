package com.cedarsoftware.service.ncube;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.AxisType;
import com.cedarsoftware.ncube.AxisValueType;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.GroovyExpression;
import com.cedarsoftware.ncube.GroovyMethod;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.util.CaseInsensitiveSet;
import com.cedarsoftware.util.StringUtilities;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
        return NCubeManager.getInstance().getNCubes(getConnection(), app, version, status, pattern, new Date());
    }

    public String getHtml(String name, String app, String version, String status)
    {
        NCube ncube = NCubeManager.getInstance().loadCube(getConnection(), app, name, version, status, new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not get HTML, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        return ncube.toHtml("trait", "traits", "businessDivisionCode", "bu", "month");
    }

    public Object getCube(String name, String app, String version, String status)
    {
        try
        {
            NCube ncube = NCubeManager.getInstance().loadCube(getConnection(), app, name, version, status, new Date());
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
        return NCubeManager.getInstance().getAppNames(getConnection(), new Date());
    }

    public Object[] getAppVersions(String app, String status)
    {
        return NCubeManager.getInstance().getAppVersions(getConnection(), app, status, new Date());
    }

    public void createCube(NCube ncube, String app, String version)
    {
        NCubeManager.getInstance().createCube(getConnection(), app, ncube, version);
    }

    public boolean deleteCube(String name, String app, String version)
    {
        return NCubeManager.getInstance().deleteCube(getConnection(), app, name, version, false);
    }

    public Set<String> getReferencedCubeNames(String name, String app, String version, String status)
    {
        Set<String> refs = new CaseInsensitiveSet<String>();
        NCubeManager.getInstance().getReferencedCubeNames(getConnection(), app, name, version, status, new Date(), refs);
        return refs;
    }

    public Set<String> getRequiredScope(String name, String app, String version, String status)
    {
        NCube ncube = NCubeManager.getInstance().loadCube(getConnection(), app, name, version, status, new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could get required scope, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        return ncube.getRequiredScope();
    }

    public void duplicateCube(String newName, String name, String newApp, String app, String newVersion, String version, String status)
    {
        NCubeManager.getInstance().duplicate(getConnection(), newName, name, newApp, app, newVersion, version, status, new Date());
    }

    public void releaseCubes(String app, String version, String newSnapVer)
    {
        NCubeManager manager = NCubeManager.getInstance();
        Connection connection = getConnection();
        manager.releaseCubes(connection, app, version);
        manager.createSnapshotCubes(connection, app, version, newSnapVer);
    }

    public void changeVersionValue(String app, String currVersion, String newSnapVer)
    {
        NCubeManager.getInstance().changeVersionValue(getConnection(), app, currVersion, newSnapVer);
    }

    public void addAxis(String name, String app, String version, String axisName, String type, String valueType)
    {
        if (StringUtilities.isEmpty(axisName))
        {
            throw new IllegalArgumentException("Axis name cannot be empty.");
        }
        Connection connection = getConnection();
        NCubeManager manager = NCubeManager.getInstance();
        NCube ncube = manager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not add axis '" + axisName + "', NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }

        Axis axis = new Axis(axisName, AxisType.valueOf(type), AxisValueType.valueOf(valueType), false);
        ncube.addAxis(axis);
        manager.updateCube(connection, app, ncube, version);
    }

    public List<Axis> getAxes(String name, String app, String version, String status)
    {
        NCubeManager manager = NCubeManager.getInstance();
        NCube ncube = manager.loadCube(getConnection(), app, name, version, status, new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not load axes, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        return ncube.getAxes();
    }

    public Axis getAxis(String name, String app, String version, String status, String axisName)
    {
        NCubeManager manager = NCubeManager.getInstance();
        NCube ncube = manager.loadCube(getConnection(), app, name, version, status, new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could get axis '" + axisName + "', NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        return ncube.getAxis(axisName);
    }

    public void deleteAxis(String name, String app, String version, String axisName)
    {
        Connection connection = getConnection();
        NCubeManager manager = NCubeManager.getInstance();
        NCube ncube = manager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not delete axis '" + axisName + "', NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }

        ncube.deleteAxis(axisName);
        manager.updateCube(connection, app, ncube, version);
    }

    public void updateAxis(String name, String app, String version, String origAxisName, String axisName, boolean hasDefault, boolean isSorted, boolean multiMatch)
    {
        Connection connection = getConnection();
        NCubeManager manager = NCubeManager.getInstance();
        NCube ncube = manager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
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

        manager.updateCube(connection, app, ncube, version);
    }

    /**
     * Update the indicate column (by ID) with the passed in String value.  The String value
     * is parsed into DISCRETE, RANGE, SET, NEAREST, or RULE, and to the proper axis value type
     * for the axis.  This allows Strings like "[10, 25]" to be passed into a Range axis, for
     * example, and it will be added as a Range(10, 25) and will go through all the proper
     * "up promotion" before being set into the column.
     */
    public void updateColumnCell(String name, String app, String version, String colId, String value)
    {
        Connection connection = getConnection();
        NCubeManager manager = NCubeManager.getInstance();
        NCube ncube = manager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
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
        manager.updateCube(connection, app, ncube, version);
    }

    public void updateAxisColumns(String name, String app, String version, Axis updatedAxis)
    {
        Connection connection = getConnection();
        NCubeManager manager = NCubeManager.getInstance();
        NCube ncube = manager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not update Column, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }
        Axis oldAxis = ncube.getAxis(updatedAxis.getName());
        oldAxis.updateColumns(updatedAxis);
        manager.updateCube(connection, app, ncube, version);
    }

    public Object updateCell(String name, String app, String version, Object[] colIds, Object value)
    {
        Connection connection = getConnection();
        NCubeManager manager = NCubeManager.getInstance();
        NCube ncube = manager.loadCube(connection, app, name, version, "SNAPSHOT", new Date());
        if (ncube == null)
        {
            throw new IllegalArgumentException("Could not update Column, NCube '" + name + "' not found for app: " + app + ", version: " + version);
        }

        Set<Long> ids = new HashSet();
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
        manager.updateCube(connection, app, ncube, version);
        return ncube.getCellByIdNoExecute(ids).toString();
    }
}
