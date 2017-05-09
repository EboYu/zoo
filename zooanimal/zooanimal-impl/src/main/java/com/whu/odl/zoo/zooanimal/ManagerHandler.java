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

import javax.inject.Inject;
import javax.inject.Singleton;

/*
 * Created by ebo on 17-5-8
 * Description: 
 */
@Singleton
public class ManagerHandler implements ZooManagerListener {

    private final DataBroker dataBroker;

    @Inject
    public ManagerHandler(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }
    @Override
    public void onAddTourists(AddTourists notification) {
        if(notification.getAmountOfTourists()>10){
            
        }
    }
}
