#!/usr/bin/env bash
# ECR prune — delete orphaned cosign sig/att + truly-untagged + old sha images.
#
# Keep-set (per repo, by DIGEST):
#   - latest/main tags
#   - N most-recent sha-* images (default 20)
#   - .sig/.att cosign signatures of the kept digests
#   - CHILD platform manifests of any kept manifest-list (multi-arch index) —
#     these are untagged but referenced; ECR (correctly) refuses to delete them
#     while the parent index is kept, so we keep them explicitly to converge.
# Everything else is deleted.
#
# Terraform-safe: operates on image contents only; does not touch the repository
# resource or its lifecycle policy (both terraform-managed in ecr.tf).
#
# Usage:
#   bash scripts/aws/ecr-prune.sh            # dry-run (default) — prints counts only
#   bash scripts/aws/ecr-prune.sh --apply    # actually batch-delete
#
# Env: PROFILE (default dev-admin), REGION (default ap-southeast-1), KEEP_SHA (default 20)
set -euo pipefail

export PRUNE_PROFILE="${PROFILE:-dev-admin}"
export PRUNE_REGION="${REGION:-ap-southeast-1}"
export PRUNE_KEEP_SHA="${KEEP_SHA:-20}"
export PRUNE_APPLY=0
[ "${1:-}" = "--apply" ] && export PRUNE_APPLY=1

python3 - <<'PY'
import json, os, subprocess, sys

PROFILE = os.environ["PRUNE_PROFILE"]
REGION  = os.environ["PRUNE_REGION"]
KEEP    = int(os.environ["PRUNE_KEEP_SHA"])
APPLY   = os.environ["PRUNE_APPLY"] == "1"
BASE    = ["aws", "--profile", PROFILE, "--region", REGION, "ecr"]

def aws(*args, _in=None):
    r = subprocess.run(BASE + list(args), capture_output=True, text=True, input=_in)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.strip())
    return r.stdout

def tags(im): return im.get("imageTags") or []

repos = aws("describe-repositories", "--query", "repositories[].repositoryName",
            "--output", "text").split()

g_keep = g_del = g_fail = 0
for repo in sorted(repos):
    data = json.loads(aws("describe-images", "--repository-name", repo, "--output", "json"))["imageDetails"]
    by_digest = {im["imageDigest"]: im for im in data}

    sha = sorted((im for im in data if any(t.startswith("sha-") for t in tags(im))),
                 key=lambda im: im.get("imagePushedAt", ""), reverse=True)
    keep = set()
    for im in data:
        if any(t in ("latest", "main") for t in tags(im)):
            keep.add(im["imageDigest"])
    for im in sha[:KEEP]:
        keep.add(im["imageDigest"])
    hexk = {d.split(":")[1] for d in keep}
    for im in data:
        for t in tags(im):
            if (t.endswith(".sig") or t.endswith(".att")) and t.startswith("sha256-") \
               and t[len("sha256-"):].split(".")[0] in hexk:
                keep.add(im["imageDigest"]); break

    # Resolve children of kept manifest-lists (multi-arch index) so we don't try to
    # delete referenced platform manifests (ECR blocks that → never converges).
    list_mt = ("manifest.list", "image.index")
    kept_lists = [d for d in list(keep)
                  if any(m in (by_digest[d].get("imageManifestMediaType") or "") for m in list_mt)]
    for d in kept_lists:
        try:
            out = json.loads(aws("batch-get-image", "--repository-name", repo,
                                 "--image-ids", f"imageDigest={d}", "--output", "json"))
            for img in out.get("images", []):
                man = json.loads(img["imageManifest"])
                for child in man.get("manifests", []):
                    if child.get("digest"):
                        keep.add(child["digest"])
        except Exception:
            pass

    dele = [im for im in data if im["imageDigest"] not in keep]
    n_keep, n_del, n_fail = len(data) - len(dele), len(dele), 0

    if APPLY and dele:
        ids = [{"imageDigest": im["imageDigest"]} for im in dele]
        for i in range(0, len(ids), 100):
            chunk = ids[i:i+100]
            out = json.loads(aws("batch-delete-image", "--repository-name", repo,
                                 "--image-ids", json.dumps(chunk), "--output", "json"))
            n_fail += len(out.get("failures", []))

    print(f"  {repo:30s} keep={n_keep:<4d} delete={n_del}" + (f"  fail={n_fail}" if n_fail else ""))
    g_keep += n_keep; g_del += n_del; g_fail += n_fail

print("------")
mode = "APPLIED" if APPLY else "DRY-RUN"
print(f"{mode} — total keep={g_keep} delete={g_del}" + (f" fail={g_fail}" if g_fail else ""))
PY
