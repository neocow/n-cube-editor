package com.cedarsoftware.controller

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.Axis
import com.cedarsoftware.ncube.AxisRef
import com.cedarsoftware.ncube.AxisType
import com.cedarsoftware.ncube.AxisValueType
import com.cedarsoftware.ncube.CellInfo
import com.cedarsoftware.ncube.Column
import com.cedarsoftware.ncube.CommandCell
import com.cedarsoftware.ncube.Delta
import com.cedarsoftware.ncube.DeltaProcessor
import com.cedarsoftware.ncube.GroovyExpression
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeInfoDto
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.NCubeManager.ACTION
import com.cedarsoftware.ncube.NCubeTest
import com.cedarsoftware.ncube.ReleaseStatus
import com.cedarsoftware.ncube.RuleInfo
import com.cedarsoftware.ncube.exception.AxisOverlapException
import com.cedarsoftware.ncube.exception.BranchMergeException
import com.cedarsoftware.ncube.exception.CommandCellException
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.exception.RuleJump
import com.cedarsoftware.ncube.exception.RuleStop
import com.cedarsoftware.ncube.formatters.NCubeTestReader
import com.cedarsoftware.ncube.formatters.NCubeTestWriter
import com.cedarsoftware.ncube.formatters.TestResultsFormatter
import com.cedarsoftware.service.ncube.NCubeService
import com.cedarsoftware.servlet.JsonCommandServlet
import com.cedarsoftware.util.ArrayUtilities
import com.cedarsoftware.util.CaseInsensitiveMap
import com.cedarsoftware.util.CaseInsensitiveSet
import com.cedarsoftware.util.Converter
import com.cedarsoftware.util.InetAddressUtilities
import com.cedarsoftware.util.PoolInterceptor
import com.cedarsoftware.util.StringUtilities
import com.cedarsoftware.util.ThreadAwarePrintStream
import com.cedarsoftware.util.ThreadAwarePrintStreamErr
import com.cedarsoftware.util.Visualizer
import com.cedarsoftware.util.io.JsonReader
import com.cedarsoftware.util.io.JsonWriter
import com.google.common.util.concurrent.AtomicDouble
import groovy.transform.CompileStatic
import net.spy.memcached.AddrUtil
import net.spy.memcached.MemcachedClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import javax.management.MBeanServer
import javax.management.ObjectName
import javax.servlet.http.HttpServletRequest
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.regex.Pattern

/**
 * NCubeController API.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License")
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
@CompileStatic
class NCubeController extends BaseController
{
    private static final Logger LOG = LogManager.getLogger(NCubeController.class)
    private static final Pattern IS_NUMBER_REGEX = ~/^[\d,.e+-]+$/
    private static final Pattern NO_QUOTES_REGEX = ~/"/
    private NCubeService nCubeService
    private static String servletHostname = null
    private static String inetHostname = null
    private static AtomicDouble processLoadPeak = new AtomicDouble(0.0d)
    private static AtomicDouble systemLoadPeak = new AtomicDouble(0.0d)
    private MemcachedClient memcachedClient

    private static final ConcurrentMap<String, ConcurrentSkipListSet<String>> appCache = new ConcurrentHashMap<>()
    private static final ConcurrentMap<String, ConcurrentSkipListSet<String>> appVersions = new ConcurrentHashMap<>()
    private static final ConcurrentMap<String, ConcurrentSkipListSet<String>> appBranches = new ConcurrentHashMap<>()

    NCubeController(NCubeService service, String memcachedServers)
    {
        nCubeService = service;
        System.err = new ThreadAwarePrintStreamErr()
        System.out = new ThreadAwarePrintStream()
//        memcachedClient = new MemcachedClient(AddrUtil.getAddresses(memcachedServers))
    }

    protected static String getUserForDatabase()
    {
        String user = null
        HttpServletRequest request = JsonCommandServlet.servletRequest.get()
        Enumeration e = request.headerNames
        while (e.hasMoreElements())
        {
            String headerName = (String) e.nextElement()
            if ("smuser".equalsIgnoreCase(headerName))
            {
                user = request.getHeader(headerName)
                break;
            }
        }

        if (StringUtilities.isEmpty(user))
        {
            user = System.getProperty("user.name")
        }

        NCubeManager.setUserId(user)
        return user
    }

    boolean checkPermissions(ApplicationID appId, String resource, String action)
    {
        try
        {
            appId = addTenant(appId)
            return nCubeService.checkPermissions(appId, resource, action == null ? ACTION.READ : ACTION.valueOf(action.toUpperCase()))
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    boolean isAppAdmin(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            return nCubeService.isAdmin(appId)
        }
        catch (Exception e)
        {
            fail(e)
            return false
        }
    }

    // ============================================= Begin API =========================================================

    String getAppLockedBy(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            return nCubeService.getAppLockedBy(appId)
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    boolean isAppLocked(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            String lockedBy = nCubeService.getAppLockedBy(appId)
            return lockedBy != null
        }
        catch (Exception e)
        {
            fail(e)
            return false
        }
    }

    void lockApp(ApplicationID appId, boolean shouldLock)
    {
        try
        {
            appId = addTenant(appId)
            if (shouldLock)
            {
                nCubeService.lockApp(appId)
            }
            else
            {
                nCubeService.unlockApp(appId)
            }
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    void moveBranch(ApplicationID appId, String newSnapVer)
    {
        try
        {
            appId = addTenant(appId)
            nCubeService.moveBranch(appId, newSnapVer)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    void releaseVersion(ApplicationID appId, String newSnapVer)
    {
        try
        {
            appId = addTenant(appId)
            nCubeService.releaseVersion(appId, newSnapVer)
            clearVersionCache(appId.app)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    Object[] search(ApplicationID appId, String cubeNamePattern, String content, boolean active)
    {
        try
        {
            appId = addTenant(appId)
            Map options = [:]
            if (active)
            {
                options[(NCubeManager.SEARCH_ACTIVE_RECORDS_ONLY)] = true
            }
            else
            {
                options[(NCubeManager.SEARCH_DELETED_RECORDS_ONLY)] = true
            }

            List<NCubeInfoDto> cubeInfos = nCubeService.search(appId, cubeNamePattern, content, options)

            Collections.sort(cubeInfos, new Comparator<NCubeInfoDto>() {
                public int compare(NCubeInfoDto info1, NCubeInfoDto info2)
                {
                    return info1.name.compareToIgnoreCase(info2.name)
                }
            })

            return cubeInfos.toArray()
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    int getSearchCount(ApplicationID appId, String cubeNamePattern = null, String content = null, boolean active = true)
    {
        return search(appId, cubeNamePattern, content, active).length
    }

    void restoreCubes(ApplicationID appId, Object[] cubeNames)
    {
        try
        {
            appId = addTenant(appId)
            nCubeService.restoreCubes(appId, cubeNames)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    Object[] getRevisionHistory(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)
            List<NCubeInfoDto> cubeInfos = nCubeService.getRevisionHistory(appId, cubeName)
            return cubeInfos.toArray()
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    String getHtml(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.loadCube(appId, cubeName)
            // The Strings below are hints to n-cube to tell it which axis to place on top
            String html = toHtmlWithColumnHints(ncube)
            return html
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    private String toHtmlWithColumnHints(NCube ncube)
    {
        ncube.toHtml('trait', 'traits', 'businessDivisionCode', 'bu', 'month', 'months', 'col', 'column', 'cols', 'columns')
    }

    String getJson(ApplicationID appId, String cubeName)
    {
        return getJson(appId, cubeName, [mode:"json-index"])
    }

    String getJson(ApplicationID appId, String cubeName, Map options)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.loadCube(appId, cubeName)
            return formatCube(ncube, options)
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Map getVisualizerJson(ApplicationID appId, Map options)
    {
        try
        {
            String cubeName = options.startCubeName
            if (!cubeName.startsWith(Visualizer.RPM_CLASS))
            {
                throw new IllegalArgumentException('n-cube name must begin with rpm.class, n-cube: ' + cubeName + ', app: ' + appId)
            }
            appId = addTenant(appId)

            Visualizer vis = new Visualizer()
            vis.input = [options:options]
            vis.output = [:]
            vis.ncube = nCubeService.getCube(appId, cubeName)
            return vis.buildGraph()
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    boolean updateCubeMetaProperties(ApplicationID appId, String cubeName, Map<String, Object> newMetaProperties)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.loadCube(appId, cubeName)
            ncube.clearMetaProperties()
            ncube.addMetaProperties(newMetaProperties)
            nCubeService.updateNCube(ncube)
            return true
        }
        catch (Exception e)
        {
            fail(e)
            return false
        }
    }

    Map getCubeMetaProperties(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.loadCube(appId, cubeName)
            return ncube.getMetaProperties()
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    boolean updateAxisMetaProperties(ApplicationID appId, String cubeName, String axisName, Map<String, Object> newMetaProperties)
    {
        try
        {
            appId = addTenant(appId)
            String resourceName = cubeName + '/' + axisName
            nCubeService.assertPermissions(appId, resourceName, ACTION.UPDATE)
            NCube ncube = nCubeService.loadCube(appId, cubeName)
            Axis axis = ncube.getAxis(axisName)
            axis.clearMetaProperties()
            axis.addMetaProperties(newMetaProperties)
            ncube.clearSha1()
            nCubeService.updateNCube(ncube)
            return true
        }
        catch (Exception e)
        {
            fail(e)
            return false
        }
    }

    Map getAxisMetaProperties(ApplicationID appId, String cubeName, String axisName)
    {
        try
        {
            appId = addTenant(appId)
            String resourceName = cubeName + '/' +axisName
            nCubeService.assertPermissions(appId, resourceName, null)
            NCube ncube = nCubeService.loadCube(appId, cubeName)
            Axis axis = ncube.getAxis(axisName)
            return axis.getMetaProperties()
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    boolean updateColumnMetaProperties(ApplicationID appId, String cubeName, String axisName, long colId, Map<String, Object> newMetaProperties)
    {
        try
        {
            appId = addTenant(appId)
            String resourceName = cubeName + '/' +axisName
            nCubeService.assertPermissions(appId, resourceName, ACTION.UPDATE)
            NCube ncube = nCubeService.loadCube(appId, cubeName)
            Axis axis = ncube.getAxis(axisName)
            Column column = axis.getColumnById(colId)
            column.clearMetaProperties()
            column.addMetaProperties(newMetaProperties)
            ncube.clearSha1()
            nCubeService.updateNCube(ncube)
            return true
        }
        catch (Exception e)
        {
            fail(e)
            return false
        }
    }

    Map getColumnMetaProperties(ApplicationID appId, String cubeName, String axisName, long colId)
    {
        try
        {
            appId = addTenant(appId)
            String resourceName = cubeName + '/' +axisName
            nCubeService.assertPermissions(appId, resourceName, null)
            NCube ncube = nCubeService.loadCube(appId, cubeName)
            Axis axis = ncube.getAxis(axisName)
            Column col = axis.getColumnById(colId)
            return col.getMetaProperties()
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    // @Deprecated
    // TODO: Remove
    Object[] getAppNames(String unused1, String unused2)
    {
        return getAppNames()
    }

    // TODO: Filter APP names by Access Control List data
    Object[] getAppNames()
    {
        try
        {
            // TODO: Snag tenant based on authentication
            String tenant = "NONE";
            ApplicationID.validateTenant(tenant)
            Object[] apps = getCachedApps(tenant)

            if (apps.length > 0)
            {
                return apps
            }

            List<String> appNames = nCubeService.getAppNames(tenant)
            if (appNames.size() == 0) {
                ApplicationID defaultAppId = new ApplicationID(tenant, ApplicationID.DEFAULT_APP, '1.0.0', ReleaseStatus.SNAPSHOT.toString(), 'DEFAULT_BRANCH')
                createCube(defaultAppId, 'defaultNewAppCube')
                clearVersionCache(defaultAppId.app)
            }
            addAllToAppCache(tenant, appNames)
            return getCachedApps(tenant)
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Object[] getAppVersions(String app)
    {
        getAppVersions(app, null)
    }

    Object[] getAppVersions(String app, String status)
    {
        try
        {
            Object[] vers = getVersions(app)
            if (ArrayUtilities.isEmpty(vers))
            {
                return vers
            }

            // Filter out duplicates using Set, order by VersionComparator, remove trailing '-SNAPSHOT' and '-RELEASE'
            Set<String> versions = new TreeSet<>(new VersionComparator())
            for (int i = 0; i < vers.length; i++)
            {
                String mvnVer = vers[i] as String
                String[] verArr = mvnVer.split('-')
                if (status == null || verArr[1] == status)
                {
                    versions.add(verArr[0])
                }
            }
            return versions.toArray()
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Object[] getVersions(String app)
    {
        try
        {
            Object[] appVers = getCachedVersions(app)
            if (appVers.length > 0)
            {   // return from cache
                return appVers
            }

            Map<String, List<String>> versionMap = nCubeService.getVersions(getTenant(), app)
            addAllToVersionCache(app, versionMap.RELEASE, '-RELEASE')
            addAllToVersionCache(app, versionMap.SNAPSHOT, '-SNAPSHOT')
            return getCachedVersions(app)
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    /**
     * App cache Management
     */
    private Object[] getCachedApps(String tenant)
    {
        return getAppCache(tenant).toArray()
    }

    private void addToAppCache(String tenant, String appName)
    {
        getAppCache(tenant).add(appName)
    }

    private void addAllToAppCache(String tenant, List<String> appNames)
    {
        getAppCache(tenant).addAll(appNames)
    }

    private void clearAppCache(String tenant)
    {
        getAppCache(tenant).clear()
    }

    private Set<String> getAppCache(String tenant)
    {
        tenant = tenant.toLowerCase()
        ConcurrentSkipListSet apps = new ConcurrentSkipListSet<>(new Comparator() {
            int compare(Object o1, Object o2) {
                return (o1 as String).compareToIgnoreCase(o2 as String)
            }
        })
        ConcurrentSkipListSet appsRef = appCache.putIfAbsent(tenant, apps)
        if (appsRef != null)
        {
            apps = appsRef
        }
        return apps
    }

    /**
     * Versions Cache Management
     */
    private Object[] getCachedVersions(String app)
    {
        return getVersionsCache(app).toArray()
    }

    private static void clearVersionCache(String app)
    {
        getVersionsCache(app).clear()
    }

    private static void addToVersionsCache(ApplicationID appId)
    {
        getVersionsCache(appId.app).add(appId.version + '-' + appId.status)
    }

    private static void addAllToVersionCache(String app, List<String> versions, String suffix)
    {
        Set<String> set = getVersionsCache(app)
        for (String version : versions)
        {
            set.add(version + suffix)
        }
    }

    private static Set<String> getVersionsCache(String app)
    {
        ConcurrentSkipListSet<String> versions = appVersions[app]
        if (versions == null)
        {
            versions = new ConcurrentSkipListSet<>(new VersionComparator())
            ConcurrentSkipListSet versionsRef = appVersions.putIfAbsent(app, versions)
            if (versionsRef != null)
            {
                versions = versionsRef
            }
        }
        return versions
    }

    /**
     * Version number Comparator that compares Strings with version number - status like
     * 1.0.1-RELEASE to 1.2.0-SNAPSHOT.  The numeric portion takes priority, however, if
     * the numeric portion is equal, then RELEASE comes before SNAPSHOT.
     * The version number components are compared numerically, not alphabetically.
     */
    static class VersionComparator implements Comparator<String>
    {
        int compare(String s1, String s2)
        {
            long v1 = ApplicationID.getVersionValue(s1)
            long v2 = ApplicationID.getVersionValue(s2)
            long diff = v2 - v1    // Reverse order (high revisions will show first)
            if (diff != 0)
            {
                return diff
            }
            return s1.compareToIgnoreCase(s2)
        }
    }

    /**
     * Branch cache management
     */
    private static Object[] getBranchesFromCache(ApplicationID appId)
    {
        return getBranchCache(getBranchCacheKey(appId)).toArray()
    }

    private static void addBranchToCache(ApplicationID appId)
    {
        getBranchCache(getBranchCacheKey(appId)).add(appId.branch)
    }

    private static void addBranchesToCache(ApplicationID appId, Collection<String> branches)
    {
        getBranchCache(getBranchCacheKey(appId)).addAll(branches)
    }

    private static void removeBranchFromCache(ApplicationID appId)
    {
        getBranchCache(getBranchCacheKey(appId)).remove(appId.branch)
    }

    private static clearBranchCache(ApplicationID appId)
    {
        getBranchCache(getBranchCacheKey(appId)).clear()
    }

    private static Set<String> getBranchCache(String key)
    {
        ConcurrentSkipListSet<String> set = appBranches[key]
        if (set == null)
        {
            set = new ConcurrentSkipListSet<>(new BranchComparator())
            ConcurrentSkipListSet setRef = appBranches.putIfAbsent(key, set)
            if (setRef != null)
            {
                set = setRef
            }
        }
        return set
    }

    private static String getBranchCacheKey(ApplicationID appId)
    {
        return appId.tenant + '/' + appId.app + '/' + appId.version + '/' + appId.status
    }

    /**
     * Comparator for comparing branches, which places 'HEAD' always first.
     */
    static class BranchComparator implements Comparator<String>
    {
        int compare(String s1, String s2)
        {
            boolean s1IsHead = ApplicationID.HEAD.equalsIgnoreCase(s1)
            boolean s2IsHead = ApplicationID.HEAD.equalsIgnoreCase(s2)
            if (s1IsHead && !s2IsHead)
                return -1
            if (!s1IsHead && s2IsHead)
                return 1
            if (s1IsHead && s2IsHead)
                return 0

            if (s1.equalsIgnoreCase(s2))
            {
                return s1.compareTo(s2)
            }
            return s1.compareToIgnoreCase(s2)
        }
    }

    /**
     * Create an n-cube (SNAPSHOT only).
     */
    void createCube(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)

            addToAppCache(appId.tenant, appId.app)
            addToVersionsCache(appId)

            NCube ncube = new NCube(cubeName)
            Axis cols = new Axis("Column", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
            cols.addColumn("A")
            cols.addColumn("B")
            cols.addColumn("C")
            cols.addColumn("D")
            cols.addColumn("E")
            cols.addColumn("F")
            cols.addColumn("G")
            cols.addColumn("H")
            cols.addColumn("I")
            cols.addColumn("J")
            Axis rows = new Axis("Row", AxisType.DISCRETE, AxisValueType.LONG, false, Axis.DISPLAY, 2)
            rows.addColumn(1)
            rows.addColumn(2)
            rows.addColumn(3)
            rows.addColumn(4)
            rows.addColumn(5)
            rows.addColumn(6)
            rows.addColumn(7)
            rows.addColumn(8)
            rows.addColumn(9)
            rows.addColumn(10)
            ncube.addAxis(cols)
            ncube.addAxis(rows)
            nCubeService.createCube(appId, ncube)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    /**
     * Delete an n-cube (SNAPSHOT only).
     * @return boolean true if successful, otherwise a String error message.
     */
    boolean deleteCubes(ApplicationID appId, Object[] cubeNames)
    {
        try
        {
            if (ArrayUtilities.isEmpty(cubeNames))
            {
                throw new IllegalArgumentException('Must send at least one cube name')
            }
            appId = addTenant(appId)

            if (!nCubeService.deleteCubes(appId, cubeNames))
            {
                markRequestFailed("Cannot delete RELEASE n-cube.")
            }
            return true
        }
        catch (Exception e)
        {
            fail(e)
            return false
        }
    }

    /**
     * Find all references from (out going) an n-cube.
     * @return Object[] of String cube names that the passed in (named) cube references,
     * otherwise a String error message.
     */
    Object[] getReferencesFrom(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)
            Set<String> references = new CaseInsensitiveSet<>()
            nCubeService.getReferencedCubeNames(appId, cubeName, references)
            Object[] refs = references.toArray()
            caseInsensitiveSort(refs)
            return refs
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    /**
     * Find all referenced input variables for a given n-cube (and through any n-cubes it
     * references).
     * @return Object[] of String names of each scope variable, otherwise a String error message.
     */
    Object[] getRequiredScope(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.getCube(appId, cubeName)
            Set<String> refs = ncube.getRequiredScope([:], [:])
            Object[] scopeKeys = refs.toArray()
            caseInsensitiveSort(scopeKeys)
            return scopeKeys
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    /**
     * Duplicate the passed in cube, but change the name to newName AND the status of the new
     * n-cube will be SNAPSHOT.
     */
    void duplicateCube(ApplicationID appId, ApplicationID destAppId, String cubeName, String newName)
    {
        try
        {
            appId = addTenant(appId)
            destAppId = addTenant(destAppId)

            addToAppCache(appId.tenant, appId.app)
            addToAppCache(destAppId.tenant, destAppId.app)
            addToVersionsCache(appId)
            addToVersionsCache(destAppId)

            nCubeService.duplicateCube(appId, destAppId, cubeName, newName)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    /**
     * Release the passed in SNAPSHOT version (update their status_cd to RELEASE), and then
     * duplicate all the n-cubes in the release, creating new ones in SNAPSHOT status with
     * the version number set to the newSnapVer.
     */
    void releaseCubes(ApplicationID appId, String newSnapVer)
    {
        try
        {
            appId = addTenant(appId)
            nCubeService.releaseCubes(appId, newSnapVer)
            clearVersionCache(appId.app)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    /**
     * Change the SNAPSHOT version number of an n-cube.
     */
    void changeVersionValue(ApplicationID appId, String newSnapVer)
    {
        try
        {
            appId = addTenant(appId)
            nCubeService.changeVersionValue(appId, newSnapVer)
            clearVersionCache(appId.app)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    /**
     * Add axis to an existing SNAPSHOT n-cube.
     */
    void addAxis(ApplicationID appId, String cubeName, String axisName, String type, String valueType)
    {
        try
        {
            appId = addTenant(appId)
            String resourceName = cubeName + '/' + axisName
            nCubeService.assertPermissions(appId, resourceName, ACTION.UPDATE)
            nCubeService.addAxis(appId, cubeName, axisName, type, valueType)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    /**
     * Add axis to an existing SNAPSHOT n-cube that is a reference to an axis in another cube.
     */
    void addAxis(ApplicationID appId, String cubeName, String axisName, ApplicationID refAppId, String refCubeName, String refAxisName, ApplicationID transformAppId, String transformCubeName, String transformMethodName)
    {
        try
        {
            appId = addTenant(appId)
            nCubeService.addAxis(appId, cubeName, axisName, refAppId, refCubeName, refAxisName, transformAppId, transformCubeName, transformMethodName)
        }
        catch (Exception e)
        {
            fail(e)
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
     * senses the data-type mismatch (json-io does) and then attempts to convert the String to a
     * numeric value (which succeeds).  This allows the full 64-bit id to make it round trip.
     */
    Map getAxis(ApplicationID appId, String cubeName, String axisName)
    {
        try
        {
            appId = addTenant(appId)
            String resourceName = cubeName + '/' +axisName
            nCubeService.assertPermissions(appId, resourceName, null)
            NCube ncube = nCubeService.loadCube(appId, cubeName)
            Axis axis = ncube.getAxis(axisName)
            return convertAxis(axis)
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    /**
     * Delete the passed in axis.
     */
    void deleteAxis(ApplicationID appId, String cubeName, String axisName)
    {
        try
        {
            appId = addTenant(appId)
            String resourceName = cubeName + '/' +axisName
            nCubeService.assertPermissions(appId, resourceName, ACTION.UPDATE)
            nCubeService.deleteAxis(appId, cubeName, axisName)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    void updateAxis(ApplicationID appId, String cubeName, String origAxisName, String axisName, boolean hasDefault, boolean isSorted, boolean fireAll)
    {
        try
        {
            appId = addTenant(appId)
            String resourceName = cubeName + '/' + origAxisName
            nCubeService.assertPermissions(appId, resourceName, ACTION.UPDATE)
            resourceName = cubeName + '/' + axisName
            nCubeService.assertPermissions(appId, resourceName, ACTION.UPDATE)
            nCubeService.updateAxis(appId, cubeName, origAxisName, axisName, hasDefault, isSorted, fireAll)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    /**
     * Update an entire set of columns on an axis at one time.  The updatedAxis is not a real axis,
     * but treated like an Axis-DTO where the list of columns within the axis are in display order.
     */
    void updateAxisColumns(ApplicationID appId, String cubeName, String axisName, Object[] cols)
    {
        try
        {
            appId = addTenant(appId)
            String resourceName = cubeName + '/' + axisName
            nCubeService.assertPermissions(appId, resourceName, ACTION.UPDATE)
            Set<Column> columns = new LinkedHashSet<>()

            if (cols != null)
            {
                cols.each {
                    Column column ->
                        Object value = column.value
                        if (value == null || "".equals(value))
                        {
                            throw new IllegalArgumentException('Column cannot have empty value, n-cube: ' + cubeName + ', axis: ' + axisName)
                        }
                        columns.add(column)
                }
            }

            NCube ncube = nCubeService.loadCube(appId, cubeName)
            ncube.updateColumns(axisName, columns)
            nCubeService.updateNCube(ncube)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    void breakAxisReference(ApplicationID appId, String cubeName, String axisName)
    {
        try
        {
            appId = addTenant(appId)
            String resourceName = cubeName + '/' + axisName
            nCubeService.assertPermissions(appId, resourceName, ACTION.UPDATE)
            nCubeService.breakAxisReference(appId, cubeName, axisName)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    void renameCube(ApplicationID appId, String oldName, String newName)
    {
        try
        {
            appId = addTenant(appId)
            nCubeService.renameCube(appId, oldName, newName)
        }
        catch(Exception e)
        {
            fail(e)
        }
    }

    void promoteRevision(ApplicationID appId, long cubeId)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.loadCubeById(cubeId)
            saveJson(appId, ncube.getName(), ncube.toFormattedJson())
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    void saveJson(ApplicationID appId, String cubeName, String json)
    {
        try
        {
            appId = addTenant(appId)
            nCubeService.updateCube(appId, cubeName, json)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    Map runTest(ApplicationID appId, String cubeName, NCubeTest test)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.getCube(appId, cubeName)
            Map<String, Object> coord = test.getCoordWithValues()
            boolean success = true
            Map output = new LinkedHashMap()
            Map args = [input:coord, output:output, ncube:ncube]
            Map<String, Object> copy = new LinkedHashMap(coord)

            // If any of the input values are a CommandCell, execute them.  Use the fellow (same) input as input.
            // In other words, other key/value pairs on the input map can be referenced in a CommandCell.
            copy.each { key, value ->
                if (value instanceof CommandCell)
                {
                    CommandCell cmd = (CommandCell) value
                    coord[key] = cmd.execute(args)
                }
            }

            Set<String> errors = new LinkedHashSet<>()
            ncube.getCell(coord, output)               // Execute test case

            RuleInfo ruleInfoMain = (RuleInfo) output[(NCube.RULE_EXEC_INFO)]
            ruleInfoMain.setSystemOut(ThreadAwarePrintStream.getContent())
            ruleInfoMain.setSystemErr(ThreadAwarePrintStreamErr.getContent())

            List<GroovyExpression> assertions = test.createAssertions()
            int i = 0;

            for (GroovyExpression exp : assertions)
            {
                i++

                try
                {
                    Map assertionOutput = new LinkedHashMap<>(output)
                    RuleInfo ruleInfo = new RuleInfo()
                    assertionOutput[(NCube.RULE_EXEC_INFO)] = ruleInfo
                    args.output = assertionOutput
                    if (!NCube.isTrue(exp.execute(args)))
                    {
                        errors.add('[assertion ' + i + ' failed]: ' + exp.getCmd())
                        success = false
                    }
                }
                catch (Exception e)
                {
                    errors.add('[exception]')
                    errors.add('\n')
                    errors.add(getTestCauses(e))
                    success = false
                }
            }

            ruleInfoMain.setAssertionFailures(errors)
            return ['_message': new TestResultsFormatter(output).format(), '_result' : success]
        }
        catch(Exception e)
        {
            markRequestFailed(getTestCauses(e))
            ThreadAwarePrintStream.getContent()
            ThreadAwarePrintStreamErr.getContent()
            return null
        }
    }

    Object[] getTests(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)
            String s = nCubeService.getTestData(appId, cubeName)
            if (StringUtilities.isEmpty(s))
            {
                return null
            }
            List<NCubeTest> tests = NCubeTestReader.convert(s)
            return tests.toArray()
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    void saveTests(ApplicationID appId, String cubeName, Object[] tests)
    {
        try
        {
            appId = addTenant(appId)
            String data = new NCubeTestWriter().format(tests)
            nCubeService.updateTestData(appId, cubeName, data)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    /**
     * In-place update of a cell.  'Value' is the final (converted) object type to be stored
     * in the indicated (by colIds) cell.
     */
    NCubeTest createNewTest(ApplicationID appId, String cubeName, String testName)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.getCube(appId, cubeName)

            if (StringUtilities.isEmpty(testName))
            {
                throw new IllegalArgumentException("Test name cannot be empty, cube: " + cubeName + ", app: " + appId)
            }

            Set<String> items = ncube.getRequiredScope([:], [:])
            int size = items == null ? 0 : items.size()

            Map<String, CellInfo> coords = new CaseInsensitiveMap<>()
            if (size > 0)
            {
                for (String s : items)
                {
                    coords[s] = (CellInfo)null
                }
            }

            CellInfo[] assertions = [ new CellInfo("exp", "output.return", false, false) ] as CellInfo[]
            NCubeTest test = new NCubeTest(testName, coords, assertions)
            return test
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    /**
     * In-place update of a cell.
     */
    boolean updateCell(ApplicationID appId, String cubeName, Object[] ids, CellInfo cellInfo)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.getCube(appId, cubeName)
            Set<Long> colIds = getCoordinate(ids)

            if (cellInfo == null)
            {
                ncube.removeCellById(colIds)
            }
            else
            {
                ncube.setCellById(cellInfo.recreate(), colIds)
            }
            nCubeService.updateNCube(ncube)
            return true
        }
        catch(Exception e)
        {
            fail(e)
            return false
        }
    }

    boolean updateCellAt(ApplicationID appId, String cubeName, Map coordinate, CellInfo cellInfo)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.getCube(appId, cubeName)

            if (cellInfo == null)
            {
                ncube.removeCell(coordinate)
            }
            else
            {
                ncube.setCell(cellInfo.recreate(), coordinate)
            }
            nCubeService.updateNCube(ncube)
            return true
        }
        catch(Exception e)
        {
            fail(e)
            return false
        }
    }

    Object getCellNoExecute(ApplicationID appId, String cubeName, Object[] ids)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.getCube(appId, cubeName)
            Set<Long> colIds = getCoordinate(ids)
            Object cell = ncube.getCellByIdNoExecute(colIds)

            CellInfo cellInfo = new CellInfo(cell)
            cellInfo.collapseToUiSupportedTypes()
            return cellInfo
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    /**
     * This API will fetch particular cell values (identified by the idArrays) for the passed
     * in appId and named cube.  The idArrays is an Object[] of Object[]'s:<pre>
     * [
     *  [1, 2, 3],
     *  [4, 5, 6],
     *  [7, 8, 9],
     *   ...
     *]
     * In the example above, the 1st entry [1, 2, 3] identifies the 1st cell to fetch.  The 2nd entry [4, 5, 6]
     * identifies the 2nd cell to fetch, and so on.
     * </pre>
     * @return Object[] The return value is an Object[] containing Object[]'s with the original coordinate
     *  as the first entry and the cell value as the 2nd entry:<pre>
     * [
     *  [[1, 2, 3], {"type":"int", "value":75}],
     *  [[4, 5, 6], {"type":"exp", "cache":false, "value":"return 25"}],
     *  [[7, 8, 9], {"type":"string", "value":"hello"}],
     *   ...
     * ]
     * </pre>
     */
    Object[] getCellsNoExecute(ApplicationID appId, String cubeName, Object[] idArrays)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.getCube(appId, cubeName)
            Object[] ret = new Object[idArrays.length]
            Set key = new HashSet()
            int idx = 0

            for (coord in idArrays)
            {
                for (item in coord)
                {
                    key.add(Converter.convert(item, Long.class))
                }
                CellInfo cellInfo = new CellInfo(ncube.getCellByIdNoExecute(key))
                cellInfo.collapseToUiSupportedTypes()
                ret[idx++] = [coord, cellInfo as Map]
                key.clear()
            }

            return ret
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Map getCellCoordinate(ApplicationID appId, String cubeName, Object[] ids)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.getCube(appId, cubeName)
            Set<Long> colIds = getCoordinate(ids)
            Map<String, Comparable> coord = ncube.getDisplayCoordinateFromIds(colIds)
            Map<String, Comparable> niceCoord = [:]
            coord.each {
                k, v ->
                    niceCoord[k] = CellInfo.formatForDisplay(v)
            }
            return niceCoord
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    String copyCells(ApplicationID appId, String cubeName, Object[] ids, boolean isCut)
    {
        try
        {
            appId = addTenant(appId)
            if (ids == null || ids.length == 0)
            {
                markRequestFailed("No IDs of cells to cut/clear were given.")
                return null
            }

            NCube ncube = nCubeService.loadCube(appId, cubeName)
            List<Object[]> cells = new ArrayList<>()

            for (Object id : ids)
            {
                Object[] cellId = (Object[]) id;
                if (ArrayUtilities.isEmpty(cellId))
                {
                    cells.add(null)
                    continue;
                }
                Set<Long> colIds = getCoordinate(cellId)
                Object content = ncube.getCellByIdNoExecute(colIds)
                CellInfo cellInfo = new CellInfo(content)
                cells.add([cellInfo.value, cellInfo.dataType, cellInfo.isUrl, cellInfo.isCached] as Object[])

                if (isCut)
                {
                    ncube.removeCellById(colIds)
                }
            }

            if (isCut)
            {
                nCubeService.updateNCube(ncube)
            }
            return JsonWriter.objectToJson(cells.toArray())
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    boolean pasteCellsNce(ApplicationID appId, String cubeName, Object[] clipboard)
    {
        try
        {
            if (ArrayUtilities.isEmpty(clipboard))
            {
                markRequestFailed("Could not paste cells, no data available on clipboard.")
                return false
            }

            NCube ncube = nCubeService.loadCube(appId, cubeName)
            if (ncube == null)
            {
                markRequestFailed("Could not paste cells, cube: " + cubeName + " not found for app: " + appId)
                return false
            }

            int len = clipboard.length;
            for (int i=0; i < len; i++)
            {
                Object[] cell = clipboard[i] as Object[]
                if (ArrayUtilities.isEmpty(cell))
                {   // null is EOL marker
                    continue
                }

                Object lastElem = cell[cell.length - 1]

                if (lastElem instanceof Object[])
                {   // If last element is an Object[], we have a coordinate (destination cell)
                    Object[] ids = lastElem as Object[]
                    Set<Long> cellId = getCoordinate(ids)
                    CellInfo info = new CellInfo(cell[1] as String, cell[0] as String, cell[2], cell[3])
                    Object value = info.recreate()
                    if (value == null)
                    {
                        ncube.removeCellById(cellId)
                    }
                    else
                    {
                        ncube.setCellById(value, cellId)
                    }
                }
            }
            nCubeService.updateNCube(ncube)
            return true
        }
        catch (Exception e)
        {
            fail(e)
            return false
        }
    }

    boolean pasteCells(ApplicationID appId, String cubeName, Object[] values, Object[] coords)
    {
        try
        {
            if (values == null || values.length == 0 || coords == null || coords.length == 0)
            {
                markRequestFailed("Could not paste cells, values and coordinates must not be empty or length of 0.")
                return false
            }

            NCube ncube = nCubeService.loadCube(appId, cubeName)
            if (ncube == null)
            {
                markRequestFailed("Could not paste cells, cube: " + cubeName + " not found for app: " + appId)
                return false
            }

            for (int i=0; i < coords.length; i++)
            {
                Object[] row = (Object[]) coords[i]
                if (ArrayUtilities.isEmpty(row))
                {
                    break
                }

                for (int j=0; j < row.length; j++)
                {
                    Object[] ids = (Object[]) row[j]
                    Set<Long> cellId = getCoordinate(ids)
                    Object value = convertStringToValue(getValueRepeatIfNecessary(values, i, j))
                    if (value == null)
                    {
                        ncube.removeCellById(cellId)
                    }
                    else
                    {
                        ncube.setCellById(value, cellId)
                    }
                }
            }
            nCubeService.updateNCube(ncube)
            return true
        }
        catch (Exception e)
        {
            fail(e)
            return false
        }
    }

    String resolveRelativeUrl(ApplicationID appId, String relativeUrl)
    {
        try
        {
            appId = addTenant(appId)
            URL absUrl = nCubeService.resolveRelativeUrl(appId, relativeUrl)
            if (absUrl == null)
            {
                throw new IllegalStateException('Unable to resolve the relative URL (' + relativeUrl + ') to a physical URL, app: ' + appId)
            }
            return absUrl
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    void clearCache(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            if (!nCubeService.isAdmin(appId))
            {
                return
            }
            nCubeService.clearCache(appId)
            clearAppCache(appId.tenant)
            clearVersionCache(appId.app)
            clearBranchCache(appId)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    void createBranch(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            nCubeService.copyBranch(appId.asHead(), appId)
            addBranchToCache(appId)
            if (appId.version != '0.0.0') {
                addBranchToCache(appId.asVersion('0.0.0'));
            }
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    void copyBranch(ApplicationID srcAppId, ApplicationID targetAppId)
    {
        try
        {
            srcAppId = addTenant(srcAppId)
            targetAppId = addTenant(targetAppId)
            nCubeService.copyBranch(srcAppId, targetAppId)
            if (ArrayUtilities.size(getCachedApps(tenant)) > 0)
            {
                addToAppCache(targetAppId.tenant, targetAppId.app)
            }
            if (getVersionsCache(targetAppId.app).size() != 0)
            {
                addToVersionsCache(targetAppId)
            }
            if ( getBranchCache(getBranchCacheKey(targetAppId)).size() != 0)
            {
                addBranchToCache(targetAppId)
            }
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    Object[] getBranches(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            Object[] branches = getBranchesFromCache(appId)
            if (branches.length > 0)
            {
                return branches
            }

            Set<String> realBranches = nCubeService.getBranches(appId)
            realBranches.add(ApplicationID.HEAD)
            clearBranchCache(appId)
            addBranchesToCache(appId, realBranches)
            return getBranchesFromCache(appId)
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    int getBranchCount(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            // Run against database as this is used to verify live record counts
            return nCubeService.getBranchCount(appId)
        }
        catch (Exception e)
        {
            fail(e)
            return 0
        }
    }

    Object[] getBranchChanges(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            List<NCubeInfoDto> branchChanges = nCubeService.getBranchChanges(appId, ApplicationID.HEAD)
            return branchChanges as Object[]
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Object[] getBranchChangesFromBranch(ApplicationID appId, String branchName)
    {
        try
        {
            //TODO change to new manager method
            appId = addTenant(appId)
            List<NCubeInfoDto> branchChanges = nCubeService.getBranchChanges(appId, branchName)
            return branchChanges.toArray()
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Object commitCube(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)
            Map options = [:]
            options[(NCubeManager.SEARCH_EXACT_MATCH_NAME)] = true
            options[(NCubeManager.SEARCH_ACTIVE_RECORDS_ONLY)] = true
            List<NCubeInfoDto> list = nCubeService.search(appId, cubeName, null, options)
            List<NCubeInfoDto> committedCubes = nCubeService.commitBranch(appId, list.toArray())
            return committedCubes.toArray()
        }
        catch (BranchMergeException e)
        {
            markRequestFailed(e.getMessage())
            return e.getErrors()
        }
        catch (Exception e)
        {
            fail(e)
            return [:]
        }
    }

    Object commitBranch(ApplicationID appId, Object[] infoDtos)
    {
        try
        {
            appId = addTenant(appId)
            List<NCubeInfoDto> committedCubes = nCubeService.commitBranch(appId, infoDtos)
            return committedCubes.toArray()
        }
        catch (BranchMergeException e)
        {
            markRequestFailed(e.getMessage())
            return e.getErrors()
        }
        catch (Exception e)
        {
            fail(e)
            return [:]
        }
    }

    int rollbackBranch(ApplicationID appId, Object[] cubeNames)
    {
        try
        {
            appId = addTenant(appId)
            return nCubeService.rollbackCubes(appId, cubeNames)
        }
        catch (Exception e)
        {
            fail(e)
            return 0
        }
    }

    Object updateBranch(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            Map<String, Object> result = nCubeService.updateBranch(appId)
            return result
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Object updateBranchCube(ApplicationID appId, String cubeName, String sourceBranch)
    {
        try
        {
            appId = addTenant(appId)
            Map<String, Object> result = nCubeService.updateBranchCube(appId, cubeName, sourceBranch)
            return result
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Object updateBranchCubes(ApplicationID appId, Object[] cubeNames, String sourceBranch)
    {
        try
        {
            appId = addTenant(appId)
            // TODO - add in manager
            throw new IllegalStateException('Functionality not yet implemented.')
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    void deleteBranch(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            nCubeService.deleteBranch(appId)
            removeBranchFromCache(appId)
        }
        catch(Exception e)
        {
            fail(e)
        }
    }

    int acceptTheirs(ApplicationID appId, Object[] cubeNames, Object[] branchSha1)
    {
        try
        {
            appId = addTenant(appId)
            return nCubeService.acceptTheirs(appId, cubeNames, branchSha1)
        }
        catch (Exception e)
        {
            fail(e)
            return 0
        }
    }

    int acceptMine(ApplicationID appId, Object[] cubeNames, Object[] headSha1)
    {
        try
        {
            appId = addTenant(appId)
            return nCubeService.acceptMine(appId, cubeNames)
        }
        catch (Exception e)
        {
            fail(e)
            return 0
        }
    }

    String loadCubeById(ApplicationID appId, long id, String mode)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.loadCubeById(id)
            nCubeService.assertPermissions(appId, ncube.name, ACTION.READ)
            return formatCube(ncube, [mode: mode])
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    /**
     * @return Map of HTTP headers for debugging display.
     */
    Map getHeaders()
    {
        HttpServletRequest request = JsonCommandServlet.servletRequest.get()
        Enumeration e = request.headerNames
        Map<String, String> headers = [:]

        while (e.hasMoreElements())
        {
            String headerName = (String) e.nextElement()
            headers[(headerName)] = request.getHeader(headerName)
        }

        return headers
    }

    Map execute(ApplicationID appId, Map args, String command)
    {
        try
        {
            appId = addTenant(appId)
            int dot = command.indexOf('.')
            String controller = command.substring(0, dot)
            String method = command.substring(dot + 1)
            Map coordinate = ['method' : method, 'service': nCubeService]
            coordinate.putAll(args)
            NCube cube = nCubeService.getCube(appId, controller)
            Map output = [:]
            cube.getCell(coordinate, output)    // return value is set on 'return' key of output Map
            output.remove('_rule')  // remove execution meta information (too big to send - add special API if needed)
            return output
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Map getMenu(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            NCube menuCube = nCubeService.getCube(appId, 'sys.menu')
            return menuCube.getCell([:])
        }
        catch (Exception e)
        {
            LOG.info("Unable to load sys.menu (sys.menu cube likely not in appId: " + appId.toString() + ", exception: " + e.getMessage())
            return ['~Title':'Enterprise Configurator',
                    'n-cube':[html:'html/ntwobe.html',img:'img/letter-n.png'],
                    'n-cube-old':[html:'html/ncube.html',img:'img/letter-o.png'],
                    'JSON':[html:'html/jsonEditor.html',img:'img/letter-j.png'],
                    'Details':[html:'html/details.html',img:'img/letter-d.png'],
                    'Test':[html:'html/test.html',img:'img/letter-t.png'],
                    'Visualizer':[html:'html/visualize.html', img:'img/letter-v.png']
            ]
        }
    }

    Object getDefaultCell(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)
            NCube menuCube = nCubeService.getCube(appId, cubeName)
            CellInfo cellInfo = new CellInfo(menuCube.getDefaultCellValue())
            cellInfo.collapseToUiSupportedTypes()
            return cellInfo
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    boolean clearDefaultCell(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)
            NCube ncube = nCubeService.getCube(appId, cubeName)
            ncube.setDefaultCellValue(null)
            nCubeService.updateNCube(ncube)
            return true
        }
        catch (Exception e)
        {
            fail(e)
            return false
        }
    }

    boolean updateDefaultCell(ApplicationID appId, String cubeName, CellInfo cellInfo)
    {
        try
        {
            appId = addTenant(appId)
            Object cellValue = cellInfo.isUrl ?
                    CellInfo.parseJsonValue(null, cellInfo.value, cellInfo.dataType, cellInfo.isCached) :
                    CellInfo.parseJsonValue(cellInfo.value, null, cellInfo.dataType, cellInfo.isCached)

            NCube ncube = nCubeService.getCube(appId, cubeName)
            ncube.setDefaultCellValue(cellValue)
            nCubeService.updateNCube(ncube)
            return true
        }
        catch (Exception e)
        {
            fail(e)
            return false
        }
    }

    private Map<String, Object> fetchJsonDiffs(NCube leftCube, NCube rightCube)
    {
        Map<String, Object> ret = [left:[''], right:[''], delta:'']
        try
        {
            ret.left = jsonToLines(leftCube.toFormattedJson())
            ret.right = jsonToLines(rightCube.toFormattedJson())
        }
        catch (Exception ignored) { }

        return addDeltaDescription(leftCube, rightCube, ret)
    }

    Map<String, Object> fetchJsonRevDiffs(long cubeId1, long cubeId2)
    {
        try
        {
            NCube leftCube = nCubeService.loadCubeById(cubeId1)
            addTenant(leftCube.getApplicationID())

            NCube rightCube = nCubeService.loadCubeById(cubeId2)
            addTenant(rightCube.getApplicationID())

            return fetchJsonDiffs(leftCube, rightCube)
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Map<String, Object> fetchJsonBranchDiffs(NCubeInfoDto leftInfoDto, NCubeInfoDto rightInfoDto)
    {
        try
        {
            ApplicationID leftAppId = addTenant(leftInfoDto.applicationID)
            ApplicationID rightAppId = addTenant(rightInfoDto.applicationID)
            NCube leftCube = nCubeService.loadCube(leftAppId, leftInfoDto.name)
            NCube rightCube = nCubeService.loadCube(rightAppId, rightInfoDto.name)

            return fetchJsonDiffs(leftCube, rightCube)
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    private Map<String, String> fetchHtmlDiffs(NCube leftCube, NCube rightCube)
    {
        Map<String, String> ret = [leftHtml: '', rightHtml: '']
        try
        {
            ret.leftHtml = toHtmlWithColumnHints(leftCube)
            ret.rightHtml = toHtmlWithColumnHints(rightCube)
        }
        catch (Exception ignored) { }

        return ret
    }

    Map<String, String> fetchHtmlRevDiffs(long cubeId1, long cubeId2)
    {
        try
        {
            NCube leftCube = nCubeService.loadCubeById(cubeId1)
            addTenant(leftCube.getApplicationID())

            NCube rightCube = nCubeService.loadCubeById(cubeId2)
            addTenant(rightCube.getApplicationID())

            return fetchHtmlDiffs(leftCube, rightCube)
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    Map<String, String> fetchHtmlBranchDiffs(NCubeInfoDto leftInfoDto, NCubeInfoDto rightInfoDto)
    {
        try
        {
            ApplicationID leftAppId = addTenant(leftInfoDto.applicationID)
            ApplicationID rightAppId = addTenant(rightInfoDto.applicationID)
            NCube leftCube = nCubeService.loadCube(leftAppId, leftInfoDto.name)
            NCube rightCube = nCubeService.loadCube(rightAppId, rightInfoDto.name)

            return fetchHtmlDiffs(leftCube, rightCube)
        }
        catch (Exception e)
        {
            fail(e)
            return null
        }
    }

    /**
     * Add the n-cube delta description between the two passed in cubes to the passed in Map.
     */
    private static Map<String, Object> addDeltaDescription(NCube leftCube, NCube rightCube, Map<String, Object> ret)
    {
        if (leftCube && rightCube)
        {
            List<Delta> delta = DeltaProcessor.getDeltaDescription(rightCube, leftCube)
            StringBuilder s = new StringBuilder()
            delta.each {
                Delta d ->
                    s.append(d.description)
                    s.append('<br/>')
            }
            ret.delta = s.toString()
        }
        return ret
    }

    List<String> jsonToLines(String json)
    {
        JsonWriter.formatJson(json).readLines()
    }

    Object[] getReferenceAxes(ApplicationID appId)
    {
        try
        {
            appId = addTenant(appId)
            List refAxes = nCubeService.getReferenceAxes(appId)
            return refAxes as Object[]
        }
        catch (Exception e)
        {
            fail(e)
            return [] as Object[]
        }
    }

    void updateReferenceAxes(Object[] axisRefs)
    {
        try
        {
            List<AxisRef> axisRefList = axisRefs as List<AxisRef>
            nCubeService.updateReferenceAxes(axisRefList)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    Map heartBeat(Map openCubes)
    {
        // If remotely accessing server, use the following to get the MBeanServerConnection...
//        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:/jmxrmi")
//        JMXConnector jmxc = JMXConnectorFactory.connect(url, null)
//
//        MBeanServerConnection conn = jmxc.getMBeanServerConnection()
//        String[] domains = conn.getDomains()
//        Set result = conn.queryMBeans(null, "Catalina:type=DataSource,path=/appdb,host=localhost,class=javax.sql.DataSource")
//        jmxc.close()

        Map results = [:]

        // Force session creation / update (only for statistics - we do NOT want to use a session - must...remain...stateless)
        JsonCommandServlet.servletRequest.get().getSession()

        // Snag the platform mbean server (singleton)
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer()

        // App server name and version
        Map serverStats = [:]
        putIfNotNull(serverStats, 'Server Info', getAttribute(mbs, 'Catalina:type=Server', 'serverInfo'))
        putIfNotNull(serverStats, 'Java version', getAttribute(mbs, 'JMImplementation:type=MBeanServerDelegate', 'ImplementationVersion'))
        putIfNotNull(serverStats, 'JVM Route', getAttribute(mbs, 'Catalina:type=Engine', 'jvmRoute'))

        putIfNotNull(serverStats, 'hostname, servlet', getServletHostname())
        putIfNotNull(serverStats, 'hostname, OS', getInetHostname())
        putIfNotNull(serverStats, 'Context', JsonCommandServlet.servletRequest.get().getContextPath())
        putIfNotNull(serverStats, 'Sessions, active', getAttribute(mbs, 'Catalina:type=Manager,host=localhost,context=' + serverStats.Context, 'activeSessions'))
        putIfNotNull(serverStats, 'Sessions, peak', getAttribute(mbs, 'Catalina:type=Manager,host=localhost,context=' + serverStats.Context, 'maxActive'))

        Set<ObjectName> set = mbs.queryNames(new ObjectName('Catalina:type=ThreadPool,name=*'), null)
        Set<String> connectors = [] as LinkedHashSet
        set.each {
            ObjectName objName ->
                connectors << objName.getKeyProperty('name')
        }

        for (String conn : connectors)
        {
            String cleanKey = cleanKey(conn)
            putIfNotNull(serverStats, cleanKey + ' t-pool max', getAttribute(mbs, 'Catalina:type=ThreadPool,name=' + conn, 'maxThreads'))
            putIfNotNull(serverStats, cleanKey + ' t-pool cur', getAttribute(mbs, 'Catalina:type=ThreadPool,name=' + conn, 'currentThreadCount'))
            putIfNotNull(serverStats, cleanKey + ' busy thread', getAttribute(mbs, 'Catalina:type=ThreadPool,name=' + conn, 'currentThreadsBusy'))
            putIfNotNull(serverStats, cleanKey + ' max conn', getAttribute(mbs, 'Catalina:type=ThreadPool,name=' + conn, 'maxConnections'))
            putIfNotNull(serverStats, cleanKey + ' curr conn', getAttribute(mbs, 'Catalina:type=ThreadPool,name=' + conn, 'connectionCount'))
        }

        // OS
        putIfNotNull(serverStats, 'OS', getAttribute(mbs, 'java.lang:type=OperatingSystem', 'Name'))
        putIfNotNull(serverStats, 'OS version', getAttribute(mbs, 'java.lang:type=OperatingSystem', 'Version'))
        putIfNotNull(serverStats, 'CPU', getAttribute(mbs, 'java.lang:type=OperatingSystem', 'Arch'))
        double processLoad = getAttribute(mbs, 'java.lang:type=OperatingSystem', 'ProcessCpuLoad') as Double
        if (processLoad > processLoadPeak.get())
        {
            processLoadPeak.set(processLoad)
        }
        double systemLoad = getAttribute(mbs, 'java.lang:type=OperatingSystem', 'SystemCpuLoad') as Double
        if (systemLoad > systemLoadPeak.get())
        {
            systemLoadPeak.set(systemLoad)
        }
        putIfNotNull(serverStats, 'Process CPU Load', processLoad)
        putIfNotNull(serverStats, 'System CPU Load', systemLoad)
        putIfNotNull(serverStats, 'Peak Process CPU Load', processLoadPeak.get())
        putIfNotNull(serverStats, 'Peak System CPU Load', systemLoadPeak.get())
        putIfNotNull(serverStats, 'CPU Cores', getAttribute(mbs, 'java.lang:type=OperatingSystem', 'AvailableProcessors'))
        double machMem = (long) getAttribute(mbs, 'java.lang:type=OperatingSystem', 'TotalPhysicalMemorySize')
        long K = 1024L
        long MB = K * 1024L
        long GB = MB * 1024L
        machMem = machMem / GB
        putIfNotNull(serverStats, 'Physical Memory', (machMem.round(2)) + ' GB')

        // JVM
        putIfNotNull(serverStats, 'Loaded class count', getAttribute(mbs, 'java.lang:type=ClassLoading', 'LoadedClassCount'))

        // JVM Memory
        Runtime rt = Runtime.getRuntime()
        double maxMem = rt.maxMemory() / MB
        double freeMem = rt.freeMemory() / MB
        double usedMem = maxMem - freeMem
        putIfNotNull(serverStats, 'Heap size (-Xmx)', (maxMem.round(1)) + ' MB')
        putIfNotNull(serverStats, 'Used memory', (usedMem.round(1)) + ' MB')
        putIfNotNull(serverStats, 'Free memory', (freeMem.round(1)) + ' MB')

        putIfNotNull(serverStats, 'JDBC Pool size', PoolInterceptor.size.get())
        putIfNotNull(serverStats, 'JDBC Pool active', PoolInterceptor.active.get())
        putIfNotNull(serverStats, 'JDBC Pool idle', PoolInterceptor.idle.get())

        putIfNotNull(results, 'serverStats', serverStats)

        Map compareResults = [:]
        openCubes.each { key, nothing ->
            if (key != null)
            {
                String cubeId = key.toString()
                String[] pieces = cubeId.split('~')
                if (pieces != null && pieces.length > 4)
                {
                    ApplicationID appId = new ApplicationID("x", pieces[0], pieces[1], pieces[2], pieces[3])
                    appId = addTenant(appId)
                    String cubeName = pieces[4]
                    Object status = nCubeService.getUpToDateStatus(appId, cubeName)
                    putIfNotNull(compareResults, cubeId, status)
                }
            }
        }
        putIfNotNull(results, 'compareResults', compareResults)

        return results
    }

    boolean isCubeUpToDate(ApplicationID appId, String cubeName)
    {
        try
        {
            appId = addTenant(appId)
            return nCubeService.isCubeUpToDate(appId, cubeName)
        }
        catch (Exception e)
        {
            fail(e)
        }
    }

    // ============================================= End API ===========================================================

    // ===================================== utility (non-API) methods =================================================

    private static String cleanKey(String key)
    {
        return key.replace('"','')
    }

    private static void markRequestFailed(Object data)
    {
        JsonCommandServlet.servletRequest.get().setAttribute(JsonCommandServlet.ATTRIBUTE_STATUS, false)
        JsonCommandServlet.servletRequest.get().setAttribute(JsonCommandServlet.ATTRIBUTE_FAIL_MESSAGE, data)
    }

    private static Object getAttribute(MBeanServer mbs, String beanName, String attribute)
    {
        try
        {
            ObjectName objectName = new ObjectName(beanName)
            mbs.getAttribute(objectName, attribute)
        }
        catch (Exception ignored)
        {
//            LOG.info('Unable to fetch attribute: ' + attribute + ' from mbean: ' + beanName)
            null
        }
    }

    private static void putIfNotNull(Map map, String key, Object value)
    {
        if (value != null)
        {
            if (value instanceof Integer)
            {
                value = value.longValue()
            }
            map[key] = value
        }
    }

    /**
     * Indicate to the Ajax servlet (JsonCommandServlet) that the 'status' field should
     * be set to 'false', and then set the 'data' field to the String of exception
     * text.
     * @param e Exception that occurred when calling the service.
     */
    private static void fail(Exception e)
    {
        markRequestFailed(getCauses(e))
        if (e instanceof AxisOverlapException ||
            e instanceof BranchMergeException ||
            e instanceof CommandCellException ||
            e instanceof CoordinateNotFoundException ||
            e instanceof RuleJump ||
            e instanceof RuleStop)
        {
            Throwable t = e
            while (t.getCause() != null)
            {
                t = t.getCause()
            }
            String msg = t.message
            if (StringUtilities.isEmpty(msg))
            {
                msg = t.getClass().getName()
            }
            LOG.info('user runtime error: ' + msg)
        }
        else
        {
            LOG.info("error occurred", e)
        }
    }

    private static String getTestCauses(Throwable t)
    {
        LinkedList<Map<String, Object>> stackTraces = new LinkedList<>()

        while (true)
        {
            stackTraces.push([msg: t.getLocalizedMessage(), trace: t.getStackTrace()])
            t = t.getCause()
            if (t == null)
            {
                break
            }
        }

        // Convert from LinkedList to direct access list
        List<Map<String, Object>> stacks = new ArrayList<>(stackTraces)
        StringBuilder s = new StringBuilder()
        int len = stacks.size()

        for (int i=0; i < len; i++)
        {
            Map<String, Object> map = stacks[i]
            s.append('<b style="color:darkred">')
            s.append(map.msg)
            s.append('</b><br>')

            if (i != len - 1)
            {
                Map nextStack = stacks[i + 1]
                StackTraceElement[] nextStackElementArray = (StackTraceElement[]) nextStack.trace
                s.append(trace(map.trace as StackTraceElement[], nextStackElementArray))
                s.append('<hr style="border-top: 1px solid #aaa;margin:8px"><b>Called by:</b><br>')
            }
            else
            {
                s.append(trace(map.trace as StackTraceElement[], null))
            }
        }

        return '<pre>' + s + '</pre>'
    }

    private static String trace(StackTraceElement[] stackTrace, StackTraceElement[] nextStrackTrace)
    {
        StringBuilder s = new StringBuilder()
        int len = stackTrace.length
        for (int i=0; i < len; i++)
        {
            s.append('&nbsp;&nbsp;')
            StackTraceElement element = stackTrace[i]
            if (alreadyExists(element, nextStrackTrace))
            {
                s.append('...continues below<br>')
                return s.toString()
            }
            else
            {
                s.append(element.className)
                s.append('.')
                s.append(element.methodName)
                s.append('()&nbsp;<small><b class="pull-right">')
                if (element.nativeMethod)
                {
                    s.append('Native Method')
                }
                else
                {
                    if (element.fileName)
                    {
                        s.append(element.fileName)
                        s.append(':')
                        s.append(element.lineNumber)
                    }
                    else
                    {
                        s.append('source n/a')
                    }
                }
                s.append('</b></small><br>')
            }
        }

        return s.toString()
    }

    private static boolean alreadyExists(StackTraceElement element, StackTraceElement[] stackTrace)
    {
        if (ArrayUtilities.isEmpty(stackTrace))
        {
            return false
        }

        for (StackTraceElement traceElement : stackTrace)
        {
            if (element.equals(traceElement))
            {
                return true
            }
        }
        return false
    }

    private static String getCauses(Throwable t)
    {
        StringBuilder s = new StringBuilder()
        while (t != null)
        {
            if (t.message == null)
            {
                s.append(t.toString())
            }
            else
            {
                s.append(t.message)
            }
            t = t.cause
            if (t != null)
            {
                s.append('<hr style="border-top: 1px solid #aaa;margin:8px">')
            }
        }

        return s.toString()
    }

    private static String getValueRepeatIfNecessary(Object[] values, int row, int col)
    {
        if (row > (values.length - 1))
        {
            row %= values.length
        }
        Object[] valueRow = (Object[]) values[row]
        if (ArrayUtilities.isEmpty(valueRow))
        {
            return null
        }
        if (col > (valueRow.length - 1))
        {
            col %= valueRow.length
        }
        return (String) valueRow[col]
    }

    private static Object convertStringToValue(String origValue)
    {
        if (StringUtilities.isEmpty(origValue))
        {
            return null
        }

        String value = origValue.trim()

        if ('0'.equals(value))
        {
            return 0L
        }
        else if ('true'.equalsIgnoreCase(value))
        {
            return true
        }
        else if ('false'.equalsIgnoreCase(value))
        {
            return false
        }

        if (isNumeric(value))
        {
            value = removeCommas(value)
            if (!value.contains("."))
            {
                try
                {
                    return Converter.convert(value, Long.class)
                }
                catch (Exception ignored) { }
            }

            try
            {
                return new BigDecimal(value)
            }
            catch (Exception ignored) { }
        }

        // Try as a date (the code below supports numerous different date formats)
        try
        {
            return Converter.convert(value, Date.class)
        }
        catch (Exception ignored) { }

        // OK, if all else fails, return it as the string it was
        return origValue
    }

    /**
     * Convert Axis to Map of Map representation (using json-io) and modify the
     * column ID to a String in the process.  This allows the column ID to work on
     * clients (like Javascript) that cannot support 64-bit values.
     */
    static Map convertAxis(Axis axis) throws IOException
    {
        String json = JsonWriter.objectToJson(axis, [(JsonWriter.TYPE): false] as Map)
        Map axisConverted = (Map) JsonReader.jsonToJava(json, [(JsonReader.USE_MAPS):true] as Map)
        axisConverted.'@type' = axis.getClass().getName()
        Object[] cols = axis.getColumns() as Object[]
        axisConverted.remove('idToCol')

        for (int i = 0; i < cols.length; i++)
        {
            Column actualCol = (Column) cols[i]
            Map col = columnToMap(actualCol)
            CellInfo cellInfo = new CellInfo(actualCol.getValue())
            String value = cellInfo.value
            if (axis.valueType == AxisValueType.DATE && axis.type != AxisType.SET && value != null)
            {
                value = NO_QUOTES_REGEX.matcher(value).replaceAll("")
            }
            col.value = value   // String version for Discrete, Range, or Set support
            col.isUrl = cellInfo.isUrl
            col.dataType = cellInfo.dataType
            col.isCached = cellInfo.isCached
            cols[i] = col
        }
        axisConverted.columns = cols
        return axisConverted
    }

    private static Map columnToMap(Column col)
    {
        Map map = [:]
        map.id = Converter.convert(col.id, String.class)  // Stringify Long ID (Javascript safe if quoted)
        map.'@type' = Column.class.getName()
        if (col.getMetaProperties().size() > 0)
        {
            map.metaProps = [:]
        }
        for (Map.Entry<String, Object> entry : col.getMetaProperties())
        {
            map.metaProps[entry.key] = entry.value == null ? 'null' : entry.value.toString()
        }
        map.displayOrder = col.displayOrder as long
        return map
    }

    public static boolean isNumeric(String str)
    {
        return IS_NUMBER_REGEX.matcher(str).matches()  // match a number with optional '-' and decimal.
    }

    private static String removeCommas(String str)
    {
        StringBuilder s = new StringBuilder()
        final int len = str.length()
        for (int i=0; i < len; i++)
        {
            char x = str.charAt(i)
            if (x != ',')
            {
                s.append(x)
            }
        }
        return s.toString()
    }

    private static Set<Long> getCoordinate(Object[] ids)
    {
        // Convert String column IDs to Longs
        Set<Long> colIds = new HashSet<>()
        for (Object id : ids)
        {
            colIds.add((Long)Converter.convert(id, Long.class))
        }
        return colIds;
    }

    private ApplicationID addTenant(ApplicationID appId)
    {
        String tenant = getTenant()
        return new ApplicationID(tenant, appId.app, appId.version, appId.status, appId.branch)
    }

    private String getTenant()
    {
        return ApplicationID.DEFAULT_TENANT
    }

    private static Object[] caseInsensitiveSort(Object[] items)
    {
        Arrays.sort(items, new Comparator<Object>() {
            public int compare(Object o1, Object o2)
            {
                ((String) o1)?.toLowerCase() <=> ((String) o2)?.toLowerCase()
            }
        })
        return items
    }

    private static String formatCube(NCube ncube, Map options)
    {
        String mode = options.mode
        if ('html' == mode)
        {
            return ncube.toHtml()
        }

        Map formatOptions = [:]
        if (mode.contains('index'))
        {
            formatOptions.indexFormat = true
        }
        if (mode.contains('nocells'))
        {
            formatOptions.nocells = true
        }

        String json = ncube.toFormattedJson(formatOptions)
        if (mode.contains('pretty'))
        {
            return JsonWriter.formatJson(json)
        }
        return json
    }

    private static String getInetHostname()
    {
        if (inetHostname == null)
        {
            inetHostname = InetAddressUtilities.getHostName()
        }
        return inetHostname
    }

    private static String getServletHostname()
    {
        if (servletHostname == null)
        {
            servletHostname = JsonCommandServlet.servletRequest.get().getServerName()
        }
        return servletHostname
    }
}
