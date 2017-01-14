package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.NCubeResourcePersister
import com.cedarsoftware.ncube.ReleaseStatus
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.exception.InvalidCoordinateException
import groovy.transform.CompileStatic
import org.junit.Before
import org.junit.Test

@CompileStatic
class VisualizerTest{

    Visualizer visualizer
    ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, 'VISUALIZER.TEST', ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name(), ApplicationID.HEAD)

    @Before
    void beforeTest(){
        visualizer = new Visualizer()
        NCubeManager.NCubePersister = new NCubeResourcePersister()
    }

    @Test
    void testBuildGraph_checkVisInfo()
    {
        Map scope = null
        String startCubeName = 'CubeWithRefs'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        VisualizerInfo visInfo = graphInfo.visInfo as VisualizerInfo
        assert null == visInfo.messages

        assert 5 == visInfo.nodes.size()
        assert 4 == visInfo.edges.size()
        assert [:] as CaseInsensitiveMap == visInfo.scope
        assert 3l == visInfo.maxLevel
        assert 6l == visInfo.nodeCount
        assert 5l == visInfo.relInfoCount
        assert 999999l == visInfo.defaultLevel
        assert [:] == visInfo.availableScopeValues
        assert '' == visInfo.groupSuffix
        assert ['NCUBE'] as Set == visInfo.availableGroupsAllLevels
        assert [:] as CaseInsensitiveMap == visInfo.availableScopeValues

        assert [NCUBE: 'n-cube',
                RULE_NCUBE: 'rule cube',
                UNSPECIFIED: 'Unspecified'] == visInfo.allGroups

        assert ['NCUBE',
                'RULE_NCUBE',
                'UNSPECIFIED'] as Set == visInfo.allGroupsKeys

        assert [CubeWithRefs: [] as Set,
                CubeWithNoDefaultsAndNoValues: ['CubeJAxis1', 'CubeJAxis2'] as Set,
                CubeHasTwoRefsToSameCube: [] as Set] == visInfo.requiredScopeKeys

        assert [CubeWithRefs: ['CubeDAxis1', 'CubeDAxis2'] as Set,
                CubeWithNoDefaultsAndNoValues: [] as Set,
                CubeHasTwoRefsToSameCube: ['CubeEAxis1', 'CubeEAxis2'] as Set] == visInfo.optionalScopeKeys

        assert [('n-cube'): ['n-cube', 'rule cube'],
                ('rule cube'): ['n-cube', 'rule cube']] == visInfo.typesToAddMap

        //Spot check the network overrides
        Map basic =  visInfo.networkOverridesBasic
        Map full =   visInfo.networkOverridesFull
        Map topNode =   visInfo.networkOverridesTopNode
        assert ['interaction', 'nodes', 'edges', 'physics', 'layout', 'groups'] as Set == basic.keySet()
        assert ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set == (basic.groups as Map).keySet()
        assert ['nodes', 'edges'] as Set == full.keySet()
        assert true == ((full.nodes as Map).shadow as Map).enabled
        assert ['shapeProperties', 'value', 'scaling', 'borderWidth', 'size', 'color'] as Set == topNode.keySet()
        assert true == (topNode.shapeProperties as Map).useBorderWithImage
    }

    @Test
    void testBuildGraph_checkNodeAndEdgeInfo()
    {
        Map scope = null
        String startCubeName = 'CubeWithRefs'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        VisualizerInfo visInfo = graphInfo.visInfo as VisualizerInfo
        assert null == visInfo.messages
        assert 5 == visInfo.nodes.size()
        assert 4 == visInfo.edges.size()

        //Check top node
        Map node = visInfo.nodes.find { Map node ->'CubeWithRefs' == node.cubeName}
        assert null == node.fromFieldName
        assert startCubeName == node.title
        assert startCubeName == node.detailsTitle1
        assert null == node.detailsTitle2
        assert VisualizerConstants.NCUBE == node.group
        assert '1' == node.level
        assert '1' == node.id
        assert startCubeName == node.label
        assert null == node.sourceCubeName
        assert null == node.sourceDescription
        assert [:] == node.scope
        assert [:] == node.availableScope
        assert ['n-cube', 'rule cube'] == node.typesToAdd
        assert true == node.showCellValuesLink
        assert false == node.showCellValues
        assert false == node.cellValuesLoaded
        String nodeDetails = node.details as String
        assert nodeDetails.contains("${VisualizerConstants.DETAILS_LABEL_SCOPE}</b><pre><ul><li>none</li></ul></pre><br><b>")
        assert nodeDetails.contains("${VisualizerConstants.DETAILS_LABEL_AVAILABLE_SCOPE}</b><pre><ul><li>none</li></ul></pre><br><b>")
        assert nodeDetails.contains("${VisualizerConstants.DETAILS_LABEL_REQUIRED_SCOPE_KEYS}</b><pre><ul><li>none</li></ul></pre><br><b>")
        assert nodeDetails.contains("${VisualizerConstants.DETAILS_LABEL_OPTIONAL_SCOPE_KEYS}</b><pre><ul><li>CubeDAxis1</li><li>CubeDAxis2</li></ul></pre><br>")
        assert nodeDetails.contains("${VisualizerConstants.DETAILS_LABEL_AXES}</b><pre><ul><li>CubeDAxis1</li><li>CubeDAxis2</li></ul></pre><br>")
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)

        //Check one target node
        node = visInfo.nodes.find { Map node2 ->'CubeHasTwoRefsToSameCube' == node2.cubeName}
        assert 'CubeDAxis1: CubeDAxis1Col3' == node.fromFieldName
        assert 'CubeHasTwoRefsToSameCube' == node.title
        assert 'CubeHasTwoRefsToSameCube' == node.detailsTitle1
        assert null == node.detailsTitle2
        assert VisualizerConstants.NCUBE == node.group
        assert '2' == node.level
        assert 'CubeHasTwoRefsToSameCube' == node.label
        assert 'CubeWithRefs' == node.sourceCubeName
        assert 'CubeWithRefs' == node.sourceDescription
        assert [CubeDAxis1: 'CubeDAxis1Col3'] == node.scope
        assert [CubeDAxis1: 'CubeDAxis1Col3'] == node.availableScope
        assert ['n-cube', 'rule cube'] == node.typesToAdd
        assert true == node.showCellValuesLink
        assert false == node.showCellValues
        assert false == node.cellValuesLoaded
        nodeDetails = node.details as String
        assert nodeDetails.contains("${VisualizerConstants.DETAILS_LABEL_SCOPE}</b><pre><ul><li>CubeDAxis1: CubeDAxis1Col3</li></ul></pre><br><b>")
        assert nodeDetails.contains("${VisualizerConstants.DETAILS_LABEL_AVAILABLE_SCOPE}</b><pre><ul><li>CubeDAxis1: CubeDAxis1Col3</li></ul></pre><br><b>")
        assert nodeDetails.contains("${VisualizerConstants.DETAILS_LABEL_REQUIRED_SCOPE_KEYS}</b><pre><ul><li>none</li></ul></pre><br><b>")
        assert nodeDetails.contains("${VisualizerConstants.DETAILS_LABEL_OPTIONAL_SCOPE_KEYS}</b><pre><ul><li>CubeEAxis1</li><li>CubeEAxis2</li></ul></pre><br><b>")
        assert nodeDetails.contains("${VisualizerConstants.DETAILS_LABEL_AXES}</b><pre><ul><li>CubeEAxis1</li><li>CubeEAxis2</li></ul></pre><br>")
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)

        //Check edge between top node and target node above
        Map edge = visInfo.edges.find { Map edge -> 'CubeHasTwoRefsToSameCube' == edge.toName && 'CubeWithRefs' == edge.fromName}
        assert 'CubeDAxis1: CubeDAxis1Col3' == edge.fromFieldName
        assert '2' == edge.level
        assert !edge.label
        assert  'CubeDAxis1: CubeDAxis1Col3' == edge.title

        //TODO: Add in check for 3 occurrences of CubeWithNoDefaultsAndNoValues and their edges
    }

    @Test
    void testBuildGraph_invokedWithDifferentVisInfoClass()
    {
        Map scope = null
        String startCubeName = 'CubeWithRefs'
        VisualizerInfo otherVisInfo = new OtherVisualizerInfo()
        assert otherVisInfo instanceof VisualizerInfo
        assert 'VisualizerInfo' != otherVisInfo.class.simpleName
        otherVisInfo.groupSuffix = 'shouldGetResetToEmpty'

        Map options = [startCubeName: startCubeName, scope: scope, visInfo: otherVisInfo]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        VisualizerInfo visInfo = graphInfo.visInfo as VisualizerInfo
        assert null == visInfo.messages

        assert 'VisualizerInfo' == visInfo.class.simpleName
        assert '' ==  visInfo.groupSuffix

        Map node = visInfo.nodes.find { Map node ->'CubeWithRefs' == node.cubeName}
        assert VisualizerConstants.NCUBE == node.group
    }

    @Test
    void testBuildGraph_cubeHasRefToNotExistsCube()
    {
        Map scope = null
        String startCubeName = 'CubeHasRefToNotExistsCube'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        VisualizerInfo visInfo = graphInfo.visInfo as VisualizerInfo
        assert ['No cube exists with name of NotExistCube. Cube not included in the visualization.'] as Set == visInfo.messages
        assert 1 == visInfo.nodes.size()
        assert [] == visInfo.edges

        Map node = visInfo.nodes.find { Map node ->'CubeHasRefToNotExistsCube' == node.cubeName}
        assert null == node.fromFieldName
        assert startCubeName == node.title
    }

    @Test
    void testBuildGraph_ruleCubeWithAllDefaultsAndOnlyDefaultValues()
    {
        Map scope = null
        String startCubeName = 'RuleCubeWithAllDefaultsAndOnlyDefaultValues'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        VisualizerInfo visInfo = graphInfo.visInfo as VisualizerInfo
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List

        assert 4l == visInfo.maxLevel

        Map node = nodes.find { Map node -> startCubeName == node.cubeName}
        assert VisualizerConstants.RULE_NCUBE == node.group

        List<Map> level2Edges = edges.findAll { Map level2Edge -> startCubeName == level2Edge.fromName && '2' == level2Edge.level}
        assert level2Edges.size() == 4

        //Cube ref is a cube level default
        Map edge = level2Edges.find { Map edge1 -> 'CubeWithNoDefaultsAndNoValues' == edge1.toName}
        assert '' == edge.fromFieldName

        //Cube ref is a column level default from a rule axis
        edge = level2Edges.find { Map edge2 -> 'CubeWithDefaultsAndNoValues' == edge2.toName}
        assert 'RuleAxis1: (Condition3): true' == edge.fromFieldName

        // Cube ref is a condition on a rule axis
        edge = level2Edges.find { Map edge3 -> 'CubeWithSingleValue' == edge3.toName}
        assert 'RuleAxis1: (Condition1): @CubeWithSingleValue[:]' == edge.fromFieldName

        // Cube ref is a column level default from non-rule axis
        edge = level2Edges.find { Map edge4 -> 'CubeWithRefs' == edge4.toName}
        assert 'Axis2: Axis2Col2' == edge.fromFieldName
    }

    @Test
    void testBuildGraph_cubeHasTwoRefsToSameCube()
    {
        Map scope = null
        String startCubeName = 'CubeHasTwoRefsToSameCube'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        VisualizerInfo visInfo = graphInfo.visInfo as VisualizerInfo
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List

        assert 2l == visInfo.maxLevel

        Map node = nodes.find { Map node ->'CubeHasTwoRefsToSameCube' == node.cubeName}
        assert null == node.fromFieldName
        assert [:] == node.scope
        assert [:] == node.availableScope

        node = nodes.find { Map node2 ->'CubeWithNoDefaultsAndNoValues' == node2.cubeName &&
                  'CubeEAxis1: CubeEAxis1Col3, CubeEAxis2: CubeEAxis2Col1' == node2.fromFieldName}
        assert [CubeEAxis1: 'CubeEAxis1Col3', CubeEAxis2: 'CubeEAxis2Col1']  as CaseInsensitiveMap == node.scope
        assert [CubeEAxis1: 'CubeEAxis1Col3', CubeEAxis2: 'CubeEAxis2Col1']  as CaseInsensitiveMap == node.availableScope

        node = nodes.find { Map node3 ->'CubeWithNoDefaultsAndNoValues' == node3.cubeName &&
                'CubeEAxis1: default column, CubeEAxis2: default column' == node3.fromFieldName}
        assert [CubeEAxis1: 'default column', CubeEAxis2: 'default column'] as CaseInsensitiveMap == node.scope
        assert [CubeEAxis1: 'default column', CubeEAxis2: 'default column'] as CaseInsensitiveMap == node.availableScope
    }

    @Test
    void testBuildGraph_hasCircularRef()
    {
        Map scope = null
        String startCubeName = 'CubeHasCircularRef1'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        VisualizerInfo visInfo = graphInfo.visInfo as VisualizerInfo
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List

        assert 4l == visInfo.maxLevel

        Map node = nodes.find { Map node ->'CubeHasCircularRef1' == node.cubeName  && null == node.sourceCubeName && '1' == node.level}
        assert null == node.fromFieldName
        assert 'CubeHasCircularRef1' == node.title

        node = nodes.find { Map node2 ->'CubeHasCircularRef2' == node2.cubeName && 'CubeHasCircularRef1' == node2.sourceCubeName && '2' == node2.level}
        assert 'CubeGAxis1: CubeGAxis1Col3, CubeGAxis2: CubeGAxis2Col2' == node.fromFieldName
        assert 'CubeHasCircularRef2' == node.title
        assert 'CubeHasCircularRef1' == node.sourceDescription
        assert [CubeGAxis1: 'CubeGAxis1Col3', CubeGAxis2: 'CubeGAxis2Col2'] as CaseInsensitiveMap == node.scope
        assert [CubeGAxis1: 'CubeGAxis1Col3', CubeGAxis2: 'CubeGAxis2Col2'] as CaseInsensitiveMap == node.availableScope

        node = nodes.find { Map node2 ->'CubeHasCircularRef1' == node2.cubeName && 'CubeHasCircularRef2' == node2.sourceCubeName && '3' == node2.level}
        assert 'CubeHAxis1: CubeHAxis1Col1, CubeHAxis2: CubeHAxis2Col1' == node.fromFieldName
        assert 'CubeHasCircularRef1' == node.title
        assert 'CubeHasCircularRef2' == node.sourceDescription
        assert [CubeHAxis1: 'CubeHAxis1Col1', CubeHAxis2: 'CubeHAxis2Col1'] as CaseInsensitiveMap== node.scope
        assert [CubeHAxis1: 'CubeHAxis1Col1', CubeHAxis2: 'CubeHAxis2Col1', CubeGAxis1: 'CubeGAxis1Col3', CubeGAxis2: 'CubeGAxis2Col2'] as CaseInsensitiveMap ==  node.availableScope

        node = nodes.find { Map node2 ->'CubeHasCircularRef2' == node2.cubeName && 'CubeHasCircularRef1' == node2.sourceCubeName && '4' == node2.level}
        assert 'CubeGAxis1: CubeGAxis1Col3, CubeGAxis2: CubeGAxis2Col2' == node.fromFieldName
        assert 'CubeHasCircularRef2' == node.title
        assert 'CubeHasCircularRef1' == node.sourceDescription
        assert [CubeGAxis1: 'CubeGAxis1Col3', CubeGAxis2: 'CubeGAxis2Col2'] as CaseInsensitiveMap == node.scope
        assert [CubeHAxis1: 'CubeHAxis1Col1', CubeHAxis2: 'CubeHAxis2Col1', CubeGAxis1: 'CubeGAxis1Col3', CubeGAxis2: 'CubeGAxis2Col2'] as CaseInsensitiveMap ==  node.availableScope

        assert nodes.size() == 4
        assert edges.size() == 4
    }

    @Test
    void testGetCellValues_showCellValues_executedCellAndThreeTypesExceptionCells()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'CubeWithExecutedCellAndThreeTypesExceptionCells'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: true,
                cellValuesLoaded: false,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.scope = new CaseInsensitiveMap()
        visInfo.appId = appId
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)
        checkDetailsExpandCollapseSection(nodeDetails)

        //Cube has four cells with values.
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_CELL_VALUES)

        //One throws InvalidCoordinateException
        assert nodeDetails.contains('class="' + InvalidCoordinateException.class.simpleName)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_MISSING_OR_INVALID_COORDINATE)

        //one throws CoordinateNotFoundException
        assert nodeDetails.contains('class="' + CoordinateNotFoundException.class.simpleName)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_MISSING_OR_INVALID_COORDINATE)

        //one throws Exception
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_EXCEPTION)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_ERROR_DURING_EXECUTION)

        //one executes ok
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_EXECUTED_CELL)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXECUTED_VALUE)
    }

    @Test
    void testGetCellValues_showCellValues_executedCell()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'CubeWithExecutedCell'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: true,
                cellValuesLoaded: false,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.scope = new CaseInsensitiveMap()
        visInfo.appId = appId
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)
        checkDetailsExpandCollapseSection(nodeDetails)

        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_CELL_VALUES)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_EXECUTED_CELL)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_EXECUTED_CELL)
        assert nodeDetails.contains('CubeMAxis1: CubeMAxis1Col4, CubeMAxis2: CubeMAxis2Col1')
        assert nodeDetails.contains('class="coord_0 ' + VisualizerConstants.DETAILS_CLASS_WORD_WRAP)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NON_EXECUTED_VALUE)
        assert nodeDetails.contains("@CubeWithSingleValue[CubeKAxis1:'CubeKAxis1Col1', CubeKAxis2: 'CubeKAxis2Col3']")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXECUTED_VALUE)
        assert nodeDetails.contains("value from CubeWithSingleValue in coordinate [CubeKAxis1:'CubeKAxis1Col1 ', CubeKAxis2: 'CubeKAxis2Col3']")
    }

    @Test
    void testGetCellValues_showCellValues_noDefaultsNoCellValues()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'CubeWithNoDefaultsAndNoValues'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: true,
                cellValuesLoaded: false,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.scope = new CaseInsensitiveMap()
        visInfo.appId = appId
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)

        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXPAND_ALL)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_COLLAPSE_ALL)

        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_CELL_VALUES)
        assert nodeDetails.contains(VisualizerConstants.NONE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NON_EXECUTED_VALUE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXECUTED_VALUE)
      }

    @Test
    void testGetCellValues_showCellValues_withDefaultsNoCellValues()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'CubeWithDefaultsAndNoValues'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: true,
                cellValuesLoaded: false,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.scope = new CaseInsensitiveMap()
        visInfo.appId = appId
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)

        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXPAND_ALL)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_COLLAPSE_ALL)

        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_CELL_VALUES)
        assert nodeDetails.contains(VisualizerConstants.NONE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NON_EXECUTED_VALUE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXECUTED_VALUE)
    }


    @Test
    void testGetCellValues_showCellValues_ruleCubeWithAllDefaultsAndOnlyDefaultValues()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'RuleCubeWithAllDefaultsAndOnlyDefaultValues'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: true,
                cellValuesLoaded: false,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.scope = new CaseInsensitiveMap()
        visInfo.appId = appId
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)

        //TODO: Should show default values
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXPAND_ALL)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_COLLAPSE_ALL)

        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_CELL_VALUES)
        assert nodeDetails.contains(VisualizerConstants.NONE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NON_EXECUTED_VALUE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXECUTED_VALUE)
    }

    @Test
    void testGetCellValues_showCellValues_exceptionCell()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'CubeWithExceptionCell'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: true,
                cellValuesLoaded: false,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.scope = new CaseInsensitiveMap()
        visInfo.appId = appId
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)
        checkDetailsExpandCollapseSection(nodeDetails)

        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_CELL_VALUES)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_ERROR_DURING_EXECUTION)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_EXCEPTION)
        assert nodeDetails.contains('CubeMAxis1: CubeMAxis1Col3, CubeMAxis2: CubeMAxis2Col1')
        assert nodeDetails.contains('class="coord_0 ' + VisualizerConstants.DETAILS_CLASS_WORD_WRAP)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NON_EXECUTED_VALUE)
        assert nodeDetails.contains("int a = 5")
        assert nodeDetails.contains("int b = 0")
        assert nodeDetails.contains("return a / b")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXCEPTION)
        assert nodeDetails.contains("An exception was thrown while loading coordinate")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_MESSAGE)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_ROOT_CAUSE)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_STACK_TRACE)
    }

    @Test
    void testGetCellValues_showCellValues_invalidCoordinateCell_dueToOneInvalidCoordinateKey()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'CubeWithInvalidCoordinateCell'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: true,
                cellValuesLoaded: false,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.scope = new CaseInsensitiveMap()
        visInfo.appId = appId
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)
        checkDetailsExpandCollapseSection(nodeDetails)

        //Cube has one cell with a value. It executed OK.
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_CELL_VALUES)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_MISSING_OR_INVALID_COORDINATE)
        assert nodeDetails.contains('class="' + InvalidCoordinateException.class.simpleName)
        assert nodeDetails.contains('CubeMAxis1: CubeMAxis1Col1, CubeMAxis2: CubeMAxis2Col1')
        assert nodeDetails.contains('class="coord_0 ' + VisualizerConstants.DETAILS_CLASS_WORD_WRAP)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NON_EXECUTED_VALUE)
        assert nodeDetails.contains("@CubeWithSingleValue[bogusAxisName:'CubeKAxis1Col1', CubeKAxis2: 'CubeKAxis2Col3']")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXCEPTION)

        assert nodeDetails.contains("Additional scope is required to load coordinate")
        assert nodeDetails.contains("Please add scope value(s) for the following scope key(s): CubeKAxis1, CubeKAxis2.")
        assert nodeDetails.contains("The following values are available for CubeKAxis1:")
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col1")
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col2")
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col3")

        //TODO: CubeKAxis2 should not get flagged as invalid
        assert nodeDetails.contains("The following values are available for CubeKAxis2:")
        assert nodeDetails.contains("CubeKAxis2: CubeKAxis2Col1")
        assert nodeDetails.contains("CubeKAxis2: CubeKAxis2Col2")
        assert nodeDetails.contains("CubeKAxis2: CubeKAxis2Col3")

        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_MESSAGE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_ROOT_CAUSE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_STACK_TRACE)
    }

    @Test
    void testGetCellValues_showCellValues_invalidCoordinateCell_dueToTwoInvalidCoordinateKeys()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'CubeWithInvalidCoordinateCellDueToTwoInvalidKeys'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: true,
                cellValuesLoaded: false,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.scope = new CaseInsensitiveMap()
        visInfo.appId = appId
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)
        checkDetailsExpandCollapseSection(nodeDetails)

        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_CELL_VALUES)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_MISSING_OR_INVALID_COORDINATE)
        assert nodeDetails.contains('class="' + InvalidCoordinateException.class.simpleName)
        assert nodeDetails.contains('CubeMAxis1: CubeMAxis1Col1, CubeMAxis2: CubeMAxis2Col1')
        assert nodeDetails.contains('class="coord_0 ' + VisualizerConstants.DETAILS_CLASS_WORD_WRAP)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NON_EXECUTED_VALUE)
        assert nodeDetails.contains("@CubeWithSingleValue[bogusAxisName:'CubeKAxis1Col1', dummyAxisName: 'CubeKAxis2Col3']")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXCEPTION)

        assert nodeDetails.contains("Additional scope is required to load coordinate")
        assert nodeDetails.contains("Please add scope value(s) for the following scope key(s): CubeKAxis1, CubeKAxis2.")
        assert nodeDetails.contains("The following values are available for CubeKAxis1:")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_CLASS_MISSING_SCOPE)
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col1")
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col2")
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col3")

        assert nodeDetails.contains("The following values are available for CubeKAxis2:")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_CLASS_MISSING_SCOPE)
        assert nodeDetails.contains("CubeKAxis2: CubeKAxis2Col1")
        assert nodeDetails.contains("CubeKAxis2: CubeKAxis2Col2")
        assert nodeDetails.contains("CubeKAxis2: CubeKAxis2Col3")

        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_MESSAGE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_ROOT_CAUSE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_STACK_TRACE)
    }

    @Test
    void testGetCellValues_showCellValues_coordinateNotFoundCell_dueToOneNotFoundValue()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'CubeWithCoordinateNotFoundCell'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: true,
                cellValuesLoaded: false,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.scope = new CaseInsensitiveMap()
        visInfo.appId = appId
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)
        checkDetailsExpandCollapseSection(nodeDetails)

        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_CELL_VALUES)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_MISSING_OR_INVALID_COORDINATE)
        assert nodeDetails.contains('class="' + CoordinateNotFoundException.class.simpleName)
        assert nodeDetails.contains('CubeMAxis1: CubeMAxis1Col2, CubeMAxis2: CubeMAxis2Col1')
        assert nodeDetails.contains('class="coord_0 ' + VisualizerConstants.DETAILS_CLASS_WORD_WRAP)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NON_EXECUTED_VALUE)
        assert nodeDetails.contains("@CubeWithSingleValue[CubeKAxis1:'bogusScopeValue', CubeKAxis2: 'CubeKAxis2Col3']")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXCEPTION)
        assert nodeDetails.contains("The scope value bogusScopeValue for scope key CubeKAxis1 cannot be found on axis CubeKAxis1 for coordinate")
        assert nodeDetails.contains("Please supply a different value for CubeKAxis1.")
        assert nodeDetails.contains("The following values are available for CubeKAxis1:")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_CLASS_MISSING_SCOPE)
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col1")
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col2")
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col3")
        assert !nodeDetails.contains("The following values are available for CubeKAxis2:")

        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_MESSAGE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_ROOT_CAUSE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_STACK_TRACE)
    }


    @Test
    void testGetCellValues_showCellValues_coordinateNotFoundCell_dueToTwoNotFoundValues()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'CubeWithCoordinateNotFoundCellDueToTwoNotFoundValues'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: true,
                cellValuesLoaded: false,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.scope = new CaseInsensitiveMap()
        visInfo.appId = appId
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)
        checkDetailsExpandCollapseSection(nodeDetails)

        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
        assert nodeDetails.contains('class="' + VisualizerConstants.DETAILS_CLASS_CELL_VALUES)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_MISSING_OR_INVALID_COORDINATE)
        assert nodeDetails.contains('class="' + CoordinateNotFoundException.class.simpleName)
        assert nodeDetails.contains('CubeMAxis1: CubeMAxis1Col2, CubeMAxis2: CubeMAxis2Col1')
        assert nodeDetails.contains('class="coord_0 ' + VisualizerConstants.DETAILS_CLASS_WORD_WRAP)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NON_EXECUTED_VALUE)
        assert nodeDetails.contains("@CubeWithSingleValue[CubeKAxis1:'bogusScopeValue', CubeKAxis2: 'dummyScopeValue']")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXCEPTION)
        assert nodeDetails.contains("The scope value bogusScopeValue for scope key CubeKAxis1 cannot be found on axis CubeKAxis1 for coordinate")
        assert nodeDetails.contains("Please supply a different value for CubeKAxis1.")
        assert nodeDetails.contains("The following values are available for CubeKAxis1:")
        assert nodeDetails.contains(VisualizerConstants.DETAILS_CLASS_MISSING_SCOPE)
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col1")
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col2")
        assert nodeDetails.contains("CubeKAxis1: CubeKAxis1Col3")

        //TODO: Should have values for CubeKAxis2
        assert !nodeDetails.contains("The following values are available for CubeKAxis2:")

        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_MESSAGE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_ROOT_CAUSE)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_STACK_TRACE)
    }

    @Test
    void testGetCellValues_hideCellValues()
    {
        Map scope = null
        Map nodeScope = null
        String cubeName = 'CubeWithExecutedCell'

        Map oldNode = [
                id: '1',
                cubeName: cubeName,
                title: cubeName,
                level: '1',
                label: cubeName,
                scope: nodeScope,
                showCellValuesLink: true,
                showCellValues: false,
                cellValuesLoaded: true,
                details: VisualizerConstants.DETAILS_LABEL_CELL_VALUES,
                availableScope: scope,
        ]

        VisualizerInfo visInfo = new VisualizerInfo()
        visInfo.allGroupsKeys = ['NCUBE', 'RULE_NCUBE', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = ''
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert VisualizerConstants.STATUS_SUCCESS == graphInfo.status
        assert null == visInfo.messages
        List<Map<String, Object>> nodes = visInfo.nodes as List
        List<Map<String, Object>> edges = visInfo.edges as List
        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.first()
        assert cubeName == node.title
        assert true == node.showCellValuesLink
        assert false == node.showCellValues
        assert true == node.cellValuesLoaded

        String nodeDetails = node.details as String
        checkDetailsTopSection(nodeDetails)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXPAND_ALL)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_COLLAPSE_ALL)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_CELL_VALUES)
     }

    private static void checkDetailsExpandCollapseSection(String nodeDetails)
    {
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_EXPAND_ALL)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_COLLAPSE_ALL)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_EXPAND_ALL)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_TITLE_COLLAPSE_ALL)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_CLASS_EXPAND_ALL)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_CLASS_COLLAPSE_ALL)
    }

    private static void checkDetailsTopSection(String nodeDetails)
    {
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_SCOPE)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_AVAILABLE_SCOPE)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_REQUIRED_SCOPE_KEYS)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_OPTIONAL_SCOPE_KEYS)
        assert nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_AXES)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(VisualizerConstants.DETAILS_LABEL_NOTE)
    }

    class OtherVisualizerInfo extends VisualizerInfo {}

}
