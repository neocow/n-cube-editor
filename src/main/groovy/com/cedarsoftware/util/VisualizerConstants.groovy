package com.cedarsoftware.util

import groovy.transform.CompileStatic

/**
 * Provides constants for the visualizer
 */

@CompileStatic
class VisualizerConstants
{
	public static final String SPACE = '&nbsp;'
	public static final String INDENT = "${SPACE}${SPACE}${SPACE}"
	public static final String BREAK = '<br>'
	public static final String COMMA_SPACE = ', '
	public static final String DOUBLE_BREAK = "${BREAK}${BREAK}"

	public static final String MISSING_SCOPE = 'Missing scope for '
	public static final String UNABLE_TO_LOAD = 'Unable to load '
	public static final String SCOPE_VALUE_NOT_FOUND = 'Scope value not found for '
	public static final String DEFAULT_SCOPE_VALUE = 'XXXX'

	public static final String UNSPECIFIED = 'UNSPECIFIED'
	public static final String NCUBE = 'NCUBE'
	public static final String RULE_NCUBE = 'RULE_NCUBE'

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
	public static final String VISUALIZER_CONFIG_NETWORK_OVERRIDES_CUBE_NAME = 'VisualizerConfig.NetworkOverrides'
	public static final String CONFIG_ITEM = 'configItem'
	public static final String CONFIG_NETWORK_OVERRIDES_BASIC = 'networkOverridesBasic'
	public static final String CONFIG_NETWORK_OVERRIDES_FULL = 'networkOverridesFull'
	public static final String CONFIG_NETWORK_OVERRIDES_TOP_NODE = 'networkOverridesTopNode'
	public static final String CONFIG_DEFAULT_LEVEL = 'defaultLevel'
	public static final String CONFIG_ALL_GROUPS = 'allGroups'
	public static final String CONFIG_ALL_TYPES = 'allTypes'
	public static final String CONFIG_GROUP_SUFFIX = 'groupSuffix'
	public static final String CUBE_TYPE = 'cubeType'
	public static final String CUBE_TYPE_DEFAULT = null

	public static final String LOAD_CELL_VALUES_LABEL = 'cell values'
	public static final String NONE = 'none'

	public static final String DETAILS_LABEL_NOTE = 'Note:'
	public static final String DETAILS_LABEL_REASON = 'Reason:'
	public static final String DETAILS_LABEL_SCOPE = 'Scope'
	public static final String DETAILS_LABEL_AVAILABLE_SCOPE = 'Available scope'
	public static final String DETAILS_LABEL_REQUIRED_SCOPE_KEYS = 'Required scope keys'
	public static final String DETAILS_LABEL_OPTIONAL_SCOPE_KEYS = 'Optional scope keys'
	public static final String DETAILS_LABEL_AXES = 'Axes'
	public static final String DETAILS_LABEL_CELL_VALUES = 'Cell values'
	public static final String DETAILS_LABEL_EXPAND_ALL = 'Expand all'
	public static final String DETAILS_LABEL_COLLAPSE_ALL = 'Collapse all'
	public static final String DETAILS_LABEL_NON_EXECUTED_VALUE = 'Non-executed value:'
	public static final String DETAILS_LABEL_EXECUTED_VALUE = 'Executed value:'
	public static final String DETAILS_LABEL_EXCEPTION = 'Exception:'

	public static final String DETAILS_LABEL_MESSAGE = 'Message:'
	public static final String DETAILS_LABEL_ROOT_CAUSE = 'Root cause:'
	public static final String DETAILS_LABEL_STACK_TRACE = 'Stack trace:'

	public static final String DETAILS_TITLE_EXPAND_ALL = 'Expand all cell details'
	public static final String DETAILS_TITLE_COLLAPSE_ALL = 'Collapse all cell details'
	public static final String DETAILS_TITLE_EXECUTED_CELL = 'Executed cell'
	public static final String DETAILS_TITLE_MISSING_OR_INVALID_COORDINATE = 'The cell was executed with a missing or invalid coordinate.'
	public static final String DETAILS_TITLE_ERROR_DURING_EXECUTION = 'An error occurred during the execution of the cell.'

	public static final String DETAILS_CLASS_CELL_VALUES = 'cellValues'
	public static final String DETAILS_CLASS_WORD_WRAP = 'wordwrap'
	public static final String DETAILS_CLASS_MISSING_SCOPE = 'missingScope'
	public static final String DETAILS_CLASS_EXCEPTION = 'Exception'
	public static final String DETAILS_CLASS_EXPAND_ALL = 'expandAll'
	public static final String DETAILS_CLASS_COLLAPSE_ALL = 'collapseAll'
	public static final String DETAILS_CLASS_EXECUTED_CELL = 'executedCell'

}