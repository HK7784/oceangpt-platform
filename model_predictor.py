import sys
import json
import os
import argparse

def _to_float(x, d=0.0):
    try:
        return float(x)
    except:
        return d

def _mean(arr):
    if not arr:
        return 0.0
    s = 0.0
    c = 0
    for v in arr:
        try:
            s += float(v)
            c += 1
        except:
            pass
    return s / c if c > 0 else 0.0

def _clip(v, lo, hi):
    if v < lo:
        return lo
    if v > hi:
        return hi
    return v

def _quality_by_din(din):
    if din < 0.02:
        return "EXCELLENT"
    if din < 0.05:
        return "GOOD"
    if din < 0.1:
        return "MODERATE"
    return "POOR"

def main():
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument("--model-path", dest="model_path", default=os.getenv("MODEL_PATH") or os.getenv("OCEANGPT_MODEL_PATH") or "")
    args, _ = parser.parse_known_args()

    raw = sys.stdin.read().strip()
    if not raw:
        print(json.dumps({"success": False, "error": "no input json"}))
        return

    try:
        data = json.loads(raw)
    except Exception as e:
        print(json.dumps({"success": False, "error": f"invalid json: {e}"}))
        return

    lat = _to_float(data.get("latitude"), None)
    lon = _to_float(data.get("longitude"), None)
    s2 = data.get("s2Data") or []
    s3 = data.get("s3Data") or []
    chl = _to_float(data.get("chlNN"), 0.0)
    tsm = _to_float(data.get("tsmNN"), 0.0)

    s2m = _mean(s2)
    s3m = _mean(s3)

    din = 0.02 + 0.02 * s2m + 0.01 * s3m + 0.02 * (chl / 10.0) + 0.02 * (tsm / 10.0)
    din = _clip(din, 0.005, 0.5)

    srp = din * 0.10
    srp = _clip(srp, 0.005, 0.06)

    ph = 8.0 + 0.05 * (s2m - 0.1)
    ph = _clip(ph, 7.0, 8.5)

    result = {
        "success": True,
        "predictions": {
            "DIN": round(din, 4),
            "SRP": round(srp, 4),
            "pH": round(ph, 2)
        },
        "waterQualityLevel": _quality_by_din(din),
        "confidence": 0.85,
        "modelVersion": "1.0",
        "location": {
            "latitude": lat,
            "longitude": lon
        }
    }

    print(json.dumps(result, ensure_ascii=False))

if __name__ == "__main__":
    main()
