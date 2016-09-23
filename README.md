n-cube-editor
=============
[![Build Status](https://travis-ci.org/jdereg/n-cube-editor.svg?branch=master)](https://travis-ci.org/jdereg/n-cube-editor)

n-cube-editor is a web-based GUI editor for editing and managing n-cubes.

```
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>n-cube</artifactId>
  <version>0.5.0</version>
</dependency>
```
#### Licensing
Copyright 2012-2016 Cedar Software, LLC
Licensed under the Apache License, Version 2.0

### Sponsors
[![Alt text](https://www.yourkit.com/images/yklogo.png "YourKit")](https://www.yourkit.com/.net/profiler/index.jsp)

YourKit supports open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of <a href="https://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a>
and <a href="https://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>,
innovative and intelligent tools for profiling Java and .NET applications.

<a href="https://www.jetbrains.com/idea/"><img alt="Intellij IDEA from JetBrains" src="https://s-media-cache-ak0.pinimg.com/236x/bd/f4/90/bdf49052dd79aa1e1fc2270a02ba783c.jpg" data-canonical-src="https://s-media-cache-ak0.pinimg.com/236x/bd/f4/90/bdf49052dd79aa1e1fc2270a02ba783c.jpg" width="100" height="100" /></a>
**Intellij IDEA**
___

## Running Example
Visit http://ncube.io to see a running version of the NCUBE Editor.  Play around with it and send your feedback.  This project is very actively being developed.

## Getting Started
Make sure to set the following environment variables (or -D system properties) in order to specify the connection to your database:
As Java system properties:

    -DNCE_JDBC_DRIVER="com.mysql.jdbc.Driver"
    -DNCE_JDBC_URL="jdbc:mysql://localhost:3306/ncube"
    -DNCE_JDBC_USER="ncube" 
    -DNCE_JDBC_PWD="ncube" 
    -DNCE_POOL_QUERY="/* ping */"

or environment variables:

    NCE_JDBC_DRIVER=com.mysql.jdbc.Driver
    NCE_JDBC_URL=jdbc:mysql://localhost:3306/ncube
    NCE_JDBC_USER=ncube
    NCE_JDBC_PWD=ncube
    NCE_POOL_QUERY="/* ping */"

Also, you will need to set up a database with one (1) table.  For the DDL see [mysql-schema.ddl](https://github.com/jdereg/n-cube/blob/master/src/test/resources/ddl/mysql-schema.sql)


See [changelog.md](/changelog.md) for revision history.

By: John DeRegnaucourt, Tym Pollack, Kenny Partlow, Josh Snynder.
