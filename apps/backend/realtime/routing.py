from django.urls import path

from .consumers import DirectoryConsumer

websocket_urlpatterns = [
    path("ws/v1/directory/", DirectoryConsumer.as_asgi()),
]
