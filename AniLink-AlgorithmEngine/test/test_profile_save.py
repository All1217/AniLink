# _*_ coding : utf-8 _*_
# 画像标签保存的并发/幂等测试：全部使用 fake 连接，不访问真实 MySQL。

import threading
import time
import unittest
from contextlib import contextmanager
from unittest.mock import Mock, patch
import src.util.profileBuilder as pb


class FakeCursor:
    def __init__(self, log, fail=False):
        self.log = log
        self.fail = fail

    def executemany(self, sql, values):
        self.log.append(("executemany", sql, [tuple(v) for v in values]))
        time.sleep(0.03)
        if self.fail:
            raise RuntimeError("simulated insert failure")

    def execute(self, sql, params=None):
        self.log.append(("execute", sql, params))
        time.sleep(0.01)
        if self.fail:
            raise RuntimeError("simulated execute failure")

    def close(self):
        self.log.append(("cursor_close",))


class FakeConn:
    def __init__(self, log, fail=False):
        self.log = log
        self.cur = FakeCursor(log, fail)
        self.autocommit_history = []

    def cursor(self):
        return self.cur

    def begin(self):
        self.log.append(("begin",))

    def commit(self):
        self.log.append(("commit",))

    def rollback(self):
        self.log.append(("rollback",))

    def autocommit(self, flag):
        self.autocommit_history.append(flag)

    def close(self):
        self.log.append(("conn_close",))


class FakeConnCtx:
    def __init__(self, conn):
        self.conn = conn

    def __enter__(self):
        return self.conn

    def __exit__(self, exc_type, exc, tb):
        self.conn.close()
        return False


class ProfileSaveTest(unittest.TestCase):
    def setUp(self):
        self.builder = pb.UserProfileBuilder(max_workers=1)

    @contextmanager
    def _patch_conn(self, log, fail=False):
        def factory():
            return FakeConnCtx(FakeConn(log, fail))

        @contextmanager
        def cursor_factory():
            yield FakeCursor(log, fail)

        with patch.object(pb, "mysql_conn", side_effect=factory, create=True), \
                patch.object(pb, "mysql_cursor", side_effect=lambda: cursor_factory()):
            yield

    def test_upsert_is_idempotent_and_cleans_stale_tags(self):
        log = []
        with self._patch_conn(log):
            self.builder.saveToDB(
                123,
                {
                    "interest_tags": {"A": 2.0, "B": 1.0},
                    "behavior_tags": None,
                    "quality_tags": None,
                },
            )

        sqls = [entry for entry in log if entry[0] == "executemany"]
        self.assertEqual(1, len(sqls))
        self.assertIn("ON DUPLICATE KEY UPDATE", sqls[0][1])
        self.assertEqual([(123, "A", 1.0), (123, "B", 0.5)], sqls[0][2])

        deletes = [entry for entry in log if entry[0] == "execute"]
        self.assertEqual(1, len(deletes))
        self.assertIn("NOT IN", deletes[0][1])
        self.assertEqual((123, "A", "B"), deletes[0][2])

        self.assertIn(("commit",), log)
        self.assertNotIn(("rollback",), log)

    def test_empty_tags_clear_all(self):
        log = []
        with self._patch_conn(log):
            self.builder.saveToDB(
                456,
                {"interest_tags": {}, "behavior_tags": None, "quality_tags": None},
            )

        self.assertFalse([entry for entry in log if entry[0] == "executemany"])
        deletes = [entry for entry in log if entry[0] == "execute"]
        self.assertEqual(1, len(deletes))
        self.assertIn("DELETE FROM user_tag WHERE uid = %s", deletes[0][1])
        self.assertEqual((456,), deletes[0][2])
        self.assertIn(("commit",), log)

    def test_failure_rolls_back(self):
        log = []
        with self._patch_conn(log, fail=True):
            with self.assertRaises(RuntimeError):
                self.builder.saveToDB(
                    789,
                    {"interest_tags": {"A": 1.0}, "behavior_tags": None, "quality_tags": None},
                )

        self.assertIn(("rollback",), log)
        self.assertNotIn(("commit",), log)

    def test_same_uid_is_serialized(self):
        log = []
        started = threading.Barrier(2)

        def worker(tag):
            started.wait()
            self.builder.saveToDB(
                999,
                {"interest_tags": {tag: 1.0}, "behavior_tags": None, "quality_tags": None},
            )

        with self._patch_conn(log):
            threads = [threading.Thread(target=worker, args=(tag,)) for tag in ("A", "B")]
            for thread in threads:
                thread.start()
            for thread in threads:
                thread.join()

        begins = [i for i, entry in enumerate(log) if entry[0] == "begin"]
        commits = [i for i, entry in enumerate(log) if entry[0] == "commit"]
        self.assertEqual(2, len(begins))
        self.assertEqual(2, len(commits))
        self.assertLess(commits[0], begins[1], "同一 uid 的保存发生了交错")

    def test_build_propagates_save_failure(self):
        self.builder._preloadUserData = lambda uid: {}
        self.builder.startInterestModule = lambda uid, preload: {"A": 1.0}
        self.builder.startBehaviorModule = lambda uid, preload: None
        self.builder.startQualityModule = lambda uid, preload: None
        self.builder.saveToDB = Mock(side_effect=RuntimeError("save failed"))

        with self.assertRaises(RuntimeError):
            self.builder.buildOneProfile(1)


if __name__ == "__main__":
    unittest.main()
