"""
KiteHub custom secret rotation handler.

Implements AWS Secrets Manager 4-step rotation lifecycle:
  createSecret  -> generate AWSPENDING staging label with new random value
  setSecret     -> no-op for in-house secrets (no external service to update)
  testSecret    -> optional HTTP health probe (skipped if PROBE_URL unset)
  finishSecret  -> move AWSCURRENT to the new version, demote previous to AWSPREVIOUS

Scope (in-house secrets only):
  - kitehub/<env>/jwt-secret         (64-char alphanum)
  - kitehub/<env>/encryption-key     (32-byte base64)
  - kitehub/<env>/seed-admin-password (32-char strong)

RDS db-password rotation uses AWS-managed
SecretsManagerRDSPostgreSQLRotationSingleUser Lambda — wired separately via
aws_secretsmanager_secret_rotation against that managed function. This custom
handler does NOT touch RDS secrets.

External vendor API keys (Cloudflare, Resend) are NOT auto-rotated; they
require coordination with the vendor portal and are documented as manual
quarterly rotation in documents/05-guides/operations/secrets-rotation-runbook.md.

References:
  https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets-lambda-function-overview.html
  https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets-managed-rotation-rds.html
"""

import base64
import logging
import os
import secrets
import string
from typing import Any

try:
    import boto3  # type: ignore[import-not-found]
except ImportError:  # pragma: no cover - test envs mock boto3
    boto3 = None  # type: ignore[assignment]

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Secret name suffix -> generator strategy.
SECRET_GENERATORS: dict[str, dict[str, Any]] = {
    "jwt-secret": {"kind": "alphanum", "length": 64},
    "encryption-key": {"kind": "base64-bytes", "byte_length": 32},
    "seed-admin-password": {"kind": "strong", "length": 32},
}


def _generator_suffix(secret_identifier: str) -> str:
    """Extract logical suffix from a secret Name OR ARN.

    For ARN `arn:aws:secretsmanager:...:secret:kitehub/production/jwt-secret-AbCdEf`
    AWS appends 6 random chars after the configured Name. Strip them.
    Logical Name `kitehub/production/jwt-secret` returns `jwt-secret`.
    """
    last_path_seg = secret_identifier.rsplit("/", 1)[-1]
    # If ARN-style trailing random suffix `-XXXXXX` (6 alphanumeric), strip it.
    if len(last_path_seg) > 7 and last_path_seg[-7] == "-":
        candidate = last_path_seg[:-7]
        if candidate in SECRET_GENERATORS:
            return candidate
    return last_path_seg


def _generate_secret_value(secret_identifier: str) -> str:
    """Pick generator strategy based on secret logical suffix."""
    suffix = _generator_suffix(secret_identifier)
    spec = SECRET_GENERATORS.get(suffix)
    if spec is None:
        raise ValueError(
            f"No generator registered for secret suffix '{suffix}'. "
            f"Custom rotation supports: {sorted(SECRET_GENERATORS.keys())}. "
            "RDS db-password rotation must use the AWS-managed Lambda."
        )

    kind = spec["kind"]
    if kind == "alphanum":
        alphabet = string.ascii_letters + string.digits
        return "".join(secrets.choice(alphabet) for _ in range(spec["length"]))
    if kind == "base64-bytes":
        raw = secrets.token_bytes(spec["byte_length"])
        return base64.b64encode(raw).decode("ascii")
    if kind == "strong":
        # 32-char mixed-case + digits + safe symbols (no shell-meta, no quotes).
        alphabet = string.ascii_letters + string.digits + "!@#%^&*()-_=+"
        return "".join(secrets.choice(alphabet) for _ in range(spec["length"]))
    raise ValueError(f"Unknown generator kind: {kind}")


def lambda_handler(event: dict[str, Any], context: Any) -> None:
    """AWS Secrets Manager rotation entry point."""
    arn = event["SecretId"]
    token = event["ClientRequestToken"]
    step = event["Step"]

    client = boto3.client("secretsmanager")

    metadata = client.describe_secret(SecretId=arn)
    if not metadata.get("RotationEnabled", False):
        logger.error("Secret %s rotation is not enabled", arn)
        raise ValueError(f"Secret {arn} is not enabled for rotation")

    versions = metadata.get("VersionIdsToStages", {})
    if token not in versions:
        logger.error("Secret version %s has no stage for rotation of secret %s", token, arn)
        raise ValueError(f"Secret version {token} has no stage for rotation of secret {arn}")
    if "AWSCURRENT" in versions[token]:
        logger.info("Secret version %s already set as AWSCURRENT for %s", token, arn)
        return
    if "AWSPENDING" not in versions[token]:
        logger.error(
            "Secret version %s not set as AWSPENDING for rotation of secret %s", token, arn
        )
        raise ValueError(
            f"Secret version {token} not set as AWSPENDING for rotation of secret {arn}"
        )

    dispatchers = {
        "createSecret": _create_secret,
        "setSecret": _set_secret,
        "testSecret": _test_secret,
        "finishSecret": _finish_secret,
    }
    handler = dispatchers.get(step)
    if handler is None:
        raise ValueError(f"Invalid step parameter: {step}")
    handler(client, arn, token)


def _create_secret(client, arn: str, token: str) -> None:
    """Generate new value, store as AWSPENDING staging label."""
    client.get_secret_value(SecretId=arn, VersionStage="AWSCURRENT")
    try:
        client.get_secret_value(SecretId=arn, VersionId=token, VersionStage="AWSPENDING")
        logger.info("createSecret: AWSPENDING already exists for %s version %s", arn, token)
        return
    except client.exceptions.ResourceNotFoundException:
        pass

    new_value = _generate_secret_value(arn)
    client.put_secret_value(
        SecretId=arn,
        ClientRequestToken=token,
        SecretString=new_value,
        VersionStages=["AWSPENDING"],
    )
    logger.info("createSecret: staged new AWSPENDING version for %s", arn)


def _set_secret(client, arn: str, token: str) -> None:
    """No-op for in-house secrets.

    Spring Boot reads JWT / encryption-key / seed-admin secrets via env-var
    injection at boot (fetch-secrets.sh + secrets-rotation-runbook.md §7).
    Rotated value picked up at next service reload, scheduled via EC2 SSM
    SendCommand triggered by post-rotation alarm.
    """
    logger.info("setSecret: no-op for in-house secret %s (no external dependency)", arn)


def _test_secret(client, arn: str, token: str) -> None:
    """Optional HTTP probe gated on PROBE_URL env var.

    Phase 1 BETA: PROBE_URL unset; rely on out-of-band service reload + health
    check loop. When PROBE_URL set, expect HTTP 200; non-200 raises and rotation
    aborts (AWSPENDING NOT promoted to AWSCURRENT).
    """
    probe_url: str | None = os.environ.get("PROBE_URL")
    if not probe_url:
        logger.info("testSecret: PROBE_URL unset; skipping probe for %s", arn)
        return

    import urllib.request  # lazy import keeps cold start fast for no-probe path

    pending = client.get_secret_value(
        SecretId=arn, VersionId=token, VersionStage="AWSPENDING"
    )
    if not pending.get("SecretString"):
        raise ValueError("AWSPENDING version has no SecretString")

    try:
        with urllib.request.urlopen(probe_url, timeout=5) as resp:  # noqa: S310
            if resp.status != 200:
                raise ValueError(
                    f"Probe {probe_url} returned HTTP {resp.status}; rotation aborted"
                )
        logger.info("testSecret: probe %s returned 200 for %s", probe_url, arn)
    except Exception as exc:  # noqa: BLE001
        logger.error("testSecret: probe failed for %s: %s", arn, exc)
        raise


def _finish_secret(client, arn: str, token: str) -> None:
    """Promote AWSPENDING -> AWSCURRENT; demote previous AWSCURRENT to AWSPREVIOUS."""
    metadata = client.describe_secret(SecretId=arn)
    current_version: str | None = None
    for version_id, stages in metadata.get("VersionIdsToStages", {}).items():
        if "AWSCURRENT" in stages:
            if version_id == token:
                logger.info("finishSecret: %s already current for %s", token, arn)
                return
            current_version = version_id
            break

    client.update_secret_version_stage(
        SecretId=arn,
        VersionStage="AWSCURRENT",
        MoveToVersionId=token,
        RemoveFromVersionId=current_version,
    )
    logger.info(
        "finishSecret: promoted %s to AWSCURRENT for %s (was %s)",
        token,
        arn,
        current_version,
    )
