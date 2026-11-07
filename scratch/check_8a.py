import json

with open(r'C:\Users\barab\.gemini\antigravity\brain\8a05b4cb-4d7d-4fa6-b22c-a8ebdf4125fb\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        d = json.loads(line)
        idx = d.get('step_index', 0)
        if idx >= 400:
            calls = d.get('tool_calls')
            if calls:
                for c in calls:
                    t = c.get('args', {}).get('Path') or c.get('args', {}).get('CommandLine', '')
                    print(f"Step {idx}: {c.get('name')} -> {str(t)[:90]}")
            elif d.get('type') == 'PLANNER_RESPONSE' and d.get('content'):
                print(f"Step {idx}: PLANNER_RESPONSE: {d.get('content')[:120]}")
