/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.jpa.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.Logs;
import org.sonar.jpa.dialect.Dialect;
import org.sonar.jpa.entity.SchemaMigration;
import org.sonar.persistence.Database;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractDatabaseConnector implements DatabaseConnector {
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractDatabaseConnector.class);

  protected Database database;
  private EntityManagerFactory factory = null;
  private int databaseVersion = SchemaMigration.VERSION_UNKNOWN;
  private boolean operational = false;
  private boolean started = false;

  protected AbstractDatabaseConnector(Database database) {
    this.database = database;
  }

  public String getDialectId() {
    return database.getDialect().getId();
  }

  /**
   * Indicates if the connector is operational : database connection OK and schema version OK
   */
  public boolean isOperational() {
    return operational;
  }

  /**
   * Indicates if the connector is started : database connection OK and schema version OK or KO
   */
  protected boolean isStarted() {
    return started;
  }

  public void start() {
    if (!started) {
      testConnection();
      started = true;
    }
    if (!operational) {
      boolean upToDate = upToDateSchemaVersion();
      if (upToDate) {
        Logs.INFO.info("Initializing Hibernate");
        factory = createEntityManagerFactory();
        operational = true;
      }
    }
  }

  public void stop() {
    if (factory != null && factory.isOpen()) {
      factory.close();
      factory = null;
    }
    operational = false;
    started = false;
    database = null;
  }

  public EntityManagerFactory getEntityManagerFactory() {
    return factory;
  }

  protected void setEntityManagerFactory(EntityManagerFactory factory) {
    this.factory = factory;
  }

  protected EntityManagerFactory createEntityManagerFactory() {
    // other settings are stored into /META-INF/persistence.xml
    Properties props = database.getHibernateProperties();
    logHibernateSettings(props);
    return Persistence.createEntityManagerFactory("sonar", props);
  }

  private void logHibernateSettings(Properties props) {
    if (LOG.isDebugEnabled()) {
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        LOG.debug(entry.getKey() + ": " + entry.getValue());
      }
    }
  }

  public EntityManager createEntityManager() {
    return factory.createEntityManager();
  }

  private String testConnection() throws DatabaseException {
    Connection connection = null;
    try {
      connection = getConnection();
      return connection.getMetaData().getURL();

    } catch (SQLException e) {
      throw new DatabaseException("Cannot open connection to database: " + e.getMessage(), e);

    } finally {
      close(connection);
    }
  }

  protected int loadVersion() {
    Connection connection = null;
    try {
      connection = getConnection();
      return SchemaMigration.getCurrentVersion(connection);

    } catch (SQLException e) {
      // schema not created
      return 0;
    } finally {
      close(connection);
    }
  }

  private void close(Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        // why does close() throw a checked-exception ???
      }
    }
  }

  protected boolean upToDateSchemaVersion() {
    if (databaseVersion == SchemaMigration.LAST_VERSION) {
      return true;
    }
    databaseVersion = loadVersion();
    return databaseVersion == SchemaMigration.LAST_VERSION;
  }

  public final int getDatabaseVersion() {
    return databaseVersion;
  }

  public final Dialect getDialect() {
    return database.getDialect();
  }
}
