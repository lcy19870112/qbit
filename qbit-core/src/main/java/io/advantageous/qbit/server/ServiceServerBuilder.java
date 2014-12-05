package io.advantageous.qbit.server;

import io.advantageous.qbit.QBit;
import io.advantageous.qbit.http.HttpServer;
import io.advantageous.qbit.json.JsonMapper;
import io.advantageous.qbit.service.ServiceBundle;
import io.advantageous.qbit.spi.ProtocolEncoder;

/**
 * Created by Richard on 11/14/14.
 */

public class ServiceServerBuilder {

    private String host = "localhost";
    private int port = 8080;
    private boolean manageQueues = true;
    private int pollTime = 100;
    private int requestBatchSize = 10;
    private int flushInterval = 100;
    private String uri = "/services";


    public String getUri() {
        return uri;
    }

    public ServiceServerBuilder setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public ServiceServerBuilder setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        return this;
    }

    private int timeoutSeconds = 30;

    public String getHost() {
        return host;
    }

    public ServiceServerBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public ServiceServerBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public boolean isManageQueues() {
        return manageQueues;
    }

    public ServiceServerBuilder setManageQueues(boolean manageQueues) {
        this.manageQueues = manageQueues;
        return this;
    }

    public int getPollTime() {
        return pollTime;
    }

    public ServiceServerBuilder setPollTime(int pollTime) {
        this.pollTime = pollTime;
        return this;
    }

    public int getRequestBatchSize() {
        return requestBatchSize;
    }

    public ServiceServerBuilder setRequestBatchSize(int requestBatchSize) {
        this.requestBatchSize = requestBatchSize;
        return this;
    }

    public int getFlushInterval() {
        return flushInterval;
    }

    public ServiceServerBuilder setFlushInterval(int flushInterval) {
        this.flushInterval = flushInterval;
        return this;
    }




    public ServiceServer build() {
        final HttpServer httpServer = QBit.factory().createHttpServer(host, port, manageQueues, pollTime, requestBatchSize, flushInterval);
        final JsonMapper jsonMapper = QBit.factory().createJsonMapper();
        final ProtocolEncoder encoder = QBit.factory().createEncoder();
        final ServiceBundle serviceBundle = QBit.factory().createServiceBundle(uri);


        final ServiceServer serviceServer = QBit.factory().createServiceServer(httpServer, encoder, serviceBundle, jsonMapper, timeoutSeconds);
        return serviceServer;

    }
}