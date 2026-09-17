# Android Play RC workspace

This directory contains policy inputs, release templates and source checks only. It contains no signing key, Play credential, production secret or fabricated store screenshot.

A Play RC is not qualified until:
- P20 connected quality passes.
- a real release AAB is built and signed using secrets outside Git.
- the AAB is installed from a Play track and device-tested.
- Data Safety/app-content forms are reconciled with the exact shipped artifact.
- account deletion works in-app and the external URL is live.
