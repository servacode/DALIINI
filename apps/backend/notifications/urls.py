from django.urls import path

from . import views

urlpatterns = [
    path("account/push-token/", views.PushTokenView.as_view()),
    path("account/push-token/unregister/", views.PushTokenUnregisterView.as_view()),
]
