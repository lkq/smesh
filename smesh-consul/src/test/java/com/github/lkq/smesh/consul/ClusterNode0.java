package com.github.lkq.smesh.consul;

import com.github.lkq.smesh.consul.cluster.AppContextCluster;

public class ClusterNode0 {
    public static void main(String[] args) {
        ClusterNode node = new ClusterNode();
        node.startNode(1026, AppContextCluster.node0());
    }
}
