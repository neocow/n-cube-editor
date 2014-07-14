package com.cedarsoftware.security;

import org.apache.log4j.Logger;

import com.gaic.util.cpr.client.CPRPropertyProvider;

public class SecurityManager implements Security
{
	private static final String LASTNAME = "LASTNAME";
	private static final String FIRSTNAME = "FIRSTNAME";
	private static final String HID = "HID";
	private static final Logger _log = Logger.getLogger(SecurityManager.class);

    public String getUserId()
    {
        return "Jed";
    }
    
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
}
