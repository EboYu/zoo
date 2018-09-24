/*
 * Copyright © 2017 Wuhan University, ITLab, Yinbo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.whu.odl.zoo.zootourist;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.ZooAnimalService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.ZooManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.tourist.rev170508.ZooTouristService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

/*
 * Created by ebo on 17-5-10
 * Description: 
 */
public class ZooTouristProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ZooTouristProvider.class);

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final NotificationService notificationService;
    private final ZooAnimalService animalService;
    private final ZooManagerService managerService;
    private BindingAwareBroker.RpcRegistration<ZooTouristService> rpcRegistration;
    private Registration managerListenerRegisteration = null;
    //private ListenerRegistration<ZooTouristImpl> listenerRegistration = null;

    public ZooTouristProvider(final DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry,
                              NotificationService notificationService, ZooManagerService managerService,
                              ZooAnimalService animalService) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.notificationService = notificationService;
        this.animalService = animalService;
        this.managerService = managerService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        rpcRegistration = rpcProviderRegistry.addRpcImplementation(ZooTouristService.class,new ZooTouristImpl(dataBroker, managerService,animalService));
        //ManagerListenerHandler managerListenerHandler = new ManagerListenerHandler(dataBroker);
        //managerListenerRegisteration = notificationService.registerNotificationListener(managerListenerHandler);
        ZooTouristImpl zooTourist = new ZooTouristImpl(dataBroker,managerService,animalService);
        //listenerRegistration = zooTourist.register(dataBroker);
        LOG.info("ZooTouristProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() throws Exception{
//        if(listenerRegistration!=null)
//            listenerRegistration.close();
        if(rpcRegistration!=null)
            rpcRegistration.close();
        if(managerListenerRegisteration!=null)
            managerListenerRegisteration.close();
        LOG.info("ZooTouristProvider Closed");

    }
}
