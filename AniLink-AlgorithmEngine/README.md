# AniLink-用户画像引擎

基于Flask框架构建的用户画像服务，与Java后端服务主要的通信方式为：

1. 共享Redis缓存
2. 收发RabbitMQ消息
3. 通过符合RESTful api规范的接口调用服务

# 1 环境配置

推荐克隆main分支，其他分支均为测试。

克隆：`git clone -b main https://github.com/All1217/AniLink-AlgorithmEngine.git`

## 1.1 如果没有anaconda

进入根目录，创建虚拟环境`python -m venv .venv`

激活虚拟环境（Windows，并且你的python版本要大于等于3.3）：`.venv\Scripts\activate`

看到 (.venv) 提示符后安装依赖：`pip install -r requirements.txt`

## 1.2 如果有anaconda

酌情选用你的anaconda环境即可

# 2 启动

## 2.1 双击启动（Windows，推荐）

| 脚本 | 作用 |
| --- | --- |
| `scripts\start.bat` | 启动服务。自动挑选已装好依赖的 Python（优先 `.venv`，其次 `anaconda3` / `miniconda3` / 当前 conda 环境，最后 PATH 上的 `python`）；端口 5000 被占用时会提示先 `stop.bat` |
| `scripts\stop.bat` | 结束占用 5000 端口的进程 |
| `scripts\install.bat` | 首次使用：在引擎目录创建 `.venv` 并执行 `pip install -r requirements.txt` |

启动脚本会自行设置 `PYTHONPATH=<引擎根>;<引擎根>\src`（代码里同时存在 `from server.app import ...` 与 `from src.xxx import ...`，两者都需要）以及 `PYTHONIOENCODING=utf-8`（代码里有 emoji 日志，GBK 控制台会直接抛 `UnicodeEncodeError`）。

启动前请确认 MySQL / Redis / RabbitMQ / Elasticsearch 已运行；未设置 `DASHSCOPE_API_KEY` 时服务仍可启动，但向量化/推荐相关功能会在调用时报错。

## 2.2 手动启动

在**引擎根目录**执行（`src/main.py` 依赖根目录在 `PYTHONPATH` 中）：

```powershell
$env:PYTHONPATH = "$PWD;$PWD\src"
$env:PYTHONIOENCODING = "utf-8"
python src\main.py
```

也可以继续使用 IDE：找到 `src/main.py`，右击运行（IDE 会把项目根加入 PYTHONPATH）。

