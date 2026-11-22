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
def key(function, kind, code): return ''.join(str(len(value)) + ':' + value for value in [function,kind,code])
FUNCTIONS=[dict(funcId='root',funcCode='ROOT',funcName='应用功能列表',funcType='CATALOG',appId='app'),dict(funcId='user',funcCode='USER',funcName='用户管理',parentFuncCode='ROOT',funcType='MENU',appId='app'),dict(funcId='report',funcCode='REPORT',funcName='报表管理',parentFuncCode='ROOT',funcType='MENU',appId='app')]
RESOURCES=[dict(resourceId='user-read',funcCode='USER',resourceType='BUTTON',resourceCode='user:read',resourceName='查看用户',appId='app'),dict(resourceId='report-read',funcCode='REPORT',resourceType='BUTTON',resourceCode='report:read',resourceName='查看报表',appId='app')]
GRANTS=[dict(funcCode='USER',opType='BUTTON',resourceCode='user:read',resourceName='查看用户'),dict(funcCode='REPORT',opType='BUTTON',resourceCode='report:read',resourceName='查看报表'),dict(funcCode='REPORT',opType='BUTTON',resourceCode='deleted',resourceName='失效资源')]
ROLES=[dict(roleId='a',roleCode='ROLE_A',roleName='示例管理员',roleScope='ORG',roleType='CUSTOM',skipUrl=''),dict(roleId='b',roleCode='ROLE_B',roleName='示例查看者',roleScope='ORG',roleType='CUSTOM',skipUrl='')]
STATE={ident:dict(funcCodes=['USER','REPORT'],grants=list(GRANTS),revision=0,members=['alice']) for ident in ['a','b']}
ROUTES=[dict(routerId='router-a',moduleCode='system',routerCode='ROLE_ROUTE',routerName='角色页面',routerUrl='/role',filePath='system/role',routerLevel=2,funcCode='USER',isAuth=1,isCache=0)]
LAST_ROUTE_QUERY={}
class Handler(SimpleHTTPRequestHandler):
    def __init__(self,*args,**kwargs): super().__init__(*args,directory=str(ROOT/'dist'),**kwargs)
    def log_message(self,*args): pass
    def do_GET(self):
        from urllib.parse import parse_qs,urlparse
        url=urlparse(self.path)
        if url.path.endswith('/role/workspace'):
            ident=parse_qs(url.query)['roleId'][0]; role=next(r for r in ROLES if r['roleId']==ident); state=STATE[ident]
            menus=[dict(menuId='report-menu',menuName='报表',menuUrl='/report')] if 'REPORT' in state['funcCodes'] else []
            self.reply(dict(code='200',data=dict(role=role,functions=FUNCTIONS,resources=RESOURCES,funcCodes=state['funcCodes'],resourceGrants=state['grants'],menus=menus,revision=str(state['revision']))));return
        if url.path.endswith('/role/members'):
            ident=parse_qs(url.query)['roleId'][0]
            self.reply(dict(code='200',data=dict(users=[dict(userCode='alice',userName='Alice'),dict(userCode='bob',userName='Bob')],userCodes=STATE[ident]['members'],revision='members')));return
        if not self.path.startswith('/assets/'):self.path='/index.html'
        super().do_GET()
    def do_POST(self):
        global LAST_ROUTE_QUERY
        body=json.loads(self.rfile.read(int(self.headers.get('Content-Length',0))) or b'{}');data=None
        if self.path.endswith('/role/pageList'):
            if body.get('keyword')=='failure': self.reply(dict(code='500',msg='角色查询失败'));return
            found=[r for r in ROLES if body.get('keyword','') in r['roleCode']+r['roleName']];data=dict(list=found,total=len(found))
        elif self.path.endswith('/function/list'):data=FUNCTIONS
        elif self.path.endswith('/role/saveGrants'):
            state=STATE[body['roleId']]; state['funcCodes']=body['funcCodes'];state['grants']=[g for g in GRANTS if key(g['funcCode'],g['opType'],g['resourceCode']) in body['resourceKeys']];state['revision']+=1
        elif self.path.endswith('/role/calibrate'):
            allowed={key(r['funcCode'],r['resourceType'],r['resourceCode']) for r in RESOURCES if r['funcCode'] in body['funcCodes']}
            data=dict(removedFuncCodes=[],removedResourceKeys=[k for k in body['resourceKeys'] if k not in allowed])
        elif self.path.endswith('/role/saveMembers'):STATE[body['roleId']]['members']=body['userCodes']
        elif self.path.endswith('/role/saveDefault'):
            next(r for r in ROLES if r['roleId']==body['roleId'])['skipUrl']=body['skipUrl'];STATE[body['roleId']]['revision']+=1
        elif self.path.endswith('/role/update'):next(r for r in ROLES if r['roleId']==body['roleId']).update(body)
        elif self.path.endswith('/router/pageList'):
            LAST_ROUTE_QUERY=body
            found=[r for r in ROUTES if all(body.get(field,'') in r[field] for field in ['moduleCode','routerCode','routerUrl'])];data=dict(list=found,total=len(found))
        elif self.path.endswith('/router/update'):
            assert 'isAuth' not in body and 'isCache' not in body
            next(r for r in ROUTES if r['routerId']==body['routerId']).update(body)
        self.reply(dict(code='200',data=data))
    def reply(self,body):
        payload=json.dumps(body,ensure_ascii=False).encode();self.send_response(200);self.send_header('Content-Type','application/json; charset=utf-8');self.send_header('Content-Length',str(len(payload)));self.end_headers();self.wfile.write(payload)

server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
threading.Thread(target=server.serve_forever, daemon=True).start()
port = server.server_address[1]
chrome = os.environ.get("CHROME_PATH", "C:/Program Files/Google/Chrome/Application/chrome.exe")
artifacts = Path(tempfile.mkdtemp(prefix="peach-admin-ui-"))
with tempfile.TemporaryDirectory(prefix="peach-admin-chrome-") as profile:
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
        info=dict(session,permissionList=['role:view','role:add','role:update','role:delete','router:view','router:add','router:update','authorization:update'],menuList=[],routerList=[],roleList=[],userOrgList=[])
        script = "localStorage.setItem('peach.auth.session.v1'," + json.dumps(json.dumps(session)) + ");localStorage.setItem('peach.auth.loginInfo.v1'," + json.dumps(json.dumps(info)) + ")"
        cdp("Page.addScriptToEvaluateOnNewDocument", dict(source=script))
        cdp("Emulation.setDeviceMetricsOverride", dict(width=1600, height=1000, deviceScaleFactor=1, mobile=False))
        cdp("Page.navigate",dict(url=f"http://127.0.0.1:{port}/role"))
        wait("document.querySelector('.resource-grant-panel')?.innerText.includes('user:read')")
        (artifacts/'role-desktop.png').write_bytes(base64.b64decode(cdp('Page.captureScreenshot',dict(format='png'))['data']))
        fill('input[aria-label="功能关键词"]','用户')
        wait("!document.querySelector('.function-grant-panel').innerText.includes('REPORT')")
        evaluate("Array.from(document.querySelectorAll('.function-grant-panel .ant-tree-treenode')).find(el => el.textContent.includes('USER')).querySelector('.ant-tree-checkbox').click()")
        click('授权')
        wait("document.querySelector('.function-grant-panel').innerText.includes('示例管理员') && !document.querySelector('.grant-note').innerText.includes('未保存')")
        assert STATE['a']['funcCodes']==['REPORT'],STATE['a']
        assert any(g['resourceCode']=='report:read' for g in STATE['a']['grants'])
        assert not any(g['funcCode']=='USER' for g in STATE['a']['grants'])
        click('自动校准');wait("document.body.innerText.includes('授权校准预览')")
        assert any(g['resourceCode']=='deleted' for g in STATE['a']['grants'])
        click('应用到草稿');time.sleep(.3)
        assert any(g['resourceCode']=='deleted' for g in STATE['a']['grants'])
        click('授权');wait("!document.querySelector('.function-grant-panel').innerText.includes('有未保存的授权')")
        assert not any(g['resourceCode']=='deleted' for g in STATE['a']['grants'])
        def role_action(label):
            evaluate("Array.from(document.querySelectorAll('.role-table tr')).find(el => el.textContent.includes('ROLE_A')).querySelectorAll('button') && Array.from(Array.from(document.querySelectorAll('.role-table tr')).find(el => el.textContent.includes('ROLE_A')).querySelectorAll('button')).find(el => el.textContent.replace(/\\s/g,'') === "+json.dumps(label)+").click()")
        role_action('用户');wait("document.querySelector('.member-list')?.innerText.includes('Alice')")
        fill('input[aria-label="筛选角色用户"]','Bob')
        wait("document.querySelector('.member-list').innerText.includes('Bob') && !document.querySelector('.member-list').innerText.includes('Alice')")
        evaluate("document.querySelector('.member-list input').click()")
        click('OK');wait("document.body.innerText.includes('角色成员已保存')")
        assert set(STATE['a']['members'])=={'alice','bob'}
        time.sleep(.3)
        role_action('默认菜单');wait("document.querySelector('input[aria-label=\"角色默认菜单\"]') || document.body.innerText.includes('仅单角色登录时生效')")
        evaluate("Array.from(document.querySelectorAll('.ant-modal .ant-select-selector')).find(el => el.getClientRects().length).dispatchEvent(new MouseEvent('mousedown',{bubbles:true}))")
        wait("Array.from(document.querySelectorAll('.ant-select-item-option-content')).some(el => el.textContent.includes('/report'))")
        evaluate("Array.from(document.querySelectorAll('.ant-select-item-option-content')).find(el => el.textContent.includes('/report')).click()")
        click('OK');wait("document.body.innerText.includes('默认菜单已保存')")
        assert next(r for r in ROLES if r['roleId']=='a')['skipUrl']=='/report'
        time.sleep(.3)
        role_action('编辑');wait("document.querySelector('.ant-modal input[id$=roleName]')")
        assert evaluate("document.querySelector('.ant-modal input[id$=roleCode]').disabled")
        fill('.ant-modal input[id$=roleName]','修改后的角色')
        click('OK');wait("document.querySelector('.role-table').innerText.includes('修改后的角色')")
        # Query all three router filters, edit a row, and keep the hidden flags unchanged.
        cdp('Page.navigate',dict(url=f'http://127.0.0.1:{port}/router'))
        wait("document.querySelector('.route-filters') && document.querySelector('table').innerText.includes('ROLE_ROUTE')")
        assert not evaluate("document.querySelector('thead').innerText.includes('是否缓存') || document.querySelector('thead').innerText.includes('是否需要授权')")
        fill('input[id$=moduleCode]','sys');fill('input[id$=routerCode]','ROLE');fill('input[id$=routerUrl]','role')
        click('查询');wait("document.querySelector('table').innerText.includes('ROLE_ROUTE')")
        time.sleep(.2)
        assert LAST_ROUTE_QUERY['moduleCode']=='sys' and LAST_ROUTE_QUERY['routerCode']=='ROLE' and LAST_ROUTE_QUERY['routerUrl']=='role'
        fill('input[id$=routerUrl]','missing');click('查询');wait("document.querySelector('table').innerText.includes('暂无匹配路由')")
        click('重置');wait("document.querySelector('table').innerText.includes('ROLE_ROUTE')")
        click('修改');wait("document.querySelector('.ant-modal input[id$=routerName]')")
        fill('.ant-modal input[id$=routerName]','已修改路由')
        click('OK');wait("document.querySelector('table').innerText.includes('已修改路由')")
        assert ROUTES[0]['isAuth']==1 and ROUTES[0]['isCache']==0
        time.sleep(.5)
        (artifacts/'router-desktop.png').write_bytes(base64.b64decode(cdp('Page.captureScreenshot',dict(format='png'))['data']))
        cdp('Emulation.setDeviceMetricsOverride',dict(width=390,height=844,deviceScaleFactor=1,mobile=True));time.sleep(.5)
        assert evaluate("document.querySelector('.admin-page-card').getBoundingClientRect().width > 300")
        (artifacts/'router-mobile.png').write_bytes(base64.b64decode(cdp('Page.captureScreenshot',dict(format='png',captureBeyondViewport=True))['data']))
        print('PASS: role filter-safe grants, calibration preview/save, filtered membership preservation, default menu, metadata edit, router combined filters/edit/hidden flags and narrow layout')
        print('Screenshots: '+str(artifacts))
    finally:
        if ws: ws.close()
        process.terminate(); process.wait(timeout=15)
        server.shutdown()
