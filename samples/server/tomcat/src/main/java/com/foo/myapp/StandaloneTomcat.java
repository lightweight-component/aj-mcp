package com.foo.myapp;


import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class StandaloneTomcat {
    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);

        // Set base directory (for temp files)
        tomcat.setBaseDir(".");

        // Create a context (no web.xml required)
        String contextPath = "";
        String docBase = new File(".").getAbsolutePath();
        Context context = tomcat.addContext(contextPath, docBase);

        // Register servlet
        Tomcat.addServlet(context, "helloServlet", new HelloServlet());
        context.addServletMappingDecoded("/hello", "helloServlet");

        // Register SSE servlet
        Tomcat.addServlet(context, "sseServlet", new SseServlet());
        context.addServletMappingDecoded("/sse", "sseServlet");

        tomcat.start();
        tomcat.getServer().await();
    }
}