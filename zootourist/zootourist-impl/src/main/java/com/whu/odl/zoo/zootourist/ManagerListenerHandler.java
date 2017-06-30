/*
 * Copyright © 2017 Wuhan University, ITLab, Yinbo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.whu.odl.zoo.zootourist;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.AddTourists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.ZooManagerListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.ZooTourists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.zoo.tourists.Tourist;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.zoo.tourists.TouristBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.zoo.tourists.TouristKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/*
 * Created by ebo on 17-6-14
 * Description: 
 */
public class ManagerListenerHandler implements ZooManagerListener {
    private static final Logger LOG = LoggerFactory.getLogger(ManagerListenerHandler.class);
    private final DataBroker dataBroker;

    public ManagerListenerHandler(DataBroker dataBroker){
        this.dataBroker = dataBroker;
    }
    @Override
    public void onAddTourists(AddTourists notification) {
        LOG.info("Get notification of addtourist "+ notification.getAmountOfTourists());
        if(notification.getAmountOfTourists()>=8){
            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            TouristBuilder touristBuilder = new TouristBuilder();
            touristBuilder.setName("Jame");
            ThreadLocalRandom data = ThreadLocalRandom.current();
            touristBuilder.setTouristId(String.valueOf(data.nextLong(10000)));
            InstanceIdentifier<Tourist> id = InstanceIdentifier.builder(ZooTourists.class).child(Tourist.class,
                    new TouristKey(touristBuilder.getTouristId())).build();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, id, touristBuilder.build());
            try {
                writeTx.submit().checkedGet();
                LOG.info("Add a tourist "+ touristBuilder.getName()+":"+touristBuilder.getTouristId());
            }catch (TransactionCommitFailedException e){
                LOG.error("Failed to add tourist", e);
            }
        }

    }

}
