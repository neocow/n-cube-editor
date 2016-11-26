package com.cedarsoftware.util

import groovy.transform.CompileStatic

/**
 * Holds information related to the visualization of cubes and their relationships.
 */

@CompileStatic
class VisualizerInfo
{
	String startCubeName
	Map<String, Object> scope
	List<Map<String, String>> nodes
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
}