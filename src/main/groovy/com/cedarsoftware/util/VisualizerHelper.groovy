package com.cedarsoftware.util

import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.exception.InvalidCoordinateException
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*


@CompileStatic
class VisualizerHelper
{
	static String handleCoordinateNotFoundException(CoordinateNotFoundException e, VisualizerInfo visInfo, String targetMsg )
	{
		String cubeName = e.cubeName
		String axisName = e.axisName
		if (cubeName && axisName)
		{
			return getCoordinateNotFoundMessage(visInfo, axisName, cubeName)
		}
		else
		{
			return handleException(e as Exception, targetMsg)
		}
	}


	static String handleInvalidCoordinateException(InvalidCoordinateException e, VisualizerInfo visInfo, VisualizerRelInfo relInfo, Set mandatoryScopeKeys)
	{
		Set<String> missingScope = findMissingScope(relInfo.scope, e.requiredKeys, mandatoryScopeKeys)
		if (missingScope)
		{
			Map<String, Object> expandedScope = new CaseInsensitiveMap<>(visInfo.scope)
			missingScope.each { String key ->
				expandedScope[key] = DEFAULT_SCOPE_VALUE
			}
			visInfo.scope = expandedScope
			return getInvalidCoordinateExceptionMessage(visInfo, missingScope, e.cubeName)
		}
		else
		{
			throw new IllegalStateException("InvalidCoordinateException thrown, but no missing scope keys found for ${relInfo.targetCube.name} and scope ${visInfo.scope.toString()}.", e)
		}
	}

	static String handleException(Throwable e, String targetMsg)
	{
		Throwable t = getDeepestException(e)
		return getExceptionMessage(t, e, targetMsg)
	}

	static String getAvailableScopeValuesMessage(VisualizerInfo visInfo, String cubeName, String key)
	{
		Set<Object> scopeValues = visInfo.availableScopeValues[key] ?: visInfo.loadAvailableScopeValues(cubeName, key)
		if (scopeValues) {
			StringBuilder sb = new StringBuilder()
			sb.append("${BREAK}The following values are available for ${key}:${DOUBLE_BREAK}<pre><ul>")
			scopeValues.each{
				String value = it.toString()
				sb.append("""<li><a class="missingScope" title="${key}: ${value}" href="#">${value}</a></li>""")
			}
			sb.append("</ul></pre>")
			return sb.toString()
		}
		return ''
	}

	private static String getInvalidCoordinateExceptionMessage(VisualizerInfo visInfo, Set<String> missingScope, String cubeName)
	{
		StringBuilder message = new StringBuilder()
		message.append("${DOUBLE_BREAK} Please add scope value(s) for the following scope key(s): ${missingScope.join(COMMA_SPACE)}.${BREAK}")
		missingScope.each{ String key ->
			message.append(getAvailableScopeValuesMessage(visInfo, cubeName, key))
		}
		return message.toString()
	}

	static protected Throwable getDeepestException(Throwable e)
	{
		while (e.cause != null)
		{
			e = e.cause
		}
		return e
	}

	private static String getCoordinateNotFoundMessage(VisualizerInfo visInfo, String key, String cubeName)
	{
		StringBuilder message = new StringBuilder()
		String messageScopeValues = getAvailableScopeValuesMessage(visInfo, cubeName, key)
		message.append("${DOUBLE_BREAK} Please supply a different value for ${key}.${BREAK}${messageScopeValues}")
		return message.toString()
	}

	private static Set<String> findMissingScope(Map<String, Object> scope, Set<String> requiredKeys, Set mandatoryScopeKeys)
	{
		return requiredKeys.findAll { String key ->
			!mandatoryScopeKeys.contains(key) && (scope == null || !scope.containsKey(key))
		}
	}

	static String getExceptionMessage(Throwable t, Throwable e, String targetMsg)
	{
		"""\
An exception was thrown while loading ${targetMsg}. \
${DOUBLE_BREAK}<b>Message:</b> ${DOUBLE_BREAK}${e.message}${DOUBLE_BREAK}<b>Root cause: </b>\
${DOUBLE_BREAK}${t.toString()}${DOUBLE_BREAK}<b>Stack trace: </b>${DOUBLE_BREAK}${t.stackTrace.toString()}"""
	}
}