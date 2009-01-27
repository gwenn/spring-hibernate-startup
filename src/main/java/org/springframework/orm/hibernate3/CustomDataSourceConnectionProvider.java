package org.springframework.orm.hibernate3;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.util.JDBCExceptionReporter;
import org.springframework.orm.hibernate3.LocalDataSourceConnectionProvider;

/**
 * Adaptation of {@link LocalDataSourceConnectionProvider}.
 */
public class CustomDataSourceConnectionProvider implements ConnectionProvider {
   private DataSource dataSource;

   public void configure(Properties props) throws HibernateException {
      this.dataSource = CustomSessionFactoryBean.getConfigTimeDataSource();
      if (this.dataSource == null) {
         throw new HibernateException("No local DataSource found for configuration - " +
               "'dataSource' property must be set on LocalSessionFactoryBean");
      }
   }

   public Connection getConnection() throws SQLException {
      try {
         return dataSource.getConnection();
      } catch (SQLException ex) {
         JDBCExceptionReporter.logExceptions(ex);
         throw ex;
      }
   }

   public void closeConnection(Connection con) throws SQLException {
      try {
         con.close();
      } catch (SQLException ex) {
         JDBCExceptionReporter.logExceptions(ex);
         throw ex;
      }
   }

   public void close() {
   }

   public boolean supportsAggressiveRelease() {
      return false;
   }
}
