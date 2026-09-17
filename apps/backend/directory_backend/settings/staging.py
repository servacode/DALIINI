from .production import *  # noqa: F403

# Staging intentionally inherits the production fail-closed security posture.
# Provider hostnames may be used until ROOT_DOMAIN is purchased, but all
# origins/hosts must still be explicit environment values.
