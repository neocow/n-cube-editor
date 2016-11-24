package com.cedarsoftware.util

import com.cedarsoftware.ncube.Column
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.RuleInfo
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.exception.InvalidCoordinateException
import com.google.common.base.Splitter
import groovy.transform.CompileStatic
import ncube.grv.method.NCubeGroovyController

/**
 * Provides the information used to visualize rpm cubes associated with a given rpm cube.
 */

// TODO: This code needs to be moved out of NCE and pulled-in via Grapes.
@CompileStatic
class Visualizer extends NCubeGroovyController
{
	public static final String RPM_CLASS = 'rpm.class'
	public static final String RPM_ENUM = 'rpm.enum'
	public static final String RPM_CLASS_DOT = 'rpm.class.'
	public static final String RPM_SCOPE_CLASS_DOT = 'rpm.scope.class.'
	public static final String RPM_ENUM_DOT = 'rpm.enum.'
	public static final String CLASS_TRAITS = 'CLASS_TRAITS'
	public static final String DOT_CLASS_TRAITS = '.classTraits'
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

	public static final String _ENUM = '_ENUM'
	public static final String UNSPECIFIED = 'UNSPECIFIED'
	public static final Map ALL_GROUPS_MAP = [PRODUCT: 'Product', FORM: 'Form', RISK: 'Risk', COVERAGE: 'Coverage', CONTAINER: 'Container', DEDUCTIBLE: 'Deductible', LIMIT: 'Limit', RATE: 'Rate', RATEFACTOR: 'Rate Factor', PREMIUM: 'Premium', PARTY: 'Party', PLACE: 'Place', ROLE: 'Role', ROLEPLAYER: 'Role Player', UNSPECIFIED: 'Unspecified']
	public static final String[] GROUPS_TO_SHOW_IN_TITLE = ['COVERAGE', 'DEDUCTIBLE', 'LIMIT', 'PREMIUM', 'PRODUCT', 'RATE', 'RATEFACTOR', 'RISK', 'ROLEPLAYER', 'ROLE']

	public static final String EFFECTIVE_VERSION = '_effectiveVersion'
	public static final String POLICY_CONTROL_DATE = 'policyControlDate'
	public static final String QUOTE_DATE = 'quoteDate'
	public static final List DERIVED_SCOPE_KEYS = ['product', 'risk', 'coverage', 'container', 'deductible', 'limit', 'rate', 'ratefactor', 'premium', 'party', 'place', 'role', 'roleplayer']
	public static final List DERIVED_SOURCE_SCOPE_KEYS = ['sourceRisk', 'sourceCoverage', 'sourceContainer', 'sourceDeductible', 'sourceLimit', 'sourceRate', 'sourceRatefactor', 'sourcePremium', 'sourceParty', 'sourcePlace', 'sourceRole', 'sourceRoleplayer']
	public static final String SOURCE_SCOPE_KEY_PREFIX = 'source'

	public static final String SYSTEM_SCOPE_KEY_PREFIX = "_";
	public static final String EFFECTIVE_VERSION_SCOPE_KEY = SYSTEM_SCOPE_KEY_PREFIX + "effectiveVersion";

	public static final List DEFAULT_OPTIONAL_SCOPE_KEYS = ['action', 'businessDivisionCode', 'context', 'date', 'edition', '_effectiveVersion', 'env', 'formId', 'LocationState', 'screen', 'state', 'transaction', 'transactionsubtype', 'username', 'view']
	public static final List DEFAULT_AVAILABLE_SCOPE_KEYS = DERIVED_SCOPE_KEYS + DERIVED_SOURCE_SCOPE_KEYS + DEFAULT_OPTIONAL_SCOPE_KEYS + [POLICY_CONTROL_DATE, QUOTE_DATE, EFFECTIVE_VERSION]
	public static final String DEFAULT_SCOPE_VALUE = '????'
	public static final long DEFAULT_LEVEL = 2

	public static final String SPACE = '&nbsp;'
	public static final String INDENT = "${SPACE}${SPACE}${SPACE}"
	public static final String BREAK = '<br>'
	public static final String COMMA_SPACE = ', '
	public static final String DOUBLE_BREAK = "${BREAK}${BREAK}"

	public static final int NODE_LABEL_MAX_LINE_LENGTH = 16
	public static final BigDecimal NODE_LABEL_LINE_LENGTH_MULTIPLIER = 1.2

	public static final List MANDATORY_RPM_SCOPE_KEYS = [AXIS_FIELD, AXIS_NAME, AXIS_TRAIT]
	public static final String MISSING_SCOPE = 'missing scope'
	public static final String UNABLE_TO_LOAD = 'unable to load'
	public static final String SCOPE_VALUE_NOT_FOUND = 'scope value not found'

	public static final String STATUS_MISSING_START_SCOPE = 'missingStartScope'
	public static final String STATUS_SUCCESS = 'success'

	private VisualizerHelper helper = new VisualizerHelper()
	private static final SafeSimpleDateFormat DATE_TIME_FORMAT = new SafeSimpleDateFormat('yyyy-MM-dd')
	private Set messages = []
	private Set visited = []
	private Map<String, Set<Set>> requiredScopeKeys = [:]
	private Map<String, Set<Set>> optionalScopeKeys = [:]
	private String defaultScopeEffectiveVersion
	private String defaultScopeDate
	private Deque<VisualizerRelInfo> stack = new ArrayDeque<>()

	/**
	 * Provides the information used to visualize rpm cubes associated with a given rpm cube.
	 *
	 * input:
	 * 	          String startCubeName, name of the starting cube
	 *            Map scope
	 *            Set selectedGroups, indicates which groups should be included in the visualization
	 *            String selectedLevel, indicates the depth of traversal from the start cube
	 *
	 * output     Map containing status, messages and visualizer information
	 *
	 */
	Map buildGraph()
	{
		Map options = input.options as Map
		String cubeName = options.startCubeName as String
		helper.ncube = ncube

		defaultScopeEffectiveVersion = applicationID.version.replace('.', '-')
		defaultScopeDate = DATE_TIME_FORMAT.format(new Date())

		VisualizerInfo visInfo = new VisualizerInfo()
		visInfo.startCubeName = cubeName
		visInfo.scope = options.scope as CaseInsensitiveMap
		visInfo.allGroups = ALL_GROUPS_MAP
		visInfo.availableGroupsAllLevels = []
		visInfo.groupSuffix = _ENUM
		Set selectedGroups = options.selectedGroups as Set
		visInfo.selectedGroups = selectedGroups ?: ALL_GROUPS_MAP.keySet()
		String selectedLevel = options.selectedLevel as String
		visInfo.selectedLevel = selectedLevel == null ? DEFAULT_LEVEL : Converter.convert(selectedLevel, long.class) as long
		Set availableScopeKeys = options.availableScopeKeys as Set
		visInfo.availableScopeKeys = availableScopeKeys ?: DEFAULT_AVAILABLE_SCOPE_KEYS as Set
		visInfo.maxLevel = 1
		visInfo.nodeCount = 1
		visInfo.nodes = []
		visInfo.edges = []

		if (hasMissingMinimumScope(visInfo))
        {
			return [status: STATUS_MISSING_START_SCOPE, visInfo: visInfo, message: messages.join(DOUBLE_BREAK)]
		}

		getRpmVisualization(visInfo)

		String message = messages.empty ? null : messages.join(DOUBLE_BREAK)
		return [status: STATUS_SUCCESS, visInfo: visInfo, message: message]
	}

	private void getRpmVisualization(VisualizerInfo visInfo)
	{
		VisualizerRelInfo relInfo = new VisualizerRelInfo()
		relInfo.targetCube = getCube(visInfo.startCubeName)
		relInfo.scope = visInfo.scope
		relInfo.targetLevel = 1
		relInfo.targetId = 1
		stack.push(relInfo)

		while (!stack.empty)
        {
			processCube(visInfo, stack.pop())
		}

		addSets(visInfo.availableScopeKeys, requiredScopeKeys.values() as Set)
		addSets(visInfo.availableScopeKeys, optionalScopeKeys.values() as Set)

		trimSelectedLevel(visInfo)
		trimSelectedGroups(visInfo)
	}

	private static Set addSets(Set<String> set, Set<Set> sets)
    {
		sets.each {
			set.addAll(it)
		}
	}

	private void processCube(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		if (relInfo.targetCube.name.startsWith(RPM_CLASS))
		{
			processClassCube(visInfo, relInfo)
		}
		else
		{
			processEnumCube(visInfo, relInfo)
		}
	}

	private void processClassCube(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		String targetCubeName = relInfo.targetCube.name

		boolean loadFieldsAndTraits = canLoadTargetAsRpmClass(relInfo)
		if (loadFieldsAndTraits)
		{
			loadFieldsAndTraits = getTraitMaps(visInfo, relInfo)
		}

		addToEdges(visInfo, relInfo)

		if (!visited.add(targetCubeName + relInfo.scope.toString()))
		{
			return
		}

		String group = getGroup(targetCubeName)
		visInfo.availableGroupsAllLevels << group
		addToNodes(visInfo, relInfo, group)

		if (loadFieldsAndTraits)
		{
			relInfo.targetTraitMaps.each { k, v ->
				String targetFieldName = k as String
				Map targetTraits = v as Map
				if (CLASS_TRAITS != targetFieldName)
				{
					String targetFieldRpmType = targetTraits[R_RPM_TYPE]

					if (!helper.isPrimitive(targetFieldRpmType))
					{
						NCube nextTargetCube
						String nextTargetCubeName = ""
						if (targetTraits.containsKey(V_ENUM))
						{
							nextTargetCubeName = RPM_ENUM_DOT + targetTraits[V_ENUM]
							nextTargetCube = getCube(nextTargetCubeName)
						}
						else if (targetFieldRpmType)
						{
							nextTargetCubeName = RPM_CLASS_DOT + targetFieldRpmType
							nextTargetCube = getCube(nextTargetCubeName)
						}

						if (nextTargetCube)
						{
							addToStack(visInfo, relInfo, nextTargetCube, targetFieldRpmType, targetFieldName)
						}
						else
                        {
							messages << "No cube exists with name of ${nextTargetCubeName}. Cube not included in the visualization."
						}
					}
				}
			}
		}
	}

	private void processEnumCube(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		String group = UNSPECIFIED
		String targetCubeName = relInfo.targetCube.name
		String sourceFieldRpmType = relInfo.sourceFieldRpmType

		if (!targetCubeName.startsWith(RPM_ENUM))
        {
			throw new IllegalStateException("Cube is not an rpm.enum cube: ${targetCubeName}.")
		}

		if (relInfo.sourceCube && (!sourceFieldRpmType || helper.isPrimitive(sourceFieldRpmType)))
		{
			return
		}

		boolean loadFieldsAndTraits = getTraitMaps(visInfo, relInfo)

		if (loadFieldsAndTraits)
        {
			relInfo.targetTraitMaps.each { k, v ->
				String targetFieldName = k as String
	    		if (CLASS_TRAITS != targetFieldName)
				{
					try
					{
						String nextTargetCubeName = getNextTargetCubeName(relInfo, targetFieldName)

						if (nextTargetCubeName)
						{
							NCube nextTargetCube = getCube(nextTargetCubeName)
							if (nextTargetCube)
							{
								addToStack(visInfo, relInfo, nextTargetCube, relInfo.sourceFieldRpmType, targetFieldName)

								if (group == UNSPECIFIED)
                                {
									group = getGroup(nextTargetCubeName)
								}
							}
							else
                            {
								messages << "No cube exists with name of ${nextTargetCubeName}. Cube not included in the visualization."
							}
						}
					}
					catch (Exception e)
					{
						throw new IllegalStateException("Error processing the cube for enum field ${targetFieldName} in enum ${targetCubeName}.", e)
					}
				}
			}
		}

		addToEdges(visInfo, relInfo)

		if (!visited.add(targetCubeName + relInfo.scope.toString()))
		{
			return
		}

		visInfo.availableGroupsAllLevels << group
		addToNodes(visInfo, relInfo, group)
	}

	private static void trimSelectedLevel(VisualizerInfo visInfo)
    {
		long nodeCount = visInfo.nodeCount
		long selectedLevel = visInfo.selectedLevel
  		visInfo.selectedLevel = selectedLevel > nodeCount ? nodeCount : selectedLevel
	}

	private static void trimSelectedGroups(VisualizerInfo visInfo)
    {
		visInfo.selectedGroups = visInfo.availableGroupsAllLevels.intersect(visInfo.selectedGroups)
	}

	private VisualizerRelInfo addToStack(VisualizerInfo visInfo, VisualizerRelInfo relInfo, NCube nextTargetCube, String rpmType, String targetFieldName)
	{
		try
		{
			NCube nextSourceCube = relInfo.targetCube

			VisualizerRelInfo nextRelInfo = new VisualizerRelInfo()
			nextRelInfo.targetCube = nextTargetCube
			nextRelInfo.scope = getScopeRelativeToSource(nextTargetCube, rpmType, targetFieldName, relInfo.scope)
			nextRelInfo.sourceCube = nextSourceCube
			nextRelInfo.sourceScope = relInfo.targetScope
			nextRelInfo.sourceFieldName = targetFieldName
			nextRelInfo.sourceFieldRpmType = rpmType
			nextRelInfo.sourceTraitMaps = relInfo.targetTraitMaps
			nextRelInfo.sourceId = relInfo.targetId

			long nextTargetTargetLevel = relInfo.targetLevel + 1
			nextRelInfo.targetLevel = nextTargetTargetLevel

			long maxLevel = visInfo.maxLevel
 			visInfo.maxLevel = maxLevel < nextTargetTargetLevel ? nextTargetTargetLevel : maxLevel
			visInfo.nodeCount += 1
			nextRelInfo.targetId = visInfo.nodeCount

			stack.push(nextRelInfo)
			return nextRelInfo
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Error processing the class for field ${relInfo.sourceFieldName} in class ${nextTargetCube.name}.", e)
		}
	}

	private boolean canLoadTargetAsRpmClass(VisualizerRelInfo relInfo)
	{
		//When the source cube points directly to the target cube (source cube and target cube are both rpm.class),
		//check if the source field name matches up with the scoped name of the target. If not, the target cube cannot be
		//loaded as an rpm.class.
		NCube sourceCube = relInfo.sourceCube
		NCube targetCube = relInfo.targetCube

		if (sourceCube && sourceCube.name.startsWith(RPM_CLASS_DOT) && targetCube.name.startsWith(RPM_CLASS_DOT) &&
				targetCube.getAxis(AXIS_TRAIT).findColumn(R_SCOPED_NAME))
		{
			String type = relInfo.sourceFieldRpmType
			NCube classTraitsCube = getCube(RPM_SCOPE_CLASS_DOT + type + DOT_CLASS_TRAITS)
			List columns = classTraitsCube.getAxis(type).columns
			String sourceFieldName = relInfo.sourceFieldName
			if (!columns.find { sourceFieldName == (it as Column).value })
            {
				relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): UNABLE_TO_LOAD]]
                String msg = """\
The source ${sourceCube.name} points directly to (${targetCube.name}) via field ${sourceFieldName}, but \
there is no ${type} named ${sourceFieldName} on this cube.  ${DOUBLE_BREAK}Therefore \
it cannot be loaded as an rpm.class in the visualization."""
				relInfo.notes << msg
				relInfo.loadFieldsAndTraits = false
				return false
			}
		}
		return true
	}

	private static String getNodeGroup(String cubeName, String group)
	{
		return cubeName.startsWith(RPM_ENUM) ? group + _ENUM : group
	}

	private static String getGroup(String cubeName)
	{
		Iterable<String> splits = Splitter.on('.').split(cubeName)
		String group = splits[2].toUpperCase()
		Set groups = ALL_GROUPS_MAP.keySet()
		return groups.contains(group) ? group : UNSPECIFIED
	}

	private static void addToEdges(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		NCube sourceCube = relInfo.sourceCube
		if (!sourceCube)
		{
			return
		}

		NCube targetCube = relInfo.targetCube
		String sourceFieldName = relInfo.sourceFieldName
		Map sourceTraitMaps = relInfo.sourceTraitMaps

		Map edgeMap = [:]
		String sourceCubeEffectiveName = getEffectiveName(sourceCube, sourceTraitMaps)
		String targetCubeEffectiveName = getEffectiveName(targetCube, relInfo.targetTraitMaps)
		edgeMap.id = String.valueOf(visInfo.edges.size() + 1)
		edgeMap.from = String.valueOf(relInfo.sourceId)
		edgeMap.to = String.valueOf(relInfo.targetId)
		edgeMap.fromName = sourceCubeEffectiveName
		edgeMap.toName = targetCubeEffectiveName
		edgeMap.fromFieldName = sourceFieldName
		edgeMap.level = String.valueOf(relInfo.targetLevel)
		edgeMap.label = ''
		String vMin = sourceTraitMaps[sourceFieldName][V_MIN]
		String vMax = sourceTraitMaps[sourceFieldName][V_MAX]

		if (vMin != null && vMax != null)
        {
			edgeMap.title = "${vMin}:${vMax}"
		}
		else
        {
			edgeMap.title = ''
		}
		visInfo.edges << edgeMap
	}

	private static void addToNodes(VisualizerInfo visInfo, VisualizerRelInfo relInfo, String group)
	{
		NCube targetCube = relInfo.targetCube
		String targetCubeName = targetCube.name
		String sourceFieldName = relInfo.sourceFieldName
		String nodeGroup = getNodeGroup(targetCubeName, group)

		Map nodeMap = [:]
		nodeMap.id = String.valueOf(relInfo.targetId)
		nodeMap.level = String.valueOf(relInfo.targetLevel)
		nodeMap.name = targetCubeName
		nodeMap.fromFieldName = sourceFieldName == null ? null : sourceFieldName
		nodeMap.label = getLabel(relInfo, nodeGroup)
		nodeMap.title = targetCubeName
		nodeMap.desc = getTitle(relInfo, nodeMap)
		nodeMap.group = nodeGroup
		visInfo.nodes << nodeMap
	}

	private static String getLabel(VisualizerRelInfo relInfo, String nodeGroup)
	{
		StringBuilder sb = new StringBuilder()
		if (GROUPS_TO_SHOW_IN_TITLE.contains(nodeGroup))
		{
			String label = ALL_GROUPS_MAP[nodeGroup]
			int len = label.length()
			sb.append(label)
			sb.append('\n')
			sb.append('-' * Math.floor(len * NODE_LABEL_LINE_LENGTH_MULTIPLIER))
			sb.append('\n')
		}

		String labelName = getDotSuffix(getEffectiveName(relInfo.targetCube, relInfo.targetTraitMaps))
		String[] splitName = labelName.split("(?=\\p{Upper})")
		String line = ''
		for (String part : splitName)
		{
			if (line.length() + part.length() < NODE_LABEL_MAX_LINE_LENGTH)
			{
				line += part
			}
			else
			{
				sb.append(line)
				sb.append('\n')
				line = part
			}
		}
		sb.append(line)
		return sb.toString()
	}

	private static String getTitle(VisualizerRelInfo relInfo, Map nodeMap)
	{
		boolean loadFieldsAndTraits = relInfo.loadFieldsAndTraits
		Map traitMaps = relInfo.targetTraitMaps
		Set notes = relInfo.notes
		String scopedName = getScopedName(traitMaps)
		StringBuilder sb = new StringBuilder()

		//Scoped Name
		if (scopedName)
        {
			sb.append('<b>scoped name = </b>')
			sb.append(scopedName)
			sb.append(DOUBLE_BREAK)
		}

		//Notes
		if (notes)
		{
			sb.append('<b>Note: </b><br>')
			notes.each { String note ->
				sb.append(' ')
				sb.append(note)
				sb.append(BREAK)
			}
			sb.append(BREAK)
		}

		//Scope
		if (loadFieldsAndTraits)
		{
			sb.append('<b>scope used to load class = </b>')
			sb.append(relInfo.targetScope.toString() - '{' - '}')
			sb.append(DOUBLE_BREAK)
		}
		sb.append('<b>available scope to load class = </b>')
		sb.append(relInfo.scope.toString() - '{' - '}')
		sb.append(DOUBLE_BREAK)

		//Level
		sb.append('<b>level = </b>')
		sb.append(nodeMap.level.toString())
		sb.append(DOUBLE_BREAK)

		//Fields
		if (loadFieldsAndTraits)
		{
			sb.append(getTitleFields(traitMaps))
		}

		return sb.toString()
	}

	private static String getTitleFields(Map traitMaps)
	{
		String fieldString = "<b>fields = </b>"
		return fieldString + getFieldDetails(traitMaps)
	}

	private static String getFieldDetails(Map traitMaps)
	{
		StringBuilder fieldDetails = new StringBuilder()
		fieldDetails.append('')

		traitMaps.each { k, v ->
			String fieldName = k as String
			if (CLASS_TRAITS != fieldName)
            {
				fieldDetails.append(BREAK)
				fieldDetails.append(SPACE)
				fieldDetails.append(fieldName)
			}
		}
		return fieldDetails.toString()
	}

	private static String getEffectiveName(NCube cube, Map traitMaps)
	{
		String scopedName = getScopedName(traitMaps)
		return scopedName == null ? cube.name : scopedName
	}

	private static String getScopedName(Map traitMaps)
	{
		Map classTraitsTraitMap = traitMaps[CLASS_TRAITS] as Map
		return classTraitsTraitMap ? classTraitsTraitMap[R_SCOPED_NAME] : null
	}

	private static String getNextTargetCubeName(VisualizerRelInfo relInfo, String targetFieldName)
	{
		if (relInfo.sourceCube.getAxis(AXIS_TRAIT).findColumn(R_SCOPED_NAME))
		{
			if (relInfo.sourceTraitMaps[CLASS_TRAITS][R_SCOPED_NAME] == null)
			{
				return null
			}
			else
			{
				return RPM_CLASS_DOT + relInfo.sourceFieldRpmType
			}
		}

		return targetFieldName
	}

	/**
	 * Sets the basic scope required to load a target class based on scoped source class, source field name, target class name, and current scope.
	 * Retains all other scope.
	 * If the source class is not a scoped class, returns the scope unchanged.
	 *
	 * @param targetCube String target cube
	 * @param targetTraitsMaps Map fields and traits of the target class
	 * @param sourceTraitsMap Map fields and traits of the source class
	 * @param sourceFieldName String source field name
	 * @param scope Map<String, Object> scope
	 *
	 * @return Map new scope
	 *
	 */
	private static Map getScopeRelativeToSource(NCube targetCube, String sourceFieldRpmType, String targetFieldName, Map scope)
	{
		Map newScope = new CaseInsensitiveMap(scope)

		if (targetCube.name.startsWith(RPM_ENUM))
		{
			newScope[SOURCE_FIELD_NAME] = targetFieldName
		}
		else if (targetCube.getAxis(AXIS_TRAIT).findColumn(R_SCOPED_NAME))
		{
			String newScopeKey = sourceFieldRpmType
			String oldValue = scope[newScopeKey]
			if (oldValue)
            {
				newScope[SOURCE_SCOPE_KEY_PREFIX + sourceFieldRpmType] = oldValue
			}
			newScope[newScopeKey] = targetFieldName
		}
		return newScope
	}

	private static String getDotSuffix(String value)
	{
		int lastIndexOfDot = value.lastIndexOf('.')
		return lastIndexOfDot == -1 ? value : value.substring(lastIndexOfDot + 1)
	}

	private Map getDefaultScope(String cubeName)
	{
		String type = getTypeFromCubeName(cubeName)
		Map scope = new CaseInsensitiveMap()
		scope[type] = DEFAULT_SCOPE_VALUE
		scope[EFFECTIVE_VERSION] = defaultScopeEffectiveVersion
		scope[POLICY_CONTROL_DATE] = defaultScopeDate
		scope[QUOTE_DATE] = defaultScopeDate
		return scope
	}


	private boolean hasMissingMinimumScope(VisualizerInfo visInfo)
	{
		boolean hasMissingScope = false
		String cubeName = visInfo.startCubeName
		Map scope = visInfo.scope
		String type = getTypeFromCubeName(cubeName)
		String messageSuffix = "Its default value may be changed as desired."
		String messageSuffixType = "Please replace ${DEFAULT_SCOPE_VALUE} for ${type} with an actual scope value."

		if (scope)
        {
			hasMissingScope = addMissingMinimumScope(visInfo, EFFECTIVE_VERSION, defaultScopeEffectiveVersion, messageSuffix) ?: hasMissingScope
			hasMissingScope = addMissingMinimumScope(visInfo, POLICY_CONTROL_DATE, defaultScopeDate, messageSuffix)  ?: hasMissingScope
			hasMissingScope = addMissingMinimumScope(visInfo, QUOTE_DATE, defaultScopeDate, messageSuffix) ?: hasMissingScope
			hasMissingScope = addMissingMinimumScope(visInfo, type, DEFAULT_SCOPE_VALUE, messageSuffixType) ?: hasMissingScope
		}
		else
        {
			hasMissingScope = true
			Map defaultScope = getDefaultScope(cubeName)
			visInfo.scope = defaultScope
            String msg = """\
The scope for the following scope keys was added since it was required: \
${DOUBLE_BREAK}${INDENT}${defaultScope.keySet().join(COMMA_SPACE)}${DOUBLE_BREAK}${messageSuffixType} \
The other default scope values may also be changed as desired."""
			messages << msg
		}
		return hasMissingScope
	}

	private boolean addMissingMinimumScope(VisualizerInfo visInfo, String key, String value, String messageSuffix)
	{
		Map scope = visInfo.scope
		boolean missingScope
		if (scope.containsKey(key))
        {
			if (!scope[key])
            {
				visInfo.scope[key] = value
				missingScope = true
			}
			else if (DEFAULT_SCOPE_VALUE == scope[key])
            {
				missingScope = true
			}
		}
		else
        {
			visInfo.scope[key] = value
			missingScope = true
		}

		if (missingScope)
        {
			messages << "Scope is required for ${key}. ${messageSuffix}"
		}
		return missingScope
	}

	private void getRequiredAndOptionalScopeKeys(VisualizerRelInfo relInfo)
	{
		NCube cube = relInfo.targetCube
		String cubeName = cube.name
		if (requiredScopeKeys.containsKey(cubeName))
		{
			relInfo.requiredScopeKeys = requiredScopeKeys[cubeName]
			relInfo.optionalScopeKeys = optionalScopeKeys[cubeName]
		}
		else
		{
			relInfo.requiredScopeKeys = getRequiredScope(relInfo)
			relInfo.optionalScopeKeys = cube.getOptionalScope(relInfo.scope, [:])
			requiredScopeKeys[cubeName] = relInfo.requiredScopeKeys
			optionalScopeKeys[cubeName] = relInfo.optionalScopeKeys
		}
	}

	private boolean getTraitMaps(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		try
		{
			getTraitMaps(relInfo)
		}
		catch (Exception e)
		{
			relInfo.loadFieldsAndTraits = false
			Throwable t = getDeepestException(e)
			if (t instanceof InvalidCoordinateException)
            {
				handleInvalidCoordinateException(t, visInfo, relInfo)
			}
			else if (t instanceof CoordinateNotFoundException)
            {
				handleCoordinateNotFoundException(t, relInfo)
			}
			else
            {
				handleException(t, relInfo)
			}
			return false
		}
		return true
	}

	private void handleCoordinateNotFoundException(CoordinateNotFoundException e, VisualizerRelInfo relInfo)
	{
		String cubeName = e.cubeName
		String axisName = e.axisName
		Object value = e.value ?: 'null'

		if (cubeName && axisName)
        {
            String msg = """\
The scope value ${value} for scope key ${axisName} cannot be found on axis ${axisName} in \
cube ${cubeName}${getSourceMessage(relInfo)}. Please supply a different value for ${axisName}."""
			relInfo.notes << msg
			messages << msg
			relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): SCOPE_VALUE_NOT_FOUND]]
		}
		else
        {
			handleException(e as Exception, relInfo)
		}
	}

	private static String getSourceMessage(VisualizerRelInfo relInfo)
	{
		if (relInfo.sourceTraitMaps)
		{
			String sourceScopedName = getScopedName(relInfo.sourceTraitMaps)
			return sourceScopedName ? ", the target of ${sourceScopedName} on ${relInfo.sourceCube.name}" : ""
		}
		return ''
	}

	private void handleInvalidCoordinateException(InvalidCoordinateException e, VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		String cubeName = e.cubeName
		Set requiredKeys = e.requiredKeys
		Set<String> missingScope = findMissingScope(relInfo.scope, requiredKeys)
		if (missingScope)
		{
			Map expandedScope = new LinkedHashMap(visInfo.scope)
			missingScope.each { String key ->
				expandedScope[key] = DEFAULT_SCOPE_VALUE
			}
			visInfo.scope = expandedScope
			relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): MISSING_SCOPE]]
            String msg = """\
Additional scope is required to load ${cubeName}${getSourceMessage(relInfo)}. Please add scope \
value(s) for the following scope key(s): ${missingScope.join(COMMA_SPACE)}."""
			relInfo.notes << msg
			messages << msg
		}
		else
		{
			throw new IllegalStateException("An InvalidCoordinateException was thrown, but no missing scope keys found for ${relInfo.targetCube.name} and scope ${visInfo.scope.toString()}.", e)
		}
	}

	private static Set<String> findMissingScope(Map scope, Set<String> requiredKeys)
	{
		Set<String> missingScope = []
		requiredKeys.each {String key ->
			if (!MANDATORY_RPM_SCOPE_KEYS.contains(key) && (scope == null || !scope.containsKey(key)))
			{
				missingScope << key
			}
		}
		return missingScope.size() > 0 ? missingScope : null
	}

	private void handleException(Throwable e, VisualizerRelInfo relInfo)
	{
		Throwable t = getDeepestException(e)
        String msg = """\
An exception was thrown while loading fields and traits for ${relInfo.targetCube.name}${getSourceMessage(relInfo)}. \
${DOUBLE_BREAK}<b>Message:</b> ${DOUBLE_BREAK}${e.message}${DOUBLE_BREAK}<b>Root cause: </b>\
${DOUBLE_BREAK}${t.toString()}${DOUBLE_BREAK}<b>Stack trace: </b>${DOUBLE_BREAK}${t.stackTrace.toString()}"""
		relInfo.notes << msg
		messages << msg
		relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): UNABLE_TO_LOAD]]
	}

	private static Throwable getDeepestException(Throwable e)
	{
		while (e.cause != null)
		{
			e = e.cause
		}
		return e
	}

	private void getTraitMaps(VisualizerRelInfo relInfo)
	{
		NCube cube = relInfo.targetCube
		Map scope = relInfo.scope
		relInfo.targetTraitMaps = [:]
		Map traitMaps = relInfo.targetTraitMaps
		Map output = [:]
		if (cube.name.startsWith(RPM_ENUM))
		{
			helper.loadRpmClassFields(RPM_ENUM, cube.name - RPM_ENUM_DOT, scope, traitMaps, output)
		}
		else
		{
			helper.loadRpmClassFields(RPM_CLASS, cube.name - RPM_CLASS_DOT, scope, traitMaps, output)
		}
		removeNotExistsFields(traitMaps)
		retainUsedScope(relInfo, output)
	}

	private void retainUsedScope(VisualizerRelInfo relInfo, Map output)
	{
		getRequiredAndOptionalScopeKeys(relInfo)
		Set scopeCollector = []
		scopeCollector.addAll(relInfo.requiredScopeKeys)
		scopeCollector.addAll(relInfo.optionalScopeKeys)
		scopeCollector << EFFECTIVE_VERSION_SCOPE_KEY

		if (output[NCube.RULE_EXEC_INFO])
		{
			Set keysUsed = (output[NCube.RULE_EXEC_INFO] as RuleInfo).getInputKeysUsed()
			scopeCollector.addAll(keysUsed)
		}
		relInfo.targetScope = new CaseInsensitiveMap(relInfo.scope)
		cullScope(relInfo.targetScope, scopeCollector)
	}

	private static void cullScope(Map<String, Object> scope, Set scopeCollector)
	{
		Set keySet = scope.keySet()
		Iterator<String> i = keySet.iterator()
		while (i.hasNext())
		{
			String scopeKey = i.next()
			if (!(scopeCollector.contains(scopeKey) || scopeKey.startsWith(SYSTEM_SCOPE_KEY_PREFIX)))
			{
				i.remove()
			}
		}
	}

	private static void removeNotExistsFields(Map<String, Map<String, Object>> traitMaps)
	{
		Iterator<String> i = traitMaps.keySet().iterator()
		while (i.hasNext())
		{
			String fieldName = i.next()
			if (!traitMaps[fieldName][R_EXISTS])
			{
				i.remove()
			}
		}
	}

	private static Set getRequiredScope(VisualizerRelInfo relInfo)
	{
		Set<String> requiredScope = relInfo.targetCube.getRequiredScope(relInfo.targetScope, [:] as Map)
		requiredScope.remove(AXIS_FIELD)
		requiredScope.remove(AXIS_NAME);
		requiredScope.remove(AXIS_TRAIT);
		return requiredScope;
	}

	private static String getTypeFromCubeName(String cubeName)
	{
		return (cubeName - RPM_CLASS_DOT)
	}
}