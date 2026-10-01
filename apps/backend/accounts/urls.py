from django.urls import path

from . import views

urlpatterns = [
    path("auth/register/start/", views.RegisterStartView.as_view()),
    path("auth/register/verify/", views.RegisterVerifyView.as_view()),
    path("auth/register/complete/", views.RegisterCompleteView.as_view()),
    path("auth/login/", views.LoginView.as_view()),
    path("auth/refresh/", views.RefreshView.as_view()),
    path("auth/logout/", views.LogoutView.as_view()),
    path("auth/logout-all/", views.LogoutAllView.as_view()),
    path("auth/sessions/", views.SessionsView.as_view()),
    path("auth/sessions/<uuid:session_id>/", views.SessionDetailView.as_view()),
    path("auth/recovery/start/", views.RecoveryStartView.as_view()),
    path("auth/recovery/verify/", views.RecoveryVerifyView.as_view()),
    path("auth/recovery/reset/", views.RecoveryResetView.as_view()),
    path("account/profile/", views.ProfileView.as_view()),
    path("account/profile/image/", views.ProfileImageView.as_view()),
    path("account/phone/start/", views.PhoneChangeStartView.as_view()),
    path("account/phone/confirm/", views.PhoneChangeConfirmView.as_view()),
    path("account/password/", views.PasswordChangeView.as_view()),
    path("account/deletion-request/", views.AccountDeletionRequestView.as_view()),
]
