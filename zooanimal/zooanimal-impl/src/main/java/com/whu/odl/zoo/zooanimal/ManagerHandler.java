/*
 * Copyright © 2017 Wuhan University, ITLab, Yinbo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.whu.odl.zoo.zooanimal;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.ZooEatingRate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.ZooEatingRateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.AddTourists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.ZooManagerListener;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
            ReadWriteTransaction readWriteTransaction = this.dataBroker.newReadWriteTransaction();
            InstanceIdentifier<ZooEatingRate> id = InstanceIdentifier.builder(ZooEatingRate.class).build();
            CheckedFuture<Optional<ZooEatingRate>, ReadFailedException> checkedFuture = readWriteTransaction.read(LogicalDatastoreType.CONFIGURATION,id);
            try {
                Optional<ZooEatingRate> optional =checkedFuture.checkedGet();
                ZooEatingRateBuilder builder = new ZooEatingRateBuilder();
                if(optional.isPresent()){
                    builder.setRate(optional.get().getRate()+notification.getAmountOfTourists());
                    readWriteTransaction.put(LogicalDatastoreType.CONFIGURATION,id,builder.build());
                    try {
                        readWriteTransaction.submit().checkedGet();
                        LOG.info("Increase rate of food eating");
                    }catch (TransactionCommitFailedException e){
                        LOG.error("Failed to get number of food",e);
                    }
                }

            }catch (ReadFailedException e){
                LOG.error("Failed to get Eating rate");
            }
        }
    }
}
