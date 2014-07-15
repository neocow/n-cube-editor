package com.cedarsoftware.security;

import javax.xml.ws.BindingProvider;

import junit.framework.TestCase;

import com.gaic.bue.uwd.ra.wsclient.factory.SSOLoginService1Factory;
import com.gaic.bue.uwd.ra.wsclient.sso1.SSOLoginPort;
import com.gaic.bue.uwd.ra.wsclient.util.ProxyFactory;
import com.gaic.esapi.ESAPI;
import com.gaic.esapi.SecurityContext;
import com.gaic.esapi.authentication.SMAuthentication;
import com.gaic.esapi.context.ThreadLocalSecurityContext;

public abstract class BaseWSClientTest extends TestCase
{
  @Override
  public void setUp() throws Exception
  {
    super.setUp();
    ssoLogin("taccount192", "Winter1");
  }

  protected void ssoLogin(String username, String password)
  {
    ESAPI.setSecurityContext(new ThreadLocalSecurityContext());
    SecurityContext ctx = ESAPI.getSecurityContext();
    ctx.setAuthentication(new SMAuthentication(""));

    ProxyFactory<SSOLoginPort> factory = new SSOLoginService1Factory();
    SSOLoginPort service = factory.createProxy();

    ((BindingProvider) service).getRequestContext().put(
      BindingProvider.USERNAME_PROPERTY, username );
    ((BindingProvider) service).getRequestContext().put(
      BindingProvider.PASSWORD_PROPERTY, password);

    String ssoToken = service.login();

    SMAuthentication smAuth = new SMAuthentication(ssoToken);
    ctx.setAuthentication(smAuth);
  }
}
