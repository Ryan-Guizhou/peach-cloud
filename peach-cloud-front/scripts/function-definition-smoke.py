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
FUNCTIONS = [dict(funcId="root", funcCode="ROOT", funcName="应用功能列表", appId="test-app"), dict(funcId="child", funcCode="FINANCE", funcName="财务处理", parentFuncCode="ROOT", appId="test-app")]
RESOURCES = [dict(resourceId="button", funcCode="FINANCE", resourceCode="finance:add", resourceName="建账向导", resourceType="BUTTON", appId="test-app")]
class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs): super().__init__(*args, directory=str(ROOT / "dist"), **kwargs)
    def log_message(self, *args): pass
    def do_GET(self):
        if not self.path.startswith("/assets/") and self.path != "/favicon.ico": self.path = "/index.html"
        super().do_GET()
    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers.get("Content-Length", 0))) or b"{}")
        data = None
        if self.path.endswith("/function/list"): data = FUNCTIONS
        elif self.path.endswith("/resource/pageList"):
            if body.get("keyword") == "failure":
                self.reply(dict(code="500", msg="资源查询失败，请重试")); return
            rows = [r for r in RESOURCES if r["funcCode"] == body.get("funcCode") and (not body.get("resourceType") or r["resourceType"] == body["resourceType"]) and (not body.get("keyword") or body["keyword"] in r["resourceCode"] + r["resourceName"])]
            data = dict(list=rows, total=len(rows))
        elif self.path.endswith("/resource/add"):
            RESOURCES.append(dict(body, resourceId="new-resource"))
        elif self.path.endswith("/resource/update"):
            next(r for r in RESOURCES if r["resourceId"] == body["resourceId"]).update(body)
        elif self.path.endswith("/function/add"):
            FUNCTIONS.append(dict(body, funcId="new-function"))
        self.reply(dict(code="200", msg="success", data=data))
    def do_DELETE(self):
        from urllib.parse import parse_qs, urlparse
        parsed = urlparse(self.path)
        if parsed.path.endswith("/resource/delById"):
            value = parse_qs(parsed.query)["resourceId"][0]
            RESOURCES[:] = [row for row in RESOURCES if row["resourceId"] != value]
        self.reply(dict(code="200", msg="success"))
    def reply(self, body):
        payload = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(200); self.send_header("Content-Type", "application/json; charset=utf-8"); self.send_header("Content-Length", str(len(payload))); self.end_headers(); self.wfile.write(payload)

server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
threading.Thread(target=server.serve_forever, daemon=True).start()
port = server.server_address[1]
chrome = os.environ.get("CHROME_PATH", "C:/Program Files/Google/Chrome/Application/chrome.exe")
artifacts = Path(tempfile.mkdtemp(prefix="peach-function-ui-"))
with tempfile.TemporaryDirectory(prefix="peach-function-chrome-") as profile:
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
        info = dict(session, permissionList=[kind + ":" + action for kind in ["function", "resource"] for action in ["view", "add", "update", "delete"]], menuList=[], routerList=[], roleList=[], userOrgList=[])
        script = "localStorage.setItem('peach.auth.session.v1'," + json.dumps(json.dumps(session)) + ");localStorage.setItem('peach.auth.loginInfo.v1'," + json.dumps(json.dumps(info)) + ")"
        cdp("Page.addScriptToEvaluateOnNewDocument", dict(source=script))
        cdp("Emulation.setDeviceMetricsOverride", dict(width=1600, height=1000, deviceScaleFactor=1, mobile=False))
        cdp("Page.navigate", dict(url=f"http://127.0.0.1:{port}/function"))
        wait("document.querySelectorAll('.function-label').length === 2")
        evaluate("Array.from(document.querySelectorAll('.function-label')).find(el => el.textContent.includes('FINANCE')).click()")
        wait("document.querySelector('.resource-panel').innerText.includes('finance:add')")
        (artifacts / "desktop.png").write_bytes(base64.b64decode(cdp("Page.captureScreenshot", dict(format="png"))["data"]))
        fill('input[aria-label="资源编码或名称"]', "nothing")
        click("查询"); wait("document.querySelector('.resource-panel').innerText.includes('暂无资源')")
        fill('input[aria-label="资源编码或名称"]', "failure")
        click("查询"); wait("document.querySelector('.ant-alert-message')?.innerText.includes('资源查询失败')")
        click("重置"); wait("document.querySelector('.resource-panel').innerText.includes('finance:add')")
        click("＋ 新增资源")
        wait("document.querySelector('.ant-modal input[id$=resourceCode]')")
        click("OK", "document.querySelector('.ant-modal')")
        wait("document.querySelector('.ant-form-item-explain-error')")
        fill('.ant-modal input[id$=resourceCode]', 'finance:export')
        fill('.ant-modal input[id$=resourceName]', '导出财务')
        click("OK", "document.querySelector('.ant-modal')")
        wait("document.querySelector('.resource-panel').innerText.includes('finance:export')")
        assert len(RESOURCES) == 2
        evaluate("Array.from(document.querySelectorAll('.resource-panel tr')).find(el => el.textContent.includes('finance:export')).querySelector('button').click()")
        wait("document.querySelector('.ant-modal input[id$=resourceName]')")
        fill('.ant-modal input[id$=resourceName]', '导出报表')
        click("OK", "document.querySelector('.ant-modal')")
        wait("document.querySelector('.resource-panel').innerText.includes('导出报表')")
        evaluate("Array.from(document.querySelectorAll('.resource-panel tr')).find(el => el.textContent.includes('finance:export')).querySelectorAll('button')[1].click()")
        wait("document.querySelector('.ant-popconfirm')")
        click("OK", "document.querySelector('.ant-popconfirm')")
        wait("!document.querySelector('.resource-panel').innerText.includes('finance:export')")
        assert len(RESOURCES) == 1
        click("新增根功能")
        wait("document.querySelector('.ant-modal input[id$=funcCode]')")
        fill('.ant-modal input[id$=funcCode]', 'REPORT')
        fill('.ant-modal input[id$=funcName]', '报表管理')
        fill('.ant-modal input[id$=appId]', 'test-app')
        click("OK", "document.querySelector('.ant-modal')")
        wait("document.querySelector('.function-panel').innerText.includes('REPORT')")
        cdp("Input.dispatchKeyEvent", dict(type="keyDown", key="Tab", code="Tab", windowsVirtualKeyCode=9))
        assert evaluate("document.activeElement.tagName !== 'BODY'")
        cdp("Emulation.setDeviceMetricsOverride", dict(width=390, height=844, deviceScaleFactor=1, mobile=True))
        time.sleep(3)
        (artifacts / "mobile.png").write_bytes(base64.b64decode(cdp("Page.captureScreenshot", dict(format="png", captureBeyondViewport=True))["data"]))
        assert evaluate("document.documentElement.scrollWidth <= window.innerWidth"), "Page overflows narrow viewport"
        print("PASS: tree selection, resource query/empty/error/recovery, modal validation, resource CRUD, function creation, keyboard and narrow viewport")
        print("Screenshots: " + str(artifacts))
    finally:
        if ws: ws.close()
        process.terminate(); process.wait(timeout=15)
        server.shutdown()
