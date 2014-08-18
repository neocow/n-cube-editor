package com.cedarsoftware.controller;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 8/17/2014.
 */
public class TestPatternMatching
{
    Pattern pattern = Pattern.compile("[$][{](.*?)[}]");

    @Test
    public void testReplacement() {
        String first = "https://cdn.com/${application}/public/${version}/private/${status}/foo";

        Matcher m = pattern.matcher(first);

        StringBuffer sb = new StringBuffer(first.length());
        while (m.find()) {
            String text = m.group(1);

            if ("version".equals(m.group(1)))
            {
                m.appendReplacement(sb, "1.1.0");
            }
            else if ("application".equals(m.group(1)))
            {
                m.appendReplacement(sb, "app");
            }
            else if ("status".equals(m.group(1)))
            {
                m.appendReplacement(sb, "status");
            }
        }
        m.appendTail(sb);

        assertEquals("https://cdn.com/app/public/1.1.0/private/status/foo", sb.toString());
    }
}
