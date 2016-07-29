package com.cedarsoftware.util

import com.cedarsoftware.controller.NCubeController
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils

import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener

/**
 * NCubeController API.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License")
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
@WebListener
class NceServletContextListener implements ServletContextListener
{
    private static final Logger LOG = LogManager.getLogger(NceServletContextListener.class)

    void contextInitialized(ServletContextEvent servletContextEvent)
    {
//        println '===> Starting NCE <==='
    }

    void contextDestroyed(ServletContextEvent event)
    {
//        println '===> Stopping NCE <==='
//
//        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(event.getServletContext())
//        NCubeController controller = (NCubeController) springContext.getBean("ncubeController")
//        if (controller == null)
//        {
//            println 'controller not found'
//        }
//        else
//        {
//            println '===> shutting down NCE <==='
//        }
//
//        println 'memcachedClient = ' + controller.memcachedClient
//        controller.getMemcachedClient().shutdown()
    }
}
