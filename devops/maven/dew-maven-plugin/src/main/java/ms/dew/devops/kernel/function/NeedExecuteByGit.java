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

package ms.dew.devops.kernel.function;

import com.ecfront.dew.common.$;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1Service;
import ms.dew.devops.helper.GitHelper;
import ms.dew.devops.helper.KubeHelper;
import ms.dew.devops.helper.KubeOpt;
import ms.dew.devops.kernel.Dew;
import ms.dew.devops.kernel.config.FinalProjectConfig;
import ms.dew.devops.kernel.flow.BasicFlow;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class NeedExecuteByGit {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static void setNeedExecuteProjects(boolean quiet) throws ApiException, IOException {
        if (initialized.getAndSet(true)) {
            return;
        }
        Dew.log.info("Fetch need process projects");
        for (FinalProjectConfig config : Dew.Config.getProjects().values()) {
            Dew.log.debug("Execute need process checking for" + config.getAppName());
            String lastVersionDeployCommit = fetchLastVersionDeployCommit(config);

            Dew.log.debug("Latest commit is " + lastVersionDeployCommit);
            // 判断有没有发过版本
            if (lastVersionDeployCommit != null) {
                List<String> changedFiles = fetchGitDiff(lastVersionDeployCommit);
                // 判断有没有代码变更
                if (!hasUnDeployFiles(changedFiles, config)) {
                    config.setSkip(true);
                }
            }
        }
        List<FinalProjectConfig> processingProjects = Dew.Config.getProjects().values().stream()
                .filter(config -> !config.isSkip())
                .collect(Collectors.toList());
        if (processingProjects.isEmpty()) {
            Dew.stopped = true;
            Dew.log.info("No project found to be processed");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n==================== Processing Projects =====================\r\n\r\n");
        sb.append(processingProjects.stream().map(config ->
                "> " + config.getMvnGroupId() + ":" + config.getMvnArtifactId())
                .collect(Collectors.joining("\r\n")));
        sb.append("\r\n\r\n==============================================================\r\n");
        if (quiet) {
            Dew.log.info(sb.toString());
        } else {
            sb.append("\r\n< Y > or < N >");
            Dew.log.info(sb.toString());
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            if (reader.readLine().trim().equalsIgnoreCase("N")) {
                Dew.stopped = true;
                Dew.log.info("Process canceled");
            }
        }
    }

    private static String fetchLastVersionDeployCommit(FinalProjectConfig config) throws ApiException {
        V1Service lastVersionService = KubeHelper.inst(config.getId()).read(config.getAppName(), config.getNamespace(), KubeOpt.RES.SERVICE, V1Service.class);
        if (lastVersionService == null) {
            return null;
        } else {
            return lastVersionService.getMetadata().getAnnotations().get(BasicFlow.FLAG_KUBE_RESOURCE_GIT_COMMIT);
        }
    }

    private static List<String> fetchGitDiff(String lastVersionDeployCommit) {
        List<String> changedFiles = GitHelper.inst().diff(lastVersionDeployCommit, "HEAD");
        Dew.log.debug("Change files:");
        Dew.log.debug("-------------------");
        changedFiles.forEach(file -> Dew.log.debug(">>" + file));
        Dew.log.debug("-------------------");
        return changedFiles;
    }

    private static boolean hasUnDeployFiles(List<String> changedFiles, FinalProjectConfig projectConfig) {
        File basePath = new File(projectConfig.getMvnDirectory());
        while (!Arrays.asList(basePath.list()).contains(".git")) {
            basePath = basePath.getParentFile();
        }
        String projectPath = projectConfig.getMvnDirectory().substring(basePath.getPath().length() + 1).replaceAll("\\\\", "/");
        changedFiles = changedFiles.stream()
                .filter(file -> file.startsWith(projectPath))
                .collect(Collectors.toList());
        Dew.log.debug("Found " + changedFiles.size() + " changed files ");
        if (changedFiles.isEmpty()) {
            return false;
        } else if (!projectConfig.getApp().getIgnoreChangeFiles().isEmpty()) {
            if (!$.file.noneMath(changedFiles, new ArrayList<>(projectConfig.getApp().getIgnoreChangeFiles()))) {
                Dew.log.debug("Found 0 changed files filter ignore files");
                return false;
            }
        }
        return true;
    }

}