<template>
	<el-dialog :close-on-click-modal="false" title="子表配置" draggable v-model="visible">
		<el-form :model="form" :rules="dataRules" ref="dataFormRef">
			<el-row :gutter="35">
				<el-col :span="8">
					<el-form-item prop="childTableName">
						<template #label> 子表名<tip content="关联表的子表，例如一对多中的存储多信息的表" /> </template>
						<el-select placeholder="请选择子表" v-model="form.childTableName" filterable @change="getChildTableColumnList">
							<el-option :key="item" :label="item" :value="item" v-for="item in childTableList"> </el-option>
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="8">
					<el-form-item prop="mainField">
						<template #label> 主表字段<tip content="一般为主表的主键字段" /> </template>
						<el-select placeholder="请选关联字段" v-model="form.mainField" filterable>
							<el-option :key="item" :label="item" :value="item" v-for="item in mainTableColumnList"> </el-option>
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="8">
					<el-form-item prop="childField">
						<template #label> 子表字段<tip content="子表中对应主表主键的关联字段" /> </template>
						<el-select placeholder="请选关联字段" v-model="form.childField" filterable>
							<el-option :key="item" :label="item" :value="item" v-for="item in childTableColumnList"> </el-option>
						</el-select>
					</el-form-item>
				</el-col>
			</el-row>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="onClear">清空</el-button>
				<el-button @click="onSubmit" type="primary">{{ $t('common.confirmButtonText') }}</el-button>
			</span>
		</template>
	</el-dialog>
</template>
<script setup lang="ts" name="child">
import { useVModel } from '@vueuse/core';
import { useListTableApi, useListTableColumnApi, useSyncTableApi } from '/@/api/gen/table';
import { useMessage } from '/@/hooks/message';

const emit = defineEmits(['update:modelValue']);

type ChildTableForm = {
	childTableName: string;
	mainField: string;
	childField: string;
};

const props = defineProps<{
	modelValue: ChildTableForm;
}>();

const visible = ref(false);
const dataFormRef = ref();
const createDefaultForm = () => ({
	childTableName: '',
	mainField: '',
	childField: '',
});

const normalizeForm = (val?: Partial<ChildTableForm>) => {
	const source = val || {};
	return {
		childTableName: source.childTableName ?? '',
		mainField: source.mainField ?? '',
		childField: source.childField ?? '',
	};
};

const form = useVModel(props, 'modelValue', emit, {
	passive: true,
	deep: true,
	clone: true,
	defaultValue: createDefaultForm(),
}) as Ref<ChildTableForm>;

const dataRules = ref({
	childTableName: [{ required: true, message: '请选择子表', trigger: 'blur' }],
	mainField: [{ required: true, message: '请选择关联字段', trigger: 'blur' }],
	childField: [{ required: true, message: '请选择关联字段', trigger: 'blur' }],
});

const childTableList = ref();
const childTableColumnList = ref();
const mainTableColumnList = ref();
const currentDsName = ref('');

// 打开弹窗
const openDialog = (row: any) => {
	currentDsName.value = row.dsName;
	visible.value = true;

	getAllTable(row.dsName);
	getAllField(row.dsName, row.tableName);
	form.value = normalizeForm(row);
};

// 获取当前数据下的所有表
const getAllTable = (dsName: string) => {
	useListTableApi(dsName).then((res) => {
		childTableList.value = res.data.map((item: any) => {
			return item.name;
		});
	});
};

// 获取对应表的所有字段
const getAllField = (dsName: string, tableName: string) => {
	useListTableColumnApi(dsName, tableName).then((res) => {
		mainTableColumnList.value = res.data.map((item: any) => {
			return item.name;
		});
	});
};

// 获取子表的全部字段
const getChildTableColumnList = (val: string) => {
	form.value.childField = '';
	useListTableColumnApi(currentDsName.value, val).then((res) => {
		childTableColumnList.value = res.data.map((item: any) => {
			return item.name;
		});
	});
};

// 确认提交
const onSubmit = async () => {
	const valid = await dataFormRef.value.validate().catch(() => {});
	if (!valid) return false;
	useSyncTableApi(currentDsName.value, form.value.childTableName).then(() => {
		useMessage().success('子表信息同步成功');
	});

	visible.value = false;
};

// 清空子表配置，后台当单表处理
const onClear = () => {
	form.value = createDefaultForm();
	visible.value = false;
};

defineExpose({
	openDialog,
});
</script>
