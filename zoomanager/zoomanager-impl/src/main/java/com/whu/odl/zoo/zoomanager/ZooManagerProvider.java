/*
 * Copyright © 2017 Wuhan University, ITLab, Yinbo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.whu.odl.zoo.zoomanager;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.ZooAnimalService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.ZooFeedsystemService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.ZooFoods;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.manager.rev170508.ZooManagerService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.concurrent.Future;

/*
 * Created by ebo on 17-5-10
 * Description: 
 */
public class ZooManagerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ZooManagerProvider.class);

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;
    private final NotificationPublishService notificationPublishService;
    private final ZooFeedsystemService zooFeedsystemService;
    private final ZooAnimalService zooAnimalService;
    private BindingAwareBroker.RpcRegistration<ZooManagerService> rpcRegistration;
    private ListenerRegistration<ZooManagerImpl> listenerRegistration = null;

    public ZooManagerProvider(DataBroker dataBroker, RpcProviderRegistry rpcRegistry,
                          NotificationPublishService notificationPublishService,
                          ZooFeedsystemService zooFeedsystemService, ZooAnimalService zooAnimalService) {
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcRegistry;
        this.notificationPublishService = notificationPublishService;
        this.zooFeedsystemService = zooFeedsystemService;
        this.zooAnimalService = zooAnimalService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    @PostConstruct
    public void init() {
        ZooManagerImpl zooManager = new ZooManagerImpl(dataBroker,notificationPublishService,zooFeedsystemService,zooAnimalService);
        rpcRegistration = rpcRegistry.addRpcImplementation(ZooManagerService.class,zooManager);
        listenerRegistration = zooManager.register(dataBroker);
        LOG.info("ZooManagerProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    @PreDestroy
    public void close() {
        listenerRegistration.close();
        rpcRegistration.close();
        LOG.info("ZooManagerProvider Closed");
    }
}
