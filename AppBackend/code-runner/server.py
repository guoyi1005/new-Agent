"""Minimal isolated execution service for the campus Python judge."""

import json
import os
import signal
import subprocess
try:
    import resource
except ImportError:
    resource = None
import tempfile
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

PORT = int(os.environ.get("CODE_JUDGE_PORT", "8099"))
TOKEN = os.environ.get("CODE_JUDGE_INTERNAL_TOKEN", "")
if not TOKEN:
    raise SystemExit("CODE_JUDGE_INTERNAL_TOKEN is required")
TIMEOUT_SECONDS = min(max(float(os.environ.get("CODE_JUDGE_TIMEOUT_SECONDS", "5")), 1), 10)
MAX_SCRIPT_BYTES = 100 * 1024
MAX_OUTPUT_BYTES = 64 * 1024
MAX_CONCURRENT = 2

_slots = threading.BoundedSemaphore(MAX_CONCURRENT)


class JudgeError(Exception):
    pass


LAUNCHER = """import os
import sys
try:
    import resource
except ImportError:
    resource = None

if resource is not None:
    _limits = (
        (resource.RLIMIT_CPU, 5, 5),
        (resource.RLIMIT_AS, 256 * 1024 * 1024, 256 * 1024 * 1024),
        (resource.RLIMIT_FSIZE, 1024 * 1024, 1024 * 1024),
        (resource.RLIMIT_NOFILE, 32, 32),
        (getattr(resource, "RLIMIT_NPROC", None), 32, 32),
    )
    for _limit, _soft, _hard in _limits:
        if _limit is None:
            continue
        try:
            resource.setrlimit(_limit, (_soft, _hard))
        except (ValueError, OSError):
            pass

os.environ.clear()
_path = sys.argv[1]
with open(_path, "r", encoding="utf-8") as _file:
    _source = _file.read()
exec(compile(_source, _path, "exec"), {"__name__": "__main__", "__file__": _path})
"""


def _read_limited(path: Path) -> str:
    data = path.read_bytes() if path.exists() else b""
    if len(data) > MAX_OUTPUT_BYTES:
        data = data[:MAX_OUTPUT_BYTES] + b"\n[output truncated]"
    return data.decode("utf-8", errors="replace")


def _execute(script: str):
    work = Path(tempfile.mkdtemp(prefix="judge-", dir="/tmp"))
    script_path = work / "main.py"
    launcher_path = work / "launcher.py"
    stdout_path = work / "stdout.txt"
    stderr_path = work / "stderr.txt"
    script_path.write_text(script, encoding="utf-8")
    launcher_path.write_text(LAUNCHER, encoding="utf-8")
    env = {
        "PATH": os.environ.get("PATH", "/usr/local/bin:/usr/bin:/bin"),
        "HOME": str(work),
        "TMPDIR": str(work),
        "PYTHONIOENCODING": "utf-8",
        "PYTHONDONTWRITEBYTECODE": "1",
    }
    timed_out = False
    exit_code = None
    with stdout_path.open("wb") as stdout_file, stderr_path.open("wb") as stderr_file:
        process = subprocess.Popen(
            ["python", "-I", "-B", str(launcher_path), str(script_path)],
            cwd=work,
            env=env,
            stdin=subprocess.DEVNULL,
            stdout=stdout_file,
            stderr=stderr_file,
            start_new_session=True,
        )
        try:
            exit_code = process.wait(timeout=TIMEOUT_SECONDS)
        except subprocess.TimeoutExpired:
            timed_out = True
            try:
                if hasattr(os, "killpg"):
                    os.killpg(process.pid, signal.SIGKILL)
                else:
                    process.kill()
            except ProcessLookupError:
                pass
            process.wait(timeout=2)
            exit_code = -9
    stdout = _read_limited(stdout_path)
    stderr = _read_limited(stderr_path)
    for child in work.iterdir():
        try:
            child.unlink()
        except OSError:
            pass
    try:
        work.rmdir()
    except OSError:
        pass
    return {
        "stdout": stdout,
        "stderr": stderr,
        "timedOut": timed_out,
        "exitCode": exit_code,
    }


class Handler(BaseHTTPRequestHandler):
    server_version = "CampusCodeJudge/1.0"

    def log_message(self, fmt, *args):
        return

    def _send_json(self, status, payload):
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if urlparse(self.path).path == "/health":
            self._send_json(200, {"status": "ok"})
            return
        self._send_json(404, {"message": "not found"})

    def do_POST(self):
        if urlparse(self.path).path != "/execute":
            self._send_json(404, {"message": "not found"})
            return
        if TOKEN and self.headers.get("X-Code-Judge-Token") != TOKEN:
            self._send_json(401, {"message": "unauthorized"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_SCRIPT_BYTES:
                raise JudgeError("invalid request size")
            payload = json.loads(self.rfile.read(length))
            script = payload.get("script")
            if not isinstance(script, str) or not script.strip():
                raise JudgeError("script is required")
            if len(script.encode("utf-8")) > MAX_SCRIPT_BYTES:
                raise JudgeError("script is too large")
            if not _slots.acquire(timeout=1):
                self._send_json(429, {"message": "judge busy"})
                return
            try:
                result = _execute(script)
            finally:
                _slots.release()
            self._send_json(200, result)
        except JudgeError as exc:
            self._send_json(400, {"message": str(exc)})
        except Exception:
            self._send_json(500, {"message": "judge failed"})


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
