package com.lkq.services.docker.daemon;

import com.lkq.services.docker.daemon.consul.ConsulAPI;
import com.lkq.services.docker.daemon.consul.ConsulController;
import com.lkq.services.docker.daemon.consul.ConsulResponseParser;
import com.lkq.services.docker.daemon.consul.context.ConsulContext;
import com.lkq.services.docker.daemon.consul.context.ConsulContextFactory;
import com.github.lkq.smesh.docker.DockerClientFactory;
import com.github.lkq.smesh.docker.SimpleDockerClient;
import com.lkq.services.docker.daemon.env.Environment;
import com.lkq.services.docker.daemon.health.ConsulHealthChecker;
import com.lkq.services.docker.daemon.logging.JulToSlf4jBridge;
import com.lkq.services.docker.daemon.routes.v1.Routes;
import com.lkq.services.docker.daemon.utils.HttpClientFactory;

public class Launcher {
    public static void main(String[] args) {
        JulToSlf4jBridge.setup();
        new Launcher().start();
    }

    private void start() {
        ConsulAPI consulAPI = new ConsulAPI(new HttpClientFactory().create(), new ConsulResponseParser(), Environment.get().consulAPIPort());
        SimpleDockerClient dockerClient = SimpleDockerClient.create(DockerClientFactory.get());

        ConsulContext context = new ConsulContextFactory().createClusterNodeContext();
        String appVersion = Environment.get().appVersion();
        ConsulHealthChecker consulHealthChecker = new ConsulHealthChecker(consulAPI, context.nodeName(), appVersion);
        ConsulController consulController = new ConsulController(dockerClient);
        WebServer webServer = new WebServer(new Routes(consulHealthChecker), Environment.get().servicePort());

        App app = new App(context,
                consulController,
                consulHealthChecker,
                webServer,
                appVersion
        );

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

        app.start(Environment.get().forceRestart());
    }

}