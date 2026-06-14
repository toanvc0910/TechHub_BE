import json,sys
raw=open(sys.argv[1],encoding='utf-8',errors='replace').read()
best=None
for ln in raw.splitlines():
    ln=ln.strip()
    if ln.startswith('data:'): ln=ln[5:].strip()
    if not ln.startswith('{'): continue
    try: o=json.loads(ln)
    except: continue
    if best is None or len(ln)>best[0]: best=(len(ln),o)
o=best[1] if best else {}
def find(d,k):
    if isinstance(d,dict):
        if k in d: return d[k]
        for v in d.values():
            r=find(v,k)
            if r is not None: return r
    elif isinstance(d,list):
        for v in d:
            r=find(v,k)
            if r is not None: return r
    return None
print('metric :',find(o,'metric'))
rows=find(o,'rows')
print('rows   :',len(rows) if isinstance(rows,list) else rows)
print('summary:',find(o,'summary'))
