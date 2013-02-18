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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query2;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.hibernate.Criteria;
import org.hibernate.Session;

/**
 * @author lazyman
 */
public class QueryContext {

    private Session session;
    private Class<? extends ObjectType> type;

    public QueryContext(Class<? extends ObjectType> type, Session session) {
        this.type = type;
        this.session = session;
    }

    public Criteria getMainCriteria() {
        //todo implement
        return null;
    }
}