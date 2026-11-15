# Трассирующие патроны: текстуры (копия обычного патрона с красным наконечником), модели предметов, переводы.
# Рецепты — в tools/workbench_variants.py (верстак для патронов).
# Запуск из корня проекта:  python tools/tracer_ammo.py
import json
from PIL import Image

A = 'src/main/resources/assets/exe_tacticals/'
CALIBERS = ['5_56x45', '5_45x39', '6_8x51', '7_62x39']
TIP_ROWS = 3       # сколько верхних пикселей пули красить
LABEL_TOP = 16     # с этой строки начинается жёлтая плашка с калибром


def tip_red(src, dst):
    im = Image.open(src).convert('RGBA')
    px = im.load()
    w, h = im.size
    opaque = lambda x, y: px[x, y][3] > 0

    # группы столбцов (пули) над плашкой
    cols = [x for x in range(w) if any(opaque(x, y) for y in range(LABEL_TOP))]
    groups, cur = [], []
    for x in cols:
        if cur and x != cur[-1] + 1:
            groups.append(cur)
            cur = []
        cur.append(x)
    if cur:
        groups.append(cur)

    for g in groups:
        top = min(y for x in g for y in range(LABEL_TOP) if opaque(x, y))
        for x in g:
            for y in range(top, min(top + TIP_ROWS, LABEL_TOP)):
                if not opaque(x, y):
                    continue
                r, gg, b, a = px[x, y]
                lum = (0.3 * r + 0.59 * gg + 0.11 * b) / 255
                # тёмный контур — тёмно-красный, светлые блики — светло-красные
                px[x, y] = (int(90 + 165 * min(1, lum * 1.3)), int(20 + 60 * lum), int(20 + 50 * lum), a)
    im.save(dst)


for c in CALIBERS:
    tip_red(A + 'textures/item/ammo_%s.png' % c, A + 'textures/item/ammo_%s_tracer.png' % c)
    with open(A + 'models/item/ammo_%s_tracer.json' % c, 'w', encoding='utf-8') as f:
        json.dump({'parent': 'minecraft:item/generated',
                   'textures': {'layer0': 'exe_tacticals:item/ammo_%s_tracer' % c}}, f, indent=2)
        f.write('\n')

NAMES = {
    'ru_ru': ('Трассер %s мм', 'Заряжено:'),
    'en_us': ('%s mm Tracer Ammo', 'Loaded:'),
    'uk_ua': ('Трасер %s мм', 'Заряджено:'),
}
PRETTY = {'5_56x45': ('5,56×45', '5.56×45'), '5_45x39': ('5,45×39', '5.45×39'),
          '6_8x51': ('6,8×51', '6.8×51'), '7_62x39': ('7,62×39', '7.62×39')}
for lang, (fmt, loaded) in NAMES.items():
    p = A + 'lang/' + lang + '.json'
    data = json.load(open(p, encoding='utf-8'))
    for c in CALIBERS:
        data['item.exe_tacticals.ammo_%s_tracer' % c] = fmt % PRETTY[c][1 if lang == 'en_us' else 0]
    data['tooltip.exe_tacticals.magazine_loaded'] = loaded
    with open(p, 'w', encoding='utf-8') as f:
        f.write(json.dumps(data, indent=2, ensure_ascii=False) + '\n')
print('ok')
