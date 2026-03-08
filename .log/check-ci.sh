#!/bin/bash
# Auto-check latest CI status from GitHub Actions
REPO="VictorAurelius/2026-Kite-Class-Platform"
RUN_ID=${1:-"latest"}

if [ "$RUN_ID" = "latest" ]; then
    RUN_DATA=$(curl -s "https://api.github.com/repos/$REPO/actions/runs?per_page=1")
    RUN_ID=$(echo "$RUN_DATA" | python3 -c "import json,sys; print(json.load(sys.stdin)['workflow_runs'][0]['id'])")
fi

curl -s "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID" > /tmp/ci-run.json
curl -s "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/jobs" > /tmp/ci-jobs.json

python3 << 'EOPY'
import json
with open('/tmp/ci-run.json') as f:
    run = json.load(f)
with open('/tmp/ci-jobs.json') as f:
    jobs = json.load(f)

print("=" * 70)
print(f"Workflow: {run['name']}")
print(f"Run: #{run['run_number']}")
print(f"Status: {run['status']} | Conclusion: {run['conclusion']}")
print(f"Commit: {run['head_sha'][:7]} - {run['head_commit']['message'].split(chr(10))[0]}")
print(f"URL: {run['html_url']}")
print("=" * 70)

for job in jobs['jobs']:
    icon = "✅" if job['conclusion'] == 'success' else "❌" if job['conclusion'] == 'failure' else "⏭️"
    print(f"\n{icon} {job['name']}: {job['conclusion']}")
    if job['conclusion'] == 'failure':
        for step in job['steps']:
            if step.get('conclusion') == 'failure':
                print(f"   ❌ Failed: {step['name']}")
EOPY
