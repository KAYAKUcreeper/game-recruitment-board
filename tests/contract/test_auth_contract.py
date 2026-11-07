from conftest import SCHEMA, BASE_URL, CHECKS, call, token
from hypothesis import settings, seed
import requests
import uuid


def test_success_flow(client, credentials):
    assert call(client, "/auth/csrf", "GET").status_code == 200
    registered = call(client, "/accounts", "POST", credentials)
    assert registered.status_code == 201
    assert not client.cookies.get("auth_token")
    login_body = {key: credentials[key] for key in ("loginId", "password")}
    assert call(client, "/auth/login", "POST", login_body).status_code == 200
    copied = client.cookies.get("auth_token")
    assert copied
    assert call(client, "/auth/me", "GET").status_code == 200
    assert call(client, "/auth/logout", "POST").status_code == 204
    assert not client.cookies.get("auth_token")
    client.cookies.set("auth_token", copied, path="/api")
    assert call(client, "/auth/me", "GET").status_code == 401


def test_error_cases(client, credentials):
    invalid = dict(credentials, loginId="aa")
    assert call(client, "/accounts", "POST", invalid).status_code == 400
    assert call(client, "/accounts", "POST", credentials).status_code == 201
    assert call(client, "/accounts", "POST", credentials).status_code == 409
    assert call(client, "/auth/login", "POST", {"loginId": credentials["loginId"], "password": "wrong-passphrase-12345"}).status_code == 401
    case = SCHEMA["/accounts"]["POST"].Case(body=credentials, media_type="application/json", headers={"X-XSRF-TOKEN": "incorrect"})
    assert case.call_and_validate(base_url=BASE_URL, session=client, checks=CHECKS).status_code == 403


@SCHEMA.parametrize()
@seed(20261010)
@settings(max_examples=20, deadline=None)
def test_generated(case):
    with requests.Session() as client:
        credentials = {"loginId": "ct_" + uuid.uuid4().hex[:20], "password": "contract-passphrase-12345", "displayName": "テスト利用者"}
        generated(case, client, credentials)


def generated(case, client, credentials):
    if case.path == "/auth/me":
        assert call(client, "/accounts", "POST", credentials).status_code == 201
        assert call(client, "/auth/login", "POST", {key: credentials[key] for key in ("loginId", "password")}).status_code == 200
    if case.method == "POST":
        case.headers = {**(case.headers or {}), "X-XSRF-TOKEN": token(client)}
        case.cookies = {"XSRF-TOKEN": client.cookies.get("XSRF-TOKEN")}
    elif case.path == "/auth/me":
        case.cookies = {"auth_token": client.cookies.get("auth_token")}
    response = case.call_and_validate(base_url=BASE_URL, session=client, timeout=10, checks=CHECKS)
    if case.method in ("GET", "POST"):
        assert response.headers.get("cache-control") == ["no-store"]
