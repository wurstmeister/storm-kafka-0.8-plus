package storm.kafka;

import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.imps.CuratorFrameworkState;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.curator.test.InstanceSpec;
import com.netflix.curator.test.TestingServer;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Date: 11/01/2014
 * Time: 13:15
 */
public class KafkaTestBroker {

    private int port;
    private KafkaServerStartable kafka;
    private TestingServer server;
    private CuratorFramework zookeeper;

    public KafkaTestBroker() {
        try {
            server = new TestingServer();
            String zookeeperConnectionString = server.getConnectString();
            ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
            zookeeper = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
            zookeeper.start();
            port = InstanceSpec.getRandomPort();
            kafka.server.KafkaConfig config = buildKafkaConfig(zookeeperConnectionString);
            kafka = new KafkaServerStartable(config);
            kafka.startup();
        } catch (Exception ex) {
            throw new RuntimeException("Could not start test broker", ex);
        }
    }

    private kafka.server.KafkaConfig buildKafkaConfig(String zookeeperConnectionString) {
        Properties p = new Properties();
        p.setProperty("zookeeper.connect", zookeeperConnectionString);
        p.setProperty("broker.id", "0");
        p.setProperty("port", "" + port);
        p.setProperty("log.dirs", getLogDir());
        return new KafkaConfig(p);
    }

    private String getLogDir() {
        File logDir = new File(System.getProperty("java.io.tmpdir"), "kafka/logs/kafka-test-" + port);
        return logDir.getAbsolutePath();
    }

    public String getBrokerConnectionString() {
        return "localhost:" + port;
    }

    public int getPort() {
        return port;
    }

    public void shutdown() throws IOException {
        kafka.shutdown();
        if (zookeeper.getState().equals(CuratorFrameworkState.STARTED)) {
            zookeeper.close();
        }
        server.close();
        FileUtils.deleteQuietly(new File(getLogDir()));
    }
}
