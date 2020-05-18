package eu.dnetlib.iis.wf.report.pushgateway.process;

import io.prometheus.client.exporter.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Implementation of metric pusher creator pushing metrics to a running instance of pushgateway.
 */
public class PushGatewayPusherCreator implements MetricPusherCreator {
    private static final Logger logger = LoggerFactory.getLogger(PushGatewayPusherCreator.class);

    public MetricPusher create(String address) {
        Supplier<PushGateway> pushGatewaySupplier = () -> {
            try {
                return new PushGateway(address);
            } catch (Exception e) {
                logger.error("PushGateway creation failed.", e);
                return null;
            }
        };
        return create(pushGatewaySupplier);
    }

    public MetricPusher create(Supplier<PushGateway> pushGatewaySupplier) {
        return (collectorRegistry, job, groupingKey) -> Optional
                .ofNullable(pushGatewaySupplier.get())
                .ifPresent(pushGateway -> {
                    try {
                        pushGateway.push(collectorRegistry, job, groupingKey);
                    } catch (IOException e) {
                        logger.error("PushGateway push failed.", e);
                    }
                });
    }
}
