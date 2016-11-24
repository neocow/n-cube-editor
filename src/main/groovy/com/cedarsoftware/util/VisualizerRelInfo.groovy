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
	Map scope

	long targetId
    NCube targetCube
	Map targetScope
	Map targetTraitMaps
	long targetLevel

	long sourceId
	NCube sourceCube
	Map sourceScope
	Map sourceTraitMaps
	String sourceFieldName
	String sourceFieldRpmType

	Set requiredScopeKeys  = []
	Set optionalScopeKeys  = []
}