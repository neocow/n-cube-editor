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
		String coordinateString = coordinateString

		if (exception)
		{
			//An exception was caught during the execution of the cell.
			sb.append(getExceptionDetails(visInfo, visRelInfo, coordinateString))
		}
		else
		{
			//The cell has a value or a null value
			sb.append(cellDetails)
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

	private String getCellDetails()
	{
		String noExecuteValue = HtmlFormatter.getCellValueAsString(noExecuteCell)
		String cellString = HtmlFormatter.getCellValueAsString(cell)
		StringBuilder sb = new StringBuilder()
		sb.append("""<li class="executedCell" title="Executed cell"><a href="#" class="executedCell" id="${nodeId}">${coordinateString}</a></li>""")
		sb.append("<pre>")
		sb.append("<b>Non-executed value:</b>")
		sb.append(DOUBLE_BREAK)
		sb.append("${noExecuteValue}")
		sb.append(DOUBLE_BREAK)
		sb.append("<b>Executed value:</b>")
		sb.append(DOUBLE_BREAK)
		if (cell && cellString.startsWith(HTTP) || cellString.startsWith(HTTPS) || cellString.startsWith(FILE))
		{
			sb.append("""<a href="#" onclick='window.open("${cellString}");return false;'>${cellString}</a></a></li>""")
		}
		else
		{
			sb.append("${cellString}")
		}
		sb.append("</pre>")
		return sb.toString()
	}

	private String getExceptionDetails(VisualizerInfo visInfo, VisualizerRelInfo relInfo, String coordinateString)
	{
		StringBuilder sb = new StringBuilder()
		StringBuilder mb = new StringBuilder()
		String noExecuteValue = HtmlFormatter.getCellValueAsString(noExecuteCell)
		Throwable t = helper.getDeepestException(exception)
		String className
		String title

		if (t instanceof InvalidCoordinateException)
		{
			title = 'The cell was executed with a missing or invalid coordinate.'
			className = t.class.simpleName
			mb.append("Additional scope is required to load coordinate ${coordinateString}.")
			mb.append(helper.handleInvalidCoordinateException(t as InvalidCoordinateException, visInfo, relInfo, [] as Set).toString())
		}
		else if (t instanceof CoordinateNotFoundException)
		{
			title = 'The cell was executed with a missing or invalid coordinate.'
			className = t.class.simpleName
			CoordinateNotFoundException exc = t as CoordinateNotFoundException
			String key = exc.axisName
			Object value = exc.value ?: 'null'
			String targetMsg = "coordinate ${coordinateString}"
			mb.append("The scope value ${value} for scope key ${key} cannot be found on axis ${key} for ${targetMsg}.")
			mb.append(helper.handleCoordinateNotFoundException(t as CoordinateNotFoundException, visInfo, targetMsg))
		}
		else
		{
			title = 'An error occurred during the execution of the cell.'
			className = 'Exception'
			String targetMsg = "coordinate ${coordinateString}"
			mb.append(helper.handleException(t, targetMsg))
		}

		sb.append("""<li class="${className}" title="${title}"><a href="#" class="${className}" id="${nodeId}">${coordinateString}</a></li>""")
		sb.append("<pre>")
		sb.append("<b>Non-executed value:</b>")
		sb.append(DOUBLE_BREAK)
		sb.append("${noExecuteValue}")
		sb.append(DOUBLE_BREAK)
		sb.append("<b>Exception:</b>")
		sb.append(DOUBLE_BREAK)
		sb.append("${mb.toString()}>")
		sb.append("</pre>")
		return sb.toString()
	}
}