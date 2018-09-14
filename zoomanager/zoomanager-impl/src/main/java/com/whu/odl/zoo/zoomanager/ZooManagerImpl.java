/*
 * Copyright © 2017 Wuhan University, ITLab, Yinbo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.whu.odl.zoo.zoomanager;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.GetNumOfAnimalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.MakeAnimalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.ZooAnimalService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.ZooAnimals;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.AddFoodInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.ZooFeedsystemService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.ZooFoods;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.zoo.foods.Food;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.*;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/*
 * Created by ebo on 17-5-8
 * Description: manager implementation
 */
public class ZooManagerImpl implements ZooManagerService, DataTreeChangeListener<ZooFoods> {
    private static final Logger LOG = LoggerFactory.getLogger(ZooManagerImpl.class);

    private final DataBroker dataBroker;
    private final NotificationPublishService notificationPublishService;
    private final ZooFeedsystemService zooFeedsystemService;
    private final ZooAnimalService zooAnimalService;
    private static final Future<RpcResult<Void>> RPC_SUCCESS = RpcResultBuilder.<Void>success().buildFuture();
    private InstanceIdentifier<ZooFoods> identifier = InstanceIdentifier.create(ZooFoods.class);

    public ZooManagerImpl(DataBroker dataBroker,
                          NotificationPublishService notificationPublishService,
                          ZooFeedsystemService zooFeedsystemService, ZooAnimalService zooAnimalService) {
        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
        this.zooFeedsystemService = zooFeedsystemService;
        this.zooAnimalService = zooAnimalService;
    }

    public ListenerRegistration<ZooManagerImpl> register(DataBroker dataBroker){
        return dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<ZooFoods>(LogicalDatastoreType.CONFIGURATION,identifier),this);
    }


    @Override
    public Future<RpcResult<BuyTicketOutput>> buyTicket(BuyTicketInput input) {
        RpcResultBuilder<BuyTicketOutput> rpcResultBuilder = null;
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<ZooTickets> ticketId = InstanceIdentifier.builder(ZooTickets.class).build();
        BuyTicketOutputBuilder output = null;
        CheckedFuture<Optional<ZooTickets>, ReadFailedException> checkedFuture = rwTx.read(LogicalDatastoreType.CONFIGURATION,ticketId);
        try {
            Optional<ZooTickets> optional = checkedFuture.checkedGet();
            if (optional.isPresent() && optional.get().getNum()!=0){
                ZooTickets zooTickets = optional.get();
                if(zooTickets.getNum()>= input.getNum()+5){
                    ZooTickets zooTickets1 = new ZooTicketsBuilder().setNum(zooTickets.getNum()-input.getNum()).build();
                    rwTx.put(LogicalDatastoreType.CONFIGURATION, ticketId, zooTickets1);
                    try {
                        rwTx.submit().checkedGet();
                        try {
                            checkAndPublishAddTouristsNotification(input.getNum());
                        }catch (InterruptedException e){
                            LOG.error("Failed to publish notification when add new tourists",e);
                        }
                        output = new BuyTicketOutputBuilder().setTicketNum(zooTickets.getNum()-input.getNum());
                        rpcResultBuilder = RpcResultBuilder.success();
                        rpcResultBuilder.withResult(output.build());
                    }catch (TransactionCommitFailedException e){
                        LOG.error("Failed to buy ticket",e);
                        rpcResultBuilder = RpcResultBuilder.failed();
                    }
                } else {
                    LOG.error("There are not enough tickets");
                    rpcResultBuilder = RpcResultBuilder.failed();
                    output = new BuyTicketOutputBuilder().setTicketNum(zooTickets.getNum());
                    rpcResultBuilder.withResult(output.build());
                }
            }else{
                LOG.error("There is no ticket");
                rpcResultBuilder = RpcResultBuilder.failed();
                output = new BuyTicketOutputBuilder().setTicketNum(0L);
                rpcResultBuilder.withResult(output.build());
            }
        }catch (ReadFailedException e){
            LOG.error("Failed to fetch tickets");
            rpcResultBuilder = RpcResultBuilder.failed();
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<Void>> putTickets(PutTicketsInput input) {
        RpcResultBuilder<Void> rpcResultBuilder = null;
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<ZooTickets> id = InstanceIdentifier.builder(ZooTickets.class).build();
        CheckedFuture<Optional<ZooTickets>, ReadFailedException> checkedFuture = rwTx.read(LogicalDatastoreType.CONFIGURATION,id);
        try {
            Optional<ZooTickets> optional = checkedFuture.checkedGet();
            if (optional.isPresent()){
                ZooTickets zooTickets1 = new ZooTicketsBuilder().setNum(optional.get().getNum()+input.getNum()).build();
                rwTx.put(LogicalDatastoreType.CONFIGURATION,id,zooTickets1);
                try {
                    rwTx.submit().checkedGet();
                    rpcResultBuilder = RpcResultBuilder.success();
                }catch (TransactionCommitFailedException e){
                    LOG.error("Failed to put tickets in adding tickets",e);
                    rpcResultBuilder = RpcResultBuilder.failed();
                }
            }else {
                InstanceIdentifier<ZooTickets> identifier = InstanceIdentifier.create(ZooTickets.class);
                ZooTickets zooTickets1 = new ZooTicketsBuilder().setNum(input.getNum()).build();
                rwTx.put(LogicalDatastoreType.CONFIGURATION,identifier,zooTickets1);
                try {
                    rwTx.submit().checkedGet();
                    rpcResultBuilder = RpcResultBuilder.success();
                }catch (TransactionCommitFailedException e){
                    LOG.error("Failed to put tickets in creating tickets",e);
                    rpcResultBuilder = RpcResultBuilder.failed();
                }
            }
        } catch (ReadFailedException e){
            LOG.error("Failed to fetch tickets");
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<Void>> manageZoo(ManageZooInput input) {
        RpcResultBuilder<Void> rpcResultBuilder = null;
        rpcResultBuilder = RpcResultBuilder.success();
        long animalNum=0, foodNums = 0;
        boolean animalFlag = false, foodFlag= false;
        if(executeMakeAnimal("Panda",input.getAnimalNum(),"make "+input.getAnimalNum()+" pandas")){
            LOG.info("make Animal");
            animalFlag=true;
        }

        final Future<RpcResult<GetNumOfAnimalOutput>> rpcResultFuture = zooAnimalService.getNumOfAnimal();
        try {
            if (rpcResultFuture.get().isSuccessful()) {
                LOG.info("success to get number of animals");
                GetNumOfAnimalOutput output = rpcResultFuture.get().getResult();
                if(output!=null){
                    animalNum = output.getNum();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to fetch animal num");
        }

        ReadOnlyTransaction rdTx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<ZooFoods> foodId = InstanceIdentifier.builder(ZooFoods.class).build();
        CheckedFuture<Optional<ZooFoods>, ReadFailedException> checkedFuture = rdTx.read(LogicalDatastoreType.CONFIGURATION, foodId);
        try {
            Optional<ZooFoods> op = checkedFuture.checkedGet();
            if (op.isPresent()){
                List<Food> foods = op.get().getFood();
                Long foodNum = 0L;
                for(Food f:foods)
                    foodNum += f.getNum();
                if(input.getFoodNum()+foodNum<=400)
                {
                    foodNums=foodNum+input.getFoodNum();
                    if(executeAddFood("apple",input.getFoodNum())){
                        LOG.info("add food");
                        foodFlag=true;
                    }
                }
            }else{
                if(input.getFoodNum()<=400)
                {
                    foodNums=input.getFoodNum();
                    if(executeAddFood("apple",input.getFoodNum())){
                        LOG.info("add food");
                        foodFlag=true;
                    }
                }
            }
            rpcResultBuilder = RpcResultBuilder.success();
        }catch (ReadFailedException e){
            LOG.error("Failed to get number of food",e);
        }

        if(animalFlag && foodFlag){
            WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<ZooGateway> gwID = InstanceIdentifier.create(ZooGateway.class);
            ZooGatewayBuilder builder = new ZooGatewayBuilder();

            if(animalNum>25 && foodNums>50){
                builder.setState(true);
                LOG.info("Open the gateway");
            }else {
                builder.setState(false);
                LOG.info("Close the gateway");
            }

            writeTransaction.merge(LogicalDatastoreType.CONFIGURATION,gwID,builder.build());
            try {
                writeTransaction.submit().checkedGet();
                rpcResultBuilder = RpcResultBuilder.success();
            }catch (TransactionCommitFailedException e){
                LOG.error("Failed to operate on the gateway",e);
                rpcResultBuilder = RpcResultBuilder.failed();
            }
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private void checkAndPublishAddTouristsNotification(Long touristNum) throws InterruptedException{
        AddTourists addTourists = new AddTouristsBuilder().setAmountOfTourists(touristNum).build();
        notificationPublishService.putNotification(addTourists);
        LOG.info("Publish notification of add tourist "+touristNum);
    }

    private boolean executeMakeAnimal(String name, Long num, String desc){
        final MakeAnimalInputBuilder builder = new MakeAnimalInputBuilder();
        builder.setName(name).setNum(num).setDescription(desc);
        final Future<RpcResult<Void>> rpcResultFuture = zooAnimalService.makeAnimal(builder.build());
        try {
            if (rpcResultFuture.get().isSuccessful()) {
                LOG.info("success to add animal");
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to make animal");
        }
        return false;
    }

    private boolean executeAddFood(String foodName, Long num){
        final AddFoodInputBuilder builder = new AddFoodInputBuilder();
        builder.setName(foodName).setNum(num);
        final Future<RpcResult<Void>> rpcResultFuture = zooFeedsystemService.addFood(builder.build());
        try {
            if (rpcResultFuture.get().isSuccessful()) {
                LOG.info("success to add food");
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to add food");
        }
        return false;
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ZooFoods>> changes) {
        /*
        for(DataTreeModification<ZooFoods> change :changes){
            ZooFoods dataAfter =change.getRootNode().getDataAfter();
            ZooFoods dataBefore = change.getRootNode().getDataBefore();
            if(dataAfter == null || dataBefore==null)
                return;
            if(dataAfter.getFood().size() < dataBefore.getFood().size() || dataAfter.getFood().size()==1){
                Long foodNum = 0L;
                for(Food food:dataAfter.getFood()){
                    foodNum+=food.getNum();
                }
                if(foodNum<2){
                    executeAddFood("fish",2L);
                }
            }
        }
        */
    }
}
