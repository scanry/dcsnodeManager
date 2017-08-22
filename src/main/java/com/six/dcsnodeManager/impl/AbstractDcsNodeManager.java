package com.six.dcsnodeManager.impl;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.six.dcsnodeManager.Node;
import com.six.dcsnodeManager.NodeEvent;
import com.six.dcsnodeManager.NodeResource;
import com.six.dcsnodeManager.api.NodeEventWatcher;
import com.six.dcsnodeManager.api.NodeProtocol;
import com.six.dcsnodeManager.api.NodeRegister;
import com.six.dcsnodeManager.api.DcsNodeManager;
import com.six.dcsnodeManager.exception.IllegalStateNodeException;

import six.com.rpc.AsyCallback;
import six.com.rpc.RpcClient;
import six.com.rpc.server.RpcServer;

/**
 * @author liusong
 * @date 2017年8月1日
 * @email 359852326@qq.com
 */

public abstract class AbstractDcsNodeManager implements DcsNodeManager {

	final static Logger log = LoggerFactory.getLogger(AbstractDcsNodeManager.class);

	private String appName;

	private String clusterName;

	private Thread keepliveThread;

	private long keepliveInterval;

	private volatile boolean shutdown = true;

	private AtomicBoolean healthy = new AtomicBoolean(false);

	public AbstractDcsNodeManager(String appName, String clusterName,long keepliveInterval) {
		Objects.requireNonNull(appName);
		Objects.requireNonNull(clusterName);
		this.appName = appName;
		this.clusterName = clusterName;
		this.keepliveInterval = keepliveInterval;
		keepliveThread = new Thread(() -> {
			while (shutdown) {
				//TODO加上与leader的心跳
				if (!isKeepalive()) {
					synchronized (healthy) {
						if (!isKeepalive()) {
							noHealthy();
							getNodeRegister().electionMaster();
							isHealthy();
						}
					}
				}
				try {
					Thread.sleep(AbstractDcsNodeManager.this.keepliveInterval);
				} catch (InterruptedException e) {
				}
			}
		}, "node-keepalive-thread");
		keepliveThread.setDaemon(true);
	}

	@Override
	public String getAppName() {
		return appName;
	}

	@Override
	public String getClusterName() {
		return clusterName;
	}

	@Override
	public Node getCurrentNode() {
		checkHealthy();
		return getNodeRegister().getCurrentNode();

	}

	@Override
	public boolean isMaster() {
		checkHealthy();
		return getCurrentNode().isMaster();

	}

	@Override
	public Node getMaster() {
		checkHealthy();
		return getNodeRegister().getMaster();

	}

	@Override
	public List<Node> getSlaveNode() {
		checkHealthy();
		return getNodeRegister().getSlaveNodes();

	}

	@Override
	public List<Node> getNodes() {
		checkHealthy();
		return getNodeRegister().getNodes();

	}

	@Override
	public void start() {
		getRpcServer().register(NodeProtocol.class, new NodeProtocolImpl(this));
		getNodeRegister().electionMaster();
		isHealthy();
		keepliveThread.start();
	}

	@Override
	public List<NodeResource> applyNodeResources(int nodeNum, int threadNum) {
		checkHealthy();
		List<NodeResource> freeList = null;
		if (getCurrentNode().isMaster()) {

		} else if (getCurrentNode().isSlave()) {

		} else {
			throw new IllegalStateNodeException("");
		}
		return freeList;

	}

	@Override
	public void lockNodeResources(List<NodeResource> list) {
		checkHealthy();
		throw new UnsupportedOperationException();

	}

	@Override
	public void returnNodeResources(List<NodeResource> list) {
		checkHealthy();
		throw new UnsupportedOperationException();

	}

	@Override
	public void registerNodeEvent(NodeEvent NodeEvent, NodeEventWatcher nodeEventWatcher) {
		getNodeRegister().registerNodeEvent(NodeEvent, nodeEventWatcher);
	}

	@Override
	public <T> T loolupService(Node node, Class<T> clz, AsyCallback asyCallback) {
		checkHealthy();
		return getRpcCilent().lookupService(node.getIp(), node.getTrafficPort(), clz, asyCallback);

	}

	@Override
	public <T> T loolupService(Node node, Class<T> clz) {
		checkHealthy();
		return getRpcCilent().lookupService(node.getIp(), node.getTrafficPort(), clz);

	}
	
	@Override
	public void registerService(Class<?> protocol, Object instance){
		getRpcServer().register(protocol, instance);
	}

	public final synchronized void shutdown() {
		shutdown = true;
		doShutdown();
	}

	void checkHealthy() {
		if (!healthy.get()) {
			synchronized (healthy) {
			}
		}
	}
	
	void isHealthy(){
		healthy.set(true);
	}
	void noHealthy(){
		healthy.set(false);
	}

	protected abstract boolean isKeepalive();

	protected abstract NodeRegister getNodeRegister();

	protected abstract List<Node> getSlaveNodes();

	protected abstract RpcServer getRpcServer();

	protected abstract RpcClient getRpcCilent();

	protected abstract void doShutdown();

}
