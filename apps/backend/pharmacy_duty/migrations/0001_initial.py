import uuid
from django.contrib.postgres.constraints import ExclusionConstraint
from django.contrib.postgres.fields import DateTimeRangeField, RangeOperators
from django.contrib.postgres.operations import BtreeGistExtension
from django.db import migrations, models
import django.db.models.deletion
from django.db.models import F, Func

class TstzRange(Func):
    function='TSTZRANGE'; output_field=DateTimeRangeField()
    def __init__(self,start,end): super().__init__(start,end,models.Value('[)'))

class Migration(migrations.Migration):
    initial=True
    dependencies=[('facilities','0001_initial')]
    operations=[
        BtreeGistExtension(),
        migrations.CreateModel(name='DutyShift',fields=[('id',models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),('starts_at',models.DateTimeField()),('ends_at',models.DateTimeField()),('created_at',models.DateTimeField(auto_now_add=True)),('updated_at',models.DateTimeField(auto_now=True)),('facility',models.ForeignKey(on_delete=django.db.models.deletion.CASCADE,related_name='duty_shifts',to='facilities.facility'))]),
        migrations.AddConstraint(model_name='dutyshift',constraint=models.CheckConstraint(condition=models.Q(('ends_at__gt',F('starts_at'))),name='duty_shift_positive_duration')),
        migrations.AddConstraint(model_name='dutyshift',constraint=ExclusionConstraint(name='prevent_overlapping_duty_for_facility',expressions=[(F('facility'),RangeOperators.EQUAL),(TstzRange(F('starts_at'),F('ends_at')),RangeOperators.OVERLAPS)])),
        migrations.AddIndex(model_name='dutyshift',index=models.Index(fields=['facility','starts_at','ends_at'],name='pharmacy_du_facilit_idx')),
    ]
