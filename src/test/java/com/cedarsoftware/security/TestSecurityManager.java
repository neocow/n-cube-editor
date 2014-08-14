package com.cedarsoftware.security;

import com.gaic.esapi.Authentication;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestSecurityManager extends BaseWSClientTest
{
    @Test
    public void testGetAuthentication()
    {
        SecurityManager sManager = new SecurityManager();
        Authentication auth = sManager.getAuthentication();
        assertNotNull(auth);
    }
}
