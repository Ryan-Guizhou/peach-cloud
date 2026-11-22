"""Browser smoke against local API fixtures; requires Chrome and websocket-client."""
import base64
import json
import os
from pathlib import Path
import subprocess
import tempfile
import threading
import time
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.request import urlopen
import websocket

ROOT = Path(__file__).resolve().parents[1]
LOGOUT_CALLS = []
LOGS = [dict(logId="known", occurredAt="2026-11-22 20:00:00.000", eventType="LOGIN", result="SUCCESS", reasonCode="OK", account="au***t", tenantId="test", orgId="test", userId="test", ipAddress="192.0.2.10", browser="Chrome / Windows", durationMs=32, traceId="0123456789abcdef0123456789abcdef"), dict(logId="unknown", occurredAt="2026-11-22 20:01:00.000", eventType="LOGIN", result="FAILED", reasonCode="BAD_CREDENTIALS", account="gu***t", tenantId=None, ipAddress="198.51.100.20", browser="Other", durationMs=8, traceId="1123456789abcdef0123456789abcdef")]
class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs): super().__init__(*args, directory=str(ROOT / "dist"), **kwargs)
    def log_message(self, *args): pass
    def do_GET(self):
        if self.path.startswith("/api/auth/loginLog/selectById"):
            from urllib.parse import parse_qs, urlparse
            ident = parse_qs(urlparse(self.path).query)["logId"][0]
            self.reply(dict(code="200", data=next(row for row in LOGS if row["logId"] == ident))); return
        if not self.path.startswith("/assets/") and self.path != "/favicon.ico": self.path = "/index.html"
        super().do_GET()
    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers.get("Content-Length", 0))) or b"{}")
        if self.path.endswith("/loginLog/pageList"):
            if body.get("account") == "failure": self.reply(dict(code="500", msg="查询失败，请重试")); return
            unassigned = body.get("scope") == "UNASSIGNED"
            rows = [row for row in LOGS if (row["tenantId"] is None) == unassigned and (not body.get("account") or body["account"] == "audit-account")]
            self.reply(dict(code="200", data=dict(list=rows, total=len(rows)))); return
        if self.path.endswith("/logout"): LOGOUT_CALLS.append(True)
        self.reply(dict(code="200", data=None))
    def reply(self, body):
        payload = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(200); self.send_header("Content-Type", "application/json; charset=utf-8"); self.send_header("Content-Length", str(len(payload))); self.end_headers(); self.wfile.write(payload)

server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
threading.Thread(target=server.serve_forever, daemon=True).start()
port = server.server_address[1]
chrome = os.environ.get("CHROME_PATH", "C:/Program Files/Google/Chrome/Application/chrome.exe")
artifacts = Path(tempfile.mkdtemp(prefix="peach-login-audit-ui-"))
with tempfile.TemporaryDirectory(prefix="peach-login-audit-chrome-") as profile:
    process = subprocess.Popen([chrome, "--headless=new", "--disable-gpu", "--in-process-gpu", "--no-sandbox", "--disable-software-rasterizer", "--no-first-run", "--no-default-browser-check", "--remote-allow-origins=*", "--remote-debugging-port=0", "--user-data-dir=" + profile, "about:blank"], stdout=subprocess.DEVNULL, stderr=(artifacts / "chrome.log").open("w"), creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    ws = None
    try:
        active = Path(profile) / "DevToolsActivePort"
        for _ in range(100):
            if active.exists(): break
            time.sleep(.1)
        debug_port = active.read_text().splitlines()[0]
        tabs = json.load(urlopen("http://127.0.0.1:" + debug_port + "/json"))
        time.sleep(1)
        ws = websocket.create_connection(next(t["webSocketDebuggerUrl"] for t in tabs if t["type"] == "page"), timeout=15, http_proxy_host=None)
        sequence = 0
        def cdp(method, params=None):
            global sequence
            sequence += 1; ident = sequence
            ws.send(json.dumps(dict(id=ident, method=method, params=params or {})))
            while True:
                result = json.loads(ws.recv())
                if result.get("id") == ident:
                    if "error" in result: raise RuntimeError(result["error"])
                    return result.get("result", {})
        def evaluate(expression):
            result = cdp("Runtime.evaluate", dict(expression=expression, returnByValue=True, awaitPromise=True))
            if "exceptionDetails" in result: raise AssertionError(str(result["exceptionDetails"]))
            return result.get("result", {}).get("value")
        def wait(expression):
            for _ in range(100):
                if evaluate(expression): return
                time.sleep(.1)
            raise AssertionError("Timed out: " + expression + "\n" + str(evaluate("document.body.innerText")))
        def click(text, scope="document"):
            evaluate("(() => { const el = Array.from(" + scope + ".querySelectorAll('button')).find(el => el.textContent.replace(/\\s/g, '') === " + json.dumps(text.replace(" ", "")) + " && el.getClientRects().length); if(!el) throw Error('Button missing'); el.click(); })()")
        def fill(selector, value):
            evaluate("(() => { const el = document.querySelector(" + json.dumps(selector) + "); if(!el) throw Error('Input missing'); Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(el, " + json.dumps(value) + "); el.dispatchEvent(new Event('input', {bubbles:true})); el.dispatchEvent(new Event('change', {bubbles:true})); })()")
        cdp("Page.enable")
        session = dict(token="local-fixture", userId="test", userName="界面测试", tenantId="test", orgId="test", fiscal="2026", expiresAt=int(time.time()*1000)+3600000)
        info = dict(session, permissionList=["loginLog:view", "loginLog:platform", "tenant:view"], menuList=[dict(menuId="settings", menuName="系统设置", isLeaf=0), dict(menuId="audit", menuName="日志审计", isLeaf=0), dict(menuId="tenant", menuName="租户管理", menuCode="tenant", menuUrl="/tenant", parentMenuId="settings", isLeaf=1), dict(menuId="logs", menuName="登录日志", menuCode="loginLog", menuUrl="/login-log", parentMenuId="audit", isLeaf=1)], routerList=[dict(routerCode="SYS_TENANT_MGMT", routerName="租户管理", routerUrl="/tenant", filePath="system/tenant"), dict(routerCode="SYS_LOGIN_LOG", routerName="登录日志", routerUrl="/login-log", filePath="log/login")], roleList=[], userOrgList=[])
        script = "localStorage.setItem('peach.auth.session.v1'," + json.dumps(json.dumps(session)) + ");localStorage.setItem('peach.auth.loginInfo.v1'," + json.dumps(json.dumps(info)) + ")"
        cdp("Page.addScriptToEvaluateOnNewDocument", dict(source=script))
        cdp("Emulation.setDeviceMetricsOverride", dict(width=1600, height=1000, deviceScaleFactor=1, mobile=False))
        cdp("Page.navigate", dict(url=f"http://127.0.0.1:{port}/login-log"))
        wait("document.querySelector('.login-log-page')?.innerText.includes('192.0.2.10')")
        assert evaluate("document.querySelector('.console-nav').innerText.includes('日志审计')")
        assert not evaluate("document.querySelector('.console-nav').innerText.includes('应用功能列表')")
        (artifacts / "desktop.png").write_bytes(base64.b64decode(cdp("Page.captureScreenshot", dict(format="png"))["data"]))
        fill('input[id$=account]', 'missing')
        click("查询"); wait("document.querySelector('.login-log-page').innerText.includes('当前范围内暂无日志')")
        fill('input[id$=account]', 'failure')
        click("查询"); wait("document.querySelector('.ant-alert-error')?.innerText.includes('查询失败')")
        click("重置"); wait("document.querySelector('.login-log-page').innerText.includes('192.0.2.10')")
        click("详情")
        wait("document.querySelector('.ant-drawer')?.innerText.includes('0123456789abcdef0123456789abcdef')")
        evaluate("document.querySelector('.ant-drawer-close').click()")
        evaluate("document.querySelector('input[id$=scope]').closest('.ant-select').querySelector('.ant-select-selector').dispatchEvent(new MouseEvent('mousedown', {bubbles:true}))")
        wait("Array.from(document.querySelectorAll('.ant-select-item-option-content')).some(el => el.textContent.includes('未归属事件'))")
        evaluate("Array.from(document.querySelectorAll('.ant-select-item-option-content')).find(el => el.textContent.includes('未归属事件')).click()")
        click("查询"); wait("document.querySelector('.login-log-page').innerText.includes('198.51.100.20')")
        cdp("Input.dispatchKeyEvent", dict(type="keyDown", key="Tab", code="Tab", windowsVirtualKeyCode=9))
        assert evaluate("document.activeElement.tagName !== 'BODY'")
        cdp("Emulation.setDeviceMetricsOverride", dict(width=390, height=844, deviceScaleFactor=1, mobile=True))
        time.sleep(.5)
        assert evaluate("document.querySelector('.login-log-page').getBoundingClientRect().width > 300")
        click("详情")
        wait("document.querySelector('.ant-drawer')?.innerText.includes('未归属')")
        time.sleep(.5)
        assert evaluate("document.querySelector('.ant-drawer-content-wrapper').getBoundingClientRect().width <= window.innerWidth")
        (artifacts / "mobile.png").write_bytes(base64.b64decode(cdp("Page.captureScreenshot", dict(format="png", captureBeyondViewport=True))["data"]))
        evaluate("document.querySelector('.ant-drawer-close').click()")
        click("退出")
        wait("localStorage.getItem('peach.auth.session.v1') === null")
        assert LOGOUT_CALLS, "Logout did not reach server"
        print("PASS: backend menu grouping, query/empty/error/recovery, log detail, platform scope/select, keyboard, narrow drawer and server logout")
        print("Screenshots: " + str(artifacts))
    finally:
        if ws: ws.close()
        process.terminate(); process.wait(timeout=15)
        server.shutdown()
