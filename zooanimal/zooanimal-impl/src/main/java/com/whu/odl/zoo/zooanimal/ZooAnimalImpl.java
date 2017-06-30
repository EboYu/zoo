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
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.zoo.animals.Animal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.zoo.animals.AnimalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.zoo.animals.AnimalKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.ZooFeedsystemListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.ZooFeedsystemService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.ZooManagerListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.ZooManagerService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.Future;

/*
 * Created by ebo on 17-5-8
 * Description: animal implementation
 */
public class ZooAnimalImpl implements ZooAnimalService{
    private static final Logger LOG = LoggerFactory.getLogger(ZooAnimalImpl.class);

    private final DataBroker dataBroker;

    public ZooAnimalImpl(DataBroker dataBroker) {
       this.dataBroker =dataBroker;
    }

    @Override
    public Future<RpcResult<Void>> makeAnimal(MakeAnimalInput input) {
        RpcResultBuilder<Void> rpcResultBuilder = null;
        rpcResultBuilder = RpcResultBuilder.failed();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<Animal> id = InstanceIdentifier.builder(ZooAnimals.class).child(Animal.class, new AnimalKey(input.getName())).build();
        CheckedFuture<Optional<Animal>, ReadFailedException> checkedFuture = rwTx.read(LogicalDatastoreType.CONFIGURATION,id);
        try {
            Optional<Animal> optional = checkedFuture.checkedGet();
            Animal animal = null;
            if (optional.isPresent()) {
                if(input.getNum()+optional.get().getNum()<=200){
                    animal = new AnimalBuilder().setId(input.getName()).setName(input.getName()).setNum(input.getNum()+optional.get().getNum()).build();
                }
            }else {
                if(input.getNum()<=200)
                animal = new AnimalBuilder().setId(input.getName()).setName(input.getName()).setNum(input.getNum()).build();
            }
            if(animal!=null){
                rwTx.put(LogicalDatastoreType.CONFIGURATION, id,animal);
                try {
                    rwTx.submit().checkedGet();
                    rpcResultBuilder = RpcResultBuilder.success();
                }catch (TransactionCommitFailedException e){
                    LOG.error("Failed to merge animal");
                }
            }
        }catch (ReadFailedException e){
            LOG.error("Failed to fetch animal");
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetNumOfAnimalOutput>> getNumOfAnimal() {
        RpcResultBuilder<GetNumOfAnimalOutput> rpcResultBuilder = null;
        rpcResultBuilder = RpcResultBuilder.failed();
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<ZooAnimals> id = InstanceIdentifier.builder(ZooAnimals.class).build();

        CheckedFuture<Optional<ZooAnimals>, ReadFailedException> checkedFuture = readTx.read(LogicalDatastoreType.CONFIGURATION,id);
        try {
            Optional<ZooAnimals> optional = checkedFuture.checkedGet();
            if (optional.isPresent()) {
                Long animalNum = 0L;
                for(Animal animal: optional.get().getAnimal()){
                    animalNum += animal.getNum();
                }
                GetNumOfAnimalOutputBuilder builder = new GetNumOfAnimalOutputBuilder().setNum(animalNum);
                rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(builder.build());
            }else {
                GetNumOfAnimalOutputBuilder builder = new GetNumOfAnimalOutputBuilder().setNum(0L);
                rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(builder.build());
            }
        }catch (ReadFailedException e){
            LOG.error("Failed to fetch animals");
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
    }


}
