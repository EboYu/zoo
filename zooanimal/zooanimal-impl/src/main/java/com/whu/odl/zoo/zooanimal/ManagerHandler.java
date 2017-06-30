/*
 * Copyright © 2017 Wuhan University, ITLab, Yinbo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.whu.odl.zoo.zooanimal;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.AddTourists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.ZooManagerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/*
 * Created by ebo on 17-5-8
 * Description: 
 */
public class ManagerHandler implements ZooManagerListener {
    private static final Logger LOG = LoggerFactory.getLogger(ZooManagerListener.class);
    private final DataBroker dataBroker;
    public static Long rate = 3000L;

    public ManagerHandler(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }
    @Override
    public void onAddTourists(AddTourists notification) {
        LOG.info("Get the notification of addTourist "+ notification.getAmountOfTourists());
        if(notification.getAmountOfTourists()>10){
            rate+=10L;
            LOG.info("Increase rate of food eating");
        }
    }
}
