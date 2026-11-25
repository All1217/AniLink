# _*_ coding : utf-8 _*_
# 画像消息重试策略：纯函数，便于单测。

DEFAULT_MAX_RETRY = 3


def decide_retry(retry_count, max_retry=DEFAULT_MAX_RETRY):
    """返回 (action, next_retry)：action 为 retry 或 dead。"""
    try:
        count = int(retry_count)
    except (TypeError, ValueError):
        count = 0
    if count < 0:
        count = 0
    if count < max_retry:
        return "retry", count + 1
    return "dead", count
