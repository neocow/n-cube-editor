package com.cedarsoftware.security;

import org.junit.Test;

import com.gaic.esapi.Authentication;


public class TestSecurityManager extends BaseWSClientTest {
	
	@Test
	  public void testGetAuthentication()
	  {
		SecurityManager sManager = new SecurityManager();
		Authentication auth = sManager.getAuthentication();
		assertNotNull(auth);
	  }
}
