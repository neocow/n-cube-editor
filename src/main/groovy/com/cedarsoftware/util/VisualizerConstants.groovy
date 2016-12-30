package com.cedarsoftware.util

import groovy.transform.CompileStatic

/**
 * Provides constants for the visualizer
 */

@CompileStatic
public final class VisualizerConstants
{
	public static final String SPACE = '&nbsp;'
	public static final String INDENT = "${SPACE}${SPACE}${SPACE}"
	public static final String BREAK = '<br>'
	public static final String COMMA_SPACE = ', '
	public static final String DOUBLE_BREAK = "${BREAK}${BREAK}"

	public static final String MISSING_SCOPE = 'Missing scope for '
	public static final String UNABLE_TO_LOAD = 'Unable to load '
	public static final String SCOPE_VALUE_NOT_FOUND = 'Scope value not found for '
	public static final String UNSPECIFIED = 'UNSPECIFIED'
	public static final String DEFAULT_SCOPE_VALUE = 'XXXX'

	public static final String STATUS_MISSING_START_SCOPE = 'missingStartScope'
	public static final String STATUS_SUCCESS = 'success'
	public static final String STATUS_INVALID_START_CUBE = 'invalidStartCube'

	public static final SafeSimpleDateFormat DATE_TIME_FORMAT = new SafeSimpleDateFormat('yyyy-MM-dd')
	public static final String HTTP = 'http:'
	public static final String HTTPS = 'https:'
	public static final String FILE = 'file:'

	public static final String JSON_FILE_PREFIX = 'config/'
	public static final String JSON_FILE_SUFFIX = '.json'
	public static final String VISUALIZER_CONFIG_CUBE_NAME = 'VisualizerConfig'
	public static final String CONFIG_ITEM = 'configItem'
	public static final String CONFIG_NETWORK_OVERRIDES_BASIC = 'networkOverridesBasic'
	public static final String CONFIG_NETWORK_OVERRIDES_FULL = 'networkOverridesFull'
	public static final String CONFIG_DEFAULT_LEVEL = 'defaultLevel'
	public static final String CONFIG_ALL_GROUPS = 'allGroups'
	public static final String CONFIG_ALL_TYPES = 'allTypes'
	public static final String CONFIG_GROUP_SUFFIX = 'groupSuffix'
	public static final String CUBE_TYPE = 'cubeType'

	//RPM related constants
	public static final String RPM_CLASS = 'rpm.class'
	public static final String RPM_ENUM = 'rpm.enum'
	public static final String RPM_CLASS_DOT = 'rpm.class.'
	public static final String RPM_SCOPE_CLASS_DOT = 'rpm.scope.class.'
	public static final String RPM_ENUM_DOT = 'rpm.enum.'
	public static final String CLASS_TRAITS = 'CLASS_TRAITS'
	public static final String DOT_CLASS_TRAITS = '.classTraits'
	public static final String DOT_TRAITS = '.traits'
	public static final String R_RPM_TYPE = 'r:rpmType'
	public static final String R_EXISTS = 'r:exists'
	public static final String V_ENUM = 'v:enum'
	public static final String R_SCOPED_NAME = 'r:scopedName'
	public static final String V_MIN = 'v:min'
	public static final String V_MAX = 'v:max'
	public static final String SOURCE_FIELD_NAME = 'sourceFieldName'
	public static final String AXIS_FIELD = 'field'
	public static final String AXIS_NAME = 'name'
	public static final String AXIS_TRAIT = 'trait'

	public static final String EFFECTIVE_VERSION = '_effectiveVersion'
	public static final String POLICY_CONTROL_DATE = 'policyControlDate'
	public static final String QUOTE_DATE = 'quoteDate'
	public static final String BUSINESS_DIVISION_CODE = 'businessDivisionCode'
	public static final String STATE = 'state'
	public static final String LOCATION_STATE = 'locationState'
	public static final String SOURCE_SCOPE_KEY_PREFIX = 'source'
	public static final String SYSTEM_SCOPE_KEY_PREFIX = "_"
	public static final String EFFECTIVE_VERSION_SCOPE_KEY = SYSTEM_SCOPE_KEY_PREFIX + "effectiveVersion"
	public static final String ENT_APP = 'ENT.APP'
	public static final String BUSINESS_DIVISION_CUBE_NAME = 'ent.manual.BusinessDivision'
	public static final String STATE_CUBE_NAME = 'ent.manual.State'
	public static final Set<String> MANDATORY_RPM_SCOPE_KEYS = [AXIS_FIELD, AXIS_NAME, AXIS_TRAIT] as Set

	public static final String CUBE_TYPE_RPM = 'rpm'
	public static final String CONFIG_DERIVED_SCOPE_KEYS = 'derivedScopeKeys'

	/*TODO: Not needed currently, but will revisit
	public static final String CONFIG_DERIVED_SOURCE_SCOPE_KEYS = 'derivedSourceScopeKeys'
	public static final String CONFIG_DEFAULT_OPTIONAL_SCOPE_KEYS = 'defaultOptionalScopeKeys'
    */

	public static final String TYPES_TO_ADD_CUBE_NAME = 'VisualizerTypesToAdd'
	public static final String SOURCE_TYPE = 'sourceType'
	public static final String TARGET_TYPE = 'targetType'
}