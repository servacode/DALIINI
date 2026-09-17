# P20 Release Quality

This directory is the executable gate harness for `P20 RELEASE QUALITY PASS`.

Local source qualification:

```bash
python infrastructure/quality/release_quality.py
```

Release-candidate enforcement fails closed when connected inputs are absent:

```bash
python infrastructure/quality/release_quality.py --require-connected
```

Connected staging:

```bash
python infrastructure/quality/golden_path.py --origin "$STAGING_API_ORIGIN"
python infrastructure/quality/load_baseline.py --origin "$STAGING_API_ORIGIN" \
  --path /health/ready/ --path /api/v1/public/provinces/ --requests 200 --concurrency 10
python infrastructure/quality/restore_evidence.py "$P20_RESTORE_EVIDENCE"
```

No credentials belong in this repository. The golden path receives disposable staging identities and OTP through environment variables.
