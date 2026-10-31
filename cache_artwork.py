from pathlib import Path
import json, hashlib, urllib.request, concurrent.futures

root = Path(__file__).parent / 'app/src/main/assets'
data = json.loads((root / 'catalog.json').read_text(encoding='utf-8'))
urls = set()
def collect(x):
    if isinstance(x, list):
        for v in x: collect(v)
    elif isinstance(x, dict):
        for k,v in x.items():
            if k.endswith('Url') and isinstance(v, str): urls.add(v)
            else: collect(v)
collect(data)
(root/'artwork').mkdir(exist_ok=True)
def fetch(url):
    name = hashlib.sha256(url.encode()).hexdigest()[:20] + '.jpg'
    dest = root/'artwork'/name
    try:
        if not dest.exists():
            req = urllib.request.Request(url, headers={'User-Agent':'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=25) as r:
                content = r.read()
                if not r.headers.get('Content-Type','').startswith('image/'): raise ValueError('Not an image')
            dest.write_bytes(content)
        return url, 'file:///android_asset/artwork/'+name
    except Exception as e:
        return url, str(e)
results = dict(concurrent.futures.ThreadPoolExecutor(max_workers=4).map(fetch, sorted(urls)))
success = {k:v for k,v in results.items() if v.startswith('file:///')}
def replace(x):
    if isinstance(x,list): return [replace(v) for v in x]
    if isinstance(x,dict): return {k:replace(v) for k,v in x.items()}
    return success.get(x,x) if isinstance(x,str) else x
(root/'catalog.json').write_text(json.dumps(replace(data),ensure_ascii=False,indent=2),encoding='utf-8')
(root/'artwork-provenance.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
print(f'Cached {len(success)}/{len(urls)} exact original artwork URLs. No substitute artwork generated.')
for url,result in results.items():
    if url not in success: print(result, url)
