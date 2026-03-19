#!/usr/bin/env python3
import base64
import hashlib
import hmac
import sys


def verify_scrypt(encoded: str, raw: str) -> bool:
    method, salt, digest = encoded.split("$", 2)
    _, n_value, r_value, p_value = method.split(":")
    computed = hashlib.scrypt(
        raw.encode("utf-8"),
        salt=salt.encode("utf-8"),
        n=int(n_value),
        r=int(r_value),
        p=int(p_value),
        maxmem=1024 * 1024 * 1024,
    ).hex()
    return hmac.compare_digest(computed, digest)


def verify_pbkdf2(encoded: str, raw: str) -> bool:
    method, salt, digest = encoded.split("$", 2)
    _, algorithm, iterations = method.split(":")
    computed = hashlib.pbkdf2_hmac(
        algorithm,
        raw.encode("utf-8"),
        salt.encode("utf-8"),
        int(iterations),
    )

    try:
        expected = bytes.fromhex(digest)
    except ValueError:
        expected = base64.b64decode(digest)
    return hmac.compare_digest(computed, expected)


def main() -> int:
    if len(sys.argv) != 3:
        return 2

    encoded = sys.argv[1]
    raw = sys.argv[2]

    if encoded.startswith("scrypt:"):
        print("true" if verify_scrypt(encoded, raw) else "false")
        return 0
    if encoded.startswith("pbkdf2:"):
        print("true" if verify_pbkdf2(encoded, raw) else "false")
        return 0

    print("false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
