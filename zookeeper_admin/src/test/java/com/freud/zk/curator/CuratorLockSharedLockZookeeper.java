package com.freud.zk.curator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * 
 * Zookeeper - Curator - Lock - SharedLock
 * 
 * @author Freud
 *
 */
public class CuratorLockSharedLockZookeeper {

	private static final int SECOND = 1000;

	public static void main(String[] args) throws Exception {

		final CuratorLockSharedLockZookeeper instance = new CuratorLockSharedLockZookeeper();
		CuratorFramework client = instance.getStartedClient();
		String path = "/curator_lock/shared_lock";
		final InterProcessSemaphoreMutex lock = new InterProcessSemaphoreMutex(client, path);
		final CountDownLatch down = new CountDownLatch(1);
		for (int i = 0; i < 30; i++) {
			new Thread(new Runnable() {
				public void run() {
					try {
						down.await();
						// 非重入锁 - 只可以获取一次
						lock.acquire();
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss|SSS");
						String orderNo = sdf.format(new Date());
						System.out.println("生成的订单号是:" + orderNo);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							// 由于获取一次，所以释放一次
							lock.release();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}).start();
		}
		// 保证所有线程内部逻辑执行时间一致
		down.countDown();
		Thread.sleep(10 * SECOND);
		if (client != null) {
			client.close();
		}
		System.out.println("Server closed...");
	}

	private CuratorFramework getStartedClient() {
		RetryPolicy rp = new ExponentialBackoffRetry(1 * SECOND, 3);
		// Fluent风格创建
		CuratorFramework cfFluent = CuratorFrameworkFactory.builder().connectString("localhost:2181")
				.sessionTimeoutMs(5 * SECOND).connectionTimeoutMs(3 * SECOND).retryPolicy(rp).build();
		cfFluent.start();
		System.out.println("Server connected...");
		return cfFluent;
	}
}
