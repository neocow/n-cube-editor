package com.cedarsoftware.util

import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.exception.InvalidCoordinateException
import groovy.transform.CompileStatic

import static com.cedarsoftware.util.VisualizerConstants.*

/**
 * Provides helper methods to handle exceptions occurring during the execution
 * of n-cube cells for the purpose of producing a visualization.
 */

@CompileStatic
class VisualizerHelper
{
	static String handleUnboundAxes(VisualizerInfo visInfo, VisualizerRelInfo relInfo, Map<String, Set<String>> unboundAxes)
	{
		StringBuilder sb = new StringBuilder()
		sb.append("${BREAK} ${ADD_SCOPE_VALUES_FOR_OPTIONAL_KEYS}${unboundAxes.keySet().join(COMMA_SPACE)}.${BREAK}")
		unboundAxes.each { String key, Set<String> cubeNames ->
			Set<Object> allOptionalScopeValues = []
			Set<String> cubeDisplayNames = []
			cubeNames.each { String cubeName ->
				Set<Object> optionalScopeValues = visInfo.getOptionalScopeValues(cubeName, key)
				allOptionalScopeValues.addAll(optionalScopeValues)
				cubeDisplayNames << relInfo.getCubeDisplayName(cubeName)
			}
			String cubeDisplayNamesString = cubeDisplayNames.join(COMMA_SPACE)
			sb.append(getUnboundAxesMessage(allOptionalScopeValues, cubeDisplayNamesString, key))
		}
		return sb.toString()
	}

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

	static protected Throwable getDeepestException(Throwable e)
	{
		while (e.cause != null)
		{
			e = e.cause
		}
		return e
	}

	static String getUnboundAxesMessage(Set<Object> optionalScopeValues, String cubeNames, String key)
	{
		StringBuilder sb = new StringBuilder()
		sb.append("${BREAK}${SCOPE_VALUES_AVAILABLE_FOR}${key} (on ${cubeNames}):${DOUBLE_BREAK}<pre><ul>")
		if (optionalScopeValues)
		{
			optionalScopeValues.each{
				String value = it.toString()
				sb.append("""<li><a class="missingScope" title="${key}: ${value}" href="#">${value}</a></li>""")
			}
		}
		else
		{
			sb.append(NONE)
		}
		sb.append("</ul></pre>")
		return sb.toString()
		return ''
	}

	static String getScopeValuesMessage(String key, Set<Object> scopeValues)
	{
		StringBuilder sb = new StringBuilder()
		sb.append("${DOUBLE_BREAK}<pre><ul>")
		if (scopeValues)
		{
			scopeValues.each{
				String value = it.toString()
				sb.append("""<li><a class="missingScope" title="${key}: ${value}" href="#">${value}</a></li>""")
			}
		}
		else
		{
			sb.append(NONE)
		}
		sb.append("</ul></pre>")
		return sb.toString()
	}

	private static String getInvalidCoordinateExceptionMessage(VisualizerInfo visInfo, Set<String> missingScope, String cubeName)
	{
		StringBuilder message = new StringBuilder()
		message.append("${DOUBLE_BREAK} ${ADD_SCOPE_VALUES_FOR_REQUIRED_KEYS}${missingScope.join(COMMA_SPACE)}.${BREAK}")
		missingScope.each{ String key ->
			message.append("${BREAK}${SCOPE_VALUES_AVAILABLE_FOR}${key}:")
			Set<Object> requiredScopeValues = visInfo.getRequiredScopeValues(cubeName, key)
			message.append(getScopeValuesMessage(key, requiredScopeValues))
		}
		return message.toString()
	}

	private static String getCoordinateNotFoundMessage(VisualizerInfo visInfo, String key, String cubeName)
	{
		StringBuilder message = new StringBuilder()
		message.append("${DOUBLE_BREAK} ${SUPPLY_DIFFERENT_VALUE_FOR}${key}.")
		Set<Object> requiredScopeValues = visInfo.getRequiredScopeValues(cubeName, key)
		message.append(getScopeValuesMessage(key, requiredScopeValues))
		return message.toString()
	}

	private static Set<String> findMissingScope(Map<String, Object> scope, Set<String> requiredKeys, Set mandatoryScopeKeys)
	{
		return requiredKeys.findAll { String key ->
			!mandatoryScopeKeys.contains(key) && (scope == null || !scope.containsKey(key))
		}
	}

	protected static String getMissingMinimumScopeMessage(Map<String, Object> scope, String messageScopeValues, String messageSuffixType, String messageSuffix )
	{
		"""\
${SCOPE_ADDED_SINCE_REQUIRED} \
${DOUBLE_BREAK}${INDENT}${scope.keySet().join(COMMA_SPACE)}\
${messageSuffixType} ${messageSuffix} \
${BREAK}${messageScopeValues}"""
	}

	static String getExceptionMessage(Throwable t, Throwable e, String targetMsg)
	{
		"""\
An exception was thrown while loading ${targetMsg}. \
${DOUBLE_BREAK}<b>Message:</b> ${DOUBLE_BREAK}${e.message}${DOUBLE_BREAK}<b>Root cause: </b>\
${DOUBLE_BREAK}${t.toString()}${DOUBLE_BREAK}<b>Stack trace: </b>${DOUBLE_BREAK}${t.stackTrace.toString()}"""
	}
}