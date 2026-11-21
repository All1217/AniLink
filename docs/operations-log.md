# 1 课程资源

课程资源链接：https://pan.baidu.com/s/19hRg2LI1AuqXTQL9IoG5dw?pwd=9987

提取码：9987

老师（或者说黑马大部分老师）上课喜欢用的截图软件叫Snipaste

Linux操作系统被装在D盘（固态硬盘，D:\Programs\Linux\CentOS-7.vmx），需要先启动固态硬盘才能开机。

# 2 课程衔接

之前的学习是通过尚硅谷的尚庭公寓课程学习（[尚硅谷Java项目【尚庭公寓】从0开始Java项目实战_哔哩哔哩_bilibili](https://www.bilibili.com/video/BV1At421K7gP/?spm_id_from=333.1387.favlist.content.click&vd_source=7e1929799f2f0e3945358988a0f7b127)），使用的远程连接软件是MobaXterm。VMware列表里有两台虚拟机是因为之前进行过虚拟机克隆，对应内容在“尚庭公寓-32-6:00”左右。

远程访问对应“尚庭公寓-45”。打开远程链接的时候，一开头输入密码时，敲了几个数字看似没反应，实际上已经输入进去了，这个时候按回车能得到反馈结果，它的UI界面更新不及时。成功进入到系统内部时就变正常了。



# 3 vi/vim编辑器备忘录

![](images/vi编辑器工作模式.png)

## 4.1 Vim编辑器命令模式下常用命令

以下是Vim编辑器在命令模式下的常用命令分类表格：

### 4.1.1 光标移动

| 命令       | 功能说明             |
| ---------- | -------------------- |
| `h` 或 `←` | 向左移动光标         |
| `j` 或 `↓` | 向下移动光标         |
| `k` 或 `↑` | 向上移动光标         |
| `l` 或 `→` | 向右移动光标         |
| `0` 或 `^` | 移动到行首           |
| `$`        | 移动到行尾           |
| `gg`       | 移动到文件第一行     |
| `G`        | 移动到文件最后一行   |
| `nG`       | 移动到第n行          |
| `:n`       | 移动到第n行          |
| `w`        | 移动到下一个单词开头 |
| `b`        | 移动到上一个单词开头 |
| `e`        | 移动到单词末尾       |

### 4.1.2 编辑操作

| 命令        | 功能说明                           |
| ----------- | ---------------------------------- |
| `i`         | 进入插入模式（当前光标前）         |
| `a`         | 进入插入模式（当前光标后）         |
| `I`         | 进入插入模式（行首）               |
| `A`         | 进入插入模式（行尾）               |
| `o`         | 在当前行下方插入新行并进入插入模式 |
| `O`         | 在当前行上方插入新行并进入插入模式 |
| `x`         | 删除当前光标字符                   |
| `dd`        | 删除当前行                         |
| `ndd`       | 删除n行（如3dd删除3行）            |
| `dw`        | 删除从光标到单词末尾               |
| `d$` 或 `D` | 删除从光标到行尾                   |
| `d0`        | 删除从光标到行首                   |
| `yy`        | 复制当前行                         |
| `nyy`       | 复制n行                            |
| `yw`        | 复制一个单词                       |
| `p`         | 粘贴到光标后                       |
| `P`         | 粘贴到光标前                       |
| `u`         | 撤销上一次操作                     |
| `Ctrl + r`  | 重做被撤销的操作                   |
| `.`         | 重复上一次编辑操作                 |

### 4.1.3 搜索与替换

| 命令                | 功能说明                 |
| ------------------- | ------------------------ |
| `/pattern`          | 向前搜索指定模式         |
| `?pattern`          | 向后搜索指定模式         |
| `n`                 | 重复上一次搜索（同方向） |
| `N`                 | 重复上一次搜索（反方向） |
| `:%s/old/new/g`     | 全文替换old为new         |
| `:s/old/new/g`      | 当前行替换old为new       |
| `:n1,n2s/old/new/g` | 在n1到n2行间替换         |

### 4.1.4 屏幕控制

| 命令       | 功能说明             |
| ---------- | -------------------- |
| `Ctrl + f` | 向前翻页             |
| `Ctrl + b` | 向后翻页             |
| `Ctrl + d` | 向下翻半页           |
| `Ctrl + u` | 向上翻半页           |
| `zz`       | 将当前行置于屏幕中央 |
| `zt`       | 将当前行置于屏幕顶部 |
| `zb`       | 将当前行置于屏幕底部 |

### 4.1.5 文件操作

| 命令          | 功能说明                     |
| ------------- | ---------------------------- |
| `:w`          | 保存文件                     |
| `:w filename` | 另存为filename               |
| `:q`          | 退出Vim                      |
| `:q!`         | 强制退出，不保存             |
| `:wq` 或 `ZZ` | 保存并退出                   |
| `:x`          | 保存并退出（仅当文件修改时） |
| `:e filename` | 打开另一个文件               |
| `:r filename` | 读取文件内容插入到当前光标后 |

### 4.1.6 其他实用命令

| 命令        | 功能说明                 |
| ----------- | ------------------------ |
| `J`         | 将下一行合并到当前行     |
| `>>`        | 向右缩进当前行           |
| `<<`        | 向左缩进当前行           |
| `==`        | 自动缩进当前行           |
| `%`         | 跳转到匹配的括号         |
| `:set nu`   | 显示行号                 |
| `:set nonu` | 隐藏行号                 |
| `v`         | 进入可视模式（字符选择） |
| `V`         | 进入可视模式（行选择）   |
| `Ctrl + v`  | 进入可视块模式           |

### 4.1.7 组合命令示例

| 命令组合          | 功能说明                      |
| ----------------- | ----------------------------- |
| `dG`              | 删除从当前行到文件末尾        |
| `yG`              | 复制从当前行到文件末尾        |
| `dgg`             | 删除从当前行到文件开头        |
| `:1,$s/old/new/g` | 全文替换（同`:%s/old/new/g`） |
| `10,20d`          | 删除第10-20行                 |

**提示**：
1. 命令前加数字表示重复次数，如`5dd`删除5行
2. 按`Esc`键可确保返回命令模式
3. 命令区分大小写，如`g`和`G`功能不同
4. 输入`:`进入底线命令模式，执行后自动返回命令模式

# 4 管道符和grep

## 4.1 管道符|

**管道符 `|` 的作用是将前一个命令的输出作为后一个命令的输入。**

```
# 基本语法
命令1 | 命令2
```

## 4.2 |和grep结合使用

```
# -i 忽略大小写
ps aux | grep -i "mysql"
cat config.txt | grep -i "password"

# -v 反向匹配（排除包含关键字的行）
ls -la | grep -v ".txt"  # 排除.txt文件
ps aux | grep -v "grep"  # 排除grep进程本身

# -n 显示行号
cat server.log | grep -n "404"  # 显示包含404的行及其行号

# -c 统计匹配行数
ps aux | grep -c "bash"  # 统计bash进程数量

# -A -B -C 显示上下文
tail -100 server.log | grep -A 2 "ERROR"  # 显示ERROR行及后2行
cat file.txt | grep -B 3 "exception"      # 显示异常行及前3行
cat file.txt | grep -C 2 "warning"        # 显示警告行及前后各2行

# -E 使用扩展正则表达式
netstat -tuln | grep -E "(80|443)"        # 匹配80或443端口
```

# 5 问题备忘录

## 5.1 参数设置

Linux登录密码（root用户）：123456

mysql登陆密码：Yuuki.1223

redis密码：123456

大部分软件已安装在/export/server目录下。每次安装记得创建软链接（可以保留源文件版本号并方便访问）。

## 5.2 tomcat

目录/export/server有已经安装好的tomcat服务器，版本为10.1.50。由于当前虚拟机仅安装了jdk8，而该版本服务器仅支持jdk11以上（最好jdk17），所以无法正常启动。

此外，为了方便学习，防火墙已关闭且禁止开机自启。

## 5.3 跳转学习

黑马的实战篇目前只大概演练了MySQL、Tomcat、Redis的安装部署。

## 5.4 关于Redis远程连接

IP套接字：192.168.234.100:6379

密码：123456

**不要添加用户名！**



## 5.5 CentOS 7 网络故障排错总结

因为之前点错到虚拟机挂起，导致虚拟机挂起前后状态不一致，配置文件混乱，出现网络问题。之前第一次发现时重启网络服务就解决了，具体文档不知道写在哪里了，而且问题确实没有得到根本性解决，遂重写一份。

### 5.5.1 问题现象

1. **开机后找不到网卡**：图形界面网络设置中无网卡选项
2. **无法上网**：远程连接失败
3. **网络服务启动失败**：执行 `systemctl restart network` 报错
   - 错误信息：`Job for network.service failed`
   - 关键错误：`RTNETLINK answers: File exists`
4. **设置-->网络一栏无可操作选项（无法像往常一样手动配置网络信息）**

### 5.5.2 排错步骤

```bash
# 查看network服务详细错误
systemctl status network.service
journalctl -xe

# 关键发现：RTNETLINK answers: File exists（网络配置冲突）
```

```bash
# 查看所有网络接口
ip link show

# 查看网络配置文件
ls /etc/sysconfig/network-scripts/ifcfg-*
```
- `ifcfg-ens33`：物理网卡配置文件
- `ifcfg-lo`：本地回环接口（正常）

**检查NetworkManager状态**

```bash
# 查看NetworkManager是否在运行
systemctl status NetworkManager

# 查看NetworkManager管理的连接
nmcli connection show
```

**解决服务冲突**

**关键问题**：`network.service` 和 `NetworkManager.service` 冲突

**解决方案**：

```bash
# 1. 停止并禁用传统的network服务
systemctl stop network
systemctl disable network

# 2. 重启NetworkManager
systemctl restart NetworkManager

# 3. 重新加载连接
nmcli connection reload
nmcli connection up ens33
```

**修复"unmanaged device"错误**

如果遇到 `错误：连接激活失败：No suitable device found`：

```bash
# 1. 查看设备状态
nmcli device status

# 2. 编辑NetworkManager配置
vi /etc/NetworkManager/NetworkManager.conf
# 确保没有 unmanaged-devices 配置

# 3. 重新创建连接（使用静态IP）
nmcli connection delete <连接UUID>
nmcli connection add type ethernet con-name ens33 ifname ens33 \
ipv4.method manual \
ipv4.addresses 192.168.150.102/24 \
ipv4.gateway 192.168.150.2 \
ipv4.dns 192.168.150.2 \
ipv6.method ignore \
connection.autoconnect yes
```

**第六步：验证网络配置**

```bash
# 查看网卡状态
ip link show ens33
# 期望输出：state UP

# 查看IP地址
ip addr show ens33
# 期望输出：inet 192.168.150.102/24

# 测试网络连通性
ping -c 4 192.168.150.2    # 测试网关
ping -c 4 8.8.8.8          # 测试外网
```

### 5.5.3 关键配置解析

#### 正确的网卡配置文件
```bash
cat /etc/sysconfig/network-scripts/ifcfg-ens33
```
```
TYPE=Ethernet
BOOTPROTO=none            # 静态IP
DEFROUTE=yes              # 默认路由
NAME=ens33
DEVICE=ens33
ONBOOT=yes                # 开机自启
IPADDR=192.168.150.102    # IP地址
PREFIX=24                 # 子网掩码
GATEWAY=192.168.150.2     # 网关
DNS1=192.168.150.2        # DNS服务器
```

#### 服务管理
```bash
# 启用NetworkManager开机自启
systemctl enable NetworkManager

# 确认network服务已禁用
systemctl is-enabled network  # 应显示 disabled or masked
```

### 5.5.4 常用诊断命令

#### 1. 查看网络状态
```bash
nmcli device status        # 查看所有设备状态
nmcli connection show       # 查看所有连接
systemctl status NetworkManager  # 查看NetworkManager状态
```

#### 2. 排查网络冲突
```bash
ip route show              # 查看路由表
ip addr show               # 查看IP地址
journalctl -u NetworkManager -f  # 实时查看NetworkManager日志
```

#### 3. 网络测试
```bash
ping -c 4 <IP地址>          # 测试连通性
traceroute <IP地址>         # 跟踪路由
nslookup <域名>             # 测试DNS解析
```

### 5.5.5 常见错误及解决方法

| 错误信息                         | 可能原因                   | 解决方法                            |
| -------------------------------- | -------------------------- | ----------------------------------- |
| `RTNETLINK answers: File exists` | 网络配置冲突               | 停止network服务，使用NetworkManager |
| `No suitable device found`       | 设备不受NetworkManager管理 | 检查NetworkManager.conf配置         |
| `Connection activation failed`   | 配置错误或服务冲突         | 重新创建连接配置                    |
| 图形界面无网卡选项               | NetworkManager未运行       | 启动NetworkManager服务              |

**注意事项：**

1. **RTNETLINK errors** 通常表示配置冲突，不是硬件问题
2. **NetworkManager** 和 **network** 服务不要同时启用
3. **ifcfg-ens33** 配置文件中的 `ONBOOT=yes` 必须设置
4. **修改配置后要重启**相应服务
5. **虚拟机环境**中Wi-Fi选项不可用是正常的

这个总结涵盖了从发现问题到解决问题的完整过程，下次遇到类似问题可以按步骤排查。

## 5.6 Docker部署组件在挂载数据卷时产生错误覆盖问题



# 6 Minio

```
endpoint: http://192.168.150.102:9000
access_key: O2gkDVBgbbiRM632bAMa
secret_key: 0lhst7ToHGX7t57w2ekL6ZxKELEE0bwWgvgw8zfS
```

默认密码和账号均为minioadmin

192.168.150.102:9001为管理界面，192.168.150.102:9000为API端口

```bash
docker run -d \
  --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -v /data/minio:/data \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  --restart=always \
  minio/minio:RELEASE.2024-08-29T01-40-52Z \
  server /data --console-address ":9001"
```

 冒号 `:` 是 Docker 卷挂载的分隔符，格式为：

```
宿主机路径:容器内路径
```

- **冒号左边** `/data/minio` —— 是 **CentOS 宿主机**上的真实目录
- **冒号右边** `/data` —— 是 **容器内部**的目录

作用是将两个目录"映射"在一起，容器写入 `/data` 的数据，实际上都保存到了宿主机的 `/opt/minio/data` 中。这样即使容器被删除，数据也不会丢失。

# 7 Redis6

## 7.1 安装Redis6

由于Redis6在docker已安装成功，因此旧版通过yum安装的3.12版本的redis已卸载！

> ### 完全卸载
>
> ```
> # 1. 停止 Redis 服务
> sudo systemctl stop redis
> sudo systemctl disable redis
> 
> # 2. 查找已安装的 Redis 包
> rpm -qa | grep -i redis
> 
> # 3. 卸载 Redis 包
> # 根据上一步的结果，通常会是以下之一：
> sudo yum remove redis
> # 或
> sudo yum remove redis6
> # 或
> sudo yum remove redis-server
> 
> # 4. 删除残留的配置和数据文件
> # 配置文件
> sudo rm -rf /etc/redis
> sudo rm -rf /etc/redis.conf
> 
> # 数据文件（如果不再需要）
> sudo rm -rf /var/lib/redis
> sudo rm -rf /var/log/redis
> sudo rm -rf /var/run/redis
> 
> # 5. 删除 redis 用户（如果存在）
> sudo userdel -r redis
> 
> # 6. 清理未使用的依赖
> sudo yum autoremove
> 
> # 7. 验证卸载
> redis-server --version  # 应该提示 command not found
> systemctl status redis  # 应该提示服务不存在
> ```

deepseek：

```
docker run -d \
  --name redis \
  -p 6379:6379 \
  -v /data/redis:/data \
  --restart always \
  redis:6.2 \
  redis-server --requirepass 123456 --appendonly yes
```

 **`-d` (detach mode)：**

- 后台运行容器
- 容器启动后，终端可以继续使用，不会阻塞

**`--name redis6`：**

- 给容器指定一个名字叫 `redis6`
- 之后可以用这个名字来管理容器，而不需要用容器ID

**`-p 6379:6379` (port mapping)：**

- **宿主机端口:容器端口**
- 将宿主机的 6379 端口映射到容器的 6379 端口
- Redis 默认使用 6379 端口
- 这样外部程序（如您的 Python 代码）可以通过宿主机的 IP 和 6379 端口访问 Redis

 **`-v /data/redis:/data` (volume mounting)：**

- **宿主机目录:容器目录**
- 将宿主机的 `/data/redis` 目录挂载到容器的 `/data` 目录
- Redis 默认将数据保存在容器的 `/data` 目录
- 作用：数据持久化，即使容器删除，数据还在宿主机上

 **`--restart always`：**

- 容器退出时总是自动重启
- 包括以下几种情况：
  - 容器崩溃
  - 宿主机重启
  - Docker 服务重启

**`redis:6.2`：**

- 使用的镜像名称和标签
- `redis`: 镜像名
- `6.2`: 标签，表示 Redis 6.2 版本

**`redis-server --requirepass yourpassword --appendonly yes`：**

- 容器启动后执行的命令
- `redis-server`: 启动 Redis 服务
- `--requirepass yourpassword`: 设置访问密码为 `yourpassword`
- `--appendonly yes`: 启用 AOF 持久化，追加方式记录所有写操作

**这条命令总体的意思是：**

> "在后台运行一个名为 redis6 的容器，使用 redis:6.2 镜像，将宿主机的 6379 端口映射到容器的 6379 端口，将宿主机的 /data/redis 目录挂载到容器的 /data 目录用于数据持久化，设置容器退出时自动重启，并在容器启动时运行 redis-server 命令，设置密码为 yourpassword，启用 AOF 持久化。"

## 7.2 redis-stack

这是redis的扩展版，除了支持缓存、消息队列等传统功能，还支持向量数据库等五大新模块。

```
# 拉取与 Redis 6.2.6 对应的版本
docker pull redis/redis-stack:6.2.6-v7
```

运行容器：

```
docker run -d \
  --name rs \
  -p 6379:6379 \
  -p 8001:8001 \
  -v /data/redis-stack:/data \
  --restart always \
  redis/redis-stack:6.2.6-v7 \
  redis-server --requirepass 123456 --appendonly yes
```



# 8 Elasticsearch

## 8.1 部署单点es

### 8.1.1 创建网络

因为我们还需要部署kibana容器，因此需要让es和kibana容器互联。这里先创建一个网络：

```sh
docker network create es-net
```

### 8.1.2 加载镜像

这里我们采用elasticsearch的7.12.1版本的镜像，这个镜像体积非常大，接近1G。不建议大家自己pull。

课前资料提供了镜像的tar包：

![image-20210510165308064](../../../IT公开课/黑马程序员/微服务技术栈/DAY5-ElasticSearch01/assets/image-20210510165308064.png)

大家将其上传到虚拟机中，然后运行命令加载即可：

```sh
# 导入数据
docker load -i es.tar
```

同理还有`kibana`的tar包也需要这样做。



### 8.1.3 运行

运行docker命令，部署单点es：

```sh
docker run -d \
	--name es \
    -e "discovery.type=single-node" \
    -v /data/elasticsearch/data:/usr/share/elasticsearch/data \
    -v /data/elasticsearch/plugins:/usr/share/elasticsearch/plugins \
    --privileged \
    --restart=always \
    --network es-net \
    -p 9200:9200 \
    -p 9300:9300 \
elasticsearch:8.5.3

限制内存可加：-e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
```

命令解释：

- `-e "cluster.name=es-docker-cluster"`：设置集群名称
- `-e "http.host=0.0.0.0"`：监听的地址，可以外网访问
- `-e "ES_JAVA_OPTS=-Xms512m -Xmx512m"`：内存大小
- `-e "discovery.type=single-node"`：非集群模式
- `-v es-data:/usr/share/elasticsearch/data`：挂载逻辑卷，绑定es的数据目录
- `-v es-logs:/usr/share/elasticsearch/logs`：挂载逻辑卷，绑定es的日志目录
- `-v es-plugins:/usr/share/elasticsearch/plugins`：挂载逻辑卷，绑定es的插件目录
- `--privileged`：授予逻辑卷访问权
- `--network es-net` ：加入一个名为es-net的网络中
- `-p 9200:9200`：端口映射配置



在浏览器中输入：http://192.168.150.101:9200 即可看到elasticsearch的响应结果：

![image-20210506101053676](../../../IT公开课/黑马程序员/微服务技术栈/DAY5-ElasticSearch01/assets/image-20210506101053676.png)





## 8.2 部署kibana

kibana可以给我们提供一个elasticsearch的可视化界面，便于我们学习。

### 8.2.1 部署

运行docker命令，部署kibana

```sh
docker run -d \
--name kibana \
-e ELASTICSEARCH_HOSTS=http://es:9200 \
--network=es-net \
-p 5601:5601  \
kibana:8.5.3
```

- `--network es-net` ：加入一个名为es-net的网络中，与elasticsearch在同一个网络中
- `-e ELASTICSEARCH_HOSTS=http://es:9200"`：设置elasticsearch的地址，因为kibana已经与elasticsearch在一个网络，因此可以用容器名直接访问elasticsearch
- `-p 5601:5601`：端口映射配置

kibana启动一般比较慢，需要多等待一会，可以通过命令：

```sh
docker logs -f kibana
```

查看运行日志，当查看到下面的日志，说明成功：

![image-20210109105135812](../../../IT公开课/黑马程序员/微服务技术栈/DAY5-ElasticSearch01/assets/image-20210109105135812.png)

此时，在浏览器输入地址访问：http://192.168.150.101:5601，即可看到结果

### 8.2.2 DevTools

kibana中提供了一个DevTools界面：

![image-20210506102630393](../../../IT公开课/黑马程序员/微服务技术栈/DAY5-ElasticSearch01/assets/image-20210506102630393.png)

这个界面中可以编写DSL来操作elasticsearch。并且对DSL语句有自动补全功能。



## 8.3 安装IK分词器



### 8.3.1 在线安装ik插件（较慢）

```shell
# 进入容器内部
docker exec -it elasticsearch /bin/bash

# 在线下载并安装
./bin/elasticsearch-plugin  install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.12.1/elasticsearch-analysis-ik-7.12.1.zip

#退出
exit
#重启容器
docker restart elasticsearch
```

### 8.3.2 离线安装ik插件（推荐）

#### 1）查看数据卷目录

安装插件需要知道elasticsearch的plugins目录位置，而我们用了数据卷挂载，因此需要查看elasticsearch的数据卷目录，通过下面命令查看:

```sh
docker volume inspect es-plugins
```

显示结果：

```json
[
    {
        "CreatedAt": "2022-05-06T10:06:34+08:00",
        "Driver": "local",
        "Labels": null,
        "Mountpoint": "/var/lib/docker/volumes/es-plugins/_data",
        "Name": "es-plugins",
        "Options": null,
        "Scope": "local"
    }
]
```

说明plugins目录被挂载到了：`/var/lib/docker/volumes/es-plugins/_data `这个目录中。



#### 2）解压缩分词器安装包

下面我们需要把课前资料中的ik分词器解压缩，重命名为ik

![image-20210506110249144](../../../IT公开课/黑马程序员/微服务技术栈/DAY5-ElasticSearch01/assets/image-20210506110249144.png)

#### 3）上传到es容器的插件数据卷中

也就是`/var/lib/docker/volumes/es-plugins/_data `：

![image-20210506110704293](../../../IT公开课/黑马程序员/微服务技术栈/DAY5-ElasticSearch01/assets/image-20210506110704293.png)



####  4）重启容器

```shell
# 4、重启容器
docker restart es
```

```sh
# 查看es日志
docker logs -f es
```

#### 5）测试：

IK分词器包含两种模式：

* `ik_smart`：最少切分

* `ik_max_word`：最细切分



```json
GET /_analyze
{
  "analyzer": "ik_max_word",
  "text": "黑马程序员学习java太棒了"
}
```

结果：

```json
{
  "tokens" : [
    {
      "token" : "黑马",
      "start_offset" : 0,
      "end_offset" : 2,
      "type" : "CN_WORD",
      "position" : 0
    },
    {
      "token" : "程序员",
      "start_offset" : 2,
      "end_offset" : 5,
      "type" : "CN_WORD",
      "position" : 1
    },
    {
      "token" : "程序",
      "start_offset" : 2,
      "end_offset" : 4,
      "type" : "CN_WORD",
      "position" : 2
    },
    {
      "token" : "员",
      "start_offset" : 4,
      "end_offset" : 5,
      "type" : "CN_CHAR",
      "position" : 3
    },
    {
      "token" : "学习",
      "start_offset" : 5,
      "end_offset" : 7,
      "type" : "CN_WORD",
      "position" : 4
    },
    {
      "token" : "java",
      "start_offset" : 7,
      "end_offset" : 11,
      "type" : "ENGLISH",
      "position" : 5
    },
    {
      "token" : "太棒了",
      "start_offset" : 11,
      "end_offset" : 14,
      "type" : "CN_WORD",
      "position" : 6
    },
    {
      "token" : "太棒",
      "start_offset" : 11,
      "end_offset" : 13,
      "type" : "CN_WORD",
      "position" : 7
    },
    {
      "token" : "了",
      "start_offset" : 13,
      "end_offset" : 14,
      "type" : "CN_CHAR",
      "position" : 8
    }
  ]
}
```





### 8.3.3 扩展词词典

随着互联网的发展，“造词运动”也越发的频繁。出现了很多新的词语，在原有的词汇列表中并不存在。比如：“奥力给”，“传智播客” 等。

所以我们的词汇也需要不断的更新，IK分词器提供了扩展词汇的功能。

1）打开IK分词器config目录：

![image-20210506112225508](../../../IT公开课/黑马程序员/微服务技术栈/DAY5-ElasticSearch01/assets/image-20210506112225508.png)

2）在IKAnalyzer.cfg.xml配置文件内容添加：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
        <comment>IK Analyzer 扩展配置</comment>
        <!--用户可以在这里配置自己的扩展字典 *** 添加扩展词典-->
        <entry key="ext_dict">ext.dic</entry>
</properties>
```

3）新建一个 ext.dic，可以参考config目录下复制一个配置文件进行修改

```properties
传智播客
奥力给
```

4）重启elasticsearch 

```sh
docker restart es

# 查看 日志
docker logs -f elasticsearch
```

![image-20201115230900504](../../../IT公开课/黑马程序员/微服务技术栈/DAY5-ElasticSearch01/assets/image-20201115230900504.png)

日志中已经成功加载ext.dic配置文件

5）测试效果：

```json
GET /_analyze
{
  "analyzer": "ik_max_word",
  "text": "传智播客Java就业超过90%,奥力给！"
}
```

> 注意当前文件的编码必须是 UTF-8 格式，严禁使用Windows记事本编辑

### 8.3.4 停用词词典

在互联网项目中，在网络间传输的速度很快，所以很多语言是不允许在网络上传递的，如：关于宗教、政治等敏感词语，那么我们在搜索时也应该忽略当前词汇。

IK分词器也提供了强大的停用词功能，让我们在索引时就直接忽略当前的停用词汇表中的内容。

1）IKAnalyzer.cfg.xml配置文件内容添加：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
        <comment>IK Analyzer 扩展配置</comment>
        <!--用户可以在这里配置自己的扩展字典-->
        <entry key="ext_dict">ext.dic</entry>
         <!--用户可以在这里配置自己的扩展停止词字典  *** 添加停用词词典-->
        <entry key="ext_stopwords">stopword.dic</entry>
</properties>
```

3）在 stopword.dic 添加停用词

```properties
习大大
```

4）重启elasticsearch 

```sh
# 重启服务
docker restart elasticsearch
docker restart kibana

# 查看 日志
docker logs -f elasticsearch
```

日志中已经成功加载stopword.dic配置文件

5）测试效果：

```json
GET /_analyze
{
  "analyzer": "ik_max_word",
  "text": "传智播客Java就业率超过95%,习大大都点赞,奥力给！"
}
```

> 注意当前文件的编码必须是 UTF-8 格式，严禁使用Windows记事本编辑

## 8.4 部署es集群

部署es集群可以直接使用docker-compose来完成，不过要求你的Linux虚拟机至少有**4G**的内存空间

首先编写一个docker-compose文件，内容如下：

```sh
version: '2.2'
services:
  es01:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.12.1
    container_name: es01
    environment:
      - node.name=es01
      - cluster.name=es-docker-cluster
      - discovery.seed_hosts=es02,es03
      - cluster.initial_master_nodes=es01,es02,es03
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - data01:/usr/share/elasticsearch/data
    ports:
      - 9200:9200
    networks:
      - elastic
  es02:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.12.1
    container_name: es02
    environment:
      - node.name=es02
      - cluster.name=es-docker-cluster
      - discovery.seed_hosts=es01,es03
      - cluster.initial_master_nodes=es01,es02,es03
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - data02:/usr/share/elasticsearch/data
    networks:
      - elastic
  es03:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.12.1
    container_name: es03
    environment:
      - node.name=es03
      - cluster.name=es-docker-cluster
      - discovery.seed_hosts=es01,es02
      - cluster.initial_master_nodes=es01,es02,es03
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - data03:/usr/share/elasticsearch/data
    networks:
      - elastic

volumes:
  data01:
    driver: local
  data02:
    driver: local
  data03:
    driver: local

networks:
  elastic:
    driver: bridge
```



Run `docker-compose` to bring up the cluster:

```sh
docker-compose up
```

## 8.5 安装es-8.x版本

以上仅适用于7.x版本，如果把同样的步骤拿去安装es-8.x版本，会报错。以下为安装8.x版本教程。

原文链接：[docker安装Elasticsearch8.x【保姆级教程】 - tans9508 - 博客园](https://www.cnblogs.com/tans9508/articles/18406380)

执行流程：

**1 pull 镜像**

```
docker pull elasticsearch:8.12.2
```

**2 创建挂载目录**

```
mkdir -p /data/elasticsearch
```

**3 先在不挂载的情况下启动**

```bash
docker run -d \
--restart=always \
--name elasticsearch \
--network es-net \
-p 9200:9200 \
-p 9300:9300 \
--privileged \
-e "discovery.type=single-node" \
-e "ES_JAVA_OPTS=-Xms2g -Xmx2g" \
elasticsearch:8.12.2
```

**4 cp初始化文件**

```bash
docker cp elasticsearch:/usr/share/elasticsearch/data /data/elasticsearch/
docker cp elasticsearch:/usr/share/elasticsearch/plugins /data/elasticsearch/
docker cp elasticsearch:/usr/share/elasticsearch/config /data/elasticsearch/

赋权限
cd /data/elasticsearch/
chmod 777 config/ data/ plugins/
```

**5 删除未挂载启动的es容器**

```
docker stop elasticsearch
docker rm elasticsearch
```

**6 启动容器并挂载**

```
docker run -d \
--restart=always \
--name elasticsearch \
--network es-net \
-p 9200:9200 \
-p 9300:9300 \
--privileged \
-v /data/elasticsearch/data:/usr/share/elasticsearch/data \
-v /data/elasticsearch/plugins:/usr/share/elasticsearch/plugins \
-v /data/elasticsearch/config:/usr/share/elasticsearch/config \
-e "discovery.type=single-node" \
-e "ES_JAVA_OPTS=-Xms2g -Xmx2g" \
elasticsearch:8.12.2
```

**7 关闭安全验证**

- 导航到配置文件

  ```
  cd /data/elasticsearch/config/
  ```

- vim elasticsearch.yml

  ```
  默认为true，设置为false
  xpack.security.enabled: false
  ```

  然后保存退出。

- 重启es

  ```
  docker restart elasticsearch
  ```

**8 验证是否安装成功**

访问服务器加端口地址 **http://192.168.150.102:9200**

**9 安装kibana**

- 创建挂载目录

```bash
mkdir /data/kibana
```

- 无挂载启动

```bash
docker run -d \
  --restart=always \
  --name kibana \
  --network es-net \
  -p 5601:5601 \
  --privileged \
  -e ELASTICSEARCH_HOSTS=http://elasticsearch:9200 \
  kibana:8.12.2
```

`ELASTICSEARCH_HOSTS`：指定es链接信息

- cp文件

```bash
docker cp kibana:/usr/share/kibana/data /data/kibana/
docker cp kibana:/usr/share/kibana/config /data/kibana/
```

- 停止并删除原容器

```bash
docker stop kibana
docker rm kibana
```

- 创建新容器并挂载

```bash
docker run -d \
--restart=always \
--name kibana \
--network es-net \
-p 5601:5601 \
--privileged \
-v /data/kibana/data:/usr/share/kibana/data \
-v /data/kibana/config:/usr/share/kibana/config \
-e ELASTICSEARCH_HOSTS=http://elasticsearch:9200 \
kibana:8.12.2
```

- 验证是否安装成功

输入服务器IP地址+端口，如**192.168.150.102:5601**

**10 安装IK分词器**

- 以root用户身份进入容器

  ```
  docker exec -u 0 -it elasticsearch /bin/bash
  ```

- cd到bin

  ```
  cd /usr/share/elasticsearch/bin/
  ```

- 执行下载

  ```
  ./elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/8.12.2
  ```

- 重启es

  ```
  docker restart elasticsearch
  ```

  

### 8.5.1 为什么之前的方法会失效？

启动报错文件缺失 Missing logging config file

> ERROR: Missing logging config file at /usr/share/elasticsearch/config/log4j2.properties, with exit code 78
> ERROR: Missing logging config file at /usr/share/elasticsearch/config/log4j2.properties, with exit code 78
> ERROR: Missing logging config file at /usr/share/elasticsearch/config/log4j2.properties, with exit code 78

挂载卷覆盖问题。挂载的本地目录覆盖了容器内部的默认目录内容，导致配置文件无法正常加载或丢失。创建了挂载目录，直接挂载启动会出现该问题，因为挂载目录为空，没有配置文件等信息。

解决方案：先启动一个无挂载的es，将容器中的data、config、plugins文件夹cp到挂载目录，再挂载启动。

**其他问题：见原文[docker安装Elasticsearch8.x【保姆级教程】 - tans9508 - 博客园](https://www.cnblogs.com/tans9508/articles/18406380)**

# 9 gogs

```
docker run -d --name=gogs \
  -p 10022:22 \
  -p 3000:3000 \
  -v /data/gogs:/data \
  --restart=always \
  gogs/gogs
```

账户：root

密码：123456

## 9.1 问题备忘录

> 我在gogs里创建了一个仓库，其http地址显示为：“http://localhost:3000/xue-cheng-group1/xue-cheng1.git”，然而我的gogs部署在linux虚拟机里，通过192.168.150.102:3000可以访问到gogs管理页面，如果我想在windows本机把代码推到仓库，我是不是应该把“http://localhost:3000/xue-cheng-group1/xue-cheng1.git”写成：“http://192.168.150.102:3000/xue-cheng-group1/xue-cheng1.git”

**Gemini3-pro：是的，你的理解完全正确。**

如果你在 Windows 本机执行 `git push` 或 `git clone`，必须把 `localhost` 替换为虚拟机的 IP 地址 `192.168.150.102`。

因为在你的 Windows 电脑上，`localhost` 代表的是 Windows 自己，而不是 Linux 虚拟机。

---

### 💡 更好的解决方案：永久修改显示的地址

每次都要手动改地址很麻烦，你可以修改 Gogs 的配置文件，让它直接显示正确的 IP 地址。

假设你按照我之前提供的 Docker 方式安装，数据挂载在宿主机的 `./data` 目录下。

#### 1. 找到并编辑配置文件
你需要修改 `app.ini` 文件。
通常文件路径在：`你的挂载目录/gogs/conf/app.ini`。

例如：
```bash
# 进入你存放 gogs 数据的目录
cd /opt/gogs/data/gogs/conf/

# 编辑配置文件
vim app.ini
```

#### 2. 修改 `[server]` 部分
找到 `[server]` 这一节，修改 **DOMAIN** 和 **ROOT_URL** 这两行：

```ini
[server]
DOMAIN           = 192.168.150.102   <-- 改为你的IP
HTTP_PORT        = 3000
EXTERNAL_URL     = http://192.168.150.102:3000/  <-- 某些旧版本叫 ROOT_URL，改为 http://IP:端口/
DISABLE_SSH      = false
SSH_PORT         = 10022
START_SSH_SERVER = false
OFFLINE_MODE     = false
```

*注意：根据 Gogs 版本不同，配置项可能是 `ROOT_URL` 或者 `EXTERNAL_URL`，请将其中包含 `localhost` 的部分改为 `192.168.150.102`。*

#### 3. 重启 Gogs 容器
修改完保存退出，然后重启 Docker 容器让配置生效：

```bash
docker restart gogs
```

重启后，你再刷新 Gogs 的网页，仓库里的克隆地址就会自动变成正确的 `http://192.168.150.102:3000/...` 了，以后直接复制即可使用。

# 10 下载安装RabbitMQ

## 1.拉取最新版本（包含管理插件）

```bash
# 拉取带管理界面的版本（推荐）
docker pull rabbitmq:management

# 或者拉取指定版本（例如 3.12.x）
docker pull rabbitmq:3.12-management
```

## 2.拉取其他常用版本

```bash
# 不带管理界面的版本
docker pull rabbitmq:latest

# 指定版本带管理界面
docker pull rabbitmq:3.11-management-alpine
```

插件下载：[Releases · rabbitmq/rabbitmq-delayed-message-exchange](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases)

## 3.运行 RabbitMQ 容器

**基础运行命令（不采用）**

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin123 \
  --restart=always \
  rabbitmq:management
```

**参数说明：**

- `-d`: 后台运行容器
- `--name rabbitmq`: 容器名称
- `-p 5672:5672`: RabbitMQ 主端口（AMQP协议）
- `-p 15672:15672`: 管理界面端口
- `-e RABBITMQ_DEFAULT_USER=admin`: 设置默认用户名
- `-e RABBITMQ_DEFAULT_PASS=admin123`: 设置默认密码
- `--restart=always`: 容器自动重启

**高级配置（带数据持久化）（已采用）**

```bash
# 创建数据目录
mkdir -p /data/rabbitmq

# 运行容器并挂载数据卷
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -p 25672:25672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin123 \
  -e RABBITMQ_ERLANG_COOKIE='secret_cookie' \
  -v /data/rabbitmq/data:/var/lib/rabbitmq \
  -v /data/rabbitmq/logs:/var/log/rabbitmq \
  -v /data/rabbitmq/plugins:/plugins  \
  --restart=always \
  --hostname rabbitmq-host \
  rabbitmq:management
  
# 不要直接运行完整挂载代码，要先运行临时容器，把rabbitmq的核心组件复制出去，再删除临时容器，进行正式挂载部署
# 复制核心组件
docker cp rabbitmq-temp:/plugins/. /data/rabbitmq/plugins/
# 加入插件后设置权限
chmod 644 *.ez
```

**检查容器状态**

```bash
# 查看容器是否运行
docker ps | grep rabbitmq

# 查看容器日志
docker logs rabbitmq

# 进入容器启用插件（启用后需重启）
docker exec -it [rabbitmq] rabbitmq-plugins enable [rabbitmq_delayed_message_exchange]

# 查看插件安装列表
docker exec 容器名或ID rabbitmq-plugins list
```

**访问管理界面**

打开浏览器访问：

```
http://服务器IP:15672
```

使用设置的账号密码登录（默认：admin/admin123）

## 4. 常用管理命令

```bash
# 启动容器
docker start rabbitmq

# 停止容器
docker stop rabbitmq

# 重启容器
docker restart rabbitmq

# 查看所有容器（包括运行中和未运行）
docker ps -a

# 进入容器内部
docker exec -it rabbitmq bash

# 查看 RabbitMQ 状态（在容器内）
rabbitmqctl status

# 查看所有用户（在容器内）
rabbitmqctl list_users

# 列出当前 Docker 中的所有网络
docker network ls
# 查看网络详细信息
docker network inspect <network_name>
# 删除所有未使用的网络
docker network prune
# 删除一个或多个网络
docker network rm NETWORK [NETWORK...] 
```

## 5. Docker Compose 方式（推荐）

创建 `docker-compose.yml` 文件：

```yaml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:management
    container_name: rabbitmq
    restart: always
    ports:
      - "5672:5672"
      - "15672:15672"
      - "25672:25672"
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=admin123
      - RABBITMQ_ERLANG_COOKIE=secret_cookie
    volumes:
      - ./data:/var/lib/rabbitmq
      - ./logs:/var/log/rabbitmq
    hostname: rabbitmq-host
    networks:
      - rabbitmq-network

networks:
  rabbitmq-network:
    driver: bridge
```

运行：

```bash
# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

**注意事项**

1. **防火墙设置**：确保开放 5672 和 15672 端口
2. **密码安全**：生产环境使用强密码
3. **数据备份**：定期备份 `/var/lib/rabbitmq` 目录
4. **资源限制**：可添加内存限制参数 `-m 1g`

## 6 启动

启用管理插件（仅首次启动时需要）

```
# 启用Web管理界面
sudo rabbitmq-plugins enable rabbitmq_management

# 如果需要其他插件
# sudo rabbitmq-plugins enable rabbitmq_management_agent
# sudo rabbitmq-plugins enable rabbitmq_shovel
# sudo rabbitmq-plugins enable rabbitmq_shovel_management
```

启停选项

```
# 启用开机自启
sudo systemctl enable rabbitmq-server

# 启动服务
sudo systemctl start rabbitmq-server

# 查看状态
sudo systemctl status rabbitmq-server

# 停止服务
sudo systemctl stop rabbitmq-server

# 重启服务
sudo systemctl restart rabbitmq-server
```

创建管理员用户(要先开启服务)

```
sudo rabbitmqctl add_user admin admin123
sudo rabbitmqctl set_user_tags admin administrator
sudo rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```

目前已添加管理员用户：root 123456

IP地址：192.168.150.102:5672/192.168.150.102:15672（UI管理界面）

# 11 Nacos

```
打开浏览器访问：`http://你的服务器IP:8848/nacos`
默认用户名：`nacos`
默认密码：`nacos`
```



### **方法一：使用 Docker Run（单机模式 - 快速开始）**

**1. 拉取 Nacos 镜像**

```bash
docker pull nacos/nacos-server:v2.3.2
```

**2. 创建必要的目录（用于数据持久化）**

```bash
# 创建数据挂载目录
mkdir -p /data/nacos/{logs,data,conf}
# 赋予权限
chmod -R 777 /data/nacos
```

**3. 运行 Nacos 容器**

```bash
docker run -d \
  --name nacos \
  --restart=always \
  -p 8848:8848 \
  -p 9848:9848 \
  -p 9849:9849 \
  -e MODE=standalone \
  -v /data/nacos/logs:/home/nacos/logs \
  nacos/nacos-server:v2.3.2
```

### **方法二：使用 Docker Compose（推荐用于生产环境）**

**1. 创建 docker-compose.yml 文件**

```yaml
version: '3.8'
services:
  nacos:
    image: nacos/nacos-server:v2.3.2
    container_name: nacos
    restart: always
    environment:
      - MODE=standalone
      - PREFER_HOST_MODE=hostname
      - SPRING_DATASOURCE_PLATFORM=mysql
      - MYSQL_SERVICE_HOST=mysql
      - MYSQL_SERVICE_DB_NAME=nacos_config
      - MYSQL_SERVICE_PORT=3306
      - MYSQL_SERVICE_USER=nacos
      - MYSQL_SERVICE_PASSWORD=nacos
      - MYSQL_SERVICE_DB_PARAM=characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false
      - NACOS_AUTH_ENABLE=true
      - NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789
      - JVM_XMS=512m
      - JVM_XMX=512m
      - JVM_XMN=256m
    ports:
      - "8848:8848"
      - "9848:9848"
      - "9849:9849"
    volumes:
      - ./nacos/logs:/home/nacos/logs
      - ./nacos/data:/home/nacos/data
      - ./nacos/conf:/home/nacos/conf
    networks:
      - nacos_network
    depends_on:
      - mysql

  mysql:
    image: mysql:8.0
    container_name: nacos-mysql
    restart: always
    environment:
      - MYSQL_ROOT_PASSWORD=root123
      - MYSQL_DATABASE=nacos_config
      - MYSQL_USER=nacos
      - MYSQL_PASSWORD=nacos
    ports:
      - "3307:3306"
    volumes:
      - ./mysql/data:/var/lib/mysql
      - ./mysql/conf:/etc/mysql/conf.d
      - ./mysql/init:/docker-entrypoint-initdb.d
    command: [
      '--character-set-server=utf8mb4',
      '--collation-server=utf8mb4_unicode_ci',
      '--default-authentication-plugin=mysql_native_password'
    ]
    networks:
      - nacos_network

networks:
  nacos_network:
    driver: bridge
```

**2. 初始化数据库脚本**

下载 Nacos 的 MySQL 初始化脚本：
```bash
# 下载 Nacos MySQL 脚本
wget https://raw.githubusercontent.com/alibaba/nacos/develop/distribution/conf/mysql-schema.sql

# 或者使用 curl
curl -O https://raw.githubusercontent.com/alibaba/nacos/develop/distribution/conf/mysql-schema.sql

# 将脚本移动到 mysql 初始化目录
mkdir -p ./mysql/init
cp mysql-schema.sql ./mysql/init/
```

**3. 启动服务**

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f nacos

# 停止服务
docker-compose down
```

### **方法三：使用外部 MySQL（生产环境推荐）**

如果你已经有 MySQL 服务，可以使用以下配置：

```bash
# 首先在 MySQL 中创建数据库
mysql -h your-mysql-host -u root -p
CREATE DATABASE nacos_config CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nacos_config;
SOURCE /path/to/mysql-schema.sql;
```

然后运行容器：
```bash
docker run -d \
  --name nacos \
  --restart=always \
  -p 8848:8848 \
  -p 9848:9848 \
  -p 9849:9849 \
  -e MODE=standalone \
  -e SPRING_DATASOURCE_PLATFORM=mysql \
  -e MYSQL_SERVICE_HOST=你的MySQL主机IP \
  -e MYSQL_SERVICE_PORT=3306 \
  -e MYSQL_SERVICE_DB_NAME=nacos_config \
  -e MYSQL_SERVICE_USER=root \
  -e MYSQL_SERVICE_PASSWORD=你的密码 \
  -e MYSQL_SERVICE_DB_PARAM="characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false&serverTimezone=Asia/Shanghai" \
  -e NACOS_AUTH_ENABLE=true \
  -e NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789 \
  -v /docker/nacos/logs:/home/nacos/logs \
  -v /docker/nacos/conf:/home/nacos/conf \
  nacos/nacos-server:latest
```

#### 
**常用配置说明**

| 环境变量                   | 说明                         | 默认值   |
| -------------------------- | ---------------------------- | -------- |
| MODE                       | 运行模式：standalone/cluster | cluster  |
| SPRING_DATASOURCE_PLATFORM | 数据源类型                   | embedded |
| MYSQL_SERVICE_HOST         | MySQL 主机地址               | -        |
| MYSQL_SERVICE_DB_NAME      | MySQL 数据库名               | -        |
| NACOS_AUTH_ENABLE          | 是否开启认证                 | false    |
| JVM_XMS                    | 初始堆大小                   | 2g       |
| JVM_XMX                    | 最大堆大小                   | 2g       |

**注意事项**

1. **端口说明**：
   - 8848：主端口（HTTP）
   - 9848：客户端 gRPC 请求服务端端口
   - 9849：服务端 gRPC 请求服务端端口

2. **数据持久化**：务必挂载 volumes，否则重启后数据丢失

3. **内存设置**：根据服务器配置调整 JVM 参数

4. **防火墙设置**：
```bash
# 开放端口
firewall-cmd --permanent --add-port=8848/tcp
firewall-cmd --permanent --add-port=9848/tcp
firewall-cmd --reload
```

5. **集群部署**：如需集群模式，需要配置多个 Nacos 实例和 MySQL 主从

6. 如果docker中3306端口部署了mysql（没有创建网络直接部署），MYSQL_SERVICE_HOST要写本机真实IP地址。**创建容器网络时：**

   ```
   # 1. 创建自定义网络
   docker network create mynetwork
   
   # 2. 将已存在的 MySQL 连接到网络
   docker network connect mynetwork mysql容器名
   
   # 3. 查看 MySQL 在网络中的IP
   docker inspect mysql容器名 | grep IPAddress
   
   # 4. 启动 Nacos 并使用容器名访问
   docker run -d \
     --name nacos \
     --network mynetwork  # 使用相同网络
     -p 8848:8848 \
     -e MODE=standalone \
     -e SPRING_DATASOURCE_PLATFORM=mysql \
     -e MYSQL_SERVICE_HOST=mysql容器名  # 直接用容器名
     -e MYSQL_SERVICE_PORT=3306 \
     -e MYSQL_SERVICE_DB_NAME=nacos_config \
     -e MYSQL_SERVICE_USER=root \
     -e MYSQL_SERVICE_PASSWORD=123456 \
     nacos/nacos-server:latest
   ```

# 12 MySQL

**1. 拉取 MySQL 最新镜像**

```bash
# 拉取最新版 MySQL
docker pull mysql:latest

# 或拉取指定版本（推荐 8.0）
docker pull mysql:8.0
```

**2. 创建数据持久化目录**

```bash
# 创建本地目录用于数据持久化
mkdir -p /data/mysql/{data,logs,conf}

# 赋予权限
chmod -R 755 /data/mysql
```

**生产环境推荐（完整配置）：**

```bash
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -v /data/mysql/data:/var/lib/mysql \
  -v /data/mysql/conf:/etc/mysql/conf.d \
  -v /data/mysql/logs:/var/log/mysql \
  --restart=unless-stopped \
  mysql:8.0
```

**如果使用compose：**

创建 `docker-compose.yml`：

```bash
version: '3.8'

services:
  mysql:
    image: mysql:latest
    container_name: mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: nacos_config
      MYSQL_USER: nacos
      MYSQL_PASSWORD: nacos123
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - ./mysql/data:/var/lib/mysql
      - ./mysql/logs:/var/log/mysql
      - ./mysql/conf:/etc/mysql/conf.d
      - ./mysql/init:/docker-entrypoint-initdb.d
    command:
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --default-authentication-plugin=mysql_native_password
      --max_connections=1000
      --innodb_buffer_pool_size=1G
      --innodb_log_file_size=256M
      --slow_query_log=1
      --long_query_time=2
    networks:
      - app_network

networks:
  app_network:
    driver: bridge
```

启动：

```bash
# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f mysql

# 停止服务
docker-compose down
```

