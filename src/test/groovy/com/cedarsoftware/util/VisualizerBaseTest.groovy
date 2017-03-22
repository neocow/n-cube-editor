package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.NCubeResourcePersister
import com.cedarsoftware.ncube.ReleaseStatus
import groovy.transform.CompileStatic
import org.junit.Before

import static com.cedarsoftware.util.VisualizerConstants.DATE_TIME_FORMAT
import static com.cedarsoftware.util.VisualizerConstants.DETAILS_CLASS_TOP_NODE
import static com.cedarsoftware.util.VisualizerTestConstants.ENTER_VALUE
import static com.cedarsoftware.util.VisualizerTestConstants.SELECT_OR_ENTER_VALUE


@CompileStatic
class VisualizerBaseTest
{
    protected static final String DEFAULT_SCOPE_DATE = DATE_TIME_FORMAT.format(new Date())

    Visualizer visualizer
    ApplicationID appId
    Map returnMap
    VisualizerInfo visInfo
    Set messages
    Map<Long, Map<String, Object>> nodes
    Map<Long, Map<String, Object>> edges
    Map<String, Object> selectedNode

    @Before
    void beforeTest(){
        appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, 'test.visualizer', ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name(), ApplicationID.HEAD)
        visualizer = getVisualizer()
        returnMap = null
        visInfo = null
        messages = null
        nodes = null
        edges = null
        selectedNode = null
        NCubeManager.NCubePersister = new NCubeResourcePersister(pathPrefix)
    }

    protected Visualizer getVisualizer()
    {
        return new Visualizer()
    }

    protected String getPathPrefix()
    {
        return 'visualizer*//**//*'
    }

    protected Map loadGraph(Map options, boolean hasMessages = false)
    {
        visInfo?.nodes = [:]
        visInfo?.edges = [:]
        Map returnMap = visualizer.loadGraph(appId, options)
        visInfo = returnMap.visInfo as VisualizerInfo
        messages = visInfo.messages
        if (!hasMessages)
        {
            assert !messages
        }
        nodes = visInfo.nodes as Map
        edges = visInfo.edges as Map
        return nodes[1l]
    }

    protected Map loadScopeChange(Map node, boolean hasMessages = false)
    {
        visInfo.selectedNodeId = node.id as Long
        Map returnMap = visualizer.loadScopeChange(appId, [visInfo: visInfo])
        visInfo = returnMap.visInfo as VisualizerInfo
        messages = visInfo.messages
        if (!hasMessages)
        {
            assert !messages
        }
        nodes = visInfo.nodes as Map
        edges = visInfo.edges as Map
        return nodes[node.id as Long]
    }

    protected Map loadNodeDetails(Map node, boolean hasMessages = false)
    {
        visInfo.selectedNodeId = node.id as Long
        Map returnMap = visualizer.loadNodeDetails(appId, [visInfo: visInfo])
        visInfo = returnMap.visInfo as VisualizerInfo
        messages = visInfo.messages
        if (!hasMessages)
        {
            assert !messages
        }
        nodes = visInfo.nodes as Map
        edges = visInfo.edges as Map
        return nodes[node.id as Long]
    }

    protected static void checkScopePromptTitle(Map node, String scopeKey, boolean required, String cubeNames = null, boolean derivedScopeKey = false, boolean unusedScopeKey = false)
    {
        String nodeDetails = node.details as String
        if (derivedScopeKey)
        {
            assert nodeDetails.contains("""title="Scope key ${scopeKey} was added by the visualizer and may not be changed""")
        }
        else if (unusedScopeKey)
        {
            assert nodeDetails.contains("""title="Scope key ${scopeKey} was added for a source class of this class, but is not used by this class""")
        }
        else if (required)
        {
            assert nodeDetails.contains("""title="Scope key ${scopeKey} is required to load""")
        }
        else
        {
            assert nodeDetails.contains("""title="Scope key ${scopeKey} is optional to load""")
        }
        if (cubeNames)
        {
            assert nodeDetails.contains(cubeNames)
        }
    }

    protected static void checkScopePromptDropdown(Map node, String scopeKey, String selectedScopeValue, List<String> availableScopeValues, List<String> unavailableScopeValues, String valueClass = '', boolean isTopNode = false, boolean isDerivedScopeKey = false)
    {
        String nodeDetails = node.details as String
        String placeHolder = availableScopeValues ? SELECT_OR_ENTER_VALUE : ENTER_VALUE
        String topNodeClass = isTopNode ? DETAILS_CLASS_TOP_NODE : ''
        String disabled = isDerivedScopeKey ? 'disabled="disabled"' : ''
        assert nodeDetails.contains("""<input id="${scopeKey}" value="${selectedScopeValue}" ${disabled} placeholder="${placeHolder}" class="scopeInput form-control ${valueClass} ${topNodeClass}""")
        if (!availableScopeValues && !unavailableScopeValues)
        {
            assert !nodeDetails.contains("""<li id="${scopeKey}:""")
            return
        }

        availableScopeValues.each{String scopeValue ->
            assert nodeDetails.contains("""<li id="${scopeKey}: ${scopeValue}" class="scopeClick ${topNodeClass}""")
        }
        unavailableScopeValues.each{String scopeValue ->
            assert !nodeDetails.contains("""<li id="${scopeKey}: ${scopeValue}" class="scopeClick ${topNodeClass}""")
        }
    }

    protected static void checkNoScopePrompt(Map node, String scopeKey = '')
    {
        String nodeDetails = node.details as String
        assert !nodeDetails.contains("""title="${scopeKey}""")
        assert !nodeDetails.contains("""<input id="${scopeKey}""")
        assert !nodeDetails.contains("""<li id="${scopeKey}""")
    }
}
