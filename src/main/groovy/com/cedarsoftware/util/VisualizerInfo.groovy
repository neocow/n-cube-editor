package com.cedarsoftware.util

import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.DEFAULT_SCOPE_VALUE

/**
 * Holds information related to the visualization of cubes and their relationships.
 */

@CompileStatic
class VisualizerInfo
{
	String startCubeName
	Map<String, Object> scope
	List<Map<String, Object>> nodes
	List<Map<String, String>> edges

	long maxLevel
	long nodeCount
	long selectedLevel

	Map<String,String> allGroups
	Set<String> selectedGroups
	Set<String> availableGroupsAllLevels
	String groupSuffix
	Set<String> availableScopeKeys = []
	Map<String, Set<Object>> availableScopeValues = [:]

	void trimSelectedLevel()
	{
		selectedLevel = selectedLevel > nodeCount ? nodeCount : selectedLevel
	}

	void trimSelectedGroups()
	{
		selectedGroups = availableGroupsAllLevels.intersect(selectedGroups)
	}

    boolean addMissingMinimumScope(String key, String value, String messageSuffix, Set<String> messages)
    {
        Map<String, Object> scope = scope
        boolean missingScope
        if (scope.containsKey(key))
        {
            if (!scope[key])
            {
                scope[key] = value
                missingScope = true
            }
            else if (DEFAULT_SCOPE_VALUE == scope[key])
            {
                missingScope = true
            }
        }
        else
        {
            scope[key] = value
            missingScope = true
        }

        if (missingScope)
        {
            messages << "Scope is required for ${key}. ${messageSuffix}".toString()
        }
        return missingScope
    }
}