# 单机 K8s 部署：Redis / Nacos / Kafka

单节点场景下通过 NodePort 暴露，便于在**容器外**（宿主机或局域网）访问。

**镜像地址**：当前使用国内镜像前缀 `docker.m.daocloud.io`（DaoCloud 镜像加速）。若使用自建或其它内网仓库，可在各 YAML 中全局替换该前缀。Kafka 镜像未加国内前缀（DaoCloud 对 apache/kafka 返回 403）。若直连 Docker Hub 超时，请先将 `apache/kafka:3.9.2` 拉到本机后推送到自建仓库，再改 YAML 中 `image` 为内网地址。

| 服务 | 当前镜像 |
|------|----------|
| Redis | `docker.m.daocloud.io/library/redis:7-alpine` |
| MySQL | `docker.m.daocloud.io/library/mysql:8.0` |
| Nacos | `docker.m.daocloud.io/nacos/nacos-server:v2.3.0` |
| Zookeeper | `docker.m.daocloud.io/library/zookeeper:3.9`（可选，当前 Kafka 用 KRaft 无需 ZK） |
| Kafka | `apache/kafka:3.9.2`（官方镜像，KRaft 模式；DaoCloud 对 apache 空间 403，故用 Docker Hub 直连） |

## 部署顺序

Kafka 已改为 **Apache 官方镜像 + KRaft 模式**，无需 Zookeeper。按需部署：

```bash
# 创建命名空间（可选）
kubectl create namespace infra

# 部署（Kafka 无需先部署 Zookeeper）
kubectl apply -f mysql/mysql-deployment.yaml
kubectl apply -f kafka/kafka-deployment.yaml
kubectl apply -f redis/redis-deployment.yaml
kubectl apply -f nacos/nacos-deployment.yaml

# 若仍需 Zookeeper 作他用，可单独部署
# kubectl apply -f kafka/zookeeper-deployment.yaml
```

若未指定 `-n`，资源会创建在 `default` 命名空间。

## 容器外访问方式

假设节点 IP 为 `<NODE_IP>`（单机即本机 IP）：

| 服务   | 用途           | 容器外访问地址                    | NodePort |
|--------|----------------|-----------------------------------|----------|
| Redis  | 缓存/会话      | `<NODE_IP>:30379`                 | 30379    |
| MySQL  | 数据库        | `<NODE_IP>:30306`                 | 30306    |
| Nacos  | 控制台 / HTTP  | `http://<NODE_IP>:30848/nacos`    | 30848    |
| Nacos  | 客户端 gRPC    | `<NODE_IP>:30849`                 | 30849    |
| Kafka  | 生产/消费      | `<NODE_IP>:30092`                 | 30092    |
| Zookeeper | 仅集群内用  | 一般不需要从外网访问             | 32181    |

- **Redis**：`redis-cli -h <NODE_IP> -p 30379` 或应用连接 `<NODE_IP>:30379`。  
- **Nacos**：浏览器打开 `http://<NODE_IP>:30848/nacos`，默认账号/密码 `nacos/nacos`。  
- **Kafka**：Bootstrap 填 `<NODE_IP>:30092`，Kafka 会通过 `status.hostIP` 对外宣告该地址。

## 集群内访问（Pod 之间）

在同一 K8s 集群内，建议用 Service 名 + 集群端口，不经过 NodePort：

- Redis: `redis:6379`
- MySQL: `mysql:3306`
- Nacos HTTP: `nacos:8848`，gRPC: `nacos:9848`
- Kafka: `kafka:9092`（集群内）、对外用 `<NODE_IP>:30092`  
- Zookeeper: `zookeeper:2181`

## 数据持久化

各中间件均通过 **PersistentVolumeClaim（PVC）** 挂载数据目录，Pod 重启或重建后数据保留：

| 服务 | PVC 名称 | 挂载路径 | 容量 |
|------|----------|----------|------|
| Redis | redis-data | /data | 1Gi |
| MySQL | mysql-data | /var/lib/mysql | 10Gi |
| Nacos | nacos-data | /home/nacos/data | 5Gi（内置 Derby，不依赖 MySQL） |
| Kafka | kafka-data | /var/lib/kafka | 10Gi |
| Zookeeper | zookeeper-data | /data | 2Gi |

需确保集群已配置 StorageClass 或使用默认的 default StorageClass，否则 PVC 可能一直 Pending。

## 应用连接 MySQL（解决 UnknownHostException: \${MYSQL_SERVICE_HOST}）

若应用或 Nacos 报错 `UnknownHostException: ${MYSQL_SERVICE_HOST}`，说明连接串里用了占位符但未注入实际主机。请在该 Pod 的 Deployment 中设置环境变量，例如：

```yaml
env:
  - name: MYSQL_SERVICE_HOST
    value: "mysql"        # 与集群内 MySQL Service 名称一致
  - name: MYSQL_SERVICE_PORT
    value: "3306"
  # 若使用独立库/账号，再配 MYSQL_SERVICE_DB_NAME、用户名密码等
```

MySQL 部署后 root 密码来自 Secret `mysql-secret`（默认 `mysql123`），库名默认 `nacos`。修改密码可先删再建：`kubectl delete secret mysql-secret`，再 `kubectl create secret generic mysql-secret --from-literal=password=你的密码`，然后重新 apply `mysql/mysql-deployment.yaml`（或只改 Secret 后重启 MySQL Pod）。

## 单机说明

- 所有 Deployment 均为 `replicas: 1`，未做高可用。  
- Kafka 使用 Apache 官方镜像、KRaft 单节点模式（无需 Zookeeper）；Nacos 为 `MODE=standalone`，使用**内置数据库（Derby）**且数据持久化到 PVC，**不依赖 MySQL**。  
- 若需修改 NodePort，在对应 Service 的 `nodePort` 字段调整（范围 30000–32767）。
