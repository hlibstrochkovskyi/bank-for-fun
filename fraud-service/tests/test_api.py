from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "UP"


def test_score_allows_a_small_transfer():
    r = client.post(
        "/score",
        json={"amount_minor": 5_000, "currency": "USD", "from_account": "a", "to_account": "b"},
    )
    assert r.status_code == 200
    body = r.json()
    assert body["action"] == "ALLOW"
    assert 0.0 <= body["score"] <= 1.0


def test_score_holds_a_large_transfer():
    r = client.post(
        "/score",
        json={"amount_minor": 2_000_000, "currency": "USD", "from_account": "a", "to_account": "b"},
    )
    assert r.status_code == 200
    body = r.json()
    assert body["action"] == "HOLD"
    assert body["reasons"]


def test_score_rejects_negative_amount():
    r = client.post(
        "/score",
        json={"amount_minor": -1, "currency": "USD", "from_account": "a", "to_account": "b"},
    )
    assert r.status_code == 422
