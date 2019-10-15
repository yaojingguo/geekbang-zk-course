# ZooKeeper实战与源代码剖析

极客时间课程首页: [ZooKeeper实战与源码剖析](https://time.geekbang.org/course/intro/100034201)

## 源码列表

### 1.7 ZooKeeper架构
- [在一个机器上配置一个3节点ZooKeeper集群的配置文件](src/main/resources/quorum)

### 2.1 ZooKeeper API介绍
- [WatcherTests](src/test/java/org/yao/WatcherTests.java)

### 2.2 ZooKeeper API-Watch示例
- [Java Watch Client](src/main/java/org/yao/watchclient)
- [seq.sh](scripts/seq.sh)
- [executor.sh](scripts/executor.sh)

### 2.6 使用 Apache Curator 简化 ZooKeeper 开发
- [CuratorTests](src/test/java/org/yao/CuratorTests.java)

### 3.4 通过动态配置实现不中断服务的集群成员变更
- [DigestGenerator](src/main/java/org/yao/DigestGenerator.java)

### 3.5 ZooKeeper节点是如何存储数据的
- [snapshotFormatter.sh](scripts/snapshotFormatter.sh)

### 4.1 使用ZooKeeper实现服务发现(1)
- [ServiceDiscoveryTests](src/test/java/org/yao/ServiceDiscoveryTests.java)

### 4.3 使用ZooKeeper实现服务发现(3)
- [A port of curator-x-discovery-server in Spring Boot](https://github.com/yaojingguo/curator-x-discovery-server)

### 4.4 Kafka是如何使用ZooKeeper的
- [multi API sample code](src/test/java/org/yao/ZooKeeperTests.java)

### 5.5 etcd API (1)
- [sample code for Range, Put and DeleteRange APIs](etcd-code/kv_test.go)

### 5.6 etcd API (2)
- [sample code for Txn API](etcd-code/kv_test.go)
- [sample code for Watch API](etcd-code/watch_test.go)
- [sample code for Lease API](etcd-code/lease_test.go)

### 5.7 使用etcd实现分布式队列
- [Queue](etcd-code/queue.go)

### 5.8 使用etcd实现分布式锁
- [Mutex](etcd-code/mutex.go)

### 5.9 如何搭建一个etcd生产环境
- [Procfile](etcd-code/cluster/Procfile)

### 5.9 如何搭建一个etcd生产环境

### 6.5 网络编程基础
- [echo_client.c](c-code/echo_client.c)
- [echo_server.c](c-code/echo_server.c)
- [EchoClient.java](src/main/java/org/yao/socket/EchoClient.java)
- [EchoServer.java](src/main/java/org/yao/socket/EchoServer.java)

### 6.6 事件驱动的网络编程
- [epoll_example.c](c-code/epoll_example.c)

### 6.7 Java的事件驱动网络编程
- [Netty Echo Example](src/main/java/org/yao/netty/echo)

## PPT
1. [第一章：基础篇](slides/第一章：基础篇.pdf)

1. [第二章：开发篇](slides/第二章：开发篇.pdf)

1. [第三章：运维篇](slides/第三章：运维篇.pdf)

1. [第四章：进阶篇](slides/第四章：进阶篇.pdf)

1. [第五章：对比Chubby、etcd和ZooKeeper](slides/第五章：对比Chubby、etcd和ZooKeeper.pdf)

1. [第六章：ZooKeeper实现原理和源码解读](slides/第六章：ZooKeeper实现原理和源码解读.pdf)
