package com.cedarsoftware.util

import com.cedarsoftware.ncube.NCube
import groovy.transform.CompileStatic

/**
 * Holds information about a source cube and target cube for purposes of creating a visualization of the cubes and their relationship.
 */

@CompileStatic
class VisualizerRelInfo
{
	boolean loadFieldsAndTraits = true
	Set<String> notes = []
	Map<String, Object> scope

	long targetId
    NCube targetCube
	Map<String, Object> targetScope
	Map<String, Map<String, Object>> targetTraitMaps
	long targetLevel

	long sourceId
	NCube sourceCube
	Map<String, Object> sourceScope
	Map<String, Map<String, Object>> sourceTraitMaps
	String sourceFieldName
	String sourceFieldRpmType

	Set<String> requiredScopeKeys  = []
	Set<String> optionalScopeKeys  = []

	Set<String> getRequiredScope()
	{
		Set<String> requiredScope = targetCube.getRequiredScope(targetScope, [:] as Map)
		requiredScope.remove(Visualizer.AXIS_FIELD)
		requiredScope.remove(Visualizer.AXIS_NAME)
		requiredScope.remove(Visualizer.AXIS_TRAIT)
		return requiredScope
	}
}