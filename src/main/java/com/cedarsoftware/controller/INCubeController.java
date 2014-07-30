package com.cedarsoftware.controller;

import com.cedarsoftware.ncube.Axis;

import java.util.Map;

/**
 * Handle n-cube Editor requests.
 *  
 * @author John DeRegnaucourt
 */
public interface INCubeController extends IBaseController
{
    Object[] getCubeList(String filter, String app, String version, String status);
    String getHtml(String name, String app, String version, String status);
    String getJson(String name, String app, String version, String status);
    Object[] getAppNames();
    Object[] getAppVersions(String app, String status);
    void createCube(String name, String app, String version);
    boolean deleteCube(String name, String app, String version);
    Object[] getReferencesTo(String name, String app, String version, String status);
    Object[] getReferencesFrom(String name, String app, String version, String status);
    Object[] getRequiredScope(String name, String app, String version, String status);
    void duplicateCube(String newName, String name, String newApp, String app, String newVersion, String version, String status);
    void releaseCubes(String app, String version, String newSnapVer);
    void changeVersionValue(String app, String currVersion, String newSnapVer);
    void addAxis(String name, String app, String version, String axisName, String type, String valueType);
    Object[] getAxes(String name, String app, String version, String status);
    Map getAxis(String name, String app, String version, String status, String axisName);
    void deleteAxis(String name, String app, String version, String axisName);
    void updateAxis(String name, String app, String version, String origAxisName, String axisName, boolean hasDefault, boolean isSorted, boolean multiMatch);
    void updateColumnCell(String name, String app, String version, String colId, String value);
    void updateAxisColumns(String name, String app, String version, Axis updatedAxis);
    Object updateCell(String name, String app, String version, Object[] colIds, String value);
    void renameCube(String oldName, String newName, String app, String version);
    void saveJson(String name, String app, String version, String json);
    public void getTestData(String name, String app, String version, String status);
    public void saveTestData(String name, String app, String version, String testData);
}
