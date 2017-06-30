/*
 * Copyright © 2017 Wuhan University, ITLab, Yinbo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.whu.odl.zoo.zoofeedsystem;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.feedsystem.rev170508.ZooFeedsystemService;
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
public class ZooFeedSystemProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ZooFeedSystemProvider.class);

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;
    private final NotificationPublishService notificationPublishService;
    private BindingAwareBroker.RpcRegistration<ZooFeedsystemService> rpcRegistration;
    private ListenerRegistration<ZooFeedSystemImpl> listenerRegistration = null;

    public ZooFeedSystemProvider(DataBroker dataBroker, RpcProviderRegistry rpcRegistry, NotificationPublishService notificationPublishService) {
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcRegistry;
        this.notificationPublishService = notificationPublishService;
    }
    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        ZooFeedSystemImpl zooFeedSystem = new ZooFeedSystemImpl(dataBroker,notificationPublishService);
        rpcRegistration = rpcRegistry.addRpcImplementation(ZooFeedsystemService.class,zooFeedSystem);
        listenerRegistration = zooFeedSystem.register(dataBroker);
        LOG.info("ZooFeedSystemProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        listenerRegistration.close();
        if(rpcRegistration!=null)
        rpcRegistration.close();
        LOG.info("ZooFeedSystemProvider Closed");
    }
}
