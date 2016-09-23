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

##Running Example
Visit http://ncube.io to see a running version of the NCUBE Editor.  Play around with it and send your feedback.  This project is very actively being developed.

##Getting Started
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

Also, you will need to set up a database with one (1) table.  The DDL for this table in MySQL format looks like this (pulled from n-cube's src/test/resources/ddl/mysql-schema.ddl.  See [mysql-schema.ddl](/src/test/resources/ddl/mysql-schema.ddl)

    CREATE TABLE if not exists n_cube (
      n_cube_id bigint(20) NOT NULL,
      n_cube_nm varchar(250) NOT NULL,
      tenant_cd varchar(10) CHARACTER SET ascii NOT NULL DEFAULT 'NONE',
      cube_value_bin longblob,
      create_dt timestamp NOT NULL,
      create_hid varchar(20) DEFAULT NULL,
      version_no_cd varchar(16) NOT NULL,
      status_cd varchar(16) NOT NULL DEFAULT 'SNAPSHOT',
      app_cd varchar(20) NOT NULL,
      test_data_bin longblob,
      notes_bin longblob,
      revision_number bigint(20) DEFAULT '0',
      branch_id varchar(80) NOT NULL DEFAULT 'HEAD',
      sha1 varchar(40) DEFAULT NULL,
      head_sha1 varchar(40) DEFAULT NULL,
      changed int DEFAULT NULL,
      PRIMARY KEY (n_cube_id),
      UNIQUE KEY n_cube_unique (n_cube_nm, tenant_cd, app_cd, version_no_cd, branch_id, revision_number),
      KEY fourIdx (tenant_cd, app_cd, version_no_cd, branch_id),
      KEY versionIdx (version_no_cd),
      KEY revIdx (revision_number),
      KEY branchIdx (branch_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=latin1;

See [changelog.md](/changelog.md) for revision history.

By: John DeRegnaucourt, Tym Pollack, Kenny Partlow, Josh Snynder.
