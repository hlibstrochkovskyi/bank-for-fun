from app.scoring import HIGH_AMOUNT_THRESHOLD_MINOR, score_transaction


def test_small_normal_transfer_is_allowed():
    d = score_transaction(amount_minor=5_000, currency="USD", new_payee=False, recent_transfer_count=1)
    assert d.action == "ALLOW"
    assert d.score < 0.7


def test_large_amount_is_held():
    d = score_transaction(
        amount_minor=HIGH_AMOUNT_THRESHOLD_MINOR, currency="USD", new_payee=False, recent_transfer_count=0
    )
    assert d.action == "HOLD"
    assert any("amount" in r.lower() for r in d.reasons)


def test_high_velocity_is_held():
    d = score_transaction(amount_minor=5_000, currency="USD", new_payee=False, recent_transfer_count=15)
    assert d.action == "HOLD"
    assert any("velocity" in r.lower() for r in d.reasons)


def test_new_payee_with_midsize_amount_adds_risk_but_is_not_held_alone():
    d = score_transaction(amount_minor=200_000, currency="USD", new_payee=True, recent_transfer_count=0)
    assert d.score > 0
    assert d.action == "ALLOW"


def test_new_payee_plus_large_amount_is_held():
    d = score_transaction(amount_minor=1_000_000, currency="USD", new_payee=True, recent_transfer_count=0)
    assert d.action == "HOLD"


def test_score_is_clamped_between_zero_and_one():
    d = score_transaction(amount_minor=10_000_000, currency="USD", new_payee=True, recent_transfer_count=50)
    assert 0.0 <= d.score <= 1.0
    assert d.action == "HOLD"
