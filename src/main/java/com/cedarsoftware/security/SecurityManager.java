package com.cedarsoftware.security;

import org.apache.log4j.Logger;

import com.gaic.esapi.Authentication;
import com.gaic.esapi.ESAPI;
import com.gaic.esapi.Protectable;
import com.gaic.esapi.Resource;
import com.gaic.esapi.decision.GenericAction;
import com.gaic.esapi.decision.GenericResource;
import com.gaic.util.cpr.client.CPRPropertyProvider;

public class SecurityManager implements Security
{
	private static final String LASTNAME = "LASTNAME";
	private static final String FIRSTNAME = "FIRSTNAME";
	private static final String HID = "HID";
	private static final Logger _log = Logger.getLogger(SecurityManager.class);

	public SecurityManager()
	{
		CPRPropertyProvider cprPropertyProvider = CPRPropertyProvider.getInstance();
		String username;
		try
		{
			username = cprPropertyProvider.getStringProperty("com.gaic.executivedashboard.securityframework.test.username");
			_log.debug(String.format("ESAPI username [%s]", username));
		}
        catch(Exception e)
        {
			_log.error("Error occurred calling CPR",e);
		}
	}
	
	public Authentication getAuthentication()
	{
		return ESAPI.getSecurityContext().getAuthentication();
	}

	public boolean checkPermission(Protectable protectable)
	{
		return ESAPI.getAccessDecisionManager().decide(protectable).isGranted();
	}

	public String getLoggedInUserName()
	{
		return getAuthentication().getName();
	}

	public String getHID()
	{
		return (String)getAuthentication().getAttributes().get(HID);
	}

	public String getFirstName()
	{
		return (String)getAuthentication().getAttributes().get(FIRSTNAME);
	}

	public String getLastName()
	{
		return (String)getAuthentication().getAttributes().get(LASTNAME);
	}

	public boolean hasAccess(String action, String resource, String resAttrKey, String resAttrValue)
	{
		Resource res = new GenericResource(resource, new GenericAction(action));
		res.getAttributes().put(resAttrKey, resAttrValue);
		return checkPermission(res);
	}

	public boolean hasAccess(String action, String resource)
	{
		return checkPermission(new GenericResource(resource, new GenericAction(action)));
	}

	public boolean hasAccess(String action, String resource, Object[] resAttrEntry)
	{
		Resource res = new GenericResource(resource, new GenericAction(action));
		for (int i=0; i < resAttrEntry.length; i++)
		{
			Object[] pair = (Object[])resAttrEntry[i];
			res.getAttributes().put((String) pair[0], pair[1]);
		}
		return checkPermission(res);
	}

}
