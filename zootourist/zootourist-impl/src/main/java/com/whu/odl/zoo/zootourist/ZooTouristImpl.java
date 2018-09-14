/*
 * Copyright © 2017 Wuhan University, ITLab, Yinbo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.whu.odl.zoo.zootourist;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.whu.odl.zoo.zoomanager.ZooManagerImpl;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.GetNumOfAnimalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.ZooAnimalService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.ZooFoods;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.zoo.foods.Food;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.AddTouristInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.ZooTouristService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.ZooTourists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.zoo.tourists.Tourist;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.zoo.tourists.TouristBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.zoo.tourists.TouristKey;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/*
 * Created by ebo on 17-5-8
 * Description: tourist implementation
 */
public class ZooTouristImpl implements ZooTouristService,DataTreeChangeListener<ZooFoods>{
    private static final Logger LOG = LoggerFactory.getLogger(ZooTouristImpl.class);

    private final DataBroker dataBroker;
    private final ZooAnimalService animalService;
    private final ZooManagerService managerService;
    private static final Future<RpcResult<Void>> RPC_SUCCESS = RpcResultBuilder.<Void>success().buildFuture();
    private static boolean addTouristAllowable = false;
    private InstanceIdentifier<ZooFoods> identifier = InstanceIdentifier.create(ZooFoods.class);

    public ZooTouristImpl(DataBroker dataBroker, ZooManagerService managerService, ZooAnimalService animalService) {
        this.dataBroker = dataBroker;
        this.animalService = animalService;
        this.managerService = managerService;
    }


    public ListenerRegistration<ZooTouristImpl> register(DataBroker dataBroker){
        return dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<ZooFoods>(LogicalDatastoreType.CONFIGURATION,identifier),this);
    }

    @Override
    public Future<RpcResult<Void>> addTourist(AddTouristInput input) {

        RpcResultBuilder<Void> rpcResultBuilder = null;

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Tourist> id = InstanceIdentifier.builder(ZooTourists.class).child(Tourist.class, new TouristKey(input.getTouristId())).build();
        TouristBuilder touristBuilder = new TouristBuilder();
        touristBuilder.setName(input.getName());
        touristBuilder.setTouristId(input.getTouristId());
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, id, touristBuilder.build());
        try {
            writeTx.submit().checkedGet();
            LOG.info("Add tourist",touristBuilder.build());
            rpcResultBuilder = RpcResultBuilder.success();
        }catch (TransactionCommitFailedException e){
            LOG.error("Add tourist Failed with id : "+input.getTouristId(), e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        checkNumOfTourists();
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private void checkTouristID(int touristID) {
        if (touristID<=999){
            checkNumOfTourists();
        }else{
            LOG.error("Illigle touristID "+touristID);
        }
    }

    private void checkNumOfTourists(){
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();

        InstanceIdentifier<ZooTourists> ids = InstanceIdentifier.builder(ZooTourists.class).build();
        CheckedFuture<Optional<ZooTourists>, ReadFailedException> checkedFuture = readTx.read(LogicalDatastoreType.CONFIGURATION,ids);
        try {
            Optional<ZooTourists> optional =checkedFuture.checkedGet();
            if(optional.isPresent()){
                List<Tourist> tourists =optional.get().getTourist();
                int touristnum = tourists.size();
                Long animalNum = executeGetNumOfAnimal();
                if(touristnum<=10 && animalNum>=5){
                    executeBugTicket("a Team",(long)touristnum);
                }else{
                    LOG.error("too much tourists or too few animals");
                }
            }
        }catch (ReadFailedException e){
            LOG.error("Failed to fetch tourist");
        }
    }
    private void executeBugTicket(String teamName, Long num) {
        ReadOnlyTransaction rdTx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<ZooGateway> gwID = InstanceIdentifier.builder(ZooGateway.class).build();
        CheckedFuture<Optional<ZooGateway>, ReadFailedException> checkedFuture = rdTx.read(LogicalDatastoreType.CONFIGURATION,gwID);
        try {
            Optional<ZooGateway> optional = checkedFuture.checkedGet();
            if(optional.isPresent()){
                boolean state = optional.get().isState();
                if(state){
                    final BuyTicketInputBuilder builder = new BuyTicketInputBuilder();
                    builder.setName(teamName).setNum(num);
                    final Future<RpcResult<BuyTicketOutput>> rpcResultFuture = managerService.buyTicket(builder.build());
                    try {
                        if (rpcResultFuture.get().isSuccessful()) {
                            InstanceIdentifier<ZooTourists> ids = InstanceIdentifier.builder(ZooTourists.class).build();
                            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
                            writeTx.delete(LogicalDatastoreType.CONFIGURATION, ids);
                            try {
                                writeTx.submit().checkedGet();
                            }catch (TransactionCommitFailedException e)
                            {
                                LOG.error("Failed to delete tourists");
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.error("Failed to bug tickets");
                    }
                }else {
                    LOG.error("Gate is close. Failed to bug tickets!");
                }
            }
        }catch (ReadFailedException e){
            LOG.error("Failed to read the status of gate way");
        }
    }


    private Long executeGetNumOfAnimal() {

        final Future<RpcResult<GetNumOfAnimalOutput>> rpcResultFuture = animalService.getNumOfAnimal();
        try {
            GetNumOfAnimalOutput result = rpcResultFuture.get().getResult();
            if(result!=null){
                return result.getNum();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ZooFoods>> changes) {
        /*
        for(DataTreeModification<ZooFoods> change :changes){
            ZooFoods dataAfter =change.getRootNode().getDataAfter();
            ZooFoods dataBefore = change.getRootNode().getDataBefore();
            if(dataAfter == null)
                return;

            Long foodNum = 0L;
            for(Food food:dataAfter.getFood()){
                foodNum+=food.getNum();
            }
            ZooGatewayBuilder builder = new ZooGatewayBuilder();
            WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<ZooGateway> id = InstanceIdentifier.create(ZooGateway.class);

            if(foodNum<30){
                LOG.info("Close the gate becasue the number of food is smaller than 8");
                builder.setState(false);
            }else {
                LOG.info("Open the gate becasue the number of food is bigger than 8");
                builder.setState(true);
            }
            writeTransaction.merge(LogicalDatastoreType.CONFIGURATION,id,builder.build());
            try{
                writeTransaction.submit().checkedGet();
                LOG.info("Update the status of gate");
            }catch (TransactionCommitFailedException e){
                e.printStackTrace();
            }
        }
        */
    }
}
