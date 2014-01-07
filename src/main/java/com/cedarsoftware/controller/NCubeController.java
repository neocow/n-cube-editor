package com.cedarsoftware.controller;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.AxisType;
import com.cedarsoftware.ncube.AxisValueType;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeInfoDto;
import com.cedarsoftware.service.ncube.NCubeService;
import com.cedarsoftware.util.CaseInsensitiveSet;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * NCubeController API.
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
public class NCubeController extends BaseController implements INCubeController
{
    private NCubeService nCubeService;

    public NCubeController(NCubeService service)
    {
        nCubeService = service;
    }

    public Object[] getCubeList(String filter, String app, String version, String status)
    {
        Object[] list = nCubeService.getNCubes(filter, app, version, status);
        Arrays.sort(list, new Comparator<Object>()
        {
            public int compare(Object o1, Object o2)
            {
                NCubeInfoDto info1 = (NCubeInfoDto) o1;
                NCubeInfoDto info2 = (NCubeInfoDto) o2;
                return info1.name.compareToIgnoreCase(info2.name);
            }
        });
        return list;
    }

    public String getHtml(String name, String app, String version, String status)
    {
        return nCubeService.getHtml(name, app, version, status);
    }

    public Object[] getAppNames()
    {
        return nCubeService.getAppNames();
    }

    public Object[] getAppVersions(String app, String status)
    {
        return nCubeService.getAppVersions(app, status);
    }

    /**
     * Create an n-cube (SNAPSHOT only).
     * @return boolean true if successful, otherwise a String error message.
     */
    public Object createCube(String name, String app, String version)
    {
        try
        {
            NCube ncube = new NCube(name);
            Axis axis = new Axis("Month", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY);
            axis.addColumn("Jan");
            axis.addColumn("Feb");
            axis.addColumn("Mar");
            axis.addColumn("Apr");
            axis.addColumn("May");
            axis.addColumn("Jun");
            axis.addColumn("Jul");
            axis.addColumn("Aug");
            axis.addColumn("Sep");
            axis.addColumn("Oct");
            axis.addColumn("Nov");
            axis.addColumn("Dec");
            ncube.addAxis(axis);
            nCubeService.createCube(ncube, app, version);
            return true;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * Delete an n-cube (SNAPSHOT only).
     * @return boolean true if successful, otherwise a String error message.
     */
    public Object deleteCube(String name, String app, String version)
    {
        try
        {
            if (!nCubeService.deleteCube(name, app, version))
            {
                return "Cannot delete RELEASE n-cube.";
            }
            return true;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * Find all references to an n-cube.  This is an expensive method, as all cubes within the
     * app (version and status) must be checked.
     * @return Object[] of String cube names that reference the named cube, otherwise a String
     * error message.
     */
    public Object getReferencesTo(String name, String app, String version, String status)
    {
        try
        {
            Set<String> references = new CaseInsensitiveSet<String>();
            Object[] ncubes = nCubeService.getNCubes("%", app, version, status);

            for (Object ncube : ncubes)
            {
                NCubeInfoDto info = (NCubeInfoDto) ncube;
                Set<String> refs = nCubeService.getReferencedCubeNames(info.name, app, version, status);
                if (refs.contains(name))
                {
                    references.add(info.name);
                }
            }
            references.remove(name);    // do not include reference to self.
            return references.toArray();
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * Find all references from (out going) an n-cube.
     * @return Object[] of String cube names that the passed in (named) cube references,
     * otherwise a String error message.
     */
    public Object getReferencesFrom(String name, String app, String version, String status)
    {
        try
        {
            Set<String> refs = nCubeService.getReferencedCubeNames(name, app, version, status);
            return refs.toArray();
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * Find all referenced input variables for a given n-cube (and through any n-cubes it
     * references).
     * @return Object[] of String names of each scope variable, otherwise a String error message.
     */
    public Object getRequiredScope(String name, String app, String version, String status)
    {
        try
        {
            Set<String> refs = nCubeService.getRequiredScope(name, app, version, status);
            return refs.toArray();
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * Duplicate the passed in cube, but change the name to newName AND the status of the new
     * n-cube will be SNAPSHOT.
     * @return boolean true if successful, otherwise a String error message.
     */
    public Object duplicateCube(String newName, String name, String newApp, String app, String newVersion, String version, String status)
    {
        try
        {
            nCubeService.duplicateCube(newName, name, newApp, app, newVersion, version, status);
            return true;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * Release the passed in SNAPSHOT version (update their status_cd to RELEASE), and then
     * duplicate all the n-cubes in the release, creating new ones in SNAPSHOT status with
     * the version number set to the newSnapVer.
     * @return boolean true if successful, otherwise a String error message.
     */
    public Object releaseCubes(String app, String version, String newSnapVer)
    {
        try
        {
            nCubeService.releaseCubes(app, version, newSnapVer);
            return true;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * Change the SNAPSHOT version number of an n-cube.
     * @return boolean true if successful, otherwise a String error message.
     */
    public Object changeVersionValue(String app, String currVersion, String newSnapVer)
    {
        try
        {
            nCubeService.changeVersionValue(app, currVersion, newSnapVer);
            return true;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * Add axis to an existing SNAPSHOT n-cube.
     * @return boolean true if successful, otherwise String error message.
     */
    public Object addAxis(String name, String app, String version, String axisName, String type, String valueType)
    {
        try
        {
            nCubeService.addAxis(name, app, version, axisName, type, valueType);
            return true;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * @return Object[] of JSON structure representing each axis or String error
     * message.  The Axis will look like this:
     * [ { "name":"axisName",
     *     "type":"DISCRETE",
     *     "valueType":"STRING",
     *     "defaultColumn":true | false,
     *     "preferredOrder":0 | 1,
     *     "multiMatch": true | false
     * }, ... ]
     */
    public Object getAxes(String name, String app, String version, String status)
    {
        try
        {
            List<Axis> axes = nCubeService.getAxes(name, app, version, status);
            return axes.toArray();
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * @return Object of JSON structure representing the requested axis, or String
     * error message.  The axis JSON object will look the same as it does for
     * getAxes() API.
     */
    public Object getAxis(String name, String app, String version, String status, String axisName)
    {
        try
        {
            return nCubeService.getAxis(name, app, version, status, axisName);
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * Delete the passed in axis.
     * @return boolean true if successful, otherwise String error message is returned.
     */
    public Object deleteAxis(String name, String app, String version, String axisName)
    {
        try
        {
            nCubeService.deleteAxis(name, app, version, axisName);
            return true;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /**
     * @return boolean true if successful, otherwise String error message is returned.
     */
    public Object updateAxis(String name, String app, String version, String origAxisName, String axisName, boolean hasDefault, boolean isSorted, boolean multiMatch)
    {
        try
        {
            nCubeService.updateAxis(name, app, version, origAxisName, axisName, hasDefault, isSorted, multiMatch);
            return true;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }
}
