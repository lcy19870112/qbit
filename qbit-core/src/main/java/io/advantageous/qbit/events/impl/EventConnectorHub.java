package io.advantageous.qbit.events.impl;

import io.advantageous.qbit.GlobalConstants;
import io.advantageous.qbit.client.ClientProxy;
import io.advantageous.qbit.client.RemoteTCPClientProxy;
import io.advantageous.qbit.events.spi.EventConnector;
import io.advantageous.qbit.events.spi.EventTransferObject;
import io.advantageous.qbit.service.ServiceProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static io.advantageous.boon.Boon.puts;

/**
 */
public class EventConnectorHub implements EventConnector, Iterable<EventConnector> {

    private final List<EventConnector> eventConnectors;
    private final Logger logger = LoggerFactory.getLogger(EventConnectorHub.class);
    private final boolean debug = GlobalConstants.DEBUG || logger.isDebugEnabled();


    public EventConnectorHub(final List<EventConnector> eventConnectors) {

        this.eventConnectors = new CopyOnWriteArrayList<>(eventConnectors);
    }

    public EventConnectorHub() {

        this.eventConnectors = new CopyOnWriteArrayList<>();
    }

    public void add(EventConnector eventConnector) {
        this.eventConnectors.add(eventConnector);
    }

    public void addAll(EventConnector... eventConnectors) {
        for (EventConnector eventConnector : eventConnectors) {
            this.eventConnectors.add(eventConnector);
        }
    }


    public void remove(EventConnector eventConnector) {
        if (eventConnector!=null) {
            try {
                this.eventConnectors.remove(eventConnector);
            }catch (Exception ex) {
                //already removed
            }
        }
    }


    @Override
    public void forwardEvent(final EventTransferObject<Object> event) {

        if (debug) puts("forwardEvent " + event.channel() + " size " + eventConnectors.size() );

        for (int index=0; index < eventConnectors.size(); index++) {

            EventConnector eventConnector=null;
            try {
                eventConnector = eventConnectors.get(index);
                eventConnector.forwardEvent(event);
            } catch (Exception ex) {
                logger.info("problem sending event to event connector", ex);

                if (eventConnector instanceof RemoteTCPClientProxy) {
                    if (!((RemoteTCPClientProxy) eventConnector).connected()) {
                        eventConnectors.remove(eventConnector);
                    }
                }
            }
        }


        if (debug) puts("forwardEvent done " + event.channel() );
    }

    @Override
    public void flush() {



        for (int index=0; index < eventConnectors.size(); index++) {

            EventConnector eventConnector=null;
            try {
                eventConnector = eventConnectors.get(index);
                if (eventConnector instanceof ClientProxy) {
                    ServiceProxyUtils.flushServiceProxy(eventConnector);
                } else {
                    eventConnector.flush();
                }
            } catch (Exception ex) {
                logger.debug("problem sending event to event connector", ex);
                if (eventConnector instanceof RemoteTCPClientProxy) {
                    if (!((RemoteTCPClientProxy) eventConnector).connected()) {

                        eventConnectors.remove(eventConnector);
                    }
                }
            }
        }
    }

    @Override
    public Iterator<EventConnector> iterator() {
        return this.eventConnectors.listIterator();
    }

    public ListIterator<EventConnector> listIterator() {
        return this.eventConnectors.listIterator();
    }

    public int size() {
        return eventConnectors.size();
    }
}
