# -*- coding: utf-8 -*-
"""CI 静态回归：不依赖浏览器，检查关键资产与内联 JS 语法。

完整 headless 运行时回归在本机 shots/p*_verify.py，CI 只跑这一份轻量版，
避免 runner 缺 Edge 导致构建失败。
"""
import io, os, re, subprocess, sys, tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IDX = os.path.join(ROOT, "app", "src", "main", "assets", "index.html")
SRC = io.open(IDX, encoding="utf-8").read()

CHECKS = [
    ("index exists", os.path.isfile(IDX)),
    ("single page shell", "<main id=\"view\">" in SRC),
    ("lazy data loader", "window.ensureData" in SRC and 'type="text/x-lazy"' in SRC),
    ("no sync heavy libs", '<script src="./data/valve.js"></script>' not in SRC
                           and '<script src="./data/scenes.js"></script>' not in SRC
                           and '<script src="./data/thinking.js"></script>' not in SRC),
    ("error page", os.path.isfile(os.path.join(ROOT, "app", "src", "main", "assets", "error.html"))),
    ("webview error hook", "onReceivedError" in io.open(
        os.path.join(ROOT, "app", "src", "main", "java", "com", "daily", "healthlife", "MainActivity.java"),
        encoding="utf-8", errors="ignore").read()),
    ("offline banner", 'id="netBanner"' in SRC and "addEventListener('offline'" in SRC),
    ("error log export", "window.dumpErrorLog" in SRC and "exportErrLog" in SRC),
    ("version visible", "window.p55AppVersion" in SRC and "getAppVersion" in io.open(
        os.path.join(ROOT, "app", "src", "main", "java", "com", "daily", "healthlife", "JsBridge.java"),
        encoding="utf-8", errors="ignore").read()),
    ("fixed keystore", os.path.isfile(os.path.join(ROOT, "debug.keystore"))),
    ("gradle wrapper", os.path.isfile(os.path.join(ROOT, "gradle", "wrapper", "gradle-wrapper.jar"))),
]

for name in ["decks.js", "valve.js", "exercise.js", "thinking.js", "scenes.js",
             "learn-extra.js", "exanim.js", "grow-shop.js"]:
    CHECKS.append(("data/" + name, os.path.isfile(os.path.join(ROOT, "app", "src", "main", "assets", "data", name))))

failed = []
for n, ok in CHECKS:
    print(("  PASS " if ok else "  FAIL ") + n)
    if not ok:
        failed.append(n)

js_blocks = re.findall(r"<script(?![^>]*src=)[^>]*>([\s\S]*?)</script>", SRC)
if js_blocks:
    js = max(js_blocks, key=len)
    fd, tmp = tempfile.mkstemp(suffix=".js")
    os.close(fd)
    io.open(tmp, "w", encoding="utf-8").write(js)
    try:
        r = subprocess.run(["node", "--check", tmp], capture_output=True)
        ok = r.returncode == 0
        print(("  PASS " if ok else "  FAIL ") + "inline js syntax")
        if not ok:
            failed.append("inline js syntax")
            print(r.stderr.decode("utf-8", "ignore")[:800])
    finally:
        os.remove(tmp)

print("\nCI VERIFY: %d checks, %d failed" % (len(CHECKS) + 1, len(failed)))
if failed:
    print("FAILED:", ", ".join(failed))
    sys.exit(1)
print("RESULT: ALL PASS")
