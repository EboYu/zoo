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
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.ZooEatingRate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.FoodStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.ZooFeedsystemListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.ZooFoods;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.zoo.foods.Food;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.zoo.foods.FoodBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Created by ebo on 17-5-8
 * Description:
 */
public class FeedsystemHandler implements ZooFeedsystemListener {
    private final Logger LOG = LoggerFactory.getLogger(FeedsystemHandler.class);

    private final DataBroker dataBroker;

    public FeedsystemHandler(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void onFoodStats(FoodStats notification) {
        LOG.info("Get notification of food changes " + notification.getAmountOfFood());
        if (notification.getAmountOfFood() > 4) {
            ReadOnlyTransaction readOnlyTransaction = this.dataBroker.newReadOnlyTransaction();
            InstanceIdentifier<ZooEatingRate> ind = InstanceIdentifier.builder(ZooEatingRate.class).build();
            CheckedFuture<Optional<ZooEatingRate>, ReadFailedException> future = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, ind);
            try {
                Optional<ZooEatingRate> optionalRate = future.checkedGet();
                //Long sleepTime = 300L;

                if (optionalRate.isPresent()) {
                    Long eatingRate = optionalRate.get().getRate();
                    if (eatingRate > 400) {
                        LOG.error("The eating rate is too high.");
                    } else {
                        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
                        InstanceIdentifier<ZooFoods> id = InstanceIdentifier.builder(ZooFoods.class).build();
                        CheckedFuture<Optional<ZooFoods>, ReadFailedException> checkedFuture = readTx.read(LogicalDatastoreType.CONFIGURATION, id);
                        try {
                            Optional<ZooFoods> optional = checkedFuture.checkedGet();
                            if (optional.isPresent()) {
                                Long eatnum = 4L;
                                for (Food food : optional.get().getFood()) {
                                    WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
                                    InstanceIdentifier<Food> identifier = InstanceIdentifier.builder(ZooFoods.class).child(Food.class, food.getKey()).build();
                                    if (food.getNum() > eatnum) {
                                        Food food1 = new FoodBuilder().setId(food.getId()).setName(food.getName()).setNum(food.getNum() - 4).build();
                                        writeTx.put(LogicalDatastoreType.CONFIGURATION, identifier, food1);
                                        try {
                                            writeTx.submit().checkedGet();
                                            LOG.info("Eat 4 food");
                                            break;
                                        } catch (TransactionCommitFailedException e) {
                                            LOG.error("Failed to eat food");
                                        }
                                    } else {
                                        writeTx.delete(LogicalDatastoreType.CONFIGURATION, identifier);
                                        try {
                                            writeTx.submit().checkedGet();
                                            eatnum = eatnum - food.getNum();
                                        } catch (TransactionCommitFailedException e) {
                                            LOG.error("Failed to eat food");
                                        }
                                    }
                                }
                            }
                        } catch (ReadFailedException e) {
                            LOG.error("Failed to fetch zoo foods");
                        }
                    }
                }
            } catch (ReadFailedException e) {
                LOG.error("Failed to get Eating rate");
            }
        }
    }
}
