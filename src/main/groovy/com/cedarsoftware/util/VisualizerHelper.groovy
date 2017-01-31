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
	static String handleUnboundAxes(Map<String, Set<Object>> unboundAxesMap)
	{
		StringBuilder sb = new StringBuilder()
		Set axisNames = unboundAxesMap.keySet()
		axisNames.each{ String axisName ->
			sb.append(BREAK + getScopeValueMessage(axisName, unboundAxesMap[axisName], true))
		}
		return sb.toString()
	}

	static String handleCoordinateNotFoundException(CoordinateNotFoundException e, VisualizerInfo visInfo, String targetMsg )
	{
		String cubeName = e.cubeName
		String axisName = e.axisName
		if (cubeName && axisName)
		{
			return getScopeValuesMessage(visInfo, [axisName] as Set, cubeName)
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
			return getScopeValuesMessage(visInfo, missingScope, e.cubeName)
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

	private static String getScopeValuesMessage(VisualizerInfo visInfo, Set<String> missingScope, String cubeName)
	{
		StringBuilder message = new StringBuilder()
		missingScope.each{ String scopeKey ->
			Set<Object> requiredScopeValues = visInfo.getRequiredScopeValues(cubeName, scopeKey)
			message.append(BREAK + getScopeValueMessage(scopeKey, requiredScopeValues))
		}
		return message.toString()
	}

	static String getScopeValueMessage(String scopeKey, Set<Object> scopeValues, boolean isUnboundAxis = false)
	{
		StringBuilder sb = new StringBuilder()
		if (scopeValues)
		{
			sb.append("""<div class="input-group input-group-sm">""")
			String selectTag = """<select class="${DETAILS_CLASS_FORM_CONTROL} ${DETAILS_CLASS_MISSING_SCOPE}">"""
			if (isUnboundAxis)
			{
				sb.append("A different scope value may be supplied for ${scopeKey}:")
				sb.append(selectTag)
				sb.append('<option>Default</option>')
			}
			else
			{
				sb.append("A scope value must be supplied for ${scopeKey}:")
				sb.append(selectTag)
				sb.append('<option>Select...</option>')
			}
			scopeValues.each {
				String value = it.toString()
				sb.append("""<option title="${scopeKey}: ${value}">${value}</option>""")
			}
			sb.append('</select>')
			sb.append('</div>')
		}
		else
		{
			if (isUnboundAxis)
			{
				sb.append("Default is the only option for ${scopeKey}.")
			}
			else
			{
				sb.append("Enter a value for ${scopeKey} manually since there are none to choose from.")
			}
		}
		return sb.toString()
	}

	private static Set<String> findMissingScope(Map<String, Object> scope, Set<String> requiredKeys, Set mandatoryScopeKeys)
	{
		return requiredKeys.findAll { String scopeKey ->
			!mandatoryScopeKeys.contains(scopeKey) && (scope == null || !scope.containsKey(scopeKey))
		}
	}

	protected static String getMissingMinimumScopeMessage(Map<String, Object> scope, String messageScopeValues)
	{
		"""\
The scope for the following scope keys was added since required. The default scope values may be changed as desired. \
${DOUBLE_BREAK}${INDENT}${scope.keySet().join(COMMA_SPACE)}\
${BREAK} \
${messageScopeValues}"""
	}

	static String getExceptionMessage(Throwable t, Throwable e, String targetMsg)
	{
		"""\
An exception was thrown while loading ${targetMsg}. \
${DOUBLE_BREAK}<b>Message:</b> ${DOUBLE_BREAK}${e.message}${DOUBLE_BREAK}<b>Root cause: </b>\
${DOUBLE_BREAK}${t.toString()}${DOUBLE_BREAK}<b>Stack trace: </b>${DOUBLE_BREAK}${t.stackTrace.toString()}"""
	}
}