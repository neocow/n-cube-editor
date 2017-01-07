package com.cedarsoftware.util

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.exception.InvalidCoordinateException
import com.cedarsoftware.ncube.formatters.HtmlFormatter
import com.google.common.base.Joiner
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Holds information about an n-cube cell that is part of a visualization.
 */

@CompileStatic
class VisualizerCellInfo
{
	ApplicationID appId
    String nodeId

	Map<String, Object> coordinate
	Object noExecuteCell
	Object cell
	Exception exception

	protected Joiner.MapJoiner mapJoiner = Joiner.on(", ").withKeyValueSeparator(": ")
	VisualizerHelper helper = new VisualizerHelper()

	VisualizerCellInfo(String nodeId, Map<String, Object> coordinate)
	{
		this.coordinate = coordinate
		this.nodeId = nodeId
	}

	void getCellValue(VisualizerInfo visInfo, VisualizerRelInfo visRelInfo, StringBuilder sb)
	{
		if (exception)
		{
			//An exception was caught during the execution of the cell.
			sb.append("""<li class="errorCell">${coordinateString} <b>==></b> ${noExecuteCell}</li>""")
			sb.append(getExceptionDetails(visInfo, visRelInfo, coordinateString))
		}
		else if (null != cell)
		{
			//The cell was executed successfully and has a value.
			String cellString = HtmlFormatter.getCellValueAsString(cell)
			String coordinateString = coordinateString
			if (cellString.startsWith(HTTP) || cellString.startsWith(HTTPS) || cellString.startsWith(FILE))
			{
				sb.append("""<li class="cell">${coordinate} <b>==></b>  <a href="#" onclick='window.open("${cellString}");return false;'>${cellString}</a></li>""")
			}
			else
			{
				sb.append("""<li class="cell">${coordinateString} <b>==></b> ${cellString}</li>""")
			}
		}
		else if (noExecuteCell)
		{
			//The cell was not executed. Display as a link. If clicked, the cell is re-executed.
			sb.append("""<li class="noExecuteCell"><a  id="${nodeId}" title="${coordinateString}" href="#">${coordinateString} <b>==></b>  ${noExecuteCell}</a></li>""")
		}
		else
		{
			//The cell was executed successfully and has a null value.
			sb.append("""<li class="cell">${coordinateString} <b>==></b>null</li>""")
		}
	}

	private String getCoordinateString()
	{
		coordinate.each {String key, Object value ->
			if (!value)
			{
				coordinate[key] = 'null'
			}
		}
		return mapJoiner.join(coordinate)
	}

	private String getExceptionDetails(VisualizerInfo visInfo, VisualizerRelInfo relInfo, String coordinateString)
	{
		StringBuilder sb = new StringBuilder()
		StringBuilder mb = new StringBuilder()
		String msg
		Throwable t = helper.getDeepestException(exception)
		if (t instanceof InvalidCoordinateException)
		{
			mb.append("Additional scope is required to load coordinate ${coordinateString}.")
			mb.append(helper.handleInvalidCoordinateException(t as InvalidCoordinateException, visInfo, relInfo, [] as Set).toString())
		}
		else if (t instanceof CoordinateNotFoundException)
		{
			CoordinateNotFoundException exc = t as CoordinateNotFoundException
			String key = exc.axisName
			Object value = exc.value ?: 'null'
			String targetMsg = "coordinate ${coordinateString}"
			mb.append("The scope value ${value} for scope key ${key} cannot be found on axis ${key} for ${targetMsg}.")
			mb.append(helper.handleCoordinateNotFoundException(t as CoordinateNotFoundException, visInfo, targetMsg))
		}
		else if (t instanceof IllegalArgumentException)
		{
			//TODO: do something special for IllegalArgumentException as well + add to RpmVisualizerRelInfo
			String targetMsg = "coordinate ${coordinateString}"
			mb.append(helper.handleException(t, targetMsg))
		}
		else
		{
			String targetMsg = "coordinate ${coordinateString}"
			mb.append(helper.handleException(t, targetMsg))
		}

		//TODO: Make message show/hide via user action
		sb.append("<pre>${mb.toString()}</pre>${BREAK}")
		return sb.toString()
	}
}