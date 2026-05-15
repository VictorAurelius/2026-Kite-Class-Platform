"""Unit tests for rotate_secret_handler.

Run via:
    python3 -m unittest \
      infrastructure.terraform-aws.lambdas.rotate-secret.tests.test_rotate_secret_handler

Or from inside the lambda dir:
    python3 -m unittest tests.test_rotate_secret_handler -v

No boto3 install needed — handler tolerates missing boto3 at import; tests
patch boto3.client with MagicMock. Validates:
  - generator output length + character set per secret kind
  - dispatcher routes the 4 lifecycle steps correctly
  - unknown step raises ValueError
  - finishSecret promotes AWSPENDING to AWSCURRENT
  - rotation-disabled secret rejected

Full integration test against AWS lives in scripts/test-secret-rotation.sh.
"""
import base64
import string
import sys
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import rotate_secret_handler as handler  # noqa: E402


class TestGenerator(unittest.TestCase):
    def test_jwt_secret_64_alphanum(self):
        value = handler._generate_secret_value("kitehub/production/jwt-secret")
        self.assertEqual(len(value), 64)
        allowed = set(string.ascii_letters + string.digits)
        self.assertTrue(set(value).issubset(allowed))

    def test_encryption_key_base64_32_bytes(self):
        value = handler._generate_secret_value("kitehub/production/encryption-key")
        decoded = base64.b64decode(value)
        self.assertEqual(len(decoded), 32)

    def test_seed_admin_password_32_chars(self):
        value = handler._generate_secret_value("kitehub/production/seed-admin-password")
        self.assertEqual(len(value), 32)

    def test_unknown_suffix_raises(self):
        with self.assertRaises(ValueError):
            handler._generate_secret_value("kitehub/production/unknown-thing")


class _FakeRNFExc(Exception):
    """Stand-in for boto3 ResourceNotFoundException."""


def _build_client(stage_map=None, current_value="current-val"):
    client = MagicMock()
    client.exceptions.ResourceNotFoundException = _FakeRNFExc
    client.describe_secret.return_value = {
        "RotationEnabled": True,
        "VersionIdsToStages": stage_map or {"tok": ["AWSPENDING"]},
    }

    def get_secret_value(SecretId, VersionStage=None, VersionId=None):
        if VersionStage == "AWSCURRENT":
            return {"SecretString": current_value}
        raise _FakeRNFExc("AWSPENDING not yet set")

    client.get_secret_value.side_effect = get_secret_value
    return client


class TestDispatch(unittest.TestCase):
    EVENT = {
        "SecretId": "arn:aws:secretsmanager:ap-southeast-1:123:secret:kitehub/production/jwt-secret-AbCdEf",
        "ClientRequestToken": "tok",
    }

    def test_create_step_puts_pending(self):
        client = _build_client()
        with patch.object(handler, "boto3", MagicMock(client=MagicMock(return_value=client))):
            handler.lambda_handler({**self.EVENT, "Step": "createSecret"}, None)
        client.put_secret_value.assert_called_once()
        kwargs = client.put_secret_value.call_args.kwargs
        self.assertEqual(kwargs["VersionStages"], ["AWSPENDING"])
        self.assertEqual(len(kwargs["SecretString"]), 64)

    def test_set_step_is_noop(self):
        client = _build_client()
        with patch.object(handler, "boto3", MagicMock(client=MagicMock(return_value=client))):
            handler.lambda_handler({**self.EVENT, "Step": "setSecret"}, None)
        client.put_secret_value.assert_not_called()

    def test_test_step_skipped_when_no_probe_url(self):
        client = _build_client()
        with (
            patch.object(handler, "boto3", MagicMock(client=MagicMock(return_value=client))),
            patch.dict("os.environ", {}, clear=True),
        ):
            handler.lambda_handler({**self.EVENT, "Step": "testSecret"}, None)

    def test_unknown_step_raises(self):
        client = _build_client()
        with (
            patch.object(handler, "boto3", MagicMock(client=MagicMock(return_value=client))),
            self.assertRaises(ValueError),
        ):
            handler.lambda_handler({**self.EVENT, "Step": "bogus"}, None)

    def test_rotation_disabled_raises(self):
        client = _build_client()
        client.describe_secret.return_value = {
            "RotationEnabled": False,
            "VersionIdsToStages": {"tok": ["AWSPENDING"]},
        }
        with (
            patch.object(handler, "boto3", MagicMock(client=MagicMock(return_value=client))),
            self.assertRaises(ValueError),
        ):
            handler.lambda_handler({**self.EVENT, "Step": "createSecret"}, None)

    def test_finish_step_promotes_pending(self):
        client = MagicMock()
        client.exceptions.ResourceNotFoundException = _FakeRNFExc
        client.describe_secret.return_value = {
            "RotationEnabled": True,
            "VersionIdsToStages": {
                "old": ["AWSCURRENT"],
                "tok": ["AWSPENDING"],
            },
        }
        with patch.object(handler, "boto3", MagicMock(client=MagicMock(return_value=client))):
            handler.lambda_handler({**self.EVENT, "Step": "finishSecret"}, None)
        client.update_secret_version_stage.assert_called_once()
        kwargs = client.update_secret_version_stage.call_args.kwargs
        self.assertEqual(kwargs["MoveToVersionId"], "tok")
        self.assertEqual(kwargs["RemoveFromVersionId"], "old")


if __name__ == "__main__":
    unittest.main()
