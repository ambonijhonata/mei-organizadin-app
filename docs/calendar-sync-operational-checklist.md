# Calendar Sync Operational Checklist

## Backend Error Contract Used By App

This app expects calendar sync errors from API `POST /api/calendar/sync` to follow the `GlobalExceptionHandler.ErrorResponse` shape:

```json
{
  "status": 403,
  "code": "INTEGRATION_REVOKED",
  "message": "Google integration requires re-authentication",
  "timestamp": "2026-04-05T12:00:00Z"
}
```

Contract source:
- `API/src/main/java/com/api/common/GlobalExceptionHandler.java`
- `INTEGRATION_REVOKED` -> HTTP 403
- `GOOGLE_API_FORBIDDEN` -> HTTP 403
- Unauthorized API token -> HTTP 401

## App Handling Semantics

- `401` on API calls:
  - Session invalidation interceptor clears local session.
  - User is redirected to login flow.
- `403` with `code=INTEGRATION_REVOKED`:
  - Session is not auto-cleared.
  - Calendar Home enters explicit `REAUTH_REQUIRED` state.
  - User sees `Reautenticar Google` action.
- `403` with `code=GOOGLE_API_FORBIDDEN`:
  - Session is not auto-cleared.
  - Sync is treated as recoverable failure.
  - Day list still loads from current persisted data.
- Sync technical failure (`5xx`, network, timeout):
  - Non-blocking warning is shown.
  - Day list continues with available data.
- Day list failure (`GET /api/calendar/events`):
  - Blocking `Erro ao carregar agenda` is shown.

## Manual Validation Checklist

1. Baseline navigation:
   - Login, open Calendar Home, switch previous/next day.
   - Confirm selected date changes and list updates.
2. Rapid day switching race:
   - Tap previous/next quickly 10+ times.
   - Confirm final rendered list matches the last selected date.
   - Confirm no return to login caused by stale requests.
3. Incremental sync fallback:
   - Force sync failure (temporary API/network failure).
   - Confirm warning is shown and day events are still listed.
4. Reauth required:
   - Revoke Google integration or force `INTEGRATION_REVOKED`.
   - Confirm explicit reauth state appears in Home.
   - Confirm `Reautenticar Google` action is visible.
5. Session handling:
   - Force `403 INTEGRATION_REVOKED` and verify session is preserved.
   - Force `401` and verify session is cleared and app returns to login.
6. Data freshness sanity:
   - Create/update event in Google Calendar.
   - Change day away and back.
   - Confirm updated/new event appears after incremental sync attempt.

## Debug Signals (Logs)

`CalendarHomeViewModel` emits structured logs with:
- requestId
- selected date
- sync outcome
- stale response drop events
- list failure details

Use these logs to diagnose concurrency and fallback behavior.
