# /// script
# requires-python = ">=3.10"
# ///
"""Synthesizes the rest sounds (no third-party audio): a short countdown beep and a two-note "go"
chime, as 16-bit mono WAV in app/src/main/res/raw. Run: uv run tools/rest_sounds.py"""
import math
import struct
import wave
from pathlib import Path

RATE = 44_100
OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/raw"


def tone(freq: float, seconds: float, attack: float = 0.005, decay: float = 8.0, harmonics=((1, 1.0),)) -> list[float]:
    n = int(RATE * seconds)
    out = []
    for i in range(n):
        t = i / RATE
        env = min(1.0, t / attack) * math.exp(-decay * t)
        out.append(env * sum(a * math.sin(2 * math.pi * freq * h * t) for h, a in harmonics))
    return out


def write(name: str, samples: list[float], gain: float) -> None:
    peak = max(abs(s) for s in samples) or 1.0
    frames = b"".join(struct.pack("<h", int(32767 * gain * s / peak)) for s in samples)
    with wave.open(str(OUT / name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(frames)


def mix(*parts: tuple[float, list[float]]) -> list[float]:
    length = max(int(start * RATE) + len(p) for start, p in parts)
    out = [0.0] * length
    for start, p in parts:
        offset = int(start * RATE)
        for i, s in enumerate(p):
            out[offset + i] += s
    return out


bell = ((1, 1.0), (2, 0.35), (3, 0.12))
# Countdown: a clean, short beep.
write("rest_tick.wav", tone(1046.5, 0.14, decay=18.0, harmonics=((1, 1.0), (2, 0.15))), gain=0.8)
# Go: two rising bell notes (G5 → D6), the second one longer.
write("rest_go.wav", mix((0.0, tone(783.99, 0.35, decay=9.0, harmonics=bell)), (0.16, tone(1174.66, 0.75, decay=5.0, harmonics=bell))), gain=0.9)
print("written to", OUT)
