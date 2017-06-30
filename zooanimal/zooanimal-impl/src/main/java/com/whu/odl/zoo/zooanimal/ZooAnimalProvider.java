/*
 * Copyright © 2017 Wuhan University, ITLab, Yinbo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.whu.odl.zoo.zooanimal;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.zoo.animal.rev170508.ZooAnimalService;
import org.opendaylight.yangtools.concepts.Registration;
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
public class ZooAnimalProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ZooAnimalProvider.class);

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;
    private final NotificationService notificationService;
    private BindingAwareBroker.RpcRegistration<ZooAnimalService> rpcRegistration;
    private Registration managerListenerRegisteration = null,feedListenerRegisteration = null;

    private static final Future<RpcResult<Void>> RPC_SUCCESS = RpcResultBuilder.<Void>success().buildFuture();

    public ZooAnimalProvider(final DataBroker dataBroker, final RpcProviderRegistry rpcRegistry, NotificationService notificationService) {
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcRegistry;
        this.notificationService = notificationService;

    }
    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        rpcRegistration = rpcRegistry.addRpcImplementation(ZooAnimalService.class,new ZooAnimalImpl(dataBroker));
        LOG.info("ZooAnimalProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() throws Exception{
        if(rpcRegistration !=null)
            rpcRegistration.close();
        if(managerListenerRegisteration!=null)
            managerListenerRegisteration.close();
        if(feedListenerRegisteration!=null)
            feedListenerRegisteration.close();
        LOG.info("ZooAnimalProvider Closed");
    }


}
