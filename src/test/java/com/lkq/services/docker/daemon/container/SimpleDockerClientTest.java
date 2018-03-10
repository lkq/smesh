package com.lkq.services.docker.daemon.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.lkq.services.docker.daemon.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import spark.utils.StringUtils;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class SimpleDockerClientTest {

    private SimpleDockerClient simpleDockerClient;

    private final String helloWorldImage = "hello-world:latest";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DockerClient dockerClient;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectContainerResponse inspectResponse;

    @BeforeEach
    void setUp() {
        initMocks(this);
        simpleDockerClient = SimpleDockerClient.create(dockerClient);
    }

    @Test
    void canStartContainer() {
        StartContainerCmd startContainerCmd = mock(StartContainerCmd.class);
        given(dockerClient.startContainerCmd(anyString())).willReturn(startContainerCmd);
        given(dockerClient.inspectContainerCmd(anyString()).exec()).willReturn(inspectResponse);
        given(inspectResponse.getState().getRunning()).willReturn(true);

        Boolean started = simpleDockerClient.startContainer("dummy-container");

        assertThat(started, is(true));
        verify(startContainerCmd, times(1)).exec();
    }

    @Test
    void willReturnTrueIfRenameSuccess() {
        boolean renamed = simpleDockerClient.renameContainer("dummy-container", "new-name");

        assertTrue(renamed);
    }

    @Test
    void willReturnFalseIfExceptionHappens() {
        given(dockerClient.renameContainerCmd(anyString())).willThrow(new RuntimeException("mock exception"));
        boolean renamed = simpleDockerClient.renameContainer("dummy-container", "new-name");

        assertFalse(renamed);
    }

    @IntegrationTest
    @Test
    void testContainerLifeCycle() {
        String containerID = null;
        SimpleDockerClient client = SimpleDockerClient.create(DockerClientFactory.get());
        try {
            String imageName = "hello-world";
            String containerName = imageName + "-" + System.currentTimeMillis();
            client.pullImage(imageName);
            containerID = client.createContainerBuilder(imageName, containerName).build();
            Boolean started = client.startContainer(containerID);
            assertTrue(started);
        } finally {
            if (StringUtils.isNotEmpty(containerID)) {
                boolean removed = client.removeContainer(containerID);
                assertTrue(removed);
            }
        }
    }

    @IntegrationTest
    @Test
    void willReturnFalseIfContainerNotExists() {
        String currentName = "dummy-container-" + System.currentTimeMillis();
        SimpleDockerClient client = SimpleDockerClient.create(DockerClientFactory.get());
        boolean renamed = client.renameContainer(currentName, "dummy-container");
        assertThat(renamed, is(false));
    }

    @IntegrationTest
    @Test
    void canActuallyRenameContainer() throws InterruptedException, IOException {
        String containerID = null;
        SimpleDockerClient client = SimpleDockerClient.create(DockerClientFactory.get());
        try {
            client.pullImage(helloWorldImage);
            String oldContainerName = "hello-world-" + System.currentTimeMillis();
            String newContainerName = oldContainerName + "-1";
            containerID = client.createContainerBuilder(helloWorldImage, oldContainerName).build();
            boolean renamed = client.renameContainer(oldContainerName, newContainerName);

            assertThat(renamed, is(true));

            InspectContainerResponse inspectResponse = client.get().inspectContainerCmd(containerID).exec();
            assertThat(inspectResponse.getName(), is("/" + newContainerName));

        } finally {
            if (StringUtils.isNotEmpty(containerID)) {
                client.removeContainer(containerID);
            }
        }
    }
}