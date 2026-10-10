import os
import uuid
from pathlib import Path

import pytest
import requests
import schemathesis
from schemathesis import checks
from hypothesis import settings

ROOT = Path(__file__).resolve().parents[2]
SCHEMA = schemathesis.openapi.from_path(ROOT / "schema/auth/openapi.yaml")
CHECKS = [checks.not_a_server_error, checks.status_code_conformance, checks.content_type_conformance, checks.response_headers_conformance, checks.response_schema_conformance]
BASE_URL = os.getenv("CONTRACT_BASE_URL", "http://127.0.0.1:8081/api")
settings.register_profile("contract", max_examples=20, deadline=None, derandomize=True)
settings.load_profile("contract")


def token(session):
    response = session.get(BASE_URL + "/auth/csrf", timeout=10)
    assert response.status_code == 200
    value = response.json()["token"]
    assert value == session.cookies.get("XSRF-TOKEN")
    return value


def call(session, path, method, body=None):
    headers = {"X-XSRF-TOKEN": token(session)} if method == "POST" else {}
    kwargs = {"body": body, "media_type": "application/json"} if body is not None else {}
    case = SCHEMA[path][method].Case(headers=headers, **kwargs)
    response = case.call_and_validate(base_url=BASE_URL, session=session, timeout=10, checks=CHECKS)
    assert response.headers.get("cache-control") == ["no-store"]
    return response


@pytest.fixture
def client():
    # CONTRACT profile is bound to its own database; no development DB reset occurs here.
    with requests.Session() as session:
        yield session


@pytest.fixture
def credentials():
    return {"loginId": "ct_" + uuid.uuid4().hex[:20], "password": "contract-passphrase-12345", "displayName": "テスト利用者"}
