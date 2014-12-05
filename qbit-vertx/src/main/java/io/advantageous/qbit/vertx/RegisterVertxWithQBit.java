package io.advantageous.qbit.vertx;

import io.advantageous.qbit.http.HttpClient;
import io.advantageous.qbit.http.HttpServer;
import io.advantageous.qbit.spi.FactorySPI;
import io.advantageous.qbit.spi.HttpClientFactory;
import io.advantageous.qbit.spi.HttpServerFactory;
import io.advantageous.qbit.vertx.http.HttpClientVertx;
import io.advantageous.qbit.vertx.http.HttpServerVertx;
import io.advantageous.qbit.vertx.service.VertxClientFactory;

/**
 * Created by rhightower on 11/6/14.
 */
public class RegisterVertxWithQBit {

    public static void registerVertxWithQBit() {
        FactorySPI.setHttpServerFactory((host, port, manageQueues, pollTime, requestBatchSize, flushInterval) -> new HttpServerVertx (port, host, manageQueues, pollTime, requestBatchSize, flushInterval));

        FactorySPI.setHttpClientFactory((host, port, pollTime, requestBatchSize, timeOutInMilliseconds, poolSize, autoFlush) -> new HttpClientVertx(host, port, pollTime, requestBatchSize, timeOutInMilliseconds, poolSize, autoFlush));

        FactorySPI.setClientFactory(new VertxClientFactory());
    }
}