package org.springframework.orm.hibernate3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

public class CustomSessionFactoryBean extends LocalSessionFactoryBean {
   private static final Log LOG = LogFactory.getLog(CustomSessionFactoryBean.class);
   private static final String TARGET_DIRECTORY = "./target/classes";
   private static final String SERIALIZED_CONFIGURATION_FILE_NAME = "Configuration.bin";
   private static final ThreadLocal<DataSource> configTimeDataSourceHolder = new ThreadLocal<DataSource>();

   private transient boolean serialize;
   private boolean cacheableConfiguration = true;
   private String cacheableConfigurationDirectory = TARGET_DIRECTORY;
   private String cacheableConfigurationFileName = SERIALIZED_CONFIGURATION_FILE_NAME;

   public static DataSource getConfigTimeDataSource() {
      return configTimeDataSourceHolder.get();
   }

   @Override
   protected SessionFactory buildSessionFactory() throws Exception {
      if (!cacheableConfiguration) {
         return super.buildSessionFactory();
      }
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(cacheableConfigurationFileName);
      if (null != inputStream) {
         LOG.info("Loading serialized Hibernate configuration...");
         DataSource dataSource = getDataSource();
         if (dataSource != null) {
            configTimeDataSourceHolder.set(dataSource);
         }
         try {
            Configuration config = (Configuration) SerializationUtils.deserialize(inputStream);
            // hibernate properties may have been modified dynamically...
            config.addProperties(getHibernateProperties());
            // LocalSessionFactoryBean.configTimeDataSourceHolder is not accessible...
            config.setProperty(Environment.CONNECTION_PROVIDER, CustomDataSourceConnectionProvider.class.getName());
            return newSessionFactory(config);
         } finally {
            IOUtils.closeQuietly(inputStream);
            if (dataSource != null) {
               configTimeDataSourceHolder.set(null);
            }
         }
      } else {
         serialize = true;
         try {
            return super.buildSessionFactory();
         } finally {
            serialize = false;
         }
      }
   }

   @Override
   protected void postProcessConfiguration(Configuration config) throws HibernateException {
      if (serialize) {
         LOG.info("Serializing Hibernate configuration...");
         final File serializedConfigurationFile = new File(cacheableConfigurationDirectory, cacheableConfigurationFileName);
         OutputStream outputStream = null;
         try {
            outputStream = FileUtils.openOutputStream(serializedConfigurationFile);
            SerializationUtils.serialize(config, outputStream);
         } catch (IOException e) {
            throw new HibernateException("Error while serializing Hibernate configuration", e);
         } finally {
            IOUtils.closeQuietly(outputStream);
         }
      }
   }

   public void setCacheableConfiguration(boolean cacheableConfiguration) {
      this.cacheableConfiguration = cacheableConfiguration;
   }

   public void setCacheableConfigurationDirectory(String cacheableConfigurationDirectory) {
      this.cacheableConfigurationDirectory = cacheableConfigurationDirectory;
   }

   public void setCacheableConfigurationFileName(String cacheableConfigurationFileName) {
      this.cacheableConfigurationFileName = cacheableConfigurationFileName;
   }
}
