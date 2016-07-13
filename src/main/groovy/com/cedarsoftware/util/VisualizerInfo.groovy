package com.cedarsoftware.util

import com.cedarsoftware.ncube.NCube
import groovy.transform.CompileStatic

/**
 * Holds information related to the visualization of cubes and their relationships.
 */

@CompileStatic
class VisualizerInfo
{
	String startCubeName
	Map scope
	List nodes
	List edges

    long maxLevel
	long nodeCount
	long selectedLevel

	Map allGroups
	Set selectedGroups
	Set availableGroupsAllLevels
	String groupSuffix
}