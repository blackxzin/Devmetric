"""Verify registration and activity CRUD through the frontend proxy."""
import json
import urllib.request
import uuid

BASE = "http://localhost:3000"

def request(path, method="GET", data=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    payload = None if data is None else json.dumps(data).encode()
    req = urllib.request.Request(BASE + path, data=payload, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=15) as response:
        raw = response.read()
        return response.status, json.loads(raw) if raw else None

with urllib.request.urlopen(BASE, timeout=15) as page:
    assert page.status == 200

email = f"smoke-{uuid.uuid4().hex}@example.com"
password = "Smoke-test-password-2026!"
status, auth = request("/api/v1/auth/register", "POST", {
    "email": email, "password": password, "displayName": "Smoke Test", "timezone": "UTC"
})
assert status == 201 and auth["success"], (status, auth)
_, login = request("/api/v1/auth/login", "POST", {"email": email, "password": password})
token = login["data"]["accessToken"]
status, created = request("/api/v1/activities", "POST", {
    "type": "STUDY", "title": "Smoke test"
}, token)
assert status == 201 and created["success"], (status, created)
activity_id = created["data"]["id"]
status, updated = request(f"/api/v1/activities/{activity_id}", "PUT", {
    "type": "STUDY", "title": "Updated smoke test"
}, token)
assert status == 200 and updated["data"]["title"] == "Updated smoke test"
status, _ = request(f"/api/v1/activities/{activity_id}", "DELETE", token=token)
assert status == 204
_, activities = request("/api/v1/activities", token=token)
assert all(item["id"] != activity_id for item in activities["data"])
print("Smoke test passed: nginx, registration, login and activity CRUD")
