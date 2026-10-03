from django.db import models


class ServiceSignal(models.Model):
    """When a background service last proved it works, and when it last failed (DECISION-073).

    The console's system page asks the services that answer requests directly (the database,
    Redis, storage, the bot) and reads this table for the ones that do not: the scheduler's
    heartbeat, the last code sent and the last that could not be, push failures, the nightly
    backup. One row per service, so the table never grows.

    `failure` is a short reason code chosen by the code that records it. It never holds a phone
    number, a token, a URL or an exception message: this table is shown to operators.
    """

    name = models.CharField(max_length=40, primary_key=True)
    ok_at = models.DateTimeField(null=True, blank=True)
    failed_at = models.DateTimeField(null=True, blank=True)
    failure = models.CharField(max_length=60, blank=True)
    # Failures since the last success, so one bad minute and an hour of them read differently.
    failures = models.PositiveIntegerField(default=0)

    def __str__(self) -> str:
        return self.name
