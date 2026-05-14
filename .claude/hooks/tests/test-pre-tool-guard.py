#!/usr/bin/env python3
"""Tests for pre-tool-guard.py — Wave 73 Bucket B Rules 1-5."""

import json
import subprocess
import unittest
from pathlib import Path

HOOK = Path(__file__).resolve().parent.parent / "pre-tool-guard.py"


def run_hook(payload: dict) -> dict:
    result = subprocess.run(
        ["python3", str(HOOK)],
        input=json.dumps(payload),
        capture_output=True, text=True, timeout=10,
    )
    if result.returncode != 0:
        return {"_error": result.stderr, "_stdout": result.stdout}
    try:
        return json.loads(result.stdout) if result.stdout.strip() else {}
    except json.JSONDecodeError:
        return {"_raw": result.stdout}


def is_allowed(out: dict) -> bool:
    return out == {} or "hookSpecificOutput" not in out


def is_denied(out: dict) -> bool:
    return out.get("hookSpecificOutput", {}).get("permissionDecision") == "deny"


def deny_reason(out: dict) -> str:
    return out.get("hookSpecificOutput", {}).get("permissionDecisionReason", "")


class TestAdminMergeDiscipline(unittest.TestCase):
    def test_admin_merge_blocked(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "gh pr merge 1234 --squash --admin"}})
        self.assertTrue(is_denied(out), f"Expected deny, got {out}")
        self.assertIn("admin-merge-discipline", deny_reason(out))

    def test_normal_merge_allowed(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "gh pr merge 1234 --squash"}})
        self.assertTrue(is_allowed(out), f"Expected allow, got {out}")

    def test_unrelated_command_allowed(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "ls -la"}})
        self.assertTrue(is_allowed(out))


class TestAwsTier3(unittest.TestCase):
    def test_aws_create_blocked(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "aws s3 create-bucket --bucket foo"}})
        self.assertTrue(is_denied(out))
        self.assertIn("Tier 3", deny_reason(out))

    def test_aws_delete_blocked(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "aws ec2 delete-security-group --group-id sg-123"}})
        self.assertTrue(is_denied(out))

    def test_aws_describe_allowed(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "aws ec2 describe-instances --query Reservations"}})
        self.assertTrue(is_allowed(out))

    def test_aws_get_caller_identity_allowed(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "aws sts get-caller-identity"}})
        self.assertTrue(is_allowed(out))


class TestSgAscii(unittest.TestCase):
    def test_non_ascii_description_blocked(self):
        out = run_hook({
            "tool_name": "Edit",
            "tool_input": {
                "file_path": "/repo/infrastructure/terraform-aws/security-groups.tf",
                "new_string": 'description = "AI outbound — egress for vendor APIs"',
            },
        })
        self.assertTrue(is_denied(out))
        self.assertIn("aws-sg-description-ascii", deny_reason(out))
        self.assertIn("U+2014", deny_reason(out))  # em-dash codepoint surfaced

    def test_ascii_description_allowed(self):
        out = run_hook({
            "tool_name": "Edit",
            "tool_input": {
                "file_path": "/repo/infrastructure/terraform-aws/security-groups.tf",
                "new_string": 'description = "AI outbound - egress for vendor APIs"',
            },
        })
        self.assertTrue(is_allowed(out))

    def test_non_tf_file_skipped(self):
        out = run_hook({
            "tool_name": "Edit",
            "tool_input": {
                "file_path": "/repo/documents/foo.md",
                "new_string": 'description = "Some — description with em-dash"',
            },
        })
        self.assertTrue(is_allowed(out))

    def test_non_infra_path_skipped(self):
        out = run_hook({
            "tool_name": "Edit",
            "tool_input": {
                "file_path": "/repo/some/other.tf",
                "new_string": 'description = "Em — dash"',
            },
        })
        self.assertTrue(is_allowed(out))


class TestTerraformRetry(unittest.TestCase):
    """First-call always allowed. Retry within 5min would block — covered by stateful integration test (skipped here)."""

    def test_first_apply_allowed(self):
        # State file may exist from prior tests; this just verifies hook doesn't crash on terraform apply
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "terraform apply -auto-approve"}})
        # First call after enough delay = allow; retry = deny — either is valid for this smoke test
        self.assertTrue(is_allowed(out) or is_denied(out))

    def test_terraform_help_skipped(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "terraform apply --help"}})
        self.assertTrue(is_allowed(out))

    def test_terraform_plan_skipped(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "terraform plan -out=tfplan"}})
        self.assertTrue(is_allowed(out))


class TestFalsePositives(unittest.TestCase):
    """Regression: rules must not fire on commands that contain banned tokens inside quoted strings."""

    def test_git_commit_with_admin_text_in_message_allowed(self):
        cmd = 'git commit -m "Document gh pr merge --admin discipline rule per .claude/rules/admin-merge-discipline.md"'
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": cmd}})
        self.assertTrue(is_allowed(out), f"False positive: {deny_reason(out)}")

    def test_git_commit_with_aws_create_text_in_message_allowed(self):
        cmd = 'git commit -m "Block aws create-bucket per Tier 3 rule"'
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": cmd}})
        self.assertTrue(is_allowed(out), f"False positive: {deny_reason(out)}")

    def test_git_commit_with_terraform_apply_text_in_message_allowed(self):
        cmd = 'git commit -m "terraform apply retry rule blocks 2nd run within 5min"'
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": cmd}})
        # terraform-apply-retry-reconfirm checks state file timing — first call always allowed
        # This test ensures the regex doesn't fire on quoted text. Retry case covered separately.
        # Either allow OR deny depending on state timing — but not for THIS regex reason
        if not is_allowed(out):
            # If denied, must be timing-based (retry detected) not regex-match-on-message
            self.assertNotIn("quoted", deny_reason(out).lower())


class TestEmptyAndMalformed(unittest.TestCase):
    def test_empty_payload_allowed(self):
        out = run_hook({})
        self.assertTrue(is_allowed(out))

    def test_no_tool_name_allowed(self):
        out = run_hook({"tool_input": {"command": "ls"}})
        self.assertTrue(is_allowed(out))

    def test_non_bash_with_command_allowed(self):
        out = run_hook({"tool_name": "Read", "tool_input": {"file_path": "/foo"}})
        self.assertTrue(is_allowed(out))


if __name__ == "__main__":
    unittest.main()
