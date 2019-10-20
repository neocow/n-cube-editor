package com.cedarsoftware.ncube

import com.cedarsoftware.servlet.JsonCommandServlet
import com.cedarsoftware.util.ArrayUtilities
import com.cedarsoftware.util.Converter
import groovy.transform.CompileStatic
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootVersion
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import org.springframework.context.annotation.Profile
import org.springframework.core.SpringVersion
import org.springframework.core.env.Environment
import org.springframework.web.filter.FormContentFilter
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.filter.HiddenHttpMethodFilter
import org.springframework.web.filter.RequestContextFilter

import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * This class defines allowable actions against persisted n-cubes
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
@ImportResource("classpath:config/ncube-beans.xml")
//@SpringBootApplication(exclude = [DataSourceAutoConfiguration, DataSourceTransactionManagerAutoConfiguration, HibernateJpaAutoConfiguration])
@SpringBootApplication
@CompileStatic
class NCubeApplication
{
    private static final Logger LOG = LogManager.getLogger(NCubeApplication)

    static void main(String[] args)
    {
        ConfigurableApplicationContext ctx = SpringApplication.run(NCubeApplication, args)
        List<String> requiredProfiles = ['runtime-server', 'storage-server', 'combined-server']
        Environment env = ctx.environment
        String[] activeProfiles = ctx.environment.activeProfiles
        if (ArrayUtilities.isEmpty(activeProfiles))
        {
            activeProfiles = [] as String[]
        }

        List<String> profiles = Arrays.asList(activeProfiles)
        if (requiredProfiles.intersect(profiles).size() != 1)
        {
            ctx.close()
            throw new IllegalArgumentException("Missing active profile or redundant server types listed.  Expecting: one of ${requiredProfiles}.  Profiles supplied: ${profiles}")
        }

        String serverType
        if (profiles.contains('runtime-server'))
        {
            serverType = 'runtime'
        }
        else if (profiles.contains('combined-server'))
        {
            serverType = 'combined'
        }
        else // if (profiles.contains('storage-server'))
        {
            serverType = 'storage'
            if (Converter.convert(env.getProperty('ncube.allow.mutable.methods'), Boolean.class))
            {
                ctx.close()
                throw new IllegalArgumentException("ncube.allow.mutable.methods cannot be 'true' when running as a storage-server.")
            }
        }

        // Display server type and key versions
        LOG.info("NCUBE ${serverType}-server started")
        if (serverType == 'runtime')
        {
            LOG.info("  Targeting: ${env.getProperty('ncube.target.scheme')}://${env.getProperty('ncube.target.host')}:${env.getProperty('ncube.target.port')}/${env.getProperty('ncube.target.context')}")
        }
        LOG.info("  Groovy version: ${GroovySystem.version}")
        LOG.info("  Java version: ${System.getProperty("java.version")}")
        LOG.info("  Spring version: ${SpringVersion.version}")
        LOG.info("  Spring-boot version: ${SpringBootVersion.version}")
    }

    @Bean
    ServletRegistrationBean servletRegistrationBean0()
    {
        ServletRegistrationBean bean = new ServletRegistrationBean(new JsonCommandServlet(), "/cmd/*")
        bean.enabled = true
        bean.loadOnStartup = 1
        bean.order = 1
        return bean
    }

    @Bean()
    @Profile('storage-server')
    ServletRegistrationBean servletRegistrationBean1()
    {
        // Stop all non dispatch (command) traffic
        ServletRegistrationBean bean = new ServletRegistrationBean(new HttpServlet()
        {
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.contentType = 'text/html'
                resp.writer.println('<html><body>NCUBE storage-server</body></html>')
                resp.writer.flush()
            }

            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                doGet(req, resp)
            }

            protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                doGet(req, resp)
            }

            protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                doGet(req, resp)
            }

            protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                doGet(req, resp)
            }

            protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                doGet(req, resp)
            }

            protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                doGet(req, resp)
            }
        }, "/*")
        bean.enabled = true
        bean.loadOnStartup = 1
        bean.order = 2
        return bean
    }

    @Bean
    FilterRegistrationBean filterRegistrationBean2()
    {
        GenericFilterBean filter = new HiddenHttpMethodFilter()
        FilterRegistrationBean registration = new FilterRegistrationBean(filter)
        registration.enabled = false
        return registration
    }

    @Bean
    FilterRegistrationBean filterRegistrationBean3()
    {
        GenericFilterBean filter = new FormContentFilter()
        FilterRegistrationBean registration = new FilterRegistrationBean(filter)
        registration.enabled = false
        return registration
    }

    @Bean
    FilterRegistrationBean filterRegistrationBean4()
    {
        GenericFilterBean filter = new RequestContextFilter()
        FilterRegistrationBean registration = new FilterRegistrationBean(filter)
        registration.enabled = false
        return registration
    }
}
