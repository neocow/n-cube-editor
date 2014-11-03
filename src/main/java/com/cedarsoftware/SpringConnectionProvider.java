package com.cedarsoftware;

import com.cedarsoftware.ncube.JdbcConnectionProvider;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Created by kpartlow on 11/3/2014.
 */
public class SpringConnectionProvider implements JdbcConnectionProvider
{
    private DataSource dataSource;

   public SpringConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection()
    {
        return DataSourceUtils.getConnection(dataSource);
    }

    @Override
    public void releaseConnection(Connection c)
    {
        DataSourceUtils.releaseConnection(c, dataSource);
    }
}
