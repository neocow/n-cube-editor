package com.cedarsoftware.controller;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.AxisType;
import com.cedarsoftware.ncube.AxisValueType;
import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeInfoDto;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.ncube.ReleaseStatus;
import com.cedarsoftware.ncube.UrlCommandCell;
import com.cedarsoftware.ncube.formatters.HtmlFormatter;
import com.cedarsoftware.service.ncube.NCubeService;
import com.cedarsoftware.servlet.JsonCommandServlet;
import com.cedarsoftware.util.CaseInsensitiveMap;
import com.cedarsoftware.util.CaseInsensitiveSet;
import com.cedarsoftware.util.DeepEquals;
import com.cedarsoftware.util.EncryptionUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
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
    private NCubeService nCubeService;
    Pattern antStyleReplacementPattern = Pattern.compile("[$][{](.*?)[}]");


    public NCubeController(NCubeService service)
    {
        nCubeService = service;
    }

    private static boolean isAllowed(String app, String version)
    {
        return isAllowed(app, version, "SNAPSHOT");
    }

    //temp condition!!
    private static boolean isAllowed(String app, String version, String status)
    {
        return "UD.REF.APP".equals(app) && version.startsWith("0.0.") && "SNAPSHOT".equals(status) || !"UD.REF.APP".equals(app);
    }

    private static List<String> _defaultUrls = new ArrayList<String>();
    public static void setDefaultUrls(List<String> urls) {
        _defaultUrls = urls;
    }

    public List<String> resolveDefaultUrls(String app, String version, String status) {

        List<String> urls = new ArrayList<>();

        for (String s : _defaultUrls)
        {
            Matcher m = antStyleReplacementPattern.matcher(s);
            StringBuffer sb = new StringBuffer(s.length());
            while (m.find())
            {
                String text = m.group(1);

                if ("version".equals(text))
                {
                    m.appendReplacement(sb, version);
                }
                else if ("application".equals(text))
                {
                    m.appendReplacement(sb, app);
                }
                else if ("status".equals(text))
                {
                    m.appendReplacement(sb, status);
                }
            }
            m.appendTail(sb);
            urls.add(sb.toString());
        }
        return urls;
    }

    public String runTest(String name, String app, String version, String status, NCubeTest test) {
        try
        {
            NCube ncube = nCubeService.getCube(name, app, version, status);
            Map coord = test.getCoordinate();
            Object actual = ncube.getCell(coord);
            Object expected = test.getExpectedResult();


            // For UrlCommands we need to actually execute the command for an apple to apple comparison.
            // Note:  a GroovyExpression that returns true is the same as a Boolean.True return type.
            if (expected instanceof UrlCommandCell) {
                UrlCommandCell cell = (UrlCommandCell)expected;

                coord.put("ncube", ncube);
                coord.put("input", new HashMap());
                coord.put("output", new HashMap());
                expected = cell.execute(coord);
            }

            if (!DeepEquals.deepEquals(actual, expected)) {
                HashMap map = new HashMap();
                map.put("status", "Failure");
                map.put("message", "Expected: '" + expected + "'<br />  Actual: '" + actual + "'");
                return JsonWriter.objectToJson(map);
            }

            HashMap map = new HashMap();
            map.put("status", "Success");
            map.put("message", "Success");
            return JsonWriter.objectToJson(map);
        }
        catch(Exception e)
        {
            fail(e);
            return null;
        }
    }

    public Object[] getCubeList(String filter, String app, String version, String status)
    {
        try
        {
            List<String> baseUrls = resolveDefaultUrls(app, version, status);
            NCubeManager.addBaseResourceUrls(baseUrls, version);
            NCube sysInfo = null;
            try
            {
                sysInfo = nCubeService.getCube(SYS_NCUBE_INFO, app, version, status);
            }
            catch (Exception ignored)
            { }
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
                        sysInfo.getCells(input, output);
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

    private Map<String, Object> makeGenericAugInfo()
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
            return nCubeService.getHtml(name, app, version, status);
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    public String getJson(String name, String app, String version, String status)
    {
        try
        {
            return nCubeService.getJson(name, app, version, status);
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
            return nCubeService.getAppNames();
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
                    int minor = Integer.valueOf(pieces[0]) * 1000;
                    int rev = Integer.valueOf(pieces[0]);
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
            if (!isAllowed(app, version))
            {
                markRequestFailed("This app and version CANNOT be edited.");
                return;
            }

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
    public boolean deleteCube(String name, String app, String version)
    {
        try
        {
            if (!isAllowed(app, version))
            {
                markRequestFailed("This app and version CANNOT be edited");
                return false;
            }

            if (!nCubeService.deleteCube(name, app, version))
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
            Set<String> refs = nCubeService.getReferencedCubeNames(name, app, version, status);
            return refs.toArray();
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
            Set<String> refs = nCubeService.getRequiredScope(name, app, version, status);
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
            if (!isAllowed(newApp, newVersion))
            {
                markRequestFailed("This app and version CANNOT be edited.");
                return;
            }
            nCubeService.duplicateCube(newName, name, newApp, app, newVersion, version, status);
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
            if (!isAllowed(app, "never"))
            {
                markRequestFailed("This app and version CANNOT be edited.");
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
            if (!isAllowed(app, "never"))
            {
                markRequestFailed("This app and version CANNOT be edited.");
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
            nCubeService.addAxis(name, app, version, axisName, type, valueType);
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
            NCube ncube = nCubeService.getCube(name, app, version, status);
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
            int i=0;
            for (Object item : items)
            {
                Map col = (Map) item;
                col.put("id", i++);
            }
        }
        return axisConverted;
    }

    /**
     * Delete the passed in axis.
     */
    public void deleteAxis(String name, String app, String version, String axisName)
    {
        try
        {
            if (!isAllowed(app, version))
            {
                markRequestFailed("This app and version CANNOT be edited.");
                return;
            }
            nCubeService.deleteAxis(name, app, version, axisName);
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    public void updateAxis(String name, String app, String version, String origAxisName, String axisName, boolean hasDefault, boolean isSorted)
    {
        try
        {
            if (!isAllowed(app, version))
            {
                markRequestFailed("This app and version CANNOT be edited.");
                return;
            }
            nCubeService.updateAxis(name, app, version, origAxisName, axisName, hasDefault, isSorted);
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
    public void updateAxisColumns(String name, String app, String version, Map<String, Object> updatedAxis)
    {
        try
        {
            if (!isAllowed(app, version))
            {
                markRequestFailed("This app and version CANNOT be edited.");
                return;
            }
            NCube ncube = nCubeService.getCube(name, app, version, ReleaseStatus.SNAPSHOT.name());
            Axis oldAxis = ncube.getAxis((String)updatedAxis.get("name"));
            updatedAxis.put("@type", Axis.class.getName());
//            Axis update = new Axis(updatedAxis.get("name"), updatedAxis.get("type"), updatedAxis.get("valueType"), updatedAxis.get("hasDefault"));

            Axis update = new Axis((String)updatedAxis.get("name"), AxisType.DISCRETE, AxisValueType.STRING, false);
            // TODO: Test adding column at FRONT
            oldAxis.updateColumns(update);
//            nCubeService.updateNCube(ncube);
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
            if (!isAllowed(app, version))
            {
                markRequestFailed("This app and version CANNOT be edited.");
                return;
            }
            nCubeService.renameCube(oldName, newName, app, version);
        }
        catch(Exception e)
        {
            fail(e);
        }
    }

    public void saveJson(String name, String app, String version, String json)
    {
        try
        {
            if (!isAllowed(app, version))
            {
                markRequestFailed("This app and version CANNOT be edited.");
                return;
            }
            nCubeService.updateCube(name, app, version, json);
        }
        catch (Exception e)
        {
            fail(e);
        }
    }

    public void getTestData(String name, String app, String version, String status)
    {
        try
        {
            nCubeService.getTestData(name, app, version, status);
        }
        catch (Exception e)
        {
            fail(e);
        }
    }


    public void saveTestData(String name, String app, String version, String testData)
    {
        try
        {
            if (!isAllowed(app, version))
            {
                markRequestFailed("This app and version CANNOT be edited.");
                return;
            }
            nCubeService.updateTestData(name, app, version, testData);
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
            NCube ncube = nCubeService.getCube(name, app, version, status);
            if (ncube == null)
            {
                return new Object[]{};
            }
            return ncube.generateNCubeTests().toArray();

            /*
            Object[] items = new Object[list.size()];
            int i=0;
            for (NCubeTest test : list) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("name", test.getName());
                map.put("coordDescription", test.getCoordDescription());
                map.put("expectedResultDescription", test.getExpectedResultDescription());
                items[i++] = map;
            }

            return items;
            */
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    public Object getCell(String name, String app, String version, String status, HashMap map)
    {
        try
        {
            return nCubeService.getCell(name, app, version, status, map);
        }
        catch (Exception e)
        {
            fail(e);
            return null;
        }
    }

    /**
     * In-place update of a cell.  Requires heavy parsing to interpret what the user's intended
     * data type is for the cell (byte, short, int, long, float, double, boolean, character,
     * String, Date, Object[], BigDecimal, BigInteger, string url, binary url, groovy expression,
     * groovy method, groovy template, template url, expression url, method url, or null).
     */
    public boolean updateCell(String name, String app, String version, Object[] ids, CellInfo cellInfo)
    {
        try
        {
            if (!isAllowed(app, version))
            {
                markRequestFailed("This app and version CANNOT be edited.");
                return false;
            }

            NCube ncube = nCubeService.getCube(name, app, version, ReleaseStatus.SNAPSHOT.name());
            Set<Long> colIds = getCoordinate(ids, ncube);

            Object cellValue = null;
            if (cellInfo == null)
            {
                ncube.removeCellById(colIds);
            }
            else
            {
                cellValue = cellInfo.isUrl ?
                        CellInfo.parseJsonValue(null, cellInfo.value, cellInfo.dataType, cellInfo.isCached) :
                        CellInfo.parseJsonValue(cellInfo.value, null, cellInfo.dataType, false);
                ncube.setCellById(cellValue, colIds);
            }
            nCubeService.updateNCube(ncube);
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
            // 1. Fetch the n-cube
            NCube ncube = nCubeService.getCube(name, app, version, status);

            // 2. create an SHA1 to axis name maps
            Set<Long> colIds = getCoordinate(ids, ncube);

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

    private Set<Long> getCoordinate(Object[] ids, NCube ncube)
    {
        Map<String, Axis> axes = new HashMap<>();
        for (Axis axis : (List<Axis>)ncube.getAxes())
        {
            final String axisName = axis.getName();
            String colName = HtmlFormatter.isSafeAxisName(axisName) ?
                    axisName :  EncryptionUtilities.calculateSHA1Hash(StringUtilities.getBytes(axisName, "UTF-8"));
            axes.put(colName, axis);
        }

        // 3. Locate columns on each axis
        Set<Long> colIds = new HashSet<>();
        for (Object id : ids)
        {
            Object[] pair = (Object[]) id;
            String axisName = (String) pair[0];
            String pos = (String) pair[1];
            Axis axis = axes.get(axisName);
            List<Column> cols = axis.getColumns();
            Column column = cols.get(Integer.parseInt(pos));
            colIds.add(column.getId());
        }
        return colIds;
    }

}
