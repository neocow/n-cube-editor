package com.cedarsoftware.controller;

import com.cedarsoftware.security.Security;
import com.cedarsoftware.security.SecurityManager;
import org.apache.log4j.Logger;

/**
 * Common functionality for all Controllers.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class BaseController implements IBaseController
{
    private static final Logger _log = Logger.getLogger(BaseController.class);
	private Security security = new SecurityManager();

	public void logout()
	{
	}
	
	protected String getHID()
	{
		return security.getUserId();
	}
    
    protected Security getSecurity()
    {
    	return security;
    }
   
}
