/*
 * Copyright © 2017 Wuhan University, ITLab, Yinbo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.whu.odl.zoo.zoofeedsystem;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMNotificationListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.zoo.foods.Food;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.zoo.foods.FoodBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.zoo.foods.FoodKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.ZooTickets;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/*
 * Created by ebo on 17-5-8
 * Description: feed system implementation
 */
public class ZooFeedSystemImpl implements ZooFeedsystemService, DataTreeChangeListener<ZooTickets> {
    private static final Logger LOG = LoggerFactory.getLogger(ZooFeedSystemImpl.class);

    private final DataBroker dataBroker;
    private final NotificationPublishService notificationPublishService;
    private InstanceIdentifier<ZooTickets> identifier = InstanceIdentifier.create(ZooTickets.class);


    public ZooFeedSystemImpl(DataBroker dataBroker, NotificationPublishService notificationPublishService){
        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
    }

    public ListenerRegistration<ZooFeedSystemImpl> register(DataBroker dataBroker){
        return dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<ZooTickets>(LogicalDatastoreType.CONFIGURATION,identifier),this);
    }

    @Override
    public Future<RpcResult<Void>> addFood(AddFoodInput input) {
        RpcResultBuilder<Void> rpcResultBuilder = null;
        rpcResultBuilder = RpcResultBuilder.failed();
        ReadOnlyTransaction rdTx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Food> id = InstanceIdentifier.builder(ZooFoods.class).child(Food.class, new FoodKey(input.getName())).build();
        CheckedFuture<Optional<Food>, ReadFailedException> checkedFuture = rdTx.read(LogicalDatastoreType.CONFIGURATION,id);
        try {
            Optional<Food> optional = checkedFuture.checkedGet();
            Food food = null;
            if (optional.isPresent()) {
                food = new FoodBuilder().setId(input.getName()).setName(input.getName()).setNum(input.getNum()+optional.get().getNum()).build();
            }else {
                food = new FoodBuilder().setId(input.getName()).setName(input.getName()).setNum(input.getNum()).build();
            }
            WriteTransaction wrTx = dataBroker.newWriteOnlyTransaction();
            wrTx.put(LogicalDatastoreType.CONFIGURATION, id,food);
            try {
                wrTx.submit().checkedGet();
                rpcResultBuilder = RpcResultBuilder.success();
                ReadOnlyTransaction rdTx1 = dataBroker.newReadOnlyTransaction();
                InstanceIdentifier<ZooFoods> foodId = InstanceIdentifier.builder(ZooFoods.class).build();
                CheckedFuture<Optional<ZooFoods>, ReadFailedException> checkedFuture1 = rdTx1.read(LogicalDatastoreType.CONFIGURATION, foodId);
                try {
                    Optional<ZooFoods> op = checkedFuture1.checkedGet();
                    if (op.isPresent()){
                        List<Food> foods = op.get().getFood();
                        Long foodNum = 0L;
                        for(Food f:foods)
                            foodNum += f.getNum();
                        try {
                            checkAndPublishAddFoodNotification(foodNum);
                        }catch (InterruptedException e){
                            LOG.error("Failed to notify about food adding",e);
                        }
                    }
                }catch (ReadFailedException e){
                    LOG.error("Failed to get number of food",e);
                }
            }catch (TransactionCommitFailedException e){
                LOG.error("Failed to merge food",e);
            }
        }catch (ReadFailedException e){
            LOG.error("Failed to fetch food",e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private void checkAndPublishAddFoodNotification(Long foodnum) throws InterruptedException{
        FoodStats foodStats = new FoodStatsBuilder().setAmountOfFood(foodnum).build();
        notificationPublishService.putNotification(foodStats);
        LOG.info("Publish notification of add food "+ foodnum);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ZooTickets>> changes) {
        for(DataTreeModification<ZooTickets> change :changes){
            ZooTickets dataAfter =change.getRootNode().getDataAfter();
            ZooTickets dataBefore = change.getRootNode().getDataBefore();
            if(dataAfter == null || dataBefore == null)
                return;
            if(dataAfter.getNum()<dataBefore.getNum() && dataBefore.getNum()-dataAfter.getNum()>5){
                AddFoodInput addFoodInput = new AddFoodInputBuilder().setName("fish").setNum(dataBefore.getNum()-dataAfter.getNum()).build();
                LOG.info("Add food for data change on tickets");
                addFood(addFoodInput);
            }
        }
    }
}
