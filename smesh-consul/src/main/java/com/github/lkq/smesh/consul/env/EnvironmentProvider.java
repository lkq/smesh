package com.github.lkq.smesh.consul.env;

import com.amazonaws.util.EC2MetadataUtils;
import com.github.lkq.smesh.exception.SmeshException;
import spark.utils.StringUtils;

public class EnvironmentProvider {

    private static Environment env;

    public synchronized static Environment get() {
        if (env == null) {
            if (StringUtils.isNotEmpty(EC2MetadataUtils.getInstanceId())) {
                env = new EC2Environment();
            } else {
                env = new LinuxEnvironment();
            }
        }
        return env;
    }

    public static void set(Environment env) {
        if (EnvironmentProvider.env != null) {
            throw new SmeshException("environment already initialized");
        }
        EnvironmentProvider.env = env;
    }

}