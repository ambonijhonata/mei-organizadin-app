# Secure Session Keystore Recovery Runbook

## Purpose

Track and validate Android startup fallback behavior for encrypted session recovery when keystore/decryption integrity errors happen (`AEADBadTagException` family).

## Recovery Signal

The app emits structured events from `SecureSessionManager`:

- `session_secure_store_init_recoverable_failure`
- `session_secure_store_recovery_start`
- `session_secure_store_recovery_success`
- `session_secure_store_recovery_failed_second_attempt`
- `session_secure_store_init_recovery_failed`

## What to Monitor

- Count of recovery events per app version/device model/OS version.
- Ratio of recovery events to app cold starts.
- Post-recovery login completion rate.
- Repeated failures on same device (possible unrecoverable keystore issue).

## Validation Checklist

1. Confirm app does not crash at startup when secure storage is invalid.
2. Confirm user state transitions to logged out after recovery.
3. Confirm no sensitive token values are present in recovery logs.
4. Confirm normal startup path still restores session when secure storage is healthy.

## Escalation Guidance

- If `session_secure_store_recovery_failed_second_attempt` or `session_secure_store_init_recovery_failed` spikes:
  - collect device/OS distribution
  - verify backup/restore and lock-screen change patterns
  - prioritize hotfix if startup crash reappears.
