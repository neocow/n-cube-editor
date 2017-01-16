package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.Axis
import com.cedarsoftware.ncube.AxisType
import com.cedarsoftware.ncube.AxisValueType
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.NCubeResourcePersister
import com.cedarsoftware.ncube.ReleaseStatus
import groovy.transform.CompileStatic
import org.junit.Before
import org.junit.Test

import static com.cedarsoftware.util.RpmVisualizerConstants.*

@CompileStatic
class RpmVisualizerTest
{
    static final String PATH_PREFIX = 'rpmvisualizer/**/'
    RpmVisualizer visualizer
    ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, 'VISUALIZER.TEST', ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name(), ApplicationID.HEAD)

    @Before
    void beforeTest(){
        visualizer = new RpmVisualizer()
        NCubeManager.NCubePersister = new NCubeResourcePersister(PATH_PREFIX)
    }

    @Test
    void testBuildGraph_checkVisInfo()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     product          : 'WProduct',
                     policyControlDate: '2017-01-01',
                     quoteDate        : '2017-01-01',
                     coverage         : 'FCoverage',
                     risk             : 'WProductOps']
        String startCubeName = 'rpm.class.Coverage'
        Map options = [startCubeName: startCubeName, scope: new CaseInsensitiveMap(scope)]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        RpmVisualizerInfo visInfo = graphInfo.visInfo as RpmVisualizerInfo

        //Check visInfo
        assert 5 == visInfo.nodes.size()
        assert 4 == visInfo.edges.size()
        assert 4l == visInfo.maxLevel
        assert 6l == visInfo.nodeCount
        assert 5l == visInfo.relInfoCount
        assert 3l == visInfo.defaultLevel
        assert '_ENUM' == visInfo.groupSuffix
        assert scope == visInfo.scope

        Map allGroups =  [PRODUCT: 'Product', FORM: 'Form', RISK: 'Risk', COVERAGE: 'Coverage', CONTAINER: 'Container', DEDUCTIBLE: 'Deductible', LIMIT: 'Limit', RATE: 'Rate', RATEFACTOR: 'Rate Factor', PREMIUM: 'Premium', PARTY: 'Party', PLACE: 'Place', ROLE: 'Role', ROLEPLAYER: 'Role Player', UNSPECIFIED: 'Unspecified']
        assert allGroups == visInfo.allGroups
        assert allGroups.keySet() == visInfo.allGroupsKeys
        assert ['COVERAGE', 'RISK'] as Set == visInfo.availableGroupsAllLevels

        //Spot check availableScopeValues
        assert allGroups.size() * 2 == visInfo.availableScopeValues.size()
        assert ['GProductOps', 'ProductLocation', 'StateOps', 'WProductOps'] as Set == visInfo.availableScopeValues['sourceRisk']

        assert ['rpm.class.Coverage': [] as Set,
                'rpm.enum.Coverage.Coverages': [] as Set,
                'rpm.class.Risk': [] as Set] == visInfo.requiredScopeKeys

        //Spot check optionalScopeKeys
        assert visInfo.requiredScopeKeys.keySet() == visInfo.optionalScopeKeys.keySet()
        assert visInfo.optionalScopeKeys['rpm.class.Risk'].containsAll(['LocationState', 'businessDivisionCode','transaction'])

        //Spot check typesToAddMap
        assert ['Coverage', 'Deductible', 'Limit', 'Premium', 'Rate', 'Ratefactor', 'Role'] == visInfo.typesToAddMap['Coverage']

        //Spot check the network overrides
        assert (visInfo.networkOverridesBasic.groups as Map).keySet().containsAll(allGroups.keySet())
        assert true == ((visInfo.networkOverridesFull.nodes as Map).shadow as Map).enabled
        assert (visInfo.networkOverridesTopNode.shapeProperties as Map).containsKey('borderDashes')
    }

    @Test
    void testBuildGraph_canLoadTargetAsRpmClass()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     product:'WProduct',
                     policyControlDate:'2017-01-01',
                     quoteDate:'2017-01-01',
                     coverage: 'CCoverage',
                     sourceCoverage: 'FCoverage',
                     risk: 'WProductOps']
        String startCubeName = 'rpm.class.Coverage'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert  STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List

        Map node = nodes.find {Map node -> "${UNABLE_TO_LOAD}Location".toString() == node.label}
        String nodeDetails = node.details as String
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert nodeDetails.contains(DETAILS_LABEL_REASON)
        assert nodeDetails.contains("Coverage points directly to Risk via field Location, but there is no risk named Location on Risk.")
        assert nodeDetails.contains("Therefore it cannot be loaded in the visualization.")
        assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS)
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
    }

    @Test
    void testBuildGraph_checkNodeAndEdge_nonEPM()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION]
        String startCubeName = 'rpm.class.partyrole.LossPrevention'
        Map options = [startCubeName: startCubeName, scope: new CaseInsensitiveMap(scope)]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List

        //Top level source node
        Map node = nodes.find { Map node1 -> startCubeName == node1.cubeName}
        assert null == node.fromFieldName
        assert 'partyrole.LossPrevention' == node.title
        assert 'partyrole.LossPrevention' == node.detailsTitle1
        assert null == node.detailsTitle2
        assert 'UNSPECIFIED' == node.group
        assert '1' == node.level
        assert 'LossPrevention' == node.label
        assert null == node.sourceCubeName
        assert null == node.sourceDescription
        assert null == node.typesToAdd
        assert true == node.showCellValuesLink
        assert false == node.showCellValues
        assert true == node.cellValuesLoaded
        assert scope == node.scope
        assert scope == node.availableScope
        String nodeDetails = node.details as String
        assert nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert nodeDetails.contains("${DETAILS_LABEL_FIELDS}</b><pre><ul><li>roleRefCode</li><li>Parties</li></ul></pre>")
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
        assert !nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)

        //Edge from top level node to enum
        Map edge = edges.find { Map edge -> 'rpm.class.partyrole.LossPrevention' == edge.fromName &&
                'rpm.enum.partyrole.BasePartyRole.Parties' == edge.toName}
        assert 'Parties' == edge.fromFieldName
        assert '2' == edge.level
        assert 'Parties' == edge.label
        assert "Field Parties cardinality ${V_MIN_CARDINALITY}:${V_MAX_CARDINALITY}".toString() == edge.title

        //Enum node under top level node
        node = nodes.find { Map nodeEnum ->'rpm.enum.partyrole.BasePartyRole.Parties' == nodeEnum.cubeName}
        assert 'Parties' == node.fromFieldName
        assert 'Valid values for field Parties on LossPrevention' == node.title
        assert 'Valid values for field Parties on LossPrevention' == node.detailsTitle1
        assert null == node.detailsTitle2
        assert 'PARTY_ENUM' == node.group
        assert '2' == node.level
        assert null == node.label
        assert 'rpm.class.partyrole.LossPrevention' == node.sourceCubeName
        assert [_effectiveVersion: ApplicationID.DEFAULT_VERSION, sourceFieldName: 'Parties'] == node.scope
        assert [_effectiveVersion: ApplicationID.DEFAULT_VERSION, sourceFieldName: 'Parties'] == node.availableScope
        assert 'LossPrevention' == node.sourceDescription
        assert null == node.typesToAdd
        assert true == node.showCellValuesLink
        assert false == node.showCellValues
        assert true == node.cellValuesLoaded
        nodeDetails = node.details as String
        assert nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert nodeDetails.contains("${DETAILS_LABEL_FIELDS}</b><pre><ul><li>party.MoreNamedInsured</li><li>party.ProfitCenter</li></ul></pre>")
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
        assert !nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)

        //Edge from enum to target node
        edge = edges.find { Map edge1 -> 'rpm.enum.partyrole.BasePartyRole.Parties' == edge1.fromName &&
                'rpm.class.party.ProfitCenter' == edge1.toName}
        assert 'party.ProfitCenter' == edge.fromFieldName
        assert '3' == edge.level
        assert !edge.label
        assert 'Valid value party.ProfitCenter cardinality 0:1' == edge.title

        //Target node under enum
        node = nodes.find { Map node2 ->'rpm.class.party.ProfitCenter' == node2.cubeName}
        assert 'party.ProfitCenter' == node.fromFieldName
        assert 'party.ProfitCenter' == node.title
        assert 'party.ProfitCenter' == node.detailsTitle1
        assert null == node.detailsTitle2
        assert 'PARTY' == node.group
        assert '3' == node.level
        assert 'ProfitCenter' == node.label
        assert 'rpm.enum.partyrole.BasePartyRole.Parties' == node.sourceCubeName
        assert 'partyrole.BasePartyRole.Parties' == node.sourceDescription
        assert  [] == node.typesToAdd
        assert true == node.showCellValuesLink
        assert false == node.showCellValues
        assert true == node.cellValuesLoaded
        assert scope == node.scope
        assert [_effectiveVersion: ApplicationID.DEFAULT_VERSION, sourceFieldName: 'Parties'] == node.availableScope
        nodeDetails = node.details as String
        assert nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert nodeDetails.contains("${DETAILS_LABEL_FIELDS}</b><pre><ul><li>roleRefCode</li><li>fullName</li><li>fein</li></ul></pre>")
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
        assert !nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)
    }

    @Test
    void testBuildGraph_checkStructure()
    {
        Map startScope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                          product          : 'WProduct',
                          policyControlDate: '2017-01-01',
                          quoteDate        : '2017-01-01',
                          coverage         : 'FCoverage',
                          risk             : 'WProductOps']


        String startCubeName = 'rpm.class.Coverage'
        Map options = [startCubeName: startCubeName, scope: startScope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List

        assert nodes.size() == 5
        assert edges.size() == 4

        assert nodes.find { Map node -> 'FCoverage' == node.label}
        assert nodes.find { Map node -> 'ICoverage' == node.label}
        assert nodes.find { Map node -> 'CCoverage' == node.label}
        assert nodes.find { Map node -> "${UNABLE_TO_LOAD}Location".toString() == node.label}
        assert nodes.find { Map node -> 'Valid values for field Coverages on FCoverage' == node.title}

        assert edges.find { Map edge -> 'FCoverage' == edge.fromName && 'rpm.enum.Coverage.Coverages' == edge.toName}
        assert edges.find { Map edge -> 'rpm.enum.Coverage.Coverages' == edge.fromName && 'ICoverage' == edge.toName}
        assert edges.find { Map edge -> 'rpm.enum.Coverage.Coverages' == edge.fromName && 'CCoverage' == edge.toName}
        assert edges.find { Map edge -> 'CCoverage' == edge.fromName && "${UNABLE_TO_LOAD}Location".toString() == edge.toName}
    }

    @Test
    void testBuildGraph_checkStructure_nonEPM()
    {
        Map scope = null

        String startCubeName = 'rpm.class.partyrole.LossPrevention'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List

        assert nodes.size() == 4
        assert edges.size() == 3

        assert nodes.find { Map node ->'rpm.class.partyrole.LossPrevention' == node.cubeName}
        assert nodes.find { Map node ->'rpm.class.party.MoreNamedInsured' == node.cubeName}
        assert nodes.find { Map node ->'rpm.class.party.ProfitCenter' == node.cubeName}
        assert nodes.find { Map node ->'rpm.enum.partyrole.BasePartyRole.Parties' == node.cubeName}

        assert edges.find { Map edge ->'rpm.enum.partyrole.BasePartyRole.Parties' == edge.fromName && 'rpm.class.party.ProfitCenter' == edge.toName}
        assert edges.find { Map edge ->'rpm.enum.partyrole.BasePartyRole.Parties' == edge.fromName && 'rpm.class.party.MoreNamedInsured' == edge.toName}
        assert edges.find { Map edge ->'rpm.class.partyrole.LossPrevention' == edge.fromName && 'rpm.enum.partyrole.BasePartyRole.Parties' == edge.toName}
    }

    @Test
    void testBuildGraph_checkNodeAndEdge()
    {
        Map startScope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     product          : 'WProduct',
                     policyControlDate: '2017-01-01',
                     quoteDate        : '2017-01-01',
                     coverage         : 'FCoverage',
                     risk             : 'WProductOps']

        Map enumScope = new CaseInsensitiveMap(startScope)
        enumScope.sourceFieldName = 'Coverages'

        Map cCoverageScope = new CaseInsensitiveMap(startScope)
        cCoverageScope.coverage = 'CCoverage'
        cCoverageScope.sourceCoverage = 'FCoverage'

        Map availableCCoverageScope = new CaseInsensitiveMap(cCoverageScope)
        availableCCoverageScope.sourceFieldName = 'Coverages'

        String startCubeName = 'rpm.class.Coverage'
        Map options = [startCubeName: startCubeName, scope: new CaseInsensitiveMap(startScope)]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List

        //Top level source node
        Map node = nodes.find { Map node1 -> 'FCoverage' == node1.label}
        assert 'rpm.class.Coverage' == node.cubeName
        assert null == node.fromFieldName
        assert 'Coverage' == node.title
        assert 'Coverage' == node.detailsTitle1
        assert 'FCoverage' == node.detailsTitle2
        assert 'COVERAGE' == node.group
        assert '1' == node.level
        assert 'FCoverage' == node.label
        assert null == node.sourceCubeName
        assert null == node.sourceDescription
        assert ['Coverage', 'Deductible', 'Limit', 'Premium', 'Rate', 'Ratefactor', 'Role'] == node.typesToAdd
        assert true == node.showCellValuesLink
        assert false == node.showCellValues
        assert true == node.cellValuesLoaded
        assert startScope == node.scope
        assert startScope == node.availableScope
        String nodeDetails = node.details as String
        assert nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert nodeDetails.contains("${DETAILS_LABEL_FIELDS}</b><pre><ul><li>Coverages</li><li>Exposure</li><li>StatCode</li></ul></pre>")
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
        assert !nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)

        //Edge from top level node to enum
        Map edge = edges.find { Map edge -> 'FCoverage' == edge.fromName &&
                'rpm.enum.Coverage.Coverages' == edge.toName}
        assert 'Coverages' == edge.fromFieldName
        assert '2' == edge.level
        assert 'Coverages' == edge.label
        assert "Field Coverages cardinality ${V_MIN_CARDINALITY}:${V_MAX_CARDINALITY}".toString() == edge.title

        //Enum node under top level node
        node = nodes.find { Map node1 -> 'Valid values for field Coverages on FCoverage' == node1.title}
        assert 'rpm.enum.Coverage.Coverages' == node.cubeName
        assert null == node.label
        assert 'Coverages' == node.fromFieldName
        assert 'Valid values for field Coverages on FCoverage' == node.detailsTitle1
        assert null == node.detailsTitle2
        assert 'COVERAGE_ENUM' == node.group
        assert '2' == node.level
        assert 'rpm.class.Coverage' == node.sourceCubeName
        assert 'FCoverage' == node.sourceDescription
        assert enumScope == node.scope
        assert enumScope == node.availableScope
        assert null == node.typesToAdd
        assert true == node.showCellValuesLink
        assert false == node.showCellValues
        assert true == node.cellValuesLoaded
        nodeDetails = node.details as String
        assert nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert nodeDetails.contains("${DETAILS_LABEL_FIELDS}</b><pre><ul><li>CCoverage</li><li>ICoverage</li></ul></pre>")
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
        assert !nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)

        //Edge from enum to target node
        edge = edges.find { Map edge1 -> 'rpm.enum.Coverage.Coverages' == edge1.fromName &&
                'CCoverage' == edge1.toName}
        assert 'CCoverage' == edge.fromFieldName
        assert '3' == edge.level
        assert !edge.label
        assert "Valid value CCoverage cardinality ${V_MIN_CARDINALITY}:${V_MAX_CARDINALITY}".toString() == edge.title

        //Target node of top level node
        node = nodes.find { Map node1 -> 'CCoverage' == node1.label}
        assert 'rpm.class.Coverage' == node.cubeName
        assert 'CCoverage' == node.fromFieldName
        assert 'Coverage' == node.title
        assert 'Coverage' == node.detailsTitle1
        assert 'CCoverage' == node.detailsTitle2
        assert 'COVERAGE' == node.group
        assert '3' == node.level
        assert 'rpm.enum.Coverage.Coverages' == node.sourceCubeName
        assert 'field Coverages on FCoverage' == node.sourceDescription
        assert ['Coverage', 'Deductible', 'Limit', 'Premium', 'Rate', 'Ratefactor', 'Role'] == node.typesToAdd
        assert true == node.showCellValuesLink
        assert false == node.showCellValues
        assert true == node.cellValuesLoaded
        assert cCoverageScope == node.scope
        assert availableCCoverageScope == node.availableScope
        nodeDetails = node.details as String
        assert nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert nodeDetails.contains("${DETAILS_LABEL_FIELDS}</b><pre><ul><li>Exposure</li><li>Location</li><li>StatCode</li></ul></pre>")
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
        assert !nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)
    }

    @Test
    void testGetCellValues_classNode_showCellValues()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                          product          : 'WProduct',
                          policyControlDate: '2017-01-01',
                          quoteDate        : '2017-01-01',
                          sourceCoverage   : 'FELACodeCoverage',
                          coverage         : 'CCoverage',
                          sourceFieldName  : 'Coverages',
                          risk             : 'WProductOps']

        Map nodeScope = new CaseInsensitiveMap(scope)
        nodeScope.remove('sourceFieldName')

        Map oldNode = [
                id: '4',
                cubeName: 'rpm.class.Coverage',
                fromFieldName: 'CCoverage',
                title: 'rpm.class.Coverage',
                level: '3',
                label: 'CCoverage',
                scope: nodeScope,
                showCellValues: true,
                showCellValuesLink: true,
                cellValuesLoaded: false,
                availableScope: scope,
                typesToAdd: [],
          ]

        RpmVisualizerInfo visInfo = new RpmVisualizerInfo()
        visInfo.allGroupsKeys = ['PRODUCT', 'FORM', 'RISK', 'COVERAGE', 'CONTAINER', 'DEDUCTIBLE', 'LIMIT', 'RATE', 'RATEFACTOR', 'PREMIUM', 'PARTY', 'PLACE', 'ROLE', 'ROLEPLAYER', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = '_ENUM'
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List

        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.find { Map node -> 'CCoverage' == node.label}
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded
        String nodeDetails = node.details as String
        assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert nodeDetails.contains("${DETAILS_LABEL_FIELDS_AND_TRAITS}</b><pre><ul><li><b>Exposure</b></li><pre><ul><li>r:declared: true</li><li>r:defaultValue: 0</li><li>r:exists: true</li><li>r:extends: DataElementInventory</li><li>r:rpmType: string</li></ul></pre><li><b>Location</b></li><pre><ul><li>r:declared: true</li><li>r:exists: true</li><li>r:rpmType: Risk</li><li>v:max: 1</li><li>v:min: 0</li></ul></pre><li><b>StatCode</b></li><pre><ul><li>r:declared: true</li><li>r:defaultValue: None</li><li>r:exists: true</li><li>r:extends: DataElementInventory[StatCode]</li><li>r:rpmType: string</li></ul></pre></ul></pre>")
        assert !nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert nodeDetails.contains("${DETAILS_LABEL_CLASS_TRAITS}</b><pre><ul><li>r:exists: true</li><li>r:name: CCoverage</li><li>r:scopedName: CCoverage</li></ul></pre><br><b>")
    }

    @Test
    void testGetCellValues_enumNode_showCellValues()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     product          : 'WProduct',
                     policyControlDate: '2017-01-01',
                     quoteDate        : '2017-01-01',
                     coverage         : 'FCoverage',
                     sourceFieldName  : 'Coverages',
                     risk             : 'WProductOps']

          Map oldNode = [
                id: '2',
                cubeName: 'rpm.enum.Coverage.Coverages',
                fromFieldName: 'FCoverage',
                title: 'Valid values for field Coverages on FCoverage',
                level: '2',
                scope: new CaseInsensitiveMap(scope),
                showCellValues: true,
                showCellValuesLink: true,
                cellValuesLoaded: false,
                availableScope: new CaseInsensitiveMap(scope),
                typesToAdd: [],
        ]

        RpmVisualizerInfo visInfo = new RpmVisualizerInfo()
        visInfo.allGroupsKeys = ['PRODUCT', 'FORM', 'RISK', 'COVERAGE', 'CONTAINER', 'DEDUCTIBLE', 'LIMIT', 'RATE', 'RATEFACTOR', 'PREMIUM', 'PARTY', 'PLACE', 'ROLE', 'ROLEPLAYER', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = '_ENUM'
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List

        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.find { Map node1 -> 'Valid values for field Coverages on FCoverage' == node1.title}
        assert true == node.showCellValuesLink
        assert true == node.showCellValues
        assert true == node.cellValuesLoaded
        String nodeDetails = node.details as String
        assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert nodeDetails.contains("${DETAILS_LABEL_FIELDS_AND_TRAITS}</b><pre><ul><li><b>CCoverage</b></li><pre><ul><li>r:declared: true</li><li>r:exists: true</li><li>r:name: CCoverage</li><li>v:max: 999999</li><li>v:min: 0</li></ul></pre><li><b>ICoverage</b></li><pre><ul><li>r:declared: true</li><li>r:exists: true</li><li>r:name: ICoverage</li><li>v:max: 1</li><li>v:min: 0</li></ul>")
        assert !nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert nodeDetails.contains("${DETAILS_LABEL_CLASS_TRAITS}</b><pre><ul><li>r:exists: true</li></ul></pre><br><b>")
    }

    @Test
    void testGetCellValues_single_classNode_hideCellValues()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     product          : 'WProduct',
                     policyControlDate: '2017-01-01',
                     quoteDate        : '2017-01-01',
                     sourceCoverage   : 'FELACodeCoverage',
                     coverage         : 'CCoverage',
                     sourceFieldName  : 'Coverages',
                     risk             : 'WProductOps']

        Map nodeScope = new CaseInsensitiveMap(scope)
        nodeScope.remove('sourceFieldName')

        Map oldNode = [
                id: '4',
                cubeName: 'rpm.class.Coverage',
                fromFieldName: 'CCoverage',
                title: 'rpm.class.Coverage',
                level: '3',
                label: 'CCoverage',
                scope: nodeScope,
                showCellValues: false,
                showCellValuesLink: true,
                cellValuesLoaded: true,
                availableScope: scope,
                typesToAdd: [],
        ]

        RpmVisualizerInfo visInfo = new RpmVisualizerInfo()
        visInfo.allGroupsKeys = ['PRODUCT', 'FORM', 'RISK', 'COVERAGE', 'CONTAINER', 'DEDUCTIBLE', 'LIMIT', 'RATE', 'RATEFACTOR', 'PREMIUM', 'PARTY', 'PLACE', 'ROLE', 'ROLEPLAYER', 'UNSPECIFIED'] as Set
        visInfo.groupSuffix = '_ENUM'
        visInfo.availableGroupsAllLevels = [] as Set

        Map options = [node: oldNode, visInfo: visInfo]

        Map graphInfo = visualizer.getCellValues(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List

        assert nodes.size() == 1
        assert edges.size() == 0

        Map node = nodes.find { Map node -> 'CCoverage' == node.label}
        assert true == node.showCellValuesLink
        assert false == node.showCellValues
        assert true == node.cellValuesLoaded
        String nodeDetails = node.details as String
        assert nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
        assert nodeDetails.contains("${DETAILS_LABEL_FIELDS}</b><pre><ul><li>Exposure</li><li>Location</li><li>StatCode</li></ul></pre>")
        assert !nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)
    }

    @Test
    void testBuildGraph_initialPromptForScope()
    {
        Map scope = null

        String startCubeName = 'rpm.class.Product'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert  STATUS_MISSING_START_SCOPE == graphInfo.status
        Set messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
        assert 1 == messages.size()
        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List
        assert 0 == nodes.size()
        assert 0 == edges.size()

        String message = messages.first()
        assert message.contains(SCOPE_ADDED_SINCE_REQUIRED)
        assert message.contains('Product, policyControlDate, quoteDate, _effectiveVersion')
        assert message.contains('Please replace XXXX for Product with an actual scope value.')
        assert message.contains(OTHER_DEFAULT_VALUE_MAY_BE_CHANGED)
        assert message.contains("${SCOPE_VALUES_AVAILABLE_FOR}Product:")
        assert message.contains('GProduct')
        assert message.contains('UProduct')
        assert message.contains('WProduct')
    }

    @Test
    void testBuildGraph_invalidScope()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     product:'xxxxxxxx',
                     policyControlDate:'2017-01-01',
                     quoteDate:'2017-01-01']

        String startCubeName = 'rpm.class.Product'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert  STATUS_SUCCESS == graphInfo.status
        Set<String> messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
        assert 1 == messages.size()
        checkInvalidScopeMessage(messages.first())

        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List
        assert 1 == nodes.size()
        assert 0 == edges.size()

        Map node = nodes.first()
        assert 'Product' == node.title
        assert 'Product' == node.detailsTitle1
        assert 'xxxxxxxx' == node.detailsTitle2
        assert "${SCOPE_VALUE_NOT_FOUND}xxxxxxxx".toString() == node.label
        String nodeDetails = node.details as String
        checkInvalidScopeMessage(nodeDetails)
        assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS)
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
        assert nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)
    }

    private static void checkInvalidScopeMessage(String message)
    {
        assert message.contains('The scope value xxxxxxxx for scope key product cannot be found on axis product in rpm.scope.class.Product.traits for xxxxxxxx.')
        assert message.contains("${SUPPLY_DIFFERENT_VALUE_FOR}product.")
        assert message.contains("${SCOPE_VALUES_AVAILABLE_FOR}product:")
        assert message.contains('GProduct')
        assert message.contains('UProduct')
        assert message.contains('WProduct')
    }

    @Test
    void testBuildGraph_missingRequiredScope()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     policyControlDate:'2017-01-01',
                     quoteDate:'2017-01-01',
                     risk: 'ProductLocation']

        String startCubeName = 'rpm.class.Risk'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        Set messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
        assert 1 == messages.size()
        checkAdditionalScopeIsRequiredMessage(messages.first() as String)

        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        Map node = nodes.find {Map node ->  "${MISSING_SCOPE}ProductLocation".toString() == node.label}
        assert 'Risk' == node.title
        assert 'Risk' == node.detailsTitle1
        assert 'ProductLocation' == node.detailsTitle2
        String nodeDetails = node.details as String
        assert nodeDetails.contains("*** ${UNABLE_TO_LOAD}fields and traits for ProductLocation")
        assert nodeDetails.contains(DETAILS_LABEL_REASON)
        checkAdditionalScopeIsRequiredMessage(nodeDetails)
        assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE)
        assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS)
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)
    }

    private static void checkAdditionalScopeIsRequiredMessage(String message)
    {
        assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}rpm.scope.class.Risk.traits.Coverages for ProductLocation.")
        assert message.contains("${ADD_SCOPE_VALUES_FOR_KEYS}sourceRisk.")
        assert message.contains("${SCOPE_VALUES_AVAILABLE_FOR}sourceRisk:")
        assert message.contains('GProductOps')
        assert message.contains('ProductLocation')
        assert message.contains('StateOps')
        assert message.contains('WProductOps')
    }

    @Test
    void testBuildGraph_missingRequiredScopeWithNoPreLoadedAvailableScopeValues()
    {
        try
        {
            //Change cube to have a required axis that won't be on the input as the class is loaded
            NCube cube = NCubeManager.getCube(appId, 'rpm.class.Coverage')
            String axisName = 'dummyAxis'
            cube.addAxis(new Axis(axisName, AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED, 3))
            cube.addColumn(axisName, 'dummy1')
            cube.addColumn(axisName, 'dummy2')
            cube.addColumn(axisName, 'dummy3')


            Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                         product:'WProduct',
                         policyControlDate:'2017-01-01',
                         quoteDate:'2017-01-01',
                         risk: 'WProductOps']

            String startCubeName = 'rpm.class.Risk'
            Map options = [startCubeName: startCubeName, scope: scope]

            Map graphInfo = visualizer.buildGraph(appId, options)
            assert STATUS_SUCCESS == graphInfo.status
            Set messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
            assert 1 == messages.size()
            String message = messages.first()
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}WWACoverage of type Coverage.")
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}WCoverage of type Coverage.")
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}CCoverage of type Coverage.")
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}FCoverage of type Coverage.")
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}ACoverage of type Coverage.")
            checkMissingRequiredScopeMessage(message)

            List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
            Map node = nodes.find {Map node ->  "${MISSING_SCOPE}FCoverage".toString() == node.label}
            assert 'Coverage' == node.title
            assert 'Coverage' == node.detailsTitle1
            assert 'FCoverage' == node.detailsTitle2
            String nodeDetails = node.details as String
            assert nodeDetails.contains("*** ${UNABLE_TO_LOAD}fields and traits for FCoverage")
            assert nodeDetails.contains(DETAILS_LABEL_REASON)
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}FCoverage of type Coverage.")
            checkMissingRequiredScopeMessage(nodeDetails)
            assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
            assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE)
            assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
            assert !nodeDetails.contains(DETAILS_LABEL_FIELDS)
            assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
            assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
            assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)
        }
        catch (Exception e)
        {
            //Reset cube
            NCubeManager.loadCube(appId, 'rpm.class.Coverage')
            throw new Exception(e)
        }
        //Reset cube
        NCubeManager.loadCube(appId, 'rpm.class.Coverage')
    }

    private static void checkMissingRequiredScopeMessage(String message)
    {
        assert message.contains("${ADD_SCOPE_VALUES_FOR_KEYS}dummyAxis")
        assert message.contains(SCOPE_VALUES_AVAILABLE_FOR)
        assert message.contains('dummy1')
        assert message.contains('dummy2')
        assert message.contains('dummy3')
    }

    @Test
    void testBuildGraph_missingDeclaredRequiredScope()
    {
        //Change cube to have declared required scope
        NCube cube = NCubeManager.getCube(appId, 'rpm.class.Coverage')
        cube.setMetaProperty('requiredScopeKeys', ['dummyRequiredScopeKey'])

        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     product          : 'WProduct',
                     policyControlDate: '2017-01-01',
                     quoteDate        : '2017-01-01',
                     risk             : 'WProductOps']

        String startCubeName = 'rpm.class.Risk'
        Map options = [startCubeName: startCubeName, scope: scope]

        try
        {
            Map graphInfo = visualizer.buildGraph(appId, options)
            assert STATUS_SUCCESS == graphInfo.status
            Set messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
            assert 1 == messages.size()

            List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List

            String message = messages.first()
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}WWACoverage of type Coverage.")
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}WCoverage of type Coverage.")
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}CCoverage of type Coverage.")
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}FCoverage of type Coverage.")
            assert message.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}ACoverage of type Coverage.")
            assert message.contains("${ADD_SCOPE_VALUES_FOR_KEYS}dummyRequiredScopeKey.")

            Map node = nodes.find {Map node ->  "${MISSING_SCOPE}WWACoverage".toString() == node.label}
            assert 'Coverage' == node.title
            assert 'Coverage' == node.detailsTitle1
            assert 'WWACoverage' == node.detailsTitle2
            String nodeDetails = node.details as String
            assert nodeDetails.contains("*** ${UNABLE_TO_LOAD}fields and traits for WWACoverage")
            assert nodeDetails.contains("${DETAILS_LABEL_REASON}")
            assert nodeDetails.contains("${ADDITIONAL_SCOPE_REQUIRED_TO_LOAD}WWACoverage of type Coverage.")
            assert nodeDetails.contains("${ADD_SCOPE_VALUES_FOR_KEYS}dummyRequiredScopeKey.")
            assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
            assert !nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE)
            assert nodeDetails.contains(DETAILS_LABEL_AVAILABLE_SCOPE)
            assert !nodeDetails.contains(DETAILS_LABEL_FIELDS)
            assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
            assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
            assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)
        }
        catch (Exception e)
        {
            //Reset cube
            cube.removeMetaProperty('requiredScopeKeys')
            throw new Exception(e)
        }
        //Reset cube
        cube.removeMetaProperty('requiredScopeKeys')
    }

    @Test
    void testBuildGraph_missingMinimumTypeScope()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     policyControlDate:'2017-01-01',
                     quoteDate:'2017-01-01']

        String startCubeName = 'rpm.class.Product'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_MISSING_START_SCOPE == graphInfo.status
        Set<String> messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
        assert 1 == messages.size()

        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List
        assert 0 == nodes.size()
        assert 0 == edges.size()

        checkMissingMinimumTypeScopeMessage(messages.first())
    }

    private static void checkMissingMinimumTypeScopeMessage(String message)
    {
        assert message.contains('Scope is required for Product.')
        assert message.contains('Please replace XXXX for Product with an actual scope value.')
        assert message.contains("${SCOPE_VALUES_AVAILABLE_FOR}Product:")
        assert message.contains('GProduct')
        assert message.contains('UProduct')
        assert message.contains('WProduct')
    }

    @Test
    void testBuildGraph_missingMinimumDateScope()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     product:'WProduct']

        String startCubeName = 'rpm.class.Product'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_MISSING_START_SCOPE == graphInfo.status
        Set messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
        assert 1 == messages.size()

        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List
        assert 0 == nodes.size()
        assert 0 == edges.size()

        String message = messages.first()
        assert message.contains('Scope is required for policyControlDate.')
        assert message.contains('Scope is required for quoteDate.')
        assert message.contains(DEFAULT_VALUE_MAY_BE_CHANGED)
    }

    @Test
    void testBuildGraph_unchangedMinimumTypeScope()
    {
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                     product:'XXXX',
                     policyControlDate:'2017-01-01',
                     quoteDate:'2017-01-01']

        String startCubeName = 'rpm.class.Product'
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_MISSING_START_SCOPE == graphInfo.status
        Set<String> messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
        assert 1 == messages.size()
        checkMissingMinimumTypeScopeMessage(messages.first())

        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List
        assert 0 == nodes.size()
        assert 0 == edges.size()
    }

    @Test
    void testBuildGraph_validMinimalRpmClass()
    {
        String startCubeName = 'rpm.class.ValidRpmClass'
        createNCubeWithValidRpmClass(startCubeName)
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION]
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        checkValidRpmClass( startCubeName, scope, graphInfo)
    }

    @Test
    void testBuildGraph_notStartWithRpmClass()
    {
        String startCubeName = 'ValidRpmClass'
        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION]
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_INVALID_START_CUBE == graphInfo.status
        Set messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
        assert 1 == messages.size()
        String message = messages.first()
        assert "Starting cube for visualization must begin with 'rpm.class', n-cube ${startCubeName} does not.".toString() == message
    }

    @Test
    void testBuildGraph_noTraitAxis()
    {
        String startCubeName = 'rpm.class.ValidRpmClass'
        createNCubeWithValidRpmClass(startCubeName)
        NCube cube = NCubeManager.getCube(appId, startCubeName)
        cube.deleteAxis(AXIS_TRAIT)

        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION]
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_INVALID_START_CUBE == graphInfo.status
        Set messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
        assert 1 == messages.size()
        String message = messages.first()
        assert "Cube ${startCubeName} is not a valid rpm class since it does not have both a field axis and a traits axis.".toString() == message
    }

    @Test
    void testBuildGraph_noFieldAxis()
    {
        String startCubeName = 'rpm.class.ValidRpmClass'
        createNCubeWithValidRpmClass(startCubeName)
        NCube cube = NCubeManager.getCube(appId, startCubeName)
        cube.deleteAxis(AXIS_FIELD)

        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION]
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_INVALID_START_CUBE == graphInfo.status
        Set messages = (graphInfo.visInfo as RpmVisualizerInfo).messages
        assert 1 == messages.size()
        String message = messages.first()
        assert "Cube ${startCubeName} is not a valid rpm class since it does not have both a field axis and a traits axis.".toString() == message
    }

    @Test
    void testBuildGraph_no_CLASSTRAITS_Field()
    {
        String startCubeName = 'rpm.class.ValidRpmClass'
        createNCubeWithValidRpmClass(startCubeName)
        NCube cube = NCubeManager.getCube(appId, startCubeName)
        cube.getAxis(AXIS_FIELD).columns.remove(CLASS_TRAITS)

        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION]
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        checkValidRpmClass( startCubeName, scope, graphInfo)
    }

    @Test
    void testBuildGraph_no_rExists_trait()
    {
        String startCubeName = 'rpm.class.ValidRpmClass'
        createNCubeWithValidRpmClass(startCubeName)
        NCube cube = NCubeManager.getCube(appId, startCubeName)
        cube.getAxis(AXIS_TRAIT).columns.remove(R_EXISTS)

        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION]
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        checkValidRpmClass( startCubeName, scope, graphInfo)
    }

    @Test
    void testBuildGraph_no_rRpmType_trait()
    {
        String startCubeName = 'rpm.class.ValidRpmClass'
        createNCubeWithValidRpmClass(startCubeName)
        NCube cube = NCubeManager.getCube(appId, startCubeName)
        cube.getAxis(AXIS_TRAIT).columns.remove(R_RPM_TYPE)

        Map scope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION]
        Map options = [startCubeName: startCubeName, scope: scope]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        assert !(graphInfo.visInfo as RpmVisualizerInfo).messages
        checkValidRpmClass( startCubeName, scope, graphInfo)
    }

    @Test
    void testBuildGraph_invokedWithParentVisualizerInfoClass()
    {
        Map startScope = [_effectiveVersion: ApplicationID.DEFAULT_VERSION,
                          product          : 'WProduct',
                          policyControlDate: '2017-01-01',
                          quoteDate        : '2017-01-01',
                          coverage         : 'FCoverage',
                          risk             : 'WProductOps']

        String startCubeName = 'rpm.class.Coverage'
        VisualizerInfo notRpmVisInfo = new VisualizerInfo()
        notRpmVisInfo.groupSuffix = 'shouldGetReset'

        Map options = [startCubeName: startCubeName, scope: startScope, visInfo: notRpmVisInfo]

        Map graphInfo = visualizer.buildGraph(appId, options)
        assert STATUS_SUCCESS == graphInfo.status
        RpmVisualizerInfo rpmVisInfo = graphInfo.visInfo as RpmVisualizerInfo
        assert null == rpmVisInfo.messages

        assert 'RpmVisualizerInfo' == rpmVisInfo.class.simpleName
        assert '_ENUM' ==  rpmVisInfo.groupSuffix

        Map node = rpmVisInfo.nodes.find { Map node ->'FCoverage' == node.label}
        assert 'COVERAGE' == node.group
    }

    private  static checkValidRpmClass( String startCubeName, Map scope,  Map graphInfo)
    {
        List<Map<String, Object>> nodes = (graphInfo.visInfo as RpmVisualizerInfo).nodes as List
        List<Map<String, Object>> edges = (graphInfo.visInfo as RpmVisualizerInfo).edges as List

        assert nodes.size() == 1
        assert edges.size() == 0
        Map node = nodes.find { startCubeName == (it as Map).cubeName}
        assert 'ValidRpmClass' == node.title
        assert 'ValidRpmClass' == node.detailsTitle1
        assert null == node.detailsTitle2
        assert 'ValidRpmClass' == node.label
        assert  null == node.typesToAdd
        assert 'UNSPECIFIED' == node.group
        assert null == node.fromFieldName
        assert '1' ==  node.level
        assert scope == node.scope
        String nodeDetails = node.details as String
        assert nodeDetails.contains(DETAILS_LABEL_UTILIZED_SCOPE_WITHOUT_ALL_TRAITS)
        assert nodeDetails.contains("${DETAILS_LABEL_FIELDS}</b><pre><ul></ul></pre>")
        assert !nodeDetails.contains(DETAILS_LABEL_FIELDS_AND_TRAITS)
        assert !nodeDetails.contains(DETAILS_LABEL_REASON)
        assert !nodeDetails.contains(DETAILS_LABEL_NOTE)
        assert !nodeDetails.contains(DETAILS_LABEL_CLASS_TRAITS)
    }

    private NCube createNCubeWithValidRpmClass(String cubeName)
    {
        NCube cube = new NCube(cubeName)
        cube.applicationID = appId
        String axisName = 'field'
        cube.addAxis(new Axis(axisName, AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED, 1))
        cube.addColumn(axisName, 'CLASS_TRAITS')
        axisName = 'trait'
        cube.addAxis(new Axis(axisName, AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED, 2))
        cube.addColumn(axisName, 'r:exists')
        cube.addColumn(axisName, 'r:rpmType')
        NCubeManager.addCube(cube.applicationID, cube)
        return cube
    }
}
