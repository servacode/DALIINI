import os

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "directory_backend.settings.development")

from django.core.asgi import get_asgi_application  # noqa: E402

# The Django application registry must be fully populated before importing anything
# that touches models. Channels routing reaches realtime.consumers, which imports
# locations.models, so get_asgi_application() has to run before that import.
django_asgi_app = get_asgi_application()

from channels.routing import ProtocolTypeRouter, URLRouter  # noqa: E402

from directory_backend.routing import websocket_urlpatterns  # noqa: E402

application = ProtocolTypeRouter(
    {
        "http": django_asgi_app,
        "websocket": URLRouter(websocket_urlpatterns),
    }
)
