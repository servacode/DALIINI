import uuid
from django.contrib.auth.models import AbstractBaseUser, PermissionsMixin, BaseUserManager
from django.db import models

class UserManager(BaseUserManager):
    def create_user(self, phone, password=None, **extra):
        if not phone:
            raise ValueError('phone is required')
        user=self.model(phone=phone, **extra)
        if password: user.set_password(password)
        else: user.set_unusable_password()
        user.save(using=self._db)
        return user
    def create_superuser(self, phone, password, **extra):
        extra.setdefault('is_staff', True); extra.setdefault('is_superuser', True)
        return self.create_user(phone, password, **extra)

class User(AbstractBaseUser, PermissionsMixin):
    id=models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    phone=models.CharField(max_length=16, unique=True)
    name=models.CharField(max_length=120)
    province=models.ForeignKey('locations.Province', null=True, blank=True, on_delete=models.SET_NULL)
    is_active=models.BooleanField(default=True)
    is_staff=models.BooleanField(default=False)
    created_at=models.DateTimeField(auto_now_add=True)
    updated_at=models.DateTimeField(auto_now=True)
    USERNAME_FIELD='phone'
    objects=UserManager()


class AdminPermission(models.Model):
    code = models.CharField(max_length=120, unique=True)
    description = models.CharField(max_length=240, blank=True)


class AdminRole(models.Model):
    code = models.CharField(max_length=80, unique=True)
    name = models.CharField(max_length=120)
    permissions = models.ManyToManyField(
        AdminPermission,
        related_name="roles",
        blank=True,
    )


class UserAdminRole(models.Model):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="admin_role_links",
    )
    role = models.ForeignKey(
        AdminRole,
        on_delete=models.CASCADE,
        related_name="user_links",
    )
    active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["user", "role"],
                name="uniq_user_admin_role",
            )
        ]
