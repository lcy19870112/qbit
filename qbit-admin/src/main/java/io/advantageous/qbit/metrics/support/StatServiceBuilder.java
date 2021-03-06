/*
 * Copyright (c) 2015. Rick Hightower, Geoff Chandler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * QBit - The Microservice lib for Java : JSON, WebSocket, REST. Be The Web!
 */

package io.advantageous.qbit.metrics.support;


import io.advantageous.boon.core.reflection.BeanUtils;
import io.advantageous.qbit.QBit;
import io.advantageous.qbit.client.Client;
import io.advantageous.qbit.client.ClientBuilder;
import io.advantageous.qbit.events.EventManager;
import io.advantageous.qbit.metrics.*;
import io.advantageous.qbit.metrics.StatServiceImpl;
import io.advantageous.qbit.server.ServiceServer;
import io.advantageous.qbit.server.ServiceServerBuilder;
import io.advantageous.qbit.service.ServiceBuilder;
import io.advantageous.qbit.service.ServiceQueue;
import io.advantageous.qbit.service.discovery.ServiceDefinition;
import io.advantageous.qbit.service.discovery.ServiceDiscovery;
import io.advantageous.qbit.util.Timer;

import java.util.ArrayList;
import java.util.List;

import static io.advantageous.qbit.client.ClientBuilder.clientBuilder;

/**
 * Stat Service Builder
 * Created by rhightower on 1/28/15.
 */
public class StatServiceBuilder {


    private EventManager eventManager;

    public static StatServiceBuilder statServiceBuilder() {
        return new StatServiceBuilder();
    }

    private Timer timer = Timer.timer();
    private StatRecorder recorder = new NoOpRecorder();
    private StatReplicator replicator = new NoOpReplicator();
    private List<StatReplicator> replicators = new ArrayList<>();
    private ServiceDiscovery serviceDiscovery;
    private StatReplicatorProvider statReplicatorProvider;
    private ClientBuilder clientBuilder;
    private ServiceQueue serviceQueue;
    private ServiceBuilder serviceBuilder;
    private ServiceServerBuilder serviceServerBuilder;

    private String serviceName = "statsService";
    private String localServiceId = "";


    public ServiceServerBuilder getServiceServerBuilder() {

        if (serviceServerBuilder == null) {
            serviceServerBuilder = ServiceServerBuilder.serviceServerBuilder();
        }
        return serviceServerBuilder;
    }

    public StatServiceBuilder setServiceServerBuilder(ServiceServerBuilder serviceServerBuilder) {
        this.serviceServerBuilder = serviceServerBuilder;
        return this;
    }

    public ServiceQueue getServiceQueue() {

        if (serviceQueue==null) {
            buildServiceQueue();
        }
        return serviceQueue;
    }

    public StatServiceBuilder setServiceQueue(ServiceQueue serviceQueue) {
        this.serviceQueue = serviceQueue;
        return this;
    }

    public ServiceBuilder getServiceBuilder() {

        if (serviceBuilder == null) {
            serviceBuilder = ServiceBuilder.serviceBuilder();
        }
        return serviceBuilder;
    }

    public StatServiceBuilder setServiceBuilder(ServiceBuilder serviceBuilder) {
        this.serviceBuilder = serviceBuilder;
        return this;
    }



    public String getLocalServiceId() {
        return localServiceId;
    }

    public StatServiceBuilder setLocalServiceId(String localServiceId) {
        this.localServiceId = localServiceId;
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    private ClientBuilder getClientBuilder() {

        if (clientBuilder==null) {
            clientBuilder = clientBuilder();
        }

        return BeanUtils.copy(clientBuilder);
    }
    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public StatServiceBuilder setServiceDiscovery(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
        return this;
    }

    public Timer getTimer() {
        return timer;
    }

    public StatServiceBuilder setTimer(Timer timer) {
        this.timer = timer;
        return this;
    }

    public StatServiceBuilder addReplicator(StatReplicator replicator) {
        replicators.add(replicator);
        return this;
    }

    public StatRecorder getRecorder() {
        return recorder;
    }

    public StatServiceBuilder setRecorder(StatRecorder recorder) {
        this.recorder = recorder;
        return this;
    }

    public StatReplicator getReplicator() {
        return replicator;
    }

    public StatServiceBuilder setReplicator(StatReplicator replicator) {
        this.replicator = replicator;
        return this;
    }

    public ServiceQueue buildServiceQueue() {
        ServiceBuilder serviceBuilder = getServiceBuilder().setServiceObject(build());
        serviceQueue = serviceBuilder.build();

        if (serviceDiscovery!=null) {

            if (eventManager!=null) {

                eventManager.joinService(serviceQueue);
            }
        }
        return serviceQueue;
    }


    public ServiceServer buildServiceServer() {


        final ServiceServerBuilder serviceServerBuilder = getServiceServerBuilder();

        if (serviceDiscovery!=null) {
            final ServiceDefinition serviceDefinition = serviceDiscovery
                    .register(this.getServiceName(), serviceServerBuilder.getPort());
            localServiceId = serviceDefinition.getId();
        }
        final ServiceQueue serviceQueue = getServiceQueue();
        final ServiceServer serviceServer = serviceServerBuilder.build();
        serviceServer.addServiceObject(this.getServiceName(), serviceQueue.service());

        return serviceServer;
    }




    public StatServiceImpl build() {

        if (serviceDiscovery!=null) {
            return new StatServiceImpl(this.getRecorder(), buildReplicator(), getTimer());
        } else if (replicators.size() == 0) {
            return new StatServiceImpl(this.getRecorder(), this.getReplicator(), getTimer());
        } else {
            return new StatServiceImpl(this.getRecorder(), new ReplicatorHub(replicators), getTimer());
        }
    }

    private StatReplicator buildReplicator() {

        if (statReplicatorProvider == null) {
            statReplicatorProvider = buildStatsReplicatorProvider();
        }
        return new ClusteredStatReplicator(serviceName, serviceDiscovery,
                statReplicatorProvider, localServiceId);
    }

    public StatReplicatorProvider buildStatsReplicatorProvider() {
        return serviceDefinition -> {

            final ClientBuilder clientBuilder1 = getClientBuilder();
            final Client client = clientBuilder1.setPort(serviceDefinition.getPort())
                    .setHost(serviceDefinition.getHost()).build();

            final StatReplicator proxy = client.createProxy(StatReplicator.class, serviceName);

            client.start();

            return new StatReplicator() {

                private Client theClient = client;

                @Override
                protected void finalize() throws Throwable {
                    if (theClient!=null) {
                        theClient.stop();
                    }
                }

                @Override
                public void recordCount(String name, int count, long now) {
                    proxy.recordCount(name, count, now);
                }

                @Override
                public void clientProxyFlush() {
                    proxy.clientProxyFlush();
                }

                @Override
                public void stop() {
                    proxy.stop();
                    theClient.stop();
                }

                @Override
                public void flush() {
                    proxy.flush();
                    theClient.flush();
                }

                public String toString() {
                    return "StatServiceReplicator " + proxy;
                }
            };
        };
    }

    public StatServiceBuilder setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
        return this;
    }

    public EventManager getEventManager() {
        if (eventManager==null) {
            eventManager = QBit.factory().systemEventManager();

        }
        return eventManager;
    }
}
