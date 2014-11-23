package com.cedarsoftware.controller;

import com.cedarsoftware.ncube.ApplicationID;
import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.AxisType;
import com.cedarsoftware.ncube.AxisValueType;
import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.GroovyExpression;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeInfoDto;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.ncube.ReleaseStatus;
import com.cedarsoftware.ncube.RuleInfo;
import com.cedarsoftware.ncube.StringValuePair;
import com.cedarsoftware.ncube.formatters.NCubeTestReader;
import com.cedarsoftware.ncube.formatters.NCubeTestWriter;
import com.cedarsoftware.ncube.formatters.TestResultsFormatter;
import com.cedarsoftware.service.ncube.NCubeService;
import com.cedarsoftware.servlet.JsonCommandServlet;
import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.CaseInsensitiveMap;
import com.cedarsoftware.util.CaseInsensitiveSet;
import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.ThreadAwarePrintStream;
import com.cedarsoftware.util.ThreadAwarePrintStreamErr;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

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
public class NCubeController extends BaseController
{
    public static final String SYS_NCUBE_INFO = "sys.groups";
    private static final Pattern VERSION_REGEX = Pattern.compile("[.]");
    private static final Pattern IS_NUMBER_REGEX = Pattern.compile("^[\\d,.e+-]+$");
    private NCubeService nCubeService;
    private static final Log LOG = LogFactory.getLog(NCubeController.class);
    static final AtomicLong baseAxisId = new AtomicLong(1);

    public NCubeController(NCubeService service)
    {
        nCubeService = service;
        System.setErr(new ThreadAwarePrintStreamErr());
        System.setOut(new ThreadAwarePrintStream());
    }

    private String getUserForDatabase()
    {
        String user = getUser();
        return StringUtilities.length(user) > 10 ? user.substring(0, 10) : user;
    }

    private static String getUser()
    {
        String user = null;
        HttpServletRequest request = JsonCommandServlet.servletRequest.get();
        Enumeration e = request.getHeaderNames();
        while (e.hasMoreElements())
        {
            String headerName = (String) e.nextElement();
            if ("smuser".equalsIgnoreCase(headerName))
            {
                user = request.getHeader(headerName);
                break;
            }
        }

        if (StringUtilities.isEmpty(user))
        {
            user = System.getProperty("user.name");
        }

        return user;
    }

    private boolean isAllowed(String app, String version)
    {
        return true;
//        String user = getUser();
//        NCube adminCube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name()), "sys.admin.permissions");
//        Map input = new HashMap();
//        input.put("username", user);
//        input.put("function", "admin");
//        return (boolean) adminCube.getCell(input);
    }

    private boolean isAllowed(String app, String version, String cubeName)
    {
        return true;
//        if (StringUtilities.isEmpty(cubeName))
//        {
//            return false;
//        }
//
//        String user = getUser();
//        NCube adminCube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name()), "sys.admin.permissions");
//        if (adminCube == null)
//        {
//            return false;
//        }
//        String permFunc = cubeName.toLowerCase().startsWith("sys.") ? "admin" : "edit";
//        Map input = new HashMap();
//        input.put("username", user);
//        input.put("function", permFunc);
//        return (boolean) adminCube.getCell(input);
    }

    public Map runTest(String name, String app, String version, String status, NCubeTest test)
    {
        try
        {
            NCube ncube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), name);
            Map coord = test.createCoord();

            Map output = new LinkedHashMap();
            ncube.getCell(coord, output);               // Execute test case

            RuleInfo ruleInfoMain = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
            ruleInfoMain.setSystemOut(ThreadAwarePrintStream.getContent());
            ruleInfoMain.setSystemErr(ThreadAwarePrintStreamErr.getContent());

            Map args = new LinkedHashMap();
            args.put("input", coord);
            args.put("output", output);
            args.put("ncube", ncube);

            List<GroovyExpression> assertions = test.createAssertions();

            boolean success = true;
            int i = 0;
            Set<String> errors = new LinkedHashSet<>();
            for (GroovyExpression exp : assertions)
            {
                i++;

                try
                {
                    Map assOutput = new LinkedHashMap<>(output);
                    RuleInfo ruleInfo = new RuleInfo();
                    assOutput.put(NCube.RULE_EXEC_INFO, ruleInfo);
                    args.put("output", assOutput);
                    if (!NCube.isTrue(exp.execute(args)))
                    {
                        errors.add("[assertion " + i + " failed]: " + exp.getCmd());
                        success = false;
                    }
                }
                catch (Exception e)
                {
                    errors.add("[exception]");
                    errors.add("\n");
                    errors.add(getCauses(e));
                    success = false;
                }
            }

            ruleInfoMain.setAssertionFailures(errors);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("_message", new TestResultsFormatter(output).format());
            map.put("_result", success);
            return map;
        }
        catch(Exception e)
        {
            fail(e);
            ThreadAwarePrintStream.getContent();
            ThreadAwarePrintStreamErr.getContent();
            return null;
        }
    }

    public Object[] getDeletedCubeList(String filter, String app, String version)
    {
        Object[] cubeInfos = nCubeService.getDeletedCubes(filter, app, version);
        return cubeInfos;
    }

    public void restoreCube(String app, String version, String status, Object[] cubeNames)
    {
        nCubeService.restoreCube(app, version, status, cubeNames, getUserForDatabase());
    }

    public Object[] getRevisionHistory(String cubeName, String app, String version, String status)
    {
        Object[] cubeInfos = nCubeService.getRevisionHistory(cubeName, app, version, status);
        return cubeInfos;
    }

    public Object[] getCubeList(String filter, String app, String version, String status)
    {
        try
        {
            NCube sysInfo = null;
            try
            {
                sysInfo = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), SYS_NCUBE_INFO);
            }
            catch (Exception e)
            {
                LOG.info("Failed to find 'sys.group' for app: '" + app + ", version: " + version + ", status: " + status);
            }
            Object[] list = nCubeService.getNCubes(null, app, version, status);
            List<Map<String, Object>> augmentedInfo = new ArrayList<>();

            for (Object dto : list)
            {
                NCubeInfoDto infoDto = (NCubeInfoDto) dto;
                Map<String, Object> input = new HashMap<>();
                Map<String, Object> output = new CaseInsensitiveMap<>();
                input.put("name", infoDto.name);
                Map<String, Object> augInfo;
                if (sysInfo == null)
                {
                    augInfo = makeGenericAugInfo();
                }
                else
                {
                    try
                    {
                        sysInfo.getCell(input, output);
                        augInfo = output.containsKey("info") ? (Map<String, Object>) output.get("info") : makeGenericAugInfo();
                    }
                    catch (Exception ignored)
                    {   // Blew up on running the rules
                        augInfo = makeGenericAugInfo();
                    }
                }

                augInfo.put("ncube", infoDto);
                augmentedInfo.add(augInfo);
            }

            // Sort by Group, then by n-cube name
            Collections.sort(augmentedInfo, new Comparator<Map>()
            {
                public int compare(Map o1, Map o2)
                {
                    String group1 = (String) o1.get("group");
                    String group2 = (String) o2.get("group");
                    if (group1.equalsIgnoreCase(group2))
                    {   // Secondary sort key - group names are the same, then use the n-cube name within the group.
                        NCubeInfoDto info1 = (NCubeInfoDto) o1.get("ncube");
                        NCubeInfoDto info2 = (NCubeInfoDto) o2.get("ncube");
                        return info1.name.compareToIgnoreCase(info2.name);
                    }
                    else
                    {
                        return group1.compareTo(group2);
                    }
                }
            });
            return augmentedInfo.toArray();
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    private static Map<String, Object> makeGenericAugInfo()
    {
        Map<String, Object> augInfo;
        augInfo = new LinkedHashMap<>();
        augInfo.put("group", "General");
        augInfo.put("prefix", "");
        return augInfo;
    }

    public String getHtml(String name, String app, String version, String status)
    {
        try
        {
            ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status);
            NCube ncube = getCube(appId, name);
            String html = ncube.toHtml("trait", "traits", "businessDivisionCode", "bu", "month", "months", "col", "column", "cols", "columns");
            return html;
        }
        catch (Exception e)
        {
            markRequestFailed(getCauses(e));
            LOG.info(getCauses(e));
            return null;
        }
    }

    private NCube getCube(ApplicationID appId, String name)
    {
        NCube cube = NCubeManager.getCube(appId, name);
        if (cube == null)
        {
            throw new IllegalArgumentException("Unable to load cube: " + name + " for app: " + appId);
        }
        return cube;
    }

    public String getJson(String name, String app, String version, String status)
    {
        try
        {
            NCube ncube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), name);
            return ncube.toFormattedJson();
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    public Object[] getAppNames()
    {
        try
        {
            // TODO: Add tenant
            return nCubeService.getAppNames(ApplicationID.DEFAULT_TENANT);
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    public Object[] getAppVersions(String app, String status)
    {
        try
        {
            Object[] appVersions = nCubeService.getAppVersions(app, status);
            Arrays.sort(appVersions, new Comparator<Object>()
            {
                public int compare(Object o1, Object o2)
                {
                    String v1 = (String)o1;
                    String v2 = (String)o2;
                    return getVersionValue(v1) - getVersionValue(v2);
                }

                int getVersionValue(String v)
                {
                    String[] pieces = VERSION_REGEX.split(v);
                    if (pieces.length != 3)
                    {
                        return 0;
                    }
                    int major = Integer.valueOf(pieces[0]) * 1000 * 1000;
                    int minor = Integer.valueOf(pieces[1]) * 1000;
                    int rev = Integer.valueOf(pieces[2]);
                    return major + minor + rev;
                }
            });
            return appVersions;
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    /**
     * Create an n-cube (SNAPSHOT only).
     */
    public void createCube(String name, String app, String version)
    {
        try
        {
            if (!isAllowed(app, version, name))
            {
                markRequestFailed("You do not have permission to create cube: " + name + " in app: " + app);
                return;
            }

            NCube ncube = new NCube(name);
            Axis axis = new Axis("Month", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, baseAxisId.getAndIncrement());
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
            nCubeService.createCube(ncube, app, version, getUserForDatabase());
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    /**
     * Delete an n-cube (SNAPSHOT only).
     * @return boolean true if successful, otherwise a String error message.
     */
    public boolean deleteCube(String cubeName, String app, String version)
    {
        try
        {
            if (!isAllowed(app, version, cubeName))
            {
                markRequestFailed("You do not have permission to delete cube: " + cubeName + " in app: " + app);
                return false;
            }

            if (!nCubeService.deleteCube(cubeName, app, version, getUserForDatabase()))
            {
                markRequestFailed("Cannot delete RELEASE n-cube.");
            }
            return true;
        }
        catch (Exception e)
        {
            fail(e);
            return false;
        }
    }

    /**
     * Find all references to an n-cube.  This is an expensive method, as all cubes within the
     * app (version and status) must be checked.
     * @return Object[] of String cube names that reference the named cube, otherwise a String
     * error message.
     */
    public Object[] getReferencesTo(String name, String app, String version, String status)
    {
        try
        {
            Set<String> references = new CaseInsensitiveSet<>();
            Object[] ncubes = nCubeService.getNCubes("%", app, version, status);

            for (Object ncube : ncubes)
            {
                NCubeInfoDto info = (NCubeInfoDto) ncube;
                NCubeManager.getReferencedCubeNames(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), info.name, references);
                if (references.contains(name))
                {
                    references.add(info.name);
                }
            }
            references.remove(name);    // do not include reference to self.
            return references.toArray();
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    /**
     * Find all references from (out going) an n-cube.
     * @return Object[] of String cube names that the passed in (named) cube references,
     * otherwise a String error message.
     */
    public Object[] getReferencesFrom(String name, String app, String version, String status)
    {
        try
        {
            Set<String> references = new CaseInsensitiveSet<>();
            NCubeManager.getReferencedCubeNames(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), name, references);
            return references.toArray();
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    /**
     * Find all referenced input variables for a given n-cube (and through any n-cubes it
     * references).
     * @return Object[] of String names of each scope variable, otherwise a String error message.
     */
    public Object[] getRequiredScope(String name, String app, String version, String status)
    {
        try
        {
            NCube ncube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), name);
            Set<String> refs = ncube.getRequiredScope();
            return refs.toArray();
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    /**
     * Duplicate the passed in cube, but change the name to newName AND the status of the new
     * n-cube will be SNAPSHOT.
     */
    public void duplicateCube(String newName, String name, String newApp, String app, String newVersion, String version, String status)
    {
        try
        {
            if (!isAllowed(newApp, newVersion, name))
            {
                markRequestFailed("You do not have permission to duplicate cube: " + name + " in app: " + app);
                return;
            }
            nCubeService.duplicateCube(newName, name, newApp, app, newVersion, version, status, getUserForDatabase());
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    /**
     * Release the passed in SNAPSHOT version (update their status_cd to RELEASE), and then
     * duplicate all the n-cubes in the release, creating new ones in SNAPSHOT status with
     * the version number set to the newSnapVer.
     */
    public void releaseCubes(String app, String version, String newSnapVer)
    {
        try
        {
            if (!isAllowed(app, version))
            {
                markRequestFailed("You do not have permission to release cubes in app: " + app);
                return;
            }
            nCubeService.releaseCubes(app, version, newSnapVer);
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    /**
     * Change the SNAPSHOT version number of an n-cube.
     */
    public void changeVersionValue(String app, String currVersion, String newSnapVer)
    {
        try
        {
            if (!isAllowed(app, currVersion))
            {
                markRequestFailed("You do not have permission to change version value in app: " + app);
                return;
            }
            nCubeService.changeVersionValue(app, currVersion, newSnapVer);
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    /**
     * Add axis to an existing SNAPSHOT n-cube.
     */
    public void addAxis(String name, String app, String version, String axisName, String type, String valueType)
    {
        try
        {
            if (!isAllowed(app, version, name))
            {
                markRequestFailed("You do not have permission to add axis to cube: " + name + " in app: " + app);
                return;
            }
            nCubeService.addAxis(name, app, version, axisName, type, valueType, getUserForDatabase());
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    /**
     * Return the requested axis.  The returned axis has some 'massaging' applied to it before
     * being returned.  First, it is being returned using the 'map-of-maps' format from json-io
     * so that the column IDs can be converted from Longs to Strings, because Javascript cannot
     * process a 64-bit long value (it stores numbers using a double, which means it can only
     * reliably process 53-bits of a long).  Converting the longs to Strings first, allows the
     * column ID to round-trip to the UI and back, and json-io will 'mash' the String column ID
     * into the Long column ID (within the JsonCommandServlet) as it receives the String.  It
     * sense the data-type mismatch (json-io does) and then attempts to convert the String to a
     * numeric value (which succeeds).  This allows the full 64-bit id to make it round trip.
     */
    public Map getAxis(String name, String app, String version, String status, String axisName)
    {
        try
        {
            NCube ncube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), name);
            Axis axis = ncube.getAxis(axisName);
            return convertAxis(axis);
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    /**
     * Convert Axis to Map of Map representation (using json-io) and modify the
     * column ID to a String in the process.  This allows the column ID to work on
     * clients (like Javascript) that cannot support 64-bit values.
     */
    static Map convertAxis(Axis axis) throws IOException
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
                Column actualCol= axis.getColumnById((Long)col.get("id"));
                col.put("id", String.valueOf(col.get("id")));
                col.put("value", new CellInfo(actualCol.getValue()).value);
            }
        }
        return axisConverted;
    }

    /**
     * Delete the passed in axis.
     */
    public void deleteAxis(String cubeName, String app, String version, String axisName)
    {
        try
        {
            if (!isAllowed(app, version, cubeName))
            {
                markRequestFailed("You do not have permission to delete axis for cube: " + cubeName + " in app: " + app);
                return;
            }
            nCubeService.deleteAxis(cubeName, app, version, axisName, getUserForDatabase());
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    public void updateAxis(String cubeName, String app, String version, String origAxisName, String axisName, boolean hasDefault, boolean isSorted)
    {
        try
        {
            if (!isAllowed(app, version, cubeName))
            {
                markRequestFailed("You do not have permission to update axis for cube: " + cubeName + " in app: " + app);
                return;
            }

            nCubeService.updateAxis(cubeName, app, version, origAxisName, axisName, hasDefault, isSorted, getUserForDatabase());
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    /**
     * Update an entire set of columns on an axis at one time.  The updatedAxis is not a real axis,
     * but treated like an Axis-DTO where the list of columns within the axis are in display order.
     */
    public void updateAxisColumns(String cubeName, String app, String version, Axis updatedAxis)
    {
        try
        {
            if (!isAllowed(app, version, cubeName))
            {
                markRequestFailed("You do not have permission to update columns on cube: " + cubeName + " in app: " + app);
                return;
            }

            NCube ncube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name()), cubeName);
            ncube.updateColumns(updatedAxis);
            nCubeService.updateNCube(ncube, getUserForDatabase());
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    public void renameCube(String oldName, String newName, String app, String version)
    {
        try
        {
            if (!isAllowed(app, version, oldName))
            {
                markRequestFailed("You do not have permission to rename cube: " + oldName + " in app: " + app);
                return;
            }
            nCubeService.renameCube(oldName, newName, app, version);
        }
        catch(Exception e)
        {
            fail(e);
        }
    }

    public void saveJson(String cubeName, String app, String version, String json)
    {
        try
        {
            if (!isAllowed(app, version, cubeName))
            {
                markRequestFailed("You do not have permission to save JSON for cube: " + cubeName + " in app: " + app);
                return;
            }
            nCubeService.updateCube(cubeName, app, version, json, getUserForDatabase());
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    public Object[] getTests(String name, String app, String version, String status)
    {
        try
        {
            NCube ncube = NCubeManager.getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), name);
            if (ncube == null)
            {
                throw new IllegalArgumentException("Could not find test data '" + name + "' not found for app: " + app);
            }

            String s = NCubeManager.getTestData(ncube.getApplicationID(), name);
            if (StringUtilities.isEmpty(s))
            {
                return null;
            }
            return new NCubeTestReader().convert(s).toArray();
        }
        catch (Exception e)
        {
            fail(e);
        }
        return null;
    }

    public void saveTests(String name, String app, String version, Object[] tests)
    {
        try
        {
            if (!isAllowed(app, version))
            {
                markRequestFailed("This app and version CANNOT be saved.");
                return;
            }
            //String data = JsonWriter.objectToJson(tests);
            String data = new NCubeTestWriter().format(tests);
            nCubeService.updateTestData(name, app, version, data);
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    private static void markRequestFailed(Object data)
    {
        JsonCommandServlet.servletRequest.get().setAttribute(JsonCommandServlet.ATTRIBUTE_STATUS, false);
        JsonCommandServlet.servletRequest.get().setAttribute(JsonCommandServlet.ATTRIBUTE_FAIL_MESSAGE, data);
    }

    /**
     * Indicate to the Ajax servlet (JsonCommandServlet) that the 'status' field should
     * be set to 'false', and then set the 'data' field to the String of exception
     * text.
     * @param e Exception that occurred when calling the service.
     */
    private static void fail(Exception e)
    {
        markRequestFailed(getCauses(e));
        LOG.warn("error occurred", e);
    }

    private static String getCauses(Throwable t)
    {
        StringBuilder s = new StringBuilder();
        while (t != null)
        {
            if (t.getMessage() == null)
            {
                s.append(t.toString());
            }
            else
            {
                s.append(t.getMessage());
            }
            t = t.getCause();
            if (t != null)
            {
                s.append("<hr class=\"hr-small\"/>");
            }
        }

        return s.toString();
    }

    /**
     * In-place update of a cell.  'Value' is the final (converted) object type to be stored
     * in the indicated (by colIds) cell.
     */
    public Object[] generateTests(String name, String app, String version, String status)
    {
        try
        {
            NCube ncube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), name);

            if (!"SNAPSHOT".equalsIgnoreCase(status))
            {
                throw new IllegalArgumentException("You cannot generate tests for release cubes");
            }

            Object[] list = ncube.generateNCubeTests().toArray();

            nCubeService.updateTestData(name, app, version, new NCubeTestWriter().format(list));
            return list;
        }
        catch (Exception e)
        {
            fail(e);
        }
        return null;
    }

    /**
     * In-place update of a cell.  'Value' is the final (converted) object type to be stored
     * in the indicated (by colIds) cell.
     */
    public NCubeTest createNewTest(String name, String app, String version, String status, String testName)
    {
        try
        {
            NCube ncube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), name);

            if (StringUtilities.isEmpty(testName))
            {
                throw new IllegalArgumentException("Invalid name during test creation: " + testName + ", cube: " + name + " not found for app " + app);
            }

            if (!"SNAPSHOT".equalsIgnoreCase(status))
            {
                throw new IllegalArgumentException("You cannot generate new tests for release cubes");
            }

            Set<String> items = ncube.getRequiredScope();
            int size = items == null ? 0 : items.size();

            StringValuePair<CellInfo>[] coords = new StringValuePair[size];
            if (size > 0) {
                int i = 0;
                for (String s : items) {
                    coords[i++] = new StringValuePair(s, null);
                }
            }

            CellInfo[] assertions = {new CellInfo("exp", "output.return", false, false)};
            NCubeTest test = new NCubeTest(testName, coords, assertions);

            return test;
        }
        catch (Exception e)
        {
            fail(e);
        }
        return null;
    }

    /**
     * In-place update of a cell.
     */
    public boolean updateCell(String cubeName, String app, String version, Object[] ids, CellInfo cellInfo)
    {
        try
        {
            if (!isAllowed(app, version, cubeName))
            {
                markRequestFailed("You do not have permissions to update cells in cube: " + cubeName + " for app: " + app);
                return false;
            }

            NCube ncube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name()), cubeName);
            Set<Long> colIds = getCoordinate(ids);

            if (cellInfo == null)
            {
                ncube.removeCellById(colIds);
            }
            else
            {
                Object cellValue = cellInfo.isUrl ?
                        CellInfo.parseJsonValue(null, cellInfo.value, cellInfo.dataType, cellInfo.isCached) :
                        CellInfo.parseJsonValue(cellInfo.value, null, cellInfo.dataType, false);
                ncube.setCellById(cellValue, colIds);
            }
            nCubeService.updateNCube(ncube, getUserForDatabase());
            return true;
        }
        catch(Exception e)
        {
            fail(e);
            return false;
        }
    }

    public Object getCellNoExecute(String name, String app, String version, String status, Object[] ids)
    {
        try
        {
            NCube ncube = getCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), name);
            Set<Long> colIds = getCoordinate(ids);
            Object cell = ncube.getCellByIdNoExecute(colIds);
            CellInfo cellInfo = new CellInfo(cell);
            cellInfo.collapseToUiSupportedTypes();
            return cellInfo;
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    public boolean clearCells(String app, String version, String status, String cubeName, Object[] ids)
    {
        try
        {
            if (!isAllowed(app, version, cubeName))
            {
                markRequestFailed("You do not have permissions to cut cells from cube: " + cubeName + " for app: " + app);
                return false;
            }

            if (ids == null || ids.length == 0)
            {
                markRequestFailed("No IDs of cells to cut/clear were given.");
                return false;
            }

            ApplicationID appId = new ApplicationID(ApplicationID. DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name());
            NCube ncube = getCube(appId, cubeName);

            for (Object id : ids)
            {
                Object[] cellId = (Object[]) id;
                if (ArrayUtilities.isEmpty(cellId))
                {
                    continue;
                }
                Set<Long> colIds = getCoordinate(cellId);
                ncube.removeCellById(colIds);
            }
            nCubeService.updateNCube(ncube, getUserForDatabase());
            return true;
        }
        catch (Exception e)
        {
            fail(e);
            return false;
        }
    }

    public boolean pasteCells(String app, String version, String cubeName, Object[] values, Object[] coords)
    {
        try
        {
            if (!isAllowed(app, version, cubeName))
            {
                markRequestFailed("You do not have permissions to paste into cube: " + cubeName + " for app: " + app);
                return false;
            }

            if (values == null || values.length == 0 || coords == null || coords.length == 0)
            {
                markRequestFailed("Values and coordinates must not be empty or length of 0.");
                return false;
            }

            ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, ReleaseStatus.SNAPSHOT.name());
            NCube ncube = getCube(appId, cubeName);
            if (ncube == null)
            {
                markRequestFailed("Cube: " + cubeName + " not found for app: " + appId);
                return false;
            }
            for (int i=0; i < coords.length; i++)
            {
                Object[] row = (Object[]) coords[i];
                if (ArrayUtilities.isEmpty(row))
                {
                    break;
                }

                for (int j=0; j < row.length; j++)
                {
                    Object[] ids = (Object[]) row[j];
                    Set<Long> cellId = getCoordinate(ids);
                    Object value = convertStringToValue(getValueRepeatIfNecessary(values, i, j));
                    if (value == null)
                    {
                        ncube.removeCellById(cellId);
                    }
                    else
                    {
                        ncube.setCellById(value, cellId);
                    }
                }
            }
            nCubeService.updateNCube(ncube, getUserForDatabase());
            return true;
        }
        catch (Exception e)
        {
            fail(e);
            return false;
        }
    }

    private static String getValueRepeatIfNecessary(Object[] values, int row, int col)
    {
        if (row > (values.length - 1))
        {
            row %= values.length;
        }
        Object[] valueRow = (Object[]) values[row];
        if (ArrayUtilities.isEmpty(valueRow))
        {
            return null;
        }
        if (col > (valueRow.length - 1))
        {
            col %= valueRow.length;
        }
        return (String) valueRow[col];
    }

    public String resolveRelativeUrl(String app, String version, String status, String relativeUrl)
    {
        try
        {
            String absUrl = NCubeManager.resolveRelativeUrl(new ApplicationID(ApplicationID.DEFAULT_TENANT, app, version, status), relativeUrl);
            return absUrl;
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    // ----------------------------------------- utility (non-API) methods ---------------------------------------------

    private static Object convertStringToValue(String origValue)
    {
        if (origValue == null || StringUtilities.isEmpty(origValue))
        {
            return null;
        }

        String value = origValue.trim();
        if (StringUtilities.isEmpty(value))
        {
            return null;
        }

        if ("0".equals(value))
        {
            return 0L;
        }
        else if ("true".equalsIgnoreCase(value))
        {
            return true;
        }
        else if ("false".equalsIgnoreCase(value))
        {
            return false;
        }

        if (isNumeric(value))
        {
            value = removeCommas(value);
            if (!value.contains("."))
            {
                try
                {
                    return Long.parseLong(value);
                }
                catch (Exception ignored) { }
            }

            try
            {
                return new BigDecimal(value);
            }
            catch (Exception ignored) { }
        }

        // Try as a date (the code below supports numerous different date formats)
        try
        {
            return DateUtilities.parseDate(value);
        }
        catch (Exception ignored) { }

        // OK, if all else fails, return it as the string it was
        return origValue;
    }

    public static boolean isNumeric(String str)
    {
        return IS_NUMBER_REGEX.matcher(str).matches();  // match a number with optional '-' and decimal.
    }

    private static String removeCommas(String str)
    {
        StringBuilder s = new StringBuilder();
        for (int i=0; i < str.length(); i++)
        {
            char x = str.charAt(i);
            if (x != ',')
            {
                s.append(x);
            }
        }
        return s.toString();
    }

    private static Set<Long> getCoordinate(Object[] ids)
    {
        // 3. Locate columns on each axis
        Set<Long> colIds = new HashSet<>();
        for (Object id : ids)
        {
            colIds.add(Long.parseLong((String)id));
        }
        return colIds;
    }
}
