package com.cedarsoftware.dto;


/**
 * Represents a logged in user.
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
public class User
{
	private String hid;
	private String userid;
	private String password;
	private String fname;
	private String lname;

	public String getHid() {
		return hid;
	}

	public void setHid(String hid) {
		this.hid = hid;
	}

	public String getUserid()
	{
		return userid;
	}
	
	public void setUserid(String id)
	{
		userid = id;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public void setPassword(String pwd)
	{
		password = pwd;
	}
	
	public String getFname()
	{
		return fname;
	}
	
	public void setFname(String name)
	{
		fname = name;
	}
	
	public String getLname()
	{
		return lname;
	}
	
	public void setLname(String name)
	{
		lname = name;
	}
}
