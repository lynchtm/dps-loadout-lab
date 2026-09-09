#!/usr/bin/env python3
# SPDX-License-Identifier: BSD-2-Clause
"""Adapt retained, locally authored/BSD UI files to replacement public contracts."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
for tree in ('main','test'):
    paths=list((root/'src'/tree/'java/com/dpscalc/scenario').glob('*.java'))
    paths+=list((root/'src'/tree/'java/com/dpscalc/wikisetups').rglob('*.java'))
    paths+=list((root/'src'/tree/'java/com/tommy').rglob('*.java'))
    if tree=='test':paths+=list((root/'src/test/java/com/dpscalc').glob('*.java'))
    for path in paths:
        text=path.read_text(encoding='utf-8-sig')
        text=text.replace('com.dpscalc.state.', 'com.loadoutlab.model.').replace('com.dpscalc.equipment.', 'com.loadoutlab.equipment.')
        text=text.replace('import com.dpscalc.data.*;', 'import com.loadoutlab.model.*;\nimport com.loadoutlab.data.*;')
        for cls in ('MonsterStats','MonsterInputs','MonsterAttribute','WeaknessElement'):
            text=text.replace('com.dpscalc.data.'+cls,'com.loadoutlab.model.'+cls)
        for cls in ('MonsterDataManager','MonsterScaling'):
            text=text.replace('com.dpscalc.data.'+cls,'com.loadoutlab.data.'+cls)
        text=text.replace('com.dpscalc.calc.', 'com.loadoutlab.calculation.')
        text=text.replace('com.dpscalc.DpsCalcPlugin','com.loadoutlab.DpsLoadoutLabPlugin').replace('DpsCalcPlugin','DpsLoadoutLabPlugin')
        if 'DpsLoadoutLabPlugin' in text and 'import com.loadoutlab.DpsLoadoutLabPlugin;' not in text:
            text=text.replace('package com.dpscalc;', 'package com.dpscalc;\nimport com.loadoutlab.DpsLoadoutLabPlugin;')
        path.write_text(text,encoding='utf-8')
