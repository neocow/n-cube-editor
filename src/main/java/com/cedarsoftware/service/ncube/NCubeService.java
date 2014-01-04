package com.cedarsoftware.service.ncube;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.util.CaseInsensitiveSet;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Date;
import java.util.Set;

/**
 * NCube CRUD API
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

    public NCube loadNCube(String name, String app, String version, String status)
    {
        return NCubeManager.getInstance().loadCube(getConnection(), app, name, version, status, new Date());
    }

    public Object[] getNCubes(String pattern, String app, String version, String status)
    {
        return NCubeManager.getInstance().getNCubes(getConnection(), app, version, status, pattern, new Date());
    }

    public String getHtml(String name, String app, String version, String status)
    {
        NCube ncube = NCubeManager.getInstance().loadCube(getConnection(), app, name, version, status, new Date());
        return ncube.toHtml();
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
}
