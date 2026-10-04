import argparse
import concurrent.futures
import json
import sys
import time
import uuid
from collections import Counter
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

TIMEOUT_SECONDS = 30


def http_json(method, url, payload=None, token=None):
    headers = {"Content-Type": "application/json"}

    if token:
        headers["Authorization"] = f"Bearer {token}"

    body = json.dumps(payload).encode() if payload is not None else None

    request = Request(
        url,
        data=body,
        headers=headers,
        method=method
    )

    start = time.perf_counter()

    try:
        with urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            raw = response.read().decode()
            try:
                data = json.loads(raw) if raw else {}
            except Exception:
                data = {"raw": raw}

            return {
                "status": response.status,
                "data": data,
                "latency_ms": (time.perf_counter() - start) * 1000
            }

    except HTTPError as e:
        raw = e.read().decode()

        try:
            data = json.loads(raw)
        except Exception:
            data = {"raw": raw}

        return {
            "status": e.code,
            "data": data,
            "latency_ms": (time.perf_counter() - start) * 1000
        }

    except Exception as e:
        return {
            "status": 599,
            "data": {"error": str(e)},
            "latency_ms": (time.perf_counter() - start) * 1000
        }


def create_show(base_url, seats):
    payload = {
        "name": f"burst-test-{uuid.uuid4()}",
        "seats": seats,
        "price_paise": 25000
    }

    result = http_json(
        "POST",
        f"{base_url}/shows",
        payload,
        token="burst-admin"
    )

    if result["status"] != 201:
        print(f"ERROR creating show: HTTP {result['status']}")
        print(json.dumps(result["data"], indent=2))
        sys.exit(1)

    return result["data"]["id"]


def reserve(base_url, show_id, seat, user_number):
    payload = {
        "seats": [seat],
        "idempotency_key": f"burst-{user_number}-{uuid.uuid4()}"
    }

    result = http_json(
        "POST",
        f"{base_url}/shows/{show_id}/reserve",
        payload,
        token=f"burst-user-{user_number}"
    )

    return result


def percentile(values, p):
    values = sorted(values)
    if not values:
        return 0

    index = int((p / 100) * (len(values) - 1))
    return values[index]


def run_requests(base_url, show_id, seats, users, concurrency, label):
    print()
    print("=" * 70)
    print(label)
    print("=" * 70)
    print(f"Requests:       {users}")
    print(f"Concurrency:    {concurrency}")
    print(f"Seats:          {len(seats)}")

    start = time.perf_counter()
    results = []

    with concurrent.futures.ThreadPoolExecutor(
        max_workers=min(concurrency, users)
    ) as executor:

        futures = []

        for i in range(users):
            seat = seats[i % len(seats)]

            futures.append(
                executor.submit(
                    reserve,
                    base_url,
                    show_id,
                    seat,
                    i + 1
                )
            )

        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    elapsed = time.perf_counter() - start

    counts = Counter(r["status"] for r in results)

    latencies = [r["latency_ms"] for r in results]

    server_errors = sum(
        count for status, count in counts.items()
        if status >= 500
    )

    print()
    print("HTTP outcomes:")

    for status, count in sorted(counts.items()):
        print(f"  HTTP {status}: {count}")

    print()
    print("Latency:")
    print(f"  P50: {percentile(latencies, 50):.2f} ms")
    print(f"  P95: {percentile(latencies, 95):.2f} ms")
    print(f"  P99: {percentile(latencies, 99):.2f} ms")
    print(f"  Max: {max(latencies):.2f} ms")

    throughput = users / elapsed

    print()
    print(f"Wall time:  {elapsed:.2f}s")
    print(f"Throughput: {throughput:.2f} req/s")
    print(f"5xx:        {server_errors}")

    if server_errors == 0:
        print()
        print(f"{label}: PASS")
        return True

    print()
    print(f"{label}: FAIL")
    return False


def reconcile(base_url, show_id):
    result = http_json(
        "GET",
        f"{base_url}/shows/{show_id}"
    )

    if result["status"] != 200:
        print(f"ERROR reading show: HTTP {result['status']}")
        return False

    show = result["data"]

    total = show["total"]
    available = show["available"]
    held = show["held"]
    confirmed = show["confirmed"]

    print()
    print("=" * 70)
    print("FINAL RECONCILIATION")
    print("=" * 70)

    print(f"Total:      {total}")
    print(f"Available:  {available}")
    print(f"Held:       {held}")
    print(f"Confirmed:  {confirmed}")

    calculated = available + held + confirmed

    print()
    print(
        f"{available} + {held} + {confirmed} = {calculated}"
    )

    if calculated == total:
        print("RECONCILIATION: PASS")
        return True

    print("RECONCILIATION: FAIL")
    return False


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument("base_url")

    parser.add_argument(
        "--users",
        type=int,
        default=500
    )

    parser.add_argument(
        "--concurrency",
        type=int,
        default=500
    )

    parser.add_argument(
        "--mode",
        choices=["hot", "mixed", "both"],
        default="both"
    )

    parser.add_argument(
        "--seats",
        type=int,
        default=5000
    )

    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")

    print("=" * 70)
    print("SEAT RESERVATION STRESS TEST")
    print("=" * 70)

    print(f"Base URL:       {base_url}")
    print(f"Requests:       {args.users}")
    print(f"Concurrency:    {args.concurrency}")
    print(f"Mode:           {args.mode}")

    passed = True

    if args.mode in ("hot", "both"):
        hot_show = create_show(
            base_url,
            ["A1"]
        )

        hot_passed = run_requests(
            base_url,
            hot_show,
            ["A1"],
            args.users,
            args.concurrency,
            "HOT-SEAT STORM"
        )

        reconcile_passed = reconcile(
            base_url,
            hot_show
        )

        passed = passed and hot_passed and reconcile_passed

    if args.mode in ("mixed", "both"):
        seats = [
            f"A{i}"
            for i in range(1, args.seats + 1)
        ]

        mixed_show = create_show(
            base_url,
            seats
        )

        mixed_passed = run_requests(
            base_url,
            mixed_show,
            seats,
            args.users,
            args.concurrency,
            "MIXED-SEAT STRESS"
        )

        reconcile_passed = reconcile(
            base_url,
            mixed_show
        )

        passed = passed and mixed_passed and reconcile_passed

    print()
    print("=" * 70)

    if passed:
        print("OVERALL RESULT: PASS")
        print("=" * 70)
        sys.exit(0)

    print("OVERALL RESULT: FAIL")
    print("=" * 70)
    sys.exit(1)


if __name__ == "__main__":
    main()