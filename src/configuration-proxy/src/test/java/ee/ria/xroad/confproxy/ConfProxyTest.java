/**
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.confproxy;

import ee.ria.xroad.common.SystemProperties;
import ee.ria.xroad.common.conf.globalconf.ConfigurationDirectoryV2;
import ee.ria.xroad.confproxy.util.ConfProxyHelper;
import ee.ria.xroad.confproxy.util.OutputBuilder;
import ee.ria.xroad.signer.protocol.SignerClient;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

/**
 * Test program for the configuration proxy,
 * uses a pre-downloaded configuration.
 */
@Slf4j
public final class ConfProxyTest {

    private static ActorSystem actorSystem;

    /**
     * Unavailable utility class constructor.
     */
    private ConfProxyTest() { }

    /**
     * Configuration proxy test program entry point.
     * @param args program args
     * @throws Exception in case configuration proxy fails to start
     */
    public static void main(final String[] args) throws Exception {
        try {
            actorSystem = ActorSystem.create("ConfigurationProxy",
                    ConfigFactory.load().getConfig("configuration-proxy"));
            SignerClient.init(actorSystem);

            ConfProxyProperties conf = new ConfProxyProperties("PROXY1");
            ConfProxyHelper.purgeOutdatedGenerations(conf);
            ConfigurationDirectoryV2 confDir = new ConfigurationDirectoryV2(
                    conf.getConfigurationDownloadPath(SystemProperties.CURRENT_GLOBAL_CONFIGURATION_VERSION));

            OutputBuilder output = new OutputBuilder(confDir, conf,
                    SystemProperties.CURRENT_GLOBAL_CONFIGURATION_VERSION);
            output.buildSignedDirectory();
            output.moveAndCleanup();
        } catch (Exception ex) {
            log.error("Error when executing configuration-proxy", ex);
        } finally {
            Await.ready(actorSystem.terminate(), Duration.Inf());
        }
    }
}
