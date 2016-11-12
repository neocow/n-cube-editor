package com.cedarsoftware.util

import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.util.io.JsonWriter
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
	public static final String RPM_ENUM_DOT = 'rpm.enum.'
	public static final String CLASS_TRAITS = 'CLASS_TRAITS'
	public static final String R_RPM_TYPE = 'r:rpmType'
	public static final String V_ENUM = 'v:enum'
	public static final String R_SCOPED_NAME = 'r:scopedName'
	public static final String R_EXISTS = 'r:exists'
	public static final String V_MIN = 'v:min'
	public static final String V_MAX = 'v:max'

	public static final String SOURCE_FIELD_NAME = 'sourceFieldName'

	public static final String AXIS_FIELD = 'field'
	public static final String AXIS_NAME = 'name'
	public static final String AXIS_TRAIT = 'trait'

	public static final String _ENUM = '_ENUM'
	public static final String UNSPECIFIED = 'UNSPECIFIED'
	public static final Map ALL_GROUPS_MAP = [PRODUCT:'Product', FORM:'Form', FORMDATA:'Form data', RISK:'Risk', COVERAGE:'Coverage', CONTAINER:'Container', DEDUCTIBLE:'Deductible', LIMIT:'Limit', RATE:'Rate', RATEFACTOR:'Rate Factor', PREMIUM:'Premium', PARTY:'Party', PLACE:'Place', ROLE:'Role', ROLEPLAYER:'Role Player', UNSPECIFIED:'Unspecified']
	public static final String[] GROUPS_TO_SHOW_IN_TITLE = ['COVERAGE', 'DEDUCTIBLE', 'LIMIT', 'PREMIUM', 'PRODUCT', 'RATE', 'RATEFACTOR', 'RISK', 'ROLEPLAYER', 'ROLE']

	public static final Map DEFAULT_SCOPE = [context: 'Edit', action: 'Edit']
	public static final long DEFAULT_LEVEL = 2

	private static final String SPACE = '&nbsp;'

	public static final String SOURCE_SCOPE_KEY_PREFIX = 'source'

	private VisualizerHelper helper = new VisualizerHelper()

	public static final String STATUS_MISSING_SCOPE = 'missingScope'
	public static final String STATUS_SUCCESS = 'success'

	private List messages = []
	private Set visited = []

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
		helper.ncube = ncube

		VisualizerInfo visInfo = new VisualizerInfo()
		visInfo.startCubeName = options.startCubeName as String
		Map scope = options.scope as Map
		visInfo.scope = scope == null ? DEFAULT_SCOPE : scope
		visInfo.allGroups = ALL_GROUPS_MAP
		visInfo.availableGroupsAllLevels = []
		visInfo.groupSuffix = _ENUM
		Set selectedGroups = options.selectedGroups as Set
		visInfo.selectedGroups = selectedGroups  == null ? ALL_GROUPS_MAP.keySet() : selectedGroups
		String selectedLevel = options.selectedLevel as String
		visInfo.selectedLevel = selectedLevel == null ? DEFAULT_LEVEL :  Converter.convert(selectedLevel, long.class) as long
		visInfo.maxLevel = 1
		visInfo.nodeCount = 1
		visInfo.nodes = []
		visInfo.edges = []

		//TODO: This is a temp location for checking scope. Move check for scope to where traitMaps are obtained.
		//TODO: Also handle the user adding the required scope key but with a value that results in a no hit.
		if (hasMissingScope(visInfo))
		{
			return [status: STATUS_MISSING_SCOPE, visInfo: visInfo, message: messages.toString()]
		}

		getRpmVisualization(visInfo)

		String message = messages.size() > 0 ? messages.toString() : null

		//TODO: Remove before creating pull request. Used temporarily for regression testing.
		String jsonFolder = 'C:/json'
		new File(jsonFolder, 'visInfoNew.json').withWriter('utf-8') {
			writer ->
				writer.writeLine(JsonWriter.objectToJson(visInfo))
		}

		return [status: STATUS_SUCCESS, visInfo: visInfo, message: message]
	}

	private void getRpmVisualization(VisualizerInfo visInfo)
	{
		VisualizerRelInfo relInfo = new VisualizerRelInfo()
		relInfo.targetCube =  getCube(visInfo.startCubeName)
		relInfo.targetScope = visInfo.scope
		relInfo.targetLevel = 1
		relInfo.id = 1
		stack.push(relInfo)

		while(!stack.empty) {
			processCube(visInfo, stack.pop())
		}

		trimSelectedLevel(visInfo)
		trimSelectedGroups(visInfo)
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

		Map targetTraitMaps = relInfo.targetTraitMaps
		if (!targetTraitMaps) {
			targetTraitMaps = helper.getTraitMaps(targetCubeName, targetScope)
			relInfo.targetTraitMaps = targetTraitMaps
		}

		String group = getGroup(targetCubeName)

		addToEdges(visInfo, relInfo)

		if (!visited.add(targetCubeName + targetScope.toString()))
		{
			return
		}

		visInfo.availableGroupsAllLevels << group
		addToNodes(visInfo, relInfo, group)

		targetTraitMaps.each{ k, v ->
			String targetFieldName = k as String
			Map targetTraits = v as Map
			Boolean exists = targetTraits[R_EXISTS]
			if (!CLASS_TRAITS.equals(targetFieldName) && exists)
			{
				String targetFieldRpmType = targetTraits[R_RPM_TYPE]

				if (!helper.isPrimitive(targetFieldRpmType))
				{
					NCube nextTargetCube = null
					if (targetTraits.containsKey(V_ENUM))
					{
						nextTargetCube = getCube(RPM_ENUM_DOT + targetTraits[V_ENUM])
					}
					else if (targetFieldRpmType)
					{
						nextTargetCube = getCube(RPM_CLASS_DOT + targetFieldRpmType)
					}

					if (nextTargetCube)
					{
						addToStack(visInfo, relInfo, nextTargetCube, targetFieldRpmType, targetFieldName)
					}
					else{
						messages << 'No cube exists with name of ' + RPM_ENUM_DOT + nextTargetCube.name + '. It is therefore not included in the visualization.'
					}
				}
			}
		}
	}

	private void processEnumCube(VisualizerInfo visInfo, VisualizerRelInfo relInfo)
	{
		Map targetScope = relInfo.targetScope
		String targetCubeName = relInfo.targetCube.name
		String sourceFieldRpmType =  relInfo.sourceFieldRpmType

		if (!targetCubeName.startsWith(RPM_ENUM)) {
			throw new IllegalStateException('Cube is not an rpm.enum cube: ' + targetCubeName +  '.')
		}

		if(relInfo.sourceCube && (!sourceFieldRpmType || helper.isPrimitive(sourceFieldRpmType)))
		{
			return
		}

		Map targetTraitMaps = relInfo.targetTraitMaps
		if (!targetTraitMaps) {
			targetTraitMaps = helper.getTraitMaps(targetCubeName, targetScope)
			relInfo.targetTraitMaps = targetTraitMaps
		}

		String group = UNSPECIFIED

		targetTraitMaps.each { k, v ->
			String targetFieldName = k as String
			Map targetTraits = v as Map
			Boolean exists = targetTraits[R_EXISTS]
			if (!CLASS_TRAITS.equals(targetFieldName) && exists)
			{
				try
				{
					String nextTargetCubeName = getNextTargetCubeName(relInfo, targetFieldName)

					if (nextTargetCubeName)
					{
						NCube nextTargetCube = getCube(RPM_CLASS_DOT + nextTargetCubeName)
						if (nextTargetCube)
						{
							VisualizerRelInfo nextTargetRelInfo = addToStack(visInfo, relInfo, nextTargetCube, relInfo.sourceFieldRpmType, targetFieldName)

							if (group == UNSPECIFIED) {
								group = getGroup(nextTargetCubeName)
							}
						}
						else{
							messages << 'No cube exists with name of ' + RPM_ENUM_DOT + nextTargetCubeName + '. It is therefore not included in the visualization.'
						}
					}
				}
				catch (Exception e)
				{
					throw new IllegalStateException('Exception caught while loading and processing the cube for enum field ' + targetFieldName + ' in enum ' + targetCubeName + '.', e)
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
			nextRelInfo.targetTraitMaps =  helper.getTraitMaps(nextTargetCube.name, nextRelInfo.targetScope)

			nextRelInfo.sourceCube = nextSourceCube
			nextRelInfo.sourceScope = relInfo.targetScope
			nextRelInfo.sourceTraitMaps = relInfo.targetTraitMaps
			nextRelInfo.sourceFieldName = targetFieldName
			nextRelInfo.sourceFieldRpmType = rpmType

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
			throw new IllegalStateException('Exception caught while loading and processing the class for field ' + relInfo.sourceFieldName + ' in class ' + nextTargetCube.name + '.', e)
		}
	}

	private static String getNodeGroup(NCube cube, String group)
	{
		if (cube.name.startsWith(RPM_ENUM))
		{
			return group + _ENUM
		}
		else
		{
			return group
		}
	}

	private static String getGroup(String cubeName)
	{
		Iterable<String> splits = Splitter.on('.').split(cubeName)
		String group = splits[splits.size()-1].toUpperCase()
		Set groups = ALL_GROUPS_MAP.keySet()
		return groups.contains(group) ? group : UNSPECIFIED
	}

	private static void addToEdges(VisualizerInfo visInfo, VisualizerRelInfo relInfo )
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
		edgeMap.from = sourceCube.name + '_' + relInfo.sourceScope.toString()
		edgeMap.to = targetCube.name + '_' + relInfo.targetScope.toString()
		edgeMap.fromName = sourceCubeEffectiveName
		edgeMap.toName = targetCubeEffectiveName
		edgeMap.fromFieldName = sourceFieldName
		edgeMap.level = String.valueOf(relInfo.targetLevel)
		edgeMap.label = ''
		String vMin = sourceTraitMaps[sourceFieldName][V_MIN]
		String vMax = sourceTraitMaps[sourceFieldName][V_MAX]
		edgeMap.title = ''
		if (vMin != null && vMax != null) {
			edgeMap.title = vMin + ':' + vMax
		}
		else{
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
		String nodeGroup = getNodeGroup(targetCube, group)
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
		nodeMap.id = targetCube.name + '_' + targetScope.toString()
		nodeMap.scope = targetScope.toString()
		nodeMap.level = String.valueOf(relInfo.targetLevel)
		nodeMap.name = targetCubeName
		nodeMap.fromFieldName = sourceFieldName == null ? null : sourceFieldName

		nodeMap.label = sb.toString()
		nodeMap.title = targetCubeName
		nodeMap.desc = getTitle(targetCube, getScopedName(targetTraitMaps), targetScope, targetTraitMaps, nodeMap)
		nodeMap.group = nodeGroup
		visInfo.nodes << nodeMap
	}

	private String getTitle(NCube targetCube, String scopedName, Map scope, Map traitMaps, Map nodeMap)
	{
		StringBuilder sb = new StringBuilder()

		if (scopedName != null )
		{
			sb.append('<strong>scoped name = </strong>' + scopedName + '<br><br>')
		}

		sb.append('<strong>scope = </strong>' + scope.toString().replace('{','').replace('}','') + '<br><br>')

		String requiredScope = getRequiredScopeAllReferenced(targetCube).toString().replace('[','').replace(']','')
		sb.append(requiredScope == '' ? '' : '' + '<strong>required scope = </strong>' + requiredScope  + '<br><br>')

		String optionalScope = getOptionalScopeAllReferenced(targetCube).toString().replace('[','').replace(']','')
		sb.append(optionalScope == '' ? '' : '' + '<strong>optional scope = </strong>' + optionalScope  + '<br><br>')

		sb.append('<strong>level = </strong>' + nodeMap.level.toString() + '<br><br>')

		sb.append(getTitleFields(traitMaps))

		return sb.toString();
	}


	private static String getTitleFields(Map traitMaps)
	{
		Map fields = getFields(traitMaps)
		String fieldString = "<strong>fields = </strong>";
		return fieldString + getFieldDetails(fields, SPACE)
	}


	private static String getFieldDetails(Map fields, String spaces)
	{
		StringBuilder fieldDetails = new StringBuilder()
		fieldDetails.append('')

		fields.each() { key, value ->

			fieldDetails.append('<br>' + spaces + key + SPACE)
		}

		return fieldDetails.toString();
	}

	private static String getEffectiveName(NCube cube, Map traitMaps)
	{
		String scopedName = getScopedName(traitMaps)
		return scopedName == null ? cube.name : scopedName
	}

	private static String getScopedName(Map traitMaps)
	{
		String classTraitsTraitMap = traitMaps[CLASS_TRAITS]

		if (classTraitsTraitMap)
		{
			return traitMaps[CLASS_TRAITS][R_SCOPED_NAME]
		}

		return null
	}

	private static Map getFields(Map traitMaps)
	{
		Map fieldInfo = [:] as TreeMap
		traitMaps.each{ fieldName, traits ->
			Boolean exists = traits[R_EXISTS] as String
			if (!CLASS_TRAITS.equals(fieldName) && exists)
			{
				Map map = [:]
				map['traits'] = getTraits(traits as Map)
				fieldInfo[fieldName] = map
			}
		}
		return fieldInfo
	}

	private static String getTraits(Map traits)
	{
		StringBuilder traitInfo = new StringBuilder()
		traits.each{ traitName, traitValue ->
			if (!'null'.equals(traitValue))
			{
				traitInfo.append((traitName as String) + '=' + (traitValue as String))
				traitInfo.append(', ')
			}
		}
		String traitInfoString = traitInfo.toString()
		if (traitInfoString.endsWith(', '))
		{
			traitInfoString = traitInfoString.substring(0, traitInfoString.size() - 2)
		}

		return traitInfoString
	}

	private static String getNextTargetCubeName(VisualizerRelInfo relInfo, String targetFieldName)
	{
		if (relInfo.sourceCube.getAxis(AXIS_TRAIT).contains(R_SCOPED_NAME))
		{
			if (relInfo.sourceTraitMaps[CLASS_TRAITS][R_SCOPED_NAME] == null)
			{
				return null
			}
			else
			{
				return relInfo.sourceFieldRpmType
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
		Map<String, Object> newScope = new CaseInsensitiveMap<>(scope)

		if (targetCube.name.startsWith(RPM_ENUM))
		{
			newScope[SOURCE_FIELD_NAME] = targetFieldName
		}
		else if (targetCube.getAxis(AXIS_TRAIT).contains(R_SCOPED_NAME))
		{
			String newScopeKey = sourceFieldRpmType.toLowerCase()
			String oldValue = scope[newScopeKey]
			if (oldValue){
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
		if (lastIndexOfDot == -1) {
			return value
		}

		return value.substring(lastIndexOfDot + 1);
	}

	private boolean hasMissingScope(VisualizerInfo visInfo ) {
		String startCubeName = visInfo.startCubeName
		Map scope = visInfo.scope
		Set<String> missingScope = findMissingScope(startCubeName, scope)
		if (missingScope) {
			Map expandedScope = scope == null ? new CaseInsensitiveMap(DEFAULT_SCOPE) : new CaseInsensitiveMap(scope)
			String missingScopeString = ''
			missingScope.each {String key ->
				expandedScope[key] = '????'
				missingScopeString = missingScopeString + key + ', '
			}
			missingScopeString = missingScopeString.substring(0, missingScopeString.size() - 2)
			messages << 'Loading the visualization for ' + startCubeName + ' requires additional scope to be provided. Please add scope value(s) for the following scope key(s): ' + missingScopeString + '.'
			visInfo.scope = expandedScope
			return true
		}
		return false
	}

	private Set findMissingScope(String startCubeName, Map scope) {
		NCube startCube = getCube(startCubeName)

		Set<String> requiredScope = getRequiredScopeAllReferenced(startCube)
		Set missingScope = []
		requiredScope.each {
			if (scope == null || !scope.containsKey(it)) {
				missingScope << it
			}
		}

		return missingScope.size() > 0 ? missingScope : null
	}

	private Set getRequiredScopeAllReferenced(NCube startCube) {
		Set requiredScope = getRequiredScope(startCube)
		startCube.referencedCubeNames.each {
			NCube refCube = getCube(it)
			requiredScope.addAll(getRequiredScope(refCube))
		}
		requiredScope
	}

	private Set getOptionalScopeAllReferenced(NCube startCube) {
		Set optionalScope = startCube.getOptionalScope([:] as Map, [:] as Map)
		startCube.referencedCubeNames.each {
			NCube refCube = getCube(it)
			optionalScope.addAll(refCube.getOptionalScope([:] as Map, [:] as Map))
		}
		optionalScope
	}

	private static Set getRequiredScope(NCube cube)
	{
		Set<String> requiredScope = cube.getRequiredScope([:] as Map, [:] as Map)

		requiredScope.remove(AXIS_FIELD);
		requiredScope.remove(AXIS_NAME);
		requiredScope.remove(AXIS_TRAIT);
		return requiredScope;
	}
}