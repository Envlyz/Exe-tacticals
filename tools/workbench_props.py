# Расстановка деталей на столе оружейного верстака + проверка, что они не заходят в след оружия
# (GunWorkbenchRenderer.FOOT_HALF_LEN/H, DIAGONAL, CENTER_Z). Запуск из src/main/resources/assets/exe_tacticals/models:
#   python ../../../../../../tools/workbench_props.py
import json, math, sys
T=15.5
FULL=[0,0,16,16]
def el(name,f,t,tex,rot=None,uv=True):
    # uv=False — UV по координатам (одинаковый масштаб текстуры на всех кусках, для ткани)
    e={"name":name,"from":[round(v,3) for v in f],"to":[round(v,3) for v in t],
       "faces":{fc:({"texture":"#"+tex,"uv":FULL} if uv else {"texture":"#"+tex}) for fc in ('north','south','east','west','up','down')}}
    if rot: e["rotation"]={"angle":rot[0],"axis":"y","origin":[round(v,3) for v in rot[1]]}
    return e
def round_lying(x,z,ang=0,along='x'):
    if along=='x':
        c=[x+1.0,T,z+0.25]
        return [el("round_case",[x,T,z],[x+1.5,T+0.5,z+0.5],"brass",(ang,c) if ang else None),
                el("round_tip",[x+1.5,T+0.05,z+0.05],[x+2.0,T+0.45,z+0.45],"copper",(ang,c) if ang else None)]
    c=[x+0.25,T,z+1.0]
    return [el("round_case",[x,T,z],[x+0.5,T+0.5,z+1.5],"brass",(ang,c) if ang else None),
            el("round_tip",[x+0.05,T+0.05,z+1.5],[x+0.45,T+0.45,z+2.0],"copper",(ang,c) if ang else None)]
D=[]
# коврик-чертёж по центру, под оружием, ровно, целиком на столе (MANG — наклон, 0 = ровно)
# (текстура mat 92×44 = 23×11 px, вся на верхнюю грань)
MAT=0.05
MCX,MCZ,MW,MD,MANG=0.0,6.5,23.0,11.0,0
mat=el("mat",[MCX-MW/2,T,MCZ-MD/2],[MCX+MW/2,T+MAT,MCZ+MD/2],"mat",(MANG,[MCX,T,MCZ]) if MANG else None)
mat['faces']['up']['uv']=[0,0,16,16]
mat['faces']['down']['uv']=[0,0,16,16]
for fc in ('north','south','east','west'): mat['faces'][fc]['uv']=[0,15.6,16,16]
D.append(mat)
# ближний угол после поворота (тот, что ближе к игроку) — там свисающий треугольник
_a=math.radians(MANG)
_corners=[(dx,dz) for dx in (-MW/2,MW/2) for dz in (-MD/2,MD/2)]
_pts=[(MCX+dx*math.cos(_a)+dz*math.sin(_a), MCZ-dx*math.sin(_a)+dz*math.cos(_a)) for dx,dz in _corners]
HX,HZ=min(_pts,key=lambda p:p[1])
print('mat front corner', round(HX,2), round(HZ,2))
assert 0.2 < HZ, 'угол коврика за краем стола'
cx,cz=-13.8,11.8
D+=[el("oil_can",[cx-1.2,T,cz-1.2],[cx+1.2,T+3.2,cz+1.2],"can"),
    el("oil_can_bevel",[cx-0.85,T,cz-0.85],[cx+0.85,T+3.2,cz+0.85],"can",(45,[cx,T,cz])),
    el("oil_can_rim",[cx-1.3,T+3.1,cz-1.3],[cx+1.3,T+3.35,cz+1.3],"chrome"),
    el("oil_can_cap",[cx-0.5,T+3.35,cz-0.5],[cx+0.5,T+3.9,cz+0.5],"red"),
    el("oil_can_spout",[cx-0.15,T+3.9,cz-0.15],[cx+0.15,T+5.0,cz+0.15],"chrome")]
# ящик с патронами за оружием (без поворота)
x0,x1,z0,z1,h=1.4,4.8,10.8,13.3,1.8
D+=[el("ammo_box_floor",[x0,T,z0],[x1,T+0.3,z1],"ammo_box"),
    el("ammo_box_wall",[x0,T,z0],[x1,T+h,z0+0.35],"ammo_box"),
    el("ammo_box_wall",[x0,T,z1-0.35],[x1,T+h,z1],"ammo_box"),
    el("ammo_box_wall",[x0,T,z0],[x0+0.35,T+h,z1],"ammo_box"),
    el("ammo_box_wall",[x1-0.35,T,z0],[x1,T+h,z1],"ammo_box")]
for ccx in (x0+0.5,x0+1.4,x0+2.3):
    for ccz in (z0+0.45,z0+1.25):
        D+=[el("ammo_round_case",[ccx,T+0.3,ccz],[ccx+0.6,T+1.9,ccz+0.6],"brass"),
            el("ammo_round_tip",[ccx+0.1,T+1.9,ccz+0.1],[ccx+0.5,T+2.35,ccz+0.5],"copper")]
# запасной магазин — не в модели блока: настоящая модель магазина АКМ рисуется в GunWorkbenchRenderer.drawSpareMag
# (центр x -7.4, z 1.5, длина 4.6 px); здесь только проверка, что его место свободно (как прямоугольник)
MAG_C=(-8.4,3.0)   # = GunWorkbenchRenderer.MAG_X/MAG_Z (px)
MAG_EL=el('spare_mag(renderer)',[MAG_C[0]-2.3,T,MAG_C[1]-1.25],[MAG_C[0]+2.3,T+1.0,MAG_C[1]+1.25],'blued',(20,[MAG_C[0],T,MAG_C[1]]))
D+=[el("wrench",[-15.3,T,4.5],[-14.7,T+0.3,8.0],"chrome"),
    el("wrench_open",[-15.6,T,8.0],[-14.4,T+0.3,8.55],"chrome"),
    el("wrench_ring",[-15.5,T,3.95],[-14.5,T+0.3,4.55],"chrome")]
sc=[9.0,T,11.4]; sr=(-22.5,sc)
D+=[el("screwdriver_handle",[9.6,T,11.0],[11.6,T+0.75,11.75],"red",sr),
    el("screwdriver_ferrule",[9.25,T+0.1,11.1],[9.6,T+0.65,11.65],"chrome",sr),
    el("screwdriver_shaft",[7.6,T+0.25,11.25],[9.25,T+0.5,11.5],"chrome",sr)]
D+=[el("case",[12.0,T,8.6],[15.6,T+1.8,12.9],"case"),
    el("case_seam",[11.95,T+0.85,8.55],[15.65,T+0.95,12.95],"blued"),
    el("case_latch",[12.6,T+0.6,8.4],[13.2,T+1.3,8.6],"chrome"),
    el("case_latch",[14.4,T+0.6,8.4],[15.0,T+1.3,8.6],"chrome"),
    el("case_handle",[13.0,T+1.8,10.5],[14.6,T+2.05,11.1],"blued")]
# тряпка: мятая, в несколько слоёв, загибается через передний край и свисает полосами разной длины
C="cloth"
D+=[el("rag_sheet",[11.7,T,0.0],[15.7,T+0.25,3.8],C,None,False),
    el("rag_layer",[12.2,T+0.25,0.9],[15.0,T+0.55,3.2],C,(22.5,[13.6,T,2.05]),False),
    el("rag_fold",[12.3,T+0.5,1.5],[15.1,T+0.95,2.0],C,(-22.5,[13.7,T,1.75]),False),
    el("rag_fold",[12.9,T+0.4,2.55],[14.6,T+0.8,2.95],C,(22.5,[13.75,T,2.75]),False),
    el("rag_bump",[13.3,T+0.95,1.4],[14.3,T+1.25,2.1],C,(45,[13.8,T,1.75]),False),
    el("rag_corner",[14.5,T,3.3],[15.6,T+0.35,4.3],C,(45,[15.05,T,3.8]),False),
    el("rag_edge",[11.9,T,-0.45],[15.6,T+0.35,0.3],C,None,False),
    el("rag_hang",[12.0,T-3.4,-0.45],[13.2,T+0.1,-0.1],C,None,False),
    el("rag_hang",[13.2,T-2.5,-0.6],[14.3,T+0.1,-0.2],C,None,False),
    el("rag_hang",[14.3,T-3.0,-0.5],[15.5,T+0.1,-0.12],C,None,False),
    el("rag_hang_tip",[12.3,T-3.9,-0.4],[12.9,T-3.4,-0.15],C,None,False)]
D+=round_lying(-3.2,0.4,22.5)
D+=round_lying(-12.9,2.0,0,'z')
D+=round_lying(-13.9,7.6,0,'z')
D+=round_lying(5.4,12.6,0)
D+=round_lying(12.4,5.0,22.5)
D+=round_lying(-1.6,12.4,0)
d=math.radians(10); cxg,czg=0.0,6.6
u=(-math.cos(d),math.sin(d)); v=(math.sin(d),math.cos(d))
HL,HH,M=10.9,3.7,0.4
def rotpt(x,z,e):
    if 'rotation' not in e: return x,z
    a=math.radians(e['rotation']['angle']); ox,_,oz=e['rotation']['origin']
    dx,dz=x-ox,z-oz
    return ox+dx*math.cos(a)+dz*math.sin(a), oz-dx*math.sin(a)+dz*math.cos(a)
bad=[]
for e in D+[MAG_EL]:
    if e['to'][1]<=T or e['name'] in ('mat','mat_hang'): continue
    for x in (e['from'][0],e['to'][0]):
        for z in (e['from'][2],e['to'][2]):
            for sgn in (1,):  # поворот MC: правое вращение вокруг +Y (FaceBakery.applyElementRotation, сверено)
                ee=json.loads(json.dumps(e))
                if 'rotation' in ee: ee['rotation']['angle']*=sgn
                px,pz=rotpt(x,z,ee)
                a=(px-cxg)*u[0]+(pz-czg)*u[1]; b=(px-cxg)*v[0]+(pz-czg)*v[1]
                # магазин лежит под стволом: там у оружия нижний край — цевьё, заметно выше нижней точки следа
                # (магазин/рукоять оружия в середине), поэтому ему можно заходить в след до 0.5 px
                m = -0.5 if e['name']=='spare_mag(renderer)' else M
                if abs(a)<HL+M and abs(b)<HH+m: bad.append((e['name'],round(px,1),round(pz,1)))
                if not(-16<=px<=16 and -1<=pz<=13.5): bad.append(('OUT',e['name'],round(px,1),round(pz,1)))
print('collisions:',sorted(set(bad)))
if bad and '--force' not in sys.argv: sys.exit(1)

def auto_uv(e):
    """UV по координатам (как у Minecraft без uv), сдвинутые в 0..16 — куски за границей блока не лезут в соседние текстуры."""
    (x1,y1,z1),(x2,y2,z2)=e['from'],e['to']
    raw={'down':[x1,16-z2,x2,16-z1],'up':[x1,z1,x2,z2],'north':[16-x2,16-y2,16-x1,16-y1],
         'south':[x1,16-y2,x2,16-y1],'west':[z1,16-y2,z2,16-y1],'east':[16-z2,16-y2,16-z1,16-y1]}
    for fc,f in e['faces'].items():
        if 'uv' in f: continue
        u0,v0,u1,v1=raw[fc]
        def fit(a,b):
            lo,hi=min(a,b),max(a,b)
            sh=0.0
            if lo<0: sh=-lo
            if hi+sh>16: sh=16-hi
            return round(a+sh,3),round(b+sh,3)
        u0,u1=fit(u0,u1); v0,v1=fit(v0,v1)
        f['uv']=[u0,v0,u1,v1]

OLDN={e['name'] for e in D}|{'mat_hang'}|{'spare_mag','spare_mag_curve','spare_mag_plate','spare_mag_rib'}|{'rag','rag_fold_top','rag_drape'}
TEX={k:"exe_tacticals:block/gun_workbench/"+k for k in ['mat','brass','copper','cloth','red','case','can','blued','chrome','ammo_box','steel_dark']}
for p,dx,dy in [('block/gun_workbench.json',0,0),('item/gun_workbench.json',8,-8)]:
    m=json.load(open(p,encoding='utf-8'))
    m['elements']=[e for e in m['elements'] if e.get('name') not in OLDN]
    base=len(m['elements'])
    for e in D:
        e2=json.loads(json.dumps(e))
        for k in ('from','to'):
            e2[k][0]=round(e2[k][0]+dx,3); e2[k][1]=round(e2[k][1]+dy,3)
        if 'rotation' in e2:
            e2['rotation']['origin'][0]+=dx; e2['rotation']['origin'][1]+=dy
        auto_uv(e2)
        m['elements'].append(e2)
    m['textures'].update(TEX)
    open(p,'w',encoding='utf-8').write(json.dumps(m,indent=1,ensure_ascii=False))
    print(p,base,'+',len(D),'=',len(m['elements']))
