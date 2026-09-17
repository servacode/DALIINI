import pytest
from directory.models import Category, CategoryCapabilities, CategoryGroup
from facilities.models import Facility
from locations.models import Province

@pytest.fixture
def facility(db):
    province=Province.objects.create(code='raqqa-test',name_ar='الرقة',active=True)
    group=CategoryGroup.objects.create(code='health-test',name_ar='الصحة')
    category=Category.objects.create(group=group,code='pharmacy-test',slug='pharmacy-test',name_ar='صيدلية',specialization=Category.Specialization.PHARMACY)
    CategoryCapabilities.objects.create(category=category,supports_duty=True)
    return Facility.objects.create(category=category,province=province,name_ar='صيدلية اختبار',status=Facility.Status.ACTIVE)


@pytest.fixture
def user(db):
    from accounts.models import User

    return User.objects.create_user(
        phone="+963900000001",
        password="StrongPass123!",
        name="Test User",
    )
