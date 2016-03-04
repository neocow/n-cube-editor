package com.cedarsoftware.controller

class TestNCubeController
{

    void testGetVisualizerJson()
    {
        Map options = [scope: [:], startCubeName: '']
        rule.input.options.scope = createScope()
        rule.input.options.scope.policyControlDate = '2015-01-01'
        rule.input.options.scope.quoteDate = '2015-01-01'
        rule.input.options.startCubeName = 'rpm.class.coverage.WorkersCompensationProduct.WCOps.StopGapCoverage'
    }


}
