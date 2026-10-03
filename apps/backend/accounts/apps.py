from django.apps import AppConfig


class AccountsConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'accounts'

    def ready(self) -> None:
        from django.db.models.signals import post_migrate

        from .roles import sync_owner_role

        # After every `migrate`, so a permission a release adds reaches the owner role at once.
        post_migrate.connect(sync_owner_role, sender=self, dispatch_uid="accounts.sync_owner_role")
