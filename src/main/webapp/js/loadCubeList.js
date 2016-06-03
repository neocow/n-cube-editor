/**
 * Web Worker thread
 *
 *     This Javascript background thread is used to filter the list of cubes
 * based on the passed in filter string.  The data passed into onmessage(),
 * which comes from a postMessage(), is an array [] of three (3) elements:
 * [0] = cube name filter list (cube names will be reduce to only those that 'contain' these characters, case-ins)
 * [1] = content filter string (cubes will further be reduced to those that have JSON content that 'contain' these characters, case-ins)
 * [2] = ApplicationID { 'app': 'foo', 'version':'1.0.0', 'status':'SNAPSHOT', 'branch':'joe'}
 *
 * @return a stable list of cubes, filtered by the passed in string.
 * @author John DeRegnaucourt
 */

onmessage = function(e) {
    var args = e.data;
    var filter = args[0];
    var content = args[1];
    var appId = args[2];
    var regex = args[3];
    var hasFilter = filter && filter.length > 0;
    var appIdString = JSON.stringify(appId);
    var req = new XMLHttpRequest();
    var searchResults;
    var cubes;
    var results;
    var i;
    var len;

    req.open("POST", getSearchUrl(), false);
    req.send('[' + appIdString + ',"' + filter + '","' + content + '",true]');

    if (req.response) {
        searchResults = JSON.parse(req.response);
        if (searchResults.status === true) {
            cubes = {};
            results = searchResults.data;
            for (i = 0, len = results.length; i < len; ++i) {
                var infoDto = results[i];
                cubes[infoDto.name.toLowerCase()] = infoDto;
                if (hasFilter) {
                    if (regex) {
                        var array = regex.exec(infoDto.name);
                        if (array) {
                            infoDto.pos = array.index;
                            infoDto.endPos = array.index + array[0].length;
                        }
                    }
                }
            }

            postMessage(cubes);
        }
    }

    function getSearchUrl() {
        var regexp = /\/([^\/]+)\//g;
        var match = regexp.exec(location.pathname);
        var url;
        var ctx;

        if (match == null || match.length != 2)
        {
            url = location.protocol + '//' + location.hostname + ":" + location.port;
        }
        else
        {
            ctx = match[1];
            url = location.protocol + '//' + location.hostname + ":" + location.port + "/" + ctx;
        }
        url += '/cmd/ncubeController/search';
        return url;
    }
};
