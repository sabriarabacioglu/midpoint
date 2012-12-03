/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.*;

/**
 * This repository factory should be used for testing purposes only. It behaves like {@link SqlRepositoryFactory},
 * but during configuration initialization it checks system properties and overrides loaded configuration
 * ({@link com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration}).
 *
 * @author lazyman
 */
public class TestSqlRepositoryFactory extends SqlRepositoryFactory implements BeanPostProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(TestSqlRepositoryFactory.class);

    private static final String TRUNCATE_PROCEDURE = "cleanupTestDatabase";

    @Override
    public synchronized void init(Configuration configuration) throws RepositoryServiceFactoryException {
        //override loaded configuration based on system properties...
        updateConfigurationBooleanProperty(configuration, PROPERTY_EMBEDDED);
        updateConfigurationBooleanProperty(configuration, PROPERTY_DROP_IF_EXISTS);
        updateConfigurationBooleanProperty(configuration, PROPERTY_AS_SERVER);
        updateConfigurationBooleanProperty(configuration, PROPERTY_TCP_SSL);

        updateConfigurationIntegerProperty(configuration, PROPERTY_PORT);

        updateConfigurationStringProperty(configuration, PROPERTY_BASE_DIR);
        updateConfigurationStringProperty(configuration, PROPERTY_FILE_NAME);
        updateConfigurationStringProperty(configuration, PROPERTY_DRIVER_CLASS_NAME);
        updateConfigurationStringProperty(configuration, PROPERTY_HIBERNATE_DIALECT);
        updateConfigurationStringProperty(configuration, PROPERTY_HIBERNATE_HBM2DDL);
        updateConfigurationStringProperty(configuration, PROPERTY_JDBC_PASSWORD);
        updateConfigurationStringProperty(configuration, PROPERTY_JDBC_URL);
        updateConfigurationStringProperty(configuration, PROPERTY_JDBC_USERNAME);

        super.init(configuration);
    }

    private void updateConfigurationIntegerProperty(Configuration configuration, String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || !value.matches("[1-9]{1}[0-9]*")) {
            return;
        }

        configuration.setProperty(propertyName, Integer.parseInt(value));
    }

    private void updateConfigurationBooleanProperty(Configuration configuration, String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null) {
            return;
        }

        configuration.setProperty(propertyName, new Boolean(value).booleanValue());
    }

    private void updateConfigurationStringProperty(Configuration configuration, String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null) {
            return;
        }

        configuration.setProperty(propertyName, value);
    }

    @Override
    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        return o;
    }

    @Override
    public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
        if (!(o instanceof SqlRepositoryServiceImpl)) {
            return o;
        }

        //we'll attempt to drop database objects if configuration contains dropIfExists=true and embedded=false
        SqlRepositoryConfiguration config = getSqlConfiguration();
        if (!config.isDropIfExists() || config.isEmbedded()) {
            LOGGER.info("We're not deleting objects from DB, drop if exists=false or embedded=true.");
            return o;
        }

        LOGGER.info("Deleting objects from database.");

        SqlRepositoryServiceImpl repositoryService = (SqlRepositoryServiceImpl) o;
        SessionFactory factory = repositoryService.getSessionFactory();

        Session session = factory.openSession();
        try {
            session.beginTransaction();

            Query query = session.createSQLQuery("select " + TRUNCATE_PROCEDURE + "();");
            query.uniqueResult();

            session.getTransaction().commit();
        } catch (Exception ex) {
            session.getTransaction().rollback();
            throw new BeanInitializationException("Couldn't delete objects from database, reason: " + ex.getMessage(), ex);
        }

        return o;
    }
}

//todo move somewhere else...
// mvn clean install -D-Dmidpoint.home=target -Dembedded=false -DdriverClassName=org.postgresql.Driver
// -DhibernateHbm2ddl=update -DhibernateDialect=org.hibernate.dialect.PostgreSQLDialect -DjdbcPassword=midpoint
// -DjdbcUsername=midpoint -DjdbcUrl=jdbc:postgresql://localhost:5432/testing -P default -o
