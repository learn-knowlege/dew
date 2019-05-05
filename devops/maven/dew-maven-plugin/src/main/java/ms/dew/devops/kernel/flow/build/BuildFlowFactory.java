/*
 * Copyright 2019. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ms.dew.devops.kernel.flow.build;

import io.kubernetes.client.ApiException;
import ms.dew.devops.kernel.Dew;
import ms.dew.devops.kernel.config.FinalProjectConfig;
import ms.dew.devops.kernel.flow.BasicFlow;

import java.io.IOException;

/**
 * Build flow factory.
 *
 * @author gudaoxuri
 */
public class BuildFlowFactory {

    /**
     * Choose basic flow.
     *
     * @return the basic flow
     */
    public static BasicFlow choose() {
        switch (Dew.Config.getCurrentProject().getKind()) {
            case JVM_SERVICE:
                return new JvmServiceBuildFlow();
            case FRONTEND:
                return new FrontendBuildFlow();
            default:
                return new BasicFlow() {
                    @Override
                    protected void process(FinalProjectConfig config, String flowBasePath) throws ApiException, IOException {
                    }
                };
        }
    }

}
