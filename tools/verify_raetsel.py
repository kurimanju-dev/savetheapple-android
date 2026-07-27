#!/usr/bin/env python3
"""Prueft die Raetsellogik von Escape the KAG gegen den Quelltext.

Liest MainActivity.kt, extrahiert die tatsaechlichen Werte und rechnet nach.
Nichts ist hier hartkodiert ausser den Erwartungen, die im Spiel dokumentiert sind.
"""
import re
import sys
from pathlib import Path

SRC = Path("/Users/luisweitl/Programming/Projects/savetheapple-android/"
           "app/src/main/java/de/kagerding/savetheapple/android/MainActivity.kt")
text = SRC.read_text(encoding="utf-8")

ok, warn, fail = [], [], []


def num(word):
    """Buchstaben -> Alphabetposition, aneinandergehaengt."""
    return "".join(str(ord(c) - 64) for c in word.upper())


# ---------------------------------------------------------------- Knotendaten
nodes = {}
for m in re.finditer(r'id\s*=\s*"([a-z_]+)"(.*?)(?=\n    \),\n|\n\)\n)', text, re.S):
    nid, body = m.group(1), m.group(2)
    ans = re.search(r'acceptedAnswers\s*=\s*setOf\(([^)]*)\)', body)
    answers = re.findall(r'"([^"]*)"', ans.group(1)) if ans else []
    hints = re.search(r'hints\s*=\s*listOf\((.*?)\n        \),', body, re.S)
    nodes[nid] = {
        "answers": answers,
        "hints": re.findall(r'"((?:[^"\\]|\\.)*)"', hints.group(1)) if hints else [],
    }

order = [m.group(1) for m in re.finditer(r'id\s*=\s*"([a-z_]+)"', text)]
print(f"Gefundene Knoten ({len(nodes)}): {', '.join(nodes)}\n")

wordle_target = re.search(r'WORDLE_TARGET\s*=\s*"([A-Z]+)"', text).group(1)

# ------------------------------------------------------- 1. Master-Code pruefen
compile_answer = nodes["compile"]["answers"][0]
fragments = ["FREIHEIT", "1994", "60", wordle_target]
built = num("FREIHEIT") + "1994" + "60" + num(wordle_target)
(ok if built == compile_answer else fail).append(
    f"Master-Code: FREIHEIT+1994+60+{wordle_target} = {built} "
    f"{'==' if built == compile_answer else '!='} Code im Quelltext ({compile_answer})")
(ok if len(compile_answer) == 24 else fail).append(
    f"Master-Code ist {len(compile_answer)} Ziffern lang (im Raetseltext steht: 24)")

# --------------------------------------------------------- 2. Tutorial pruefen
kag_sum = sum(ord(c) - 64 for c in "KAG")
tut = nodes["tutorial"]["answers"][0]
expected_tut = "4" + str(kag_sum)
(ok if expected_tut == tut else fail).append(
    f"Tutorial: 4 Baeume + Buchstabenwert KAG ({kag_sum}) = {expected_tut} "
    f"{'==' if expected_tut == tut else '!='} {tut}")

# ------------------------------------------------------ 3. KAG-Gleichung pruefen
kag = int(nodes["kag_formula"]["answers"][0])
X, G = 8122, 4          # Vorwahl Erding ohne Null / letzte Ziffer von 2004
produkt = kag - X
if produkt % G == 0:
    ka = produkt // G
    ok.append(f"KAG-Gleichung: (K*A*G)+{X} = {kag}  =>  K*A*G = {produkt}, "
              f"mit G={G} folgt K*A = {ka}")
    faktoren = [(k, ka // k) for k in range(1, ka + 1) if ka % k == 0]
    ok.append(f"  Moegliche Zaehlwerte K*A = {ka}: " +
              ", ".join(f"{k}x{a}" for k, a in faktoren))
else:
    fail.append(f"KAG-Gleichung: {kag}-{X} = {produkt} ist nicht durch G={G} teilbar!")

# ----------------------------------------------------------- 4. Finale pruefen
locks = re.findall(r'FinaleLock\(\s*label\s*=\s*"([^"]+)".*?answer\s*=\s*"([^"]+)"',
                   text, re.S)
lock_answers = [a for _, a in locks]
ok.append(f"Finale hat {len(locks)} Riegel: {', '.join(lock_answers)}")

node_answers = {a for n in nodes.values() for a in n["answers"]} | {wordle_target}
for label, ans in locks:
    if ans in node_answers:
        ok.append(f"  Riegel '{label}' -> {ans}: kommt so im Spiel vor")
    elif compile_answer.endswith(ans):
        ok.append(f"  Riegel '{label}' -> {ans}: sind die letzten {len(ans)} "
                  f"Ziffern des Master-Codes (korrekt)")
    else:
        fail.append(f"  Riegel '{label}' -> {ans}: kommt NIRGENDS im Spiel vor!")

# ----------------------------------------------------- 5. ERDING-Matrix pruefen
cols = re.search(r"ERDING_COLUMNS\s*=\s*listOf\(([^)]*)\)", text).group(1)
columns = re.findall(r"'([A-Z])'", cols)
rows_total = int(re.search(r"ERDING_ROWS\s*=\s*(\d+)", text).group(1))

task_block = re.search(r"ERDING_TASKS\s*=\s*listOf\((.*?)\n\)\n", text, re.S).group(1)
tasks = []
for tm in re.finditer(r'label\s*=\s*"([^"]+)".*?cells\s*=\s*([^\n]+)', task_block, re.S):
    label, cell_expr = tm.group(1), tm.group(2)
    cells = set()
    rng = re.search(r"\((\d+)\.\.(\d+)\)\.map\s*\{\s*row\s*->\s*(\d+) to row", cell_expr)
    if rng:
        lo, hi, col = int(rng.group(1)), int(rng.group(2)), int(rng.group(3))
        cells = {(col, r) for r in range(lo, hi + 1)}
    else:
        cells = {(int(a), int(b)) for a, b in re.findall(r"(\d+) to (\d+)", cell_expr)}
    tasks.append((label, cells))

all_cells = set()
overlap = set()
for label, cells in tasks:
    dup = all_cells & cells
    if dup:
        overlap |= dup
    all_cells |= cells
    coords = ", ".join(f"{columns[c]}{r}" for c, r in sorted(cells, key=lambda x: (x[1], x[0])))
    ok.append(f"  {label}: {len(cells)} Felder -> {coords}")

(ok if not overlap else fail).append(
    f"Matrix: Aufgaben ueberschneiden sich nicht" if not overlap
    else f"Matrix: Aufgaben ueberschneiden sich bei {overlap}!")

bad = [(c, r) for c, r in all_cells
       if not (0 <= c < len(columns)) or not (1 <= r <= rows_total)]
(ok if not bad else fail).append(
    "Matrix: alle Felder liegen im Raster" if not bad
    else f"Matrix: Felder ausserhalb des Rasters: {bad}")

print("Von den Aufgaben erzeugtes Muster:\n")
for r in range(1, rows_total + 1):
    line = "  " + " ".join("#" if (c, r) in all_cells else "." for c in range(len(columns)))
    print(f"{line}    Zeile {r}")
print("  " + " ".join(columns) + "\n")

matrix_answer = nodes["erding_matrix"]["answers"][0]
ok.append(f"Matrix: {len(all_cells)} Felder insgesamt, erwartete Eingabe '{matrix_answer}'")

# ------------------------------------------------- 6. Raumnummern durchrechnen
room_answer = nodes["room_sorting"]["answers"][0]
print(f"Raumnummern-Code laut Quelltext: {room_answer}\n")
print("Alle Zerlegungen in 2-4 Raumnummern, die sortiert genau diesen Code ergeben:")


def partitions(s, parts):
    if parts == 1:
        yield [s]
        return
    for i in range(1, len(s)):
        for rest in partitions(s[i:], parts - 1):
            yield [s[:i]] + rest


found = 0
for n_parts in (2, 3, 4):
    for p in partitions(room_answer, n_parts):
        # Sortiert nach Zahlenwert muss die Reihenfolge exakt so bleiben
        vals = [int(x) for x in p]
        if vals != sorted(vals):
            continue
        if len(set(vals)) != len(vals):
            continue           # gleiche Raumnummer zweimal waere unsinnig
        found += 1
        print(f"  {n_parts} Raeume: " + " < ".join(p) +
              f"   (Zahlenwerte {', '.join(str(v) for v in vals)})")
print(f"  -> {found} moegliche Kombinationen\n")

# ------------------------------------------------------------ 7. Hinweise pruefen
for nid, data in nodes.items():
    for h in data["hints"]:
        for a in data["answers"]:
            if a in h:
                break
    else:
        continue
for nid, data in nodes.items():
    if data["answers"] and data["hints"]:
        last = data["hints"][-1]
        if not any(a in last for a in data["answers"]):
            warn.append(f"Knoten '{nid}': letzter Hinweis nennt die Loesung "
                        f"{data['answers']} nicht woertlich -> Spicken funktioniert evtl. nicht")
        else:
            ok.append(f"Knoten '{nid}': letzter Hinweis nennt die Loesung")

# ------------------------------------------------------------------- Ausgabe
print("=" * 70)
for line in ok:
    print("OK    " + line)
for line in warn:
    print("WARN  " + line)
for line in fail:
    print("FEHL  " + line)
print("=" * 70)
print(f"{len(ok)} ok, {len(warn)} Warnungen, {len(fail)} Fehler")
sys.exit(1 if fail else 0)
