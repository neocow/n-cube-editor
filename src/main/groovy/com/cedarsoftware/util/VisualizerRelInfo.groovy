package com.cedarsoftware.util

import com.cedarsoftware.ncube.NCube
import groovy.transform.CompileStatic
import ncube.grv.method.NCubeGroovyController

/**
 * Holds information about a source cube and target cube for purposes of creating a visualization of the cubes and their relationship.
 */

@CompileStatic
class VisualizerRelInfo
{
	long id

    NCube targetCube
	Map targetScope
	Map targetTraitMaps
	long targetLevel

	NCube sourceCube
	Map sourceScope
	Map sourceTraitMaps
	String sourceFieldName
	String sourceFieldRpmType
}