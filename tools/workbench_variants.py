# Верстаки для магазинов, обвесов и патронов: модели (блок + предмет), текстуры, blockstate, лут, теги, рецепты, переводы.
# Запуск из корня проекта:  python tools/workbench_variants.py
# Система координат модели как у оружейного верстака (смотрит на север): z 0 — перед (к игроку), z 16 — стенка;
# главная часть x 0..16, вторая (правее для игрока) x -16..0; столешница на 15.5 px.
import json, os, random
from PIL import Image

R = 'src/main/resources/'
A = R + 'assets/exe_tacticals/'
D = R + 'data/exe_tacticals/'
TEXDIR = A + 'textures/block/gun_workbench/'

# ---------------------------------------------------------------- текстуры

def noise_tex(name, size, base, var, seed, extra=None):
    rnd = random.Random(seed)
    w, h = size
    im = Image.new('RGBA', size)
    px = im.load()
    for y in range(h):
        for x in range(w):
            n = rnd.uniform(-var, var)
            px[x, y] = tuple(max(0, min(255, int(c + n))) for c in base) + (255,)
    if extra:
        extra(im, px, rnd)
    im.save(TEXDIR + name + '.png')

def scratches(color, count):
    def f(im, px, rnd):
        w, h = im.size
        for _ in range(count):
            x, y = rnd.randrange(w), rnd.randrange(h)
            for i in range(rnd.randint(2, 5)):
                if 0 <= x < w and 0 <= y < h:
                    px[x, y] = color + (255,)
                x += rnd.choice((-1, 0, 1)); y += rnd.choice((0, 1))
    return f

def cutmat(im, px, rnd):
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if x % 16 == 0 or y % 16 == 0:
                px[x, y] = (150, 205, 170, 255)
            elif x % 4 == 0 or y % 4 == 0:
                px[x, y] = (min(255, r + 28), min(255, g + 38), min(255, b + 30), 255)
    for x in range(1, w, 2):  # линейка по краю
        px[x, 0] = (230, 240, 230, 255)
    for _ in range(6):  # следы ножа
        x, y = rnd.randrange(w), rnd.randrange(h)
        for i in range(rnd.randint(4, 9)):
            if x < w and y < h:
                px[x, y] = (30, 70, 52, 255)
            x += 1; y += rnd.choice((0, 0, 1))

def glass(im, px, rnd):
    w, h = im.size
    for y in range(h):
        for x in range(w):
            t = (x + y) / (w + h)
            px[x, y] = (int(20 + 40 * t), int(60 + 60 * t), int(80 + 70 * t), 255)
    for i in range(w):
        for d in (0, 1):
            x, y = i, i - 4 + d
            if 0 <= y < h:
                px[x, y] = (170, 220, 235, 255)

def press(im, px, rnd):
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            k = 1.0 - 0.25 * (y / h)
            px[x, y] = (int(r * k), int(g * k), int(b * k), 255)
    scratches((150, 150, 155), 10)(im, px, rnd)

noise_tex('olive', (32, 32), (92, 98, 60), 7, 11, scratches((140, 142, 120), 14))
noise_tex('cutmat', (64, 32), (38, 92, 66), 4, 12, cutmat)
noise_tex('glass', (16, 16), (0, 0, 0), 0, 13, glass)
noise_tex('press', (32, 32), (172, 32, 30), 9, 14, press)

# ---------------------------------------------------------------- элементы

def auto_uv(e):
    """UV по координатам, сдвинутые в 0..16 (куски за границей блока не лезут в соседние текстуры)."""
    (x1, y1, z1), (x2, y2, z2) = e['from'], e['to']
    raw = {'down': [x1, 16 - z2, x2, 16 - z1], 'up': [x1, z1, x2, z2], 'north': [16 - x2, 16 - y2, 16 - x1, 16 - y1],
           'south': [x1, 16 - y2, x2, 16 - y1], 'west': [z1, 16 - y2, z2, 16 - y1], 'east': [16 - z2, 16 - y2, 16 - z1, 16 - y1]}
    for fc, f in e['faces'].items():
        if 'uv' in f:
            continue
        u0, v0, u1, v1 = raw[fc]
        def fit(a, b):
            lo, hi = min(a, b), max(a, b)
            if hi - lo > 16:
                return (0.0, 16.0) if a <= b else (16.0, 0.0)
            sh = 0.0
            if lo < 0: sh = -lo
            if hi + sh > 16: sh = 16 - hi
            return round(a + sh, 3), round(b + sh, 3)
        u0, u1 = fit(u0, u1); v0, v1 = fit(v0, v1)
        f['uv'] = [u0, v0, u1, v1]

def box(name, f, t, tex, rot=None, faces=None, full=()):
    """tex — текстура всех граней; faces — {грань: текстура} поверх; full — грани с uv на всю текстуру."""
    e = {'name': name, 'from': [round(v, 3) for v in f], 'to': [round(v, 3) for v in t], 'faces': {}}
    for fc in ('north', 'south', 'east', 'west', 'up', 'down'):
        e['faces'][fc] = {'texture': '#' + (faces or {}).get(fc, tex)}
        if fc in full:
            e['faces'][fc]['uv'] = [0, 0, 16, 16]
    if rot:
        angle, axis, origin = rot
        e['rotation'] = {'angle': angle, 'axis': axis, 'origin': [round(v, 3) for v in origin]}
    return e

T = 15.5

def frame(E, x0, x1, top_tex, side_tex, wall_tex, post_tex, beam_tex):
    """Столешница, задняя стенка со стойками и верхняя балка с лампой — общий каркас."""
    E.append(box('top', [x0, 13, 0], [x1, T, 16], side_tex, faces={'up': top_tex}, full=('up',)))
    E.append(box('wall', [x0 + 0.5, T, 14.5], [x1 - 0.5, 30, 15.5], post_tex, faces={'north': wall_tex}, full=('north',)))
    E.append(box('post', [x0, T, 13.6], [x0 + 1.2, 30, 15.9], post_tex))
    E.append(box('post', [x1 - 1.2, T, 13.6], [x1, 30, 15.9], post_tex))
    E.append(box('beam', [x0, 30, 13], [x1, 31.2, 16.2], beam_tex))
    E.append(box('lamp_bar', [x0 + 2, 30.4, 9.4], [x1 - 2, 31.2, 12.2], 'lamp_bar', faces={'down': 'lamp'}))
    E.append(box('lamp_clip', [x0 + 3, 31.2, 9.8], [x0 + 4.5, 32, 15.8], 'lamp_bar'))
    E.append(box('lamp_clip', [x1 - 4.5, 31.2, 9.8], [x1 - 3, 32, 15.8], 'lamp_bar'))

def lying_round(E, x, z, along='x'):
    if along == 'x':
        E += [box('round_case', [x, T, z], [x + 1.5, T + 0.5, z + 0.5], 'brass'),
              box('round_tip', [x + 1.5, T + 0.05, z + 0.05], [x + 2.0, T + 0.45, z + 0.45], 'copper')]
    else:
        E += [box('round_case', [x, T, z], [x + 0.5, T + 0.5, z + 1.5], 'brass'),
              box('round_tip', [x + 0.05, T + 0.05, z + 1.5], [x + 0.45, T + 0.45, z + 2.0], 'copper')]

def standing_round(E, x, y, z, bullet=True):
    E.append(box('case', [x, y, z], [x + 0.6, y + 1.2, z + 0.6], 'brass'))
    if bullet:
        E.append(box('bullet', [x + 0.1, y + 1.2, z + 0.1], [x + 0.5, y + 1.7, z + 0.5], 'copper'))

def hanging_mag(E, x, tex, y=23.0):
    """Магазин на крючке стенки: прямая часть + изогнутый низ (поворот вокруг z)."""
    E.append(box('mag_hook', [x + 0.6, y + 4.2, 13.1], [x + 1.0, y + 4.6, 14.5], 'chrome'))
    E.append(box('mag', [x, y, 13.5], [x + 1.6, y + 4.2, 14.4], tex))
    E.append(box('mag_low', [x + 0.15, y - 2.4, 13.5], [x + 1.75, y + 0.2, 14.4], tex, rot=(-22.5, 'z', [x + 0.8, y, 13.95])))
    E.append(box('mag_plate', [x + 0.0, y - 2.75, 13.4], [x + 1.9, y - 2.35, 14.5], 'steel_dark', rot=(-22.5, 'z', [x + 0.8, y, 13.95])))

def lying_mag(E, x, z, tex, length=4.6):
    E.append(box('mag', [x, T, z], [x + length, T + 0.9, z + 1.6], tex))
    E.append(box('mag_lip', [x + length, T + 0.1, z + 0.3], [x + length + 0.5, T + 0.8, z + 1.3], 'steel_dark'))

def handles(E, x0, x1, z, ys):
    for y in ys:
        c = (x0 + x1) / 2
        E.append(box('handle', [c - 1.5, y, z - 0.45], [c + 1.5, y + 0.5, z], 'chrome'))

# ---------------------------------------------------------------- верстак для магазинов (2×2)

def magazine_bench():
    E = []
    frame(E, -16, 16, 'top', 'olive', 'pegboard', 'olive', 'olive')
    E.append(box('top_trim', [-16, 12.6, -0.3], [16, T + 0.1, 0.2], 'steel'))
    for x, z in ((-15.5, 0.5), (13.5, 0.5), (-15.5, 13.5), (13.5, 13.5)):
        E.append(box('leg', [x, 0.6, z], [x + 2, 13, z + 2], 'olive'))
        E.append(box('foot', [x - 0.3, 0, z - 0.3], [x + 2.3, 0.6, z + 2.3], 'steel_dark'))
    # тумба с ящиками (справа для игрока)
    E.append(box('cabinet', [-13.5, 1, 1], [-1.5, 13, 14], 'olive', faces={'north': 'drawer'}, full=('north',)))
    handles(E, -13.5, -1.5, 1, (11, 7.3, 3.6))
    # полка слева, на ней ящик и банка
    E.append(box('shelf', [-1.5, 3, 1.5], [13.5, 4, 14], 'steel_dark'))
    E.append(box('crate', [2, 4, 4], [9, 8, 11], 'ammo_box'))
    E.append(box('crate_lid', [1.8, 8, 3.8], [9.2, 8.4, 11.2], 'olive'))
    E.append(box('can', [10, 4, 6], [12.5, 7.5, 8.5], 'can'))
    E.append(box('can_cap', [10.6, 7.5, 6.6], [11.9, 8.1, 7.9], 'red'))
    # магазины на перфорированной стенке
    for i, (x, tex) in enumerate(((-13, 'blued'), (-10.6, 'part'), (-8.2, 'blued'), (-5.8, 'part'), (-3.4, 'olive'))):
        hanging_mag(E, x, tex)
    # пружины на крючке
    E.append(box('spring_hook', [1.2, 27.4, 13.1], [1.6, 27.8, 14.5], 'chrome'))
    for k in range(7):
        y = 26.9 - k * 0.7
        E.append(box('spring', [0.6, y, 13.3], [2.2, y + 0.3, 14.3], 'chrome'))
    # планшет с бумагой и полка с коробками патронов
    E.append(box('clipboard', [8, 23.2, 14.2], [13, 28.5, 14.5], 'paper', faces={'south': 'steel_dark'}, full=('north',)))
    E.append(box('clip', [9.8, 28.2, 14.0], [11.2, 28.8, 14.2], 'steel'))
    E.append(box('wall_shelf', [3, 19, 11.8], [14, 19.8, 14.5], 'olive'))
    E.append(box('wall_shelf_bracket', [4, 17.4, 13.5], [4.8, 19, 14.5], 'steel_dark'))
    E.append(box('wall_shelf_bracket', [12, 17.4, 13.5], [12.8, 19, 14.5], 'steel_dark'))
    E.append(box('box', [3.5, 19.8, 12.1], [7.5, 22.3, 14.3], 'ammo_box'))
    E.append(box('box', [8, 19.8, 12.1], [11.5, 21.8, 14.3], 'ammo_box'))
    E.append(box('box', [11.8, 19.8, 12.3], [13.6, 22.8, 14.2], 'case'))
    # на столе: лоток с магазинами (справа спереди)
    E.append(box('tray', [-13.5, T, 1.5], [-5.5, T + 0.4, 8], 'steel_dark'))
    for z0, z1 in ((1.5, 1.85), (7.65, 8)):
        E.append(box('tray_wall', [-13.5, T, z0], [-5.5, T + 1.2, z1], 'steel_dark'))
    for x0, x1 in ((-13.5, -13.15), (-5.85, -5.5)):
        E.append(box('tray_wall', [x0, T, 1.5], [x1, T + 1.2, 8], 'steel_dark'))
    for z, tex in ((2.2, 'blued'), (4.1, 'part'), (6.0, 'blued')):
        E.append(box('mag', [-12.8, T + 0.4, z], [-7.2, T + 1.3, z + 1.5], tex))
        E.append(box('mag_lip', [-7.2, T + 0.5, z + 0.3], [-6.6, T + 1.15, z + 1.2], 'steel_dark'))
    # в центре — разобранный магазин: корпус, пружина, подаватель, крышка
    lying_mag(E, -3, 3.2, 'blued', 5.0)
    for k in range(8):
        x = -2.6 + k * 0.62
        E.append(box('spring_coil', [x, T, 6.0], [x + 0.25, T + 0.9, 7.2], 'chrome'))
    E.append(box('follower', [3.2, T, 6.0], [4.4, T + 0.7, 7.3], 'red'))
    E.append(box('floorplate', [3.4, T, 3.2], [4.9, T + 0.25, 4.9], 'steel_dark'))
    # пачки патронов (слева сзади) и снаряжатель
    E.append(box('ammo_pack', [8, T, 10], [13, T + 2, 13], 'ammo_box'))
    E.append(box('ammo_pack', [8.6, T + 2, 10.4], [12.4, T + 3.4, 12.6], 'ammo_box'))
    E.append(box('loader', [5, T, 2], [6.8, T + 2.2, 3.6], 'red'))
    E.append(box('loader_rail', [5.4, T + 2.2, 2.3], [6.4, T + 2.6, 3.3], 'chrome'))
    for x, z, a in ((9, 4, 'x'), (11.5, 6.2, 'z'), (8.2, 7.4, 'x'), (13.5, 3, 'z')):
        lying_round(E, x, z, a)
    # коробка с пружинами за центром
    E.append(box('spring_box', [-2, T, 11], [2, T + 1.5, 13.5], 'case'))
    return E

# ---------------------------------------------------------------- верстак для обвесов (2×2)

def attachment_bench():
    E = []
    frame(E, -16, 16, 'wood_board', 'wood', 'metal_board', 'steel_dark', 'steel')
    # две тумбы с ящиками, посередине место для ног
    for x0, x1 in ((-15.5, -6), (6, 15.5)):
        E.append(box('cabinet', [x0, 0, 0.5], [x1, 13, 15.5], 'steel_dark', faces={'north': 'drawer'}, full=('north',)))
        handles(E, x0, x1, 0.5, (10.6, 6.6, 2.6))
    E.append(box('knee_panel', [-6, 4, 14], [6, 13, 15], 'steel_dark'))
    # резиновый коврик для резки
    E.append(box('cutmat', [-10, T, 1.5], [8, T + 0.05, 11], 'cutmat', full=('up',)))
    # пикатинни-планка на стенке, на ней прицел и коллиматор
    E.append(box('rail', [-13, 25, 13.8], [13, 25.8, 14.5], 'steel_dark'))
    for k in range(20):
        x = -12.6 + k * 1.3
        E.append(box('rail_rib', [x, 25.8, 13.9], [x + 0.6, 26.2, 14.5], 'part'))
    E.append(box('scope_tube', [-11, 26.8, 12.7], [-3, 27.9, 13.8], 'part'))
    E.append(box('scope_bell', [-3, 26.4, 12.3], [-1, 28.3, 14.2], 'part'))
    E.append(box('scope_lens', [-1, 26.7, 12.6], [-0.9, 28.0, 13.9], 'glass', full=('east',)))
    E.append(box('scope_eye', [-12.6, 26.5, 12.5], [-11, 28.2, 14.0], 'part'))
    E.append(box('scope_turret', [-7.5, 27.9, 12.8], [-6.5, 28.9, 13.7], 'steel_dark'))
    for x in (-9.5, -5.5):
        E.append(box('scope_ring', [x, 26.2, 12.6], [x + 0.8, 28.1, 13.9], 'steel_dark'))
    E.append(box('red_dot', [1.5, 26.2, 12.8], [4.5, 28.0, 14.0], 'part'))
    E.append(box('red_dot_glass', [4.5, 26.5, 13.0], [4.6, 27.7, 13.8], 'glass', full=('east',)))
    E.append(box('red_dot_hood', [4.2, 28.0, 12.8], [4.6, 28.4, 14.0], 'part'))
    # глушитель и рукоять на крючках
    for x in (6.5, 11.5):
        E.append(box('hook', [x, 23.3, 12.8], [x + 0.6, 23.8, 14.5], 'chrome'))
    E.append(box('suppressor', [5, 22.0, 13.0], [13, 23.3, 14.3], 'part'))
    E.append(box('suppressor_cap', [13, 22.15, 13.1], [13.4, 23.15, 14.2], 'steel'))
    E.append(box('hook', [-11.6, 22.6, 12.8], [-11.0, 23.1, 14.5], 'chrome'))
    E.append(box('grip', [-12, 19.2, 13.4], [-10.6, 22.6, 14.4], 'part'))
    E.append(box('grip_cap', [-12.15, 19.0, 13.3], [-10.45, 19.4, 14.5], 'steel_dark'))
    E.append(box('hook', [-6.6, 22.6, 12.8], [-6.0, 23.1, 14.5], 'chrome'))
    E.append(box('pistol_grip', [-7.2, 19.0, 13.4], [-5.4, 22.6, 14.4], 'part', rot=(-22.5, 'z', [-6.3, 22.6, 13.9])))
    E.append(box('manual', [-2, 17, 14.2], [3, 23, 14.5], 'paper', faces={'south': 'steel_dark'}, full=('north',)))
    # на столе: лампа-лупа (слева сзади)
    E.append(box('lamp_base', [11, T, 11], [14, T + 0.5, 14], 'steel_dark'))
    E.append(box('lamp_arm', [12.2, T + 0.5, 12.2], [12.8, 22, 12.8], 'steel'))
    E.append(box('lamp_arm', [9, 21.6, 12.2], [12.8, 22.2, 12.8], 'steel'))
    E.append(box('lamp_arm', [8.7, 21.6, 8.4], [9.3, 22.2, 12.8], 'steel'))
    E.append(box('lamp_head', [7, 21.0, 4.6], [11, 21.7, 8.6], 'steel_dark'))
    E.append(box('lamp_lens', [7.6, 20.95, 5.2], [10.4, 21.05, 8.0], 'glass', full=('down', 'up')))
    # прицел в работе на коврике
    E.append(box('scope_tube', [-6, T, 4], [1.5, T + 1.1, 5.1], 'part'))
    E.append(box('scope_bell', [1.5, T, 3.6], [3.3, T + 1.8, 5.5], 'part'))
    E.append(box('scope_eye', [-7.5, T, 3.8], [-6, T + 1.5, 5.3], 'part'))
    E.append(box('scope_turret', [-2.5, T + 1.1, 4.1], [-1.5, T + 1.9, 5.0], 'steel_dark'))
    # рукоять и винтики рядом
    E.append(box('grip', [3, T, 7.2], [4.6, T + 0.9, 10.4], 'part', rot=(22.5, 'y', [3.8, T, 8.8])))
    for x, z in ((-3, 7.5), (-2.2, 8.4), (-1.1, 7.8), (0, 8.9)):
        E.append(box('screw', [x, T, z], [x + 0.35, T + 0.3, z + 0.35], 'chrome'))
    # органайзер с битами (справа спереди)
    E.append(box('bit_case', [-14.5, T, 1.5], [-10.5, T + 1.1, 6], 'case'))
    for i in range(4):
        for j in range(2):
            x, z = -14.1 + i * 0.95, 2.3 + j * 2.0
            E.append(box('bit', [x, T + 1.1, z], [x + 0.4, T + 1.5, z + 0.9], 'chrome'))
    # отвёртка с красной ручкой
    E.append(box('driver_handle', [-9, T, 7.5], [-8.2, T + 0.8, 10], 'red'))
    E.append(box('driver_shaft', [-8.8, T + 0.25, 10], [-8.4, T + 0.55, 13], 'chrome'))
    # лотки с мелочёвкой (справа сзади)
    for x0, tex in ((-14.5, 'red'), (-11.8, 'blued'), (-9.1, 'steel')):
        E.append(box('bin', [x0, T, 11], [x0 + 2.4, T + 1.6, 13.8], tex))
        E.append(box('bin_inner', [x0 + 0.3, T + 1.2, 11.3], [x0 + 2.1, T + 1.61, 13.5], 'case', full=('up',)))
    E.append(box('sheet', [3, T, 10.5], [7.5, T + 0.05, 13.5], 'paper', rot=(-22.5, 'y', [5.2, T, 12]), full=('up',)))
    return E

# ---------------------------------------------------------------- верстак для патронов (1×2)

def ammo_bench():
    E = []
    frame(E, 0, 16, 'wood_top', 'wood_leg', 'pegboard', 'steel_dark', 'steel')
    E.append(box('cabinet', [0.5, 0, 0.5], [15.5, 13, 15.5], 'steel_dark', faces={'north': 'drawer'}, full=('north',)))
    handles(E, 0.5, 15.5, 0.5, (10.6, 6.6, 2.6))
    # полка: порох, коробки с матрицами и пулями
    E.append(box('wall_shelf', [1.2, 24, 11.4], [14.8, 24.8, 14.5], 'wood_leg'))
    for x in (2, 4.5):
        E.append(box('powder', [x, 24.8, 12], [x + 2, 27.6, 14], 'can'))
        E.append(box('powder_cap', [x + 0.3, 27.6, 12.3], [x + 1.7, 28.2, 13.7], 'red'))
    E.append(box('die_box', [7.3, 24.8, 12], [10.5, 26.4, 14.2], 'case'))
    E.append(box('bullet_box', [11, 24.8, 12], [14.4, 27, 14.2], 'ammo_box'))
    # на стенке: штангенциркуль и триммер
    E.append(box('caliper', [2, 18.5, 14.1], [2.7, 23.2, 14.5], 'chrome'))
    E.append(box('caliper_jaw', [2, 22.3, 14.1], [3.9, 22.8, 14.5], 'chrome'))
    E.append(box('caliper_jaw', [2, 21.3, 14.1], [3.6, 21.7, 14.5], 'chrome'))
    E.append(box('trimmer', [11, 18, 13.6], [14, 19.5, 14.5], 'press'))
    E.append(box('trimmer_shaft', [9, 18.5, 13.8], [11, 19, 14.3], 'chrome'))
    # пресс для снаряжения (справа)
    E.append(box('press_base', [1.5, T, 5.5], [7, T + 1, 10.5], 'press'))
    E.append(box('press_frame', [3.2, T + 1, 8.8], [5.8, 22.2, 10.2], 'press'))
    E.append(box('press_head', [2.6, 22.2, 6.4], [6.4, 23.7, 10.2], 'press'))
    E.append(box('press_ram', [4, T + 1, 7.2], [5, 19.2, 8.2], 'chrome'))
    E.append(box('press_shell', [3.7, 19.2, 6.9], [5.3, 19.6, 8.5], 'steel_dark'))
    E.append(box('press_case', [4.2, 19.6, 7.4], [4.8, 20.6, 8.0], 'brass'))
    E.append(box('press_die', [4, 20.8, 7.2], [5, 22.2, 8.2], 'chrome'))
    E.append(box('press_die', [3.9, 23.7, 7.1], [5.1, 25, 8.3], 'chrome'))
    E.append(box('press_nut', [3.6, 23.7, 6.8], [5.4, 24.2, 8.6], 'steel_dark'))
    E.append(box('press_pivot', [5.8, 18.4, 7.3], [6.6, 19.4, 8.1], 'steel_dark'))
    E.append(box('press_lever', [6.2, 18.6, 7.4], [13.6, 19.2, 8.0], 'chrome', rot=(22.5, 'z', [6.2, 18.9, 7.7])))
    E.append(box('press_knob', [13.4, 18.3, 7.1], [14.8, 19.5, 8.3], 'part', rot=(22.5, 'z', [6.2, 18.9, 7.7])))
    # дозатор пороха (слева сзади)
    E.append(box('measure_stand', [10, T, 10], [13.2, T + 0.5, 13.2], 'steel_dark'))
    E.append(box('measure_post', [11.3, T + 0.5, 11.3], [11.9, 18, 11.9], 'steel'))
    E.append(box('measure_body', [10.6, 18, 10.6], [12.6, 19.2, 12.6], 'steel'))
    E.append(box('measure_hopper', [10.4, 19.2, 10.4], [12.8, 23, 12.8], 'glass'))
    E.append(box('measure_cap', [10.3, 23, 10.3], [12.9, 23.5, 12.9], 'red'))
    E.append(box('measure_arm', [12.6, 18.3, 11.4], [14, 18.8, 11.8], 'chrome'))
    # весы
    E.append(box('scale', [7.3, T, 10.6], [9.8, T + 0.6, 13.6], 'steel_dark'))
    E.append(box('scale_pan', [7.8, T + 0.6, 11.4], [9.3, T + 0.8, 12.9], 'chrome'))
    E.append(box('scale_display', [7.6, T + 0.1, 10.5], [9.5, T + 0.5, 10.6], 'glass', full=('north',)))
    # блок с гильзами (слева спереди)
    E.append(box('loading_block', [9, T, 1.5], [14.2, T + 1, 5.2], 'case'))
    for i in range(4):
        for j in range(3):
            standing_round(E, 9.4 + i * 1.2, T + 1, 1.9 + j * 1.1, bullet=(i + j) % 2 == 0)
    # коробка готовых патронов (справа спереди)
    E.append(box('ammo_box', [1, T, 1.2], [5.6, T + 1.5, 4.4], 'ammo_box'))
    for i in range(3):
        standing_round(E, 1.6 + i * 1.4, T + 1.0, 2.4)
    lying_round(E, 6.6, 3.2, 'z')
    lying_round(E, 7.8, 6.8, 'x')
    return E

# ---------------------------------------------------------------- запись

TEX_ALL = ['olive', 'cutmat', 'glass', 'press', 'top', 'pegboard', 'drawer', 'wood', 'wood_board', 'wood_top', 'wood_leg',
           'steel', 'steel_dark', 'metal_board', 'lamp', 'lamp_bar', 'part', 'ammo_box', 'paper', 'brass', 'copper', 'red',
           'case', 'can', 'blued', 'chrome']

def textures(E, particle):
    used = sorted({f['texture'][1:] for e in E for f in e['faces'].values()})
    for u in used:
        assert u in TEX_ALL, u
    t = {u: 'exe_tacticals:block/gun_workbench/' + u for u in used}
    t['particle'] = 'exe_tacticals:block/gun_workbench/' + particle
    return t

def check(name, E, x0, x1):
    for e in E:
        for k in ('from', 'to'):
            for v in e[k]:
                assert -16 <= v <= 32, (name, e)
        assert e['from'][0] >= x0 - 0.5 and e['to'][0] <= x1 + 0.5, (name, 'вне ширины', e)

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(json.dumps(obj, indent=1, ensure_ascii=False))

WIDE_DISPLAY = json.load(open(A + 'models/item/gun_workbench.json', encoding='utf-8'))['display']
NARROW_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'head': {'rotation': [0, 0, 0], 'translation': [0, 10, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.3, 0.3, 0.3]},
    'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.3, 0.3, 0.3]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 2, 0], 'scale': [0.3, 0.3, 0.3]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 2, 0], 'scale': [0.3, 0.3, 0.3]},
}

BENCHES = [
    ('magazine_workbench', magazine_bench, -16, 16, 'olive', 8),
    ('attachment_workbench', attachment_bench, -16, 16, 'steel_dark', 8),
    ('ammo_workbench', ammo_bench, 0, 16, 'steel_dark', 0),
]

for name, build, x0, x1, particle, dx in BENCHES:
    E = build()
    check(name, E, x0, x1)
    for e in E:
        auto_uv(e)
    tex = textures(E, particle)
    write(A + 'models/block/' + name + '.json',
          {'credit': 'Exe-tacticals ' + name + ' (tools/workbench_variants.py)', 'ambientocclusion': False,
           'textures': tex, 'elements': E})
    write(A + 'models/block/' + name + '_part.json', {'textures': {'particle': tex['particle']}})
    IE = json.loads(json.dumps(E))
    for e in IE:
        for k in ('from', 'to'):
            e[k][0] = round(e[k][0] + dx, 3); e[k][1] = round(e[k][1] - 8, 3)
        if 'rotation' in e:
            e['rotation']['origin'][0] += dx; e['rotation']['origin'][1] -= 8
    write(A + 'models/item/' + name + '.json',
          {'ambientocclusion': False, 'textures': tex, 'gui_light': 'side',
           'display': WIDE_DISPLAY if dx else NARROW_DISPLAY, 'elements': IE})
    variants = {}
    for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for part in ('main', 'right', 'top', 'top_right'):
            v = {'model': 'exe_tacticals:block/' + name + ('' if part == 'main' else '_part')}
            if part == 'main' and y:
                v['y'] = y
            variants['facing=%s,part=%s' % (facing, part)] = v
    write(A + 'blockstates/' + name + '.json', {'variants': variants})
    write(D + 'loot_tables/blocks/' + name + '.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'exe_tacticals:' + name}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'},
                                  {'condition': 'minecraft:block_state_property', 'block': 'exe_tacticals:' + name,
                                   'properties': {'part': 'main'}}]}]})
    print(name, len(E), 'elements')

tag = R + 'data/minecraft/tags/blocks/mineable/pickaxe.json'
t = json.load(open(tag, encoding='utf-8'))
for name, *_ in BENCHES:
    if 'exe_tacticals:' + name not in t['values']:
        t['values'].append('exe_tacticals:' + name)
write(tag, t)

# ---------------------------------------------------------------- рецепты верстаков (обычный верстак)

def shaped(pattern, key, result):
    return {'type': 'minecraft:crafting_shaped', 'pattern': pattern,
            'key': {k: ({'tag': v[1:]} if v.startswith('#') else {'item': v}) for k, v in key.items()},
            'result': {'item': 'exe_tacticals:' + result}}

write(D + 'recipes/magazine_workbench.json', shaped(['III', 'IHI', 'P P'],
      {'I': 'minecraft:iron_ingot', 'H': 'minecraft:chest', 'P': '#minecraft:planks'}, 'magazine_workbench'))
write(D + 'recipes/attachment_workbench.json', shaped(['IGI', 'ICI', 'P P'],
      {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass', 'C': 'minecraft:crafting_table', 'P': '#minecraft:planks'},
      'attachment_workbench'))
write(D + 'recipes/ammo_workbench.json', shaped(['IGI', 'IFI', 'P P'],
      {'I': 'minecraft:iron_ingot', 'G': 'minecraft:gunpowder', 'F': 'minecraft:furnace', 'P': '#minecraft:planks'},
      'ammo_workbench'))

# ---------------------------------------------------------------- рецепты предметов (тип exe_tacticals:gun_crafting)

def ing(*pairs):
    out = []
    for item, count in pairs:
        out.append(({'tag': item[1:]} if item.startswith('#') else {'item': 'minecraft:' + item}) | {'count': count})
    return out

def recipe(station, category, item, ingredients, count=1):
    res = {'item': 'exe_tacticals:' + item}
    if count > 1:
        res['count'] = count
    write(D + 'recipes/workbench/%s/%s.json' % (station, item),
          {'type': 'exe_tacticals:gun_crafting', 'station': station, 'category': category,
           'result': res, 'ingredients': ingredients})

STEEL = ing(('iron_ingot', 5), ('iron_nugget', 4))
POLY = ing(('iron_ingot', 2), ('black_dye', 3), ('slime_ball', 1))
for item, cat, ings in [
    ('stanag_mag_30r', 'magazine_ar', ing(('iron_ingot', 4), ('redstone', 1))),
    ('usgi_mag_20r', 'magazine_ar', ing(('iron_ingot', 3), ('redstone', 1))),
    ('pmag_30r', 'magazine_ar', POLY),
    ('pmag_40r', 'magazine_ar', ing(('iron_ingot', 3), ('black_dye', 4), ('slime_ball', 1))),
    ('surefire_mag_60r', 'magazine_ar', ing(('iron_ingot', 8), ('redstone', 2), ('iron_nugget', 6))),
    ('spear_mag_20r', 'magazine_ar', ing(('iron_ingot', 4), ('black_dye', 2), ('redstone', 1))),
    ('akm_steel_mag', 'magazine_ak', STEEL),
    ('6l18', 'magazine_ak', ing(('iron_ingot', 2), ('brown_dye', 3), ('slime_ball', 1))),
    ('6l23', 'magazine_ak', POLY),
    ('6l31', 'magazine_ak', ing(('iron_ingot', 3), ('black_dye', 4), ('slime_ball', 1), ('iron_nugget', 6))),
]:
    recipe('magazine', cat, item, ings)

A_SIGHT = ing(('iron_ingot', 3), ('glass', 2), ('copper_ingot', 2), ('redstone', 1))
A_DOT = ing(('iron_ingot', 2), ('glass', 1), ('redstone', 2), ('glowstone_dust', 1))
A_IRON = ing(('iron_ingot', 2), ('iron_nugget', 3))
A_GRIP = ing(('iron_nugget', 4), ('black_dye', 2), ('slime_ball', 1))
A_BARREL = ing(('iron_ingot', 6), ('iron_nugget', 4))
A_STOCK = ing(('iron_ingot', 2), ('black_dye', 2), ('slime_ball', 1))
A_MUZZLE = ing(('iron_ingot', 2), ('iron_nugget', 2))
A_SUPP = ing(('iron_ingot', 6), ('white_wool', 2), ('copper_ingot', 2))
A_HG = ing(('iron_ingot', 3), ('black_dye', 1))
A_WOOD = ing(('#minecraft:planks', 3), ('iron_nugget', 4))
for item, cat, ings in [
    ('acog', 'sight', A_SIGHT), ('elcan', 'sight', A_SIGHT), ('vortex_razor_hd', 'sight', A_SIGHT),
    ('vortex_razor_red_dot', 'sight', A_DOT), ('eotech_exps3', 'sight', A_DOT),
    ('a2_carry_handle', 'sight', ing(('iron_ingot', 3), ('iron_nugget', 2))),
    ('kac_folding_sight_rear', 'sight', A_IRON), ('kac_folding_sight_far', 'sight', A_IRON),
    ('magpul_mbus_pro_sight_rear', 'sight', A_IRON), ('magpul_mbus_pro_sight_far', 'sight', A_IRON),
    ('canted_rail', 'rail', ing(('iron_ingot', 1), ('iron_nugget', 4))),
    ('b10_mount_rail', 'rail', ing(('iron_ingot', 1), ('iron_nugget', 4))),
    ('b13_bracket', 'rail', ing(('iron_ingot', 2), ('iron_nugget', 2))),
    ('a2_pistol_grip', 'grip', A_GRIP), ('moe_grip', 'grip', A_GRIP), ('sako_arg_grip', 'grip', A_GRIP),
    ('mcx_m400_grip', 'grip', A_GRIP), ('ak_polymer_grip', 'grip', A_GRIP), ('rk9_grip', 'grip', A_GRIP),
    ('ak_bakelite_grip', 'grip', ing(('iron_nugget', 4), ('brown_dye', 2), ('clay_ball', 1))),
    ('kac_forward_grip', 'foregrip', A_GRIP), ('rk_2_grip', 'foregrip', A_GRIP),
    ('m4_profile_fsb_barrel', 'barrel', A_BARREL), ('urgi_barrel', 'barrel', A_BARREL),
    ('ak24a_barrel', 'barrel', A_BARREL), ('ak24b_barrel', 'barrel', A_BARREL), ('sig_fury_barrel', 'barrel', A_BARREL),
    ('m4_carbine_stock', 'stock', A_STOCK), ('moe_carbine_stock', 'stock', A_STOCK), ('ctr_stock', 'stock', A_STOCK),
    ('magpul_sl_m_stock', 'stock', A_STOCK), ('6p34_stock', 'stock', A_STOCK), ('pt1_stock', 'stock', A_STOCK),
    ('akm_stock', 'stock', ing(('#minecraft:planks', 4), ('iron_nugget', 4))),
    ('ar_stock_adapter_type1', 'stock', A_MUZZLE), ('mcx_stock_tube', 'stock', A_MUZZLE),
    ('a2_flash_hinder', 'muzzle', A_MUZZLE), ('ase_flash_hider', 'muzzle', A_MUZZLE),
    ('clutch_lok_qd_flash_hider', 'muzzle', A_MUZZLE), ('socom_rc2', 'suppressor', A_SUPP),
    ('ar15_muzzle_brake', 'muzzle', A_MUZZLE), ('ak74_muzzle_brake', 'muzzle', A_MUZZLE),
    ('akm_muzzle_brake', 'muzzle', A_MUZZLE), ('pbs_4', 'suppressor', A_SUPP), ('dtkp_545', 'suppressor', A_SUPP),
    ('dtk1_compensator', 'muzzle', A_MUZZLE), ('slx_suppressor', 'suppressor', A_SUPP),
    ('car_15_handguard', 'handguard', A_HG), ('kac_ras_handguard', 'handguard', A_HG),
    ('daniel_defense_ris_ii_handguard', 'handguard', A_HG), ('urgi_handguard', 'handguard', A_HG),
    ('ak24a_handguard', 'handguard', A_HG), ('ak_polymer_handguard_lower', 'handguard', A_HG),
    ('ak_polymer_handguard_upper', 'handguard', A_HG), ('ak_wooden_handguard_lower', 'handguard', A_WOOD),
    ('ak_wooden_handguard_upper', 'handguard', A_WOOD), ('b10_handguard', 'handguard', A_HG),
    ('b19_handguard', 'handguard', A_HG), ('spear_handguard', 'handguard', A_HG), ('spear_lt_handguard', 'handguard', A_HG),
    ('m203', 'underbarrel', ing(('iron_ingot', 8), ('redstone', 2), ('gunpowder', 2))),
    ('gp25', 'underbarrel', ing(('iron_ingot', 8), ('redstone', 2), ('gunpowder', 2))),
    ('peq_15', 'laser', ing(('iron_ingot', 2), ('redstone', 4), ('glowstone_dust', 2), ('glass', 1))),
    ('6p34_dustcover', 'dustcover', ing(('iron_ingot', 3))), ('pdc_dustcover', 'dustcover', ing(('iron_ingot', 4), ('iron_nugget', 2))),
    ('ak_ribbed_dustcover', 'dustcover', ing(('iron_ingot', 3))),
]:
    recipe('attachment', cat, item, ings)

for item in ('ammo_5_56x45', 'ammo_5_45x39', 'ammo_6_8x51', 'ammo_7_62x39'):
    recipe('ammo', 'ammo', item, ing(('copper_ingot', 2), ('gunpowder', 2), ('iron_nugget', 1)), count=16)
    # трассер: тот же патрон + состав (редстоун и светопыль)
    recipe('ammo', 'ammo', item + '_tracer',
           ing(('copper_ingot', 2), ('gunpowder', 2), ('iron_nugget', 1), ('redstone', 1), ('glowstone_dust', 1)),
           count=16)

# ---------------------------------------------------------------- переводы

LANG = {
    'ru_ru': {
        'itemGroup.exe_tacticals.workbenches': 'Верстаки',
        'block.exe_tacticals.magazine_workbench': 'Верстак для магазинов',
        'block.exe_tacticals.attachment_workbench': 'Верстак для обвесов',
        'block.exe_tacticals.ammo_workbench': 'Верстак для патронов',
        'category': {'magazine_ar': 'Магазин AR', 'magazine_ak': 'Магазин АК', 'sight': 'Прицел', 'rail': 'Планка',
                     'grip': 'Рукоять', 'foregrip': 'Передняя рукоять', 'barrel': 'Ствол', 'stock': 'Приклад',
                     'muzzle': 'Дульное устройство', 'handguard': 'Цевьё', 'underbarrel': 'Подствольник',
                     'laser': 'ЛЦУ', 'dustcover': 'Крышка', 'ammo': 'Патроны',
                     'suppressor': 'Глушитель'},
        'group': {'all': 'Все', 'rifle': 'Автоматы', 'magazine_ar': 'AR', 'magazine_ak': 'АК', 'sight': 'Прицелы',
                  'rail': 'Планки', 'suppressor': 'Глушители', 'muzzle': 'Дульные', 'stock': 'Приклады',
                  'grip': 'Рукояти', 'foregrip': 'Передние рукояти', 'handguard': 'Цевья', 'barrel': 'Стволы',
                  'underbarrel': 'Подствольники', 'laser': 'ЛЦУ', 'dustcover': 'Крышки', 'ammo': 'Патроны'},
    },
    'en_us': {
        'itemGroup.exe_tacticals.workbenches': 'Workbenches',
        'block.exe_tacticals.magazine_workbench': 'Magazine Workbench',
        'block.exe_tacticals.attachment_workbench': 'Attachment Workbench',
        'block.exe_tacticals.ammo_workbench': 'Ammo Workbench',
        'category': {'magazine_ar': 'AR magazine', 'magazine_ak': 'AK magazine', 'sight': 'Sight', 'rail': 'Rail',
                     'grip': 'Grip', 'foregrip': 'Foregrip', 'barrel': 'Barrel', 'stock': 'Stock',
                     'muzzle': 'Muzzle device', 'handguard': 'Handguard', 'underbarrel': 'Underbarrel',
                     'laser': 'Laser', 'dustcover': 'Dust cover', 'ammo': 'Ammunition',
                     'suppressor': 'Suppressor'},
        'group': {'all': 'All', 'rifle': 'Rifles', 'magazine_ar': 'AR', 'magazine_ak': 'AK', 'sight': 'Sights',
                  'rail': 'Rails', 'suppressor': 'Suppressors', 'muzzle': 'Muzzle', 'stock': 'Stocks',
                  'grip': 'Grips', 'foregrip': 'Foregrips', 'handguard': 'Handguards', 'barrel': 'Barrels',
                  'underbarrel': 'Underbarrel', 'laser': 'Lasers', 'dustcover': 'Dust covers', 'ammo': 'Ammo'},
    },
    'uk_ua': {
        'itemGroup.exe_tacticals.workbenches': 'Верстати',
        'block.exe_tacticals.magazine_workbench': 'Верстат для магазинів',
        'block.exe_tacticals.attachment_workbench': 'Верстат для обвісів',
        'block.exe_tacticals.ammo_workbench': 'Верстат для набоїв',
        'category': {'magazine_ar': 'Магазин AR', 'magazine_ak': 'Магазин АК', 'sight': 'Приціл', 'rail': 'Планка',
                     'grip': 'Руків\'я', 'foregrip': 'Переднє руків\'я', 'barrel': 'Ствол', 'stock': 'Приклад',
                     'muzzle': 'Дульний пристрій', 'handguard': 'Цівка', 'underbarrel': 'Підствольник',
                     'laser': 'ЛЦВ', 'dustcover': 'Кришка', 'ammo': 'Набої',
                     'suppressor': 'Глушник'},
        'group': {'all': 'Усі', 'rifle': 'Автомати', 'magazine_ar': 'AR', 'magazine_ak': 'АК', 'sight': 'Приціли',
                  'rail': 'Планки', 'suppressor': 'Глушники', 'muzzle': 'Дульні', 'stock': 'Приклади',
                  'grip': 'Руків\'я', 'foregrip': 'Передні руків\'я', 'handguard': 'Цівки', 'barrel': 'Стволи',
                  'underbarrel': 'Підствольники', 'laser': 'ЛЦВ', 'dustcover': 'Кришки', 'ammo': 'Набої'},
    },
}
for lang, keys in LANG.items():
    p = A + 'lang/' + lang + '.json'
    data = json.load(open(p, encoding='utf-8'))
    for k, v in keys.items():
        if k in ('category', 'group'):
            for c, n in v.items():
                data['gui.exe_tacticals.workbench.' + k + '.' + c] = n
        else:
            data[k] = v
    with open(p, 'w', encoding='utf-8') as f:
        f.write(json.dumps(data, indent=2, ensure_ascii=False) + '\n')
print('ok')
