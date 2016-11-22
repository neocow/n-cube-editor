package com.cedarsoftware.util

import com.cedarsoftware.ncube.Column
import com.cedarsoftware.ncube.NCube
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
	public static final String RPM_SCOPE = 'rpm.scope'
	public static final String RPM = 'rpm'
	public static final String DOT_CLASS_DOT = '.class.'
	public static final String DOT_ENUM_DOT = '.enum.'
	public static final List RPM_PREFIXES = [RPM, RPM_SCOPE]
	public static final String CLASS_TRAITS = 'CLASS_TRAITS'
	public static final String DOT_CLASS_TRAITS = '.classTraits'
	public static final String R_RPM_TYPE = 'r:rpmType'
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
	public static final List DERIVED_SCOPE_KEYS = ['product', 'form', 'risk', 'coverage', 'container', 'deductible', 'limit', 'rate', 'ratefactor', 'premium', 'party', 'place', 'role', 'roleplayer']
	public static final List DERIVED_SOURCE_SCOPE_KEYS = ['sourceProduct', 'sourceForm', 'sourceRisk', 'sourceCoverage', 'sourceContainer', 'sourceDeductible', 'sourceLimit', 'sourceRate', 'sourceRate Factor', 'sourcePremium', 'sourceParty', 'sourcePlace', 'sourceRole', 'sourceRolepayer']
	public static final String SOURCE_SCOPE_KEY_PREFIX = 'source'

	public static final List DEFAULT_OPTIONAL_SCOPE_KEYS = ['action', 'businessDivisionCode', 'context', 'Coverage', 'date', 'LocationState', 'screen', 'state', 'transaction', 'transactionsubtype']
	public static final List DEFAULT_AVAILABLE_SCOPE_KEYS = DERIVED_SCOPE_KEYS + DERIVED_SOURCE_SCOPE_KEYS + DEFAULT_OPTIONAL_SCOPE_KEYS + [POLICY_CONTROL_DATE, QUOTE_DATE, EFFECTIVE_VERSION]
	public static final String DEFAULT_SCOPE_VALUE = '????'
	public static final long DEFAULT_LEVEL = 2

	private static final String SPACE = '&nbsp;'
	private static final String INDENT = "${SPACE}${SPACE}${SPACE}"
	private static final String BREAK = '<br>'
	private static final String COMMA_SPACE = ', '
	private static final String DOUBLE_BREAK = "${BREAK}${BREAK}"

	private VisualizerHelper helper = new VisualizerHelper()
	static final SafeSimpleDateFormat DATE_TIME_FORMAT = new SafeSimpleDateFormat('yyyy-MM-dd')

	public static final List MANDATORY_RPM_SCOPE_KEYS = [AXIS_FIELD, AXIS_NAME, AXIS_TRAIT]
	public static final String MISSING_SCOPE = 'missing scope'
	public static final String UNABLE_TO_LOAD = 'unable to load'
	public static final String SCOPE_VALUE_NOT_FOUND = 'scope value not found'

	public static final String STATUS_MISSING_START_SCOPE = 'missingStartScope'
	public static final String STATUS_SUCCESS = 'success'

	private Set messages = []
	private Set visited = []
	Map<String, Set> requiredScopeKeys = [:]
	Map<String, Set> optionalScopeKeys = [:]
	String defaultScopeEffectiveVersion
	String defaultScopeDate
	Deque<VisualizerRelInfo> stack = new ArrayDeque<>()

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
		visInfo.scope = options.scope as LinkedHashMap
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

		if (hasMissingMinimumScope(visInfo)) {
			return [status: STATUS_MISSING_START_SCOPE, visInfo: visInfo, message: String.join(DOUBLE_BREAK, messages)]
		}

		getRpmVisualization(visInfo)

		String message = messages.size() > 0 ? String.join(DOUBLE_BREAK, messages) : null
		return [status: STATUS_SUCCESS, visInfo: visInfo, message: message]
	}

	private void getRpmVisualization(VisualizerInfo visInfo)
	{
		VisualizerRelInfo relInfo = new VisualizerRelInfo()
		relInfo.targetCube = getCube(visInfo.startCubeName)
		relInfo.targetScope = visInfo.scope
		relInfo.targetLevel = 1
		relInfo.id = 1
		stack.push(relInfo)

		while (!stack.empty) {
			processCube(visInfo, stack.pop())
		}

		addSets(visInfo.availableScopeKeys, requiredScopeKeys.values() as Set)
		addSets(visInfo.availableScopeKeys, optionalScopeKeys.values() as Set)

		trimSelectedLevel(visInfo)
		trimSelectedGroups(visInfo)
	}

	private static Set addSets(Set<String> set, Set<Set> sets) {
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
		Map targetScope = relInfo.targetScope
		Map targetTraitMaps = [:]

		boolean loadFieldsAndTraits = canLoadTargetAsRpmClass(relInfo)
		if (loadFieldsAndTraits)
		{
			loadFieldsAndTraits = getTraitMaps(visInfo, relInfo, targetTraitMaps)
		}

		addToEdges(visInfo, relInfo)

		if (!visited.add(targetCubeName + targetScope.toString()))
		{
			return
		}

		String group = getGroup(targetCubeName)
		visInfo.availableGroupsAllLevels << group
		addToNodes(visInfo, relInfo, group)

		if (loadFieldsAndTraits)
		{
			targetTraitMaps.each { k, v ->
				String targetFieldName = k as String
				Map targetTraits = v as Map
				if (CLASS_TRAITS != targetFieldName)
				{
					String targetFieldRpmType = targetTraits[R_RPM_TYPE]

					if (!helper.isPrimitive(targetFieldRpmType))
					{
						NCube nextTargetCube
						String nextTargetCubeName
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
						else {
							messages << "No cube exists with name of ${nextTargetCubeName}. It is therefore not included in the visualization."
						}
					}
				}
			}
		}
	}

	private void processEnumCube(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		String group = UNSPECIFIED
		Map targetTraitMaps = [:]
		Map targetScope = relInfo.targetScope
		String targetCubeName = relInfo.targetCube.name
		String sourceFieldRpmType = relInfo.sourceFieldRpmType

		if (!targetCubeName.startsWith(RPM_ENUM)) {
			throw new IllegalStateException("Cube is not an rpm.enum cube: ${targetCubeName}.")
		}

		if (relInfo.sourceCube && (!sourceFieldRpmType || helper.isPrimitive(sourceFieldRpmType)))
		{
			return
		}

		boolean loadFieldsAndTraits = getTraitMaps(visInfo, relInfo, targetTraitMaps)

		if (loadFieldsAndTraits) {
			targetTraitMaps.each { k, v ->
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

								if (group == UNSPECIFIED) {
									group = getGroup(nextTargetCubeName)
								}
							}
							else {
								messages << "No cube exists with name of ${nextTargetCubeName}. It is therefore not included in the visualization."
							}
						}
					}
					catch (Exception e)
					{
						throw new IllegalStateException("Exception caught while loading and processing the cube for enum field ${targetFieldName} in enum ${targetCubeName}.", e)
					}
				}
			}
		}

		addToEdges(visInfo, relInfo)

		if (!visited.add(targetCubeName + targetScope.toString()))
		{
			return
		}

		visInfo.availableGroupsAllLevels << group
		addToNodes(visInfo, relInfo, group)

	}

	private static void trimSelectedLevel(VisualizerInfo visInfo) {
		long nodeCount = visInfo.nodeCount
		long selectedLevel = visInfo.selectedLevel
		visInfo.selectedLevel = selectedLevel.compareTo(nodeCount) > 0 ? nodeCount : selectedLevel
	}

	private static void trimSelectedGroups(VisualizerInfo visInfo) {
		visInfo.selectedGroups = visInfo.availableGroupsAllLevels.intersect(visInfo.selectedGroups)
	}

	private VisualizerRelInfo addToStack(VisualizerInfo visInfo, VisualizerRelInfo relInfo, NCube nextTargetCube, String rpmType, String targetFieldName)
	{
		try
		{
			NCube nextSourceCube = relInfo.targetCube

			VisualizerRelInfo nextRelInfo = new VisualizerRelInfo()
			nextRelInfo.targetCube = nextTargetCube
			nextRelInfo.targetScope = getScopeRelativeToSource(nextTargetCube, rpmType, targetFieldName, relInfo.targetScope)
			nextRelInfo.sourceCube = nextSourceCube
			nextRelInfo.sourceScope = relInfo.targetScope
			nextRelInfo.sourceFieldName = targetFieldName
			nextRelInfo.sourceFieldRpmType = rpmType
			nextRelInfo.sourceTraitMaps = relInfo.targetTraitMaps

			long nextTargetTargetLevel = relInfo.targetLevel + 1
			nextRelInfo.targetLevel = nextTargetTargetLevel

			long maxLevel = visInfo.maxLevel
			visInfo.maxLevel = maxLevel.compareTo(nextTargetTargetLevel) < 0 ? nextTargetTargetLevel : maxLevel
			visInfo.nodeCount += 1
			nextRelInfo.id = visInfo.nodeCount

			stack.push(nextRelInfo)
			return nextRelInfo
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Exception caught while loading and processing the class for field ${relInfo.sourceFieldName} in class ${nextTargetCube.name}.", e)
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
			String type = getTypeFromCubeName(targetCube.name)
			NCube classTraitsCube = getCube(RPM_SCOPE_CLASS_DOT + type + DOT_CLASS_TRAITS)
			List columns = classTraitsCube.getAxis(type).columns
			String sourceFieldName = relInfo.sourceFieldName
			if (!columns.find { sourceFieldName == (it as Column).value }) {
				relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): UNABLE_TO_LOAD]]
				StringBuilder sb = new StringBuilder()
				sb.append('The source cube ')
				sb.append(sourceCube.name)
				sb.append(' points directly to this cube (')
				sb.append(targetCube.name )
				sb.append(') via field ')
				sb.append(sourceFieldName)
				sb.append(', but there is no ')
				sb.append(type.toLowerCase())
				sb.append(' named ')
				sb.append(sourceFieldName)
				sb.append(' on this cube.')
				sb.append(DOUBLE_BREAK)
				sb.append('It can therefore not be loaded as an rpm.class in the visualization.')
				relInfo.notes << sb.toString()
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
		edgeMap.id = String.valueOf(relInfo.id)
		edgeMap.from = "${sourceCube.name}_${relInfo.sourceScope.toString()}".toString()
		edgeMap.to = "${targetCube.name}_${relInfo.targetScope.toString()}".toString()
		edgeMap.fromName = sourceCubeEffectiveName
		edgeMap.toName = targetCubeEffectiveName
		edgeMap.fromFieldName = sourceFieldName
		edgeMap.level = String.valueOf(relInfo.targetLevel)
		edgeMap.label = ''
		String vMin = sourceTraitMaps[sourceFieldName][V_MIN]
		String vMax = sourceTraitMaps[sourceFieldName][V_MAX]
		edgeMap.title = ''
		if (vMin != null && vMax != null) {
			edgeMap.title = "${vMin}:${vMax}"
		}
		else {
			edgeMap.title = ''
		}
		visInfo.edges << edgeMap
	}

	private void addToNodes(VisualizerInfo visInfo, VisualizerRelInfo relInfo, String group)
	{
		NCube targetCube = relInfo.targetCube
		String targetCubeName = targetCube.name
		Map targetTraitMaps = relInfo.targetTraitMaps
		Map targetScope = relInfo.targetScope
		String sourceFieldName = relInfo.sourceFieldName

		int maxLineLength = 16
		String nodeGroup = getNodeGroup(targetCubeName, group)
		StringBuilder sb = new StringBuilder()
		if (GROUPS_TO_SHOW_IN_TITLE.contains(nodeGroup))
		{
			String label = ALL_GROUPS_MAP[nodeGroup]
			int len = label.length()
			sb.append(label)
			sb.append('\n')
			sb.append('-'.multiply(Math.floor(len * 1.2)))
			sb.append('\n')
		}

		String labelName = getDotSuffix(getEffectiveName(targetCube, targetTraitMaps))
		String[] splitName = labelName.split("(?=\\p{Upper})")
		String line = ''
		for (String part : splitName)
		{
			if (line.length() + part.length() < maxLineLength)
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

		Map nodeMap = [:]
		nodeMap.id = "${targetCube.name}_${targetScope.toString()}".toString()
		nodeMap.scope = targetScope.toString()
		nodeMap.level = String.valueOf(relInfo.targetLevel)
		nodeMap.name = targetCubeName
		nodeMap.fromFieldName = sourceFieldName == null ? null : sourceFieldName

		nodeMap.label = sb.toString()
		nodeMap.title = targetCubeName
		nodeMap.desc = getTitle(relInfo, nodeMap)
		nodeMap.group = nodeGroup
		visInfo.nodes << nodeMap
	}

	private String getTitle(VisualizerRelInfo relInfo, Map nodeMap)
	{
		boolean loadFieldsAndTraits = relInfo.loadFieldsAndTraits
		Map scope = relInfo.targetScope
		Map traitMaps = relInfo.targetTraitMaps
		Set notes = relInfo.notes
		String scopedName = getScopedName(traitMaps)

		StringBuilder sb = new StringBuilder()

		//Scoped Name
		if (scopedName) {
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
		sb.append('<b>scope = </b>')
		sb.append(scope.toString() - '{' - '}')
		sb.append(DOUBLE_BREAK)

		//Required scope
		getRequiredAndOptionalScopeKeys(relInfo)
		String cubeName = relInfo.targetCube.name
		String requiredScope = String.join(COMMA_SPACE, requiredScopeKeys[cubeName])
		sb.append('<b>required scope to access all cells for cube = </b>')
		sb.append(requiredScope)
		sb.append(DOUBLE_BREAK)

		//Optional scope
		String optionalScope = String.join(COMMA_SPACE, optionalScopeKeys[cubeName])
		sb.append('<b>optional scope for cube = </b>')
		sb.append(optionalScope)
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
			if (CLASS_TRAITS != fieldName) {
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
	private static Map getScopeRelativeToSource(NCube targetCube, String sourceFieldRpmType, String targetFieldName, Map<String, Object> scope)
	{
		Map<String, Object> newScope = new LinkedHashMap(scope)

		if (targetCube.name.startsWith(RPM_ENUM))
		{
			newScope[SOURCE_FIELD_NAME] = targetFieldName
		}
		else if (targetCube.getAxis(AXIS_TRAIT).findColumn(R_SCOPED_NAME))
		{
			String newScopeKey = sourceFieldRpmType.toLowerCase()
			String oldValue = scope[newScopeKey]
			if (oldValue) {
				newScope[SOURCE_SCOPE_KEY_PREFIX + sourceFieldRpmType] = oldValue
			}
			newScope[newScopeKey] = targetFieldName
		}
		else
		{
			return scope
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
		String type = getTypeFromCubeName(cubeName).toLowerCase()
		return [(type): DEFAULT_SCOPE_VALUE,
				(EFFECTIVE_VERSION): defaultScopeEffectiveVersion,
				(POLICY_CONTROL_DATE): defaultScopeDate,
				(QUOTE_DATE): defaultScopeDate]
	}


	private boolean hasMissingMinimumScope(VisualizerInfo visInfo)
	{
		boolean hasMissingScope
		String cubeName = visInfo.startCubeName
		Map scope = visInfo.scope
		String type = getTypeFromCubeName(cubeName).toLowerCase()

		String messageSuffix = "Its default value may be changed as desired."
		String messageSuffixType = "Please replace ${DEFAULT_SCOPE_VALUE} for ${type} with an actual scope value."

		if (scope) {
			hasMissingScope = addMissingScope(visInfo, EFFECTIVE_VERSION, defaultScopeEffectiveVersion, messageSuffix) ?: hasMissingScope
			hasMissingScope = addMissingScope(visInfo, POLICY_CONTROL_DATE, defaultScopeDate, messageSuffix)  ?: hasMissingScope
			hasMissingScope = addMissingScope(visInfo, QUOTE_DATE, defaultScopeDate, messageSuffix) ?: hasMissingScope
			hasMissingScope = addMissingScope(visInfo, type, DEFAULT_SCOPE_VALUE, messageSuffixType) ?: hasMissingScope
		}
		else{
			hasMissingScope = true
			Map<String, String> defaultScope = getDefaultScope(cubeName)
			visInfo.scope = defaultScope
			StringBuilder sb = new StringBuilder()
			sb.append("The scope for the following scope keys was added since it is required: ")
			sb.append(DOUBLE_BREAK)
			sb.append(INDENT)
			sb.append(String.join(COMMA_SPACE, defaultScope.keySet()))
			sb.append(DOUBLE_BREAK)
			sb.append(messageSuffixType)
			sb.append(" The other default scope values may also be changed as desired.")
			messages << sb.toString()
		}
		return hasMissingScope
	}


	private boolean addMissingScope(VisualizerInfo visInfo, String key, String value, String messageSuffix)
	{
		Map scope = visInfo.scope
		boolean missingScope
		if (scope.containsKey(key)){
			if (!scope[key]){
				visInfo.scope[key] = value
				missingScope = true
			}
			else if (DEFAULT_SCOPE_VALUE == scope[key]){
				missingScope = true
			}
		}
		else{
			visInfo.scope[key] = value
			missingScope = true
		}

		if (missingScope){
			messages << "Scope is required for ${key}. ${messageSuffix}"
		}
		return missingScope
	}

	private void getRequiredAndOptionalScopeKeys(VisualizerRelInfo relInfo)
	{
		String cubeName = relInfo.targetCube.name
		if (!requiredScopeKeys.containsKey(cubeName))
		{
			if (isRpmClassOrScopeCube(cubeName))
			{
				List<Set> scopeKeys = getScopeAllReferenced(relInfo.targetCube, [[] as Set, [] as Set] as LinkedList)
				requiredScopeKeys[cubeName] = scopeKeys.get(0)
				optionalScopeKeys[cubeName] = scopeKeys.get(1)
			}
			else
			{
				requiredScopeKeys[cubeName] = [] as Set
				optionalScopeKeys[cubeName] = [] as Set
			}
		}
	}

	private static boolean isRpmClassOrScopeCube(String cubeName)
	{
		String cubeNamePrefix = Splitter.on(DOT_CLASS_DOT).split(cubeName).first()
		cubeNamePrefix = cubeNamePrefix ?: Splitter.on(DOT_ENUM_DOT).split(cubeName).first()
		return RPM_PREFIXES.contains(cubeNamePrefix)
	}

	private boolean getTraitMaps(VisualizerInfo visInfo, VisualizerRelInfo relInfo, Map traitMaps)
	{
		String cubeName = relInfo.targetCube.name
		try {
			helper.getTraitMaps(cubeName, relInfo.targetScope, traitMaps)
			relInfo.targetTraitMaps = traitMaps
		}
		catch (Exception e)
		{
			Throwable t = getDeepestException(e)
			if (t instanceof InvalidCoordinateException ) {
				handleInvalidCoordinateException(t, visInfo, relInfo)
			}
			else if (t instanceof CoordinateNotFoundException ) {
				handleCoordinateNotFoundException(t, relInfo)
			}
			else{
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

		if (cubeName && axisName) {
			StringBuilder sb = new StringBuilder()
			sb.append('The scope value ')
			sb.append(value)
			sb.append(' for scope key ')
			sb.append(axisName)
			sb.append(' cannot be found on axis ')
			sb.append(axisName)
			sb.append(' on ')
			sb.append(cubeName)
			sb.append(getSourceMessage(relInfo))
			sb.append('.')
			sb.append(' Please supply a different value for ')
			sb.append(axisName)
			sb.append('.')
			String message = sb.toString()
			relInfo.notes << message
			messages << message
			relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): SCOPE_VALUE_NOT_FOUND]]
			relInfo.loadFieldsAndTraits = false
		}
		else{
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
		Set<String> missingScope = findMissingScope(relInfo.targetScope, requiredKeys)
		if (missingScope)
		{
			Map expandedScope = new LinkedHashMap(visInfo.scope)
			missingScope.each { String key ->
				expandedScope[key] = DEFAULT_SCOPE_VALUE
			}
			visInfo.scope = expandedScope
			relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): MISSING_SCOPE]]
			StringBuilder sb = new StringBuilder()
			sb.append('Additional scope is required to load ')
			sb.append(cubeName)
			sb.append(getSourceMessage(relInfo))
			sb.append('. Please add scope value(s) for the following scope key(s): ')
			sb.append(String.join(COMMA_SPACE, missingScope))
			sb.append('.')
			String message = sb.toString()
			relInfo.notes << message
			messages << message
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
		StringBuilder sb = new StringBuilder()
		sb.append('An exception was thrown while loading fields and traits for ')
		sb.append(relInfo.targetCube.name)
		sb.append(getSourceMessage(relInfo))
		sb.append('.')
		sb.append(DOUBLE_BREAK)
		sb.append('<b>Message:</b> ')
		sb.append(DOUBLE_BREAK)
		sb.append(e.message)
		sb.append(DOUBLE_BREAK)
		sb.append('<b>Root cause: </b>')
		sb.append(DOUBLE_BREAK)
		sb.append(t.toString())
		sb.append(DOUBLE_BREAK)
		sb.append('<b>Stack trace: </b>')
		sb.append(DOUBLE_BREAK)
		sb.append(t.stackTrace.toString())
		String message = sb.toString()
		relInfo.notes << message
		messages << message
		relInfo.targetTraitMaps = [(CLASS_TRAITS): [(R_SCOPED_NAME): UNABLE_TO_LOAD]]
		relInfo.loadFieldsAndTraits = false
	}

	private static Throwable getDeepestException(Throwable e)
	{
		while (e.cause != null)
		{
			e = e.cause
		}
		return e
	}

	private List getScopeAllReferenced(NCube cube, List<Set> scopeKeys)
	{
		Set requiredScopeKeys = scopeKeys.get(0)
		requiredScopeKeys.addAll(getRequiredScope(cube))
		Set optionalScopeKeys = scopeKeys.get(1)
		optionalScopeKeys.addAll(cube.getOptionalScope([:] as Map, [:] as Map))
		cube.referencedCubeNames.each {
			NCube refCube = getCube(it)
			scopeKeys.addAll(getScopeAllReferenced(refCube, scopeKeys))
		}
		return scopeKeys
	}

	private static Set getRequiredScope(NCube cube)
	{
		Set<String> requiredScope = cube.getRequiredScope([:] as Map, [:] as Map)

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