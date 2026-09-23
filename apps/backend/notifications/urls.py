from django.urls import path

from . import views

urlpatterns = [
    path("account/push-token/", views.PushTokenView.as_view()),
    path("account/push-token/unregister/", views.PushTokenUnregisterView.as_view()),
    path("account/notifications/", views.NotificationsView.as_view()),
    path("account/notifications/unread-count/", views.NotificationsUnreadCountView.as_view()),
    path("account/notifications/read-all/", views.NotificationsReadAllView.as_view()),
    path(
        "account/notifications/<uuid:notification_id>/read/",
        views.NotificationReadView.as_view(),
    ),
]
