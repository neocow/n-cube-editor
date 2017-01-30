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
		String scopeKeysString = "${axisNames.join(COMMA_SPACE)}.${BREAK}"
		sb.append(BREAK)
		String message = axisNames.size() > 1 ? 'Scope values may be supplied for: ' : 'A scope value may be supplied for '
		sb.append("${message}${scopeKeysString}.")
		axisNames.each{ String axisName ->
			sb.append(getScopeValuesMessage(axisName, unboundAxesMap[axisName], axisName))
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

	static String getScopeValuesMessage(String scopeKey, Set<Object> scopeValues, String availableFor)
	{
		StringBuilder sb = new StringBuilder()
		sb.append("""${BREAK}<div class="input-group input-group-sm">""")
		sb.append("""<select class="${DETAILS_CLASS_FORM_CONTROL} ${DETAILS_CLASS_MISSING_SCOPE}">""")
		sb.append("<option>Values available for ${availableFor}</option>")
		if (scopeValues)
		{
			scopeValues.each{
				String value = it.toString()
				sb.append("""<option title="${scopeKey}: ${value}">${value}</option>""")
			}
		}
		else
		{
			sb.append("""<option title="${scopeKey}: none">none</option>""")
		}
		sb.append("</select>")
		sb.append("""</div>""")
		return sb.toString()
	}

	private static String getInvalidCoordinateExceptionMessage(VisualizerInfo visInfo, Set<String> missingScope, String cubeName)
	{
		StringBuilder message = new StringBuilder()

		String msg = missingScope.size() > 1 ? 'Scope values must be supplied for: ' : 'A scope value must be supplied for '
		message.append("${msg}${missingScope.join(COMMA_SPACE)}.${BREAK}")
		missingScope.each{ String scopeKey ->
			Set<Object> requiredScopeValues = visInfo.getRequiredScopeValues(cubeName, scopeKey)
			message.append(getScopeValuesMessage(scopeKey, requiredScopeValues, scopeKey))
		}
		return message.toString()
	}

	private static String getCoordinateNotFoundMessage(VisualizerInfo visInfo, String scopeKey, String cubeName)
	{
		StringBuilder message = new StringBuilder()
		Set<Object> requiredScopeValues = visInfo.getRequiredScopeValues(cubeName, scopeKey)
		message.append(BREAK + getScopeValuesMessage(scopeKey, requiredScopeValues, scopeKey))
		return message.toString()
	}

	private static Set<String> findMissingScope(Map<String, Object> scope, Set<String> requiredKeys, Set mandatoryScopeKeys)
	{
		return requiredKeys.findAll { String scopeKey ->
			!mandatoryScopeKeys.contains(scopeKey) && (scope == null || !scope.containsKey(scopeKey))
		}
	}

	protected static String getMissingMinimumScopeMessage(Map<String, Object> scope, String messageScopeValues, String messageSuffixType)
	{
		"""\
The scope for the following scope keys was added since required. The default scope values may be changed as desired. \
${DOUBLE_BREAK}${INDENT}${scope.keySet().join(COMMA_SPACE)}\
${messageSuffixType} \
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